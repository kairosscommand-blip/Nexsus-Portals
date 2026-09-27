package com.nexuscraft.nexusportals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This project's own real answer to "folding in stronghold-finding as the same ancient, unexplained
 * mechanic problem": there is no dedicated real event for an eye-of-ender throw, and -- a real
 * {@code mvn clean package} against real paper-api 1.21.4-R0.1-SNAPSHOT corrected this project on
 * directly -- real {@code EnderSignal} is a plain {@code Entity}, not a {@code Projectile}, so it
 * never carries a real shooter/owner accessor at all and can never be delivered through a real
 * {@code ProjectileLaunchEvent} either (see {@code EnderSignal.java}'s own comment for the exact
 * real compile error this drew out). Real vanilla still fires a real, generic
 * {@code EntitySpawnEvent} for it -- attribution to a specific player is recovered here instead by
 * remembering the instant a player right-clicks with a real Ender Eye in hand
 * ({@code PlayerInteractEvent}) and correlating that against the next {@code EnderSignal} that
 * spawns near them, shortly after, in the same world.
 */
public final class EnderEyeListener implements Listener {

    /** How close (real blocks) a freshly-spawned {@code EnderSignal} must appear to a player's own
     *  recent Ender Eye interaction to count as "this is the one they threw" -- real vanilla spawns
     *  it essentially at the thrower's eye location, so this is a generous, not a tight, margin. */
    private static final double MAX_MATCH_DISTANCE_SQUARED = 16.0 * 16.0;
    /** How long (real milliseconds) a recorded interaction stays eligible to be matched against --
     *  real vanilla spawns the entity the same tick, but this leaves real headroom for event-order
     *  jitter without letting stale, never-matched entries linger indefinitely. */
    private static final long MAX_MATCH_AGE_MILLIS = 3_000L;

    private final StrongholdSightingTracker tracker;
    private final Map<UUID, PendingThrow> pendingThrows = new ConcurrentHashMap<>();

    public EnderEyeListener(StrongholdSightingTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) {
            return;
        }
        Player player = event.getPlayer();
        pendingThrows.put(player.getUniqueId(), new PendingThrow(player.getLocation(), System.currentTimeMillis()));
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity spawned = event.getEntity();
        if (!(spawned instanceof EnderSignal signal)) {
            return;
        }
        Location target = signal.getTargetLocation();
        if (target == null) {
            return;
        }

        UUID matchedPlayerId = findMatchingThrower(spawned.getLocation());
        if (matchedPlayerId == null) {
            return;
        }
        pendingThrows.remove(matchedPlayerId);

        // The spawn location itself is the real, near-exact origin of the throw -- no need to
        // re-derive it from the player's own recorded interaction location.
        Location origin = spawned.getLocation();

        tracker.record(matchedPlayerId, new Sighting(origin.getX(), origin.getZ(), target.getX(), target.getZ()));
        Player player = Bukkit.getPlayer(matchedPlayerId);
        if (player != null) {
            int count = tracker.sightingsOf(matchedPlayerId).size();
            player.sendMessage("§5[Portals] §fThe eye of ender drifts off toward a stronghold. §7("
                    + count + " sighting" + (count == 1 ? "" : "s") + " recorded -- §f/nexusportals stronghold§7 for your best estimate.)");
        }
    }

    private UUID findMatchingThrower(Location spawnLocation) {
        long now = System.currentTimeMillis();
        UUID best = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        for (Map.Entry<UUID, PendingThrow> entry : pendingThrows.entrySet()) {
            PendingThrow pending = entry.getValue();
            if (now - pending.timestampMillis() > MAX_MATCH_AGE_MILLIS) {
                continue;
            }
            if (!sameWorld(pending.origin(), spawnLocation)) {
                continue;
            }
            double distanceSquared = pending.origin().distanceSquared(spawnLocation);
            if (distanceSquared <= MAX_MATCH_DISTANCE_SQUARED && distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = entry.getKey();
            }
        }
        return best;
    }

    private static boolean sameWorld(Location a, Location b) {
        return a.getWorld() != null && b.getWorld() != null && a.getWorld().getName().equals(b.getWorld().getName());
    }

    private record PendingThrow(Location origin, long timestampMillis) {
    }
}
