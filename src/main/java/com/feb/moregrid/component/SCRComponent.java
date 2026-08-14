package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.sim.SCRWire;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import java.util.List;
import java.util.Locale;

/** Three-terminal, latching, unidirectional silicon-controlled rectifier. */
public final class SCRComponent extends OrientableComponent implements IComponentGoggleInformation {
    private static final float RATED_ANODE_CURRENT = 8.0F;

    public static final FloatProperty TRIGGER_CURRENT = new FloatProperty(
            MoreGrid.MOD_ID,
            "scr_trigger_current",
            0.020F,
            0.001F,
            0.500F
    );

    public static final FloatProperty HOLDING_CURRENT = new FloatProperty(
            MoreGrid.MOD_ID,
            "scr_holding_current",
            0.040F,
            0.001F,
            2.000F
    );

    public static final FloatProperty ON_RESISTANCE = new FloatProperty(
            MoreGrid.MOD_ID,
            "scr_on_resistance",
            0.150F,
            0.010F,
            2.000F
    );

    public static final FloatProperty FORWARD_DROP = new FloatProperty(
            MoreGrid.MOD_ID,
            "scr_forward_drop",
            1.20F,
            0.50F,
            2.00F
    );

    public static final CalculatedProperty<Float> GATE_RESISTANCE = new CalculatedProperty<>(
            MoreGrid.MOD_ID,
            "scr_gate_resistance",
            placed -> gateResistance(placed.get(TRIGGER_CURRENT)),
            value -> String.format(Locale.ROOT, "%.2f Ω", value)
    );

    public static final BooleanProperty STATE = (BooleanProperty) new BooleanProperty(
            MoreGrid.MOD_ID,
            "scr_state"
    ).hidden().cast();

    public SCRComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(TRIGGER_CURRENT);
        properties.add(HOLDING_CURRENT);
        properties.add(ON_RESISTANCE);
        properties.add(FORWARD_DROP);
        properties.add(GATE_RESISTANCE);
        properties.add(STATE);
        properties.add(power(20.0F));
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        float triggerCurrent = placed.get(TRIGGER_CURRENT);
        float holdingCurrent = placed.get(HOLDING_CURRENT);
        float onResistance = placed.get(ON_RESISTANCE);
        float forwardDrop = placed.get(FORWARD_DROP);

        ElectricWire gateWire = builder.connect(
                gateResistance(triggerCurrent),
                builder.terminalNode(2),
                builder.terminalNode(1)
        );

        SCRWire mainWire = new SCRWire(
                builder.terminalNode(0),
                builder.terminalNode(1),
                gateWire,
                placed.get(STATE),
                triggerCurrent,
                holdingCurrent,
                onResistance,
                forwardDrop
        );
        builder.add(mainWire);
        placed.add(mainWire);

        thermals.builder()
                .setThermalMass(0.03F)
                .setMaxCurrent(RATED_ANODE_CURRENT, onResistance, 125.0F)
                .addHeatSource(mainWire);
        thermals.builder()
                .setThermalMass(0.005F)
                .setMaxPower(1.0F, 125.0F)
                .addHeatSource(gateWire);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if (placed.wires.isEmpty()) {
            return true;
        }
        SCRWire wire = (SCRWire) placed.wires.get(0);
        if (wire.wasSwitched()) {
            placed.set(STATE, wire.getState());
        }
        return true;
    }

    @Override
    public boolean addToGoggleTooltip(
            @NotNull PlacedComponent placed,
            @NotNull List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        tooltip.add(Component.translatable(
                placed.get(STATE) ? "moregrid.tooltip.scr.on" : "moregrid.tooltip.scr.off"
        ));
        tooltip.add(Component.translatable(
                "moregrid.tooltip.scr.trigger",
                String.format(Locale.ROOT, "%.1f mA", placed.get(TRIGGER_CURRENT) * 1000.0F)
        ));
        tooltip.add(Component.translatable(
                "moregrid.tooltip.scr.drop",
                String.format(Locale.ROOT, "%.2f V", placed.get(FORWARD_DROP))
        ));
        return true;
    }

    private static float gateResistance(float triggerCurrent) {
        float current = Math.max(0.001F, triggerCurrent);
        return Math.max(1.0F, Math.min(10_000.0F, 0.8F / current));
    }
}
