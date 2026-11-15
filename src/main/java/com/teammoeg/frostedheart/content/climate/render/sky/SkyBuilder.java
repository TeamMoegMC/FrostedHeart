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

import net.minecraft.Util;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Builds the states the calculated sky is drawn from on Minecraft's background executor, one build at a time.
 * Halo maps exist for sun elevations on a grid of HALO_MAP_STEP_DEGREES, tables for ice optical depths on
 * IceDepthGrid, and sky views for each of those depths and sun elevations on a grid of VIEW_STEP_DEGREES.
 * A state holds the two of each around the sun and the ice depth, so only the missing ends are built when the sun or
 * the depth leaves them.
 * Builds reuse one workspace and refill the halo maps and views that the previous state dropped, so following the sun
 * allocates no halo maps or views. Everything except the build itself runs on one thread, and nothing here uses GL.
 */
public final class SkyBuilder {
    private static final double VIEW_STEP_DEGREES = 0.25;
    private static final double LOWEST_VIEW_SUN_DEGREES = -18.0;
    private static final double HIGHEST_VIEW_SUN_DEGREES = 90.0;
    private static final double HALO_MAP_STEP_DEGREES = 0.5;
    private static final double LOWEST_HALO_SUN_DEGREES = -6.0;
    private static final double HIGHEST_HALO_SUN_DEGREES = 90.0;

    @Nullable
    private SkyState state;
    @Nullable
    private CompletableFuture<BuildResult> pendingBuild;
    /**
     * Created and used only inside build, and only one build runs at a time.
     */
    @Nullable
    private IceHaloMap.Workspace haloWorkspace;
    /**
     * Halo maps and views that no state uses any more, for the next build to fill. The calling thread owns them except
     * while a build runs, which takes from them.
     */
    private final IceHaloMap[] spareHalos = new IceHaloMap[SkyState.HALO_COUNT];
    private int spareHaloCount;
    private final SkyView[] spareViews = new SkyView[SkyViewSet.VIEW_COUNT];
    private int spareViewCount;
    /**
     * Build times of the running build, written only by the build and read after it finished.
     */
    private double buildTablesMilliseconds;
    private double buildHaloMapMilliseconds;
    private double buildViewMilliseconds;
    private double lastTablesMilliseconds = Double.NaN;
    private double lastViewMilliseconds = Double.NaN;
    private double lastHaloProfileMilliseconds = Double.NaN;
    private double lastHaloMapMilliseconds = Double.NaN;

    private record BuildResult(SkyState state, double tablesMilliseconds, double haloProfileMilliseconds,
            double haloMapMilliseconds, double viewMilliseconds) {
    }

    SkyBuilder() {
    }

    static int haloGridIndex(double sunElevationRadians) {
        return gridIndex(sunElevationRadians, HALO_MAP_STEP_DEGREES, LOWEST_HALO_SUN_DEGREES,
                HIGHEST_HALO_SUN_DEGREES);
    }

    static int viewGridIndex(double sunElevationRadians) {
        return gridIndex(sunElevationRadians, VIEW_STEP_DEGREES, LOWEST_VIEW_SUN_DEGREES, HIGHEST_VIEW_SUN_DEGREES);
    }

    /**
     * The index on a grid of sun elevations at or below the sun, clamped so that the next index is still on the grid.
     */
    private static int gridIndex(double sunElevationRadians, double stepDegrees, double lowestDegrees,
            double highestDegrees) {
        int lowest = (int) Math.round(lowestDegrees / stepDegrees);
        int highest = (int) Math.round(highestDegrees / stepDegrees) - 1;
        int index = (int) Math.floor(Math.toDegrees(sunElevationRadians) / stepDegrees);
        return Math.max(lowest, Math.min(highest, index));
    }

    private static double gridSunElevation(int index, double stepDegrees) {
        return Math.toRadians(index * stepDegrees);
    }

    /**
     * The state of the last build that was installed, or null before the first one.
     */
    @Nullable
    SkyState state() {
        return state;
    }

    boolean isBuilding() {
        return pendingBuild != null;
    }

