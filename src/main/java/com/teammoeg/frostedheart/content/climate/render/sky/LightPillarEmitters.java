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

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The blocks whose light can show a pillar, and their color and strength.
 * A block state shows one when its light emission is at least MINIMUM_EMISSION. Its strength per channel is the square
 * of its emission over 15 times its color, a linear color whose brightest channel is its intensity factor.
 * The color comes from the override table assets/frostedheart/datasets/light_pillars/emitters.csv, matched by block id
 * first and then by tag in table order, and otherwise from the block's particle texture with intensity factor 1.
 */
public final class LightPillarEmitters {
    /**
     * The lowest light emission that shows a pillar. A light of 3 would show a pillar of 1/25 of a full light's.
     */
    public static final int MINIMUM_EMISSION = 4;
    private static final String FOLDER = "light_pillars";
    private static final String FILE_NAME = "emitters.csv";
    private static final int CHANNELS = 3;
    private static final float OPAQUE_ALPHA = 128.0F;

    @Nullable
    private static LightPillarEmitters overrides;

    private final Map<ResourceLocation, float[]> blockColors;
    private final List<TagKey<Block>> tags;
    private final List<float[]> tagColors;

    private LightPillarEmitters(Map<ResourceLocation, float[]> blockColors, List<TagKey<Block>> tags,
            List<float[]> tagColors) {
        this.blockColors = blockColors;
        this.tags = tags;
        this.tagColors = tagColors;
    }

    @SuppressWarnings("deprecation")
    public static boolean showsPillar(BlockState state) {
        return state.getLightEmission() >= MINIMUM_EMISSION;
    }

    /**
     * The strength of a light with the emission relative to a light of 15, before its color.
     */
    public static float strength(int emission) {
        float relative = emission / 15.0F;
        return relative * relative;
    }

    /**
     * The override table, loaded at the first call.
     */
    public static LightPillarEmitters overrides() {
        if (overrides == null) {
            overrides = load();
        }
        return overrides;
    }

    /**
     * The color of the state from the override table, or null when the table has no row for it.
     * The returned array must not be changed.
     */
    @Nullable
    public float[] overrideColor(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        float[] color = id != null ? blockColors.get(id) : null;
        if (color != null) {
            return color;
        }
        for (int row = 0; row < tags.size(); row++) {
            if (state.is(tags.get(row))) {
                return tagColors.get(row);
            }
        }
        return null;
    }

    /**
     * Writes the average color of a texture's opaque pixels into the result, normalized so its brightest channel is 1.
     * Pixels are packed as NativeImage holds them, with red in the lowest byte and alpha in the highest.
     * Each pixel is weighted by the fourth power of its brightest sRGB channel, so a torch gives the color of its flame
     * and not of its stick. The colors are averaged in linear light. A texture without an opaque pixel that is not black
     * gives white.
     */
    public static void averageTextureColor(int[] pixels, int count, float[] result) {
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        for (int index = 0; index < count; index++) {
            int pixel = pixels[index];
            if ((pixel >>> 24) < OPAQUE_ALPHA) {
                continue;
            }
            int pixelRed = pixel & 0xFF;
            int pixelGreen = (pixel >>> 8) & 0xFF;
            int pixelBlue = (pixel >>> 16) & 0xFF;
            double brightest = Math.max(pixelRed, Math.max(pixelGreen, pixelBlue)) / 255.0;
            double weight = brightest * brightest * brightest * brightest;
            red += weight * decodeSrgb(pixelRed / 255.0);
            green += weight * decodeSrgb(pixelGreen / 255.0);
            blue += weight * decodeSrgb(pixelBlue / 255.0);
        }
        double brightest = Math.max(red, Math.max(green, blue));
        if (!(brightest > 0.0)) {
            result[0] = 1.0F;
            result[1] = 1.0F;
            result[2] = 1.0F;
            return;
        }
        result[0] = (float) (red / brightest);
        result[1] = (float) (green / brightest);
        result[2] = (float) (blue / brightest);
    }

    private static double decodeSrgb(double encoded) {
        return encoded <= 0.04045 ? encoded / 12.92 : Math.pow((encoded + 0.055) / 1.055, 2.4);
    }

    /**
     * Reads the override table. Each row holds a block id or a tag starting with #, a linear color in any scale, and
     * an intensity factor. The color is normalized to a brightest channel of 1 and multiplied by the factor.
     */
    private static LightPillarEmitters load() {
        Map<ResourceLocation, float[]> blockColors = new HashMap<>();
        List<TagKey<Block>> tags = new ArrayList<>();
        List<float[]> tagColors = new ArrayList<>();
        List<String[]> rows = readCsv();
        for (int row = 1; row < rows.size(); row++) {
            String[] cells = rows.get(row);
            float red = number(cells[1]);
            float green = number(cells[2]);
            float blue = number(cells[3]);
            float brightest = Math.max(red, Math.max(green, blue));
            if (!(brightest > 0.0F)) {
                throw new IllegalStateException("The light pillar override for " + cells[0] + " has no color");
            }
            float intensity = number(cells[4]);
            if (!(intensity > 0.0F)) {
                throw new IllegalStateException("The light pillar override for " + cells[0]
                        + " has an intensity that is not above 0");
            }
            float scale = intensity / brightest;
            float[] color = {red * scale, green * scale, blue * scale};
            String target = cells[0].trim();
            if (target.startsWith("#")) {
                tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(target.substring(1))));
                tagColors.add(color);
            } else {
                blockColors.put(new ResourceLocation(target), color);
            }
        }
        return new LightPillarEmitters(blockColors, tags, tagColors);
    }

    private static float number(String cell) {
        return Float.parseFloat(cell.trim());
    }

    private static List<String[]> readCsv() {
        Path path = ModList.get().getModFileById(FHMain.MODID).getFile()
                .findResource("assets", FHMain.MODID, "datasets", FOLDER, FILE_NAME);
        String text;
        try {
            text = Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the light pillar dataset " + FILE_NAME, exception);
        }
        List<String[]> rows = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                String[] cells = line.split(",");
                if (cells.length != CHANNELS + 2) {
                    throw new IllegalStateException("The light pillar dataset row \"" + line + "\" does not have "
                            + (CHANNELS + 2) + " cells");
                }
                rows.add(cells);
            }
        }
        return rows;
    }
}
