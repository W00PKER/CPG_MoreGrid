package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.sim.TransformerRatedWire;
import com.feb.moregrid.util.MoreGridMath;
import com.feb.moregrid.util.MoreGridMath.TransformerEquivalent;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.Locale;

/**
 * Compact transformer using Power Grid's lumped transformer coupling and native
 * circuit-board thermal overload/destruction system.
 */
public final class TransformerComponent extends OrientableComponent {
    private static final float THERMAL_RATING_WATTS = 120.0F;

    public static final IntProperty PRIMARY_TURNS = new IntProperty(
            MoreGrid.MOD_ID,
            "transformer_primary_turns",
            1,
            1,
            MoreGridMath.TRANSFORMER_MAX_TOTAL_TURNS - 1
    );

    public static final IntProperty SECONDARY_TURNS = new IntProperty(
            MoreGrid.MOD_ID,
            "transformer_secondary_turns",
            1,
            1,
            MoreGridMath.TRANSFORMER_MAX_TOTAL_TURNS - 1
    );

    public static final CalculatedProperty<Integer> TOTAL_TURNS = new CalculatedProperty<>(
            MoreGrid.MOD_ID,
            "transformer_total_turns",
            placed -> placed.get(PRIMARY_TURNS) + placed.get(SECONDARY_TURNS),
            value -> value + " / " + MoreGridMath.TRANSFORMER_MAX_TOTAL_TURNS
    );

    public static final CalculatedProperty<Float> RATIO = new CalculatedProperty<>(
            MoreGrid.MOD_ID,
            "transformer_ratio",
            placed -> MoreGridMath.transformerRatio(placed.get(PRIMARY_TURNS), placed.get(TOTAL_TURNS)),
            value -> String.format(Locale.ROOT, "%.4f", value)
    );

    public TransformerComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(TOTAL_TURNS);
        properties.add(PRIMARY_TURNS);
        properties.add(SECONDARY_TURNS);
        properties.add(RATIO);
        properties.add(power(THERMAL_RATING_WATTS));
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        TransformerEquivalent equivalent = MoreGridMath.transformerEquivalent(
                placed.get(PRIMARY_TURNS),
                placed.get(TOTAL_TURNS)
        );

        FloatingNode primaryInternal = builder.addInternalNode();
        FloatingNode secondaryInternal = builder.addInternalNode();

        IElectricNode primary1 = builder.terminalNode(0);
        IElectricNode primary2 = builder.terminalNode(1);
        IElectricNode secondary1 = builder.terminalNode(2);
        IElectricNode secondary2 = builder.terminalNode(3);

        TransformerRatedWire primaryStray = new TransformerRatedWire(
                equivalent.primaryStrayResistance(),
                primary1,
                primaryInternal,
                primary1,
                primary2,
                secondary1,
                secondary2
        );
        builder.add(primaryStray);

        ElectricWire secondaryStray = builder.connect(
                equivalent.secondaryStrayResistance(),
                secondaryInternal,
                secondary1
        );
        primaryStray.setSecondaryWinding(secondaryStray);

        ElectricWire magnetizingBranch = builder.connect(
                equivalent.magnetizingResistance(),
                primaryInternal,
                primary2
        );

        builder.couple(
                equivalent.ratio(),
                MoreGridMath.MIN_RESISTANCE,
                primaryInternal,
                primary2,
                secondaryInternal,
                secondary2
        );

        placed.add(primaryStray);
        placed.add(secondaryStray);
        placed.add(magnetizingBranch);

        thermals.builder()
                .setThermalMass(0.10F)
                .setMaxPower(THERMAL_RATING_WATTS, 150.0F)
                .addHeatSource(primaryStray);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        int primary = placed.get(PRIMARY_TURNS);
        int secondary = placed.get(SECONDARY_TURNS);
        int max = MoreGridMath.TRANSFORMER_MAX_TOTAL_TURNS;

        if (primary + secondary > max) {
            secondary = max - primary;
            if (secondary < 1) {
                secondary = 1;
                primary = max - 1;
                placed.set(PRIMARY_TURNS, primary);
            }
            placed.set(SECONDARY_TURNS, secondary);
        }
        return true;
    }
}
