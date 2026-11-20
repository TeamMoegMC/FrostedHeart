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

import javax.annotation.Nullable;

/**
 * Everything the calculated sky is drawn from. The low halo map is for the sun elevation of the halo index on its
 * grid, and the high one for the next grid step. Tables and views are on their own grids in the same way, with a view
 * for each pair of depth and sun elevation. The settings are those the build was asked for, of which only the ones
 * that rebuild something were used. The light kind is the one the build was asked for. Halo maps and views depend
 * only on the light's elevation, so a build for the other kind reuses those on the same grid indices.
 */
record SkyState(SkySettings settings, SkyLight.Kind lightKind, IceHaloProfile haloProfile, int haloIndex,
        IceHaloMap lowHalo, IceHaloMap highHalo, int depthIndex, SkyTables lowDepthTables, SkyTables highDepthTables,
        int viewIndex, SkyViewSet views) {
    static final int HALO_COUNT = 2;

    IceHaloMap halo(int slot) {
        return slot == 0 ? lowHalo : highHalo;
    }

    @Nullable
    IceHaloMap haloAt(int index) {
        if (index == haloIndex) {
            return lowHalo;
        }
        if (index == haloIndex + 1) {
            return highHalo;
        }
        return null;
    }

    boolean holdsHalo(IceHaloMap halo) {
        return halo == lowHalo || halo == highHalo;
    }

    @Nullable
    SkyTables tablesAt(int index) {
        if (index == depthIndex) {
            return lowDepthTables;
        }
        if (index == depthIndex + 1) {
            return highDepthTables;
        }
        return null;
    }

    @Nullable
    SkyView viewAt(int depthGridIndex, int viewGridIndex) {
        int depthEnd = depthGridIndex - depthIndex;
        int sunEnd = viewGridIndex - viewIndex;
        if (depthEnd < 0 || depthEnd >= SkyViewSet.ENDS || sunEnd < 0 || sunEnd >= SkyViewSet.ENDS) {
            return null;
        }
        return views.view(SkyViewSet.slot(depthEnd, sunEnd));
    }

    /**
     * Whether the state was built for the light kind and the halo, depth, and view grid indices.
     */
    boolean isFor(SkyLight.Kind kind, int haloGridIndex, int depthGridIndex, int viewGridIndex) {
        return lightKind == kind && haloIndex == haloGridIndex && depthIndex == depthGridIndex
                && viewIndex == viewGridIndex;
    }
}
