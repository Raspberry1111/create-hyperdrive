package com.github.raspberry1111.hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.hyperdrive.Hyperdrive;
import com.github.raspberry1111.hyperdrive.configs.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jline.utils.Log;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HyperdriveStateMachine {
    public static final int LAZY_TICK_RATE = 20;
    public static final int ACTIVE_TICKS = 2;
    private final Supplier<Float> speedSupplier;
    private final Runnable onTrigger;

    public Phase phase = Phase.CHARGING;
    public ShulkerStatus shulkerStatus = ShulkerStatus.NORMAL;

    int currentProgress = 0;

    HyperdriveStateMachine(Supplier<Float> speedSupplier, Runnable onTrigger) {
        this.speedSupplier = speedSupplier;
        this.onTrigger = onTrigger;
    }

    public static int targetChargeProgress() {
        return AllConfigs.server().chargeTicks.get() * 256; // the charge represent at 256 rpm
    }

    public static int targetCooldownProgress() {
        return AllConfigs.server().cooldownTicks.get();
    }

    public void tick() {
        switch (phase) {
            case CHARGING -> tickCharging();
            case ACTIVE -> tickActive();
            default -> {
            }
        }
    }

    public void lazyTick() {
        if (phase == Phase.COOLDOWN) {
            tickCooldown();
        }
    }

    private void tickCharging() {
        int work = (int) Math.round(speedSupplier.get() * shulkerStatus.chargeSpeedMultiplier());
        currentProgress += work;

        if (Math.abs(currentProgress) >= targetChargeProgress()) {
            currentProgress = 0;
            phase = Phase.ACTIVE;
        }
    }

    private void tickActive() {
        currentProgress += 1;

        if (currentProgress >= ACTIVE_TICKS) {
            onTrigger.run();

            currentProgress = 0;
            phase = Phase.COOLDOWN;
        }
    }

    private void tickCooldown() {
        Hyperdrive.LOGGER.debug("Target = {}", targetCooldownProgress());
        currentProgress += LAZY_TICK_RATE;

        if (currentProgress >= targetCooldownProgress()) {
            currentProgress = 0;
            phase = Phase.CHARGING;
        }
    }

    public enum ShulkerStatus {
        EXHAUSTED,
        NORMAL,
        INFUSED;


        public double chargeSpeedMultiplier() {
            return switch (this) {
                case EXHAUSTED -> AllConfigs.server().exhaustionMultiplier.get();
                case INFUSED -> AllConfigs.server().infusionMultiplier.get();
                default -> 1.0;
            };
        }
    }

    public enum Phase {
        CHARGING, ACTIVE, COOLDOWN
    }

    public record TickContext(
            HyperdriveBlockEntity be,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
    }
}
