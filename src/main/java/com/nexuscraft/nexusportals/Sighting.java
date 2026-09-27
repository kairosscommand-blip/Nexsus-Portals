package com.nexuscraft.nexusportals;

/**
 * One real eye-of-ender throw, captured the instant a real {@code ProjectileLaunchEvent} fires for
 * an {@code EnderSignal} entity: where the player was standing ({@link #originX}/{@link #originZ})
 * and the real, vanilla-computed target it immediately reported via
 * {@code EnderSignal#getTargetLocation()} ({@link #targetX}/{@link #targetZ}). Only the horizontal
 * plane matters for stronghold-finding -- real vanilla strongholds are found by X/Z alone. Kept as
 * a plain data holder (not tied to a real {@code Location}/world) so {@link StrongholdTriangulator}
 * stays pure, dependency-free math this project can actually unit-test.
 */
public record Sighting(double originX, double originZ, double targetX, double targetZ) {

    public double directionX() {
        return targetX - originX;
    }

    public double directionZ() {
        return targetZ - originZ;
    }
}
