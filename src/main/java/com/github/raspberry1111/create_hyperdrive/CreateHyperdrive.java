package com.github.raspberry1111.create_hyperdrive;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import dev.egg.registries.BlockEntityRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreateHyperdrive.MODID)
public class CreateHyperdrive {
    public static final String MODID = "create_hyperdrive";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_CREATIVE_TAB = CREATIVE_MODE_TABS.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create.base"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)

                    .build());

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ModelTemplate EMPTY_SHULKER_TEMPLATE = new ModelTemplate(
            Optional.empty(),
            Optional.of("_empty")
    );

    static {
        REGISTRATE.setCreativeTab(BASE_CREATIVE_TAB);
    }

    public CreateHyperdrive(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        AllBlocks.register();
        AllBlockEntityTypes.register();
        AllPartialModels.register();
        AllItems.register();
        AllDataComponents.register(modEventBus);
        AllConfigs.register(modLoadingContext, modContainer);

        BlockEntityRegistry.PublishCompoundPosFixer(MODID, Set.of("hyperdrive"), Set.of("Source"));
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
