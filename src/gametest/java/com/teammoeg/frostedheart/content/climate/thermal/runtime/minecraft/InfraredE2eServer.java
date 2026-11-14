/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.network.InfraredDiagnostics;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in dedicated-server fixture. No synthetic infrared packets are injected. */
@Mod.EventBusSubscriber(modid=FHMain.MODID)
public final class InfraredE2eServer {
    private static final boolean ENABLED=Boolean.getBoolean("frostedheart.irE2eServer");
    private static final Path ROOT=Path.of(System.getProperty("frostedheart.irE2eRoot","build/infrared-e2e-20261108"));
    private static final ResourceLocation PROVIDER=new ResourceLocation("frostedheart","ir_e2e");
    private static final Set<String> READY=new HashSet<>(),UPDATED=new HashSet<>(),DONE=new HashSet<>();
    private static MinecraftServer server;
    private static int round=-1,phase;
    private static boolean onlySlow;
    private static boolean physical;

    @SubscribeEvent public static void started(ServerStartedEvent event) throws Exception {
        if(!ENABLED)return;
        server=event.getServer();var level=server.overworld();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
        level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
        for(int x=-10;x<=10;x++)for(int z=-3;z<=13;z++)level.setBlockAndUpdate(new BlockPos(x,64,z),Blocks.STONE.defaultBlockState());
        for(int x=-8;x<=8;x++)for(int y=65;y<=70;y++)level.setBlockAndUpdate(new BlockPos(x,y,0),Blocks.STONE.defaultBlockState());
        BlockPos fire=new BlockPos(0,65,3);
        level.setBlockAndUpdate(fire,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,true));
        if(level.getBlockEntity(fire) instanceof ICampfireExtra extra)extra.setLifeTime(24000);
        write("server-ready.json",Map.of("ready",true));
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!ENABLED||!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player))return;
        player.setGameMode(GameType.SPECTATOR);player.setInvulnerable(true);
        player.teleportTo(server.overworld(),.5,66,10.5,180,0);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,24000,0,false,false));
        InfraredDiagnostics.setTracing(player,true);
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if(!ENABLED)return;
        var root=Commands.literal("ir_e2e");
        for(String stage:new String[]{"ready","updated","done","physical_baseline","physical_heated","physical_cooled","physical_removed","physical_restored","physical_natural-cleared"})root.then(Commands.literal(stage).executes(c->{
            String name=c.getSource().getPlayerOrException().getScoreboardName();
            if(stage.startsWith("physical_")){InfraredPhysicalE2e.receipt(stage,name);return 1;}
            (stage.equals("ready")?READY:stage.equals("updated")?UPDATED:DONE).add(name);return 1;
        }));
        event.getDispatcher().register(root);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(!ENABLED||server==null||event.phase!=TickEvent.Phase.END)return;
        try {
            Path control=ROOT.resolve("round.json");
            if(Files.exists(control)) {
                var request=JsonParser.parseString(Files.readString(control)).getAsJsonObject();
                int next=request.get("round").getAsInt();
                if(next!=round) {
                    round=next;phase=0;READY.clear();UPDATED.clear();DONE.clear();
                    onlySlow=request.has("onlySlow")&&request.get("onlySlow").getAsBoolean();
                    physical=request.has("physical")&&request.get("physical").getAsBoolean();
                    if(physical) {
                        MinecraftGameplayFields.beginProviderRefresh(server.overworld(),PROVIDER);
                        MinecraftGameplayFields.endProviderRefresh(server.overworld(),PROVIDER);
                        InfraredPhysicalE2e.start(server,ROOT,round,request.has("coolingOnly")&&request.get("coolingOnly").getAsBoolean());
                    } else {
                        fields(40,request.get("bulk").getAsBoolean());
                        stressFields(request.has("stressFields")&&request.get("stressFields").getAsBoolean());
                    }
                    write("server-round-"+round+".json",Map.of("round",round,"phase",phase));
                }
            }
            if(round<0)return;
            if(physical){InfraredPhysicalE2e.tick(server);return;}
            Set<String> observers=onlySlow?Set.of("IRSlow"):Set.of("IRFast","IRSlow");
            if(phase==0&&READY.containsAll(observers)) {
                phase=1;warm(60);write("server-update-"+round+".json",Map.of("phase",phase));
            } else if(phase==1&&UPDATED.containsAll(observers)) {
                phase=2;
                for(int x=-6;x<=-2;x++)MinecraftGameplayFields.remove(server.overworld(),key(x));
                write("server-remove-"+round+".json",Map.of("phase",phase));
            } else if(phase==2&&DONE.containsAll(observers)) {
                phase=3;write("round-complete-"+round+".json",Map.of("round",round,"passed",true));
            }
        }catch(Exception failure){failure.printStackTrace();try{write("server-failure.json",Map.of("failure",failure.toString()));}catch(Exception ignored){}}
    }

    private static ThermalFieldKey key(int x){return new ThermalFieldKey(PROVIDER,0,x,1);}
    private static void warm(double value) {
        for(int x=-6;x<=-2;x++)column(x,value);
    }
    private static void column(int x,double value) {
        MinecraftGameplayFields.upsert(server.overworld(),new ThermalAnalyticField(key(x),1,
                ThermalAnalyticField.CombineMode.OVERRIDE,ThermalAnalyticField.Shape.PILLAR,x+.5,67.5,.5,.49,3,3,value));
    }
    private static void fields(double value,boolean bulk) {
        warm(value);for(int x=2;x<=6;x++)column(x,-40);
        var key=new ThermalFieldKey(PROVIDER,0,1000,2);
        if(bulk)MinecraftGameplayFields.upsert(server.overworld(),new ThermalAnalyticField(key,0,
                ThermalAnalyticField.CombineMode.ADD_DELTA,ThermalAnalyticField.Shape.CUBE,.5,67.5,5.5,96,96,96,0));
        else MinecraftGameplayFields.remove(server.overworld(),key);
    }
    private static void stressFields(boolean enabled) {
        var random=new Random(812762);
        for(int i=0;i<192;i++) {
            var key=new ThermalFieldKey(PROVIDER,0,2000+i,3);
            if(!enabled){MinecraftGameplayFields.remove(server.overworld(),key);continue;}
            ThermalAnalyticField field;
            do {
                field=new ThermalAnalyticField(key,0,ThermalAnalyticField.CombineMode.ADD_DELTA,
                        -64+random.nextDouble()*144,random.nextDouble()*144,-64+random.nextDouble()*144,
                        24+random.nextDouble()*24,-20+random.nextDouble()*40);
            }while(field.intersects(-9,64,-1,9,72,5));
            MinecraftGameplayFields.upsert(server.overworld(),field);
        }
    }
    private static void write(String name,Object value)throws Exception {
        Files.createDirectories(ROOT);Files.writeString(ROOT.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(value));
    }
}
