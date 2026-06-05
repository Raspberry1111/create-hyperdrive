package com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive;

import com.github.raspberry1111.create_hyperdrive.AllPartialModels;
import com.github.raspberry1111.create_hyperdrive.AllConfigs;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class HyperdriveStateMachine {
    public static final int LAZY_TICK_RATE = 20;
    public static final int ACTIVE_TICKS = 1;
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
        return AllConfigs.server().chargeTicks.get() * 256; // the chargeTicks represents at 256 rpm
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

    public void moveTowardsZero() {
        if (Math.abs(currentProgress) < 8) {
            currentProgress = 0;
        } else if (currentProgress < 0) {
            currentProgress += 16;
        } else {
            currentProgress -= 16;
        }
    }

    private void tickCharging() {
        int work = (int) Math.round(speedSupplier.get() * shulkerStatus.chargeSpeedMultiplier());
        currentProgress += work;

        if (Math.abs(currentProgress) >= targetChargeProgress()) {
            currentProgress = 0;
            phase = Phase.ACTIVE;

            shulkerStatus = ShulkerStatus.NORMAL;
        }
    }

    private void tickActive() {
        currentProgress += 1;

        if (currentProgress >= ACTIVE_TICKS) {
            currentProgress = 0;
            phase = Phase.COOLDOWN;

            onTrigger.run(); // this needs to run after we change the phase or whenever sable teleports the contraption it will trigger again
        }
    }

    private void tickCooldown() {
        currentProgress += LAZY_TICK_RATE;

        if (currentProgress >= targetCooldownProgress()) {
            endCooldown();
        }
    }

    private void endCooldown() {
        currentProgress = 0;
        phase = Phase.CHARGING;
    }

    public boolean infuse() {
        switch (phase) {
            case COOLDOWN -> {
                endCooldown();

                shulkerStatus = HyperdriveStateMachine.ShulkerStatus.EXHAUSTED;
                return true;
            }
            case CHARGING -> {
                if (shulkerStatus == ShulkerStatus.EXHAUSTED) {
                    shulkerStatus = ShulkerStatus.NORMAL;
                    return true;
                }

                if (shulkerStatus == ShulkerStatus.NORMAL) {
                    shulkerStatus = ShulkerStatus.INFUSED;
                    return true;
                }

                return false;
            }
            case ACTIVE -> {
            }
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public PartialModel getShulkerHeadModel() {
        if (shulkerStatus == ShulkerStatus.EXHAUSTED) {
            return AllPartialModels.SHULKER_HEAD_EXHAUSTED;
        }
        if (shulkerStatus == ShulkerStatus.INFUSED) {
            return AllPartialModels.SHULKER_HEAD_INFUSED;
        }
        if (phase == Phase.COOLDOWN) {
            return AllPartialModels.SHULKER_HEAD_COOLDOWN;
        }
        return AllPartialModels.SHULKER_HEAD_NORMAL;
    }

    public boolean shouldMoveSlow() {
        return phase == Phase.COOLDOWN || shulkerStatus == ShulkerStatus.EXHAUSTED;
    }

    public boolean shouldMoveFast() {
        return shulkerStatus == ShulkerStatus.INFUSED;
    }

    public enum ShulkerStatus implements StringRepresentable {
        EXHAUSTED("exhausted"),
        NORMAL("normal"),
        INFUSED("infusted");

        private final String serializedName;

        ShulkerStatus(String serializedName) {
            this.serializedName = serializedName;
        }

        public double chargeSpeedMultiplier() {
            return switch (this) {
                case EXHAUSTED -> AllConfigs.server().exhaustionMultiplier.get();
                case INFUSED -> AllConfigs.server().infusionMultiplier.get();
                default -> 1.0;
            };
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }

    public enum Phase implements StringRepresentable {
        CHARGING("charging"), ACTIVE("active"), COOLDOWN("cooldown");


        private final String serializedName;

        Phase(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }
}
