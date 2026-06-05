package com.github.raspberry1111.create_hyperdrive.configs;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CClient extends ConfigBase {

    public final ConfigGroup client = group(0, "client",
            Comments.client);

    public final ConfigBool jittering = b(true, "enableJittering", Comments.jittering);
    public final ConfigInt rotations = i(4, 0, Integer.MAX_VALUE, "rotations", Comments.rotations);

    @Override
    public @NotNull String getName() {
        return "client";
    }

    private static class Comments {
        static String client = "Client-side configs. These do not affect gameplay and are only used for visual and audio effects.";
        static String jittering = "Enables jittering on some of the hyperdrive components during running";
        static String rotations = "How many rotations should the hyperdrive lid take before it triggers (does not affect the time taken)";
    }
}
