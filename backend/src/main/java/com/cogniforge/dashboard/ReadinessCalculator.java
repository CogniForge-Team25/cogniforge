package com.cogniforge.dashboard;

// Readiness indicator from the interim report (section 6.4.1).
// It is an internal indicator, not an official certification result.
public final class ReadinessCalculator {

    public static final double PENALTY_PER_OPEN_GAP = 2.5;

    private ReadinessCalculator() {}

    public static double score(int requiredControls, int supportedControls, int openGaps) {
        if (requiredControls < 0 || supportedControls < 0 || openGaps < 0) {
            throw new IllegalArgumentException("Counts must not be negative");
        }
        if (supportedControls > requiredControls) {
            throw new IllegalArgumentException("Supported controls cannot exceed required controls");
        }
        if (requiredControls == 0) {
            return 0.0;
        }
        double base = supportedControls * 100.0 / requiredControls;
        double penalised = base - PENALTY_PER_OPEN_GAP * openGaps;
        return Math.max(0.0, Math.min(100.0, penalised));
    }
}
