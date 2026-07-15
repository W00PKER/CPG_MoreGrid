package com.feb.moregrid.sim;

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

/**
 * A real transformer winding wire that also exposes the transformer's apparent
 * load to Power Grid's native circuit-board thermal system.
 *
 * <p>The electrical behaviour is unchanged from {@link ElectricWire}: the
 * configured winding resistance is still stamped into the circuit normally.
 * Only {@link #internalPower()} is extended. ThermalBuilder therefore owns all
 * heating, cooling, over-temperature and component-destruction behaviour; this
 * class does not implement a parallel custom trip timer.</p>
 */
public final class TransformerRatedWire extends ElectricWire {
    private final IElectricNode primaryTerminal1;
    private final IElectricNode primaryTerminal2;
    private final IElectricNode secondaryTerminal1;
    private final IElectricNode secondaryTerminal2;
    private ElectricWire secondaryWinding;

    public TransformerRatedWire(
            double resistance,
            IElectricNode windingStart,
            IElectricNode windingEnd,
            IElectricNode primaryTerminal1,
            IElectricNode primaryTerminal2,
            IElectricNode secondaryTerminal1,
            IElectricNode secondaryTerminal2
    ) {
        super(resistance, windingStart, windingEnd);
        this.primaryTerminal1 = primaryTerminal1;
        this.primaryTerminal2 = primaryTerminal2;
        this.secondaryTerminal1 = secondaryTerminal1;
        this.secondaryTerminal2 = secondaryTerminal2;
    }

    public void setSecondaryWinding(ElectricWire secondaryWinding) {
        this.secondaryWinding = secondaryWinding;
    }

    /**
     * Effective thermal load in watts.
     *
     * <p>Power Grid's thermal system normally consumes I²R loss from a wire.
     * A transformer's nameplate rating, however, is a transferred-power limit.
     * Returning the larger primary/secondary terminal VA lets the normal
     * ThermalBuilder temperature and overload path enforce that nameplate
     * rating, while still retaining at least the physical copper loss.</p>
     */
    @Override
    public double internalPower() {
        double copperLoss = super.internalPower();
        ElectricWire secondary = secondaryWinding;
        if (secondary == null || !isConverged() || !secondary.isConverged()) {
            return finiteNonNegative(copperLoss);
        }

        double primaryVoltage = primaryTerminal1.getVoltage() - primaryTerminal2.getVoltage();
        double secondaryVoltage = secondaryTerminal1.getVoltage() - secondaryTerminal2.getVoltage();
        double primaryLoad = Math.abs(primaryVoltage * current());
        double secondaryLoad = Math.abs(secondaryVoltage * secondary.current());
        double apparentLoad = Math.max(primaryLoad, secondaryLoad);

        if (!Double.isFinite(apparentLoad)) {
            return finiteNonNegative(copperLoss);
        }
        return Math.max(finiteNonNegative(copperLoss), apparentLoad);
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
