package com.github.raspberry1111.hyperdrive.blocks;

import com.github.raspberry1111.hyperdrive.Hyperdrive;
import com.github.raspberry1111.hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity;
import com.github.raspberry1111.hyperdrive.blocks.hyperdrive.HyperdriveVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllBlockEntityTypes {
    public static final BlockEntityEntry<HyperdriveBlockEntity> HYPERDRIVE = Hyperdrive.REGISTRATE.blockEntity("hyperdrive", HyperdriveBlockEntity::new)
            .visual(() -> HyperdriveVisual::new, false)
            .validBlocks(AllBlocks.HYPERDRIVE)
            .register();

    public static void register() {
    }
}
