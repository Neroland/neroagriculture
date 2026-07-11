package za.co.neroland.neroagriculture.config;

import za.co.neroland.nerolandcore.config.ConfigManager;
import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;

/** Server-authoritative Stage 2 tuning surface. Lists use comma-separated identifiers. */
public final class AgricultureConfig {
    public static final ConfigSchema SCHEMA = ConfigSchema.create("neroagriculture",
            "NeroAgriculture discovery, cultivation and automation settings");

    public static final ConfigValue<Integer> DISCOVERY_SCAN_CAP = integer("discovery.scan_cap", 512, 16, 65_536);
    public static final ConfigValue<String> MATERIAL_BLACKLIST = text("discovery.material_blacklist", "");
    public static final ConfigValue<String> MATERIAL_OVERRIDES = text("discovery.material_overrides", "");
    public static final ConfigValue<Integer> CONDENSATION_TICKS = integer("condensation.ticks", 200, 1, 72_000);
    public static final ConfigValue<Integer> MACHINE_ENERGY_CAPACITY = integer("machines.energy_capacity", 100_000, 1_000, 10_000_000);
    public static final ConfigValue<Integer> MACHINE_ENERGY_RATE = integer("machines.energy_rate", 80, 1, 32_000);
    public static final ConfigValue<Integer> MACHINE_FLUID_CAPACITY = integer("machines.fluid_capacity_mb", 8_000, 1_000, 1_000_000);
    public static final ConfigValue<Integer> GROW_BED_ENERGY_COST = integer("grow_beds.energy_per_growth", 40, 0, 100_000);
    public static final ConfigValue<Integer> GROW_BED_NUTRIENT_COST = integer("grow_beds.nutrient_per_growth_mb", 25, 0, 10_000);
    public static final ConfigValue<Double> GROWTH_MULTIPLIER = decimal("growth.multiplier", 1.0, 0.05, 100.0);
    public static final ConfigValue<Double> YIELD_MULTIPLIER = decimal("growth.yield_multiplier", 1.0, 0.0, 100.0);
    public static final ConfigValue<Integer> AUTOMATION_INTERVAL = integer("automation.interval_ticks", 20, 1, 1_200);
    public static final ConfigValue<Integer> AUTOMATION_RANGE = integer("automation.range", 8, 1, 64);
    public static final ConfigValue<String> DIMENSION_ALLOWLIST = text("dimensions.allowlist", "");
    public static final ConfigValue<String> DIMENSION_DENYLIST = text("dimensions.denylist", "");

    private AgricultureConfig() { }

    private static ConfigValue<Integer> integer(String key, int value, int min, int max) {
        return SCHEMA.intRange(key, value, min, max, true, key);
    }

    private static ConfigValue<Double> decimal(String key, double value, double min, double max) {
        return SCHEMA.doubleRange(key, value, min, max, true, key);
    }

    private static ConfigValue<String> text(String key, String value) {
        return SCHEMA.string(key, value, true, key);
    }

    public static void init() {
        ConfigManager.register(SCHEMA);
    }
}