    /**
     * Returns the state of the pending build once it finished and forgets the build, or null while none is pending
     * or it still runs. A build that failed throws its CompletionException here.
     */
    @Nullable
    SkyState takeFinished() {
        if (pendingBuild == null || !pendingBuild.isDone()) {
            return null;
        }
        CompletableFuture<BuildResult> finished = pendingBuild;
        pendingBuild = null;
        BuildResult result = finished.join();
        if (!Double.isNaN(result.tablesMilliseconds())) {
            lastTablesMilliseconds = result.tablesMilliseconds();
        }
        if (!Double.isNaN(result.haloProfileMilliseconds())) {
            lastHaloProfileMilliseconds = result.haloProfileMilliseconds();
        }
        if (!Double.isNaN(result.haloMapMilliseconds())) {
            lastHaloMapMilliseconds = result.haloMapMilliseconds();
        }
        if (!Double.isNaN(result.viewMilliseconds())) {
            lastViewMilliseconds = result.viewMilliseconds();
        }
        return result.state();
    }

    /**
     * Makes a finished state the current one, and keeps the halo maps and views of the previous state that it no
     * longer uses for the following builds to fill.
     */
    void install(SkyState next) {
        keepSpares(state, next);
        state = next;
    }

    /**
     * Starts a build for the settings, light kind, and grid indices unless one runs already or the current state is
     * built for them. The build reuses what the current state has, apart from what a changed setting rebuilds.
     */
    void request(SkySettings settings, SkyLight.Kind lightKind, int haloIndex, int depthIndex, int viewIndex) {
        SkyState current = state;
        if (pendingBuild != null) {
            return;
        }
        if (current != null && current.isFor(lightKind, haloIndex, depthIndex, viewIndex)
                && settings.sameBuild(current.settings())) {
            return;
        }
        pendingBuild = CompletableFuture.supplyAsync(
                () -> build(current, settings, lightKind, haloIndex, depthIndex, viewIndex), Util.backgroundExecutor());
    }

    double lastTablesMilliseconds() {
        return lastTablesMilliseconds;
    }

    double lastViewMilliseconds() {
        return lastViewMilliseconds;
    }

    double lastHaloProfileMilliseconds() {
        return lastHaloProfileMilliseconds;
    }

    double lastHaloMapMilliseconds() {
        return lastHaloMapMilliseconds;
    }

    /**
     * Keeps the halo maps and views of the previous state that the next state no longer uses, so following builds
     * fill them instead of allocating. Runs after the build finished, when nothing reads the previous state any more.
     */
    private void keepSpares(@Nullable SkyState previous, SkyState next) {
        if (previous == null) {
            return;
        }
        for (int slot = 0; slot < SkyState.HALO_COUNT; slot++) {
            IceHaloMap halo = previous.halo(slot);
            if (!next.holdsHalo(halo) && spareHaloCount < SkyState.HALO_COUNT) {
                spareHalos[spareHaloCount++] = halo;
            }
        }
        for (int slot = 0; slot < SkyViewSet.VIEW_COUNT; slot++) {
            SkyView view = previous.views().view(slot);
            if (!next.views().holds(view) && spareViewCount < SkyViewSet.VIEW_COUNT) {
                spareViews[spareViewCount++] = view;
            }
        }
    }

