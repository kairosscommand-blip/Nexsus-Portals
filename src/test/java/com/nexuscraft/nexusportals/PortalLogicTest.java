package com.nexuscraft.nexusportals;

import java.util.List;

/**
 * A standalone, dependency-free test suite for this project's pure-logic classes -- no Bukkit
 * stub involved at all, same "pure logic gets a real, runnable test; Bukkit-glue gets compile
 * verification only" split this family already applies elsewhere. Run directly with
 * {@code java PortalLogicTest} after a plain {@code javac} of just this package (no stub tree on
 * the classpath needed, since none of these classes touch Bukkit types).
 */
public final class PortalLogicTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testStabilityTierBoundaries();
        testDecayMath();
        testRefuelMathCapsAtHundred();
        testPortalKeyStorageRoundTrip();
        testTriangulationSingleSightingUsesRawTarget();
        testTriangulationTwoConvergingSightings();
        testTriangulationParallelSightingsFallsBackToAverage();
        testTriangulationThreeAgreeingSightingsIsHighConfidence();

        if (failures == 0) {
            System.out.println("All PortalLogicTest checks passed.");
        } else {
            System.out.println(failures + " PortalLogicTest check(s) FAILED.");
            System.exit(1);
        }
    }

    private static void testStabilityTierBoundaries() {
        expect(StabilityTier.fromStability(100.0) == StabilityTier.STABLE, "100 -> STABLE");
        expect(StabilityTier.fromStability(76.0) == StabilityTier.STABLE, "76 -> STABLE");
        expect(StabilityTier.fromStability(75.0) == StabilityTier.WEATHERED, "75 -> WEATHERED");
        expect(StabilityTier.fromStability(41.0) == StabilityTier.WEATHERED, "41 -> WEATHERED");
        expect(StabilityTier.fromStability(40.0) == StabilityTier.UNSTABLE, "40 -> UNSTABLE");
        expect(StabilityTier.fromStability(11.0) == StabilityTier.UNSTABLE, "11 -> UNSTABLE");
        expect(StabilityTier.fromStability(10.0) == StabilityTier.COLLAPSING, "10 -> COLLAPSING");
        expect(StabilityTier.fromStability(1.0) == StabilityTier.COLLAPSING, "1 -> COLLAPSING");
        expect(StabilityTier.fromStability(0.0) == StabilityTier.DORMANT, "0 -> DORMANT");
    }

    private static void testDecayMath() {
        expect(PortalMaintenanceService.decay(20.0, 4.0) == 16.0, "decay 20 by 4 -> 16");
        expect(PortalMaintenanceService.decay(2.0, 4.0) == 0.0, "decay floors at 0, never negative");
    }

    private static void testRefuelMathCapsAtHundred() {
        expect(PortalMaintenanceService.refuel(90.0, 2, 8.0) == 100.0, "refuel caps at 100");
        expect(PortalMaintenanceService.refuel(50.0, 1, 8.0) == 58.0, "refuel one item adds restore-per-item");
    }

    private static void testPortalKeyStorageRoundTrip() {
        PortalKey key = new PortalKey("world_nether", 100, 64, -200);
        PortalKey roundTripped = PortalKey.fromStorageKey(key.toStorageKey());
        expect(key.equals(roundTripped), "PortalKey survives storage-key round trip");
    }

    private static void testTriangulationSingleSightingUsesRawTarget() {
        Sighting only = new Sighting(0, 0, 300, -450);
        StrongholdEstimate estimate = StrongholdTriangulator.estimate(List.of(only));
        expect(estimate.x() == 300 && estimate.z() == -450, "single sighting reports the raw real target");
        expect(estimate.confidence() == StrongholdEstimate.Confidence.LOW, "single sighting is LOW confidence");
    }

    private static void testTriangulationTwoConvergingSightings() {
        // Two rays that genuinely cross at (100, 100): one thrown from (0,0) heading toward
        // (100,100), another thrown from (200,0) heading toward (100,100).
        Sighting a = new Sighting(0, 0, 50, 50);
        Sighting b = new Sighting(200, 0, 150, 50);
        StrongholdEstimate estimate = StrongholdTriangulator.estimate(List.of(a, b));
        expect(closeEnough(estimate.x(), 100.0) && closeEnough(estimate.z(), 100.0),
                "two converging rays triangulate to their real intersection, got (" + estimate.x() + "," + estimate.z() + ")");
    }

    private static void testTriangulationParallelSightingsFallsBackToAverage() {
        // Two throws heading in the exact same direction never really cross -- must fall back to
        // averaging the raw targets rather than exploding or reporting nonsense.
        Sighting a = new Sighting(0, 0, 100, 0);
        Sighting b = new Sighting(0, 50, 100, 50);
        StrongholdEstimate estimate = StrongholdTriangulator.estimate(List.of(a, b));
        expect(!Double.isNaN(estimate.x()) && !Double.isInfinite(estimate.x()), "parallel sightings never produce NaN/Infinity");
        expect(estimate.confidence() == StrongholdEstimate.Confidence.LOW, "parallel-fallback estimate stays LOW confidence");
    }

    private static void testTriangulationThreeAgreeingSightingsIsHighConfidence() {
        Sighting a = new Sighting(0, 0, 50, 50);
        Sighting b = new Sighting(200, 0, 150, 50);
        Sighting c = new Sighting(100, 200, 100, 150);
        StrongholdEstimate estimate = StrongholdTriangulator.estimate(List.of(a, b, c));
        expect(estimate.confidence() == StrongholdEstimate.Confidence.HIGH,
                "three tightly-agreeing sightings reach HIGH confidence, got " + estimate.confidence());
    }

    private static boolean closeEnough(double a, double b) {
        return Math.abs(a - b) < 0.001;
    }

    private static void expect(boolean condition, String description) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + description);
        }
    }
}
