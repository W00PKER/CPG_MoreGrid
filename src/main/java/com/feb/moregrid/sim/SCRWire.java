package com.feb.moregrid.sim;

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;
import org.patryk3211.powergrid.electricity.sim.special.CompoundWire;

import java.util.Collection;
import java.util.List;

/**
 * Three-terminal SCR approximation for Power Grid's steady-state solver.
 *
 * <p>OFF state: 10 MΩ forward/reverse blocking resistance.</p>
 * <p>ON state: V(AK) = Vdrop + I * Ron.</p>
 * <p>The gate triggers the device; anode current then keeps it latched until
 * it falls below the holding current or the device becomes reverse biased.</p>
 */
public final class SCRWire extends CompoundWire.ConductanceWire implements IOuterHook, ISolverHook {
    private static final double OFF_RESISTANCE = 10_000_000.0D;

    private final ElectricWire gateWire;
    private final float triggerCurrent;
    private final float holdingCurrent;
    private final float onResistance;
    private final float forwardDrop;
    private boolean state;
    private boolean switched;

    public SCRWire(
            IElectricNode anode,
            IElectricNode cathode,
            ElectricWire gateWire,
            boolean initialState,
            float triggerCurrent,
            float holdingCurrent,
            float onResistance,
            float forwardDrop
    ) {
        super(anode, cathode);
        this.gateWire = gateWire;
        this.triggerCurrent = triggerCurrent;
        this.holdingCurrent = holdingCurrent;
        this.onResistance = onResistance;
        this.forwardDrop = forwardDrop;
        this.state = initialState;
        updateConductance();
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        updateConductance();
        switched = true;
    }

    private void updateConductance() {
        setConductance(1.0D / (state ? onResistance : OFF_RESISTANCE));
    }

    @Override
    public void preSolve() {
        double vak = node1.getVoltage() - node2.getVoltage();
        if (state) {
            if (vak <= 0.0D || (isConverged() && current() < holdingCurrent)) {
                setState(false);
            }
            return;
        }

        if (!gateWire.isConverged()) {
            return;
        }
        double gateCurrent = Math.max(0.0D, gateWire.current());
        if (vak > 0.5D && gateCurrent >= triggerCurrent) {
            setState(true);
        }
    }

    @Override
    public double current() {
        double resistiveCurrent = conductance() * (node1.getVoltage() - node2.getVoltage());
        return state ? resistiveCurrent - conductance() * forwardDrop : resistiveCurrent;
    }

    @Override
    public double internalPower() {
        if (!state) {
            return 0.0D;
        }
        double current = Math.max(0.0D, current());
        return current * forwardDrop + current * current * onResistance;
    }

    @Override
    public void startIteration(int iteration) {
        // Piecewise-linear state is selected in preSolve().
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        if (!state) {
            return;
        }
        double offsetCurrent = conductance() * forwardDrop;
        residual.add(node1.getIndex(), -offsetCurrent);
        residual.add(node2.getIndex(), offsetCurrent);
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return List.of(node1, node2);
    }

    public boolean wasSwitched() {
        boolean result = switched;
        switched = false;
        return result;
    }

    @Override
    public String toString() {
        return String.format("SCRWire(state=%s, Ron=%g, Vf=%g)", state, onResistance, forwardDrop);
    }
}
