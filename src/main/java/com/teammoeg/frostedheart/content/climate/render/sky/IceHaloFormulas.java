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

import java.util.Arrays;

/**
 * Halo arcs of oriented ice crystals from formulas, splatted into the bins of IceHaloMap.
 * Light leaving through side faces lies on the cone around the crystal axis through the sun, at the in-plane
 * deviation of the side face table for Bravais' effective index. Light entering an end face is traced exactly in 3D,
 * and light passing from end face to end face is followed through its side face reflections in the plane.
 * Each axis is weighted by its projected side area. Plates mirror the sun into a pillar from IceMirror, and the other
 * paths go to the sun's bin. Single threaded and deterministic. An instance reuses its scratch
 * buffers, so one instance must not be shared between threads.
 */
final class IceHaloFormulas {
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final double COLUMN_AZIMUTH_STEP_RADIANS = Math.toRadians(0.25);
    private static final int DEVIATION_STEPS_PER_TABLE_COLUMN = 2;
    private static final int FACE_ANGLE_STEPS = 360;
    private static final int PARRY_ENTRY_SAMPLES = 32;
    private static final int MIRROR_ENTRY_SAMPLES = 32;
    private static final int PARRY_MIRROR_ENTRY_SAMPLES = 64;
    private static final int PLATE_MIRROR_ROTATIONS = 8;
    private static final int COLUMN_MIRROR_ROTATIONS = 2;
    private static final int MAXIMUM_PLATE_AXES = 196;
    private static final double PLATE_TILT_STEP_RADIANS = Math.toRadians(0.5);
    private static final double PILLAR_SPAN_SIGMAS = 6.0;
    private static final int COLUMN_AXES = (int) Math.round(Math.PI / COLUMN_AZIMUTH_STEP_RADIANS);
    private static final int MAXIMUM_AXES = Math.max(MAXIMUM_PLATE_AXES, COLUMN_AXES);
    private static final int PRISM_OUTPUT_CAPACITY = Math.max(Math.max(
            Math.max(PLATE_MIRROR_ROTATIONS, COLUMN_MIRROR_ROTATIONS) * MIRROR_ENTRY_SAMPLES, PARRY_MIRROR_ENTRY_SAMPLES),
            PARRY_ENTRY_SAMPLES * (1 + IcePrism.MAXIMUM_INTERACTIONS));
    private static final double[] REFRACTIVE_INDEX = new double[CHANNELS];
    private static final double[] FACE_ANGLE_COSINES = new double[FACE_ANGLE_STEPS];
    private static final double[] FACE_ANGLE_SINES = new double[FACE_ANGLE_STEPS];

    static {
        for (int channel = 0; channel < CHANNELS; channel++) {
            double micrometers = SkyParticleOptics.CHANNEL_CENTER_NANOMETERS[channel] / 1000.0;
            REFRACTIVE_INDEX[channel] = 1.3005 + 0.0030 / (micrometers * micrometers);
        }
        for (int face = 0; face < FACE_ANGLE_STEPS; face++) {
            double angle = (face + 0.5) * 2.0 * Math.PI / FACE_ANGLE_STEPS;
            FACE_ANGLE_COSINES[face] = Math.cos(angle);
            FACE_ANGLE_SINES[face] = Math.sin(angle);
        }
    }

    /**
     * One sampled crystal axis. The instances are reused for every crystal kind and map.
     */
    private static final class Axis {
        private double x;
        private double y;
        private double z;
        private double weight;
        private double rotation;

        double x() {
            return x;
        }

        double y() {
            return y;
        }

        double z() {
            return z;
        }

        double weight() {
            return weight;
        }

        double rotation() {
            return rotation;
        }

