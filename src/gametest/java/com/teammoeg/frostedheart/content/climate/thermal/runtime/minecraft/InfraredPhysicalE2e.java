/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Real world actions and public backend reads only: no analytic field or numerical heat writes. */
final class InfraredPhysicalE2e {
    private static final BlockPos WALL=new BlockPos(-4,67,0);
    private static final Set<String> BOTH=Set.of("IRFast","IRSlow");
    private static final Map<String,Set<String>> RECEIPTS=new HashMap<>();
    private static final MaterialSample MATERIAL=new MaterialSample();
    private static final ThermalEnvironmentSample NEAR=new ThermalEnvironmentSample(),FAR=new ThermalEnvironmentSample();
    private static Path root;
    private static int round,phase;
    private static long started,stopped;
    private static double baseline=Double.NaN,peakAir=Double.NEGATIVE_INFINITY,stopAir=Double.NaN;
    private static boolean coolingOnly,probed;
    private static long missingSince=-1;
    private static double peakMaterial=Double.NEGATIVE_INFINITY;
    private static int firstFireX,lastFireX;

    static void start(MinecraftServer server,Path directory,int nextRound,boolean naturalCooling)throws Exception {
        root=directory;round=nextRound;phase=0;started=server.overworld().getGameTime();
        baseline=Double.NaN;peakAir=Double.NEGATIVE_INFINITY;RECEIPTS.clear();
        coolingOnly=naturalCooling;missingSince=-1;peakMaterial=Double.NEGATIVE_INFINITY;probed=false;
        firstFireX=naturalCooling?-4:-6;lastFireX=naturalCooling?-4:-2;
        var level=server.overworld();
        var fields=MinecraftGameplayFields.existing(level);
        if(fields!=null&&!fields.isEmpty())throw new IllegalStateException("Physical scene still contains analytic fields");
        level.setBlockAndUpdate(new BlockPos(0,65,3),Blocks.AIR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        for(int y=64;y<=70;y++)for(int z=-5;z<=0;z++)for(int x=-8;x<=-1;x++)
            level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        if(!naturalCooling)for(int y=64;y<=70;y++)for(int z=-5;z<=0;z++)for(int x=-8;x<=-1;x++) {
            boolean shell=x==-8||x==-1||y==64||y==70||z==-5||z==0;
            if(shell)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
        }
        if(!naturalCooling)for(int x=-6;x<=-2;x++)for(int y=65;y<=69;y++)
            level.setBlockAndUpdate(new BlockPos(x,y,0),Blocks.IRON_BLOCK.defaultBlockState());
        if(naturalCooling)level.setBlockAndUpdate(WALL,Blocks.IRON_BLOCK.defaultBlockState());
        for(int x=2;x<=6;x++)for(int y=65;y<=69;y++)
            level.setBlockAndUpdate(new BlockPos(x,y,0),Blocks.IRON_BLOCK.defaultBlockState());
        for(int x=firstFireX;x<=lastFireX;x++) {
            BlockPos fire=new BlockPos(x,66,-1);
            level.setBlockAndUpdate(fire.below(),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(fire,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));
            var fuel=level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .orElseThrow(()->new IllegalStateException("Actual campfire fuel capability missing"));
            if(!fuel.insertItem(0,new ItemStack(Items.COAL,4),false).isEmpty())
                throw new IllegalStateException("Actual campfire rejected fuel");
        }
        write("physical-scene.json",Map.of("physical",true,"campfires",lastFireX-firstFireX+1,"analyticFields",0,"warmPoint","-4,67,0","outdoor",naturalCooling));
    }

    static void receipt(String stage,String name) { RECEIPTS.computeIfAbsent(stage,k->new HashSet<>()).add(name); }
    private static boolean received(String stage) { return RECEIPTS.getOrDefault(stage,Set.of()).containsAll(BOTH); }

    static void tick(MinecraftServer server)throws Exception {
        var level=server.overworld();long tick=level.getGameTime();
        var fields=MinecraftGameplayFields.existing(level);
        if(fields!=null&&!fields.isEmpty())throw new IllegalStateException("Analytic field entered physical scene");
        if(phase==0&&received("physical_baseline")) {
            for(int x=firstFireX;x<=lastFireX;x++)ContinuousAirProductionGameTests.useItem(level,new BlockPos(x,66,-1),Items.FLINT_AND_STEEL);
            phase=1;
        }
        if(tick%20!=0)return;
        ContinuousAirProductionGameTests.sampleAt(level,-3.5,67.5,-.5,NEAR);
        ContinuousAirProductionGameTests.sampleAt(level,-6.5,67.5,-3.5,FAR);
        boolean material=MinecraftThermalInput.sampleMaterial(level,WALL,MATERIAL);
        double temperature=material?MATERIAL.temperatureC():Double.NaN;
        if(phase>=1&&material)peakMaterial=Math.max(peakMaterial,temperature);
        if(phase>=2&&!material) { if(missingSince<0)missingSince=tick; }
        else missingSince=-1;
        if(phase>=1&&Double.isNaN(baseline)&&material&&MATERIAL.source()==MaterialSample.Source.LIVE)baseline=temperature;
        if(NEAR.airAvailable())peakAir=Math.max(peakAir,NEAR.airTemperatureC());
        boolean airHeated=NEAR.airAvailable()&&FAR.airAvailable()&&NEAR.airBasisTerms()>=8
                &&NEAR.airTemperatureC()>FAR.airTemperatureC()+.05;
        boolean heated=phase>=1&&airHeated&&material&&MATERIAL.source()==MaterialSample.Source.LIVE
                &&Double.isFinite(baseline)&&temperature>=baseline+.5;
        if(phase==1&&received("physical_heated")) {
            for(int x=firstFireX;x<=lastFireX;x++)ContinuousAirProductionGameTests.useItem(level,new BlockPos(x,66,-1),Items.IRON_SHOVEL);
            stopped=tick;stopAir=NEAR.airTemperatureC();phase=2;
        }
        boolean airCooled=phase>=2&&NEAR.airAvailable()&&tick-stopped>=80&&NEAR.airTemperatureC()<stopAir-.05;
        if(coolingOnly) {
            if(phase==2&&received("physical_cooled"))phase=3;
            if(phase==3&&received("physical_natural-cleared")){phase=5;write("round-complete-"+round+".json",Map.of("passed",true,"physical",true,"naturalCooling",true));}
        } else {
            if(phase==2&&received("physical_cooled")) { level.setBlockAndUpdate(WALL,Blocks.AIR.defaultBlockState());phase=3; }
            else if(phase==3&&received("physical_removed")) { level.setBlockAndUpdate(WALL,Blocks.IRON_BLOCK.defaultBlockState());phase=4; }
            else if(phase==4&&received("physical_restored")) { phase=5;write("round-complete-"+round+".json",Map.of("passed",true,"physical",true)); }
        }
        var state=new LinkedHashMap<String,Object>();
        state.put("phase",phase);state.put("tick",tick);state.put("elapsedTicks",tick-started);state.put("analyticFields",0);
        state.put("materialC",temperature);state.put("materialSource",material?MATERIAL.source().name():"UNAVAILABLE");
        state.put("materialQ",material?InfraredBrickCodec.quantize(temperature):Short.MIN_VALUE);
        state.put("baselineC",baseline);state.put("baselineQ",Double.isFinite(baseline)?InfraredBrickCodec.quantize(baseline):Short.MIN_VALUE);
        state.put("airNearC",NEAR.airTemperatureC());state.put("airFarC",FAR.airTemperatureC());state.put("airSampleTick",NEAR.airSampleTick());
        state.put("airBasisTerms",NEAR.airBasisTerms());state.put("airHeated",airHeated);state.put("materialHeated",heated);
        state.put("airCooled",airCooled);state.put("airAtStopC",stopAir);state.put("peakAirC",peakAir);
        state.put("coolingOnly",coolingOnly);state.put("peakMaterialC",peakMaterial);
        state.put("peakMaterialQ",Double.isFinite(peakMaterial)?InfraredBrickCodec.quantize(peakMaterial):Short.MIN_VALUE);
        state.put("materialCooled",phase>=2&&(material&&temperature<=peakMaterial-.25||!material));
        state.put("missingTicks",missingSince<0?0:tick-missingSince);
        state.put("naturalCleared",coolingOnly&&missingSince>=0&&tick-missingSince>=100);
        state.put("naturalC",WorldTemperature.naturalAir(level,WALL));
        if(coolingOnly&&!probed&&missingSince>=0&&tick-missingSince>=100) {
            var observer=server.getPlayerList().getPlayerByName("IRFast");
            var full=MinecraftThermalInput.gameplayInfraredSnapshot(observer,true,0,0,0,new long[12],false,new long[0],0,0);
            if(full!=null) {
                write("natural-final-full.json",Map.of("tick",tick,"warmQ",point(full),"summary",
                        com.teammoeg.frostedheart.content.climate.network.InfraredDiagnostics.summarize(full)));
                probed=true;
            }
        }
        write("physical-state.json",state);
        Files.writeString(root.resolve("physical-history.jsonl"),new GsonBuilder().serializeSpecialFloatingPointValues().create().toJson(state)+"\n",
                java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
        if(tick-started>(coolingOnly?16000:7200))throw new IllegalStateException("Physical source lifecycle timeout: "+state);
    }

    private static void write(String name,Object data)throws Exception {
        Path temporary=root.resolve(name+".tmp");
        Files.writeString(temporary,new GsonBuilder().serializeSpecialFloatingPointValues().setPrettyPrinting().create().toJson(data));
        Files.move(temporary,root.resolve(name),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static short point(MinecraftThermalInput.InfraredSnapshot snapshot) {
        int px=(WALL.getX()>>4)-(snapshot.centerChunkX()-4),py=(WALL.getY()>>4)-(snapshot.centerSectionY()-4),pz=(WALL.getZ()>>4)-(snapshot.centerChunkZ()-4);
        int address=((py*9+pz)*9+px)*64+((WALL.getY()&15)>>2)*16+((WALL.getZ()&15)>>2)*4+((WALL.getX()&15)>>2);
        int block=((WALL.getY()&3)<<4)|((WALL.getZ()&3)<<2)|(WALL.getX()&3);
        var decoder=new InfraredBrickCodec.Decoder();short[] values=new short[64];
        for(byte[] records:snapshot.brickRecords()) {
            var input=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(records));
            try {int next;while((next=decoder.readRecord(input,values))>=0)if(next==address)return values[block];}
            finally{input.release();}
        }
        return Short.MIN_VALUE;
    }
}
