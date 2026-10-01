package com.feb.moregrid.component.rotary;

import com.feb.moregrid.MoreGrid;
import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.EnumProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.components.properties.StringProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.Map;

public final class RotarySwitchComponent extends OrientableComponent
        implements IInteractableComponent, IRenderedComponent, IComponentGoggleInformation {
    public static final int COMMON_PAD = 0;
    private static final float CONTACT_RESISTANCE = 0.05F;
    private static final float RATED_CURRENT = 16.0F;
    private static final int TRANSITION_TICKS = 3;

    public static final EnumProperty<RotaryLayout> LAYOUT = new EnumProperty<>(
            MoreGrid.MOD_ID,
            "rotary_layout",
            RotaryLayout.class,
            RotaryLayout.values(),
            RotaryLayout.ONE_TWO
    ) {
        @Override
        public String toString(RotaryLayout value) {
            return value.displayName();
        }
    };

    public enum Transition {
        BBM,
        MBB
    }

    public static final EnumProperty<Transition> TRANSITION = new EnumProperty<>(
            MoreGrid.MOD_ID,
            "rotary_transition",
            Transition.class,
            Transition.values(),
            Transition.BBM
    );

    public static final StringProperty POSITION_NAMES = new StringProperty(MoreGrid.MOD_ID, "rotary_position_names");

    public static final IntProperty POSITION = new IntProperty(
            MoreGrid.MOD_ID,
            "rotary_position",
            0,
            0,
            RotaryLayout.MAX_DETENTS - 1
    ).hidden().cast();

    private final Map<RotaryLayout, ComponentFootprint> footprints;

    public RotarySwitchComponent(Map<RotaryLayout, ComponentFootprint> footprints) {
        super(footprints.get(RotaryLayout.ONE_TWO));
        this.footprints = footprints;
    }

    @Override
    public ComponentFootprint footprint(PlacedComponent placed) {
        if (placed == null) {
            return super.footprint(null);
        }
        return footprints.get(placed.get(LAYOUT)).rotated(placed.get(ORIENTATION));
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(LAYOUT);
        properties.add(POSITION_NAMES);
        properties.add(TRANSITION);
        properties.add(POSITION);
        properties.add(current(RATED_CURRENT));
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        RotaryLayout layout = placed.get(LAYOUT);
        int position = position(placed);
        int closed = layout.contactAt(position);
        IElectricNode common = builder.terminalNode(COMMON_PAD);

        ThermalBuilder thermal = thermals.builder()
                .setThermalMass(0.01F)
                .setMaxCurrent(RATED_CURRENT, CONTACT_RESISTANCE, 150.0F);
        for (int throwPad = 1; throwPad <= layout.throwCount(); throwPad++) {
            SwitchedWire contact = builder.connectSwitch(
                    CONTACT_RESISTANCE,
                    common,
                    builder.terminalNode(throwPad),
                    closed == throwPad
            );
            placed.add(contact);
            thermal.addHeatSource(contact);
        }
        placed.customData = new Rotation(position);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if (placed.customData instanceof Rotation rotation
                && rotation.pendingTicks > 0
                && --rotation.pendingTicks == 0) {
            closeOnly(placed, placed.get(LAYOUT).contactAt(rotation.position));
        }
        return true;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        super.stateUpdated(placed);
        if (!(placed.customData instanceof Rotation rotation)) {
            return;
        }
        RotaryLayout layout = placed.get(LAYOUT);
        int target = position(placed);
        if (target != rotation.position) {
            int from = layout.contactAt(rotation.position);
            int to = layout.contactAt(target);
            rotation.position = target;
            rotation.pendingTicks = TRANSITION_TICKS;
            if (placed.get(TRANSITION) == Transition.MBB) {
                closeOnly(placed, from, to);
            } else {
                closeOnly(placed);
            }
        } else if (rotation.pendingTicks == 0) {
            closeOnly(placed, layout.contactAt(target));
        }
    }

    @Override
    public void render(
            CircuitBoardBlockEntity be,
            PlacedComponent placed,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        float angle = placed.get(LAYOUT).knobAt(position(placed)).angle()
                + placed.get(ORIENTATION).ordinal() * 90.0F;
        RotarySwitchClientHandler.renderKnob(be.getBlockState(), angle, poseStack, bufferSource, light);
    }

    @Override
    public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent placed, Player player) {
        placed.onClientWorld(() -> world -> RotarySwitchClientHandler.openScreen(be, placed));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean addToGoggleTooltip(
            @NotNull PlacedComponent placed,
            @NotNull List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        Lang.text("Switch Information:")
                .forGoggles(tooltip);
        Lang.text("Position:")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
        Lang.text(positionName(placed, position(placed)))
                .style(ChatFormatting.AQUA)
                .forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 3.0F / 16.0F);
    }

    public static int position(PlacedComponent placed) {
        return Math.min(placed.get(POSITION), placed.get(LAYOUT).detents() - 1);
    }

    public static String positionName(PlacedComponent placed, int detent) {
        String[] names = placed.get(POSITION_NAMES).split("/", -1);
        if (detent < names.length && !names[detent].isBlank()) {
            return names[detent].trim();
        }
        return placed.get(LAYOUT).label(detent);
    }

    private static void closeOnly(PlacedComponent placed, int... throwsToClose) {
        for (int throwPad = 1; throwPad <= placed.wires.size(); throwPad++) {
            boolean closed = false;
            for (int t : throwsToClose) {
                closed |= t == throwPad;
            }
            ((SwitchedWire) placed.wires.get(throwPad - 1)).setState(closed);
        }
    }

    private static final class Rotation {
        private int position;
        private int pendingTicks;

        private Rotation(int position) {
            this.position = position;
        }
    }
}
