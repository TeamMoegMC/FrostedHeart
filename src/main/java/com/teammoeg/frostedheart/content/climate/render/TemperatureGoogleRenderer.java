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

package com.teammoeg.frostedheart.content.climate.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.outliner.Outliner;
import com.teammoeg.frostedheart.content.steamenergy.ClientHeatNetworkData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;


/**
 * Adapted from @link{com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer}
 * <p>
 * Add temperature info to goggles overlay
 */
public class TemperatureGoogleRenderer {
    /** Uses the same named primary-layer and child-element structure as the other CUI HUDs. */
    public static final IGuiOverlay OVERLAY = new com.teammoeg.chorda.client.cui.screenadapter.CUIOverlay(TemperatureOverlay.INSTANCE);

    /** Presentation contract for alternate overlay styles. */
    @FunctionalInterface
    public interface OverlayStyle {
        void render(GuiGraphics graphics, List<Component> tooltip, float partialTicks, int width, int height);
    }

    private static OverlayStyle overlayStyle = new TemperatureTooltipStyle();
    private static final List<java.util.function.BiConsumer<GoggleOverlayContext, List<Component>>> informationProviders = new ArrayList<>();

    /** Selects the presentation used by subsequent frames. Call on the client thread. */
    public static void setOverlayStyle(OverlayStyle style) {
        overlayStyle = java.util.Objects.requireNonNull(style);
    }

    /** Appends information after built-in providers. Register on the client thread. */
    public static void registerInformationProvider(java.util.function.BiConsumer<BlockPos, List<Component>> provider) {
        java.util.Objects.requireNonNull(provider);
        registerContextInformationProvider((context, tooltip) -> provider.accept(context.position(), tooltip));
    }

    /** Appends information with access to the shared target/equipment snapshot. Register on the client thread. */
    public static void registerContextInformationProvider(
            java.util.function.BiConsumer<GoggleOverlayContext, List<Component>> provider) {
        informationProviders.add(java.util.Objects.requireNonNull(provider));
    }

    private static final Map<Object, Outliner.OutlineEntry> outlines = CreateClient.OUTLINER.getOutlines();

    public static int hoverTicks = 0;
    private static final PlantOverlayInformation plantInformation = new PlantOverlayInformation();
    public static BlockPos lastHeatNetworkPos = null;
    private static final HeatNetworkOverlayInformation heatInformation = new HeatNetworkOverlayInformation();

    /** Routes a server plant readout to the currently tracked target. */
    public static void receivePlantReadout(BlockPos position, float temperature, boolean exposedToBadWeather,
            @org.jetbrains.annotations.Nullable Component plantInfo,
            @org.jetbrains.annotations.Nullable com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo treeInfo) {
        plantInformation.receivePlantReadout(position, temperature, exposedToBadWeather, plantInfo, treeInfo);
    }

    public static boolean hasHeatNetworkData() {
        return heatInformation.hasData();
    }

    public static ClientHeatNetworkData getHeatNetworkData() {
        return heatInformation.getData();
    }

    public static void setHeatNetworkData(ClientHeatNetworkData data) {
        heatInformation.setData(data);
    }

    /** Compatibility entry point for callers that render without the registered CUI adapter. */
    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        drawReadout(graphics, collectFrameInformation(), partialTicks, width, height);
    }

    /** Collects one frame before CUI updates and renders its child elements. */
    static List<Component> collectFrameInformation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return List.of();
        if (!(mc.hitResult instanceof BlockHitResult hit)) {
            plantInformation.clearTarget();
            hoverTicks = 0;
            return List.of();
        }
        if (hasActiveValueBox()) return List.of();
        GoggleOverlayContext context = GoggleOverlayContext.at(mc, hit.getBlockPos());
        int previousHoverTicks = hoverTicks++;
        List<Component> tooltip = collectInformation(context, previousHoverTicks);
        if (tooltip.isEmpty()) hoverTicks = 0;
        return tooltip;
    }

    /** Draws the already-collected readout; this stage does not collect information or send requests. */
    static void drawReadout(GuiGraphics graphics, List<Component> tooltip, float partialTick, int width, int height) {
        if (!tooltip.isEmpty()) overlayStyle.render(graphics, tooltip, partialTick, width, height);
    }

    private static boolean hasActiveValueBox() {
        for (Outliner.OutlineEntry entry : outlines.values()) {
            if (entry.isAlive() && entry.getOutline() instanceof ValueBox box && !box.isPassive) return true;
        }
        return false;
    }

    private static List<Component> collectInformation(GoggleOverlayContext context, int previousHoverTicks) {
        var readout = new CreateOverlayInformation.Readout(context);
        heatInformation.request(context);
        CreateOverlayInformation.appendGogglesAndHover(context, readout);
        boolean plantAdded = plantInformation.append(context, readout.tooltip);
        if (CreateOverlayInformation.appendAssemblyAndTrain(context, readout)) hoverTicks = previousHoverTicks + 1;
        if (readout.suppresses(context.hasPlantInformation(), plantAdded)
                || !CreateOverlayInformation.appendPiston(context, readout.tooltip)) return List.of();
        for (var provider : informationProviders) provider.accept(context, readout.tooltip);
        return readout.tooltip;
    }

    public static BlockPos proxiedOverlayPosition(Level level, BlockPos pos) {
        BlockState targetedState = level.getBlockState(pos);
        if (targetedState.getBlock() instanceof IProxyHoveringInformation proxy)
            return proxy.getInformationSource(level, pos, targetedState);
        return pos;
    }

    /** Compatibility entry point for existing heat-network block tooltip callbacks. */
    public static boolean addHeatNetworkInfoToTooltip(List<Component> tooltip, boolean isPlayerSneaking, BlockPos worldPosition) {
        return heatInformation.appendTooltip(tooltip, worldPosition);
    }
}
