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

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.outliner.Outline;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.agriculture.tree.CultivatedOakBlock;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.network.SoilThermometerRequestPacket;
import com.teammoeg.frostedheart.content.climate.tooltips.PlantTempStats;
import com.teammoeg.frostedheart.content.steamenergy.ClientHeatNetworkData;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkRequestC2SPacket;
import com.teammoeg.frostedheart.content.utility.SoilThermometer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static net.minecraft.ChatFormatting.GRAY;

/**
 * Adapted from @link{com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer}
 * <p>
 * Add temperature info to goggles overlay
 */
public class TemperatureGoogleRenderer {
    /** CUI lifecycle adapter; information gathering remains independent of presentation. */
    public static final IGuiOverlay OVERLAY = new com.teammoeg.chorda.client.cui.screenadapter.CUIOverlay(
            new com.teammoeg.chorda.client.cui.base.PrimaryLayer() {
                private float overlayPartialTick;

                @Override
                public void updateRenderInfo(int x, int y, double mouseX, double mouseY, float partialTick) {
                    super.updateRenderInfo(x, y, mouseX, mouseY, partialTick);
                    // UIElement retains its previous value for zero; this HUD needs the exact current frame.
                    overlayPartialTick = partialTick;
                }

                @Override
                public boolean shouldRenderGradient() { return false; }
                @Override
                public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h,
                        com.teammoeg.chorda.client.RenderingHint hint) {
                    renderOverlay(null, graphics, overlayPartialTick,
                            Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                            Minecraft.getInstance().getWindow().getGuiScaledHeight());
                }
            });

    /** Presentation contract for alternate overlay styles. */
    @FunctionalInterface
    public interface OverlayStyle {
        void render(GuiGraphics graphics, List<Component> tooltip, float partialTicks, int width, int height);
    }

    private static OverlayStyle overlayStyle = new TemperatureTooltipStyle();
    private static final List<java.util.function.BiConsumer<BlockPos, List<Component>>> informationProviders = new ArrayList<>();

    /** Selects the presentation used by subsequent frames. Call on the client thread. */
    public static void setOverlayStyle(OverlayStyle style) {
        overlayStyle = java.util.Objects.requireNonNull(style);
    }

    /** Appends information after built-in providers. Register on the client thread. */
    public static void registerInformationProvider(java.util.function.BiConsumer<BlockPos, List<Component>> provider) {
        informationProviders.add(java.util.Objects.requireNonNull(provider));
    }

    private static final Map<Object, Outliner.OutlineEntry> outlines = CreateClient.OUTLINER.getOutlines();

    public static int hoverTicks = 0;
    private static BlockPos cropTarget;
    private static java.lang.ref.WeakReference<ClientLevel> cropLevel = new java.lang.ref.WeakReference<>(null);
    private static long lastCropRequest;

    /** Updates the growth readout for the currently inspected plant. */
    public static void receivePlantReadout(BlockPos position, float temperature, boolean exposedToBadWeather,
            @org.jetbrains.annotations.Nullable Component plantInfo,
            @org.jetbrains.annotations.Nullable com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo treeInfo) {
        if (position.equals(cropTarget) && Minecraft.getInstance().level == cropLevel.get()) {
            cachedTemperature = temperature;
            cropExposedToBadWeather = exposedToBadWeather;
            cachedPlantInfo = plantInfo;
            cachedTreeInfo = treeInfo;
        }
    }
    public static BlockPos lastHeatNetworkPos = null;
    private static float cachedTemperature = Float.NaN;
    private static boolean cropExposedToBadWeather;
    private static Component cachedPlantInfo;
    private static com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo cachedTreeInfo;
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
            cropTarget = null;
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
        pos = proxiedOverlayPosition(world, pos);

        BlockEntity be = world.getBlockEntity(pos);

        boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        PlantTempData plantData = PlantTempData.cacheList.get(block);
        boolean cultivatedSapling = block instanceof CultivatedOakBlock;
        boolean hasPlantTempInfo = plantData != null || cultivatedSapling;

        // Check if the player is wearing goggles
        boolean wearingSoilThermometer = SoilThermometer.isWearingSoilThermometer(mc.player);

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
        	if(lastHeatNetworkData==null||!lastHeatNetworkData.pos.equals(pos))
            // Set invalid data by default
        		lastHeatNetworkData = new ClientHeatNetworkData(pos);
            // Try to request the data
            FHNetwork.INSTANCE.sendToServer(new HeatNetworkRequestC2SPacket(pos));
        }

        // Icon to render
        ItemStack item = AllItems.GOGGLES.asStack();
        ItemStack soilThermometerStack = FHItems.soil_thermometer.asStack();

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

        boolean addedPlantTempInfo = false;
        // plant temperature
        if (!wearingSoilThermometer || !hasPlantTempInfo) cropTarget = null;
        else {
            long tick = world.getGameTime();
            boolean changed = world != cropLevel.get() || !pos.equals(cropTarget);
            if (changed || tick < lastCropRequest || tick - lastCropRequest >= 20) {
                if (changed) {
                    cachedTemperature = Float.NaN;
                    cachedPlantInfo = null;
                    cachedTreeInfo = null;
                }
                cropTarget = pos.immutable();
                if (world != cropLevel.get()) cropLevel = new java.lang.ref.WeakReference<>(world);
                lastCropRequest = tick;
                FHNetwork.INSTANCE.sendToServer(new SoilThermometerRequestPacket(cropTarget));
            }
        }
        if (cropTarget != null) {
            List<Component> stats = List.of();
            if (cultivatedSapling && cachedPlantInfo != null) {
                stats = com.teammoeg.frostedheart.content.climate.tooltips.CultivatedTreeTempStats.getStats(
                        cachedTreeInfo, cachedPlantInfo, world.getBlockState(pos.below()), cachedTemperature);
            } else if (!cultivatedSapling && cachedPlantInfo == null && Float.isFinite(cachedTemperature)) {
                stats = PlantTempStats.getStats(block, mc.player, pos, cachedTemperature, cropExposedToBadWeather);
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
            (hasPlantTempInfo && !addedPlantTempInfo)) {
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

        for (var provider : informationProviders) provider.accept(pos, tooltip);

        if (tooltip.isEmpty()) {
            hoverTicks = 0;
            return;
        }

        overlayStyle.render(graphics, tooltip, partialTicks, width, height);
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
