/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.geometry;

import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Minecraft {@link VoxelShape} into block-local blocker boxes.
 * Boxes are intersected with the unit block before {@link
 * ConservativeAirGeometry.UnitBox} construction because valid Minecraft
 * shapes, such as fences, may extend outside that range.
 */
public final class VoxelShapeUnitBoxAdapter {
    private VoxelShapeUnitBoxAdapter() {
    }

    public enum Status {
        RESOLVED,
        CONSERVATIVE_UNSUPPORTED
    }

    public record Adaptation(
            Status status,
            List<ConservativeAirGeometry.UnitBox> blockers
    ) {
        public Adaptation {
            if (status == null || blockers == null) {
                throw new IllegalArgumentException("adaptation fields are required");
            }
            blockers = List.copyOf(blockers);
            if (status == Status.CONSERVATIVE_UNSUPPORTED
                    && !blockers.isEmpty()) {
                throw new IllegalArgumentException(
                        "unsupported adaptation cannot expose blockers");
            }
        }
    }

    /**
     * Enumerates the shape's exact AABB union and clips every box to the unit
     * block. No sampling or aperture threshold is used.
     */
    public static Adaptation adapt(VoxelShape shape) {
        if (shape == null) {
            throw new IllegalArgumentException("shape is required");
        }

        List<ConservativeAirGeometry.UnitBox> blockers = new ArrayList<>();
        try {
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                if (!validBounds(minX, minY, minZ, maxX, maxY, maxZ)) {
                    throw InvalidBoxBounds.INSTANCE;
                }

                double clippedMinX = Math.max(0.0D, minX);
                double clippedMinY = Math.max(0.0D, minY);
                double clippedMinZ = Math.max(0.0D, minZ);
                double clippedMaxX = Math.min(1.0D, maxX);
                double clippedMaxY = Math.min(1.0D, maxY);
                double clippedMaxZ = Math.min(1.0D, maxZ);
                if (clippedMinX < clippedMaxX
                        && clippedMinY < clippedMaxY
                        && clippedMinZ < clippedMaxZ) {
                    blockers.add(new ConservativeAirGeometry.UnitBox(
                            clippedMinX,
                            clippedMinY,
                            clippedMinZ,
                            clippedMaxX,
                            clippedMaxY,
                            clippedMaxZ
                    ));
                }
            });
        } catch (InvalidBoxBounds ignored) {
            return unsupported();
        } catch (RuntimeException ignored) {
            return unsupported();
        }

        return new Adaptation(Status.RESOLVED, blockers);
    }

    private static Adaptation unsupported() {
        return new Adaptation(Status.CONSERVATIVE_UNSUPPORTED, List.of());
    }

    private static boolean validBounds(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        return Double.isFinite(minX)
                && Double.isFinite(minY)
                && Double.isFinite(minZ)
                && Double.isFinite(maxX)
                && Double.isFinite(maxY)
                && Double.isFinite(maxZ)
                && minX <= maxX
                && minY <= maxY
                && minZ <= maxZ;
    }

    private static final class InvalidBoxBounds extends RuntimeException {
        private static final InvalidBoxBounds INSTANCE = new InvalidBoxBounds();

        private InvalidBoxBounds() {
            super(null, null, false, false);
        }
    }
}
