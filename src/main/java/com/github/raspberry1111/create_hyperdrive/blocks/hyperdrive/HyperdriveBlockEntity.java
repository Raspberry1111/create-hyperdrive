package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllBlocks;
import com.github.raspberry1111.create_hyperdrive.AllDataComponents;
import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import com.github.raspberry1111.create_hyperdrive.AllConfigs;
import com.github.raspberry1111.create_hyperdrive.utility.MathHelper;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.egg.SubLevelWarper;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HyperdriveBlockEntity extends KineticBlockEntity {
    public static final List<ResourceLocation> ALLOW_LIST = List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "air")
    );
    final HyperdriveStateMachine stateMachine;
    public LerpedFloat headAnimation;
    protected LerpedFloat headAngle;
    protected ScrollOptionBehaviour<TargetDimension> targetDimensions;

    public HyperdriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        setLazyTickRate(HyperdriveStateMachine.LAZY_TICK_RATE);
        stateMachine = new HyperdriveStateMachine(this::getSpeed, this::triggerTeleportation);
    }

    public float getOpenProgress(float progress, float partialTick) {
        var partialWork = partialTick * (float) stateMachine.shulkerStatus.chargeSpeedMultiplier();

        if (!shouldTick()) {
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

    public boolean shouldTick() {
        if (stateMachine.phase == HyperdriveStateMachine.Phase.CHARGING) {
            return isSpeedRequirementFulfilled() && SableCompanion.INSTANCE.getContaining(this) != null;
        } else {
            return true;
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        targetDimensions = new ScrollOptionBehaviour<>(TargetDimension.class,
                Component.translatable(CreateHyperdrive.MODID + ".hyperdrive.target_dimension"), this, new TargetDimensionValueBox());
        targetDimensions.value = 0;
        behaviours.add(targetDimensions);
    }

    @Override
    public void tick() {
        super.tick();

        if (!shouldTick()) {
            stateMachine.moveTowardsZero();
            return;
        }

        stateMachine.tick();

        if (stateMachine.phase == HyperdriveStateMachine.Phase.CHARGING) {
            pushEntities();
        }

    }

    private void pushEntities() {
        if (level == null) {
        }
//
//        BlockState state = getBlockState();
//        Direction direction = getBlockState().getValue(HyperdriveBlock.FACING);
//        Vec3 worldPosition = SableCompanion.INSTANCE.projectOutOfSubLevel(getLevel(), (Position) getBlockPos().getBottomCenter());
//        AABB aabb = Shulker.getProgressDeltaAabb(1.0F, direction, getOpenProgress(oldProgress, 0), getOpenProgress(stateMachine.currentProgress, 0)).move(worldPosition);
//
//        CreateHyperdrive.LOGGER.debug("pushEntities position {}", getBlockPos());
//        List<Entity> list = level.getEntities(null, aabb);
//        if (!list.isEmpty()) {
//            for (Entity entity : list) {
//                if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
//
//                    AABB entityAABB = entity.getBoundingBox();
//
//                    double overlapX = Math.min(aabb.maxX, entityAABB.maxX) - Math.max(aabb.minX, entityAABB.minX);
//                    double overlapY = Math.min(aabb.maxY, entityAABB.maxY) - Math.max(aabb.minY, entityAABB.minY);
//                    double overlapZ = Math.min(aabb.maxZ, entityAABB.maxZ) - Math.max(aabb.minZ, entityAABB.minZ);
//
//                    CreateHyperdrive.LOGGER.debug("overlap: ( {} {} ) ( {} {} ) | {} {} {}", aabb.minY, aabb.maxY, entityAABB.minY, entityAABB.maxY, overlapX, overlapY, overlapZ);
//                    entity.move(
//                            MoverType.SHULKER_BOX,
//                            new Vec3(
//                                    overlapX * direction.getStepX(),
//                                    overlapY * direction.getStepY(),
//                                    overlapZ * direction.getStepZ()
//                            )
//                    );
//                }
//            }
//        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        stateMachine.lazyTick();
    }

    private void triggerTeleportation() {
        if (level == null || level.isClientSide) {
            return;
        }
        MinecraftServer server = Objects.requireNonNull(level.getServer());

        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(this);
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            ResourceKey<Level> target = switch (targetDimensions.get()) {
                case NETHER -> Level.NETHER;
                case END -> Level.END;
                case OVERWORLD -> Level.OVERWORLD;
            };

            ServerLevel targetLevel = Objects.requireNonNull(server.getLevel(target));
            double scale = MathHelper.dimensionScale(serverSubLevel.getLevel(), targetLevel);
            Vector3d position = serverSubLevel.logicalPose().position().mul(scale, 1.0, scale, new Vector3d());

            CreateHyperdrive.LOGGER.debug("[Hyperdrive::triggerTeleportation] trying to teleport to {} in {}", position, target);
            if (!MathHelper.subLevelChainIntersectsAny(serverSubLevel, targetLevel, ALLOW_LIST)) {

                SubLevelWarper.WarpSubLevel(serverSubLevel, targetLevel, position);
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
        compound.putInt("current_progress", stateMachine.currentProgress);

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


        stateMachine.currentProgress = compound.getInt("current_progress");
        super.read(compound, registries, clientPacket);
    }

    public AABB getBoundingBox(BlockState state) {
        return Shulker.getProgressAabb(1.0F, state.getValue(HyperdriveBlock.FACING), Math.abs(getOpenProgress(stateMachine.currentProgress, 0)));
    }

    public ItemStack getItemStackWithData() {
        ItemStack stack = HyperdriveBlockItem.filledStack();

        stack.set(AllDataComponents.PHASE, stateMachine.phase);
        stack.set(AllDataComponents.SHULKER_STATUS, stateMachine.shulkerStatus);
        stack.set(AllDataComponents.CURRENT_PROGRESS, stateMachine.currentProgress);

        return stack;
    }

    public enum TargetDimension implements INamedIconOptions {
        OVERWORLD(AllIcons.I_ROTATE_NEVER_PLACE, "Overworld"),
        NETHER(AllIcons.I_3x3, "The Nether"),
        END(AllIcons.I_ACTIVE, "The End"),
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
