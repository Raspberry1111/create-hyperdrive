package com.github.raspberry1111.create_hyperdrive.utility;

import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import dev.egg.SubLevelWarper;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

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
    public static Vector3f projectOntoPlane(Vector3f v, Vector3f normal) {
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
    public static Vector3f projectOntoPlane(Vector3f v, Vector3f normal, Vector3f dest) {
        float dot = v.dot(normal);
        return dest.set(
                v.x - normal.x * dot,
                v.y - normal.y * dot,
                v.z - normal.z * dot
        );
    }

    public static double dimensionScale(Level from, Level to) {
        return from.dimensionType().coordinateScale() / to.dimensionType().coordinateScale();
    }

    public static boolean subLevelChainIntersectsAny(SubLevel baseSubLevel, ServerLevel targetLevel, Collection<ResourceLocation> allowList) {
        final Collection<SubLevel> subLevels = SubLevelHelper.getConnectedChain(baseSubLevel);

        double scaleRatio = dimensionScale(baseSubLevel.getLevel(), targetLevel);
        for (SubLevel subLevel : subLevels) {
            final BoundingBox3dc boundingBox = subLevel.boundingBox();
            Vector3d oldCenter = boundingBox.center();
            Vector3d newCenter = boundingBox.center().mul(scaleRatio, 1, scaleRatio);

            final BoundingBox3d transformedBoundingBox = boundingBox.move(newCenter.x - oldCenter.x, newCenter.y - oldCenter.y, newCenter.z - oldCenter.z, new BoundingBox3d());

            if (transformedBoundingBox.minY() < targetLevel.getMinBuildHeight()) {
                CreateHyperdrive.LOGGER.debug("[subLevelChainIntersectsAny] below void");
                return true;
            }

            if (boundingBoxIntersectsBlocks(transformedBoundingBox, targetLevel, allowList)) {
                CreateHyperdrive.LOGGER.debug("[subLevelChainIntersectsAny] intersecting blocks");
                return true;
            }
            if (boundingBoxIntersectsSubLevels(transformedBoundingBox, targetLevel)) {
                CreateHyperdrive.LOGGER.debug("[subLevelChainIntersectsAny] intersecting sublevels");
                return true;
            }
        }

        return false;
    }

    public static boolean boundingBoxIntersectsBlocks(BoundingBox3dc boundingBox, ServerLevel level, Collection<ResourceLocation> allowList) {
        BoundingBox3i chunkBounds = boundingBox.chunkBoundsFrom();
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
            BlockState state = level.getBlockState(position);

            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (!allowList.contains(id)) {
                return true;
            }
        }

        return false;
    }

    public static boolean boundingBoxIntersectsSubLevels(BoundingBox3dc boundingBox, ServerLevel level) {
        ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);

        if (container == null) {
            return false;
        }

        List<ServerSubLevel> subLevels = container.getAllSubLevels();
        for (ServerSubLevel subLevel : subLevels) {
            if (boundingBox.intersects(subLevel.boundingBox())) { // todo: maybe use OBB collision detection here
                return true;
            }
        }
        return false;
    }


}
