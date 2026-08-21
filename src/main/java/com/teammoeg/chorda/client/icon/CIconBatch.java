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

import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.CGuiHelper.ItemRenderRequest;
import com.teammoeg.chorda.client.ui.CGuiHelper.ItemBatchPlanner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reusable ordered icon pass. Item icons are submitted in compatible lighting
 * segments, while unsupported icon types form an immediate-render barrier.
 */
public final class CIconBatch {
    public enum Ordering {
        SUBMISSION_ORDER,
        LAYER_THEN_LIGHTING
    }

    private final List<ItemRenderRequest> itemRequests = new ArrayList<>();
    private final ItemBatchPlanner itemBatchPlanner = new ItemBatchPlanner();
    private GuiGraphics graphics;
    private int itemRequestCount;
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
            flushItems();
            return flushCount;
        } finally {
            graphics = null;
            itemRequestCount = 0;
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

    void drawImmediately(CIcon icon, GuiGraphics graphics, int x, int y, int width, int height) {
        flushItems();
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
        flushItems();
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

    private void flushItems() {
        if (itemRequestCount == 0) {
            return;
        }
        flushCount += ordering == Ordering.LAYER_THEN_LIGHTING
                ? CGuiHelper.drawItemsByLayerAndLighting(graphics, itemRequests, itemRequestCount)
                : CGuiHelper.drawItems(graphics, itemRequests, itemRequestCount, itemBatchPlanner);
        itemRequestCount = 0;
    }

    private void requireActive() {
        if (!active) {
            throw new IllegalStateException("Icon batch is not active");
        }
    }
}
