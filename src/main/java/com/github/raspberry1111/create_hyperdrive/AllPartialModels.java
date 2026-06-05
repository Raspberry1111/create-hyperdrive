package com.github.raspberry1111.create_hyperdrive;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class AllPartialModels {
    public static final PartialModel
            SHULKER_HEAD_NORMAL = block("hyperdrive/shulker_head/normal"),
            SHULKER_HEAD_EXHAUSTED = block("hyperdrive/shulker_head/exhausted"),
            SHULKER_HEAD_COOLDOWN = block("hyperdrive/shulker_head/cooldown"),
            SHULKER_HEAD_INFUSED = block("hyperdrive/shulker_head/infused"),

    HYPERDRIVE_LID = block("hyperdrive/lid");


    private static PartialModel block(String path) {
        return PartialModel.of(CreateHyperdrive.asResource("block/" + path));
    }

    public static void register() {
    }
}
