package com.nexuscraft.nexusportals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Purely in-memory, per-player eye-of-ender sighting history -- same "an interrupted session
 * losing its own progress on restart is correct, not data loss" reasoning as this family's other
 * in-memory session trackers ({@code BrewSession}, {@code SleepSession}): a player's own
 * in-progress stronghold hunt isn't durable server state worth a YAML file, just a convenience
 * that resets cleanly on restart.
 */
public final class StrongholdSightingTracker {

    private final Map<UUID, List<Sighting>> sightings = new ConcurrentHashMap<>();

    public void record(UUID playerId, Sighting sighting) {
        sightings.computeIfAbsent(playerId, id -> new ArrayList<>()).add(sighting);
    }

    public List<Sighting> sightingsOf(UUID playerId) {
        return sightings.getOrDefault(playerId, List.of());
    }

    public void clear(UUID playerId) {
        sightings.remove(playerId);
    }
}
