package za.co.neroland.neroagriculture.config;

import za.co.neroland.nerolandcore.config.ConfigManager;
import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;

/** Server-authoritative Stage 2 tuning surface. Lists use comma-separated identifiers. */
public final class AgricultureConfig {
    public static final ConfigSchema SCHEMA = ConfigSchema.create("neroagriculture",
            "NeroAgriculture discovery, cultivation and automation settings");

    public static final ConfigValue<Integer> DISCOVERY_SCAN_CAP = SCHEMA.intRange("discovery.scan_cap", 512,
            16, 65_536, true, "Maximum enabled catalog entries exposed and considered for client sync");
    public static final ConfigValue<String> MATERIAL_BLACKLIST = SCHEMA.string("discovery.material_blacklist", "",
            true, "Comma-separated material ids disabled before exposure");
    public static final ConfigValue<String> MATERIAL_OVERRIDES = SCHEMA.string("discovery.material_overrides", "",
            true, "Semicolon entries: id|tier=orbital|gate=id|yield=min:max:ramp|conversion=n|enabled=true");
    public static final ConfigValue<Integer> CONDENSATION_TICKS = integer("condensation.ticks", 200, 1, 72_000);
    public static final ConfigValue<Integer> MACHINE_ENERGY_CAPACITY = integer("machines.energy_capacity", 100_000, 1_000, 10_000_000);
    public static final ConfigValue<Integer> MACHINE_ENERGY_RATE = integer("machines.energy_rate", 80, 1, 32_000);
    public static final ConfigValue<Integer> MACHINE_FLUID_CAPACITY = integer("machines.fluid_capacity_mb", 8_000, 1_000, 1_000_000);
    public static final ConfigValue<Integer> GROW_BED_ENERGY_COST = integer("grow_beds.energy_per_growth", 40, 0, 100_000);
    public static final ConfigValue<Integer> GROW_BED_NUTRIENT_COST = integer("grow_beds.nutrient_per_growth_mb", 25, 0, 10_000);
    public static final ConfigValue<Double> GROWTH_MULTIPLIER = decimal("growth.multiplier", 1.0, 0.05, 100.0);
    public static final ConfigValue<Double> YIELD_MULTIPLIER = decimal("growth.yield_multiplier", 1.0, 0.0, 100.0);
    public static final ConfigValue<Integer> YIELD_TIER_CAP_BASE = integer("growth.tier_yield_cap_base", 3, 1, 64);
    public static final ConfigValue<Integer> YIELD_TIER_CAP_STEP = integer("growth.tier_yield_cap_step", 1, 0, 16);
    public static final ConfigValue<String> CONTROLLED_TIER = SCHEMA.string("growth.controlled_tier", "orbital",
            true, "Lowest tier that always requires a sealed greenhouse (terran|industrial|orbital|colonial|deepvoid)");
    public static final ConfigValue<Integer> GREENHOUSE_VOLUME_CAP = integer("greenhouse.volume_cap", 4_096, 27, 65_536);
    public static final ConfigValue<Integer> GREENHOUSE_REVALIDATE_TICKS = integer("greenhouse.revalidate_ticks", 100, 20, 12_000);
    public static final ConfigValue<Integer> GREENHOUSE_UPKEEP_TICKS = integer("greenhouse.upkeep_ticks", 20, 1, 1_200);
    public static final ConfigValue<Integer> GREENHOUSE_NF_PER_VOLUME = integer("greenhouse.nf_per_32_volume", 2, 0, 10_000);
    public static final ConfigValue<Integer> GREENHOUSE_NUTRIENT_PER_CROP = integer("greenhouse.nutrient_mb_per_crop", 1, 0, 10_000);
    public static final ConfigValue<Integer> AUTOMATION_INTERVAL = integer("automation.interval_ticks", 20, 1, 1_200);
    public static final ConfigValue<Integer> AUTOMATION_RANGE = integer("automation.range", 8, 1, 64);
    public static final ConfigValue<Integer> AUTOMATION_PER_PASS = integer("automation.columns_per_pass", 4, 1, 81);
    public static final ConfigValue<Integer> AUTOMATION_ENERGY_PER_OP = integer("automation.energy_per_op", 60, 0, 100_000);
    public static final ConfigValue<Boolean> AUTOMATION_TRACK_OWNER = SCHEMA.bool("automation.track_owner", true,
            true, "Record the placing player's UUID (only) on automation machines for claim checks; opt-out");
    public static final ConfigValue<Integer> FERTILISER_MAX_DOSE = integer("fertiliser.max_dose", 8, 1, 64);
    public static final ConfigValue<Integer> FERTILISER_DURATION_TICKS = integer("fertiliser.duration_ticks", 6_000, 20, 720_000);
    public static final ConfigValue<Integer> FERTILISER_ENERGY_PER_APPLY = integer("fertiliser.energy_per_apply", 40, 0, 100_000);
    public static final ConfigValue<Integer> GENETICS_HARDINESS_RELAX = integer("genetics.hardiness_hostile_relax", 3, 1, 5);
    public static final ConfigValue<Integer> POLLINATION_CHANCE_PERCENT = integer("genetics.pollination_chance_percent", 6, 0, 100);
    public static final ConfigValue<Integer> POLLINATION_MUTATION_PERCENT = integer("genetics.mutation_chance_percent", 20, 0, 100);
    public static final ConfigValue<Integer> POLLINATION_COOLDOWN_TICKS = integer("genetics.pollination_cooldown_ticks", 200, 20, 72_000);
    public static final ConfigValue<Integer> POLLINATION_BEACON_RANGE = integer("genetics.beacon_range", 4, 1, 16);
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
