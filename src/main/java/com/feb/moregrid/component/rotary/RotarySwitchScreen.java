package com.feb.moregrid.component.rotary;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.UpdateComponentBiPacket;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class RotarySwitchScreen extends CustomValueSettingsScreen {
    private final CircuitBoardBlockEntity be;
    private final PlacedComponent placed;
    private final Consumer<ValueSettings> onSelect;
    private int lastValue;
    private boolean cursorSettled;

    RotarySwitchScreen(
            CircuitBoardBlockEntity be,
            PlacedComponent placed,
            ValueSettingsBoard board,
            ValueSettings initial
    ) {
        super(be.getBlockPos(), board, initial, settings -> select(be, placed, settings.value()));
        this.be = be;
        this.placed = placed;
        this.onSelect = settings -> select(be, placed, settings.value());
        this.lastValue = initial.value();
    }

    @Override
    public void removed() {
        super.removed();
        int current = RotarySwitchComponent.position(placed);
        select(be, placed, placed.get(RotarySwitchComponent.LAYOUT).springReturn(current));
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWindow(graphics, mouseX, mouseY, partialTicks);
        ValueSettings hovered = getClosestCoordinate(mouseX, mouseY);
        if (!cursorSettled) {
            cursorSettled = hovered.value() == lastValue;
            return;
        }
        if (hovered.value() != lastValue) {
            lastValue = hovered.value();
            onSelect.accept(hovered);
        }
    }

    private static void select(CircuitBoardBlockEntity be, PlacedComponent placed, int detent) {
        if (RotarySwitchComponent.position(placed) == detent) {
            return;
        }
        placed.set(RotarySwitchComponent.POSITION, detent);
        placed.stateUpdated();
        ModdedPackets.sendToServer(new UpdateComponentBiPacket(be, placed, RotarySwitchComponent.POSITION));
    }
}
