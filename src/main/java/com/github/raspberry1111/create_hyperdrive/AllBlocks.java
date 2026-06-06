package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlock;
import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockItem;
import com.github.raspberry1111.create_hyperdrive.configs.CStress;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.material.MapColor;

import static com.github.raspberry1111.create_hyperdrive.CreateHyperdrive.REGISTRATE;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class AllBlocks {
    public static final BlockEntry<HyperdriveBlock> HYPERDRIVE = REGISTRATE.block("hyperdrive", HyperdriveBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.noOcclusion().mapColor(MapColor.COLOR_GRAY))
            .blockstate((c, p) -> p.directionalBlock(c.get(), state -> {
                if (state.getValue(HyperdriveBlock.HAS_SHULKER)) {
                    return AssetLookup.partialBaseModel(c, p);
                } else {
                    return AssetLookup.partialBaseModel(c, p, "empty");
                }
            }))
            .transform(CStress.setImpact(16.0))
            .transform(pickaxeOnly())
            .item(HyperdriveBlockItem::withShulker)
            .lang($ -> "item." + CreateHyperdrive.MODID + ".hyperdrive", "Hyperdrive")

            .transform(b -> b.model(AssetLookup.itemModel("hyperdrive")).build())
            .register();

    public static void register() {
    }


}
