package com.github.raspberry1111.hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.hyperdrive.AllPartialModels;
import com.github.raspberry1111.hyperdrive.Hyperdrive;
import com.github.raspberry1111.hyperdrive.utility.MathHelper;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import java.util.function.Consumer;

public class HyperdriveVisual extends KineticBlockEntityVisual<HyperdriveBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {

    protected final RotatingInstance shaft;
    protected final TransformedInstance shulkerHead;
    protected final Matrix4f baseTransform = new Matrix4f();
    final Direction direction;
    private final Direction opposite;
    HyperdriveBlockEntity be;
    private float previousHeadAngle = 0;
    private float previousOpenProgress = 0;


    public HyperdriveVisual(VisualizationContext context, HyperdriveBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        direction = blockState.getValue(FACING);

        opposite = direction.getOpposite();
        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(com.simibubi.create.AllPartialModels.SHAFT_HALF))
                .createInstance();
        shulkerHead = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.SHULKER_HEAD_NORMAL))
                .createInstance();

        shaft.setup(blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();


        Direction align = Direction.fromAxisAndDirection(rotationAxis(), direction.getAxisDirection());
        shulkerHead.translate(getVisualPosition())
                .center()
                .rotate(new Quaternionf().rotateTo(0, 1, 0, align.getStepX(), align.getStepY(), align.getStepZ())).setChanged();
        baseTransform.set(shulkerHead.pose);

        be = blockEntity;
    }


    @Override
    public void update(float pt) {
        shaft.setup(blockEntity)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        BlockPos behind = pos.relative(opposite);
        relight(behind, shaft);

        BlockPos inFront = pos.relative(direction);
        relight(inFront, shulkerHead);
    }

    @Override
    protected void _delete() {
        shaft.delete();
        shulkerHead.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(shulkerHead);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {

        var openProgress = switch (be.stateMachine.phase) {
            case COOLDOWN -> 0;
            case ACTIVE ->
                    (HyperdriveStateMachine.ACTIVE_TICKS - (be.stateMachine.currentProgress + ctx.partialTick())) / HyperdriveStateMachine.ACTIVE_TICKS;
            case CHARGING ->
                    (be.stateMachine.currentProgress + ctx.partialTick() * be.getSpeed()) / HyperdriveStateMachine.targetChargeProgress();
        };

        openProgress = Mth.clamp(openProgress, 0, 1);
        previousOpenProgress = openProgress;

//        shulkerHead.setTransform(baseTransform).rotateY((float) (8 * Math.PI * (openProgress))).uncenter();

        Vector3f up = baseTransform.transformDirection(0, 1, 0, new Vector3f()).normalize();
        Vector3f forward = baseTransform.transformDirection(0, 0, -1, new Vector3f()).normalize();
        MathHelper.projectOntoPlane(forward, up);

        var target = ctx.camera().getPosition();
        var pos = getVisualPosition().getCenter();

        Vector3f toTarget = new Vector3f(
                (float) (target.x - pos.x),
                (float) (target.y - pos.y),
                (float) (target.z - pos.z)
        ).normalize();
        MathHelper.projectOntoPlane(toTarget, up);

        double angle = Math.atan2(
                up.dot(new Vector3f(forward).cross(toTarget)),
                forward.dot(toTarget)
        );

        previousHeadAngle = Mth.approachDegrees(previousHeadAngle, AngleHelper.deg(angle), 5.0f);


        shulkerHead.setTransform(baseTransform);
        shulkerHead.rotateYDegrees(previousHeadAngle);
        shulkerHead.uncenter().setChanged();
    }

    @Override
    public void tick(TickableVisual.Context context) {
//        progress = be.stateMachine.currentProgress;
    }
}
