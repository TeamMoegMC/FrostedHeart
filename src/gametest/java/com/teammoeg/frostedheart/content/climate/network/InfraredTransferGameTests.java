/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_infrared_stream")
@PrefixGameTestTemplate(false)
public final class InfraredTransferGameTests {
    @GameTest(template="phase0a_empty",timeoutTicks=100)
    public static void firstReceiptWindowIsSmallAndAllRawRecordsSurvive(GameTestHelper helper) {
        byte[][] parts;short[] values=new short[64];
        try(var builder=new InfraredBrickCodec.Builder()) {
            for(int page=0;page<32;page++) {
                builder.beginPage();
                for(int brick=0;brick<64;brick++) {
                    for(int block=0;block<64;block++)values[block]=(short)(page*73+brick*31+block*461);
                    builder.writeBrick(page*64+brick,values,false);
                }
            }
            parts=builder.finishParts();
        }
        helper.assertTrue(parts.length>2,"RAW fixture exercises both initial and sustained transfer budgets");
        var snapshot=new InfraredSnapshot(0,0,0,1,1,true,true,new long[12],new long[12],parts,0,0);
        int records=0;
        for(int part=0;part<parts.length;part++) {
            var wire=new FriendlyByteBuf(Unpooled.buffer());
            try {
                new FHResponseInfraredViewDataSyncPacket(1,snapshot,part).encode(wire);
                helper.assertTrue(wire.readableBytes()<=(part==0?32*1024:128*1024),"Actual encoded headers and payload fit the smaller first window");
                var received=new FHResponseInfraredViewDataSyncPacket(wire);
                var input=new FriendlyByteBuf(Unpooled.wrappedBuffer(received.snapshot().brickRecords()[0]));
                try {
                    var decoder=new InfraredBrickCodec.Decoder();int address;
                    while((address=decoder.readRecord(input,values))>=0) {
                        helper.assertTrue(address==records++,"Budget changes preserve every record's ordering");
                        for(int block=0;block<64;block++)helper.assertTrue(values[block]==(short)((address/64)*73+(address%64)*31+block*461),"RAW temperatures survive repartitioning");
                    }
                }finally{input.release();}
            }finally{wire.release();}
        }
        helper.assertTrue(records==32*64,"The complete window survives the smaller first response");
        helper.succeed();
    }

    @GameTest(template="phase0a_empty",timeoutTicks=100)
    public static void creditsCannotSkipOrRepeatPartsAndLastKeepsMetadata(GameTestHelper helper) {
        var snapshot=new InfraredSnapshot(0,0,0,5,6,true,true,new long[12],new long[12],
                new byte[][]{new byte[0],new byte[0],new byte[0]},7,8);
        var transfer=new InfraredTransfers.Transfer(11,Level.OVERWORLD,snapshot);
        helper.assertTrue(transfer.take(12,0)==null&&transfer.take(11,1)==null,"Only this transaction's next part consumes credit");
        for(int part=0;part<3;part++) {
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try {
                transfer.take(11,part).encode(buffer);
                var received=new FHResponseInfraredViewDataSyncPacket(buffer);
                helper.assertTrue(received.firstPart()==(part==0)&&received.lastPart()==(part==2),"Bounded stream retains FIRST/LAST");
                helper.assertTrue((received.snapshot().presence().length!=0)==(part==2),"Only LAST commits display metadata");
                helper.assertTrue(transfer.take(11,part)==null,"Repeating a receipt does not enqueue the same part again");
                helper.assertTrue(transfer.complete()==(part==2),"The reply remains owned until its final credit");
            }finally{buffer.release();}
        }
        helper.assertTrue(transfer.take(11,3)==null,"Completed transfer cannot enqueue additional data");
        helper.succeed();
    }

    @GameTest(template="phase0a_empty",timeoutTicks=100)
    public static void receiptAndCancellationUseCompactWire(GameTestHelper helper) {
        for(int part:new int[]{-1,1,48}) {
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try {
                var sent=new FHContinueInfraredViewPacket(7,part);sent.encode(buffer);
                helper.assertTrue(buffer.readableBytes()==2,"Ordinary receipt and cancellation require only two body bytes");
                helper.assertTrue(sent.equals(new FHContinueInfraredViewPacket(buffer)),"Receipt and cancellation survive real wire decode");
            }finally{buffer.release();}
        }
        helper.succeed();
    }
}
