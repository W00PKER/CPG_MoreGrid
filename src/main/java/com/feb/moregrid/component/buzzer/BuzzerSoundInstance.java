package com.feb.moregrid.component.buzzer;

import com.feb.moregrid.registry.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

@OnlyIn(Dist.CLIENT)
public class BuzzerSoundInstance extends AbstractTickableSoundInstance {
    private final PlacedComponent placed;

    public BuzzerSoundInstance(PlacedComponent placed) {
        super(ModSoundEvents.ANNUNCIATOR.get(), SoundSource.BLOCKS,
                Minecraft.getInstance().level.random);
        this.placed = placed;
        var pos = placed.getPos().getCenter();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.attenuation = Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float vol = BuzzerComponent.getVolume(placed);
        if (vol == 0) {
            stop();
            return;
        }
        volume = vol;
        pitch = BuzzerComponent.getPitch(placed);
    }
}