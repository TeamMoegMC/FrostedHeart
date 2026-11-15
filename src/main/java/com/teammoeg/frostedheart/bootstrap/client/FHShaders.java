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

package com.teammoeg.frostedheart.bootstrap.client;

import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.client.shader.management.Shader;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderProgram;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class FHShaders extends Shaders {
    private static Shader INFRARED_VIEW;
    public static class RegionShaders{
	    private final Shader fs;
	    private final Shader vs;
	    private RegionShaders(Shader fs, Shader vs) {
			super();
			this.fs = fs;
			this.vs = vs;
		}
	    public void attachShader(ShaderProgram program) {
	    	fs.attachShader(program);
	    	vs.attachShader(program);
	    }

	    public void deleteShader() {
	        fs.deleteShader();
	        vs.deleteShader();
	    }

    }
    private static RegionShaders REGION;
    private static Shader CALCULATED_SKY;
    private static Shader LIGHT_PILLARS_VERTEX;
    private static Shader LIGHT_PILLARS_FRAGMENT;
    private static Shader LIGHT_PILLAR_CELLS_VERTEX;
    private static Shader LIGHT_PILLAR_CELLS_FRAGMENT;
    private static Shader LIGHT_PILLARS_COMPOSITE;
    /**
     * The defines that light_pillars.glsl is compiled with for each stage, in the order of loadLightPillars.
     */
    private static final String[] LIGHT_PILLAR_DEFINES = {"#define VERTEX_SHADER\n", "#define FRAGMENT_SHADER\n",
            "#define VERTEX_SHADER\n#define CELL_ATLAS\n", "#define FRAGMENT_SHADER\n#define CELL_ATLAS\n"};
    private static boolean lightPillarsUnloadOnReload;
    private static Shader DIAMOND_DUST_VERTEX;
    private static Shader DIAMOND_DUST_FRAGMENT;
    private static boolean diamondDustUnloadOnReload;
    public static Shader getCalculatedSky() {
        if (CALCULATED_SKY == null) {
            runAndAddReloadListener(() -> CALCULATED_SKY = load(Shader.ShaderType.FRAGMENT, FHMain.rl("calculated_sky")));
        }
        return CALCULATED_SKY;
    }
    /**
     * The vertex stage of light_pillars.glsl. Both stages come from that one file, compiled with a define for each.
     */
    public static Shader getLightPillarsVertex() {
        loadLightPillars();
        return LIGHT_PILLARS_VERTEX;
    }

    public static Shader getLightPillarsFragment() {
        loadLightPillars();
        return LIGHT_PILLARS_FRAGMENT;
    }

    /**
     * The vertex stage of the pass that fills the light pillar cell atlas, light_pillars.glsl with CELL_ATLAS defined.
     */
    public static Shader getLightPillarCellsVertex() {
        loadLightPillars();
        return LIGHT_PILLAR_CELLS_VERTEX;
    }

    public static Shader getLightPillarCellsFragment() {
        loadLightPillars();
        return LIGHT_PILLAR_CELLS_FRAGMENT;
    }

    /**
     * The full screen stage that tone maps the light pillars summed in linear light, for IMAGE_V.
     */
    public static Shader getLightPillarsComposite() {
        if (LIGHT_PILLARS_COMPOSITE == null) {
            runAndAddReloadListener(() -> LIGHT_PILLARS_COMPOSITE = load(Shader.ShaderType.FRAGMENT,
                    FHMain.rl("light_pillars_composite")));
        }
        return LIGHT_PILLARS_COMPOSITE;
    }

    private static void loadLightPillars() {
        if (!lightPillarsUnloadOnReload) {
            addReloadListener(FHShaders::unloadLightPillars);
            lightPillarsUnloadOnReload = true;
        }
        if (LIGHT_PILLARS_VERTEX == null) {
            String source = readShaderSource(FHMain.rl("shaders/light_pillars.glsl"));
            Shader[] stages = new Shader[LIGHT_PILLAR_DEFINES.length];
            try {
                for (int stage = 0; stage < stages.length; stage++) {
                    Shader.ShaderType type = stage % 2 == 0 ? Shader.ShaderType.VERTEX : Shader.ShaderType.FRAGMENT;
                    stages[stage] = Shader.loadShader(type, "#version 150\n" + LIGHT_PILLAR_DEFINES[stage] + source);
                }
            } catch (RuntimeException exception) {
                for (Shader loaded : stages) {
                    if (loaded != null) {
                        loaded.deleteShader();
                    }
                }
                throw exception;
            }
            LIGHT_PILLARS_VERTEX = stages[0];
            LIGHT_PILLARS_FRAGMENT = stages[1];
            LIGHT_PILLAR_CELLS_VERTEX = stages[2];
            LIGHT_PILLAR_CELLS_FRAGMENT = stages[3];
        }
    }

    /**
     * Deletes the light pillar stages after a resource reload, so the next use compiles them from the new resources.
     */
    private static void unloadLightPillars() {
        if (LIGHT_PILLARS_VERTEX != null) {
            LIGHT_PILLARS_VERTEX.deleteShader();
            LIGHT_PILLARS_FRAGMENT.deleteShader();
            LIGHT_PILLAR_CELLS_VERTEX.deleteShader();
            LIGHT_PILLAR_CELLS_FRAGMENT.deleteShader();
            LIGHT_PILLARS_VERTEX = null;
            LIGHT_PILLARS_FRAGMENT = null;
            LIGHT_PILLAR_CELLS_VERTEX = null;
            LIGHT_PILLAR_CELLS_FRAGMENT = null;
        }
    }

    /**
     * The vertex stage of diamond_dust.glsl. Both stages come from that one file, compiled with a define for each.
     */
    public static Shader getDiamondDustVertex() {
        loadDiamondDust();
        return DIAMOND_DUST_VERTEX;
    }

    public static Shader getDiamondDustFragment() {
        loadDiamondDust();
        return DIAMOND_DUST_FRAGMENT;
    }

    private static void loadDiamondDust() {
        if (!diamondDustUnloadOnReload) {
            addReloadListener(FHShaders::unloadDiamondDust);
            diamondDustUnloadOnReload = true;
        }
        if (DIAMOND_DUST_VERTEX == null) {
            String source = readShaderSource(FHMain.rl("shaders/diamond_dust.glsl"));
            Shader vertex = Shader.loadShader(Shader.ShaderType.VERTEX, "#version 150\n#define VERTEX_SHADER\n" + source);
            Shader fragment;
            try {
                fragment = Shader.loadShader(Shader.ShaderType.FRAGMENT,
                        "#version 150\n#define FRAGMENT_SHADER\n" + source);
            } catch (RuntimeException exception) {
                vertex.deleteShader();
                throw exception;
            }
            DIAMOND_DUST_VERTEX = vertex;
            DIAMOND_DUST_FRAGMENT = fragment;
        }
    }

    /**
     * Deletes the diamond dust stages after a resource reload, so the next use compiles them from the new resources.
     */
    private static void unloadDiamondDust() {
        if (DIAMOND_DUST_VERTEX != null) {
            DIAMOND_DUST_VERTEX.deleteShader();
            DIAMOND_DUST_FRAGMENT.deleteShader();
            DIAMOND_DUST_VERTEX = null;
            DIAMOND_DUST_FRAGMENT = null;
        }
    }

    private static String readShaderSource(ResourceLocation location) {
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
            return reader.lines().collect(Collectors.joining("\n", "", "\n"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read the shader " + location, exception);
        }
    }

    public static Shader getInfraredView() {
        if (INFRARED_VIEW == null) {
        	runAndAddReloadListener(()->INFRARED_VIEW = load(Shader.ShaderType.FRAGMENT, FHMain.rl("infrared_view")));
        }
        return INFRARED_VIEW;
    }
    public static RegionShaders getRegion() {
        if (REGION == null) {
        	runAndAddReloadListener(()->REGION=new RegionShaders(load(Shader.ShaderType.FRAGMENT, FHMain.rl("region_marker")),
        		load(Shader.ShaderType.VERTEX, FHMain.rl("region_marker"))));
        }
        return REGION;
    }
	public static void runAndAddReloadListener(Runnable runnable) {
		runnable.run();
		addReloadListener(runnable);
	}
}
