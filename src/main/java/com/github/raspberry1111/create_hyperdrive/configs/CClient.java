package com.github.raspberry1111.create_hyperdrive.configs;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CClient extends ConfigBase {

    public final ConfigGroup client = group(0, "client",
            Comments.client);

    public final ConfigBool jittering = b(true, "enableJittering", Comments.jittering);
    public final ConfigInt rotations = i(4, 0, Integer.MAX_VALUE, "rotations", Comments.rotations);

    @Override
    public String getName() {
        return "client";
    }

    private static class Comments {
        static final String client = "Client-side configs. These do not affect gameplay and are only used for visual and audio effects.";
        static final String jittering = "Enables jittering on some of the hyperdrive components during running";
        static final String rotations = "How many rotations should the hyperdrive lid take before it triggers (does not affect the time taken)";
    }
}
