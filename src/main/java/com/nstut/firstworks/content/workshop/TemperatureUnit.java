package com.nstut.firstworks.content.workshop;

import java.util.Locale;

public enum TemperatureUnit {
    CELSIUS("°C"), FAHRENHEIT("°F"), KELVIN("K");
    private final String symbol;
    TemperatureUnit(String symbol) { this.symbol = symbol; }
    public double convert(double celsius) {
        return switch (this) {
            case CELSIUS -> celsius;
            case FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0;
            case KELVIN -> celsius + 273.15;
        };
    }
    public String format(double celsius) {
        return String.format(Locale.ROOT, "%.0f%s", convert(celsius), symbol);
    }
}
