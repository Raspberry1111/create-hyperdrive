package com.github.raspberry1111.create_hyperdrive.configs;

import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

public class CStress extends ConfigBase {
    private static final Object2DoubleMap<ResourceLocation> DEFAULT_IMPACTS = new Object2DoubleOpenHashMap<>();

    protected final Map<ResourceLocation, ModConfigSpec.ConfigValue<Double>> impacts = new HashMap<>();

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double value) {
        return builder -> {
            DEFAULT_IMPACTS.put(
                    ResourceLocation.fromNamespaceAndPath(CreateHyperdrive.MODID, builder.getName()),
                    value
            );
            return builder;
        };
    }

    @Override
    public void registerAll(ModConfigSpec.Builder builder) {
        builder.comment(".", Comments.su, Comments.impact)
                .push("impact");
        DEFAULT_IMPACTS.forEach((id, value) -> this.impacts.put(id, builder.define(id.getPath(), value)));

        builder.pop();
    }

    @Nullable
    public DoubleSupplier getImpact(Block block) {
        ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(block);
        ModConfigSpec.ConfigValue<Double> value = this.impacts.get(id);
        return value == null ? null : value::get;
    }

    @Override
    public @NotNull String getName() {
        return "stress";
    }

    private static class Comments {
        static final String su = "[in Stress Units]";
        static final String impact =
                "Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives.";
    }
}
