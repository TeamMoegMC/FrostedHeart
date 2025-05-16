/*
 * Copyright (c) 2024 TeamMoeg
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

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.mixin.accessor.MouseHandlerAccessor;
import com.simibubi.create.foundation.outliner.Outline;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.data.BlockTempData;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.tooltips.BlockTempStats;
import com.teammoeg.frostedheart.content.climate.tooltips.PlantTempStats;
import com.teammoeg.frostedheart.content.steamenergy.ClientHeatNetworkData;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkRequestC2SPacket;
import com.teammoeg.frostedheart.content.utility.SoilThermometer;
import com.teammoeg.frostedheart.content.utility.TemperatureProbe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static net.minecraft.ChatFormatting.GRAY;

/**
 * Adapted from @link{com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer}
 *
 * Add temperature info to goggles overlay
 *
 * TODO: Remove Create's Google Renderer
 */
public class TemperatureGoogleRenderer {
    public static final IGuiOverlay OVERLAY = TemperatureGoogleRenderer::renderOverlay;

    private static final Map<Object, Outliner.OutlineEntry> outlines = CreateClient.OUTLINER.getOutlines();

    public static int hoverTicks = 0;
    public static BlockPos lastHovered = null;
    public static BlockPos lastHeatNetworkPos = null;
    private static ClientHeatNetworkData lastHeatNetworkData = null;

    public static boolean hasHeatNetworkData() {
        return lastHeatNetworkData != null && !lastHeatNetworkData.invalid();
    }

    public static ClientHeatNetworkData getHeatNetworkData() {
        return lastHeatNetworkData;
    }

    public static void setHeatNetworkData(ClientHeatNetworkData data) {
        lastHeatNetworkData = data;
    }

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width,
                                     int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
            return;

        // Get the block the player is looking at
        HitResult objectMouseOver = mc.hitResult;
        if (!(objectMouseOver instanceof BlockHitResult)) {
            lastHovered = null;
            hoverTicks = 0;
            return;
        }

        for (Outliner.OutlineEntry entry : outlines.values()) {
            if (!entry.isAlive())
                continue;
            Outline outline = entry.getOutline();
            if (outline instanceof ValueBox && !((ValueBox) outline).isPassive)
                return;
        }

        BlockHitResult result = (BlockHitResult) objectMouseOver;
        ClientLevel world = mc.level;
        BlockPos pos = result.getBlockPos();

        int prevHoverTicks = hoverTicks;
        hoverTicks++;
        lastHovered = pos;

        pos = proxiedOverlayPosition(world, pos);

        BlockEntity be = world.getBlockEntity(pos);

        boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);

        BlockState state = world.getBlockState(pos);
        BlockState belowState = world.getBlockState(pos.below());
        Block block = world.getBlockState(pos).getBlock();
        BlockTempData blockData = BlockTempData.getData(block);
        PlantTempData plantData = PlantTempData.cacheList.get(block);
        boolean hasBlockTempInfo = blockData != null;
        boolean hasPlantTempInfo = plantData != null;

        // Check if the player is wearing goggles
        boolean wearingSoilThermometer = SoilThermometer.isWearingSoilThermometer(mc.player);
        boolean wearingTemperatureProbe = TemperatureProbe.isWearingTemperatureProbe(mc.player);

        boolean hasGoggleInformation = be instanceof IHaveGoggleInformation;
        boolean hasHoveringInformation = be instanceof IHaveHoveringInformation;

        boolean goggleAddedInformation = false;
        boolean hoverAddedInformation = false;

        boolean hasHeatNetworkInformation = be instanceof HeatNetworkProvider;

        // special case: multiblock heat network provider
        boolean hasMBGoogleInformation = false;
        boolean hasMBHoveringInformation = false;
        boolean hasMBHeatNetworkInformation = false;
        if (be instanceof IMultiblockBE multiblockBE) {
            IMultiblockState mbState = multiblockBE.getHelper().getState();
            if (mbState != null) {
                hasMBGoogleInformation = mbState instanceof IHaveGoggleInformation;
                hasMBHoveringInformation = mbState instanceof IHaveHoveringInformation;
                hasMBHeatNetworkInformation = mbState instanceof HeatNetworkProvider;
            }
        }

        // Request HeatNetwork data
        // Note: We cannot really do cache by pos as heat network data can change every tick...
        // TODO: Separate network request and endpoint request
        if (wearingGoggles &&
                (
                        (hasGoggleInformation && hasHeatNetworkInformation) || (hasMBGoogleInformation && hasMBHeatNetworkInformation)
                )
        ) {
            // Set invalid data by default
            if (lastHeatNetworkData == null)
                lastHeatNetworkData = new ClientHeatNetworkData(pos);
            // Try to request the data
            FHNetwork.INSTANCE.sendToServer(new HeatNetworkRequestC2SPacket(pos));
        }

        // Icon to render
        ItemStack item = AllItems.GOGGLES.asStack();
        ItemStack soilThermometerStack = FHItems.soil_thermometer.asStack();
        ItemStack temperatureProbeStack = FHItems.temperatureProbe.asStack();

        List<Component> tooltip = new ArrayList<>();

        if (hasGoggleInformation && wearingGoggles) {
            boolean isShifting = mc.player.isShiftKeyDown();

            IHaveGoggleInformation gte = (IHaveGoggleInformation) be;
            goggleAddedInformation = gte.addToGoggleTooltip(tooltip, isShifting);
            item = gte.getIcon(isShifting);
        }

        if (hasMBGoogleInformation && wearingGoggles) {
            boolean isShifting = mc.player.isShiftKeyDown();
            IMultiblockState mbState = ((IMultiblockBE) be).getHelper().getState();
            if (mbState != null) {
                IHaveGoggleInformation gte = (IHaveGoggleInformation) mbState;
                goggleAddedInformation = gte.addToGoggleTooltip(tooltip, isShifting);
                item = gte.getIcon(isShifting);
            }
        }

        if (hasHoveringInformation) {
            if (!tooltip.isEmpty())
                tooltip.add(Components.immutableEmpty());
            IHaveHoveringInformation hte = (IHaveHoveringInformation) be;
            hoverAddedInformation = hte.addToTooltip(tooltip, mc.player.isShiftKeyDown());

            if (goggleAddedInformation && !hoverAddedInformation)
                tooltip.remove(tooltip.size() - 1);
        }

        boolean addedBlockTempInfo = false;
        // block temperature
        if (wearingTemperatureProbe && hasBlockTempInfo) {
            boolean isNotLit = state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT);
            boolean requiresLit = blockData.isLit();
            List<Component> stats = BlockTempStats.getStats(block, null, mc.player);
            if (!stats.isEmpty() && !(requiresLit && isNotLit)) {
                addedBlockTempInfo = true;
                if (!tooltip.isEmpty())
                    tooltip.add(Components.immutableEmpty());
                tooltip.addAll(stats);
            }
        }

        boolean addedPlantTempInfo = false;
        // plant temperature
        if (wearingSoilThermometer && hasPlantTempInfo) {
            List<Component> stats;
            if (belowState.is(FHBlocks.FERTILIZED_FARMLAND.get()) || belowState.is(FHBlocks.FERTILIZED_DIRT.get())) {
                stats = PlantTempStats.getStats(block, null, mc.player, belowState);
            } else {
                stats = PlantTempStats.getStats(block, null, mc.player, null);
            }
            if (!stats.isEmpty()) {
                addedPlantTempInfo = true;
                if (!tooltip.isEmpty())
                    tooltip.add(Components.immutableEmpty());
                tooltip.addAll(stats);
            }
        }

        // assembly
        if (be instanceof IDisplayAssemblyExceptions) {
            boolean exceptionAdded = ((IDisplayAssemblyExceptions) be).addExceptionToTooltip(tooltip);
            if (exceptionAdded) {
                hasHoveringInformation = true;
                hoverAddedInformation = true;
            }
        }

        // train
        if (!hasHoveringInformation)
            if (hasHoveringInformation =
                    hoverAddedInformation = TrainRelocator.addToTooltip(tooltip, mc.player.isShiftKeyDown()))
                hoverTicks = prevHoverTicks + 1;

        // break early if goggle or hover returned false when present
        if ((hasGoggleInformation && !goggleAddedInformation) && (hasHoveringInformation && !hoverAddedInformation) &&
            (hasBlockTempInfo && !addedBlockTempInfo) && (hasPlantTempInfo && !addedPlantTempInfo)) {
            hoverTicks = 0;
            return;
        }

        // check for piston poles if goggles are worn
        if (wearingGoggles && AllBlocks.PISTON_EXTENSION_POLE.has(state)) {
            Direction[] directions = Iterate.directionsInAxis(state.getValue(PistonExtensionPoleBlock.FACING)
                    .getAxis());
            int poles = 1;
            boolean pistonFound = false;
            for (Direction dir : directions) {
                int attachedPoles = PistonExtensionPoleBlock.PlacementHelper.get()
                        .attachedPoles(world, pos, dir);
                poles += attachedPoles;
                pistonFound |= world.getBlockState(pos.relative(dir, attachedPoles + 1))
                        .getBlock() instanceof MechanicalPistonBlock;
            }

            if (!pistonFound) {
                hoverTicks = 0;
                return;
            }
            if (!tooltip.isEmpty())
                tooltip.add(Components.immutableEmpty());

            Lang.translate("gui.goggles.pole_length").text(" " + poles)
                    .forGoggles(tooltip);
        }

        if (tooltip.isEmpty()) {
            hoverTicks = 0;
            return;
        }

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // Text formatting
        int tooltipTextWidth = 0;
        for (FormattedText textLine : tooltip) {
            int textLineWidth = mc.font.width(textLine);
            if (textLineWidth > tooltipTextWidth)
                tooltipTextWidth = textLineWidth;
        }

        int tooltipHeight = 8;
        if (tooltip.size() > 1) {
            tooltipHeight += 2; // gap between title lines and next lines
            tooltipHeight += (tooltip.size() - 1) * 10;
        }

        CClient cfg = AllConfigs.client();
        int posX = width / 2 + cfg.overlayOffsetX.get();
        int posY = height / 2 + cfg.overlayOffsetY.get();

        posX = Math.min(posX, width - tooltipTextWidth - 20);
        posY = Math.min(posY, height - tooltipHeight - 20);

        float fade = Mth.clamp((hoverTicks + partialTicks) / 24f, 0, 1);
        Boolean useCustom = cfg.overlayCustomColor.get();
        Color colorBackground = useCustom ? new Color(cfg.overlayBackgroundColor.get())
                : Theme.c(Theme.Key.VANILLA_TOOLTIP_BACKGROUND)
                .scaleAlpha(.75f);
        Color colorBorderTop = useCustom ? new Color(cfg.overlayBorderColorTop.get())
                : Theme.c(Theme.Key.VANILLA_TOOLTIP_BORDER, true)
                .copy();
        Color colorBorderBot = useCustom ? new Color(cfg.overlayBorderColorBot.get())
                : Theme.c(Theme.Key.VANILLA_TOOLTIP_BORDER, false)
                .copy();

        if (fade < 1) {
            poseStack.translate(Math.pow(1 - fade, 3) * Math.signum(cfg.overlayOffsetX.get() + .5f) * 8, 0, 0);
            colorBackground.scaleAlpha(fade);
            colorBorderTop.scaleAlpha(fade);
            colorBorderBot.scaleAlpha(fade);
        }

        // Render google item
