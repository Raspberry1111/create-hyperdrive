package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.*;
import com.github.raspberry1111.create_hyperdrive.utility.MathHelper;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.egg.SubLevelWarper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3d;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.simibubi.create.foundation.sound.SoundScapes;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HyperdriveBlockEntity extends KineticBlockEntity {
    public static final List<ResourceLocation> ALLOW_LIST = List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "air"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "void_air"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "cave_air")
    );
    final HyperdriveStateMachine stateMachine;
    protected ScrollOptionBehaviour<TargetDimension> targetDimension;
    private boolean shouldTick = false;


    public HyperdriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        setLazyTickRate(HyperdriveStateMachine.LAZY_TICK_RATE);
        stateMachine = new HyperdriveStateMachine(this::getSpeed, this::triggerTeleportation);
    }

    public float getOpenProgress(float progress, float partialTick) {
        var partialWork = partialTick * (float) stateMachine.shulkerStatus.chargeSpeedMultiplier();

        if (partialTick == 0 || !shouldTick()) {
            partialWork = 0;
        }

        var openProgress = switch (stateMachine.phase) {
            case COOLDOWN -> 0;
            case ACTIVE ->
                    (HyperdriveStateMachine.ACTIVE_TICKS - (progress + partialWork)) / HyperdriveStateMachine.ACTIVE_TICKS;
            case CHARGING -> (progress + partialWork * getSpeed()) / HyperdriveStateMachine.targetChargeProgress();
        };
        return Math.clamp(openProgress, -1, 1);
    }

    public boolean serverShouldTick() {
        if (stateMachine.phase == HyperdriveStateMachine.Phase.CHARGING) {
            if (isSpeedRequirementFulfilled() && SableCompanion.INSTANCE.getContaining(this) != null && !isTargetDimensionCurrent()) {
                if (AllConfigs.server().continousChecking.get()) {
                    MinecraftServer server = Objects.requireNonNull(level.getServer());
                    ServerSubLevel subLevel = (ServerSubLevel) SableCompanion.INSTANCE.getContaining(this);

                    ResourceKey<Level> target = switch (targetDimension.get()) {
                        case NETHER -> Level.NETHER;
                        case END -> Level.END;
                        case OVERWORLD -> Level.OVERWORLD;
                    };

                    ServerLevel targetLevel = Objects.requireNonNull(server.getLevel(target));
                    return !MathHelper.subLevelChainIntersectsAny(subLevel, targetLevel, ALLOW_LIST);
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean shouldTick() {
        if (level == null || level.isClientSide()) {
            return shouldTick;
        } else {
            boolean newShouldTick = serverShouldTick();
            if (newShouldTick != shouldTick) {
                shouldTick = newShouldTick;
                sync();
            }
            return shouldTick;
        }

    }

    // Called when the chunk is first sent to the client
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("shouldTick", shouldTick);
        return tag;
    }

    // Creates the update packet sent to nearby clients
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Called on the client when the packet arrives
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        CompoundTag tag = packet.getTag();
        shouldTick = tag.getBoolean("shouldTick");
    }

    // Call this on the server to push the update to all watching clients
    public void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), HyperdriveBlock.UPDATE_CLIENTS);
            setChanged();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
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
        if (stateMachine.phase != HyperdriveStateMachine.Phase.CHARGING) {
            return;
        }
        if (stateMachine.getCurrentProgress() == 0) {
            return;
        }

        SoundScapes.play(SoundScapes.AmbienceGroup.COG, BlockPos.containing(SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) getBlockPos().getCenter())), 1f);
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

        ResourceKey<Level> targetID = switch (targetDimension.get()) {
            case NETHER -> Level.NETHER;
            case END -> Level.END;
            case OVERWORLD -> Level.OVERWORLD;
        };
        return targetID == level.dimension();
    }

    private void triggerTeleportation() {
        if (level == null || level.isClientSide) {
            return;
        }
        MinecraftServer server = Objects.requireNonNull(level.getServer());

        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(this);
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            ResourceKey<Level> target = switch (targetDimension.get()) {
                case NETHER -> Level.NETHER;
                case END -> Level.END;
                case OVERWORLD -> Level.OVERWORLD;
            };

            ServerLevel targetLevel = Objects.requireNonNull(server.getLevel(target));
            double scale = MathHelper.dimensionScale(serverSubLevel.getLevel(), targetLevel);
            Vector3d newSublevelPosition = serverSubLevel.logicalPose().position().mul(scale, 1.0, scale, new Vector3d());

            Vec3 hyperdrivePosition = SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) getBlockPos().getCenter());
            Vec3 newHyperdrivePosition = hyperdrivePosition.multiply(scale, 1.0, scale);

            CreateHyperdrive.LOGGER.debug("[Hyperdrive::triggerTeleportation] trying to teleport to {} in {}", newSublevelPosition, target);
            if (!MathHelper.subLevelChainIntersectsAny(serverSubLevel, targetLevel, ALLOW_LIST)) {
                level.playSound(null, BlockPos.containing(hyperdrivePosition), AllSounds.HYPERDRIVE_ACTIVATE_SUCCEEDED.get(), SoundSource.MASTER, 3f, 0.5f);
                SubLevelWarper.WarpSubLevel(serverSubLevel, targetLevel, newSublevelPosition);
                targetLevel.playSound(null, BlockPos.containing(newHyperdrivePosition), AllSounds.HYPERDRIVE_ACTIVATE_SUCCEEDED.get(), SoundSource.MASTER, 3f, 0.5f);
            } else {
                level.playSound(null, BlockPos.containing(hyperdrivePosition), AllSounds.HYPERDRIVE_ACTIVATE_FAILED.get(), SoundSource.MASTER, 1.5f, 1f);
            }
        }
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        if (!super.isSpeedRequirementFulfilled()) {
            return false;
        }

        if (stateMachine.phase != HyperdriveStateMachine.Phase.CHARGING) {
            return true;
        }

        return Math.abs(getSpeed()) >= AllConfigs.server().minimumRPM.get();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putString("phase", stateMachine.phase.toString());
        compound.putString("shulker_status", stateMachine.shulkerStatus.toString());
        compound.putInt("current_progress", stateMachine.getCurrentProgress());

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        for (HyperdriveStateMachine.Phase phase : HyperdriveStateMachine.Phase.values()) {
            if (phase.name().equals(compound.getString("phase"))) {
                stateMachine.phase = phase;
                break;
            }
        }

        for (HyperdriveStateMachine.ShulkerStatus status : HyperdriveStateMachine.ShulkerStatus.values()) {
            if (status.name().equals(compound.getString("shulker_status"))) {
                stateMachine.shulkerStatus = status;
                break;
            }
        }

        stateMachine.setCurrentProgress(compound.getInt("current_progress"));
        super.read(compound, registries, clientPacket);
    }

    public AABB getBoundingBox(BlockState state) {
        return Shulker.getProgressAabb(1.0F, state.getValue(HyperdriveBlock.FACING), Math.abs(getOpenProgress(stateMachine.getCurrentProgress(), 0)));
    }

    public ItemStack getItemStackWithData() {
        ItemStack stack = HyperdriveBlockItem.filledStack();

        stack.set(AllDataComponents.PHASE, stateMachine.phase);
        stack.set(AllDataComponents.SHULKER_STATUS, stateMachine.shulkerStatus);
        stack.set(AllDataComponents.CURRENT_PROGRESS, stateMachine.getCurrentProgress());

        return stack;
    }

    public boolean infuse() {
        boolean value = stateMachine.infuse();
        shouldTick();
        return value;
    }

    public enum TargetDimension implements INamedIconOptions {
        OVERWORLD(AllIcons.I_OVERWORLD_ISLANDS, "Overworld"),
        NETHER(AllIcons.I_NETHER_PORTAL, "The Nether"),
        END(AllIcons.I_END_PEARL, "The End"),
        ;

        private final String translationKey;
        private final String translation;
        private final AllIcons icon;

        TargetDimension(AllIcons icon, String translation) {
            this.icon = icon;
            this.translationKey = CreateHyperdrive.MODID + ".hyperdrive." + Lang.asId(name());
            this.translation = translation;
        }

        public static void provideLang(BiConsumer<String, String> consumer) {
            consumer.accept(CreateHyperdrive.MODID + ".hyperdrive.target_dimension", "Target Dimension");
            for (TargetDimension dimension : TargetDimension.values()) {
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
                Direction facing = blockState.getValue(HyperdriveBlock.FACING);
                return facing.getAxis() != direction.getAxis();
            });
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(HyperdriveBlock.FACING);
            return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal())
                    .scale(-4 / 16f));
        }
    }
}
