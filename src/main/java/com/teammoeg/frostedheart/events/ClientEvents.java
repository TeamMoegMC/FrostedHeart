/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import com.teammoeg.frostedheart.*;
import com.teammoeg.frostedheart.base.team.FHClientTeamDataManager;
import com.teammoeg.frostedheart.client.hud.FrostedHud;
import com.teammoeg.frostedheart.client.renderer.InfraredViewRenderer;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.research.events.ClientResearchStatusEvent;
import com.teammoeg.frostedheart.content.research.gui.ResearchToast;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.content.research.research.effects.EffectCrafting;
import com.teammoeg.frostedheart.content.research.research.effects.EffectShowCategory;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.dialog.HUDDialog;
import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import com.teammoeg.frostedheart.content.tips.TipLockManager;
import com.teammoeg.frostedheart.content.tips.client.TipElement;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.GuiClickedEvent;
import com.teammoeg.frostedheart.util.client.RenderHelper;
import com.teammoeg.frostedheart.util.version.FHVersion;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SuppressWarnings({"unchecked"})
    @SubscribeEvent
    public static void drawUpdateReminder(ScreenEvent.Render event) {
        Screen gui = event.getScreen();
        if (gui instanceof TitleScreen) {
            FHMain.remote.fetchVersion().ifPresent(stableVersion -> {
                boolean isStable = true;
                if (FHMain.pre != null && FHMain.pre.fetchVersion().isPresent()) {
                    FHVersion preversion = FHMain.pre.fetchVersion().resolve().get();
                    if (preversion.laterThan(stableVersion)) {
                        stableVersion = preversion;
                        isStable = false;
                    }
                }
                if (stableVersion.isEmpty())
                    return;
                GuiGraphics matrixStack = event.getGuiGraphics();
                FHVersion clientVersion = FHMain.local.fetchVersion().orElse(FHVersion.empty);
                Font font = gui.getMinecraft().font;
                if (!stableVersion.isEmpty() && (clientVersion.isEmpty() || !clientVersion.laterThan(stableVersion))) {
                    List<FormattedCharSequence> list = font.split(TranslateUtils.translateGui("update_recommended")
                            .append(stableVersion.getOriginal()).withStyle(ChatFormatting.BOLD), 70);
                    int l = 0;
                    for (FormattedCharSequence line : list) {
                        FHGuiHelper.drawLine(matrixStack.pose(), Color4I.rgba(0, 0, 0, 255), 0, gui.height / 2 - 1 + l, 72,
                                gui.height / 2 + 9 + l);
                        matrixStack.drawString(ClientUtils.mc().font, line, 1, gui.height / 2.0F + l, 0xFFFFFF, true);
                        l += 9;
                    }
                    if (isStable) {
                        MutableComponent itxc = TranslateUtils.str("CurseForge")
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
    public static void onResearchStatus(ClientResearchStatusEvent event) {
        if (event.isStatusChanged()) {
            if (event.isCompletion())
                ClientUtils.mc().getToasts().addToast(new ResearchToast(event.getResearch()));
        } else if (!event.isCompletion())
            return;
        for (Effect e : event.getResearch().getEffects())
            if (e instanceof EffectCrafting || e instanceof EffectShowCategory) {
                JEICompat.syncJEI();
                return;
            }
    }

    @SubscribeEvent
    public static void fireLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FHClientTeamDataManager.INSTANCE::reset);
        // TODO: temporary fix for client not sending ready packet
        ClientScene.INSTANCE = new ClientScene();
        ClientScene.INSTANCE.sendClientReady();

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
                    dialog.render(stack, 0, 0, partialTicks);
                }
                event.setCanceled(true);
            }
            if (event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type() && FrostedHud.renderExperience) {
                if (FrostedHud.renderHypothermia) {
                    FrostedHud.renderHypothermia(stack, anchorX, anchorY, mc, clientPlayer);
                } else {
                    // TODO: Provide some method to render both
//                    FrostedHud.renderExperience(stack, anchorX, anchorY, mc, clientPlayer);
                    FrostedHud.renderInsight(stack, anchorX, anchorY, mc, clientPlayer);
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
            if (FHMain.pre != null && FHMain.pre.fetchVersion().isPresent()) {
                FHVersion preversion = FHMain.pre.fetchVersion().resolve().get();
                if (preversion.laterThan(stableVersion)) {
                    stableVersion = preversion;
                    isStable = false;
                }
            }
            if (stableVersion.isEmpty())
                return;
            FHVersion clientVersion = FHMain.local.fetchVersion().orElse(FHVersion.empty);
            if (!stableVersion.isEmpty() && (clientVersion.isEmpty() || !clientVersion.laterThan(stableVersion))) {
                event.getPlayer().displayClientMessage(TranslateUtils.translateGui("update_recommended")
                        .append(stableVersion.getOriginal()).withStyle(ChatFormatting.BOLD), false);
                if (isStable) {
                    event.getPlayer()
                            .displayClientMessage(TranslateUtils.str("CurseForge")
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
                        TranslateUtils.translateGui("save_update_needed", FHMain.lastServerConfig.getAbsolutePath())
                                .withStyle(ChatFormatting.RED),
                        false);
            } else if (FHMain.lastbkf != null) {
                event.getPlayer().displayClientMessage(TranslateUtils.translateGui("save_updated")
                                .append(TranslateUtils.str(FHMain.lastbkf.getName()).setStyle(Style.EMPTY
                                        .withClickEvent(
                                                new ClickEvent(ClickEvent.Action.OPEN_FILE, FHMain.lastbkf.getAbsolutePath()))
                                        .applyFormat(ChatFormatting.UNDERLINE))),
                        false);
            }

    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {

        // event.addListener(KGlyphProvider.INSTANCE);

    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void tickClient(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            InfraredViewRenderer.clientTick();
            if (ClientUtils.mc().level != null) {
                Minecraft mc = ClientUtils.mc();
                if (ClientScene.INSTANCE != null)
                    ClientScene.INSTANCE.tick(mc);

            }
            Player pe = ClientUtils.getPlayer();
            PlayerTemperatureData.getCapability(pe).ifPresent(t -> {
                t.smoothedBodyPrev = t.smoothedBody;
                t.smoothedBody = t.smoothedBody * .9f + t.getBodyTemp() * .1f;
            });

            if (pe != null && pe.getEffect(FHMobEffects.NYCTALOPIA.get()) != null) {
                ClientUtils.applyspg = true;
                ClientUtils.spgamma = Mth.clamp((float) (double) (ClientUtils.mc().options.gamma().get()), 0f, 1f) * 0.1f
                        - 1f;
            } else {
                ClientUtils.applyspg = false;
                ClientUtils.spgamma = Mth.clamp((float) (double) ClientUtils.mc().options.gamma().get(), 0f, 1f);
            }
        }
    }

    @SubscribeEvent
    public static void unloadWorld(LevelEvent.Unload event) {
        ClientUtils.applyspg = false;
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

    @SubscribeEvent
    public static void onClientKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (ClientRegistryEvents.key_skipDialog.get().consumeClick()) {
                if (ClientScene.INSTANCE != null)
                    ClientScene.INSTANCE.sendContinuePacket(true);
                //event.setCanceled(true);
            } else if (ClientRegistryEvents.key_InfraredView.get().consumeClick()) {
                InfraredViewRenderer.toggleInfraredView();
            }
        }
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
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            //获取渲染信息
            RenderHelper.projectionMatrix = event.getProjectionMatrix();
            RenderHelper.poseStack = event.getPoseStack();
            RenderHelper.frustum = event.getFrustum();
            RenderHelper.camera = event.getCamera();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // write a default tip with form { "contents": ["Default Tip."] } in config/fhtips/tips/default.json
        File filePath = new File(TipLockManager.TIPS, "default.json");
        if (!filePath.exists()) {

            // Write the file to the file system as a json file
            String content = "{\"contents\": [\"Default Tip.\"]}";
            // Write the file to the file system as a json file
            // Use raw java methods
            try {
                Files.write(Paths.get(filePath.getAbsolutePath()), content.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        TipDisplayManager.clearRenderQueue();
        TipDisplayManager.displayTip("default", false);
//        TipDisplayManager.displayTip("default2", false);
//        if (Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.MUSIC) == 0) {
//            TipDisplayManager.displayTip("music_warning", false);
//        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TipDisplayManager.clearRenderQueue();
        ClientWaypointManager.clear();
    }

    @SubscribeEvent
    public static void onGUIOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen) {
            if (!TipLockManager.errorType.isEmpty()) {
                TipElement ele = new TipElement();
                ele.replaceToError(TipLockManager.UNLOCKED_FILE, TipLockManager.errorType);
                TipDisplayManager.displayTip(ele, true);
                TipLockManager.errorType = "";
            }
        }
    }

//    @SubscribeEvent
//    public static void onGUIRender(ScreenEvent.Render event) {
//        if (event.getScreen() instanceof SoundOptionsScreen) {
//            if (Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC) <= 0) {
//                TipDisplayManager.displayTip("music_warning", false);
//            }
//        }
//    }


}
