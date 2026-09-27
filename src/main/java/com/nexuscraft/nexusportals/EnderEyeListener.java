package com.nexuscraft.nexusportals;

import org.bukkit.Location;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * This project's own real answer to "folding in stronghold-finding as the same ancient, unexplained
 * mechanic problem": there is no dedicated real event for an eye-of-ender throw (see
 * {@code EnderSignal.java}'s own comment) -- real Bukkit fires the exact same
 * {@code ProjectileLaunchEvent} every thrown projectile does, filtered here by
 * {@code getEntity() instanceof EnderSignal}. Captures the real, vanilla-computed
 * {@code getTargetLocation()} the instant it's thrown, before this project's own {@link
 * StrongholdTriangulator} ever sees it.
 */
public final class EnderEyeListener implements Listener {

    private final StrongholdSightingTracker tracker;

    public EnderEyeListener(StrongholdSightingTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof EnderSignal signal)) {
            return;
        }
        ProjectileSource shooter = signal.getShooter();
        if (!(shooter instanceof Player player)) {
            return;
        }
        Location target = signal.getTargetLocation();
        if (target == null) {
            return;
        }
        Location origin = player.getLocation();

        tracker.record(player.getUniqueId(), new Sighting(origin.getX(), origin.getZ(), target.getX(), target.getZ()));
        int count = tracker.sightingsOf(player.getUniqueId()).size();
        player.sendMessage("§5[Portals] §fThe eye of ender drifts off toward a stronghold. §7("
                + count + " sighting" + (count == 1 ? "" : "s") + " recorded -- §f/nexusportals stronghold§7 for your best estimate.)");
    }
}
