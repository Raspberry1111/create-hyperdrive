package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType.Builder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class AllDataComponents {
    public static final Codec<HyperdriveBlockEntity.HyperdriveStateMachine.Phase> PHASE_CODEC = Codec.STRING.xmap(HyperdriveBlockEntity.HyperdriveStateMachine.Phase::fromString, HyperdriveBlockEntity.HyperdriveStateMachine.Phase::getSerializedName);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateHyperdrive.MODID);

    public static final DataComponentType<HyperdriveBlockEntity.HyperdriveStateMachine.Phase> PHASE =
            register("phase",
                    builder -> builder.persistent(PHASE_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(PHASE_CODEC)));

    public static final DataComponentType<Integer> CURRENT_PROGRESS =
            register("current_progress",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
