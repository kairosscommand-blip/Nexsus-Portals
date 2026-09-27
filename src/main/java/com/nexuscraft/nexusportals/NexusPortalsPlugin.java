package com.nexuscraft.nexusportals;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * NexusPortals -- a massive overhaul of portals, the ninth entry in the ongoing engine-overhaul
 * series (after NexusIntegrity's structural collapse, NexusHydro's real fluid volume, NexusMinds'
 * mob AI + speech, NexusWands' spellcasting, NexusArcanum's enchanting table, NexusMerchants'
 * villager trading, NexusAngler's fishing, NexusAlchemy's brewing, and NexusSomnus' sleep), going
 * after a mechanic that's been genuinely confusing since Beta 1.9 (2011): vanilla's own blind,
 * unexplained 8:1 coordinate-scaling algorithm for linking Nether portals, and the equally
 * unaided eye-of-ender/stronghold guessing game underneath it.
 *
 * <p>Real, deliberate linking ({@code PortalLightingListener}/{@code PortalRegistry} -- every
 * portal a player actually lights becomes its own named, cataloged entry, never vanilla's blind
 * coordinate math); a real, browsable destination menu ({@code PortalTravelListener}/{@code
 * PortalMenu} -- stepping into a lit, network-managed portal always opens a real choice among
 * every portal you've discovered, never an automatic teleport); real portal upkeep
 * ({@code PortalMaintenanceService}/{@code PortalMaintenanceListener} -- a real, decaying
 * stability stat that only real fuel, fed to the frame, holds back, and a real reignite ritual a
 * portal that's fully collapsed actually needs); and a real stronghold aid
 * ({@code EnderEyeListener}/{@code StrongholdTriangulator} -- every real eye-of-ender throw is
 * tracked and triangulated into a real, growing-confidence X/Z estimate, exposed on request).
 *
 * <p>Deliberately its own standalone plugin: no other Nexus plugin touches Nether/End portals,
 * portal linking, or eye-of-ender throws at all. See README.md's own section for the full
 * reasoning.
 */
public final class NexusPortalsPlugin extends JavaPlugin {

    private PortalConfig config;
    private PortalRegistry registry;
    private PortalMaintenanceService maintenance;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new PortalConfig(this);
        config.load(getLogger());

        this.registry = new PortalRegistry(this);
        registry.load(getLogger());

        this.maintenance = new PortalMaintenanceService(config, registry);
        StrongholdSightingTracker sightings = new StrongholdSightingTracker();
        Random random = new Random();

        getServer().getPluginManager().registerEvents(new PortalLightingListener(config, registry, maintenance), this);
        getServer().getPluginManager().registerEvents(new PortalTravelListener(config, registry, random), this);
        getServer().getPluginManager().registerEvents(new PortalMenuListener(registry), this);
        getServer().getPluginManager().registerEvents(new PortalMaintenanceListener(config, registry, maintenance), this);
        getServer().getPluginManager().registerEvents(new EnderEyeListener(sightings), this);

        long decayPeriodTicks = 20L * 60L * config.decayIntervalMinutes;
        getServer().getScheduler().runTaskTimer(this, () -> maintenance.tick(), decayPeriodTicks, decayPeriodTicks);

        var command = getCommand("nexusportals");
        if (command != null) {
            command.setExecutor(new PortalCommand(config, registry, sightings, this::reload));
        }

        getLogger().info("NexusPortals enabled -- portals have been overhauled.");
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.save();
        }
        getLogger().info("NexusPortals disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(getLogger());
    }
}
