package com.nexuscraft.nexusportals;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * All of this plugin's shared, tunable numbers -- fixed shapes (the five {@link StabilityTier}
 * bands, the reignite ritual's own two-ingredient requirement) stay in code, same
 * "shared numbers in config, fixed shapes in code" split this whole family already uses.
 */
public final class PortalConfig {

    private final JavaPlugin plugin;

    public boolean enabled;

    public int decayIntervalMinutes;
    public double decayAmountPerInterval;

    public Material fuelItem;
    public double fuelRestorePerItem;
    public int fuelMaxItemsPerInteraction;

    public Material reigniteCatalystOne;
    public int reigniteCatalystOneAmount;
    public Material reigniteCatalystTwo;
    public int reigniteCatalystTwoAmount;
    public double reigniteRestoredStability;

    public double roughTransitChanceUnstable;
    public double roughTransitChanceCollapsing;
    public double roughTransitDamage;

    public String autoNamePrefix;
    public String menuTitle;

    public int strongholdMinSightingsForEstimate;

    public PortalConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(Logger logger) {
        var config = plugin.getConfig();

        enabled = config.getBoolean("enabled", true);

        decayIntervalMinutes = config.getInt("decay.interval-minutes", 30);
        decayAmountPerInterval = config.getDouble("decay.amount-per-interval", 4.0);

        fuelItem = parseMaterial(config.getString("fuel.item", "GLOWSTONE_DUST"), Material.GLOWSTONE_DUST, logger);
        fuelRestorePerItem = config.getDouble("fuel.restore-per-item", 8.0);
        fuelMaxItemsPerInteraction = config.getInt("fuel.max-items-per-interaction", 8);

        reigniteCatalystOne = parseMaterial(config.getString("reignite.catalyst-one", "BLAZE_POWDER"), Material.BLAZE_POWDER, logger);
        reigniteCatalystOneAmount = config.getInt("reignite.catalyst-one-amount", 1);
        reigniteCatalystTwo = parseMaterial(config.getString("reignite.catalyst-two", "ENDER_PEARL"), Material.ENDER_PEARL, logger);
        reigniteCatalystTwoAmount = config.getInt("reignite.catalyst-two-amount", 1);
        reigniteRestoredStability = config.getDouble("reignite.restored-stability", 55.0);

        roughTransitChanceUnstable = config.getDouble("transit.rough-transit-chance-unstable", 0.20);
        roughTransitChanceCollapsing = config.getDouble("transit.rough-transit-chance-collapsing", 0.45);
        roughTransitDamage = config.getDouble("transit.rough-transit-damage", 2.0);

        autoNamePrefix = config.getString("naming.auto-name-prefix", "Portal");
        menuTitle = config.getString("menu.title", "Portal Network");

        strongholdMinSightingsForEstimate = config.getInt("stronghold.min-sightings-for-estimate", 1);
    }

    private static Material parseMaterial(String name, Material fallback, Logger logger) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        if (material == null) {
            logger.warning("nexusportals: unknown material '" + name + "', falling back to " + fallback);
            return fallback;
        }
        return material;
    }
}
