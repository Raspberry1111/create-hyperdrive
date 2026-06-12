package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockEntity;
import com.mojang.logging.LogUtils;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import dev.egg.registries.BlockEntityRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(CreateHyperdrive.MODID)
public class CreateHyperdrive {
    public static final String MODID = "create_hyperdrive";

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateHyperdrive(final IEventBus modEventBus, final ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);
        final ModLoadingContext modLoadingContext = ModLoadingContext.get();

        modEventBus.addListener(this::commonSetup);

        AllCreativeModeTabs.register(modEventBus);
        REGISTRATE.setCreativeTab(AllCreativeModeTabs.CREATIVE_TAB);


        AllItems.register();
        AllBlocks.register();
        AllBlockEntityTypes.register();
        AllPartialModels.register();
        AllDataComponents.register(modEventBus);
        AllConfigs.register(modContainer);
        AllSounds.register(modEventBus);

        REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            final BiConsumer<String, String> langConsumer = provider::add;

            HyperdriveBlockEntity.TargetDimension.provideLang(langConsumer);
            AllCreativeModeTabs.provideLang(langConsumer);
            AllSounds.provideLang(langConsumer);

            langConsumer.accept(MODID + ".tooltip.hyperdrive.phase", "Phase: ");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.phase.cooldown", "Cooldown");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.phase.charging", "Charging");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.phase.active", "Active");

            langConsumer.accept(MODID + ".tooltip.hyperdrive.shulker_status", "Shulker Status: ");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.shulker_status.exhausted", "Exhausted");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.shulker_status.normal", "Normal");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.shulker_status.infused", "Infused");

            langConsumer.accept(MODID + ".tooltip.hyperdrive.progress", "Progress: ");

            langConsumer.accept(MODID + ".tooltip.hyperdrive.failed", "Unable to Teleport:");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.failed.not_on_sublevel", "Not in a Sublevel!");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.failed.target_dimension", "Target dimension is current dimension!");
            langConsumer.accept(MODID + ".tooltip.hyperdrive.failed.would_collide", "Teleportation would collide with a block!");

            langConsumer.accept(MODID + ".tooltip.hyperdrive.failed_last", "Failed Previous Teleport Attempt:");

        });

        BlockEntityRegistry.PublishCompoundPosFixer(MODID, Set.of("hyperdrive"), Set.of("Source"));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CreateHyperdriveClient.init(modEventBus);
        }
    }

    public static ResourceLocation asResource(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                Mods.JEI.executeIfInstalled(() -> AllRecipes::register)
        );
    }
}
