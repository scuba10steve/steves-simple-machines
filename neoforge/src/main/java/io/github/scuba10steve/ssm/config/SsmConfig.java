package io.github.scuba10steve.ssm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SsmConfig {

    public static final ModConfigSpec SPEC;

    // Crusher
    public static final ModConfigSpec.IntValue CRUSHER_CAPACITY;
    public static final ModConfigSpec.IntValue CRUSHER_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue CRUSHER_PROCESS_TICKS;

    // Smelter
    public static final ModConfigSpec.IntValue SMELTER_CAPACITY;
    public static final ModConfigSpec.IntValue SMELTER_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue SMELTER_PROCESS_TICKS;

    // Sawmill
    public static final ModConfigSpec.IntValue SAWMILL_CAPACITY;
    public static final ModConfigSpec.IntValue SAWMILL_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue SAWMILL_PROCESS_TICKS;

    // Combustion Generator
    public static final ModConfigSpec.IntValue COMBUSTION_CAPACITY;
    public static final ModConfigSpec.IntValue COMBUSTION_GENERATION_RATE;
    public static final ModConfigSpec.IntValue COMBUSTION_MAX_OUTPUT;

    // Solar Generator
    public static final ModConfigSpec.IntValue SOLAR_CAPACITY;
    public static final ModConfigSpec.IntValue SOLAR_GENERATION_RATE;
    public static final ModConfigSpec.IntValue SOLAR_MAX_OUTPUT;

    // Water Wheel
    public static final ModConfigSpec.IntValue WATER_WHEEL_CAPACITY;
    public static final ModConfigSpec.IntValue WATER_WHEEL_GENERATION_RATE;
    public static final ModConfigSpec.IntValue WATER_WHEEL_MAX_OUTPUT;

    // Wind Turbine
    public static final ModConfigSpec.IntValue WIND_TURBINE_CAPACITY;
    public static final ModConfigSpec.IntValue WIND_TURBINE_GENERATION_RATE;
    public static final ModConfigSpec.IntValue WIND_TURBINE_MAX_OUTPUT;
    public static final ModConfigSpec.IntValue WIND_TURBINE_MIN_HEIGHT;
    public static final ModConfigSpec.IntValue WIND_TURBINE_MIN_FREE_NEIGHBORS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Crusher").push("crusher");
        CRUSHER_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 10_000, 1, Integer.MAX_VALUE);
        CRUSHER_ENERGY_PER_TICK = builder.comment("FE consumed per tick while processing").defineInRange("energy_per_tick", 40, 0, Integer.MAX_VALUE);
        CRUSHER_PROCESS_TICKS = builder.comment("Ticks required per operation").defineInRange("process_ticks", 100, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Smelter").push("smelter");
        SMELTER_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 10_000, 1, Integer.MAX_VALUE);
        SMELTER_ENERGY_PER_TICK = builder.comment("FE consumed per tick while processing").defineInRange("energy_per_tick", 40, 0, Integer.MAX_VALUE);
        SMELTER_PROCESS_TICKS = builder.comment("Ticks required per operation (flat, ignores vanilla cookingTime)").defineInRange("process_ticks", 100, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Sawmill").push("sawmill");
        SAWMILL_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 10_000, 1, Integer.MAX_VALUE);
        SAWMILL_ENERGY_PER_TICK = builder.comment("FE consumed per tick while processing").defineInRange("energy_per_tick", 40, 0, Integer.MAX_VALUE);
        SAWMILL_PROCESS_TICKS = builder.comment("Ticks required per operation").defineInRange("process_ticks", 80, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Combustion Generator").push("combustion_generator");
        COMBUSTION_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 50_000, 1, Integer.MAX_VALUE);
        COMBUSTION_GENERATION_RATE = builder.comment("FE generated per burn-tick").defineInRange("generation_rate", 160, 0, Integer.MAX_VALUE);
        COMBUSTION_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 200, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Solar Generator").push("solar_generator");
        SOLAR_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE);
        SOLAR_GENERATION_RATE = builder.comment("FE generated per tick during daytime with sky access").defineInRange("generation_rate", 20, 0, Integer.MAX_VALUE);
        SOLAR_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 100, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Water Wheel").push("water_wheel");
        WATER_WHEEL_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 30_000, 1, Integer.MAX_VALUE);
        WATER_WHEEL_GENERATION_RATE = builder.comment("FE generated per tick while adjacent to water").defineInRange("generation_rate", 30, 0, Integer.MAX_VALUE);
        WATER_WHEEL_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 120, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Wind Turbine").push("wind_turbine");
        WIND_TURBINE_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 30_000, 1, Integer.MAX_VALUE);
        WIND_TURBINE_GENERATION_RATE = builder.comment("FE generated per tick while at/above min_height with min_free_neighbors open").defineInRange("generation_rate", 30, 0, Integer.MAX_VALUE);
        WIND_TURBINE_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 120, 0, Integer.MAX_VALUE);
        WIND_TURBINE_MIN_HEIGHT = builder.comment("Minimum Y-level required to generate").defineInRange("min_height", 100, -64, 320);
        WIND_TURBINE_MIN_FREE_NEIGHBORS = builder.comment("Minimum number of the 6 adjacent positions that must be air").defineInRange("min_free_neighbors", 5, 0, 6);
        builder.pop();

        SPEC = builder.build();
    }
}
