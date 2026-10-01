package com.feb.moregrid.component.rotary;

import com.feb.moregrid.MoreGrid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RotarySwitchClientHandler {
    private static final float PIVOT = 1.5F / 16.0F;

    private static final PartialModel KNOB = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "component/rotary_switch_knob")
    );

    public static void init() {
    }

    public static void openScreen(CircuitBoardBlockEntity be, PlacedComponent placed) {
        RotaryLayout layout = placed.get(RotarySwitchComponent.LAYOUT);
        ValueSettingsBoard board = new ValueSettingsBoard(
                Component.translatable("moregrid.gui.rotary_switch.title"),
                layout.detents() - 1,
                1,
                List.of(Component.translatable("moregrid.gui.rotary_switch.position")),
                new ValueSettingsFormatter(settings -> Component.literal(
                        RotarySwitchComponent.positionName(placed, settings.value())
                ))
        );
        ValueSettings initial = new ValueSettings(0, RotarySwitchComponent.position(placed));
        CustomValueSettingsScreen.beginInteraction(() -> new RotarySwitchScreen(be, placed, board, initial));
    }

    public static void renderKnob(BlockState state, float angle, PoseStack poseStack, MultiBufferSource buffers, int light) {
        CachedBuffers.partial(KNOB, state)
                .translate(PIVOT, 0.0F, PIVOT)
                .rotateYDegrees(angle)
                .translate(-PIVOT, 0.0F, -PIVOT)
                .light(light)
                .renderInto(poseStack, buffers.getBuffer(RenderType.solid()));
    }
}
