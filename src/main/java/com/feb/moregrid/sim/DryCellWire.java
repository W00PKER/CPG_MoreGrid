package com.feb.moregrid.sim;

import com.feb.moregrid.util.MoreGridMath;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;
import org.patryk3211.powergrid.electricity.sim.special.CompoundWire;

import java.util.Collection;
import java.util.List;

/**
 * Thevenin battery model implemented as its Norton equivalent: a dynamic
 * conductance in parallel with a current source.  This avoids depending on
 * Power Grid's internal voltage-source constructor while remaining an exact
 * DC equivalent of an ideal source in series with internal resistance.
 */
public final class DryCellWire extends CompoundWire.ConductanceWire implements ISolverHook {
    private double openCircuitVoltage;
    private double internalResistance;
    private double nortonCurrent;

    public DryCellWire(
            double openCircuitVoltage,
            double internalResistance,
            IElectricNode positive,
            IElectricNode negative
    ) {
        super(positive, negative);
        update(openCircuitVoltage, internalResistance);
    }

    public void update(double voltage, double resistance) {
        openCircuitVoltage = Double.isFinite(voltage) ? Math.max(0.0D, voltage) : 0.0D;
        internalResistance = Math.max(MoreGridMath.MIN_RESISTANCE, resistance);
        setConductance(1.0D / internalResistance);
        nortonCurrent = openCircuitVoltage / internalResistance;
    }

    /** Current from the positive terminal to the negative terminal. */
    @Override
    public double current() {
        return conductance() * (node1.getVoltage() - node2.getVoltage()) - nortonCurrent;
    }

    /** Only the internal-resistance loss becomes battery heat. */
    @Override
    public double internalPower() {
        double current = current();

        if (!Double.isFinite(current)) {
            return 0.0;
        }
        return current * current * internalResistance;
    }

    @Override
    public void startIteration(int iteration) {
        // Linear source: no per-iteration linearisation is required.
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        // Current source flows from negative to positive.
        residual.add(node1.getIndex(), -nortonCurrent);
        residual.add(node2.getIndex(), nortonCurrent);
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return List.of(node1, node2);
    }

    public double openCircuitVoltage() {
        return openCircuitVoltage;
    }

    public double internalResistance() {
        return internalResistance;
    }

    @Override
    public String toString() {
        return String.format("DryCellWire(Voc=%g, R=%g)", openCircuitVoltage, internalResistance);
    }
}
