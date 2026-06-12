package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllPartialModels;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity.HyperdriveStateMachine.Phase;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.Consumer;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HyperdriveVisual extends KineticBlockEntityVisual<HyperdriveBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {

    private final Matrix4f baseHeadTransform = new Matrix4f();
    private final Matrix4f baseLidTransform = new Matrix4f();
    private final RotatingInstance shaft;
    private final TransformedInstance shulkerHead;
    private final TransformedInstance lid;
    private final Direction opposite;
    private final HyperdriveBlockEntity be;
    private final RandomSource random = RandomSource.create();

    public HyperdriveVisual(final VisualizationContext context, final HyperdriveBlockEntity blockEntity, final float partialTick) {
        super(context, blockEntity, partialTick);

        be = blockEntity;
        final Direction direction = blockState.getValue(FACING);

        opposite = direction.getOpposite();
        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.TINY_SHAFT))
                .createInstance();
        shulkerHead = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        Objects.requireNonNullElse(HyperdriveRenderer.getHeadModel(be.getPhase()), AllPartialModels.SHULKER_HEAD_NORMAL)
                ))
                .createInstance();

        lid = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.HYPERDRIVE_LID)).createInstance();

        shaft.setup(blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();

        shulkerHead.translate(getVisualPosition())
                .center()
                .rotate(new Quaternionf().rotateTo(0, 1, 0, direction.getStepX(), direction.getStepY(), direction.getStepZ())).setChanged();

        lid.translate(getVisualPosition())
                .center()
                .rotate(new Quaternionf().rotateTo(0, 1, 0, direction.getStepX(), direction.getStepY(), direction.getStepZ()))
                .rotateYDegrees(switch (direction) {
                    case EAST -> -90;
                    case SOUTH -> -180;
                    case WEST -> -270;
                    default -> 0;
                })
                .setChanged();

        baseHeadTransform.set(shulkerHead.pose);
        baseLidTransform.set(lid.pose);

    }

    @Override
    public void update(final float pt) {
        shaft.setup(blockEntity)
                .setChanged();
    }

    @Override
    public void updateLight(final float partialTick) {
        final BlockPos behind = pos.relative(opposite);
        relight(behind, shaft);
        relight(shulkerHead);
        relight(lid);
    }

    @Override
    protected void _delete() {
        shaft.delete();
        shulkerHead.delete();
        lid.delete();
    }

    @Override
    public void collectCrumblingInstances(final Consumer<Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(lid);
    }

    @Override
    public void beginFrame(final DynamicVisual.Context ctx) {
        final float openProgress = HyperdriveRenderer.getOpenProgress(be, ctx.partialTick());

        animateHead(openProgress, ctx.partialTick());
        animateLid(openProgress);
    }

    private void animateHead(final float openProgress, final float partialTick) {
        final float deg = HyperdriveRenderer.getHeadRotationDegrees(be.getBlockPos().getCenter(),
                baseHeadTransform.transformDirection(0, 1, 0, new Vector3f()).normalize(),
                baseHeadTransform.transformDirection(0, 0, -1, new Vector3f()).normalize(),
                SableCompanion.INSTANCE.getContainingClient(be),
                partialTick, random, be.getPhase(), be.headAngle
        );
        final Vector3f dir = HyperdriveRenderer.getHeadDisplacement(openProgress);

        shulkerHead
                .setTransform(baseHeadTransform)
                .rotateYDegrees(deg)
                .uncenter()
                .translate(dir)
                .setChanged();
    }

    private void animateLid(final float openProgress) {
        final Phase phase = be.getPhase();
        final float rads = HyperdriveRenderer.getLidRotationRads(phase, openProgress);
        final var dir = new Vector3f(0, Math.abs(openProgress), 0);

        lid
                .setTransform(baseLidTransform)
                .rotateY(rads)
                .uncenter()
                .translate(dir)
                .setChanged();
    }

    @Override
    public void tick(final TickableVisual.Context context) {
        final var headModel = HyperdriveRenderer.getHeadModel(be.getPhase());
        if (headModel != null) {
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(headModel))
                    .stealInstance(shulkerHead);
        }
    }
}
