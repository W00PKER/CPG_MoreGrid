package com.feb.moregrid.util;

public final class MoreGridMath {
    public static final int TRANSFORMER_MAX_TOTAL_TURNS = 30;
    public static final double TRANSFORMER_CORE_AL = 1.5D;
    public static final double TRANSFORMER_COUPLING_K = 0.9999D;
    public static final double TRANSFORMER_MUTUAL_MULTIPLIER = 10.0D;
    public static final float MIN_RESISTANCE = 1.0E-4F;

    private MoreGridMath() {
    }

    public static int totalTurns(int requested) {
        return Math.max(2, Math.min(TRANSFORMER_MAX_TOTAL_TURNS, requested));
    }

    public static int primaryTurns(int requested, int totalTurns) {
        int total = totalTurns(totalTurns);
        return Math.max(1, Math.min(total - 1, requested));
    }

    public static int secondaryTurns(int primaryTurns, int totalTurns) {
        int total = totalTurns(totalTurns);
        return total - primaryTurns(primaryTurns, total);
    }

    public static float transformerRatio(int primaryTurns, int totalTurns) {
        int total = totalTurns(totalTurns);
        int np = primaryTurns(primaryTurns, total);
        int ns = secondaryTurns(np, total);
        return (float) ns / (float) np;
    }

    public static TransformerEquivalent transformerEquivalent(int primaryTurns, int totalTurns) {
        int total = totalTurns(totalTurns);
        int np = primaryTurns(primaryTurns, total);
        int ns = secondaryTurns(np, total);
        double ratio = (double) ns / (double) np;
        double lp = np * (double) np * TRANSFORMER_CORE_AL;
        double ls = ns * (double) ns * TRANSFORMER_CORE_AL;
        double lm = TRANSFORMER_COUPLING_K * lp;

        return new TransformerEquivalent(
                total,
                np,
                ns,
                (float) ratio,
                safeResistance(lp - lm),
                safeResistance(ls - ratio * ratio * lm),
                safeResistance(lm * TRANSFORMER_MUTUAL_MULTIPLIER)
        );
    }

    public static float fuseResistance(float ratedCurrent) {
        float current = Math.max(0.25F, Math.min(32.0F, ratedCurrent));
        return Math.max(0.001F, Math.min(0.25F, 0.04F / current));
    }

    public static double fuseDamageIncrement(double current, double ratedCurrent, double tripTimeAt2x, double dt) {
        double ratio = Math.abs(current) / Math.max(1.0E-6D, ratedCurrent);
        if (ratio <= 1.0D) {
            return 0.0D;
        }
        double normalizedDenominator = Math.max(0.01D, 3.0D * tripTimeAt2x);
        return (ratio * ratio - 1.0D) * dt / normalizedDenominator;
    }

    public static double fuseCooling(double tripTimeAt2x, double dt) {
        return dt / Math.max(0.25D, 5.0D * tripTimeAt2x);
    }

    public static double dryCellVoltagePerCell(double stateOfCharge) {
        double soc = clamp01(stateOfCharge);
        return 0.9D + 0.7D * Math.sqrt(soc);
    }

    public static double dryCellOpenVoltage(int cells, double stateOfCharge) {
        double soc = clamp01(stateOfCharge);
        if (soc <= 1.0E-6D) {
            return 0.0D;
        }
        return Math.max(1, Math.min(12, cells)) * dryCellVoltagePerCell(soc);
    }

    public static double dryCellResistancePerCell(double stateOfCharge) {
        double emptyFraction = 1.0D - clamp01(stateOfCharge);
        return 0.15D + (1.2D - 0.15D) * emptyFraction * emptyFraction;
    }

    public static double dryCellInternalResistance(int cells, double stateOfCharge) {
        double soc = clamp01(stateOfCharge);
        if (soc <= 1.0E-6D) {
            // A depleted primary cell becomes an open circuit instead of an
            // everlasting low-voltage source.
            return 1.0E6D;
        }
        return Math.max(1, Math.min(12, cells)) * dryCellResistancePerCell(soc);
    }

    public static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static float safeResistance(double resistance) {
        if (!Double.isFinite(resistance)) {
            return MIN_RESISTANCE;
        }
        return (float) Math.max(MIN_RESISTANCE, resistance);
    }

    public record TransformerEquivalent(
            int totalTurns,
            int primaryTurns,
            int secondaryTurns,
            float ratio,
            float primaryStrayResistance,
            float secondaryStrayResistance,
            float magnetizingResistance
    ) {
    }
}
