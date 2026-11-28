/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.render.sky;

/**
 * Light paths through the hexagonal cross section of an ice prism, in the plane perpendicular to its axis.
 * The hexagon has side 1, and light enters travelling along minus x. Traces append their outputs to the instance as
 * the cosine and sine of the deviation, the signed angle from the incoming direction to the outgoing one, and a
 * weight. Outputs of one trace call that leave in the same direction are merged into one.
 * A reflection off a side face maps a direction angle a to 2 * normal angle + pi - a, so the directions inside the
 * prism after any number of reflections form six classes, by whether the count is odd and by a turn of 0, 120, or
 * 240 degrees. Light that entered through a side face has six classes for each entry face, and its exit direction
 * follows from its class and exit face.
 * An instance reuses its buffers, so one instance must not be shared between threads.
 */
final class IcePrism {
    static final int MAXIMUM_INTERACTIONS = 12;
    private static final int SIDE_COUNT = 6;
    private static final int TURNS = 3;
    private static final int MIRROR_CLASSES = 2 * TURNS;
    private static final int TRACE_CLASSES = SIDE_COUNT * MIRROR_CLASSES;
    private static final int SLOT_COUNT = SIDE_COUNT + TRACE_CLASSES * SIDE_COUNT;
    private static final int MAXIMUM_MIRROR_REFLECTIONS = 40;
    private static final double MINIMUM_WEIGHT = 1.0e-5;
    private static final double APOTHEM = Math.cos(Math.PI / 6.0);
    private static final double MEAN_PROJECTED_WIDTH = 6.0 / Math.PI;

    /**
     * SplitMix64, seeded from four numbers so that results do not depend on the order of work.
     */
    static final class SplitMixRandom {
        private long state;

        void seed(long seed, long row, long column, long sample) {
            state = seed * 0x9E3779B97F4A7C15L + row * 0xBF58476D1CE4E5B9L
                    + column * 0x94D049BB133111EBL + sample * 0xD6E8FEB86659FD93L;
        }

        double next() {
            state += 0x9E3779B97F4A7C15L;
            long mixed = state;
            mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
            mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
            mixed = mixed ^ (mixed >>> 31);
            return (mixed >>> 11) * 0x1.0p-53;
        }
    }

    private final double[] normalX = new double[SIDE_COUNT];
    private final double[] normalY = new double[SIDE_COUNT];
    private final double[] vertexX = new double[SIDE_COUNT + 1];
    private final double[] vertexY = new double[SIDE_COUNT + 1];
    private final double[] outputCosines;
    private final double[] outputSines;
    private final double[] outputWeights;
    private int outputCount;
    private final double[] slotCosines = new double[SLOT_COUNT];
    private final double[] slotSines = new double[SLOT_COUNT];
    private final double[] slotWeights = new double[SLOT_COUNT];
    private final boolean[] slotUsed = new boolean[SLOT_COUNT];
    private final int[] usedSlots = new int[SLOT_COUNT];
    private int usedSlotCount;

    /**
     * A trace call adds at most one output per entry sample for traceMirrors, and at most
     * 1 + MAXIMUM_INTERACTIONS per entry sample for trace.
     */
    IcePrism(int outputCapacity) {
        this.outputCosines = new double[outputCapacity];
        this.outputSines = new double[outputCapacity];
        this.outputWeights = new double[outputCapacity];
    }

    void clearOutputs() {
        outputCount = 0;
    }

    int outputCount() {
        return outputCount;
    }

    double outputCosine(int output) {
        return outputCosines[output];
    }

    double outputSine(int output) {
        return outputSines[output];
    }

    double outputWeight(int output) {
        return outputWeights[output];
    }

    /**
     * Adds light to a slot. The first light in a slot sets its direction, and later light in the same slot leaves in
     * the same direction up to rounding.
     */
    private void addToSlot(int slot, double deviationCosine, double deviationSine, double weight) {
        if (!slotUsed[slot]) {
            slotUsed[slot] = true;
            slotCosines[slot] = deviationCosine;
            slotSines[slot] = deviationSine;
            usedSlots[usedSlotCount] = slot;
            usedSlotCount++;
        }
        slotWeights[slot] += weight;
    }

    /**
     * Turns the used slots into outputs, in the order they were first used, and clears them.
     */
    private void flushSlots() {
        for (int used = 0; used < usedSlotCount; used++) {
            int slot = usedSlots[used];
            outputCosines[outputCount] = slotCosines[slot];
            outputSines[outputCount] = slotSines[slot];
            outputWeights[outputCount] = slotWeights[slot];
            outputCount++;
            slotWeights[slot] = 0.0;
            slotUsed[slot] = false;
        }
        usedSlotCount = 0;
    }

