package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.sim.TransformerRatedWire;
import com.feb.moregrid.util.MoreGridMath;
import com.feb.moregrid.util.MoreGridMath.TransformerEquivalent;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Compact transformer using Power Grid's lumped transformer coupling and native
 * circuit-board thermal overload/destruction system.
 */
public final class TransformerComponent extends org.patryk3211.powergrid.circuits.components.Component implements IComponentGoggleInformation {
    /** Nameplate apparent-power rating passed directly to Power Grid thermal overload. */
    private static final float THERMAL_RATING_WATTS = 120.0F;

    private record RuntimeState(
            IElectricNode primary1,
            IElectricNode primary2,
            IElectricNode secondary1,
            IElectricNode secondary2,
            TransformerRatedWire primaryWire,
            ElectricWire secondaryWire
    ) {}

    private static final Map<PlacedComponent, RuntimeState> RUNTIME =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static final IntProperty TOTAL_TURNS = new IntProperty(
            MoreGrid.MOD_ID,
            "transformer_total_turns",
            30,
            2,
            MoreGridMath.TRANSFORMER_MAX_TOTAL_TURNS
    );

    public static final IntProperty PRIMARY_TURNS = new IntProperty(
            MoreGrid.MOD_ID,
            "transformer_primary_turns",
            25,
            1,
            MoreGridMath.TRANSFORMER_MAX_TOTAL_TURNS - 1
    );

    public static final CalculatedProperty<Integer> SECONDARY_TURNS = new CalculatedProperty<>(
            MoreGrid.MOD_ID,
            "transformer_secondary_turns",
            placed -> MoreGridMath.secondaryTurns(placed.get(PRIMARY_TURNS), placed.get(TOTAL_TURNS)),
            Object::toString
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

        /*
         * This is a normal electrically connected Power Grid wire. Its only
         * extension is internalPower(), which reports terminal VA to the native
         * ThermalBuilder so the 120 W nameplate rating becomes a real thermal
         * overload rather than a separate custom trip timer.
         */
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
        RUNTIME.put(placed, new RuntimeState(
                primary1,
                primary2,
                secondary1,
                secondary2,
                primaryStray,
                secondaryStray
        ));

        /*
         * Native Power Grid circuit-board overload path:
         * - internalPower() supplies the current transformer load in watts;
         * - setMaxPower defines the 120 W continuous rating;
         * - Power Grid owns temperature integration, cooling, overheat effects
         *   and destructive failure/explosion.
         */
        thermals.builder()
                .setThermalMass(0.10F)
                .setMaxPower(THERMAL_RATING_WATTS, 150.0F)
                .addHeatSource(primaryStray);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        int total = MoreGridMath.totalTurns(placed.get(TOTAL_TURNS));
        int effectivePrimary = MoreGridMath.primaryTurns(placed.get(PRIMARY_TURNS), total);
        if (placed.get(PRIMARY_TURNS) != effectivePrimary) {
            placed.set(PRIMARY_TURNS, effectivePrimary);
        }
        return true;
    }

    @Override
    public boolean addToGoggleTooltip(
            @NotNull PlacedComponent placed,
            @NotNull List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        int total = MoreGridMath.totalTurns(placed.get(TOTAL_TURNS));
        int primary = MoreGridMath.primaryTurns(placed.get(PRIMARY_TURNS), total);
        int secondary = MoreGridMath.secondaryTurns(primary, total);
        float ratio = MoreGridMath.transformerRatio(primary, total);
        tooltip.add(Component.translatable("moregrid.tooltip.transformer.turns", primary, secondary, total));
        tooltip.add(Component.translatable("moregrid.tooltip.transformer.ratio", String.format(Locale.ROOT, "%.4f", ratio)));
        tooltip.add(Component.translatable("moregrid.tooltip.transformer.rating", Math.round(THERMAL_RATING_WATTS)));

        RuntimeState state = RUNTIME.get(placed);
        if (state != null
                && state.primaryWire().isConverged()
                && state.secondaryWire().isConverged()) {
            double primaryPower = Math.abs(
                    (state.primary1().getVoltage() - state.primary2().getVoltage())
                            * state.primaryWire().current()
            );
            double secondaryPower = Math.abs(
                    (state.secondary1().getVoltage() - state.secondary2().getVoltage())
                            * state.secondaryWire().current()
            );
            tooltip.add(Component.translatable(
                    "moregrid.tooltip.transformer.load",
                    String.format(Locale.ROOT, "%.1f", Math.max(primaryPower, secondaryPower))
            ));
        }

        if (placed.wires.size() >= 3
                && placed.wires.get(0) instanceof ElectricWire primaryWire
                && placed.wires.get(1) instanceof ElectricWire secondaryWire
                && placed.wires.get(2) instanceof ElectricWire magnetizingWire
                && primaryWire.isConverged()
                && secondaryWire.isConverged()
                && magnetizingWire.isConverged()) {
            double totalCurrent = Math.abs(primaryWire.current())
                    + Math.abs(secondaryWire.current())
                    + Math.abs(magnetizingWire.current());
            double physicalCopperAndCoreLoss = primaryWire.current() * primaryWire.current() * primaryWire.getResistance()
                    + secondaryWire.internalPower()
                    + magnetizingWire.internalPower();
            tooltip.add(Component.translatable(
                    "moregrid.tooltip.transformer.total_current",
                    String.format(Locale.ROOT, "%.3f", totalCurrent)
            ));
            tooltip.add(Component.translatable(
                    "moregrid.tooltip.transformer.loss",
                    String.format(Locale.ROOT, "%.3f", physicalCopperAndCoreLoss)
            ));
        }
        return true;
    }
}
