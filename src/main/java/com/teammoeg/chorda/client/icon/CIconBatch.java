/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.chorda.client.icon;

import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.TesselateHelper.TextureTesselator;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.CGuiHelper.ItemRenderRequest;
import com.teammoeg.chorda.client.ui.CGuiHelper.ItemBatchPlanner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reusable ordered icon pass. Item icons are submitted in compatible lighting
 * segments, while unsupported icon types form an immediate-render barrier.
 * Non-overlapping callers may opt into layer ordering, which also delays texture
 * icons and groups contiguous requests that use the same texture.
 */
public final class CIconBatch {
    private static final long NO_TEXTURE_LAYER = Long.MAX_VALUE;

    public enum Ordering {
        SUBMISSION_ORDER,
        LAYER_THEN_LIGHTING
    }

    private final List<ItemRenderRequest> itemRequests = new ArrayList<>();
    private final List<TextureRenderRequest> textureRequests = new ArrayList<>();
    private final ItemBatchPlanner itemBatchPlanner = new ItemBatchPlanner();
    private GuiGraphics graphics;
    private int itemRequestCount;
    private int textureRequestCount;
    private int zOffset;
    private int flushCount;
    private boolean drawDecorations;
    private Ordering ordering = Ordering.SUBMISSION_ORDER;
    private boolean active;

    public void begin(GuiGraphics graphics, boolean drawDecorations) {
        begin(graphics, drawDecorations, Ordering.SUBMISSION_ORDER);
    }

    public void begin(GuiGraphics graphics, boolean drawDecorations, Ordering ordering) {
        if (active) {
            throw new IllegalStateException("Icon batch is already active");
        }
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        this.drawDecorations = drawDecorations;
        this.ordering = Objects.requireNonNull(ordering, "ordering");
        itemRequestCount = 0;
        textureRequestCount = 0;
        zOffset = 0;
        flushCount = 0;
        active = true;
    }

    public void draw(CIcon icon, int x, int y, int width, int height) {
        requireActive();
        if (icon != null) {
            icon.drawBatched(this, graphics, x, y, width, height);
        }
    }

    public int end() {
        requireActive();
        try {
            flushQueued();
            return flushCount;
        } finally {
            graphics = null;
            itemRequestCount = 0;
            textureRequestCount = 0;
            zOffset = 0;
            active = false;
        }
    }

    void submitItem(ItemStack stack, int x, int y, int width, int height, int zIndex) {
        if (drawDecorations) {
            drawItemImmediately(stack, x, y, width, height, zIndex);
            return;
        }
        ItemRenderRequest request;
        if (itemRequestCount < itemRequests.size()) {
            request = itemRequests.get(itemRequestCount);
        } else {
            request = new ItemRenderRequest();
            itemRequests.add(request);
        }
        request.set(stack, x, y, zIndex + zOffset, width / 16.0F, height / 16.0F);
        itemRequestCount++;
    }

    void submitTexture(CTextureIcon icon, int x, int y, int width, int height) {
        if (ordering != Ordering.LAYER_THEN_LIGHTING) {
            drawImmediately(icon, graphics, x, y, width, height);
            return;
        }
        TextureRenderRequest request;
        if (textureRequestCount < textureRequests.size()) {
            request = textureRequests.get(textureRequestCount);
        } else {
            request = new TextureRenderRequest();
            textureRequests.add(request);
        }
        request.set(icon, x, y, width, height, zOffset);
        textureRequestCount++;
    }

    void drawImmediately(CIcon icon, GuiGraphics graphics, int x, int y, int width, int height) {
        flushQueued();
        graphics.flush();
        graphics.pose().pushPose();
        try {
            if (zOffset != 0) {
                graphics.pose().translate(0.0F, 0.0F, zOffset);
            }
            icon.draw(graphics, x, y, width, height);
        } finally {
            graphics.pose().popPose();
            graphics.flush();
            flushCount++;
        }
    }

    void pushZ(int amount) {
        zOffset += amount;
    }

    void popZ(int amount) {
        zOffset -= amount;
    }

