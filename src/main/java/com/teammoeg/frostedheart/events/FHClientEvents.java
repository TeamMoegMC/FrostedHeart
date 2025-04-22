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

package com.teammoeg.frostedheart.events;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.client.CameraHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.PartialTickTracker;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.GuiClickedEvent;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.network.C2SOpenClothesScreenMessage;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import com.teammoeg.frostedheart.content.health.network.C2SOpenNutritionScreenMessage;
import com.teammoeg.frostedheart.content.health.screen.HealthStatScreen;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.dialog.HUDDialog;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.wheelmenu.Selection;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuSelectionRegisterEvent;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuRenderer;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.data.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.util.FHVersion;
import com.teammoeg.frostedheart.util.Lang;

import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.item.FTBQuestsItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.List;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FHClientEvents {

    @SuppressWarnings({"unchecked", "deprecation"})
    @SubscribeEvent
    public static void drawUpdateReminder(ScreenEvent.Render event) {
        Screen gui = event.getScreen();
        if (gui instanceof TitleScreen) {
        	
            FHMain.remote.fetchVersion().ifPresent(stableVersion -> {
                boolean isStable = true;
                if (stableVersion.isEmpty())
                    return;
                GuiGraphics matrixStack = event.getGuiGraphics();
                FHVersion clientVersion = FHMain.local.fetchVersion().orElse(FHVersion.empty);
                Font font = gui.getMinecraft().font;
                if (!stableVersion.isEmpty() && (clientVersion.isEmpty() || !clientVersion.laterThan(stableVersion))) {
                    List<FormattedCharSequence> list = font.split(Lang.translateGui("update_recommended")
                            .append(stableVersion.getOriginal()).withStyle(ChatFormatting.BOLD), 70);
                    int l = 0;
                    for (FormattedCharSequence line : list) {
                        //TODO Uncomment after draw line fixed
                    	//CGuis.drawLine(matrixStack, Color4I.rgba(0, 0, 0, 255), 0, gui.height / 2 - 1 + l, 72,gui.height / 2 + 9 + l);
                        matrixStack.drawString(ClientUtils.mc().font, line, 1, gui.height / 2.0F + l, 0xFFFFFF, true);
                        l += 9;
                    }
                    if (isStable) {
                        MutableComponent itxc = Components.str("CurseForge")
                                .withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GOLD);
                        boolean needEvents = true;
                        for (GuiEventListener x : gui.children())
                            if (x instanceof GuiClickedEvent) {
                                needEvents = false;
                                break;
                            }
                        matrixStack.drawString(ClientUtils.mc().font, itxc, 1, (int) (gui.height / 2.0F + l), 0xFFFFFF);
                        Style opencf = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                "https://www.curseforge.com/minecraft/modpacks/the-winter-rescue"));
                        // Though the capture is ? extends IGuiEventListener, I can't add new to it
                        // unless I cast it to List
                        if (needEvents)
                            ((List<GuiEventListener>) gui.children()).add(new GuiClickedEvent(1,
                                    (int) (gui.height / 2.0F + l), font.width(itxc) + 1,
                                    (int) (gui.height / 2.0F + l + 9), () -> gui.handleComponentClicked(opencf)));
                        /*if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()
                                .equalsIgnoreCase("zh_cn")) {
                            l += 9;
                            Style openmcbbs = Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                    "https://www.mcbbs.net/thread-1227167-1-1.html"));
                            IFormattableTextComponent itxm = new StringTextComponent("MCBBS")
                                    .mergeStyle(TextFormatting.UNDERLINE).mergeStyle(TextFormatting.BOLD)
                                    .mergeStyle(TextFormatting.DARK_RED);
                            if (needEvents)
                                ((List<IGuiEventListener>) gui.getEventListeners()).add(new GuiClickedEvent(1,
                                        (int) (gui.height / 2.0F + l), font.getStringPropertyWidth(itxm) + 1,
                                        (int) (gui.height / 2.0F + l + 9),
                                        () -> gui.handleComponentClicked(openmcbbs)));
                            font.drawTextWithShadow(matrixStack, itxm, 1, gui.height / 2.0F + l, 0xFFFFFF);
                        }*/
                    }
                }
            });
        }
    }
    @SubscribeEvent
    public static void registerSelections(WheelMenuSelectionRegisterEvent event) {
		if (CompatModule.isFTBQLoaded()) {
			
			event.register(new ResourceLocation("ftb_quests","open_book"),new Selection(Component.translatable("key.ftbquests.quests"), CIcons.getIcon(FTBQuestsItems.BOOK.get()),
					s -> FTBQuestsClient.openGui()));
		}
		event.register(new ResourceLocation("curios","open_gui"),new Selection("key.curios.open.desc",CIcons.getIcon(FHItems.heater_vest)));
		
		event.register(FHMain.rl("debug"),new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.debug"),
				CIcons.getIcon(FHItems.debug_item), ColorHelper.CYAN, 
				s -> ClientUtils.getPlayer().isCreative(), s -> DebugScreen.openDebugScreen(), Selection.NO_ACTION));

		event.register(FHMain.rl("health"),new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.nutrition"),
			CIcons.getIcon(HealthStatScreen.fat_icon), s -> FHNetwork.INSTANCE.sendToServer(new C2SOpenNutritionScreenMessage())));

		event.register(FHMain.rl("clothing"),new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.clothing"),
			CIcons.getIcon(FHItems.gambeson), 
				s -> FHNetwork.INSTANCE.sendToServer(new C2SOpenClothesScreenMessage())));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
       // if (!Minecraft.getInstance().hasSingleplayerServer()) {
            //FHMain.LOGGER.info("Frostedheart recipes updated from server, rebuilding recipe lists");
            FHRecipeCachingReloadListener.buildRecipeLists(event.getRecipeManager());
        //}
    }

    @SubscribeEvent
    public static void fireLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> CClientTeamDataManager.INSTANCE::reset);
        // TODO: temporary fix for client not sending ready packet
        ClientScene.INSTANCE = new ClientScene();
        ClientScene.INSTANCE.sendClientReady();
        FHMain.LOGGER.info("loaded wheel menu");
        WheelMenuRenderer.load();

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderAfterBlockEntity(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            ClientUtils.mc().getProfiler().push("frostedheart:render_infrared_view");
            InfraredViewRenderer.renderInfraredView();
            ClientUtils.mc().getProfiler().pop();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderCustomHUD(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer clientPlayer = mc.player;
        Player renderViewPlayer = FrostedHud.getRenderViewPlayer();

        if (renderViewPlayer == null || clientPlayer == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics stack = event.getGuiGraphics();
        int anchorX = event.getWindow().getGuiScaledWidth() / 2;
        int anchorY = event.getWindow().getGuiScaledHeight();
        float partialTicks = event.getPartialTick();

        FrostedHud.renderSetup(clientPlayer, renderViewPlayer);
        if (FHConfig.CLIENT.enableUI.get()) {
            if (event.getOverlay() == VanillaGuiOverlay.VIGNETTE.type()) {
                if (FrostedHud.renderFrozenOverlay)
                    FrostedHud.renderFrozenOverlay(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderFrozenVignette)
                    FrostedHud.renderFrozenVignette(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderHeatVignette)
                    FrostedHud.renderHeatVignette(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderWaypoint)
                    ClientWaypointManager.renderAll(stack);
                if (FrostedHud.renderDebugOverlay)
                    FrostedHud.renderDebugOverlay(stack, mc);


            }
            if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() && FrostedHud.renderHotbar) {
                if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
                    mc.gui.getSpectatorGui().renderHotbar(stack);
                } else {
                    if (FrostedHud.renderForecast)
                        FrostedHud.renderForecast(stack, anchorX, anchorY, mc, renderViewPlayer);
                    FrostedHud.renderHotbar(stack, anchorX, anchorY, mc, renderViewPlayer, partialTicks);
                    if (FHConfig.CLIENT.renderScenario.get())
                        FrostedHud.renderScenarioAct(stack, anchorX, anchorY, mc, renderViewPlayer);
                }

                if (ClientScene.INSTANCE != null && ClientScene.INSTANCE.dialog instanceof HUDDialog dialog) {
                    dialog.render(stack, 0, 0, PartialTickTracker.getTickAlignedPartialTicks());
                }
                event.setCanceled(true);
            }
            if (event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type() && FrostedHud.renderExperience) {
                if (FrostedHud.renderHypothermia) {
                    FrostedHud.renderHypothermia(stack, anchorX, anchorY, mc, clientPlayer);
                } else {
                    // TODO: Provide some method to render both
                    FrostedHud.renderExperience(stack, anchorX, anchorY, mc, clientPlayer);
//                    FrostedHud.renderInsight(stack, anchorX, anchorY, mc, clientPlayer);
                }
                event.setCanceled(true);
            }
            if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() && FrostedHud.renderHealth) {
                FrostedHud.renderHealth(stack, anchorX, anchorY, mc, renderViewPlayer);
                event.setCanceled(true);
            }
            if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
                if (FrostedHud.renderFood)
                    FrostedHud.renderFood(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderThirst)
                    FrostedHud.renderThirst(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderHealth)
                    FrostedHud.renderTemperature(stack, anchorX, anchorY, mc, renderViewPlayer);
                event.setCanceled(true);
            }
            if (event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() && FrostedHud.renderArmor) {
                FrostedHud.renderArmor(stack, anchorX, anchorY, mc, clientPlayer);
                event.setCanceled(true);
            }
            if (event.getOverlay() == VanillaGuiOverlay.MOUNT_HEALTH.type() && FrostedHud.renderHealthMount) {
                FrostedHud.renderMountHealth(stack, anchorX, anchorY, mc, clientPlayer);
                event.setCanceled(true);
            }
            if (event.getOverlay() == VanillaGuiOverlay.JUMP_BAR.type() && FrostedHud.renderJumpBar) {
                FrostedHud.renderJumpbar(stack, anchorX, anchorY, mc, clientPlayer);
                event.setCanceled(true);
            }
        }
        // add compatibility to other MOD UIs, may cause problem?
        if (event.isCanceled()) {
            if (event.getOverlay() != VanillaGuiOverlay.FOOD_LEVEL.type())
                MinecraftForge.EVENT_BUS
                        .post(new RenderGuiOverlayEvent.Post(event.getWindow(), event.getGuiGraphics(), event.getPartialTick(), event.getOverlay()));// compatibility
        }
    }

    @SubscribeEvent
    public static void sendLoginUpdateReminder(ClientPlayerNetworkEvent.LoggingIn event) {

        FHMain.remote.fetchVersion().ifPresent(stableVersion -> {
            boolean isStable = true;
            if (stableVersion.isEmpty())
                return;
            FHVersion clientVersion = FHMain.local.fetchVersion().orElse(FHVersion.empty);
            if (!stableVersion.isEmpty() && (clientVersion.isEmpty() || !clientVersion.laterThan(stableVersion))) {
                event.getPlayer().displayClientMessage(Lang.translateGui("update_recommended")
                        .append(stableVersion.getOriginal()).withStyle(ChatFormatting.BOLD), false);
                if (isStable) {
                    event.getPlayer()
                            .displayClientMessage(Components.str("CurseForge")
                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                            "https://www.curseforge.com/minecraft/modpacks/the-winter-rescue")))
                                    .withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.BOLD)
                                    .withStyle(ChatFormatting.GOLD), false);

                    /*if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()
                            .equalsIgnoreCase("zh_cn")) {
                        event.getPlayer()
                                .sendStatusMessage(new StringTextComponent("MCBBS")
                                        .setStyle(Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                                "https://www.mcbbs.net/thread-1227167-1-1.html")))
                                        .mergeStyle(TextFormatting.UNDERLINE).mergeStyle(TextFormatting.BOLD)
                                        .mergeStyle(TextFormatting.DARK_RED), false);
                    }*/
                }
            }
        });
        if (ServerLifecycleHooks.getCurrentServer() != null)
            if (FHMain.saveNeedUpdate) {
                event.getPlayer().displayClientMessage(
                        Lang.translateGui("save_update_needed", FHMain.lastServerConfig.getAbsolutePath())
                                .withStyle(ChatFormatting.RED),
                        false);
            } else if (FHMain.lastbkf != null) {
                event.getPlayer().displayClientMessage(Lang.translateGui("save_updated")
                                .append(Components.str(FHMain.lastbkf.getName()).setStyle(Style.EMPTY
                                        .withClickEvent(
                                                new ClickEvent(ClickEvent.Action.OPEN_FILE, FHMain.lastbkf.getAbsolutePath()))
                                        .applyFormat(ChatFormatting.UNDERLINE))),
                        false);
            }

    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void tickClient(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            InfraredViewRenderer.clientTick();
            Minecraft mc = ClientUtils.mc();
            Player pe = ClientUtils.getPlayer();
            if (mc.level != null) {
                if (ClientScene.INSTANCE != null)
                    ClientScene.INSTANCE.tick(mc);
	            PlayerTemperatureData.getCapability(pe).ifPresent(t -> {
	                t.smoothedBodyPrev = t.smoothedBody;
	                t.smoothedBody = t.smoothedBody * .9f + t.getCoreBodyTemp() * .1f;
	            });
	
	            if (pe != null && pe.getEffect(FHMobEffects.NYCTALOPIA.get()) != null) {
	                ClientUtils.DoApplyGammaValue = true;
	                ClientUtils.OverwriteGammaValue = Mth.clamp((float) (double) (mc.options.gamma().get()), 0f, 1f) * 0.1f
	                        - 1f;
	            } else {
	                ClientUtils.DoApplyGammaValue = false;
	                ClientUtils.OverwriteGammaValue = Mth.clamp((float) (double) mc.options.gamma().get(), 0f, 1f);
	            }

            //if(WheelMenuRenderer.isOpened)
            	WheelMenuRenderer.tick();
            }
        }
    }

    @SubscribeEvent
    public static void unloadWorld(LevelEvent.Unload event) {
        ClientUtils.DoApplyGammaValue = false;
    }

    /*
        @SuppressWarnings({"resource", "unchecked", "rawtypes"})
        @SubscribeEvent
        public void onWorldLoad(LevelEvent.Load event) {
            if (!HeaterVestRenderer.rendersAssigned) {
                for (Object render : ClientUtils.mc().getEntityRenderDispatcher().renderers.values())
                    if (HumanoidMobRenderer.class.isAssignableFrom(render.getClass()))
                        ((HumanoidMobRenderer) render).addLayer(new HeaterVestRenderer<>((HumanoidMobRenderer) render));
                    else if (ArmorStandRenderer.class.isAssignableFrom(render.getClass()))
                        ((ArmorStandRenderer) render).addLayer(new HeaterVestRenderer<>((ArmorStandRenderer) render));
                HeaterVestRenderer.rendersAssigned = true;
            }
        }*/
    @SuppressWarnings({"resource", "unchecked", "rawtypes"})


    @SubscribeEvent
    public void onWorldUnLoad(LevelEvent.Unload event) {

    }

    /**
     * Add our custom render player
     *
     * @param event fired before player is rendered
     */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
