/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

/** Matches actual wire values to GPU values, with independent backend and framebuffer evidence. */
final class InfraredPhysicalE2eClient {
    private static Path root;
    private static String role,phase;
    private static int round,frames,heatedQ;
    private static long opened,lastCheck,geometryReady;
    private static boolean coolingOnly;
    private static int[] baselineRgb;
    private static final Map<String,Object> RESULTS=new LinkedHashMap<>();

    static void start(Path directory,String observer,int number,boolean naturalCooling) {
        root=directory;role=observer;round=number;phase="baseline";frames=0;lastCheck=0;geometryReady=0;RESULTS.clear();
        coolingOnly=naturalCooling;
    }

    static Map<String,Object> results() { return new LinkedHashMap<>(RESULTS); }

    static boolean frame()throws Exception {
        var mc=Minecraft.getInstance();
        if(phase.equals("done"))return true;
        if(phase.equals("baseline")) {
            var fire=mc.level.getBlockState(new BlockPos(-4,66,-1));
            if(!mc.level.getBlockState(new BlockPos(-4,67,0)).is(Blocks.IRON_BLOCK)
                    ||!fire.is(Blocks.CAMPFIRE)||fire.getValue(CampfireBlock.LIT))return false;
            if(++frames<25)return false;
            baselineRgb=pixels("baseline");InfraredViewRenderer.toggleInfraredView();
            opened=System.nanoTime();phase="initial";return false;
        }
        if(phase.equals("initial")) {
            if(System.nanoTime()-opened<2_000_000_000L)return false;
            pixels("initial");receipt("baseline");phase="heated";return false;
        }
        if(System.nanoTime()-lastCheck<500_000_000L)return false;lastCheck=System.nanoTime();
        Path stateFile=root.resolve("physical-state.json"),wireFile=root.resolve("wire-samples-"+role+".json");
        if(!Files.exists(stateFile)||!Files.exists(wireFile))return false;
        JsonObject state=JsonParser.parseString(Files.readString(stateFile)).getAsJsonObject();
        if(state.get("analyticFields").getAsInt()!=0)throw new IllegalStateException("Physical test acquired analytic fields");
        String status=InfraredViewRenderer.diagnosticStatus();
        if(!status.contains("baseline=true")||status.contains("staging=true"))return false;
        var match=Pattern.compile("(?:^| )display=(\\d+)").matcher(status);
        if(!match.find())return false;
        String id=match.group(1);
        JsonObject replies=JsonParser.parseString(Files.readString(wireFile)).getAsJsonObject();
        if(!replies.has(id))return false;
        JsonObject expected=replies.getAsJsonObject(id);
        short[] gpu=InfraredE2eClient.gpu(status);
        if(gpu[0]!=expected.get("warm").getAsShort()||gpu[1]!=expected.get("cold").getAsShort())
            throw new IllegalStateException("GPU differs from actual server wire for display "+id+": "+gpu[0]+","+gpu[1]+" expected="+expected);
        Map<String,Object> observed=new LinkedHashMap<>();
        observed.put("phase",phase);observed.put("elapsedMs",(System.nanoTime()-opened)/1_000_000.0);
        observed.put("status",status);observed.put("gpuWarmQ",gpu[0]);observed.put("gpuColdQ",gpu[1]);
        observed.put("wire",expected);observed.put("backend",state);
        write(role+"-round-"+round+"-physics-latest.json",observed);
        int baseline=state.get("baselineQ").getAsInt();
        if(coolingOnly&&state.get("missingTicks").getAsLong()>=240&&gpu[0]!=Short.MIN_VALUE)
            throw new IllegalStateException("Naturally missing server material remains in infrared after 12 seconds: "+observed);
        boolean ready=switch(phase) {
            case "heated" -> state.get("materialHeated").getAsBoolean()&&gpu[0]>=baseline+2;
            case "cooled" -> state.get("airCooled").getAsBoolean()&&(coolingOnly?
                    state.get("materialCooled").getAsBoolean()&&(gpu[0]==Short.MIN_VALUE||gpu[0]<=state.get("peakMaterialQ").getAsInt()-1)
                    :gpu[0]!=heatedQ&&gpu[0]!=Short.MIN_VALUE);
            case "natural-cleared" -> state.get("naturalCleared").getAsBoolean()&&gpu[0]==Short.MIN_VALUE;
            case "removed" -> mc.level.getBlockState(new BlockPos(-4,67,0)).isAir()&&gpu[0]==Short.MIN_VALUE;
            case "restored" -> mc.level.getBlockState(new BlockPos(-4,67,0)).is(Blocks.IRON_BLOCK)
                    &&(gpu[0]==Short.MIN_VALUE||gpu[0]<=baseline+2);
            default -> false;
        };
        if(!ready){geometryReady=0;return false;}
        if(phase.equals("removed")||phase.equals("restored")) {
            if(geometryReady==0)geometryReady=System.nanoTime();
            if(System.nanoTime()-geometryReady<1_000_000_000L)return false;
        }
        int[] rgb=pixels(phase);observed.put("frameRgb",rgb);observed.put("baselineRgb",baselineRgb);
        if(phase.equals("heated")||phase.equals("restored")||phase.equals("natural-cleared")) {
            double[] color=color(gpu[0]==Short.MIN_VALUE?-20:gpu[0]*.25);double error=0,blueError=0;
            for(int c=0;c<3;c++) {
                double wanted=baselineRgb[c]*.57+color[c]*255*.43;
                double blue=baselineRgb[c]*.57+(c==2?255*.43:0);
                error+=Math.abs(rgb[c]-wanted);blueError+=Math.abs(rgb[c]-blue);
            }
            observed.put("expectedColorError",error);observed.put("bluePlaceholderError",blueError);
            if(error>60||phase.equals("heated")&&error+20>=blueError)throw new IllegalStateException("Published physical temperature reached GPU but framebuffer does not match: "+observed);
        }
        write(role+"-round-"+round+"-physics-"+phase+".json",observed);RESULTS.put(phase,observed);
        receipt(phase);
        geometryReady=0;
        if(phase.equals("heated")){heatedQ=gpu[0];phase="cooled";}
        else if(phase.equals("cooled"))phase=coolingOnly?"natural-cleared":"removed";
        else if(phase.equals("removed"))phase="restored";
        else phase="done";
        return phase.equals("done");
    }

