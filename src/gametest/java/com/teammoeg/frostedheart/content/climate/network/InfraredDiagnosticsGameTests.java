/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_infrared_diagnostics")
@PrefixGameTestTemplate(false)
public final class InfraredDiagnosticsGameTests {
    @GameTest(template="phase0a_empty",timeoutTicks=100)
    public static void validTemperatureSummaryReadsTheActualEncodedValues(GameTestHelper helper) {
        byte[][] parts;
        try(var builder=new InfraredBrickCodec.Builder()) {
            builder.writeUniform(0,(short)80);
            builder.writeUniform(1,(short)-120);
            parts=builder.finishParts();
        }
        long[] mask=new long[12];mask[0]=1;
        var snapshot=new InfraredSnapshot(0,0,0,1,2,true,true,mask,mask,parts,3,4);
        var summary=InfraredDiagnostics.summarize(snapshot);
        helper.assertTrue(summary.validSamples()==128&&summary.records()==2&&summary.payloadBytes()==10
                        &&summary.minC()==-30&&summary.maxC()==20&&summary.materialPages()==1&&summary.fieldPages()==1,
                "Probe counts encoded scalar samples, pages and temperature range without inventing values");
        helper.assertTrue(InfraredDiagnostics.summarize(snapshot).equals(summary),"Inspecting a reply does not consume its data");
        helper.succeed();
    }

    @GameTest(template="phase0a_empty",timeoutTicks=100)
    public static void noSnapshotAndAnEmptyFullDisplayRemainDistinct(GameTestHelper helper) {
        var unavailable=InfraredDiagnostics.summarize(null);
        var empty=InfraredDiagnostics.summarize(new InfraredSnapshot(0,0,0,0,0,false,true,
                new long[12],new long[0],InfraredBrickCodec.NO_PARTS,0,0));
        helper.assertTrue(unavailable.state().equals("NO_SNAPSHOT")&&unavailable.parts()==0,
                "A missing coherent cut is visible as an absent snapshot");
        helper.assertTrue(empty.state().equals("NO_VALID_SAMPLES")&&empty.parts()==1&&Double.isNaN(empty.maxC()),
                "A valid empty full reply remains distinguishable from no reply");
        helper.succeed();
    }

    @GameTest(template="phase0a_empty",timeoutTicks=100)
    public static void deletedSamplesAreNotReportedAsARealColdTemperature(GameTestHelper helper) {
        byte[][] parts;
        try(var builder=new InfraredBrickCodec.Builder()){builder.writeInvalid(0,false);parts=builder.finishParts();}
        var summary=InfraredDiagnostics.summarize(new InfraredSnapshot(0,0,0,0,0,false,false,
                new long[0],new long[0],parts,0,0));
        helper.assertTrue(summary.records()==1&&summary.validSamples()==0&&Double.isNaN(summary.minC()),
                "INVALID is missing data, not an extreme measured temperature");
        helper.succeed();
    }

    @GameTest(template="phase0a_empty",timeoutTicks=200)
    public static void serverProbeIsAvailableToAnOrdinaryPlayer(GameTestHelper helper) throws Exception {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());
        var p=helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        player.setPos(p.getX(),p.getY(),p.getZ());
        var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();
        var source=player.createCommandSourceStack().withPermission(0);
        helper.assertTrue(dispatcher.execute("fh_ir_server status",source)==1,"Own-position probe does not require captain or OP status");
        helper.assertTrue(dispatcher.execute("fh_ir_server trace true",source)==1,"An ordinary player can enable their own request trace");
        dispatcher.execute("fh_ir_server trace false",source);
        helper.succeed();
    }
}
