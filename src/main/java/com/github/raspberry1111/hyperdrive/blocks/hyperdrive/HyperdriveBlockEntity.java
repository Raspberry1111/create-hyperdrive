package com.github.raspberry1111.hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.hyperdrive.Hyperdrive;
import com.github.raspberry1111.hyperdrive.AllConfigs;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.egg.SubLevelWarper;

public class HyperdriveBlockEntity extends KineticBlockEntity {
    final HyperdriveStateMachine stateMachine;

    public HyperdriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        setLazyTickRate(HyperdriveStateMachine.LAZY_TICK_RATE);
        stateMachine = new HyperdriveStateMachine(this::getSpeed, this::triggerTeleportation);
    }

    private void getSublevel() {
    }

    @Override
    public void tick() {
        super.tick();

        if (stateMachine.phase == HyperdriveStateMachine.Phase.CHARGING && (!isSpeedRequirementFulfilled()) || SableCompanion.INSTANCE.getContaining(this) == null) {
            stateMachine.moveTowardsZero();
            return;
        }

        stateMachine.tick();
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

        SubLevelAccess sublevel = SableCompanion.INSTANCE.getContaining(this);
        if (sublevel instanceof ServerSubLevel serverSubLevel) {
            Hyperdrive.LOGGER.info("TELEPORTING HYPERDRIVE at {}!!! {}", this.getBlockPos(), serverSubLevel.getRuntimeId());

            SubLevelWarper.WarpSubLevel(serverSubLevel, level.getServer().getLevel(Level.NETHER));
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
        compound.putString("Phase", stateMachine.phase.name());
        compound.putString("ShulkerStatus", stateMachine.shulkerStatus.name());
        compound.putInt("CurrentProgress", stateMachine.currentProgress);

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        for (HyperdriveStateMachine.Phase phase : HyperdriveStateMachine.Phase.values()) {
            if (phase.name().equals(compound.getString("Phase"))) {
                stateMachine.phase = phase;
                break;
            }
        }

        for (HyperdriveStateMachine.ShulkerStatus status : HyperdriveStateMachine.ShulkerStatus.values()) {
            if (status.name().equals(compound.getString("ShulkerStatus"))) {
                stateMachine.shulkerStatus = status;
                break;
            }
        }


        stateMachine.currentProgress = compound.getInt("CurrentProgress");
        super.read(compound, registries, clientPacket);
    }

}