//        if (event.getPlayer() != null && event.getPlayer().world.isRemote()) {
//            PlayerRenderer renderer = event.getRenderer();
//            // add our custom render layer
//            renderer.addLayer(new FrostbiteRenderer<>(renderer));
//        }
    }

    /*
     * @SubscribeEvent
     * public static void addFutureTempToDebug(RenderGameOverlayEvent.Text event) {
     * Minecraft mc = Minecraft.getInstance();
     * List<String> list = event.getRight();
     * if (mc.gameSettings.showDebugInfo && mc.world != null && mc.player != null) {
     * float currentHourTemp = ClimateData.getTemp(mc.world);
     * float hour1Temp = ClimateData.getFutureTemp(mc.world, 1);
     * float hour2Temp = ClimateData.getFutureTemp(mc.world, 2);
     * float hour3Temp = ClimateData.getFutureTemp(mc.world, 3);
     * float hour4Temp = ClimateData.getFutureTemp(mc.world, 4);
     * float hour5Temp = ClimateData.getFutureTemp(mc.world, 5);
     * float hour6Temp = ClimateData.getFutureTemp(mc.world, 6);
     * float hour7Temp = ClimateData.getFutureTemp(mc.world, 7);
     * float day1Temp = ClimateData.getFutureTemp(mc.world, 1, 0);
     * float day2Temp = ClimateData.getFutureTemp(mc.world, 2, 0);
     * float day3Temp = ClimateData.getFutureTemp(mc.world, 3, 0);
     * float day4Temp = ClimateData.getFutureTemp(mc.world, 4, 0);
     * float day5Temp = ClimateData.getFutureTemp(mc.world, 5, 0);
     * float day6Temp = ClimateData.getFutureTemp(mc.world, 6, 0);
     * float day7Temp = ClimateData.getFutureTemp(mc.world, 7, 0);
     * list.add("TWR Climate Temperature:");
     * list.add(String.format("This Hour: %.1f", currentHourTemp));
     * list.add(String.
     * format("Next 7 Hours: %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f",
     * hour1Temp, hour2Temp, hour3Temp, hour4Temp, hour5Temp, hour6Temp,
     * hour7Temp));
     * list.add(String.
     * format("Next 7 Days: %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f",
     * day1Temp, day2Temp, day3Temp, day4Temp, day5Temp, day6Temp, day7Temp));
     * }
     * }
     */

    @SubscribeEvent
    public static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            //获取渲染信息
            CameraHelper.projectionMatrix = event.getProjectionMatrix();
            CameraHelper.poseStack = event.getPoseStack();
            CameraHelper.frustum = event.getFrustum();
            CameraHelper.camera = event.getCamera();
        }
    }

//    @SubscribeEvent
//    public static void onGUIOpen(ScreenEvent.Opening event) {
//        if (event.getScreen() instanceof TitleScreen) {
//            if (!TipLockManager.errorType.isEmpty()) {
//                TipElement ele = new TipElement();
//                ele.replaceToError(TipLockManager.UNLOCKED_FILE, TipLockManager.errorType);
//                TipDisplayManager.displayTip(ele, true);
//                TipLockManager.errorType = "";
//            }
//        }
//    }


}
