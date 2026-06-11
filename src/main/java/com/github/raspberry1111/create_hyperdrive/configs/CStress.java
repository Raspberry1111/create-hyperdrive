package com.github.raspberry1111.create_hyperdrive.configs;

import com.github.raspberry1111.create_hyperdrive.CreateHyperdrive;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CStress extends ConfigBase {
    private static final Object2DoubleMap<ResourceLocation> DEFAULT_IMPACTS = new Object2DoubleOpenHashMap<>();

    protected final Map<ResourceLocation, ModConfigSpec.ConfigValue<Double>> impacts = new HashMap<>();

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(final double value) {
        return builder -> {
            DEFAULT_IMPACTS.put(
                    ResourceLocation.fromNamespaceAndPath(CreateHyperdrive.MODID, builder.getName()),
                    value
            );
            return builder;
        };
    }

    @Override
    public void registerAll(final ModConfigSpec.Builder builder) {
        builder.comment(".", Comments.su, Comments.impact)
                .push("impact");
        DEFAULT_IMPACTS.forEach((id, value) -> this.impacts.put(id, builder.define(id.getPath(), value)));

        builder.pop();
    }


    public @Nullable DoubleSupplier getImpact(final Block block) {
        final ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(block);
        final ModConfigSpec.ConfigValue<Double> value = this.impacts.get(id);
        return value == null ? null : value::get;
    }

    @Override
    public String getName() {
        return "stress";
    }

    private static class Comments {
        static final String su = "[in Stress Units]";
        static final String impact =
                "Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives.";
    }
}
