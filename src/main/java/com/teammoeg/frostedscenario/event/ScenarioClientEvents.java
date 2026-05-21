package com.teammoeg.frostedscenario.event;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.PartialTickTracker;
import com.teammoeg.chorda.client.ui.TextPosition;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.math.Point;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedscenario.FSConfig;
import com.teammoeg.frostedscenario.FSMain;
import com.teammoeg.frostedscenario.client.ClientScene;
import com.teammoeg.frostedscenario.client.dialog.HUDDialog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FSMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value=Dist.CLIENT)
public class ScenarioClientEvents {
    @SubscribeEvent
    public static void fireLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientScene.INSTANCE = new ClientScene();
        ClientScene.INSTANCE.sendClientReady();

    }
    static final TextPosition act_title = new TextPosition(5, 70);
    static final Point act_split = new Point(5, 80);
    static final TextPosition act_subtitle = new TextPosition(5, 83);
    public static void renderScenarioAct(GuiGraphics stack, int x, int y, Minecraft mc) {
        mc.getProfiler().push("frostedheart_scenario_act");
       /* double guiScale = mc.getMainWindow().getGuiScaleFactor();
        int ww = mc.getMainWindow().getScaledWidth();
		int wh = mc.getMainWindow().getScaledHeight();
    	float scale = (float) (FTBChunksClientConfig.MINIMAP_SCALE.get() * 4D / guiScale);
    	float minimapRotation = (FTBChunksClientConfig.MINIMAP_LOCKED_NORTH.get() ? 180F : -mc.player.rotationYaw) % 360F;

    	int s = (int) (64D * scale);
    	double s2d = s / 2D;
    	float s2f = s / 2F;
    	int x = FTBChunksClientConfig.MINIMAP_POSITION.get().getX(ww, s);
    	int y = FTBChunksClientConfig.MINIMAP_POSITION.get().getY(wh, s);//Render our act screen in lower than map;
    	*/
        RenderSystem.enableBlend();

        if (ClientScene.INSTANCE!=null) {
        	Component t=ClientScene.INSTANCE.getCurrentActTitle();
        	Component st=ClientScene.INSTANCE.getCurrentActSubtitle();
        	if(t!=null||st!=null) {
        		int deflen=60;
	        	
	            if(t!=null) { 
	            	int len=mc.font.width(t.getString());
	            	deflen=Math.max(deflen, len-30);
	            	if(ClientScene.INSTANCE.ticksActUpdate>0)
	            		stack.enableScissor( act_title.getX(), act_title.getY(), act_title.getX()+(int) (len*(1-ClientScene.INSTANCE.ticksActUpdate/20f)),act_title.getY()+40);
	            	act_title.drawText(stack, t, 0xfeff06);
	            	if(ClientScene.INSTANCE.ticksActUpdate>0)
	            		stack.disableScissor();
	            }
	            stack.hLine(act_split.getX(), act_split.getX()+deflen, act_split.getY(), 0xFFFFFF06);
	            
	            if(st!=null) {
	            	int len=mc.font.width(st.getString());
	            	if(ClientScene.INSTANCE.ticksActStUpdate>0)
	            		stack.enableScissor(act_title.getX(), act_title.getY(), act_title.getX()+(int) (len*(1-ClientScene.INSTANCE.ticksActStUpdate/20f)),act_title.getY()+40);
	            	act_subtitle.drawText(stack, st, 0xffffff);
	            	if(ClientScene.INSTANCE.ticksActStUpdate>0)
	            		stack.disableScissor();
	            }
	            
        	}
            
        }
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderCustomHUD(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer clientPlayer = mc.player;

        if (clientPlayer == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics stack = event.getGuiGraphics();
        int anchorX = event.getWindow().getGuiScaledWidth() / 2;
        int anchorY = event.getWindow().getGuiScaledHeight();
        float partialTicks = event.getPartialTick();
    	 if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {

             if (ClientScene.INSTANCE != null && ClientScene.INSTANCE.dialog instanceof HUDDialog dialog) {
                 dialog.render(stack, 0, 0, PartialTickTracker.getTickAlignedPartialTicks());
             }
	         if (FSConfig.CLIENT.renderScenario.get())
	             renderScenarioAct(stack, anchorX, anchorY, mc);
             
         }
    }
    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void tickClient(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            Minecraft mc = ClientUtils.getMc();
            if (mc.level != null) {
                if (ClientScene.INSTANCE != null)
                    ClientScene.INSTANCE.tick(mc);
            }
        }
    }
    @SubscribeEvent
    public static void onClientKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            // skip scenario dialog
            if (FSMain.key_skipDialog.get().consumeClick()) {
                if (ClientScene.INSTANCE != null)
                    ClientScene.INSTANCE.sendContinuePacket(true);
                //event.setCanceled(true);
            }

        }
    }
}
