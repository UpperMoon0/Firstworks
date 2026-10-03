package com.nstut.firstworks.content.workshop;

/** Shared Celsius arithmetic for stations and transferable workpieces. */
public final class ThermalModel {
    public static final double AMBIENT = 20;
    public static double approach(double current, double ceiling, double risePerTick) {
        return Math.max(AMBIENT, Math.min(ceiling, current + Math.max(0, risePerTick)));
    }
    public static double cool(double current, long ticks, double fallPerTick) {
        return Math.max(AMBIENT, current - Math.max(0, ticks) * Math.max(0, fallPerTick));
    }
    public static double boostedCeiling(double base, double boosted, int remaining, int decayTicks) {
        return base + (boosted - base) * Math.max(0, Math.min(1, remaining / (double) Math.max(1, decayTicks)));
    }
    public static int workableSeconds(double current, double minimum, double fallPerTick) {
        return current <= minimum || fallPerTick <= 0 ? 0 : (int) Math.ceil((current - minimum) / fallPerTick / 20);
    }
    private ThermalModel() {}
}
