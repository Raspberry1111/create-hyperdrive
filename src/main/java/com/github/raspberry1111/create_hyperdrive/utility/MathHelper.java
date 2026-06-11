package com.github.raspberry1111.create_hyperdrive.utility;

import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import org.joml.Vector3d;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MathHelper {

    /**
     * Projects {@code v} onto the plane orthogonal to {@code normal}
     * <p>
     * Stores result in {@code v}
     *
     * @param v      the vector to project
     * @param normal the unit normal of the plane
     * @return the projected vector
     */
    public static Vector3f projectOntoPlane(final Vector3f v, final Vector3f normal) {
        return projectOntoPlane(v, normal, v);
    }

    /**
     * Projects {@code v} onto the plane orthogonal to {@code normal}
     * <p>
     * Stores result in {@code dest}
     *
     * @param v      the vector to project
     * @param normal the unit normal of the plane
     * @param dest   where the result is stored
     * @return the projected vector
     */
    public static Vector3f projectOntoPlane(final Vector3f v, final Vector3f normal, final Vector3f dest) {
        final float dot = v.dot(normal);
        return dest.set(
                v.x - normal.x * dot,
                v.y - normal.y * dot,
                v.z - normal.z * dot
        );
    }

    public static boolean subLevelChainIntersectsAny(final SubLevel baseSubLevel, final ServerLevel targetLevel, final Collection<ResourceLocation> allowList, final Vector3d shift) {
        final Collection<SubLevel> subLevels = SubLevelHelper.getConnectedChain(baseSubLevel);

        for (final SubLevel subLevel : subLevels) {
            final BoundingBox3dc boundingBox = subLevel.boundingBox();
            final BoundingBox3d transformedBoundingBox = boundingBox.move(shift.x, shift.y, shift.z, new BoundingBox3d());

            if (transformedBoundingBox.minY() < targetLevel.getMinBuildHeight()) {
                CreateHyperdrive.LOGGER.info("[subLevelChainIntersectsAny] below void");
                return true;
            }

            final WorldBorder worldBorder = targetLevel.getWorldBorder();
            if (!worldBorder.isWithinBounds(transformedBoundingBox.toMojang())) {
                CreateHyperdrive.LOGGER.info("[subLevelChainIntersectsAny] not inside world border");
                return true;
            }

            if (boundingBoxIntersectsBlocks(transformedBoundingBox, targetLevel, allowList)) {
                CreateHyperdrive.LOGGER.info("[subLevelChainIntersectsAny] intersecting blocks");
                return true;
            }
            if (boundingBoxIntersectsSubLevels(transformedBoundingBox, targetLevel)) {
                CreateHyperdrive.LOGGER.info("[subLevelChainIntersectsAny] intersecting sublevels");
                return true;
            }
        }

        return false;
    }

    public static boolean boundingBoxIntersectsBlocks(final BoundingBox3dc boundingBox, final ServerLevel level, final Collection<ResourceLocation> allowList) {
        final BoundingBox3i chunkBounds = boundingBox.chunkBoundsFrom();
        for (int cx = chunkBounds.minX(); cx < chunkBounds.maxX(); cx++) {
            for (int cz = chunkBounds.minZ(); cz < chunkBounds.maxZ(); cz++) {
                level.getChunk(cx, cz); //make sure all chunks are generated
            }
        }

        final Iterable<BlockPos> stream = BlockPos.betweenClosed(
                Mth.floor(boundingBox.minX()), Mth.floor(boundingBox.minY()), Mth.floor(boundingBox.minZ()),
                Mth.floor(boundingBox.maxX()), Mth.floor(boundingBox.maxY()), Mth.floor(boundingBox.maxZ())
        );


        for (final BlockPos position : stream) {
            final BlockState state = level.getBlockState(position);

            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (!allowList.contains(id)) {
                CreateHyperdrive.LOGGER.debug("[boundingBoxIntersectsBlocks] intersecting {}", id);
                return true;
            }
        }

        return false;
    }

    public static boolean boundingBoxIntersectsSubLevels(final BoundingBox3dc boundingBox, final ServerLevel level) {
        final ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);

        if (container == null) {
            return false;
        }

        final List<ServerSubLevel> subLevels = container.getAllSubLevels();
        for (final ServerSubLevel subLevel : subLevels) {
            if (boundingBox.intersects(subLevel.boundingBox())) { // todo: maybe use OBB collision detection here
                return true;
            }
        }
        return false;
    }


}
