package com.nexuscraft.nexusportals;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, dependency-free math turning a real player's accumulated {@link Sighting}s into one
 * {@link StrongholdEstimate}. This is this project's own real answer to "eye-of-ender
 * triangulation" -- vanilla itself gives a player nothing but a floating item drifting in some
 * direction and an unmarked map; this project tracks the real bearings and actually does the
 * geometry a player would otherwise have to eyeball.
 */
public final class StrongholdTriangulator {

    private static final double AGREEMENT_MARGIN_TIGHT = 12.0;
    private static final double PARALLEL_EPSILON = 1.0E-9;

    private StrongholdTriangulator() {
    }

    public static StrongholdEstimate estimate(List<Sighting> sightings) {
        if (sightings == null || sightings.isEmpty()) {
            return null;
        }
        if (sightings.size() == 1) {
            Sighting only = sightings.get(0);
            return new StrongholdEstimate(only.targetX(), only.targetZ(), 1, Double.NaN, StrongholdEstimate.Confidence.LOW);
        }

        List<double[]> intersections = new ArrayList<>();
        for (int i = 0; i < sightings.size(); i++) {
            for (int j = i + 1; j < sightings.size(); j++) {
                double[] point = intersect(sightings.get(i), sightings.get(j));
                if (point != null) {
                    intersections.add(point);
                }
            }
        }

        if (intersections.isEmpty()) {
            // Every pair of throws came out parallel (or too close to call) -- fall back to
            // averaging the raw reported targets rather than reporting nothing at all.
            double sumX = 0;
            double sumZ = 0;
            for (Sighting sighting : sightings) {
                sumX += sighting.targetX();
                sumZ += sighting.targetZ();
            }
            double avgX = sumX / sightings.size();
            double avgZ = sumZ / sightings.size();
            return new StrongholdEstimate(avgX, avgZ, sightings.size(), Double.NaN, StrongholdEstimate.Confidence.LOW);
        }

        double meanX = 0;
        double meanZ = 0;
        for (double[] point : intersections) {
            meanX += point[0];
            meanZ += point[1];
        }
        meanX /= intersections.size();
        meanZ /= intersections.size();

        double maxSpread = 0;
        for (double[] point : intersections) {
            double dx = point[0] - meanX;
            double dz = point[1] - meanZ;
            maxSpread = Math.max(maxSpread, Math.sqrt(dx * dx + dz * dz));
        }

        StrongholdEstimate.Confidence confidence;
        if (sightings.size() >= 3 && maxSpread <= AGREEMENT_MARGIN_TIGHT) {
            confidence = StrongholdEstimate.Confidence.HIGH;
        } else if (maxSpread <= AGREEMENT_MARGIN_TIGHT * 2) {
            confidence = StrongholdEstimate.Confidence.MEDIUM;
        } else {
            confidence = StrongholdEstimate.Confidence.LOW;
        }

        return new StrongholdEstimate(meanX, meanZ, sightings.size(), maxSpread, confidence);
    }

    /** Real 2D ray/ray intersection (X/Z plane): solves {@code P1 + t1*D1 = P2 + t2*D2} for
     *  {@code t1}, returning the resulting point, or {@code null} if the two throws were too close
     *  to parallel to triangulate anything useful from (two throws from nearly the same spot in
     *  nearly the same direction, most often). */
    private static double[] intersect(Sighting a, Sighting b) {
        double d1x = a.directionX();
        double d1z = a.directionZ();
        double d2x = b.directionX();
        double d2z = b.directionZ();

        double denominator = d1x * d2z - d1z * d2x;
        if (Math.abs(denominator) < PARALLEL_EPSILON) {
            return null;
        }

        double t1 = ((b.originX() - a.originX()) * d2z - (b.originZ() - a.originZ()) * d2x) / denominator;
        double x = a.originX() + t1 * d1x;
        double z = a.originZ() + t1 * d1z;
        return new double[] {x, z};
    }
}
