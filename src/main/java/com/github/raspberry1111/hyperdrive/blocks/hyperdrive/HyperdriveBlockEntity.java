package com.github.raspberry1111.hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.hyperdrive.Hyperdrive;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HyperdriveBlockEntity extends KineticBlockEntity {
    final HyperdriveStateMachine stateMachine;

    public HyperdriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        setLazyTickRate(HyperdriveStateMachine.LAZY_TICK_RATE);
        stateMachine = new HyperdriveStateMachine(this::getSpeed, this::triggerTeleportation);
    }

    @Override
    public void tick() {
        super.tick();

        Hyperdrive.LOGGER.debug("Ticking hyperdrive at {}. Phase = {}, Status = {}, Progress = {}", this.getBlockPos(), this.stateMachine.phase, this.stateMachine.shulkerStatus, this.stateMachine.currentProgress);
        this.stateMachine.tick();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        Hyperdrive.LOGGER.debug("Lazy ticking hyperdrive at {}. Phase = {}, Status = {}, Progress = {}", this.getBlockPos(), this.stateMachine.phase, this.stateMachine.shulkerStatus, this.stateMachine.currentProgress);
        this.stateMachine.lazyTick();
    }


    private void triggerTeleportation() {
        Hyperdrive.LOGGER.info("TELEPORTING HYPERDRIVE at {}!!!", this.getBlockPos());
    }
}
