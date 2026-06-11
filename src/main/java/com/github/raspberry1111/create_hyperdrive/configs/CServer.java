package com.github.raspberry1111.create_hyperdrive.configs;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CServer extends ConfigBase {
    public final ConfigGroup server = group(0, "hyperdrive",
            Comments.server);

    public final ConfigBool continousChecking = b(true, "continous checking", Comments.continousChecking);

    public final ConfigInt chargeTicks = i(50, "charge ticks",
            Comments.chargeTicks);

    public final ConfigInt cooldownTicks = i(20 * 60 * 3, "cooldown ticks",
            Comments.cooldownTicks);
    public final ConfigFloat exhaustionMultiplier = f(0.5f, 0.0f, 5.0f, "exhaustion multiplier", Comments.exhaustionMultiplier);
    public final ConfigFloat infusionMultiplier = f(2.0f, 0.0f, "infusion multiplier", Comments.infusionMultiplier);
    public final ConfigInt minimumRPM = i(32, 1, 256, "minimum rpm", "The minimum rpm required for the hyperdrive to charge");

    public final CStress stressValues = nested(0, CStress::new, Comments.stress);
    public final ConfigFloat failedTeleportMultiplier = f(0.25f, 0.0f, 5.0f, "failed teleport multiplier", Comments.failedTeleportMultiplier);

    @Override
    public @NotNull String getName() {
        return "server";
    }

    private static class Comments {
        static final String server = "Change the behaviour of the hyperdrive";
        static final String stress = "Fine tune the kinetic stats of individual components";
        static final String chargeTicks = "The number of ticks it takes for the hyperdrive to trigger at 256 rpm";
        static final String cooldownTicks = "The number of ticks it takes for the hyperdrive to be ready to use again after triggering";
        static final String exhaustionMultiplier = "Changes how long the shulker takes to charge when exhausted. 0.5 means that the charge time is doubled when the shulker is exhausted";
        static final String infusionMultiplier = "Changes how long the shulker takes to charge when infused. 2.0 means that the charge time is halved when the shulker is infused";
        static final String continousChecking = "Continuously check if the hyperdrive can teleport and stop charging if it cannot";
        static final String failedTeleportMultiplier = "How much to multiplier the cooldown by if the previous teleport failed";
    }
}
