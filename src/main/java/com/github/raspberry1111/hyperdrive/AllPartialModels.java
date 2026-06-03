package com.github.raspberry1111.hyperdrive;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class AllPartialModels {
    public static final PartialModel
            SHULKER_HEAD_NORMAL = block("hyperdrive/shulker_head/normal");


    private static PartialModel block(String path) {
        return PartialModel.of(Hyperdrive.asResource("block/" + path));
    }

    public static void register() {
    }
}
