package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
        modid = CreateHyperdrive.MODID,
        value = Dist.CLIENT
)
public class CreateHyperdriveClient {
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(CreateHyperdriveClient::clientSetup);
    }

    @SubscribeEvent
    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(AllBlocks.HYPERDRIVE.asItem(), CreateHyperdrive.asResource("shulker_model"), (stack, level, entity, seed) -> HyperdriveBlockItem.getShulkerProperty(stack));
        });
    }
}
