package com.github.raspberry1111.hyperdrive.utility;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

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
}
