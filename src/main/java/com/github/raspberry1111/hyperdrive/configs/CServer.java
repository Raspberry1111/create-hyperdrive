package com.github.raspberry1111.hyperdrive.configs;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CServer extends ConfigBase {
    public final ConfigGroup server = group(0, "server",
            Comments.server);

    public final ConfigInt chargeTicks = i(50, "charge ticks",
            Comments.chargeTicks);

    public final ConfigInt cooldownTicks = i(2400, "cooldown ticks",
            Comments.cooldownTicks);
    public final ConfigFloat exhaustionMultiplier = f(0.5f, 0.0f, 1.0f, "exhaustion multiplier", Comments.exhautionMultiplier);
    public final ConfigFloat infusionMultiplier = f(2.0f, 1.0f, "infusion multiplier", Comments.exhautionMultiplier);

    public final CStress stressValues = nested(0, CStress::new, Comments.stress);

    @Override
    public @NotNull String getName() {
        return "server";
    }

    private static class Comments {
        static String server = "Server-side configs. These affect gameplay and are used for configuring the behavior of blocks and items";
        static String stress = "Fine tune the kinetic stats of individual components";
        static String chargeTicks = "The number of ticks it takes for the hyperdrive to trigger at 256 rpm";
        static String cooldownTicks = "The number of ticks it takes for the hyperdrive to be ready to use again after triggering";
        static String exhautionMultiplier = "Changes how long the shulker takes to charge when exhausted. 0.5 means that the charge time is doubled when the shulker is exhausted";
        static String infusionMultiplier = "Changes how long the shulker takes to charge when infused. 2.0 means that the charge time is halved when the shulker is infused";
    }
}
