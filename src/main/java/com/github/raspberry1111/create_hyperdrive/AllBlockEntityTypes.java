package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveRenderer;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllBlockEntityTypes {
    public static final BlockEntityEntry<HyperdriveBlockEntity> HYPERDRIVE = CreateHyperdrive.REGISTRATE.blockEntity("hyperdrive", HyperdriveBlockEntity::new)
            .visual(() -> HyperdriveVisual::new, false)
            .renderer(() -> HyperdriveRenderer::new)
            .validBlocks(AllBlocks.HYPERDRIVE)
            .register();

    public static void register() {
    }
}
