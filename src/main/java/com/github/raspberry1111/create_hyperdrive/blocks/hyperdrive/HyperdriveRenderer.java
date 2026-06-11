package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllConfigs;
import com.github.raspberry1111.create_hyperdrive.AllPartialModels;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity.HyperdriveStateMachine.Phase;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity.HyperdriveStateMachine.ShulkerStatus;
import com.github.raspberry1111.create_hyperdrive.utility.MathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HyperdriveRenderer extends SafeBlockEntityRenderer<HyperdriveBlockEntity> {
    private final RandomSource random = RandomSource.create();
    private PartialModel previousHeadModel = AllPartialModels.SHULKER_HEAD_NORMAL;

    public HyperdriveRenderer(final BlockEntityRendererProvider.Context ignored) {
    }

    public static boolean shouldHeadMoveSlow(final Phase phase) {
        return switch (phase) {
            case final Phase.Cooldown ignored -> true;
            case Phase.Charging(
                    final ShulkerStatus shulkerStatus
            ) when shulkerStatus == ShulkerStatus.EXHAUSTED -> true;
            default -> false;
        };
    }

    public static boolean shouldHeadMoveFast(final Phase phase) {
        return phase instanceof Phase.Charging(final ShulkerStatus status) && status == ShulkerStatus.INFUSED;
    }

    public static @Nullable PartialModel getHeadModel(final Phase phase) {
        if (phase instanceof Phase.Cooldown) {
            return AllPartialModels.SHULKER_HEAD_COOLDOWN;
        }

        if (phase instanceof Phase.Charging(final ShulkerStatus status)) {
            return switch (status) {
                case EXHAUSTED -> AllPartialModels.SHULKER_HEAD_EXHAUSTED;
                case NORMAL -> AllPartialModels.SHULKER_HEAD_NORMAL;
                case INFUSED -> AllPartialModels.SHULKER_HEAD_INFUSED;
            };
        }

        return null; // for active, the renderer will cache the previous state
    }

    public static float getHeadRotationDegrees(final Vec3 center, final Vector3f upNormal, final Vector3f forwardNormal, @Nullable final ClientSubLevelAccess sublevel, final float partialTick, final RandomSource random, final Phase phase, final LerpedFloat chaser) {
        final Vec3 pos;
        Vector3f up = new Vector3f(upNormal);
        Vector3f forward = new Vector3f(forwardNormal);
        if (sublevel != null) {
            // apply the sable rotation onto the normals
            final Vector3dc upd = sublevel.renderPose().transformNormal(new Vector3d(up.x, up.y, up.z)).normalize();
            final Vector3dc forwardd = sublevel.renderPose().transformNormal(new Vector3d(forward.x, forward.y, forward.z)).normalize();

            up = new Vector3f((float) upd.x(), (float) upd.y(), (float) upd.z());
            forward = new Vector3f((float) forwardd.x(), (float) forwardd.y(), (float) forwardd.z());
            pos = sublevel.logicalPose().transformPosition(center);
        } else {
            pos = center;
        }

        MathHelper.projectOntoPlane(forward, up);

        final var player = Minecraft.getInstance().player;
        final var target = player == null ? null : player.getEyePosition();

        final float deg;
        if (target != null && target.distanceToSqr(pos) < 75.0) {
            final Vector3f toTarget = new Vector3f(
                    (float) (target.x - pos.x),
                    (float) (target.y - pos.y),
                    (float) (target.z - pos.z)
            ).normalize();
            MathHelper.projectOntoPlane(toTarget, up);

            final double angle = Math.atan2(
                    up.dot(new Vector3f(forward).cross(toTarget)),
                    forward.dot(toTarget)
            );

            deg = AngleHelper.deg(angle);
        } else {
            deg = chaser.getChaseTarget();
        }


        float speed = 0.5f;
        float maxSpeed = 5f;
        float randomNoise = 0;

        if (HyperdriveRenderer.shouldHeadMoveSlow(phase)) {
            speed = 0.2f;
            maxSpeed = 1f;
        } else if (HyperdriveRenderer.shouldHeadMoveFast(phase)) {
            speed = 1f;
            maxSpeed = 20f;

            if (AllConfigs.client().jittering.get() && random.nextFloat() > 0.90) {
                randomNoise += (3 - random.nextFloat() * 6); // goes from -3 to 3
            }
        }

        chaser.chase(deg, speed, LerpedFloat.Chaser.exp(maxSpeed));
        return chaser.getValue(partialTick) + randomNoise;
    }

    public static Vector3f getHeadDisplacement(final float openProgress) {
        return new Vector3f(0, Math.abs(openProgress / 4), 0);
    }

    public static float getLidRotationRads(final Phase phase, final float openProgress) {
        if (phase instanceof Phase.Active) {
            return 0;
        } else {
            return (float) Math.PI * 2 * AllConfigs.client().rotations.get() * openProgress;
        }
    }

    public static Vector3f getLidDisplacement(final float openProgress) {
        return new Vector3f(0, Math.abs(openProgress), 0);
    }

    @Override
    protected void renderSafe(final HyperdriveBlockEntity be, final float partialTick, final PoseStack ms, final MultiBufferSource bufferSource, final int light, final int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        final BlockState blockState = be.getBlockState();

        final var headModel = getHeadModel(be.getPhase());
        if (headModel != null) {
            previousHeadModel = headModel;
        }

        SuperByteBuffer headBuffer = CachedBuffers.partial(previousHeadModel, blockState);
        SuperByteBuffer lidBuffer = CachedBuffers.partial(AllPartialModels.HYPERDRIVE_LID, blockState);
        final SuperByteBuffer shaftBuffer = CachedBuffers.partial(AllPartialModels.TINY_SHAFT, blockState);

        final VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutoutMipped());

        final Direction direction = be.getBlockState().getValue(FACING);
        final Direction oppositeDirection = direction.getOpposite();

        final Quaternionf rotationQuat = new Quaternionf().rotateTo(
                Direction.UP.getStepX(), Direction.UP.getStepY(), Direction.UP.getStepZ(),
                direction.getStepX(), direction.getStepY(), direction.getStepZ()
        );

        final Vector3f up = rotationQuat.transform(new Vector3f(0, 1, 0));
        final Vector3f forward = rotationQuat.transform(new Vector3f(0, 0, -1));

        headBuffer = headBuffer
                .rotateCentered(rotationQuat);
        lidBuffer = lidBuffer.rotateCentered(rotationQuat).rotateYCenteredDegrees(switch (direction) {
            case EAST -> -90;
            case SOUTH -> -180;
            case WEST -> -270;
            default -> 0;
        });

        final Phase phase = be.getPhase();
        final float openProgress = be.getOpenProgress(be.getCurrentProgress(), partialTick);


        headBuffer
                .rotateYCenteredDegrees(getHeadRotationDegrees(
                        be.getBlockPos().getCenter(),
                        up,
                        forward,
                        SableCompanion.INSTANCE.getContainingClient(be),
                        partialTick, random, phase, be.headAngle)
                )
                .translate(getHeadDisplacement(openProgress))
                .light(light)
                .renderInto(ms, cutout);

        lidBuffer
                .rotateYCentered(getLidRotationRads(phase, openProgress))
                .translate(getLidDisplacement(openProgress))
                .light(light).renderInto(ms, cutout);

        KineticBlockEntityRenderer.standardKineticRotationTransform(shaftBuffer, be, light).rotateCentered(new Quaternionf().rotateTo(
                Direction.SOUTH.getStepX(), Direction.SOUTH.getStepY(), Direction.SOUTH.getStepZ(),
                oppositeDirection.getStepX(), oppositeDirection.getStepY(), oppositeDirection.getStepZ()
        )).light(light).renderInto(ms, cutout);

    }
}