    private void drawItemImmediately(ItemStack stack, int x, int y, int width, int height, int zIndex) {
        flushQueued();
        CGuiHelper.drawItem(
                graphics,
                stack,
                x,
                y,
                zIndex + zOffset,
                width / 16.0F,
                height / 16.0F,
                true,
                null);
        flushCount++;
    }

    private void flushQueued() {
        if (itemRequestCount == 0 && textureRequestCount == 0) {
            return;
        }
        try {
            boolean itemBufferFlushed = false;
            if (itemRequestCount > 0) {
                int itemSubmissions = ordering == Ordering.LAYER_THEN_LIGHTING
                        ? CGuiHelper.drawItemsByLayerAndLighting(graphics, itemRequests, itemRequestCount)
                        : CGuiHelper.drawItems(graphics, itemRequests, itemRequestCount, itemBatchPlanner);
                flushCount += itemSubmissions;
                itemBufferFlushed = itemSubmissions > 0;
            }
            if (textureRequestCount > 0) {
                flushCount += drawTexturesByLayer(!itemBufferFlushed);
            }
        } finally {
            itemRequestCount = 0;
            textureRequestCount = 0;
        }
    }

    private int drawTexturesByLayer(boolean flushPendingGraphics) {
        if (flushPendingGraphics) {
            graphics.flush();
        }
        CGuiHelper.resetGuiDrawing();
        try {
            return drawTextureLayers();
        } finally {
            CGuiHelper.resetGuiDrawing();
        }
    }

    private int drawTextureLayers() {
        int submissions = 0;
        long previousLayer = Long.MIN_VALUE;
        while (true) {
            long nextLayer = findNextTextureLayer(previousLayer);
            if (nextLayer == NO_TEXTURE_LAYER) {
                return submissions;
            }
            submissions += drawTextureLayer((int) nextLayer);
            previousLayer = nextLayer;
        }
    }

    private long findNextTextureLayer(long previousLayer) {
        long nextLayer = NO_TEXTURE_LAYER;
        for (int i = 0; i < textureRequestCount; i++) {
            int layer = textureRequests.get(i).zIndex;
            if (layer > previousLayer && layer < nextLayer) {
                nextLayer = layer;
            }
        }
        return nextLayer;
    }

    private int drawTextureLayer(int layer) {
        int submissions = 0;
        int cursor = 0;
        while (true) {
            int firstIndex = findTextureIndexAtLayer(cursor, layer);
            if (firstIndex < 0) {
                return submissions;
            }
            cursor = drawTextureGroup(firstIndex, layer);
            submissions++;
        }
    }

    private int drawTextureGroup(int firstIndex, int layer) {
        TextureRenderRequest first = textureRequests.get(firstIndex);
        ResourceLocation texture = first.icon.getTexture();
        int nextCursor = textureRequestCount;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, layer);
        try (TextureTesselator tessellator = TesselateHelper.getTextureTesselator(texture)) {
            for (int i = firstIndex; i < textureRequestCount; i++) {
                TextureRenderRequest request = textureRequests.get(i);
                if (request.zIndex != layer) {
                    continue;
                }
                if (!texture.equals(request.icon.getTexture())) {
                    nextCursor = i;
                    break;
                }
                request.icon.tesselate(
                        tessellator,
                        graphics,
                        request.x,
                        request.y,
                        request.width,
                        request.height);
            }
        } finally {
            graphics.pose().popPose();
        }
        return nextCursor;
    }

    private int findTextureIndexAtLayer(int start, int layer) {
        for (int i = start; i < textureRequestCount; i++) {
            if (textureRequests.get(i).zIndex == layer) {
                return i;
            }
        }
        return -1;
    }

    private void requireActive() {
        if (!active) {
            throw new IllegalStateException("Icon batch is not active");
        }
    }

    private static final class TextureRenderRequest {
        private CTextureIcon icon;
        private int x;
        private int y;
        private int width;
        private int height;
        private int zIndex;

        private void set(CTextureIcon icon, int x, int y, int width, int height, int zIndex) {
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.zIndex = zIndex;
        }
    }
}
