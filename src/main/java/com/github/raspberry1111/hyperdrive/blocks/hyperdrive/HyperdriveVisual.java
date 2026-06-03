package com.github.raspberry1111.hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.hyperdrive.Hyperdrive;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import java.util.function.Consumer;

public class HyperdriveVisual extends KineticBlockEntityVisual<HyperdriveBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {

    protected final RotatingInstance shaft;
    protected final RotatingInstance fan;
    final Direction direction;
    private final Direction opposite;
    HyperdriveBlockEntity be;

    private float previousOpenProgress = 0;

    public HyperdriveVisual(VisualizationContext context, HyperdriveBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        direction = blockState.getValue(FACING);

        opposite = direction.getOpposite();
        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance();
        fan = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.ENCASED_FAN_INNER))
                .createInstance();

        shaft.setup(blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();

        fan.setup(blockEntity, getFanSpeed())
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();

        be = blockEntity;
    }

    private float getFanSpeed() {
        float speed = blockEntity.getSpeed() * 5;
        if (speed > 0)
            speed = Mth.clamp(speed, 80, 64 * 20);
        if (speed < 0)
            speed = Mth.clamp(speed, -64 * 20, -80);
        return speed;
    }

    @Override
    public void update(float pt) {
        shaft.setup(blockEntity)
                .setChanged();
        fan.setup(blockEntity, getFanSpeed())
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        BlockPos behind = pos.relative(opposite);
        relight(behind, shaft);

        BlockPos inFront = pos.relative(direction);
        relight(inFront, fan);
    }

    @Override
    protected void _delete() {
        shaft.delete();
        fan.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(fan);
    }

    private float getOpenProgress(float partialTick) {
        return switch (be.stateMachine.phase) {
            case COOLDOWN -> 0;
            case ACTIVE ->
                    (HyperdriveStateMachine.ACTIVE_TICKS - (be.stateMachine.currentProgress + partialTick)) / HyperdriveStateMachine.ACTIVE_TICKS;
            case CHARGING ->
                    Math.abs(be.stateMachine.currentProgress + partialTick * be.getSpeed() * 20) / HyperdriveStateMachine.targetChargeProgress();
        };
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {

        var openProgress = switch (be.stateMachine.phase) {
            case COOLDOWN -> 0;
            case ACTIVE ->
                    (HyperdriveStateMachine.ACTIVE_TICKS - (be.stateMachine.currentProgress + ctx.partialTick())) / HyperdriveStateMachine.ACTIVE_TICKS;
            case CHARGING ->
                    Math.abs(be.stateMachine.currentProgress + ctx.partialTick() * be.getSpeed()) / HyperdriveStateMachine.targetChargeProgress();
        };

        openProgress = Mth.clamp(openProgress, 0, 1);
        if (previousOpenProgress == openProgress)
            return;
        previousOpenProgress = openProgress;

        fan.x = getVisualPosition().getX() + openProgress;
        fan.setChanged();
    }

    @Override
    public void tick(TickableVisual.Context context) {
//        progress = be.stateMachine.currentProgress;
    }
}
