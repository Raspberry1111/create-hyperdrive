package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.*;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity.HyperdriveStateMachine.Phase;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity.HyperdriveStateMachine.ShulkerStatus;
import com.github.raspberry1111.create_hyperdrive.utility.MathHelper;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.sound.SoundScapes;
import dev.egg.SubLevelWarper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangNumberFormat;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static com.github.raspberry1111.create_hyperdrive.ComponentBuilder.builder;
import static com.github.raspberry1111.create_hyperdrive.ComponentBuilder.translate;
import static net.minecraft.ChatFormatting.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HyperdriveBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {
    public static final List<ResourceLocation> ALLOW_LIST = List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "air"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "void_air"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "cave_air")
    );
    public static final int LAZY_TICK_RATE = 10;
    public final LerpedFloat headAngle = LerpedFloat.angular();
    private final HyperdriveStateMachine stateMachine;
    private int previousRedstonePower = -1;
    private boolean wouldTeleportCollide = false;
    private boolean checkedWouldCollideThisTick = false;
    private ScrollOptionBehaviour<TargetDimension> targetDimension;

    public HyperdriveBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        stateMachine = new HyperdriveStateMachine(this::getSpeed, this::triggerTeleportation);
    }

    public int redstonePower() {
        return (!isTargetDimensionCurrent() && !wouldTeleportCollide()) ? 15 : 0;
    }

    private boolean addFailedHeaderIfNeeded(final List<Component> tooltip, final boolean alreadyAdded) {
        if (!alreadyAdded) {
            translate("tooltip.hyperdrive.failed").style(GRAY).forGoggles(tooltip);
        }
        return true;
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        final HyperdriveStateMachine.Phase phase = getPhase();

        builder().space().forGoggles(tooltip);

        final var phaseHeader = translate("tooltip.hyperdrive.phase").style(GRAY);
        switch (phase) {
            case final Phase.Cooldown ignored -> {
                phaseHeader.add(translate("tooltip.hyperdrive.phase.cooldown").style(DARK_GRAY)).forGoggles(tooltip);
            }
            case final Phase.Active ignored -> {
                phaseHeader.add(translate("tooltip.hyperdrive.phase.active").style(DARK_PURPLE)).forGoggles(tooltip);
            }
            case Phase.Charging(final ShulkerStatus shulkerStatus) -> {
                phaseHeader.add(translate("tooltip.hyperdrive.phase.charging").style(AQUA)).forGoggles(tooltip);

                final var statusHeader = translate("tooltip.hyperdrive.shulker_status").style(GRAY);
                switch (shulkerStatus) {
                    case EXHAUSTED ->
                            statusHeader.add(translate("tooltip.hyperdrive.shulker_status.exhausted").style(DARK_GRAY)).forGoggles(tooltip);
                    case NORMAL ->
                            statusHeader.add(translate("tooltip.hyperdrive.shulker_status.normal").style(AQUA)).forGoggles(tooltip);
                    case INFUSED ->
                            statusHeader.add(translate("tooltip.hyperdrive.shulker_status.infused").style(DARK_PURPLE)).forGoggles(tooltip);
                }
            }
        }

        builder().space().forGoggles(tooltip);

        final double progress = getProgressPercent();
        translate("tooltip.hyperdrive.progress").style(GRAY)
                .add(
                        builder().text(LangNumberFormat.format(
                                (int) (progress * 100))
                        ).style(progress > 0.85 ? AQUA :
                                progress > 0.60 ? YELLOW :
                                progress > 0.25 ? GOLD :
                                progress > 0.05 ? RED : DARK_GRAY)
                )
                .add(builder().text("%").style(DARK_GRAY))
                .forGoggles(tooltip);

        builder().space().forGoggles(tooltip);

        if (stateMachine.failedLastTeleport) {
            translate("tooltip.hyperdrive.failed_last").style(GRAY).forGoggles(tooltip);
            translate("tooltip.hyperdrive.failed.would_collide").style(RED).forGoggles(tooltip, 1);
        }

        boolean failedHeader = false;


        if (SableCompanion.INSTANCE.getContaining(this) == null) {
            failedHeader = addFailedHeaderIfNeeded(tooltip, failedHeader);
            translate("tooltip.hyperdrive.failed.not_on_sublevel").style(RED).forGoggles(tooltip, 1);
        }

        if (isTargetDimensionCurrent()) {
            failedHeader = addFailedHeaderIfNeeded(tooltip, failedHeader);
            translate("tooltip.hyperdrive.failed.target_dimension").style(RED).forGoggles(tooltip, 1);
        }

        if (wouldTeleportCollide()) {
            failedHeader = addFailedHeaderIfNeeded(tooltip, failedHeader);
            translate("tooltip.hyperdrive.failed.would_collide").style(RED).forGoggles(tooltip, 1);
        }

        return true;
    }

    public double getProgressPercent() {
        return Math.abs((double) getCurrentProgress()) / stateMachine.targetProgress();
    }

    public ResourceKey<Level> getTargetDimension() {
        return switch (targetDimension.get()) {
            case NETHER -> Level.NETHER;
            case END -> Level.END;
            case OVERWORLD -> Level.OVERWORLD;
        };
    }

    @Nullable
    private TeleportContext buildTeleportContext() {
        final SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(this);
        if (!(subLevel instanceof final ServerSubLevel serverSubLevel)) return null;

        final MinecraftServer server = Objects.requireNonNull(Objects.requireNonNull(level).getServer());
        final ResourceKey<Level> target = getTargetDimension();
        final ServerLevel sourceLevel = serverSubLevel.getLevel();
        final ServerLevel targetLevel = Objects.requireNonNull(server.getLevel(target));
        final double scale = DimensionType.getTeleportationScale(sourceLevel.dimensionType(), targetLevel.dimensionType());
        final Vec3 hyperdrivePosition = serverSubLevel.logicalPose().transformPosition(getBlockPos().getCenter());
        final Vec3 newHyperdrivePosition = hyperdrivePosition.multiply(scale, 1.0, scale);
        final Vector3d shift = new Vector3d(newHyperdrivePosition.x - hyperdrivePosition.x, 0, newHyperdrivePosition.z - hyperdrivePosition.z);
        final Vector3d newSublevelPosition = serverSubLevel.logicalPose().position().add(shift, new Vector3d());

        return new TeleportContext(serverSubLevel, sourceLevel, targetLevel, target, hyperdrivePosition, newHyperdrivePosition, newSublevelPosition, shift);
    }

    private boolean checkWouldTeleportCollide() {
        final TeleportContext ctx = buildTeleportContext();
        if (ctx == null)
            return false;

        return MathHelper.subLevelChainIntersectsAny(ctx.serverSubLevel, ctx.targetLevel, ALLOW_LIST, ctx.shift);
    }

    public boolean wouldTeleportCollide() {
        if (!AllConfigs.server().continousChecking.get())
            return false;

        if (level != null && !level.isClientSide() && !checkedWouldCollideThisTick) {
            checkedWouldCollideThisTick = true;

            final boolean b = checkWouldTeleportCollide();
            if (b != wouldTeleportCollide) {
                wouldTeleportCollide = b;
                sync();
            }
        }

        return wouldTeleportCollide;
    }

    public boolean shouldTick() {
        if (!(stateMachine.phase instanceof HyperdriveStateMachine.Phase.Charging)) return true;

        return isSpeedRequirementFulfilled() && !isTargetDimensionCurrent() && !wouldTeleportCollide();
    }

    public float targetProgress() {
        return stateMachine.targetProgress();
    }

    public double chargeSpeedMultiplier() {
        return stateMachine.chargeSpeedMultiplier();
    }

    public void sync() {
        if (level != null && !level.isClientSide() && level.getBlockState(worldPosition) == getBlockState()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), HyperdriveBlock.UPDATE_ALL);
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            setChanged();
        }
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        targetDimension = new ScrollOptionBehaviour<>(TargetDimension.class,
                Component.translatable(CreateHyperdrive.MODID + ".hyperdrive.target_dimension"), this, new TargetDimensionValueBox());
        targetDimension.value = 0;

        behaviours.add(targetDimension);
    }

    @Override
    public void tick() {
        super.tick();

        if (level != null) {
            if (level.isClientSide()) {
                tickAudio();
                headAngle.tickChaser();

                final float openProgress = Math.abs(HyperdriveRenderer.getOpenProgress(this));
                if (level.getRandom().nextFloat() > 0.99 - 0.5 * openProgress) {
                    final Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, level.getRandom(), (float) (0.125f + 0.125 * openProgress));
                    final Vec3 pos = getBlockPos().getCenter();
                    level.addParticle(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
                }
            } else {
                final int redstonePower = redstonePower();
                if (previousRedstonePower != redstonePower)
                    level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
                previousRedstonePower = redstonePower;
            }
        }

        if (!shouldTick()) {
            stateMachine.moveTowardsZero();
            return;
        }

        if (stateMachine.tick())
            sync();

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {
        if (stateMachine.phase instanceof HyperdriveStateMachine.Phase.Charging && stateMachine.getCurrentProgress() != 0) {
            SoundScapes.play(SoundScapes.AmbienceGroup.COG, BlockPos.containing(SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) getBlockPos().getCenter())), 1f);
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        checkedWouldCollideThisTick = false;

        if (stateMachine.lazyTick(LAZY_TICK_RATE))
            sync();
    }

    public boolean isTargetDimensionCurrent() {
        if (level == null) {
            return false;
        }

        final ResourceKey<Level> targetID = getTargetDimension();
        return targetID == level.dimension();
    }

    private void triggerTeleportation() {
        if (level == null || level.isClientSide) {
            return;
        }

        final TeleportContext ctx = buildTeleportContext();

        if (ctx == null) { // we aren't on a sublevel
            stateMachine.failedLastTeleport = true;
            return;
        }

        if (!MathHelper.subLevelChainIntersectsAny(ctx.serverSubLevel, ctx.targetLevel, ALLOW_LIST, ctx.shift)) {
            stateMachine.failedLastTeleport = false; // update stateMachine before we teleport

            SubLevelWarper.WarpSubLevel(ctx.serverSubLevel, ctx.targetLevel, ctx.newSublevelPosition);

            ctx.sourceLevel.playSound(null,
                    ctx.hyperdrivePosition.x, ctx.hyperdrivePosition.y, ctx.hyperdrivePosition.z,
                    AllSounds.HYPERDRIVE_ACTIVATE_SUCCEEDED.get(), SoundSource.MASTER, 3f, 0.5f);
            ctx.targetLevel.playSound(null,
                    ctx.newHyperdrivePosition.x, ctx.newHyperdrivePosition.y, ctx.newHyperdrivePosition.z,
                    AllSounds.HYPERDRIVE_ACTIVATE_SUCCEEDED.get(), SoundSource.MASTER, 3f, 0.5f);

            final RandomSource random = ctx.sourceLevel.getRandom();
            final Vec3 motionA = VecHelper.offsetRandomly(Vec3.ZERO, random, 5.0f);
            final Vec3 motionB = VecHelper.offsetRandomly(Vec3.ZERO, random, 5.0f);

            ctx.sourceLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    ctx.hyperdrivePosition.x, ctx.hyperdrivePosition.y, ctx.hyperdrivePosition.z,
                    1000,
                    motionA.x, motionA.y, motionA.z,
                    2.0);
            ctx.targetLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    ctx.newHyperdrivePosition.x, ctx.newHyperdrivePosition.y, ctx.newHyperdrivePosition.z,
                    1000,
                    motionB.x, motionB.y, motionB.z,
                    2.0);

        } else {
            level.playSound(null,
                    ctx.hyperdrivePosition.x, ctx.hyperdrivePosition.y, ctx.hyperdrivePosition.z,
                    AllSounds.HYPERDRIVE_ACTIVATE_FAILED.get(), SoundSource.MASTER, 1.5f, 1f);
        }
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        if (!super.isSpeedRequirementFulfilled()) {
            return false;
        }

        if (stateMachine.phase instanceof HyperdriveStateMachine.Phase.Charging) {
            return Math.abs(getSpeed()) >= AllConfigs.server().minimumRPM.get();
        }
        return true;
    }

    @Override
    public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        compound.putString("phase", stateMachine.phase.getSerializedName());
        compound.putInt("current_progress", stateMachine.getCurrentProgress());
        compound.putBoolean("failed_last_teleport", stateMachine.failedLastTeleport);

        if (clientPacket) {
            compound.putBoolean("would_teleport_collide", wouldTeleportCollide);
        }

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        stateMachine.setPhase(HyperdriveStateMachine.Phase.fromString(compound.getString("phase")));
        stateMachine.setCurrentProgress(compound.getInt("current_progress"));
        stateMachine.failedLastTeleport = compound.getBoolean("failed_last_teleport");

        if (clientPacket) {
            wouldTeleportCollide = compound.getBoolean("would_teleport_collide");
        }

        super.read(compound, registries, clientPacket);
    }

    public AABB getBoundingBox(final BlockState state) {
        return Shulker.getProgressAabb(1.0F, state.getValue(HyperdriveBlock.FACING), Math.abs(HyperdriveRenderer.getOpenProgress(this)));
    }

    public ItemStack getItemStackWithData() {
        final ItemStack stack = HyperdriveBlockItem.filledStack();

        stack.set(AllDataComponents.PHASE, stateMachine.phase);
        stack.set(AllDataComponents.CURRENT_PROGRESS, stateMachine.getCurrentProgress());

        return stack;
    }

    public boolean infuse() {
        final boolean value = stateMachine.infuse();
        shouldTick();
        return value;
    }

    public HyperdriveStateMachine.Phase getPhase() {
        return stateMachine.getPhase();
    }

    public void setPhase(final HyperdriveStateMachine.Phase phase) {
        stateMachine.setPhase(phase);
    }

    public int getCurrentProgress() {
        return stateMachine.getCurrentProgress();
    }

    public void setCurrentProgress(final int currentProgress) {
        stateMachine.currentProgress = currentProgress;
    }

    public enum TargetDimension implements INamedIconOptions {
        OVERWORLD(AllIcons.I_OVERWORLD_ISLANDS, "Overworld"),
        NETHER(AllIcons.I_NETHER_PORTAL, "The Nether"),
        END(AllIcons.I_END_PEARL, "The End"),
        ;

        private final String translationKey;
        private final String translation;
        private final AllIcons icon;

        TargetDimension(final AllIcons icon, final String translation) {
            this.icon = icon;
            this.translationKey = CreateHyperdrive.MODID + ".hyperdrive." + Lang.asId(name());
            this.translation = translation;
        }

        public static void provideLang(final BiConsumer<String, String> consumer) {
            consumer.accept(CreateHyperdrive.MODID + ".hyperdrive.target_dimension", "Target Dimension");
            for (final TargetDimension dimension : TargetDimension.values()) {
                consumer.accept(dimension.translationKey, dimension.translation);
            }
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    private record TeleportContext(
            ServerSubLevel serverSubLevel,
            ServerLevel sourceLevel,
            ServerLevel targetLevel,
            ResourceKey<Level> target,
            Vec3 hyperdrivePosition,
            Vec3 newHyperdrivePosition,
            Vector3d newSublevelPosition,
            Vector3d shift
    ) {
    }

    private static class TargetDimensionValueBox extends CenteredSideValueBoxTransform {
        public TargetDimensionValueBox() {
            super((blockState, direction) -> {
                final Direction facing = blockState.getValue(HyperdriveBlock.FACING);
                return facing.getAxis() != direction.getAxis();
            });
        }

        @Override
        public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
            final Direction facing = state.getValue(HyperdriveBlock.FACING);
            return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal())
                    .scale(-4 / 16f));
        }
    }

    public static class HyperdriveStateMachine {
        public static final int ACTIVE_TICKS = 1;
        public static final int DECAY_RATE = 16; // how fast to decay to 0 when not actively charging
        private final Supplier<Float> speedSupplier;
        private final Runnable onTrigger;

        private Phase phase = new Phase.Cooldown();
        private int currentProgress = 0;
        private boolean failedLastTeleport = false;

        HyperdriveStateMachine(final Supplier<Float> speedSupplier, final Runnable onTrigger) {
            this.speedSupplier = speedSupplier;
            this.onTrigger = onTrigger;
        }

        public Phase getPhase() {
            return phase;
        }

        public void setPhase(final Phase phase) {
            currentProgress = 0;
            this.phase = phase;
        }

        public int targetProgress() {
            return switch (phase) {
                case final Phase.Cooldown ignored -> targetCooldownProgress();
                case final Phase.Charging ignored -> targetChargeProgress();
                case final Phase.Active ignored -> ACTIVE_TICKS;
            };
        }

        public int targetChargeProgress() {
            return AllConfigs.server().chargeTicks.get() * 256; // the chargeTicks represents at 256 rpm
        }

        public int targetCooldownProgress() {
            final int target = AllConfigs.server().cooldownTicks.get();

            if (failedLastTeleport) {
                return (int) (target * AllConfigs.server().failedTeleportMultiplier.getF());
            } else {
                return target;
            }
        }

        public int getCurrentProgress() {
            return currentProgress;
        }

        void setCurrentProgress(final int value) {
            currentProgress = value;
        }

        public boolean tick() {
            if (phase instanceof Phase.Charging(final ShulkerStatus status)) {
                final int work = (int) Math.round(speedSupplier.get() * status.chargeSpeedMultiplier());
                currentProgress += work;

                if (Math.abs(currentProgress) >= targetChargeProgress()) {
                    setPhase(Phase.active());
                    return true;
                }
            } else if (phase instanceof Phase.Active) {
                currentProgress += 1;

                if (currentProgress >= ACTIVE_TICKS) {
                    setPhase(Phase.cooldown());
                    onTrigger.run();  // this needs to run after we change the phase or whenever sable teleports the contraption it will trigger again
                    return false; // syncing after a teleport will crash the game because the block no longer exists
                }
            }
            return false;
            // we don't tick cooldown here, only in the lazyTick
        }

        public boolean lazyTick(final int lazyTickRate) {
            if (phase instanceof Phase.Cooldown) {
                currentProgress += lazyTickRate;

                if (currentProgress >= targetCooldownProgress()) {
                    setPhase(Phase.charging(ShulkerStatus.NORMAL));
                    return true;
                }
            }
            return false;
        }

        public void moveTowardsZero() {
            if (Math.abs(currentProgress) <= 8) {
                setCurrentProgress(0);
            } else if (currentProgress < 0) {
                setCurrentProgress(currentProgress + DECAY_RATE);
            } else {
                setCurrentProgress(currentProgress - DECAY_RATE);
            }
        }

        public boolean infuse() {
            return switch (phase) {
                case final Phase.Cooldown ignored -> {
                    setPhase(Phase.charging(ShulkerStatus.EXHAUSTED));
                    yield true;
                }
                case Phase.Charging(
                        final ShulkerStatus shulkerStatus
                ) when shulkerStatus == ShulkerStatus.EXHAUSTED -> {
                    final int oldProgress = getCurrentProgress();
                    setPhase(Phase.charging(ShulkerStatus.NORMAL));
                    setCurrentProgress(oldProgress);
                    yield true;
                }
                case Phase.Charging(final ShulkerStatus shulkerStatus) when shulkerStatus == ShulkerStatus.NORMAL -> {
                    final int oldProgress = getCurrentProgress();
                    setPhase(Phase.charging(ShulkerStatus.INFUSED));
                    setCurrentProgress(oldProgress);
                    yield true;
                }
                default -> false;
            };
        }

        public double chargeSpeedMultiplier() {
            if (phase instanceof Phase.Charging(final ShulkerStatus status)) {
                return status.chargeSpeedMultiplier();
            } else {
                return 1.0;
            }
        }


        public enum ShulkerStatus implements StringRepresentable {
            EXHAUSTED,
            NORMAL,
            INFUSED;


            public static ShulkerStatus fromString(final String s) {
                return switch (s) {
                    case "exhausted" -> EXHAUSTED;
                    case "normal" -> NORMAL;
                    case "infused" -> INFUSED;
                    default -> throw new IllegalArgumentException("Unknown shulker status: " + s);
                };
            }

            public double chargeSpeedMultiplier() {
                return switch (this) {
                    case EXHAUSTED -> AllConfigs.server().exhaustionMultiplier.get();
                    case INFUSED -> AllConfigs.server().infusionMultiplier.get();
                    case NORMAL -> 1.0;
                };
            }

            @Override
            public String getSerializedName() {
                return switch (this) {
                    case EXHAUSTED -> "exhausted";
                    case NORMAL -> "normal";
                    case INFUSED -> "infused";
                };
            }
        }

        public sealed interface Phase permits Phase.Charging, Phase.Active, Phase.Cooldown {
            static Phase fromString(final String s) {
                return switch (s) {
                    case "active" -> new Active();
                    case "cooldown" -> new Cooldown();
                    case final String chargingString when s.startsWith("charging:") -> {
                        final String[] parts = chargingString.split(":", 2);
                        final ShulkerStatus status = parts.length > 1
                                ? ShulkerStatus.fromString(parts[1])
                                : ShulkerStatus.NORMAL;
                        yield new Charging(status);
                    }
                    default -> throw new IllegalArgumentException("Unknown phase: " + s);
                };
            }

            static Phase charging(final ShulkerStatus status) {
                return new Charging(status);
            }

            static Phase active() {
                return new Active();
            }

            static Phase cooldown() {
                return new Cooldown();
            }

            String getSerializedName();

            record Charging(ShulkerStatus shulkerStatus) implements Phase {
                @Override
                public String getSerializedName() {
                    return "charging:" + shulkerStatus().getSerializedName();
                }
            }

            record Active() implements Phase {
                @Override
                public String getSerializedName() {
                    return "active";
                }
            }

            record Cooldown() implements Phase {
                @Override
                public String getSerializedName() {
                    return "cooldown";
                }
            }
        }
    }

}
