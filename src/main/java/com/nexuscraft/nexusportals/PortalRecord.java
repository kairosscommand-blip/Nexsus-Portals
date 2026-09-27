package com.nexuscraft.nexusportals;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One registered portal's full, persistent state. Mutable on purpose -- {@link PortalRegistry}
 * holds exactly one live instance per {@link PortalKey} and saves the whole set to
 * {@code portal_network.yml} on every change, same "in-memory cache backed by an eager-saved YAML
 * file" convention as this family's own per-location data stores elsewhere.
 */
public final class PortalRecord {

    private final PortalKey key;
    private String name;
    private UUID ownerId;
    private double stability;
    private long lastMaintainedMillis;
    /** Every real {@code NETHER_PORTAL} block belonging to this frame, encoded as
     *  {@code "x,y,z"} (this record's own {@link #key}'s world is implied -- a portal never spans
     *  two worlds). This is what lets {@link PortalRegistry}'s block index answer "which
     *  registered portal is this player physically standing in" precisely, rather than by a fuzzy
     *  radius around the anchor that could misfire near two portals built close together. */
    private final Set<String> blockCoordinates = new LinkedHashSet<>();
    private String standWorld;
    private double standX;
    private double standY;
    private double standZ;
    private float standYaw;
    private float standPitch;
    private final Set<UUID> discoveredBy = new LinkedHashSet<>();
    private boolean dormant;

    public PortalRecord(PortalKey key, String name) {
        this.key = key;
        this.name = name;
        this.stability = 100.0;
        this.lastMaintainedMillis = System.currentTimeMillis();
    }

    public PortalKey key() {
        return key;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public double stability() {
        return stability;
    }

    public StabilityTier tier() {
        return StabilityTier.fromStability(stability);
    }

    public void setStability(double stability) {
        this.stability = Math.max(0.0, Math.min(100.0, stability));
        this.dormant = this.stability <= 0.0;
    }

    public long lastMaintainedMillis() {
        return lastMaintainedMillis;
    }

    public void setLastMaintainedMillis(long millis) {
        this.lastMaintainedMillis = millis;
    }

    public Set<String> blockCoordinates() {
        return blockCoordinates;
    }

    public void addBlockCoordinate(int x, int y, int z) {
        blockCoordinates.add(x + "," + y + "," + z);
    }

    public boolean containsBlock(int x, int y, int z) {
        return blockCoordinates.contains(x + "," + y + "," + z);
    }

    public void setStandLocation(Location location) {
        this.standWorld = location.getWorld().getName();
        this.standX = location.getX();
        this.standY = location.getY();
        this.standZ = location.getZ();
        this.standYaw = location.getYaw();
        this.standPitch = location.getPitch();
    }

    public void setStandLocationRaw(String world, double x, double y, double z, float yaw, float pitch) {
        this.standWorld = world;
        this.standX = x;
        this.standY = y;
        this.standZ = z;
        this.standYaw = yaw;
        this.standPitch = pitch;
    }

    public Location standLocation() {
        World world = org.bukkit.Bukkit.getWorld(standWorld != null ? standWorld : key.world());
        if (world == null) {
            return null;
        }
        return new Location(world, standX, standY, standZ, standYaw, standPitch);
    }

    public String standWorld() {
        return standWorld;
    }

    public double standX() {
        return standX;
    }

    public double standY() {
        return standY;
    }

    public double standZ() {
        return standZ;
    }

    public float standYaw() {
        return standYaw;
    }

    public float standPitch() {
        return standPitch;
    }

    public Set<UUID> discoveredBy() {
        return discoveredBy;
    }

    public void markDiscovered(UUID playerId) {
        discoveredBy.add(playerId);
    }

    public boolean isDiscoveredBy(UUID playerId) {
        return discoveredBy.contains(playerId);
    }

    public boolean isDormant() {
        return dormant;
    }

    public void setDormant(boolean dormant) {
        this.dormant = dormant;
    }

    /** Wipes this portal's own history (name reverts to a fresh auto-generated one by the caller,
     *  discovery list clears, ownership clears) while keeping its {@link #key} and physical
     *  {@link #blockCoordinates} intact -- exactly what a "wild relight" (see
     *  {@code PortalLightingListener}'s own comment) deliberately does to a dormant portal that
     *  gets re-lit WITHOUT the real reignite ritual, as the real, lasting cost of letting a portal
     *  collapse and then cutting corners on bringing it back. */
    public void resetHistory() {
        this.ownerId = null;
        this.discoveredBy.clear();
    }
}
