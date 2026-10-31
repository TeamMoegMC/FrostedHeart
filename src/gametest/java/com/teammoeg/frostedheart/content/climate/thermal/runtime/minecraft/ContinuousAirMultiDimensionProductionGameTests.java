/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Two actual dimensions compete for the normal global budget and shared worker pool. */
@GameTestHolder("frostedheart_multi_dimension")
@PrefixGameTestTemplate(false)
public final class ContinuousAirMultiDimensionProductionGameTests {
    private static volatile boolean inspectingNumbers;
    private static volatile String numericalFailure;
    @GameTest(template="phase0a_empty",timeoutTicks=4300)
    public static void twoDenseMachineBasesKeepIndependentFieldsAndUnload(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper,true);
        inspectingNumbers=true;
        numericalFailure=null;
        ServerLevel[] levels={helper.getLevel(),helper.getLevel().getServer().getLevel(Level.NETHER)};
        helper.assertTrue(levels[1]!=null,"Use the actual loaded Nether dimension");
        helper.assertTrue(MinecraftThermalInput.thermalBudgetLimitBytes()==128L*1024*1024,"Use the normal shared128MiB budget");
        BlockPos origin=new BlockPos(2048,80,2048);
        BlockPos[][] fires=new BlockPos[2][],probes=new BlockPos[2][],machines=new BlockPos[2][128];
        FakePlayer[][] consumers=new FakePlayer[2][50];
        for(int d=0;d<2;d++) {
            ContinuousAirTwoBaseProductionGameTests.forceBase(levels[d],origin,true);
            var built=ContinuousAirLargeBaseProductionGameTests.buildBase(levels[d],origin);
            fires[d]=built.fires();probes[d]=built.probes();
            for(int i=0;i<128;i++) {
                machines[d][i]=(i&1)==0?fires[d][i].south():fires[d][i].west();
                helper.assertTrue(levels[d].getBlockState(machines[d][i]).isAir(),"Place the actual machine in free Air");
                levels[d].setBlockAndUpdate(machines[d][i],Blocks.FURNACE.defaultBlockState());
                if(i%5==0)levels[d].setBlockAndUpdate(machines[d][i].above(),Blocks.IRON_BLOCK.defaultBlockState());
            }
            for(int i=0;i<50;i++) {
                String name="dimensionHeat"+d+"_"+i;
                consumers[d][i]=FakePlayerFactory.get(levels[d],new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),name));
            }
        }
        long[] lastCut={-1,-1};int[][] mismatch=new int[2][8];
        int[] firstCurrent={-1,-1};
        long[] maximumAge={0,0},coarseReads={0,0};
        long[] unavailableReads={0,0},consumerUnavailable={0,0};
        int[] longestUnavailable={0,0},longestGeometryWait={0,0};
        int[][] unavailableRun=new int[2][8];
        boolean[] firstUnavailable={false,false};
        long[][] lastSamples=new long[2][8];
        int[][] windowAdvances=new int[2][8];
        for(var times:lastSamples)java.util.Arrays.fill(times,-1);
        int endTick=Integer.parseInt(System.getenv().getOrDefault("FH_CONTINUOUS_AIR_MULTI_TICKS","4000"));
        helper.assertTrue(endTick>=2400&&endTick<=4000,"Include both real recipe batches and reload in the bounded test");
        for(var row:mismatch)java.util.Arrays.fill(row,-1);
        boolean[] recovered={false};long[] queries={0};int[] elapsed={0};
        var sample=new ThermalEnvironmentSample();
        long[] resources=ContinuousAirProductionTiming.resources();
        ContinuousAirGameTestPacing.beginMeasurement();
        FHMain.LOGGER.info("THERMAL_MULTI_START,dimensions=2,sources=256,machines=256,consumers=100,rootLimitBytes={}",MinecraftThermalInput.thermalBudgetLimitBytes());
        helper.startSequence().thenExecuteFor(endTick,()->{
            if(numericalFailure!=null)helper.fail(numericalFailure);
            int tick=++elapsed[0];
            boolean absent=tick>=1400&&tick<1600;
            for(int d=0;d<2;d++) {
                if(d==1&&absent)continue;
                if(tick%400==0)for(var fire:fires[d])levels[d].getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                        .orElseThrow(IllegalStateException::new).insertItem(0,new ItemStack(Items.COAL),false);
                if(tick==120||tick==1900)for(var machine:machines[d]) {
                    var furnace=(AbstractFurnaceBlockEntity)levels[d].getBlockEntity(machine);
                    furnace.setItem(0,new ItemStack(Items.RAW_IRON,2));furnace.setItem(1,new ItemStack(Items.COAL));furnace.setChanged();
                }
                if(tick==760||tick==900) {
                    for(int y=1;y<=2;y++)levels[d].setBlockAndUpdate(origin.offset(24,y,6),tick==760?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                }
            }
            if(tick==600)for(int i=0;i<32;i++)ContinuousAirProductionGameTests.useItem(levels[0],fires[0][i],Items.IRON_SHOVEL);
            if(tick==1100)for(int i=0;i<32;i++)ContinuousAirProductionGameTests.useItem(levels[0],fires[0][i],Items.FLINT_AND_STEEL);
            if(tick==1400)ContinuousAirTwoBaseProductionGameTests.forceBase(levels[1],origin,false);
            if(tick==1580)helper.assertTrue(levels[1].getChunkSource().getChunkNow(probes[1][0].getX()>>4,probes[1][0].getZ()>>4)==null,"Observe actual Nether chunk absence");
            if(tick==1600) {
                ContinuousAirTwoBaseProductionGameTests.forceBase(levels[1],origin,true);
                java.util.Arrays.fill(mismatch[1],-1);
            }
            for(int d=0;d<2;d++) {
                if(d==1&&absent)continue;
                boolean recovering=d==1&&tick>=1600&&!recovered[0];
                boolean all=true;
                for(int p=0;p<8;p++) {
                    var position=probes[d][p];
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(levels[d],position,WorldTemperature.naturalAir(levels[d],position),sample);
                    long age=sample.airSampleTick()<0?-1:levels[d].getGameTime()-sample.airSampleTick();
                    boolean fresh=sample.airAvailable()&&sample.airSampleTick()>=0&&age>=0
                            &&(sample.airMinimumQuality()||age<=40);
                    if(sample.airSampleTick()>=0) {
                        maximumAge[d]=Math.max(maximumAge[d],age);
                        if(sample.airMinimumQuality())coarseReads[d]++;
                        helper.assertTrue(lastSamples[d][p]<=sample.airSampleTick(),"A dimension probe must not regress physical time");
                        if(lastSamples[d][p]<sample.airSampleTick())windowAdvances[d][p]++;
                        lastSamples[d][p]=sample.airSampleTick();
                    }
                    boolean current=MinecraftThermalInput.describeAirRead(levels[d],position).startsWith("geometry_current");
                    if(!fresh) {
                        unavailableReads[d]++;
                        longestUnavailable[d]=Math.max(longestUnavailable[d],++unavailableRun[d][p]);
                    } else unavailableRun[d][p]=0;
                    all&=fresh&&current;
                    if(tick>=400) {
                        if(recovering)helper.assertTrue(all||tick-1600<=40,"Reloaded Nether must recover within40 ticks");
                        else if(!fresh&&!firstUnavailable[d]) {
                            firstUnavailable[d]=true;
                            FHMain.LOGGER.info("THERMAL_MULTI_FIRST_UNAVAILABLE,"+"dimension="
                                +levels[d].dimension().location()+",elapsed="+tick+",point="+position
                                +",available="+sample.airAvailable()+",sampleTick="+sample.airSampleTick()
                                +",routed="+sample.sharedAirRegion()
                                +",minimum="+sample.airMinimumQuality()
                                +",read="+MinecraftThermalInput.describeAirRead(levels[d],position)
                                +",worker="+MinecraftThermalInput.describeWorkerProgress(levels[d])
                                +",payload="+describePayload(levels[d],position));
                        }
                        if(current)mismatch[d][p]=-1;
                        else {if(mismatch[d][p]<0)mismatch[d][p]=tick;longestGeometryWait[d]=Math.max(longestGeometryWait[d],tick-mismatch[d][p]);}
                    }
                }
                if(firstCurrent[d]<0&&all) {firstCurrent[d]=tick;FHMain.LOGGER.info("THERMAL_MULTI_FIRST_CURRENT,dimension={},elapsed={}",levels[d].dimension().location(),tick);}
                if(tick==41&&firstCurrent[d]<0)FHMain.LOGGER.info(
                        "THERMAL_MULTI_COLD_TARGET_MISSED,dimension={},targetTicks=40,observedAt={},worker={}",
                        levels[d].dimension().location(),tick,MinecraftThermalInput.describeWorkerProgress(levels[d]));
                if(recovering&&all) { recovered[0]=true;FHMain.LOGGER.info("THERMAL_MULTI_RECOVERED,elapsed={},delayTicks={}",tick,tick-1600); }
                if(tick%20==0)for(int i=0;i<50;i++) {
                    var fire=fires[d][i];consumers[d][i].setPos(fire.getX()+.5,fire.getY(),fire.getZ()+.5);
                    MinecraftThermalInput.gameplayPlayerEnvironment(consumers[d][i],WorldTemperature.naturalAir(levels[d],fire),sample);queries[0]++;
                    if(tick>=400&&!recovering&&(!sample.airAvailable()||sample.airSampleTick()<0
                            ||!sample.airMinimumQuality()&&levels[d].getGameTime()-sample.airSampleTick()>40))consumerUnavailable[d]++;
                }
                if(tick>=800&&tick%400==0) {
                    for(int p=0;p<8;p++)helper.assertTrue(windowAdvances[d][p]>=2,
                            "Every dimension probe must actually advance in each pressure window, not merely return an old coarse value");
                    FHMain.LOGGER.info("THERMAL_MULTI_WINDOW,dimension={},elapsed={},advances={},maximumAgeTicks={},coarseReads={}",
                            levels[d].dimension().location(),tick,java.util.Arrays.toString(windowAdvances[d]),maximumAge[d],coarseReads[d]);
                    java.util.Arrays.fill(windowAdvances[d],0);
                }
                if(tick==700)helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(levels[d])==(d==0?96:128),"An Overworld source action must not change Nether source state");
            }
            if(tick==endTick) {
                helper.assertTrue(recovered[0],"Complete the real Nether unload/reload");
                for(int d=0;d<2;d++) {
                    helper.assertTrue(firstCurrent[d]>=0,"Each real dimension must complete cold initialization");
                    helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(levels[d])==128,"Each actual dimension retains128 sources");
                    helper.assertTrue(MinecraftThermalInput.workLimitedCompletions(levels[d])==0,"No dimension accumulated WORK_LIMITED");
                    for(var machine:machines[d])helper.assertTrue(((AbstractFurnaceBlockEntity)levels[d].getBlockEntity(machine)).getItem(2).getCount()>=4,"Each actual machine cooked both recipe batches");
                    helper.assertTrue(MinecraftThermalInput.usesNativeCheckpointScheduling(levels[d]),"Each dimension uses the saved native checkpoint config");
                }
                long[] after=ContinuousAirProductionTiming.resources();
                FHMain.LOGGER.info("THERMAL_MULTI_RESULT,elapsed={},queries={},rootPeakBytes={},workerCpuNs={},mainCpuNs={},gcMillis={},overworldColdTicks={},netherColdTicks={},overworldMaximumAgeTicks={},netherMaximumAgeTicks={},coarseReads={},unavailableReads={},consumerUnavailable={},longestUnavailableTicks={},longestGeometryWaitTicks={}",tick,queries[0],MinecraftThermalInput.thermalBudgetPeakBytes(),after[0]-resources[0],after[2]-resources[2],after[5]-resources[5],firstCurrent[0],firstCurrent[1],maximumAge[0],maximumAge[1],java.util.Arrays.toString(coarseReads),java.util.Arrays.toString(unavailableReads),java.util.Arrays.toString(consumerUnavailable),java.util.Arrays.toString(longestUnavailable),java.util.Arrays.toString(longestGeometryWait));
            }
        }).thenWaitUntil(()->{
            if(numericalFailure!=null)helper.fail(numericalFailure);
            for(int d=0;d<2;d++)for(var position:probes[d]) {
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(levels[d],position,WorldTemperature.naturalAir(levels[d],position),sample);
                helper.assertTrue(sample.airAvailable()&&sample.airSampleTick()>=0
                                &&MinecraftThermalInput.describeAirRead(levels[d],position).startsWith("geometry_current"),
                        "Both real dimensions must drain structural pressure into readable current geometry before shutdown");
            }
        }).thenExecute(()->{
            for(var level:levels) { ContinuousAirTwoBaseProductionGameTests.forceBase(level,origin,false);MinecraftThermalInput.closeActiveLevel(level); }
        }).thenWaitUntil(()->helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes()==0,"All dimension Root ownership must release")).thenSucceed();
    }

    /** Failure-only inspection of the actual immutable frame, without changing thermal state. */
    private static String describePayload(ServerLevel level,BlockPos position) {
        var publication=MinecraftThermalInput.queryPublicationForObservation(level);
        if(publication==null)return "publication_missing";
        synchronized(publication) {
            try {
                var lookup=publication.getClass().getDeclaredMethod("frameAt",double.class,double.class,double.class);
                lookup.setAccessible(true);
                var frame=(com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot)
                        lookup.invoke(publication,position.getX()+.5,position.getY()+.3125,position.getZ()+.5);
                if(frame!=null) {
                    var component=frame.airLayout().componentAt(position.getX(),position.getY(),position.getZ());
                    if(component==null)return "component_missing";
                    double x=position.getX()+.5,y=position.getY()+.3125,z=position.getZ()+.5;
                    var minimum=frame.minimumState();
                    double background=minimum==null?Double.NaN:minimum.layout().background().referenceC
                            +component.temperatureOffsetC(x,y,z,minimum.layout().background().borrowedAirOffsetsC());
                    double residual=minimum==null?Double.NaN:minimum.temperatureOffsetC(x,y,z);
                    int nonfiniteNodes=0;
                    if(minimum!=null)for(int node=0;node<com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState.MAXIMUM_NODES;node++)
                        if(!Double.isFinite(minimum.nodeTemperatureC(node)))nonfiniteNodes++;
                    if(nonfiniteNodes!=0||!Double.isFinite(frame.airTemperatureC(x,y,z)))
                        numericalFailure="Published thermal state contains nonfinite values at "+position+",tick="+frame.sampleTick();
                    return "frameTick="+frame.sampleTick()+",actualPointC="+frame.airTemperatureC(x,y,z)
                            +",centerC="+frame.airTemperatureC(x,position.getY()+.5,z)+",backgroundC="+background
                            +",residualC="+residual+",nonfiniteNodes="+nonfiniteNodes+",unitGrid="+component.isUnitGrid()
                            +",blocks="+Long.toHexString(component.airBlocks)+",basis="+component.basisCount();
                }
                return "component_missing";
            } catch(ReflectiveOperationException failure) {return failure.toString();}
        }
    }

    public static void qualityNumbers(com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot before,
            com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot after) {
        if(!inspectingNumbers||before.airLayout().componentAt(2053,82,2050)==null)return;
        int badAir=0,badH=0;
        for(double value:before.borrowedAirCoefficients())if(!Double.isFinite(value))badAir++;
        for(int body=0;body<before.materialCount();body++)if(!Double.isFinite(before.materialEnthalpyJ(body)))badH++;
        if(badAir!=0||badH!=0||before.minimumSummary()!=null&&!Double.isFinite(before.minimumSummary().totalEnergyJ())
                ||after.minimumState()!=null&&!Double.isFinite(after.minimumState().totalEnergyJ()))
            numericalFailure="Quality publication contains nonfinite values at tick "+before.sampleTick();
        FHMain.LOGGER.info("THERMAL_MULTI_QUALITY_NUMBERS,tick={},referenceC={},badFineAir={},badFineH={},summary={},activated={}",
                before.sampleTick(),before.referenceTemperatureC(),badAir,badH,minimumNumbers(before.minimumSummary()),minimumNumbers(after.minimumState()));
    }
    private static String minimumNumbers(com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState state) {
        if(state==null)return "none";
        int bad=0;String first="none";
        for(int node=0;node<com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState.MAXIMUM_NODES;node++) {
            if(Double.isFinite(state.nodeTemperatureC(node)))continue;
            bad++;
            if(first.equals("none"))first="node="+node+",E="+state.energyJ(node)+",C="+state.layout().capacity(node)
                    +",ref="+state.layout().reference(node)+",bg="+state.layout().background().nodeC(node);
        }
        return "bad="+bad+",totalJ="+state.totalEnergyJ()+",first=["+first+"]";
    }
}
