package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.ItemEntry;

import static com.github.raspberry1111.create_hyperdrive.CreateHyperdrive.REGISTRATE;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class AllItems {
    public static final ItemEntry<HyperdriveBlockItem> EMPTY_HYPERDRIVE =
            REGISTRATE.item("empty_hyperdrive", HyperdriveBlockItem::empty)
                    .model((c, p) ->
                            p.itemTexture(c)
                    )
                    .lang($ -> "item." + CreateHyperdrive.MODID + ".hyperdrive_empty", "Empty Hyperdrive")
                    .register();

    public static void register() {
    }
}
