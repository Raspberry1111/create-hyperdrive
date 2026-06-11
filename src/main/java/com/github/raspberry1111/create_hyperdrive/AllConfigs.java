package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.configs.CClient;
import com.github.raspberry1111.create_hyperdrive.configs.CServer;
import com.github.raspberry1111.create_hyperdrive.configs.CStress;
import com.simibubi.create.api.stress.BlockStressValues;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateHyperdrive.MODID)
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

    private static <T extends ConfigBase> T register(final Supplier<T> factory, final ModConfig.Type side) {
        final Pair<T, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(builder -> {
            final T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        final T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void register(final ModContainer container) {
        CreateHyperdrive.LOGGER.debug("Registering configs");
        client = register(CClient::new, ModConfig.Type.CLIENT);
        server = register(CServer::new, ModConfig.Type.SERVER);

        for (final Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet()) {
            CreateHyperdrive.LOGGER.debug("Registering config {} for side {}", pair.getValue().getName(), pair.getKey());
            container.registerConfig(pair.getKey(), pair.getValue().specification);
        }

        final CStress stress = server().stressValues;
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);

        CreateHyperdrive.LOGGER.debug("Finished registering configs");
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        CreateHyperdrive.LOGGER.debug("Loading config {}", event.getConfig().getSpec());
        for (final ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onLoad();
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        CreateHyperdrive.LOGGER.debug("Reloading config {}", event.getConfig().getSpec());
        for (final ConfigBase config : CONFIGS.values())
            if (config.specification == event.getConfig()
                    .getSpec())
                config.onReload();
    }
}
