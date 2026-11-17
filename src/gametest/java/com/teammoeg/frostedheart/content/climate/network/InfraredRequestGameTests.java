/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import java.util.Arrays;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;

import io.netty.buffer.Unpooled;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_infrared_network")
@PrefixGameTestTemplate(false)
public final class InfraredRequestGameTests {
    private InfraredRequestGameTests() { }

    @GameTest(template = "phase0a_empty", timeoutTicks = 130)
    public static void slowFirstReplyRemainsAcceptedAcrossSeveralPollingIntervals(GameTestHelper helper) {
        var requests = new InfraredRequestState();
        int request = requests.tryBegin();
        for (int tick : new int[]{40, 60, 80}) {
            helper.runAfterDelay(tick, () -> helper.assertTrue(
                    requests.tryBegin() == InfraredRequestState.NO_REQUEST && requests.accepts(request),
                    "Polling must not replace an outstanding request before its delayed reply arrives"));
        }
        helper.startSequence().thenIdle(90).thenExecute(() -> {
            var snapshot = new InfraredSnapshot(4, 6, 5, 7, 8, true, true,
                    new long[12], new long[0], InfraredBrickCodec.NO_PARTS, 9, 10);
            var reply = wire(new FHResponseInfraredViewDataSyncPacket(request, snapshot, 0));
            helper.assertTrue(!reply.isAcknowledgement() && requests.accepts(reply.requestId()),
                    "A full reply delayed beyond the old 41..59-tick retry window remains current");
            helper.assertTrue(requests.complete(reply.requestId()) && !requests.isPending(),
                    "The delayed reply completes its original transaction");
            helper.assertTrue(requests.tryBegin() != request, "The next poll receives a new request identity");
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void acknowledgementKeepsTheBaselineAndAllowsAnotherPoll(GameTestHelper helper) {
        var requests = new InfraredRequestState();
        int id = requests.tryBegin();
        long[] fields = new long[12];
        fields[3] = 0x100L;
        long center = SectionPos.asLong(4, 5, 6);
        var request = new FHRequestInfraredViewDataSyncPacket(id, false, 7, center, 8,
                new long[12], true, fields, 9, 10);
        var acknowledgement = wire(FHResponseInfraredViewDataSyncPacket.acknowledge(request));
        helper.assertTrue(acknowledgement.isAcknowledgement() && acknowledgement.firstPart() && acknowledgement.lastPart(),
                "A no-change or temporarily unavailable query has an explicit completed reply");
        helper.assertTrue(acknowledgement.snapshot() == null,
                "A compact acknowledgement carries no display state that could overwrite the committed baseline");
        helper.assertTrue(requests.complete(acknowledgement.requestId())
                        && requests.tryBegin() != InfraredRequestState.NO_REQUEST,
                "An unchanged server response must release the next polling request");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void cancelledReplyCannotCompleteANewerRequest(GameTestHelper helper) {
        var requests = new InfraredRequestState();
        int old = requests.tryBegin();
        requests.invalidate();
        int current = requests.tryBegin();
        helper.assertTrue(current != old && !requests.complete(old) && requests.accepts(current),
                "Closing, world reset or recipe invalidation keeps delayed replies out of the new transaction");
        helper.assertTrue(requests.tryBegin() == InfraredRequestState.NO_REQUEST,
                "An obsolete acknowledgement cannot unblock the current request");
        helper.assertTrue(requests.complete(current) && !requests.complete(current),
                "Only the current reply completes once");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void multipartReplyKeepsItsIdentityUntilTheLastPart(GameTestHelper helper) {
        var requests = new InfraredRequestState();
        int request = requests.tryBegin();
        var snapshot = new InfraredSnapshot(0, 0, 0, 1, 2, true, true,
                new long[12], new long[0], new byte[][]{new byte[0], new byte[0], new byte[0]}, 3, 4);
        for (int part = 0; part < 3; part++) {
            var reply = wire(new FHResponseInfraredViewDataSyncPacket(request, snapshot, part));
            helper.assertTrue(!reply.isAcknowledgement() && requests.accepts(reply.requestId())
                            && reply.firstPart() == (part == 0) && reply.lastPart() == (part == 2),
                    "Every data part retains the same request and its actual FIRST/LAST flags");
            if (reply.lastPart()) requests.complete(reply.requestId());
            else helper.assertTrue(requests.tryBegin() == InfraredRequestState.NO_REQUEST,
                    "A partial response cannot be superseded by a periodic poll or movement");
        }
        helper.assertTrue(!requests.isPending(), "LAST releases the transaction after all parts arrive");
        helper.succeed();
    }

    private static FHResponseInfraredViewDataSyncPacket wire(FHResponseInfraredViewDataSyncPacket source) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.encode(buffer);
            return new FHResponseInfraredViewDataSyncPacket(buffer);
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void emptySparseAndDenseMasksPreserveAllPages(GameTestHelper helper) {
        for (int stride : new int[]{0, 127, 3, 1}) {
            long[] mask = new long[12];
            if(stride>0) for(int page=0;page<768;page+=stride) mask[page>>>6]|=1L<<(page&63);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                FHRequestInfraredViewDataSyncPacket.writeFieldPages(buffer,mask);
                helper.assertTrue(buffer.readableBytes()<=97,"Mask chooses compact or dense encoding without expansion");
                helper.assertTrue(Arrays.equals(mask,FHRequestInfraredViewDataSyncPacket.readPresence(buffer)),
                        "Every Page bit survives the selected mask representation");
            } finally { buffer.release(); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void fullWindowPartitionsCommitMetadataOnlyAtLast(GameTestHelper helper) {
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalInfraredGameTests
                .fullWindowPartitionsPreserveEveryBrickAndFieldFootprint(helper);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void encodedPageComparisonKeepsOnlyChangedAndRemovedBricks(GameTestHelper helper) {
        short[] values = new short[64];
        for(int kinds : new int[]{1,2,35,64}) try(var builder=new InfraredBrickCodec.Builder()) {
            for(int i=0;i<64;i++) values[i]=(short)(i%kinds);
            builder.beginPage(); int start=builder.pageStart();
            builder.writeBrick(3*64+5,values,true);
            builder.writeUniform(3*64+7,(short)80);
            byte[] baseline=builder.finishPage(start,null);
            builder.reset(); builder.beginPage(); start=builder.pageStart();
            builder.writeBrick(9*64+5,values,false);
            builder.writeUniform(9*64+7,(short)80);
            builder.writeInvalid(9*64+8,false);
            builder.finishPage(start,baseline);
            helper.assertTrue(builder.size()==0,"Identical encoded values ignore window-local Page addresses");
            builder.reset(); builder.beginPage(); start=builder.pageStart();
            builder.writeBrick(9*64+5,values,false);
            builder.writeInvalid(9*64+7,false);
            builder.finishPage(start,baseline);
            helper.assertTrue(builder.size()==3,"Deleting a previously valid Brick retains its explicit INVALID record");
        }
        helper.succeed();
    }
}
