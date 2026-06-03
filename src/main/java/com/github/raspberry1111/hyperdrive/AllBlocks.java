package com.github.raspberry1111.hyperdrive;

import com.github.raspberry1111.hyperdrive.blocks.hyperdrive.HyperdriveBlock;
import com.github.raspberry1111.hyperdrive.configs.CStress;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.material.MapColor;

import static com.github.raspberry1111.hyperdrive.Hyperdrive.REGISTRATE;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class AllBlocks {
    public static final BlockEntry<HyperdriveBlock> HYPERDRIVE = REGISTRATE.block("hyperdrive", HyperdriveBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.noOcclusion().mapColor(MapColor.COLOR_GRAY))
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .transform(CStress.setImpact(16.0))
            .transform(pickaxeOnly())
            .item()
            .transform(customItemModel())
            .register();


    public static void register() {
    }
}