    private static void receipt(String stage) { Minecraft.getInstance().player.connection.sendCommand("ir_e2e physical_"+stage); }

    private static double[] color(double temperature) {
        double n=Math.max(0,Math.min(1,(temperature+20)/40));
        if(n<.33){double t=n/.33;return new double[]{t,.5*t,1-t};}
        if(n<.66){double t=(n-.33)/.33;return new double[]{1,.5+.5*t,0};}
        return new double[]{1,1-(n-.66)/.34,0};
    }

    private static int[] pixels(String label)throws Exception {
        var mc=Minecraft.getInstance();long[] sum=new long[3];int count=0;
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Files.createDirectories(root.resolve("images"));image.writeToFile(root.resolve("images/"+role+"-r"+round+"-physics-"+label+".png"));
            var camera=mc.gameRenderer.getMainCamera().getPosition();
            double focal=image.getHeight()*RenderSystem.getProjectionMatrix().m11()*.5;
            double depth=camera.z-1.0;
            int cx=(int)Math.round(image.getWidth()*.5+(-3.5-camera.x)*focal/depth);
            int cy=(int)Math.round(image.getHeight()*.5-(67.5-camera.y)*focal/depth);
            for(int y=cy-3;y<=cy+3;y++)for(int x=cx-3;x<=cx+3;x++) {
                int pixel=image.getPixelRGBA(x,y);sum[0]+=pixel&255;sum[1]+=(pixel>>>8)&255;sum[2]+=(pixel>>>16)&255;count++;
            }
        }
        return new int[]{(int)(sum[0]/count),(int)(sum[1]/count),(int)(sum[2]/count)};
    }

    private static void write(String name,Object data)throws Exception {
        Files.writeString(root.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(data));
    }
}
