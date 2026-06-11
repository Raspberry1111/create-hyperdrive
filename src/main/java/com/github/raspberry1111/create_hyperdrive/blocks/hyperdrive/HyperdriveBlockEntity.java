package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.*;
import com.github.raspberry1111.create_hyperdrive.utility.MathHelper;
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
import org.joml.Vector3d;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HyperdriveBlockEntity extends KineticBlockEntity {
    public static final List<ResourceLocation> ALLOW_LIST = List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "air"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "void_air"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "cave_air")
    );
    public final LerpedFloat headAngle = LerpedFloat.angular();
    private final HyperdriveStateMachine stateMachine;
    private ScrollOptionBehaviour<TargetDimension> targetDimension;
    private boolean shouldTick = false;


    public HyperdriveBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        setLazyTickRate(HyperdriveStateMachine.LAZY_TICK_RATE);
        stateMachine = new HyperdriveStateMachine(this::getSpeed, this::triggerTeleportation);
    }

    public float getOpenProgress(final float progress, final float partialTick) {
        float partialWork;

//        if (partialTick == 0 || !shouldTick()) {
//            partialWork = 0;
//        } else {
        partialWork = partialTick * (float) stateMachine.chargeSpeedMultiplier();
//        }

        if (!shouldTick()) {
            partialWork = 0;
        }

        final var openProgress = switch (stateMachine.phase) {
            case final HyperdriveStateMachine.Phase.Cooldown ignored -> 0;
            case final HyperdriveStateMachine.Phase.Active ignored -> (HyperdriveStateMachine.ACTIVE_TICKS -
                    (progress + partialWork) // progress during active is always positive so we can just add
            )
                    / HyperdriveStateMachine.ACTIVE_TICKS;
            case final HyperdriveStateMachine.Phase.Charging ignored -> {
                float speed = getSpeed();
                if (!shouldTick() && progress != 0) {
                    speed = -Math.signum(progress) * HyperdriveStateMachine.DECAY_RATE;
                    partialWork = partialTick;
                }

                yield (progress + partialWork * speed) / stateMachine.targetChargeProgress();
            }
        };
        return Math.clamp(openProgress, -1, 1);
    }

    public ResourceKey<Level> getTargetDimension() {
        return switch (targetDimension.get()) {
            case NETHER -> Level.NETHER;
            case END -> Level.END;
            case OVERWORLD -> Level.OVERWORLD;
        };
    }

    public boolean serverShouldTick() {
        if (!(stateMachine.phase instanceof HyperdriveStateMachine.Phase.Charging)) return true;

        if (!isSpeedRequirementFulfilled()) return false;
        if (isTargetDimensionCurrent()) return false;

        final SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(this);
        if (subLevel == null) return false;

        if (AllConfigs.server().continousChecking.get()) {
            final MinecraftServer server = Objects.requireNonNull(Objects.requireNonNull(level).getServer());
            final ResourceKey<Level> target = getTargetDimension();
            final ServerLevel targetLevel = Objects.requireNonNull(server.getLevel(target));

            final double scale = DimensionType.getTeleportationScale(level.dimensionType(), targetLevel.dimensionType());
            final Vec3 hyperdrivePosition = subLevel.logicalPose().transformPosition(getBlockPos().getCenter());
            final Vec3 newHyperdrivePosition = hyperdrivePosition.multiply(scale, 1.0, scale);
            final Vector3d shift = new Vector3d(newHyperdrivePosition.x - hyperdrivePosition.x, newHyperdrivePosition.y - hyperdrivePosition.y, newHyperdrivePosition.z - hyperdrivePosition.z);

            return !MathHelper.subLevelChainIntersectsAny((ServerSubLevel) subLevel, targetLevel, ALLOW_LIST, shift);
        } else {
            return true;
        }
    }

    public boolean shouldTick() {
        if (level != null && !level.isClientSide()) {
            final boolean newShouldTick = serverShouldTick();
            if (newShouldTick != shouldTick) {
                shouldTick = newShouldTick;
                sync();
            }
        }
        return shouldTick;
    }

    public void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), HyperdriveBlock.UPDATE_CLIENTS);
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

        if (level != null && level.isClientSide()) {
            tickAudio();
            headAngle.tickChaser();

            final var openProgress = getOpenProgress(getCurrentProgress(), 0);
            if (level.getRandom().nextFloat() > 0.99 - 0.5 * Math.abs(openProgress)) {
                final Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, level.getRandom(), (float) (0.125f + 0.125 * Math.abs(openProgress)));
                final Vec3 pos = getBlockPos().getCenter();
                level.addParticle(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
            }
        }

        if (!shouldTick()) {
            stateMachine.moveTowardsZero();
            return;
        }

        stateMachine.tick();

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
        stateMachine.lazyTick();
    }

    private boolean isTargetDimensionCurrent() {
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
        final MinecraftServer server = Objects.requireNonNull(level.getServer());

        final SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(this);
        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final ResourceKey<Level> target = getTargetDimension();
            final ServerLevel sourceLevel = serverSubLevel.getLevel();
            final ServerLevel targetLevel = Objects.requireNonNull(server.getLevel(target));

            final double scale = DimensionType.getTeleportationScale(sourceLevel.dimensionType(), targetLevel.dimensionType());
            final Vector3d sublevelPosition = serverSubLevel.logicalPose().position();

            final Vec3 hyperdrivePosition = serverSubLevel.logicalPose().transformPosition(getBlockPos().getCenter());
            final Vec3 newHyperdrivePosition = hyperdrivePosition.multiply(scale, 1.0, scale);
            final Vector3d shift = new Vector3d(newHyperdrivePosition.x - hyperdrivePosition.x, newHyperdrivePosition.y - hyperdrivePosition.y, newHyperdrivePosition.z - hyperdrivePosition.z);

            final Vector3d newSublevelPosition = sublevelPosition.add(shift, new Vector3d());

            CreateHyperdrive.LOGGER.debug("[Hyperdrive::triggerTeleportation] trying to teleport to {} in {}", newSublevelPosition, target);
            if (!MathHelper.subLevelChainIntersectsAny(serverSubLevel, targetLevel, ALLOW_LIST, shift)) {
                stateMachine.failedLastTeleport = false; // update stateMachine before we teleport

                sourceLevel.playSound(null, hyperdrivePosition.x, hyperdrivePosition.y, hyperdrivePosition.z, AllSounds.HYPERDRIVE_ACTIVATE_SUCCEEDED.get(), SoundSource.MASTER, 3f, 0.5f);
                SubLevelWarper.WarpSubLevel(serverSubLevel, targetLevel, newSublevelPosition);
                targetLevel.playSound(null, newHyperdrivePosition.x, newHyperdrivePosition.y, newHyperdrivePosition.z, AllSounds.HYPERDRIVE_ACTIVATE_SUCCEEDED.get(), SoundSource.MASTER, 3f, 0.5f);

                final RandomSource random = sourceLevel.getRandom();
                final Vec3 motionA = VecHelper.offsetRandomly(Vec3.ZERO, random, 5.0f);
                final Vec3 motionB = VecHelper.offsetRandomly(Vec3.ZERO, random, 5.0f);

                sourceLevel.sendParticles(ParticleTypes.DRAGON_BREATH, hyperdrivePosition.x, hyperdrivePosition.y, hyperdrivePosition.z, 1000, motionA.x, motionA.y, motionA.z, 2.0);
                targetLevel.sendParticles(ParticleTypes.DRAGON_BREATH, newHyperdrivePosition.x, newHyperdrivePosition.y, newHyperdrivePosition.z, 1000, motionB.x, motionB.y, motionB.z, 2.0);

                return;
            } else {
                level.playSound(null, BlockPos.containing(hyperdrivePosition), AllSounds.HYPERDRIVE_ACTIVATE_FAILED.get(), SoundSource.MASTER, 1.5f, 1f);
            }
        }
        stateMachine.failedLastTeleport = true;
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
            compound.putBoolean("should_tick", shouldTick);
        }

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        stateMachine.setPhase(HyperdriveStateMachine.Phase.fromString(compound.getString("phase")));
        stateMachine.setCurrentProgress(compound.getInt("current_progress"));
        stateMachine.failedLastTeleport = compound.getBoolean("failed_last_teleport");

        if (clientPacket) {
            shouldTick = compound.getBoolean("should_tick");
        }

        super.read(compound, registries, clientPacket);
    }

    public AABB getBoundingBox(final BlockState state) {
        return Shulker.getProgressAabb(1.0F, state.getValue(HyperdriveBlock.FACING), Math.abs(getOpenProgress(stateMachine.getCurrentProgress(), 0)));
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
        public static final int LAZY_TICK_RATE = 20;
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

        public void tick() {
            if (phase instanceof Phase.Charging(final ShulkerStatus status)) {
                final int work = (int) Math.round(speedSupplier.get() * status.chargeSpeedMultiplier());
                currentProgress += work;

                if (Math.abs(currentProgress) >= targetChargeProgress()) {
                    setPhase(Phase.active());
                }
                return;
            }

            if (phase instanceof Phase.Active) {
                currentProgress += 1;

                if (currentProgress >= ACTIVE_TICKS) {
                    setPhase(Phase.cooldown());
                    onTrigger.run();  // this needs to run after we change the phase or whenever sable teleports the contraption it will trigger again
                }
            }
            // we don't tick cooldown here, only in the lazyTick
        }

        public void lazyTick() {
            if (phase instanceof Phase.Cooldown) {
                currentProgress += LAZY_TICK_RATE;

                if (currentProgress >= targetCooldownProgress()) {
                    setPhase(Phase.charging(ShulkerStatus.NORMAL));
                }
            }
        }

        public void moveTowardsZero() {
            if (Math.abs(currentProgress) < 8) {
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
