package com.github.raspberry1111.create_hyperdrive.mixin;

import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import net.minecraft.client.particle.DragonBreathParticle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DragonBreathParticle.class)
public class DragonBreathParticleMixin implements ParticleSubLevelKickable {
    @Override
    public boolean sable$shouldKickFromTracking() {
        return false;
    }

    @Override
    public boolean sable$shouldCollideWithTrackingSubLevel() {
        return false;
    }
}