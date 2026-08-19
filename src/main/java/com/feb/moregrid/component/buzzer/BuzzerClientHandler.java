package com.feb.moregrid.component.buzzer;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class BuzzerClientHandler {
    private static final Map<PlacedComponent, Boolean> HAS_SOUND =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void tickSound(PlacedComponent placed) {
        float vol = BuzzerComponent.getVolume(placed);
        Boolean playing = HAS_SOUND.get(placed);
        if (vol > 0 && (playing == null || !playing)) {
            Minecraft.getInstance().getSoundManager()
                    .play(new BuzzerSoundInstance(placed));
            HAS_SOUND.put(placed, true);
        } else if (vol == 0 && playing != null && playing) {
            HAS_SOUND.put(placed, false);
        }
    }
}