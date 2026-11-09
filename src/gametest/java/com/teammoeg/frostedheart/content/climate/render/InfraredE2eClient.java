/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/** Actual client connection, S2C handling, GL_R16I readback and framebuffer evidence. */
@Mod.EventBusSubscriber(modid=FHMain.MODID,value=Dist.CLIENT)
public final class InfraredE2eClient {
    private static final boolean ENABLED=Boolean.getBoolean("frostedheart.irE2eClient");
    private static final Path ROOT=Path.of(System.getProperty("frostedheart.irE2eRoot","build/infrared-e2e-20261108"));
    private static final String ROLE=System.getProperty("frostedheart.irE2eRole","fast");
    private static final long START=System.nanoTime();
    private static final ShortBuffer READBACK=ENABLED?BufferUtils.createShortBuffer(144*144*144):null;
    private static boolean connected,worldReady,finished,hidden,savedPreview,physical,coolingOnly;
    private static int round=-1,frames,badPixelFrames,connectAttempts;
    private static String phase="joining";
    private static long roundStart,opened,lastCheck,lastJoining;
    private static int[] baseline,hotPixels;
    private static final Map<String,Object> RESULTS=new LinkedHashMap<>();

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!ENABLED||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            if(!hidden){GLFW.glfwHideWindow(mc.getWindow().getWindow());hidden=true;}
            mc.options.pauseOnLostFocus=false;
            mc.options.framerateLimit().set(30);mc.options.renderDistance().set(5);mc.options.fov().set(70);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if(System.nanoTime()-START>(coolingOnly?1_800_000_000_000L:900_000_000_000L))throw new IllegalStateException("Client E2E run timed out");
            if(mc.getOverlay()==null&&mc.screen instanceof AccessibilityOnboardingScreen onboarding){onboarding.onClose();return;}
            if(connected&&mc.screen instanceof DisconnectedScreen) {
                if(worldReady)throw new IllegalStateException("Connected E2E player was disconnected during round "+round);
                if(connectAttempts>=3)throw new IllegalStateException("Three local login attempts disconnected");
                connected=false;mc.setScreen(new TitleScreen());return;
            }
            if(!connected&&mc.getOverlay()==null&&mc.screen instanceof TitleScreen&&Files.exists(ROOT.resolve("server-ready.json"))) {
                connected=true;connectAttempts++;
                String address="127.0.0.1:"+Integer.getInteger("frostedheart.irE2ePort",25573);
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("IR E2E",address,false),false);
                return;
            }
            if(mc.level==null||mc.player==null) {
                if(System.nanoTime()-lastJoining>5_000_000_000L) {
                    lastJoining=System.nanoTime();
                    FHMain.LOGGER.info("IR_E2E_JOIN {} screen={} overlay={} connected={}",ROLE,
                            mc.screen==null?null:mc.screen.getClass().getName(),
                            mc.getOverlay()==null?null:mc.getOverlay().getClass().getName(),connected);
                    if(connected)pixels("joining");
                }
                return;
            }
            mc.setScreen(null);mc.options.hideGui=true;
            mc.player.setYRot(180);mc.player.setXRot(0);
            mc.mouseHandler.releaseMouse();
            var wall=mc.level.getBlockState(new BlockPos(-4,67,0));
            if(!worldReady&&!wall.is(Blocks.STONE)&&!wall.is(Blocks.IRON_BLOCK))return;
            if(!worldReady){worldReady=true;write(ROLE+"-world-ready.json",Map.of("ready",true));}
            Path control=ROOT.resolve("round.json");
            if(Files.exists(control)) {
                int next=JsonParser.parseString(Files.readString(control)).getAsJsonObject().get("round").getAsInt();
                if(next!=round&&Files.exists(ROOT.resolve("server-round-"+next+".json"))&&Files.exists(ROOT.resolve("start-round-"+next))) {
                    round=next;phase="baseline";frames=0;badPixelFrames=0;savedPreview=false;roundStart=System.nanoTime();
                    InfraredViewRenderer.reset();InfraredViewRenderer.setDiagnosticTrace(true);
                    var profile=JsonParser.parseString(Files.readString(control)).getAsJsonObject();
                    physical=profile.has("physical")&&profile.get("physical").getAsBoolean();
                    coolingOnly=profile.has("coolingOnly")&&profile.get("coolingOnly").getAsBoolean();
                    if(physical){phase="physical";InfraredPhysicalE2eClient.start(ROOT,ROLE,round,coolingOnly);}
                    if(ROLE.equals("fast")&&profile.has("onlySlow")&&profile.get("onlySlow").getAsBoolean())phase="done";
                }
            }
            if(Files.exists(ROOT.resolve("finish"))){finish(null);return;}
            if(round>=0&&!phase.equals("done")&&System.nanoTime()-roundStart>(coolingOnly?900_000_000_000L:physical?420_000_000_000L:240_000_000_000L))
                throw new IllegalStateException("Round timeout: "+phase+" "+InfraredViewRenderer.diagnosticStatus());
        }catch(java.nio.file.NoSuchFileException transientFixtureUpdate) { }
        catch(Throwable failure){finish(failure);}
    }

    @SubscribeEvent public static void frame(RenderLevelStageEvent event) {
        if(!ENABLED||finished||round<0||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();
        try {
            if(physical) {
                if(InfraredPhysicalE2eClient.frame()){phase="done";RESULTS.put("round-"+round+"-physics",InfraredPhysicalE2eClient.results());}
                return;
            }
            frames++;
            if(phase.equals("baseline")&&frames>=25) {
                baseline=pixels("baseline");
                InfraredViewRenderer.toggleInfraredView();opened=System.nanoTime();lastCheck=0;phase="warm";
                return;
            }
            if(phase.equals("done")||phase.equals("baseline")||System.nanoTime()-lastCheck<500_000_000L)return;
            lastCheck=System.nanoTime();
            String status=InfraredViewRenderer.diagnosticStatus();
            if(number(status,"dataParts")==0||System.nanoTime()-opened<1_500_000_000L)return;
            short[] samples=gpu(status);
            boolean ready=phase.equals("warm")?samples[0]==160&&samples[1]==-160
                    :phase.equals("updated")?samples[0]==240&&samples[1]==-160
                    :samples[0]<80&&samples[1]==-160;
            Map<String,Object> observed=new LinkedHashMap<>();
            observed.put("phase",phase);observed.put("status",status);observed.put("warm",samples[0]);observed.put("cold",samples[1]);
            observed.put("elapsedMs",(System.nanoTime()-opened)/1_000_000.0);
            write(ROLE+"-round-"+round+"-latest.json",observed);
            if(!ready)return;
            int[] image=pixels(phase);
            observed.put("redPixels",image[0]);observed.put("bluePixels",image[1]);observed.put("baselineRed",baseline[0]);observed.put("baselineBlue",baseline[1]);
            boolean colors=phase.equals("removed")?image[0]<hotPixels[0]-500
                    :image[0]>baseline[0]+500&&image[1]>baseline[1]+500;
            if(!colors) {
                if(++badPixelFrames>=12)throw new IllegalStateException("GPU temperatures arrived but framebuffer heat is absent: "+observed);
                return;
            }
            badPixelFrames=0;
            if(number(status,"commits")==0) {
                if(!savedPreview) {
                    savedPreview=true;pixels("preview");
                    write(ROLE+"-round-"+round+"-preview.json",observed);
                    RESULTS.put("round-"+round+"-preview",observed);
                }
                return;
            }
            write(ROLE+"-round-"+round+"-"+phase+".json",observed);
            RESULTS.put("round-"+round+"-"+phase,observed);
            if(phase.equals("warm")){hotPixels=image;phase="updated";mc.player.connection.sendCommand("ir_e2e ready");}
            else if(phase.equals("updated")){phase="removed";mc.player.connection.sendCommand("ir_e2e updated");}
            else {phase="done";mc.player.connection.sendCommand("ir_e2e done");}
        }catch(java.nio.file.NoSuchFileException transientFixtureUpdate) { }
        catch(Throwable failure){finish(failure);}
    }

    private static long number(String status,String name) {
        var match=Pattern.compile("(?:^| )"+name+"=(\\d+)").matcher(status);
        return match.find()?Long.parseLong(match.group(1)):0;
    }

    static short[] gpu(String status) {
        int texture=InfraredViewRenderer.frameTemperatureTexture();
        if(texture==0)return new short[]{Short.MIN_VALUE,Short.MIN_VALUE};
        var center=Pattern.compile("center=(-?\\d+),(-?\\d+),(-?\\d+)").matcher(status);
        if(!center.find())throw new IllegalStateException(status);
        int ox=(Integer.parseInt(center.group(1))-4)*16,oy=(Integer.parseInt(center.group(2))-4)*16,oz=(Integer.parseInt(center.group(3))-4)*16;
        int binding=GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D),pbo=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] names={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL12.GL_PACK_IMAGE_HEIGHT,GL11.GL_PACK_SKIP_PIXELS,GL11.GL_PACK_SKIP_ROWS,GL12.GL_PACK_SKIP_IMAGES};
        int[] old=new int[names.length];for(int i=0;i<names.length;i++)old[i]=GL11.glGetInteger(names[i]);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],i==0?2:0);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D,texture);READBACK.clear();
            GL11.glGetTexImage(GL12.GL_TEXTURE_3D,0,GL30.GL_RED_INTEGER,GL11.GL_SHORT,READBACK);
            int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("GL readback error "+error);
            return new short[]{sample(-4,67,0,ox,oy,oz),sample(4,67,0,ox,oy,oz)};
        }finally {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D,binding);GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo);
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],old[i]);
        }
    }
    private static short sample(int x,int y,int z,int ox,int oy,int oz) {
        x-=ox;y-=oy;z-=oz;
        return x<0||x>=144||y<0||y>=144||z<0||z>=144?Short.MIN_VALUE:READBACK.get((z*144+y)*144+x);
    }

    private static int[] pixels(String label)throws Exception {
        var mc=Minecraft.getInstance();int red=0,blue=0;
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Files.createDirectories(ROOT.resolve("images"));
            image.writeToFile(ROOT.resolve("images/"+ROLE+"-r"+round+"-"+label+".png"));
            for(int y=image.getHeight()/5;y<image.getHeight()*4/5;y++)for(int x=image.getWidth()/10;x<image.getWidth()*9/10;x++) {
                int pixel=image.getPixelRGBA(x,y),r=pixel&255,g=(pixel>>>8)&255,b=(pixel>>>16)&255;
                // Ambient -11 C is reddish on this palette; count saturated heat separately.
                if(r>g+64&&r>b+64)red++;if(b>r+32&&b>g+32)blue++;
            }
        }
        return new int[]{red,blue};
    }

    private static void write(String name,Object data)throws Exception {
        Files.createDirectories(ROOT);Files.writeString(ROOT.resolve(name),new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data));
    }
    private static void finish(Throwable failure) {
        if(finished)return;finished=true;
        try {
            var result=new LinkedHashMap<String,Object>();result.put("passed",failure==null);result.put("failure",failure==null?null:failure.toString());result.put("rounds",RESULTS);
            write(ROLE+"-result.json",result);FHMain.LOGGER.info("IR_E2E_CLIENT_RESULT {} {}",ROLE,result);
        }catch(Exception writeFailure){writeFailure.printStackTrace();}
        if(failure!=null)failure.printStackTrace();Minecraft.getInstance().stop();
    }
}
