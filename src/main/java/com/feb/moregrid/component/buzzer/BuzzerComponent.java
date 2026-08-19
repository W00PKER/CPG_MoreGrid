package com.feb.moregrid.component.buzzer;

import com.feb.moregrid.MoreGrid;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;


public final class BuzzerComponent extends OrientableComponent {

    public static final FloatProperty RATED_VOLTAGE = new FloatProperty(
            MoreGrid.MOD_ID,
            "buzzer_rated_voltage",
            12.0F,
            6.0F,
            24.0F
    );

    public BuzzerComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if (placed.isClient()) {
            BuzzerClientHandler.tickSound(placed);
        }
        return true;
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RATED_VOLTAGE);
        properties.add(power(2.0F));
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        float voltage = placed.get(RATED_VOLTAGE);
        float resistance = voltage * voltage;
        ElectricWire wire = builder.connect(
                resistance,
                builder.terminalNode(0),
                builder.terminalNode(1)
        );
        placed.add(wire);

        thermals.builder()
                .setThermalMass(0.01F)
                .setMaxPower(1.5F, 125.0F)
                .addHeatSource(wire);
    }

    public static float getVolume(PlacedComponent placed) {
        if (placed.wires.isEmpty()) return 0;
        ElectricWire wire = (ElectricWire) placed.wires.getFirst();
        if (!wire.isConverged()) return 0;
        double current = Math.abs(wire.current());
        if (current < 0.02) return 0;
        float ratedVoltage = placed.get(RATED_VOLTAGE);
        double ratedCurrent = 2.0 / ratedVoltage;
        return (float) Math.min(current / ratedCurrent, 1.0);
    }

    public static float getPitch(PlacedComponent placed) {
        if (placed.wires.isEmpty()) return 1.0F;
        ElectricWire wire = (ElectricWire) placed.wires.getFirst();
        double current = Math.abs(wire.current());
        return (float) Math.clamp(0.8 + current * 0.4, 0.8, 1.4);
    }
}