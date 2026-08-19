package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MoreGrid.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ANNUNCIATOR =
            SOUNDS.register("annunciator", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "annunciator")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ANNUNCIATOR_END =
            SOUNDS.register("annunciator_end", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "annunciator_end")));
}