//        GuiGameElement.of(item)
//                .at(posX + 10, posY - 16, 450)
//                .render(graphics);

        // Render hovering text
        if (!Mods.MODERNUI.isLoaded()) {
            // default tooltip rendering when modernUI is not loaded
            RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
                    colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);

            poseStack.popPose();

            return;
        }

        /*
         * special handling for modernUI
         *
         * their tooltip handler causes the overlay to jiggle each frame,
         * if the mouse is moving, guiScale is anything but 1 and exactPositioning is enabled
         *
         * this is a workaround to fix this behavior
         */
        MouseHandler mouseHandler = Minecraft.getInstance().mouseHandler;
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        double cursorX = mouseHandler.xpos();
        double cursorY = mouseHandler.ypos();
        ((MouseHandlerAccessor) mouseHandler).create$setXPos(Math.round(cursorX / guiScale) * guiScale);
        ((MouseHandlerAccessor) mouseHandler).create$setYPos(Math.round(cursorY / guiScale) * guiScale);

        RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
                colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);

        ((MouseHandlerAccessor) mouseHandler).create$setXPos(cursorX);
        ((MouseHandlerAccessor) mouseHandler).create$setYPos(cursorY);

        poseStack.popPose();

    }

    public static BlockPos proxiedOverlayPosition(Level level, BlockPos pos) {
        BlockState targetedState = level.getBlockState(pos);
        if (targetedState.getBlock() instanceof IProxyHoveringInformation proxy)
            return proxy.getInformationSource(level, pos, targetedState);
        return pos;
    }

    public static boolean addHeatNetworkInfoToTooltip(List<Component> tooltip, boolean isPlayerSneaking, BlockPos worldPosition) {
        com.teammoeg.frostedheart.util.Lang.tooltip("heat_stats").forGoggles(tooltip);

        if (TemperatureGoogleRenderer.hasHeatNetworkData()) {
            ClientHeatNetworkData data = TemperatureGoogleRenderer.getHeatNetworkData();

            com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.network")
                    .style(GRAY)
                    .forGoggles(tooltip);

            com.teammoeg.frostedheart.util.Lang.number(data.totalEndpointIntake)
                    .translate("generic", "unit.pressure")
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.intake")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            com.teammoeg.frostedheart.util.Lang.number(data.totalEndpointOutput)
                    .translate("generic", "unit.pressure")
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.output")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            // show number of endpoints
            com.teammoeg.frostedheart.util.Lang.number(data.endpoints.size())
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.endpoints")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.endpoint")
                    .style(GRAY)
                    .forGoggles(tooltip);

            // stream through endpoints, filter by pos
            data.endpoints.stream()
                    .filter(e -> e.getPos().equals(worldPosition))
                    .forEach(e -> {
                        float maxIntake = e.getMaxIntake();
                        float maxOutput = e.getMaxOutput();
                        float avgIntake = e.getAvgIntake();
                        float avgOutput = e.getAvgOutput();

                        if (maxIntake > 0)
                            com.teammoeg.frostedheart.util.Lang.number(e.getMaxIntake())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.max_intake")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (maxOutput > 0)
                            com.teammoeg.frostedheart.util.Lang.number(e.getMaxOutput())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.max_output")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (avgIntake > 0)
                            com.teammoeg.frostedheart.util.Lang.number(e.getAvgIntake())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.average_intake")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (avgOutput > 0)
                            com.teammoeg.frostedheart.util.Lang.number(e.getAvgOutput())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.average_output")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);
                    });

        } else {
            com.teammoeg.frostedheart.util.Lang.translate("tooltip", "pressure.no_network")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }

        return true;

    }
}
