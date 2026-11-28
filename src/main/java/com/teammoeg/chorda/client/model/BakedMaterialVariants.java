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

package com.teammoeg.chorda.client.model;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.FastColor;
import net.minecraftforge.client.model.IQuadTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Client-render-thread cache for one immutable baked mesh component and one authored tint slot.
 * The selected slot is a bake-time marker, not an ItemColor: it is consumed and never applied
 * again at draw time. Other faces (including unrelated tint slots) are passed through by identity.
 *
 * Use one instance per stable list of baked faces, shared by all displays of that component.
 * Treat the input vertex arrays as immutable: uncolored geometry shares them with the source.
 * Recreate it with the mesh on model rebakes; clear colors when the caller's palette is replaced.
 *
 * @param <T> caller's immutable view, for example a BakedModel or the baked quad list itself
 */
public final class BakedMaterialVariants<T> {
    public static final int WHITE = 0xFFFFFF;

    private final List<BakedQuad> neutralQuads;
    private final T neutral;
    private final int[] marked;
    private final Function<List<BakedQuad>, T> wrap;
    private final Int2ObjectOpenHashMap<T> colors = new Int2ObjectOpenHashMap<>();

    /**
     * Binds a nonnegative authored tint index without copying any packed vertex arrays.
     * The adapter wraps the neutral list now, then a new immutable list on each nonwhite miss.
     */
    public BakedMaterialVariants(List<BakedQuad> source, int markerTintIndex, Function<List<BakedQuad>, T> wrap) {
        if (markerTintIndex < 0) throw new IllegalArgumentException("Use a nonnegative material marker");
        List<BakedQuad> base = new ArrayList<>(source);
        int[] positions = new int[base.size()];
        int count = 0;
        for (int index = 0; index < base.size(); index++) {
            BakedQuad quad = base.get(index);
            if (quad.getTintIndex() == markerTintIndex) {
                positions[count++] = index;
                base.set(index, withoutTint(quad, quad.getVertices()));
            }
        }
        this.neutralQuads = List.copyOf(base);
        this.neutral = wrap.apply(neutralQuads);
        this.marked = Arrays.copyOf(positions, count);
        this.wrap = wrap;
    }

    /** White is the original packed geometry, with the marker removed; only nonwhite misses clone vertex data. */
    public T resolve(int rgb) {
        rgb &= WHITE;
        if (rgb == WHITE || marked.length == 0) return neutral;
        T ready = colors.get(rgb);
        if (ready != null) return ready;

        int red = FastColor.ARGB32.red(rgb);
        int green = FastColor.ARGB32.green(rgb);
        int blue = FastColor.ARGB32.blue(rgb);
        List<BakedQuad> result = new ArrayList<>(neutralQuads);
        for (int index : marked) {
            BakedQuad quad = neutralQuads.get(index);
            int[] vertices = quad.getVertices().clone();
            for (int off = IQuadTransformer.COLOR; off < vertices.length; off += IQuadTransformer.STRIDE) {
                int base = vertices[off];
                // Model vertex slots are ABGR; material values are standard 0xRRGGBB.
                vertices[off] = FastColor.ABGR32.color(FastColor.ABGR32.alpha(base),
                        multiply(FastColor.ABGR32.blue(base), blue),
                        multiply(FastColor.ABGR32.green(base), green),
                        multiply(FastColor.ABGR32.red(base), red));
            }
            result.set(index, withoutTint(quad, vertices));
        }
        ready = wrap.apply(List.copyOf(result));
        colors.put(rgb, ready);
        return ready;
    }

    /** Drops only derived colors; the neutral geometry and the source mask remain stable. */
    public void clearColors() {
        colors.clear();
    }

    private static int multiply(int a, int b) {
        return (a * b + 127) / 255;
    }

    private static BakedQuad withoutTint(BakedQuad quad, int[] vertices) {
        return new BakedQuad(vertices, -1, quad.getDirection(), quad.getSprite(),
                quad.isShade(), quad.hasAmbientOcclusion());
    }
}