    /**
     * Builds the next state, reusing what the previous state already has and filling spare halo maps and views where
     * there are any. Tables are built again when a setting that rebuilds tables changed, the halo profile and maps
     * when one that rebuilds halos changed, and the views when either did. Without a previous state everything is
     * built for the given settings.
     */
    private BuildResult build(@Nullable SkyState previous, SkySettings settings, SkyLight.Kind lightKind,
            int haloIndex, int depthIndex, int viewIndex) {
        buildTablesMilliseconds = Double.NaN;
        buildHaloMapMilliseconds = Double.NaN;
        buildViewMilliseconds = Double.NaN;
        SkyState tablesSource = previous != null
                && settings.samePart(previous.settings(), SkySetting.Rebuilds.TABLES) ? previous : null;
        SkyState halosSource = previous != null
                && settings.samePart(previous.settings(), SkySetting.Rebuilds.HALOS) ? previous : null;
        SkyState viewsSource = tablesSource != null && halosSource != null ? previous : null;
        long start = System.nanoTime();
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(settings.actualTexelDegrees());
        IceHaloProfile haloProfile = halosSource != null ? halosSource.haloProfile()
                : new IceHaloProfile(data, blurSigma);
        double haloProfileMilliseconds = halosSource != null ? Double.NaN : (System.nanoTime() - start) / 1.0e6;
        if (haloWorkspace == null) {
            haloWorkspace = new IceHaloMap.Workspace(data);
        }

        IceHaloMap lowHalo = haloFor(halosSource, haloIndex, blurSigma);
        IceHaloMap highHalo = haloFor(halosSource, haloIndex + 1, blurSigma);
        SkyTables lowDepthTables = tablesFor(tablesSource, depthIndex, settings);
        SkyTables highDepthTables = tablesFor(tablesSource, depthIndex + 1, settings);
        SkyViewSet views = new SkyViewSet();
        for (int depthEnd = 0; depthEnd < SkyViewSet.ENDS; depthEnd++) {
            SkyTables tables = depthEnd == 0 ? lowDepthTables : highDepthTables;
            for (int sunEnd = 0; sunEnd < SkyViewSet.ENDS; sunEnd++) {
                views.setView(SkyViewSet.slot(depthEnd, sunEnd), viewFor(viewsSource, depthIndex + depthEnd,
                        viewIndex + sunEnd, tables, haloProfile, lowHalo, highHalo));
            }
        }
        SkyState next = new SkyState(settings, lightKind, haloProfile, haloIndex, lowHalo, highHalo, depthIndex,
                lowDepthTables, highDepthTables, viewIndex, views);
        return new BuildResult(next, buildTablesMilliseconds, haloProfileMilliseconds, buildHaloMapMilliseconds,
                buildViewMilliseconds);
    }

    private IceHaloMap haloFor(@Nullable SkyState previous, int index, double blurSigma) {
        IceHaloMap halo = previous != null ? previous.haloAt(index) : null;
        if (halo != null) {
            return halo;
        }
        long start = System.nanoTime();
        if (spareHaloCount > 0) {
            halo = spareHalos[--spareHaloCount];
            spareHalos[spareHaloCount] = null;
        } else {
            halo = new IceHaloMap();
        }
        halo.build(haloWorkspace, gridSunElevation(index, HALO_MAP_STEP_DEGREES), blurSigma);
        buildHaloMapMilliseconds = (System.nanoTime() - start) / 1.0e6;
        return halo;
    }

    private SkyTables tablesFor(@Nullable SkyState previous, int depthGridIndex, SkySettings settings) {
        SkyTables tables = previous != null ? previous.tablesAt(depthGridIndex) : null;
        if (tables != null) {
            return tables;
        }
        long start = System.nanoTime();
        tables = new SkyTables(settings.withIceOpticalDepth(IceDepthGrid.depth(depthGridIndex)));
        buildTablesMilliseconds = (System.nanoTime() - start) / 1.0e6;
        return tables;
    }

    /**
     * Returns the previous state's view for the grid indices, or fills a spare or new view for them from the tables,
     * with the light that ice scatters onto the ground from the nearer halo map.
     */
    private SkyView viewFor(@Nullable SkyState previous, int depthGridIndex, int viewGridIndex, SkyTables tables,
            IceHaloProfile haloProfile, IceHaloMap lowHalo, IceHaloMap highHalo) {
        SkyView view = previous != null ? previous.viewAt(depthGridIndex, viewGridIndex) : null;
        if (view != null) {
            return view;
        }
        long start = System.nanoTime();
        if (spareViewCount > 0) {
            view = spareViews[--spareViewCount];
            spareViews[spareViewCount] = null;
        } else {
            view = new SkyView();
        }
        double sunElevation = gridSunElevation(viewGridIndex, VIEW_STEP_DEGREES);
        double lowDistance = Math.abs(sunElevation - lowHalo.sunElevationRadians());
        double highDistance = Math.abs(sunElevation - highHalo.sunElevationRadians());
        tables.computeView(sunElevation, haloProfile, lowDistance <= highDistance ? lowHalo : highHalo, view);
        buildViewMilliseconds = (System.nanoTime() - start) / 1.0e6;
        return view;
    }
}