    /**
     * The class after a reflection off a side, with the same entry face, the other parity, and the turn moved.
     */
    private static int reflectedClass(int directionClass, int side) {
        int entryBase = directionClass / MIRROR_CLASSES * MIRROR_CLASSES;
        int parity = directionClass % MIRROR_CLASSES / TURNS;
        int turn = directionClass % TURNS;
        return entryBase + (1 - parity) * TURNS + Math.floorMod(side % TURNS - turn, TURNS);
    }

    private void setNormals(double rotation) {
        for (int side = 0; side < SIDE_COUNT; side++) {
            normalX[side] = Math.cos(rotation + side * Math.PI / 3.0);
            normalY[side] = Math.sin(rotation + side * Math.PI / 3.0);
        }
    }

    /**
     * Traces light that enters and leaves through side faces, splitting at every face by Fresnel weights.
     * The weights of one call add up to the prism's projected width over its mean projected width.
     */
    void trace(double index, double rotation, int entrySamples, SplitMixRandom random) {
        setNormals(rotation);
        double lowest = Double.MAX_VALUE;
        double highest = -Double.MAX_VALUE;
        for (int side = 0; side < SIDE_COUNT; side++) {
            double vertexHeight = Math.sin(rotation + side * Math.PI / 3.0 + Math.PI / 6.0);
            lowest = Math.min(lowest, vertexHeight);
            highest = Math.max(highest, vertexHeight);
        }
        double width = highest - lowest;
        double sampleWeight = width / entrySamples / MEAN_PROJECTED_WIDTH;
        for (int sample = 0; sample < entrySamples; sample++) {
            double height = lowest + (sample + random.next()) / entrySamples * width;
            int entrySide = -1;
            double entryX = Double.MAX_VALUE;
            for (int side = 0; side < SIDE_COUNT; side++) {
                if (normalX[side] > 1.0e-12) {
                    double x = (APOTHEM - normalY[side] * height) / normalX[side];
                    if (x < entryX) {
                        entryX = x;
                        entrySide = side;
                    }
                }
            }
            if (entrySide < 0) {
                continue;
            }
            double cosineIncident = normalX[entrySide];
            double inverseIndex = 1.0 / index;
            double cosineTransmitted = Math.sqrt(1.0 - inverseIndex * inverseIndex
                    * (1.0 - cosineIncident * cosineIncident));
            double reflectance = fresnelReflectance(1.0, index, cosineIncident, cosineTransmitted);
            double reflectedX = -1.0 + 2.0 * cosineIncident * normalX[entrySide];
            double reflectedY = 2.0 * cosineIncident * normalY[entrySide];
            addToSlot(entrySide, -reflectedX, -reflectedY, sampleWeight * reflectance);
            double scale = inverseIndex * cosineIncident - cosineTransmitted;
            double directionX = -inverseIndex + scale * normalX[entrySide];
            double directionY = scale * normalY[entrySide];
            inside(index, entryX, height, directionX, directionY, entrySide * MIRROR_CLASSES,
                    sampleWeight * (1.0 - reflectance), 0);
        }
        flushSlots();
    }

    private void inside(double index, double positionX, double positionY, double directionX, double directionY,
            int directionClass, double weight, int depth) {
        if (depth >= MAXIMUM_INTERACTIONS || weight < MINIMUM_WEIGHT) {
            return;
        }
        int exitSide = -1;
        double nearest = Double.MAX_VALUE;
        for (int side = 0; side < SIDE_COUNT; side++) {
            double along = directionX * normalX[side] + directionY * normalY[side];
            if (along > 1.0e-12) {
                double distance = (APOTHEM - positionX * normalX[side] - positionY * normalY[side]) / along;
                if (distance < nearest) {
                    nearest = distance;
                    exitSide = side;
                }
            }
        }
        if (exitSide < 0) {
            return;
        }
        nearest = Math.max(0.0, nearest);
        double nextX = positionX + nearest * directionX;
        double nextY = positionY + nearest * directionY;
        double cosine = directionX * normalX[exitSide] + directionY * normalY[exitSide];
        double sineSquared = index * index * (1.0 - cosine * cosine);
        double reflectedX = directionX - 2.0 * cosine * normalX[exitSide];
        double reflectedY = directionY - 2.0 * cosine * normalY[exitSide];
        int nextClass = reflectedClass(directionClass, exitSide);
        if (sineSquared >= 1.0) {
            inside(index, nextX, nextY, reflectedX, reflectedY, nextClass, weight, depth + 1);
            return;
        }
        double cosineOut = Math.sqrt(1.0 - sineSquared);
        double reflectance = fresnelReflectance(index, 1.0, cosine, cosineOut);
        double scale = cosineOut - index * cosine;
        double exitX = index * directionX + scale * normalX[exitSide];
        double exitY = index * directionY + scale * normalY[exitSide];
        addToSlot(SIDE_COUNT + directionClass * SIDE_COUNT + exitSide, -exitX, -exitY, weight * (1.0 - reflectance));
        inside(index, nextX, nextY, reflectedX, reflectedY, nextClass, weight * reflectance, depth + 1);
    }

