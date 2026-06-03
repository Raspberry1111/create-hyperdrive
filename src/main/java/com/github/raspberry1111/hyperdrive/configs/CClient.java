package com.github.raspberry1111.hyperdrive.configs;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CClient extends ConfigBase {

    public final ConfigGroup client = group(0, "client",
            Comments.client);

    public final ConfigBool tooltips = b(true, "enableTooltips",
            "Test settings for client-side configs");

    @Override
    public @NotNull String getName() {
        return "client";
    }

    private static class Comments {
        static String client = "Client-side configs. These do not affect gameplay and are only used for visual and audio effects.";
    }
}