        void set(double x, double y, double z, double weight, double rotation) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.weight = weight;
            this.rotation = rotation;
        }
    }

    private final IceCrystalData data;
    private final double deviationStep;
    private final double[] deviationCosines;
    private final double[] deviationSines;
    private final double[][] sideFaceDensities;
    private final double[] sideWeights = new double[CHANNELS];
    private final double[] pendingSideWeights = new double[CHANNELS];
    private final double[] faceViewX = new double[FACE_ANGLE_STEPS];
    private final double[] faceViewY = new double[FACE_ANGLE_STEPS];
    private final double[] faceViewZ = new double[FACE_ANGLE_STEPS];
    private final double[] faceWeights = new double[FACE_ANGLE_STEPS];
    private final double[] mirroredShares = new double[CHANNELS];
    private final double[] pillarShares = new double[CHANNELS];
    private final double[] pillarScales = new double[CHANNELS];
    private final double[] mirrorLobeScales = new double[CHANNELS];
    private final double[] rowStrengths = new double[CHANNELS];
    private final double[] coneBase = new double[4];
    private final double[] perpendicular = new double[3];
    private final Axis[] axes = new Axis[MAXIMUM_AXES];
    private final IcePrism prism = new IcePrism(PRISM_OUTPUT_CAPACITY);
    private final IcePrism.SplitMixRandom random = new IcePrism.SplitMixRandom();
    private final IcePrism.SplitMixRandom axisRandom = new IcePrism.SplitMixRandom();

    IceHaloFormulas(IceCrystalData data) {
        this.data = data;
        int columns = data.sideFaceDeviationColumns();
        int steps = columns * DEVIATION_STEPS_PER_TABLE_COLUMN;
        this.deviationStep = data.sideFaceDeviationStep() / DEVIATION_STEPS_PER_TABLE_COLUMN;
        this.deviationCosines = new double[steps];
        this.deviationSines = new double[steps];
        for (int step = 0; step < steps; step++) {
            double deviation = (step + 0.5) * deviationStep;
            deviationCosines[step] = Math.cos(deviation);
            deviationSines[step] = Math.sin(deviation);
        }
        this.sideFaceDensities = new double[CHANNELS][columns];
        for (int axis = 0; axis < MAXIMUM_AXES; axis++) {
            axes[axis] = new Axis();
        }
    }

    static double refractiveIndex(int channel) {
        return REFRACTIVE_INDEX[channel];
    }

    /**
     * The factor on IceMirror.pillarPhase that gives the mirror pillar of the crystal kind added last per steradian
     * over the whole sky, inside the map and below it, or 0 for a kind that is not a plate.
     */
    double mirrorLobeScale(int channel) {
        return mirrorLobeScales[channel];
    }

    /**
     * Adds one oriented crystal kind into maps of phase per steradian, indexed by channel and then by bin.
     * Path shares are indexed by channel and then by IceCrystalData path class.
     */
    void addCrystal(double[][] maps, IceCrystalData.Crystal crystal, double[] sun, double weight,
            double[][] pathShares) {
        int axisCount = sampleAxes(crystal, sun);
        boolean parry = crystal.orientation() == IceCrystalData.Orientation.PARRY;
        int mirrorRotations = crystal.orientation() == IceCrystalData.Orientation.PLATE
                ? PLATE_MIRROR_ROTATIONS : COLUMN_MIRROR_ROTATIONS;
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
            Axis axis = axes[axisIndex];
            double shared = weight * axis.weight();
            if (!parry) {
                for (int mirrored = 0; mirrored <= 1; mirrored++) {
                    int path = mirrored == 0 ? IceCrystalData.SIDE_PATH : IceCrystalData.SIDE_MIRRORED_PATH;
                    for (int channel = 0; channel < CHANNELS; channel++) {
                        sideWeights[channel] = shared * pathShares[channel][path];
                    }
                    addSideCones(maps, sun, axis, mirrored == 1);
                }
            }
            for (int channel = 0; channel < CHANNELS; channel++) {
                double index = REFRACTIVE_INDEX[channel];
                double[] shares = pathShares[channel];
                random.seed(43, axisIndex, channel, 0);
                if (parry) {
                    for (int mirrored = 0; mirrored <= 1; mirrored++) {
                        int path = mirrored == 0 ? IceCrystalData.SIDE_PATH : IceCrystalData.SIDE_MIRRORED_PATH;
                        addFixedSidePaths(maps[channel], sun, axis, index, shared * shares[path], mirrored == 1);
                    }
                }
                addEndToSide(maps[channel], sun, axis, index, shared * shares[IceCrystalData.END_TO_SIDE_PATH],
                        parry);
                addEndToEnd(maps[channel], sun, axis, index, shared * shares[IceCrystalData.END_TO_END_PATH],
                        crystal.halfHeightOverRadius(), parry ? 1 : mirrorRotations, parry);
            }
        }
        Arrays.fill(mirroredShares, 0.0);
        Arrays.fill(mirrorLobeScales, 0.0);
        if (crystal.orientation() == IceCrystalData.Orientation.PLATE) {
            addMirrorPillar(maps, crystal, Math.asin(Math.max(-1.0, Math.min(1.0, sun[1]))), weight, pathShares);
        }
        int sunBin = IceHaloMap.binOf(sun[0], sun[1], sun[2]);
        if (sunBin >= 0) {
            for (int channel = 0; channel < CHANNELS; channel++) {
                double atSun = pathShares[channel][IceCrystalData.END_MIRROR_PATH]
                        + pathShares[channel][IceCrystalData.OTHER_PATH];
                maps[channel][sunBin] += (weight * atSun - mirroredShares[channel])
                        / IceHaloMap.binSolidAngle(sunBin);
            }
        }
    }

    /**
     * Moves the light that the end faces of plates mirror from the sun into the pillar, as far as the pillar lies in
     * the map, and writes what each channel moved into mirroredShares. The data set gives the mirrored share, and
     * IceMirror the shape it spreads over. The part of the pillar below the map stays at the sun.
     */
    private void addMirrorPillar(double[][] maps, IceCrystalData.Crystal crystal, double sunElevation, double weight,
            double[][] pathShares) {
        double tiltSigma = Math.toRadians(crystal.tiltDegrees());
        double crossSection = IceMirror.crossSection(sunElevation, tiltSigma, crystal.halfHeightOverRadius());
        Arrays.fill(pillarShares, 0.0);
        splatMirrorPillar(maps, sunElevation, tiltSigma, crossSection, false);
        for (int channel = 0; channel < CHANNELS; channel++) {
            double whole = IceMirror.pillarShare(sunElevation, tiltSigma, crossSection, REFRACTIVE_INDEX[channel]);
            double insideShare = whole > 0.0 ? Math.min(1.0, pillarShares[channel] / whole) : 0.0;
            mirroredShares[channel] = weight * pathShares[channel][IceCrystalData.END_MIRROR_PATH] * insideShare;
            mirrorLobeScales[channel] = whole > 0.0
                    ? weight * pathShares[channel][IceCrystalData.END_MIRROR_PATH] / whole : 0.0;
            pillarScales[channel] = pillarShares[channel] > 0.0 ? mirroredShares[channel] / pillarShares[channel]
                    : 0.0;
        }
        splatMirrorPillar(maps, sunElevation, tiltSigma, crossSection, true);
    }

    /**
     * Goes over the bins the pillar covers, with its mass over each bin from Gaussian masses along and across.
     * Without adding it only sums the masses per unit of light hitting plates into pillarShares. With adding it adds
     * each mass times pillarScales into the maps.
     */
    private void splatMirrorPillar(double[][] maps, double sunElevation, double tiltSigma, double crossSection,
            boolean add) {
        double heightSigma = IceMirror.pillarHeightSigma(tiltSigma);
        double center = -sunElevation;
        int firstRow = Math.max(0, IceHaloMap.rowOfElevation(center - PILLAR_SPAN_SIGMAS * heightSigma));
        int lastRow = Math.min(IceHaloMap.ELEVATION_BINS - 1,
                IceHaloMap.rowOfElevation(center + PILLAR_SPAN_SIGMAS * heightSigma));
        for (int row = firstRow; row <= lastRow; row++) {
            double lower = IceHaloMap.LOWEST_ELEVATION + row * IceHaloMap.BIN_STEP;
            double upper = Math.min(Math.PI / 2.0, lower + IceHaloMap.BIN_STEP);
            double rowMass = IceMirror.gaussianMass((lower - center) / heightSigma, (upper - center) / heightSigma);
            if (rowMass <= 0.0) {
                continue;
            }
            double elevation = 0.5 * (lower + upper);
            for (int channel = 0; channel < CHANNELS; channel++) {
                rowStrengths[channel] = rowMass
                        * IceMirror.pillarStrength(elevation, sunElevation, crossSection, REFRACTIVE_INDEX[channel]);
            }
            double columnWidth = IceHaloMap.BIN_STEP * Math.cos(elevation)
                    / IceMirror.pillarWidthSigma(elevation, sunElevation, tiltSigma);
            for (int column = 0; column < IceHaloMap.AZIMUTH_BINS; column++) {
                double inner = column * columnWidth;
                if (inner > PILLAR_SPAN_SIGMAS) {
                    break;
                }
                double columnMass = 2.0 * IceMirror.gaussianMass(inner, inner + columnWidth);
                if (columnMass <= 0.0) {
                    continue;
                }
                int bin = column + row * IceHaloMap.AZIMUTH_BINS;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    double mass = rowStrengths[channel] * columnMass;
                    if (add) {
                        maps[channel][bin] += mass * pillarScales[channel] * IceHaloMap.inverseBinSolidAngle(bin);
                    } else {
                        pillarShares[channel] += mass;
                    }
                }
            }
        }
    }

    private static double gaussian(IcePrism.SplitMixRandom random) {
        return Math.sqrt(-2.0 * Math.log(1.0 - random.next())) * Math.cos(2.0 * Math.PI * random.next());
    }

    /**
     * Fills the axes of a crystal kind into the reused axis array and returns how many there are.
     */
    private int sampleAxes(IceCrystalData.Crystal crystal, double[] sun) {
        double tilt = Math.toRadians(crystal.tiltDegrees());
        IcePrism.SplitMixRandom random = axisRandom;
        random.seed(47, crystal.name().hashCode(), 0, 0);
        if (crystal.orientation() == IceCrystalData.Orientation.PLATE) {
            if (tilt <= 0.0) {
                axes[0].set(0.0, 1.0, 0.0, 1.0, 0.0);
                return 1;
            }
            int side = (int) Math.min(Math.sqrt(MAXIMUM_PLATE_AXES),
                    Math.max(7.0, Math.ceil(6.0 * tilt / PLATE_TILT_STEP_RADIANS)));
            int count = 0;
            for (int row = 0; row < side; row++) {
                for (int column = 0; column < side; column++) {
                    double first = (row + random.next()) / side;
                    double second = (column + random.next()) / side;
                    double angle = tilt * Math.sqrt(-2.0 * Math.log(1.0 - first));
                    double direction = 2.0 * Math.PI * second;
                    axes[count].set(Math.sin(angle) * Math.cos(direction), Math.cos(angle),
                            Math.sin(angle) * Math.sin(direction), 1.0 / (side * side), 0.0);
                    count++;
                }
            }
            return count;
        }
        boolean parry = crystal.orientation() == IceCrystalData.Orientation.PARRY;
        double rotationSpread = Math.toRadians(crystal.rotationDegrees());
        double total = 0.0;
        for (int step = 0; step < COLUMN_AXES; step++) {
            double azimuth = (step + random.next()) * COLUMN_AZIMUTH_STEP_RADIANS;
            double elevation = tilt > 0.0 ? tilt * gaussian(random) : 0.0;
            double x = Math.cos(elevation) * Math.cos(azimuth);
            double y = Math.sin(elevation);
            double z = Math.cos(elevation) * Math.sin(azimuth);
            double along = x * sun[0] + y * sun[1] + z * sun[2];
            double projected = Math.sqrt(Math.max(0.0, 1.0 - along * along));
            double rotation = rotationSpread > 0.0 && parry ? rotationSpread * gaussian(random) : 0.0;
            axes[step].set(x, y, z, projected, rotation);
            total += projected;
        }
        for (int step = 0; step < COLUMN_AXES; step++) {
            Axis axis = axes[step];
            axis.set(axis.x(), axis.y(), axis.z(), axis.weight() / total, axis.rotation());
        }
        return COLUMN_AXES;
    }

    private static void splat(double[] map, double x, double y, double z, double weight) {
        int bin = IceHaloMap.binOf(x, y, z);
        if (bin >= 0) {
            map[bin] += weight * IceHaloMap.inverseBinSolidAngle(bin);
        }
    }

    /**
     * The cone base for side face light, the sun itself or its mirror image in the end faces, followed by the
     * component along the axis. Returns the reused coneBase array.
     */
    private double[] coneBase(double[] sun, Axis axis, boolean mirrored) {
        double along = axis.x() * sun[0] + axis.y() * sun[1] + axis.z() * sun[2];
        if (!mirrored) {
            coneBase[0] = sun[0];
            coneBase[1] = sun[1];
            coneBase[2] = sun[2];
            coneBase[3] = along;
            return coneBase;
        }
        coneBase[0] = sun[0] - 2.0 * along * axis.x();
        coneBase[1] = sun[1] - 2.0 * along * axis.y();
        coneBase[2] = sun[2] - 2.0 * along * axis.z();
        coneBase[3] = -along;
        return coneBase;
    }

    private static void splatOnCone(double[] map, double[] base, Axis axis, double crossX, double crossY,
            double crossZ, double cosine, double sine, double weight) {
        double parallel = base[3] * (1.0 - cosine);
        splat(map, base[0] * cosine + crossX * sine + axis.x() * parallel,
                base[1] * cosine + crossY * sine + axis.y() * parallel,
                base[2] * cosine + crossZ * sine + axis.z() * parallel, weight);
    }

    private void addSideCones(double[][] maps, double[] sun, Axis axis, boolean mirrored) {
        double along = axis.x() * sun[0] + axis.y() * sun[1] + axis.z() * sun[2];
        double radialSquared = 1.0 - along * along;
        if (radialSquared < 1.0e-8) {
            return;
        }
        for (int channel = 0; channel < CHANNELS; channel++) {
            double index = REFRACTIVE_INDEX[channel];
            double effectiveIndex = Math.sqrt(index * index - along * along) / Math.sqrt(radialSquared);
            data.sideFaceDensities(effectiveIndex, sideFaceDensities[channel]);
        }
        double[] base = coneBase(sun, axis, mirrored);
        double crossX = axis.y() * sun[2] - axis.z() * sun[1];
        double crossY = axis.z() * sun[0] - axis.x() * sun[2];
        double crossZ = axis.x() * sun[1] - axis.y() * sun[0];
        double scale = 0.5 * deviationStep;
        for (int sign = -1; sign <= 1; sign += 2) {
            int pendingBin = -1;
            for (int step = 0; step < deviationCosines.length; step++) {
                double cosine = deviationCosines[step];
                double parallel = base[3] * (1.0 - cosine);
                double centerX = base[0] * cosine + axis.x() * parallel;
                double centerY = base[1] * cosine + axis.y() * parallel;
                double centerZ = base[2] * cosine + axis.z() * parallel;
                double sine = sign * deviationSines[step];
                int bin = IceHaloMap.binOf(centerX + crossX * sine, centerY + crossY * sine, centerZ + crossZ * sine);
                if (bin != pendingBin) {
                    addPendingSideWeights(maps, pendingBin, scale);
                    pendingBin = bin;
                }
                if (bin < 0) {
                    continue;
                }
                int column = step / DEVIATION_STEPS_PER_TABLE_COLUMN;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    pendingSideWeights[channel] += sideWeights[channel] * sideFaceDensities[channel][column];
                }
            }
            addPendingSideWeights(maps, pendingBin, scale);
        }
    }

    /**
     * Adds the side face light gathered for one bin and clears it. Consecutive points on a cone mostly share a bin.
     */
    private void addPendingSideWeights(double[][] maps, int bin, double scale) {
        if (bin >= 0) {
            double factor = scale * IceHaloMap.inverseBinSolidAngle(bin);
            for (int channel = 0; channel < CHANNELS; channel++) {
                maps[channel][bin] += factor * pendingSideWeights[channel];
            }
        }
        for (int channel = 0; channel < CHANNELS; channel++) {
            pendingSideWeights[channel] = 0.0;
        }
    }

    /**
     * The sun's angle around the axis, measured from the given up direction toward the axis cross up.
     */
    private static double sunAngleAroundAxis(double[] sun, Axis axis, double[] up) {
        double upCrossX = axis.y() * up[2] - axis.z() * up[1];
        double upCrossY = axis.z() * up[0] - axis.x() * up[2];
        double upCrossZ = axis.x() * up[1] - axis.y() * up[0];
        return Math.atan2(sun[0] * upCrossX + sun[1] * upCrossY + sun[2] * upCrossZ,
                sun[0] * up[0] + sun[1] * up[1] + sun[2] * up[2]);
    }

    private void addFixedSidePaths(double[] map, double[] sun, Axis axis, double index, double weight,
            boolean mirrored) {
        double along = axis.x() * sun[0] + axis.y() * sun[1] + axis.z() * sun[2];
        double radialSquared = 1.0 - along * along;
        if (radialSquared < 1.0e-8 || weight <= 0.0) {
            return;
        }
        double effectiveIndex = Math.sqrt(index * index - along * along) / Math.sqrt(radialSquared);
        double sunAngle = sunAngleAroundAxis(sun, axis, perpendicularUp(axis));
        double[] base = coneBase(sun, axis, mirrored);
        double crossX = axis.y() * sun[2] - axis.z() * sun[1];
        double crossY = axis.z() * sun[0] - axis.x() * sun[2];
        double crossZ = axis.x() * sun[1] - axis.y() * sun[0];
        prism.clearOutputs();
        prism.trace(effectiveIndex, axis.rotation() - sunAngle, PARRY_ENTRY_SAMPLES, random);
        for (int output = 0; output < prism.outputCount(); output++) {
            splatOnCone(map, base, axis, crossX, crossY, crossZ, prism.outputCosine(output), prism.outputSine(output),
                    weight * prism.outputWeight(output));
        }
    }

    private void addEndToEnd(double[] map, double[] sun, Axis axis, double index, double weight, double halfHeight,
            int rotations, boolean parry) {
        double along = axis.x() * sun[0] + axis.y() * sun[1] + axis.z() * sun[2];
        double cosineIncident = Math.abs(along);
        if (cosineIncident < 1.0e-6 || weight <= 0.0) {
            return;
        }
        double horizontalInside = Math.sqrt(1.0 - cosineIncident * cosineIncident) / index;
        double axialInside = Math.sqrt(1.0 - horizontalInside * horizontalInside);
        double travel = 2.0 * halfHeight * horizontalInside / axialInside;
        double sunAngle = sunAngleAroundAxis(sun, axis, perpendicularUp(axis));
        double[] base = coneBase(sun, axis, false);
        double crossX = axis.y() * sun[2] - axis.z() * sun[1];
        double crossY = axis.z() * sun[0] - axis.x() * sun[2];
        double crossZ = axis.x() * sun[1] - axis.y() * sun[0];
        prism.clearOutputs();
        for (int rotationSample = 0; rotationSample < rotations; rotationSample++) {
            double rotation = parry ? axis.rotation() - sunAngle : random.next() * Math.PI / 3.0;
            int entrySamples = parry ? PARRY_MIRROR_ENTRY_SAMPLES : MIRROR_ENTRY_SAMPLES;
            prism.traceMirrors(rotation, travel, entrySamples, random, index, horizontalInside);
        }
        double total = 0.0;
        for (int output = 0; output < prism.outputCount(); output++) {
            total += prism.outputWeight(output);
        }
        if (total <= 0.0) {
            return;
        }
        for (int output = 0; output < prism.outputCount(); output++) {
            splatOnCone(map, base, axis, crossX, crossY, crossZ, prism.outputCosine(output), prism.outputSine(output),
                    weight * prism.outputWeight(output) / total);
        }
    }

    /**
     * The unit direction perpendicular to the axis that leans most toward the zenith. Returns the reused
     * perpendicular array.
     */
    private double[] perpendicularUp(Axis axis) {
        double x = -axis.y() * axis.x();
        double y = 1.0 - axis.y() * axis.y();
        double z = -axis.y() * axis.z();
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0e-9) {
            perpendicular[0] = 1.0;
            perpendicular[1] = 0.0;
            perpendicular[2] = 0.0;
            return perpendicular;
        }
        perpendicular[0] = x / length;
        perpendicular[1] = y / length;
        perpendicular[2] = z / length;
        return perpendicular;
    }

    private void addEndToSide(double[] map, double[] sun, Axis axis, double index, double weight,
            boolean parry) {
        double along = axis.x() * sun[0] + axis.y() * sun[1] + axis.z() * sun[2];
        if (Math.abs(along) < 1.0e-6 || weight <= 0.0) {
            return;
        }
        double sign = Math.signum(along);
        double normalX = sign * axis.x();
        double normalY = sign * axis.y();
        double normalZ = sign * axis.z();
        double cosineIncident = Math.abs(along);
        double inverseIndex = 1.0 / index;
        double cosineTransmitted = Math.sqrt(1.0 - inverseIndex * inverseIndex
                * (1.0 - cosineIncident * cosineIncident));
        double scale = inverseIndex * cosineIncident - cosineTransmitted;
        double insideX = -inverseIndex * sun[0] + scale * normalX;
        double insideY = -inverseIndex * sun[1] + scale * normalY;
        double insideZ = -inverseIndex * sun[2] + scale * normalZ;
        double[] first = perpendicularUp(axis);
        double secondX = axis.y() * first[2] - axis.z() * first[1];
        double secondY = axis.z() * first[0] - axis.x() * first[2];
        double secondZ = axis.x() * first[1] - axis.y() * first[0];
        int faceCount = parry ? 6 : FACE_ANGLE_STEPS;
        double total = 0.0;
        for (int face = 0; face < faceCount; face++) {
            faceWeights[face] = 0.0;
            double angleCosine;
            double angleSine;
            if (parry) {
                double angle = axis.rotation() + face * Math.PI / 3.0;
                angleCosine = Math.cos(angle);
                angleSine = Math.sin(angle);
            } else {
                angleCosine = FACE_ANGLE_COSINES[face];
                angleSine = FACE_ANGLE_SINES[face];
            }
            double faceX = angleCosine * first[0] + angleSine * secondX;
            double faceY = angleCosine * first[1] + angleSine * secondY;
            double faceZ = angleCosine * first[2] + angleSine * secondZ;
            double cosine = insideX * faceX + insideY * faceY + insideZ * faceZ;
            if (cosine <= 0.0) {
                continue;
            }
            double sineSquared = index * index * (1.0 - cosine * cosine);
            if (sineSquared >= 1.0) {
                continue;
            }
            double cosineOut = Math.sqrt(1.0 - sineSquared);
            double transmitted = 1.0 - IcePrism.fresnelReflectance(index, 1.0, cosine, cosineOut);
            double exitScale = cosineOut - index * cosine;
            faceViewX[face] = -(index * insideX + exitScale * faceX);
            faceViewY[face] = -(index * insideY + exitScale * faceY);
            faceViewZ[face] = -(index * insideZ + exitScale * faceZ);
            faceWeights[face] = cosine * transmitted;
            total += faceWeights[face];
        }
        if (total <= 0.0) {
            return;
        }
        for (int face = 0; face < faceCount; face++) {
            if (faceWeights[face] > 0.0) {
                splat(map, faceViewX[face], faceViewY[face], faceViewZ[face], weight * faceWeights[face] / total);
            }
        }
    }
}
