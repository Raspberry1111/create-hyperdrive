package com.github.raspberry1111.create_hyperdrive;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;


public class AllSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateHyperdrive.MODID);
    private static final List<Pair<String, String>> translations = new ArrayList<>();
    public static final DeferredHolder<SoundEvent, SoundEvent>
            HYPERDRIVE_ACTIVATE_SUCCEEDED = register("hyperdrive_activate_succeeded", "Hyperdrive Succeeded"),
            HYPERDRIVE_ACTIVATE_FAILED = register("hyperdrive_activate_failed", "Hyperdrive Failed");

    public static DeferredHolder<SoundEvent, SoundEvent> register(String name, String translation) {
        translations.add(new Pair<>(name, translation));
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        CreateHyperdrive.asResource(name)
                )
        );
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        for (final Pair<String, String> translation : translations) {
            consumer.accept("subtitle." + CreateHyperdrive.MODID + "." + translation.getA(), translation.getB());
        }
    }
}