    /**
     * Follows light that entered an end face as it reflects off the side faces on its way to the other end face.
     * Travel is the in-plane distance covered between the two end faces. Entry points are uniform over the hexagon.
     * Horizontal inside is the in-plane share of the light's direction inside the crystal, which sets the angle at
     * which it meets the side faces.
     */
    void traceMirrors(double rotation, double travel, int entrySamples, SplitMixRandom random, double index,
            double horizontalInside) {
        setNormals(rotation);
        for (int vertex = 0; vertex <= SIDE_COUNT; vertex++) {
            double angle = rotation + Math.PI / 6.0 + vertex * Math.PI / 3.0;
            vertexX[vertex] = Math.cos(angle);
            vertexY[vertex] = Math.sin(angle);
        }
        for (int sample = 0; sample < entrySamples; sample++) {
            int triangle = Math.min(SIDE_COUNT - 1, (int) (random.next() * SIDE_COUNT));
            double root = Math.sqrt(random.next());
            double split = random.next();
            double positionX = root * (1.0 - split) * vertexX[triangle] + root * split * vertexX[triangle + 1];
            double positionY = root * (1.0 - split) * vertexY[triangle] + root * split * vertexY[triangle + 1];
            double directionX = -1.0;
            double directionY = 0.0;
            int directionClass = 0;
            double remaining = travel;
            double weight = 1.0 / entrySamples;
            for (int interaction = 0; interaction <= MAXIMUM_MIRROR_REFLECTIONS; interaction++) {
                int nearestSide = -1;
                double nearest = Double.MAX_VALUE;
                for (int side = 0; side < SIDE_COUNT; side++) {
                    double along = directionX * normalX[side] + directionY * normalY[side];
                    if (along > 1.0e-12) {
                        double distance = (APOTHEM - positionX * normalX[side] - positionY * normalY[side]) / along;
                        if (distance < nearest) {
                            nearest = Math.max(0.0, distance);
                            nearestSide = side;
                        }
                    }
                }
                if (nearestSide < 0 || nearest >= remaining) {
                    addToSlot(directionClass, -directionX, -directionY, weight);
                    break;
                }
                positionX += nearest * directionX;
                positionY += nearest * directionY;
                remaining -= nearest;
                double cosine = directionX * normalX[nearestSide] + directionY * normalY[nearestSide];
                weight *= sideFaceReflectance(index, horizontalInside, cosine);
                if (weight < MINIMUM_WEIGHT) {
                    break;
                }
                directionX -= 2.0 * cosine * normalX[nearestSide];
                directionY -= 2.0 * cosine * normalY[nearestSide];
                directionClass = reflectedClass(directionClass, nearestSide);
            }
        }
        flushSlots();
    }

    /**
     * The share of light inside the crystal that a side face reflects, from the in-plane cosine of its angle to the
     * face normal, with total internal reflection.
     */
    private static double sideFaceReflectance(double index, double horizontalInside, double inPlaneCosine) {
        double cosine = inPlaneCosine * horizontalInside;
        double sineSquared = index * index * (1.0 - cosine * cosine);
        if (sineSquared >= 1.0) {
            return 1.0;
        }
        return fresnelReflectance(index, 1.0, cosine, Math.sqrt(1.0 - sineSquared));
    }

    static double fresnelReflectance(double fromIndex, double toIndex, double cosineIncident,
            double cosineTransmitted) {
        double perpendicular = (fromIndex * cosineIncident - toIndex * cosineTransmitted)
                / (fromIndex * cosineIncident + toIndex * cosineTransmitted);
        double parallel = (toIndex * cosineIncident - fromIndex * cosineTransmitted)
                / (toIndex * cosineIncident + fromIndex * cosineTransmitted);
        return 0.5 * (perpendicular * perpendicular + parallel * parallel);
    }
}
