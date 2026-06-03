package com.github.raspberry1111.hyperdrive;

import com.github.raspberry1111.hyperdrive.configs.CClient;
import com.github.raspberry1111.hyperdrive.configs.CServer;
import com.github.raspberry1111.hyperdrive.configs.CStress;
import com.simibubi.create.api.stress.BlockStressValues;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Hyperdrive.MODID)
public class AllConfigs {
    private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

    private static CServer server;
    private static CClient client;

    public static CServer server() {
        return server;
    }

    public static CClient client() {
        return client;
    }

    private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type side) {
        Pair<T, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void register(ModLoadingContext context, ModContainer container) {
        Hyperdrive.LOGGER.debug("Registering configs");
        client = register(CClient::new, ModConfig.Type.CLIENT);
        server = register(CServer::new, ModConfig.Type.SERVER);

        for (Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet()) {
            Hyperdrive.LOGGER.debug("Registering config {} for side {}", pair.getValue().getName(), pair.getKey());
            container.registerConfig(pair.getKey(), pair.getValue().specification);
        }

        CStress stress = server().stressValues;
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);

        Hyperdrive.LOGGER.debug("Finished registering configs");
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        Hyperdrive.LOGGER.debug("Loading config {}", event.getConfig().getSpec());
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onLoad();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        Hyperdrive.LOGGER.debug("Reloading config {}", event.getConfig().getSpec());
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onReload();
    }
}
