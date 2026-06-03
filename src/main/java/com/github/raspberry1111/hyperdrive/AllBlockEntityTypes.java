package com.github.raspberry1111.hyperdrive;

import com.github.raspberry1111.hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity;
import com.github.raspberry1111.hyperdrive.blocks.hyperdrive.HyperdriveRenderer;
import com.github.raspberry1111.hyperdrive.blocks.hyperdrive.HyperdriveVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllBlockEntityTypes {
    public static final BlockEntityEntry<HyperdriveBlockEntity> HYPERDRIVE = Hyperdrive.REGISTRATE.blockEntity("hyperdrive", HyperdriveBlockEntity::new)
            .visual(() -> HyperdriveVisual::new, false)
            .renderer(() -> HyperdriveRenderer::new)
            .validBlocks(AllBlocks.HYPERDRIVE)
            .register();

    public static void register() {
    }
}
