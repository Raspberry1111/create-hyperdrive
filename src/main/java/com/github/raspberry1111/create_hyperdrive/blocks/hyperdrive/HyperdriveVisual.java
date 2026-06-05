package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllConfigs;
import com.github.raspberry1111.create_hyperdrive.AllPartialModels;
import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import com.github.raspberry1111.create_hyperdrive.utility.MathHelper;
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
import dev.ryanhcode.sable.companion.SableCompanion;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import java.lang.Math;
import java.util.function.Consumer;

public class HyperdriveVisual extends KineticBlockEntityVisual<HyperdriveBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {

    protected final Matrix4f baseHeadTransform = new Matrix4f();
    protected final Matrix4f baseLidTransform = new Matrix4f();
    private final RotatingInstance shaft;
    private final TransformedInstance shulkerHead;
    private final TransformedInstance lid;
    private final Direction direction;
    private final Direction opposite;
    private final LerpedFloat headAngle = LerpedFloat.angular();
    HyperdriveBlockEntity be;

    public HyperdriveVisual(VisualizationContext context, HyperdriveBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        be = blockEntity;
        direction = blockState.getValue(FACING);

        opposite = direction.getOpposite();
        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(com.simibubi.create.AllPartialModels.SHAFT_HALF))
                .createInstance();
        shulkerHead = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(be.stateMachine.getShulkerHeadModel()))
                .createInstance();

        lid = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.HYPERDRIVE_LID)).createInstance();

        shaft.setup(blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();

        Direction shulkerAlign = Direction.fromAxisAndDirection(rotationAxis(), direction.getAxisDirection());
        shulkerHead.translate(getVisualPosition())
                .center()
                .rotate(new Quaternionf().rotateTo(0, 1, 0, shulkerAlign.getStepX(), shulkerAlign.getStepY(), shulkerAlign.getStepZ())).setChanged();

        Direction lidAlign = shulkerAlign;
        lid.translate(getVisualPosition())
                .center()
                .rotate(new Quaternionf().rotateTo(0, 1, 0, lidAlign.getStepX(), lidAlign.getStepY(), lidAlign.getStepZ()))
                .rotateYDegrees(switch (direction) {
                    case EAST -> -90;
                    case SOUTH -> -180;
                    case WEST -> -270;
                    default -> 0;
                })
                .setChanged();
//                .rotateXDegrees(180).setChanged();

        baseHeadTransform.set(shulkerHead.pose);
        baseLidTransform.set(lid.pose);

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
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(lid);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        float openProgress = be.getOpenProgress(be.stateMachine.currentProgress, ctx.partialTick());
        animateLid(openProgress, ctx.partialTick());
        animateHead(openProgress, ctx.partialTick());
    }

    private void animateHead(float openProgress, float partialTick) {
        var level = be.getLevel();

        var sublevel = SableCompanion.INSTANCE.getContainingClient(be);

        Vector3f up = baseHeadTransform.transformDirection(0, 1, 0, new Vector3f()).normalize();
        Vector3f forward = baseHeadTransform.transformDirection(0, 0, -1, new Vector3f()).normalize();
        if (sublevel != null) {
            // apply the sable rotation onto the normals
            var upd = sublevel.renderPose().transformNormal(new Vector3d(up.x, up.y, up.z)).normalize();
            var forwardd = sublevel.renderPose().transformNormal(new Vector3d(forward.x, forward.y, forward.z)).normalize();

            up = new Vector3f((float) upd.x, (float) upd.y, (float) upd.z);
            forward = new Vector3f((float) forwardd.x, (float) forwardd.y, (float) forwardd.z);
        }

        MathHelper.projectOntoPlane(forward, up);


        var player = Minecraft.getInstance().player;
        var target = player == null ? null : player.getEyePosition();

        Vec3 pos = be.getBlockPos().getCenter();
        if (sublevel != null) {
            pos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) pos);
        }

        float deg;
        if (target != null && target.distanceToSqr(pos) < 75.0) {
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

            deg = AngleHelper.deg(angle);
        } else {
            deg = headAngle.getChaseTarget();
        }

        shulkerHead.setTransform(baseHeadTransform);
        rotateTowards(deg, partialTick);

        var dir = new Vector3f(0, Math.abs(openProgress / 4), 0);
        shulkerHead.translate(dir);
        shulkerHead.uncenter().setChanged();
    }

    private void animateLid(float openProgress, float partialTick) {
        lid.setTransform(baseLidTransform);

        if (be.stateMachine.phase != HyperdriveStateMachine.Phase.ACTIVE) {
            lid.rotateY((float) Math.PI * 2 * AllConfigs.client().rotations.get() * openProgress);
        }

        var dir = new Vector3f(0, Math.abs(openProgress), 0);
        lid
                .uncenter()
                .translate(dir)
                .setChanged();
    }

    public void rotateTowards(float deg, float partialTick) {
        float speed = 0.1f;
        float maxSpeed = 2f;
        float randomNoise = 0;

        if (be.stateMachine.shouldMoveSlow()) {
            speed = 0.05f;
            maxSpeed = 1f;
        } else if (be.stateMachine.shouldMoveFast()) {
            speed = 0.5f;
            maxSpeed = 10f;

            if (AllConfigs.client().jittering.get() && Math.random() > 0.90) {
                randomNoise += (float) (3 - Math.random() * 6); // goes from -3 to 3
            }
        }

        headAngle.chase(deg, speed, LerpedFloat.Chaser.exp(maxSpeed));
        headAngle.tickChaser();

        shulkerHead.rotateYDegrees(headAngle.getValue(partialTick) + randomNoise).setChanged();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(be
                        .stateMachine.getShulkerHeadModel()))
                .stealInstance(shulkerHead);
    }
}
