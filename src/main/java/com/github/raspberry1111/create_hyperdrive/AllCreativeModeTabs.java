package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.github.raspberry1111.create_hyperdrive.CreateHyperdrive.REGISTRATE;

public class AllCreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateHyperdrive.MODID);


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = REGISTER.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CreateHyperdrive.MODID))
                    .withTabsBefore(com.simibubi.create.AllCreativeModeTabs.BASE_CREATIVE_TAB.getId())
                    .icon(HyperdriveBlockItem::emptyStack)
                    .displayItems((params, output) -> {
                        output.accept(HyperdriveBlockItem.emptyStack(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        output.accept(HyperdriveBlockItem.filledStack(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    })
                    .build());


    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
