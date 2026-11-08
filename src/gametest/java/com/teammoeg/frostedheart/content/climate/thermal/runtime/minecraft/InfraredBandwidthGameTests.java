/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.Arrays;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.climate.network.FHRequestInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.FHResponseInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.InfraredWindow;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Varint21LengthFieldPrepender;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.NetworkDirection;

/** Counts both directions, including actual Forge envelopes and Minecraft compression/framing. */
@GameTestHolder("frostedheart_infrared_bandwidth")
@PrefixGameTestTemplate(false)
public final class InfraredBandwidthGameTests {
    private static final ResourceLocation PROVIDER = new ResourceLocation("frostedheart", "bandwidth_test");

    public static void constantFieldDoesNotResendItsCommittedColors(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var view = new View(scene.player);
            poll(view, true, new Traffic());
            for (int i = 0; i < 8; i++) poll(view, false, new Traffic());
            var traffic = new Traffic();
            InfraredSnapshot last = null;
            for (int i = 0; i < 24; i++) last = poll(view, false, traffic);
            traffic.report("constant-field");
            helper.assertTrue(last == null && traffic.dataParts == 0 && traffic.acknowledgements == 24,
                    "A committed constant field must send completion acknowledgements instead of repeated temperature records");
            helper.succeed();
        }
    }

    public static void movingOneSectionReusesTheOverlappingField(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var view = new View(scene.player);
            poll(view, true, new Traffic());
            scene.player.setPos(scene.p.getX() + 16.5, scene.p.getY(), scene.p.getZ() + .5);
            var traffic = new Traffic();
            var moved = poll(view, false, traffic);
            traffic.report("move-one-section");
            helper.assertTrue(moved != null && !moved.full(), "Overlapping windows must move with a delta transaction");
            helper.assertTrue(Arrays.stream(moved.brickRecords()).mapToInt(part -> part.length).sum() == 0,
                    "The unchanged field is entirely inside the overlap and needs no repeated temperature records");
            helper.succeed();
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 500)
    public static void fieldChangesAndDeletionReachIndependentObservers(GameTestHelper helper) {
        try(var scene=new Scene(helper)) {
            var first=new View(scene.player);
            var secondPlayer=FakePlayerFactory.get(scene.level,new GameProfile(new UUID(0x9876,scene.p.asLong()),"ir_second"));
            secondPlayer.setPos(scene.player.getX(),scene.player.getY(),scene.player.getZ());
            var second=new View(secondPlayer);
            poll(first,true,new Traffic()); poll(second,true,new Traffic());
            scene.temperature(20.01);
            helper.assertTrue(poll(first,false,new Traffic())==null&&poll(second,false,new Traffic())==null,
                    "Sub-quantum field changes cannot resend unchanged final colors");
            scene.temperature(20.13);
            var a=poll(first,false,new Traffic()); var b=poll(second,false,new Traffic());
            helper.assertTrue(Short.valueOf((short)81).equals(temperature(a,scene.p))
                            &&Short.valueOf((short)81).equals(temperature(b,scene.p)),
                    "Both independent committed baselines receive the new quantized value");
            MinecraftGameplayFields.remove(scene.level,scene.key);
            a=poll(first,false,new Traffic()); b=poll(second,false,new Traffic());
            helper.assertTrue(Short.valueOf(InfraredBrickCodec.INVALID_TEMPERATURE).equals(temperature(a,scene.p))
                            &&Short.valueOf(InfraredBrickCodec.INVALID_TEMPERATURE).equals(temperature(b,scene.p)),
                    "Both observers receive explicit deletion after the field disappears");
            helper.succeed();
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 500)
    public static void missingObserverBaselineResendsThenBecomesQuiet(GameTestHelper helper) {
        try(var scene=new Scene(helper)) {
            var view=new View(scene.player);
            poll(view,true,new Traffic());
            view.committed=-1;
            var resent=poll(view,false,new Traffic());
            helper.assertTrue(Short.valueOf((short)80).equals(temperature(resent,scene.p)),
                    "An unknown or evicted baseline falls back to actual temperature records");
            helper.assertTrue(poll(view,false,new Traffic())==null,"The replacement baseline supports the next unchanged ACK");
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(scene.player));
            helper.assertTrue(Short.valueOf((short)80).equals(temperature(poll(view,false,new Traffic()),scene.p)),
                    "Logout removes the acceleration entry while a missing cache still permits correct resynchronization");
            helper.succeed();
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 500)
    public static void movingWindowReceivesEnteringHeatAndFarJumpUsesFull(GameTestHelper helper) {
        try(var scene=new Scene(helper)) {
            BlockPos entering=scene.p.east(80);
            scene.level.getChunkAt(entering);
            var key=new ThermalFieldKey(PROVIDER,0,entering.asLong(),2);
            try {
                MinecraftGameplayFields.upsert(scene.level,new ThermalAnalyticField(key,0,
                        ThermalAnalyticField.CombineMode.OVERRIDE,entering.getX()+.5,entering.getY()+.5,
                        entering.getZ()+.5,2,80));
                var view=new View(scene.player);
                var initial=poll(view,true,new Traffic());
                helper.assertTrue(temperature(initial,entering)==null,"The second heat source begins outside the old window");
                scene.player.setPos(scene.p.getX()+16.5,scene.p.getY(),scene.p.getZ()+.5);
                var moved=poll(view,false,new Traffic());
                helper.assertTrue(!moved.full()&&Short.valueOf((short)320).equals(temperature(moved,entering)),
                        "The entering strip receives its new field while overlap colors are retained");
                helper.assertTrue(temperature(moved,scene.p)==null,"The unchanged overlapping heat need not be retransmitted");
                scene.player.setPos(scene.p.getX()+16*12+.5,scene.p.getY(),scene.p.getZ()+.5);
                var far=poll(view,false,new Traffic());
                helper.assertTrue(far.full()&&far.fieldPages().length==0,"A non-overlapping jump clears the previous window through full");
                helper.succeed();
            } finally {MinecraftGameplayFields.remove(scene.level,key);}
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 800)
    public static void materialChangesRemainVisibleUnderAnUnchangedFloorField(GameTestHelper helper) {
        try(var scene=new Scene(helper)) {
            scene.mode=ThermalAnalyticField.CombineMode.FLOOR_FROM_NATURAL;
            scene.temperature(5);
            scene.level.setBlockAndUpdate(scene.p,Blocks.STONE.defaultBlockState());
            var chunk=scene.level.getChunkAt(scene.p);
            var previous=MinecraftThermalInput.dormantState(chunk);
            var stored=new DormantChunkThermalState(chunk.getMinSection(),chunk.getSections().length);
            try {
                stored.replaceMaterials(scene.p.getY()>>4,ThermalMaterialBehaviorGameTests.storedBrick(helper,scene.p,60));
                MinecraftThermalInput.setDormantState(chunk,stored);
                var view=new View(scene.player);
                poll(view,true,new Traffic());
                stored.replaceMaterials(scene.p.getY()>>4,ThermalMaterialBehaviorGameTests.storedBrick(helper,scene.p,80));
                var traffic=new Traffic();
                var changed=poll(view,false,traffic);
                traffic.report("material-change-under-field");
                helper.assertTrue(Short.valueOf((short)320).equals(temperature(changed,scene.p)),
                        "Actual material changes cannot be hidden by an unchanged analytic-field baseline");
                MinecraftThermalInput.setDormantState(chunk,null);
                var removed=poll(view,false,new Traffic());
                short floor=InfraredBrickCodec.quantize(WorldTemperature.naturalAir(scene.level,scene.p)+5);
                helper.assertTrue(Short.valueOf(floor).equals(temperature(removed,scene.p)),
                        "Removing the material restores its remaining field, rather than clearing the tower heat");
                helper.succeed();
            } finally {MinecraftThermalInput.setDormantState(chunk,previous);}
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1000)
    public static void oneHundredObserversShareEncodedPages(GameTestHelper helper) {
        try(var scene=new Scene(helper)) {
            var before=MinecraftThermalInput.infraredDisplayStatistics();
            View[] views=new View[100];
            for(int i=0;i<views.length;i++) {
                var player=FakePlayerFactory.get(scene.level,new GameProfile(new UUID(0x10000+i,scene.p.asLong()),"ir_view_"+i));
                player.setPos(scene.player.getX(),scene.player.getY(),scene.player.getZ());
                views[i]=new View(player); poll(views[i],true,new Traffic());
            }
            var traffic=new Traffic();
            for(var view:views) helper.assertTrue(poll(view,false,traffic)==null,"Every observer keeps its own committed request identity");
            traffic.report("100-observers-constant");
            var after=MinecraftThermalInput.infraredDisplayStatistics();
            FHMain.LOGGER.info("IR_DISPLAY_CACHE,observers={},pages={},bytes={},pageIncrease={}",
                    after.observers(),after.pages(),after.bytes(),after.pages()-before.pages());
            helper.assertTrue(after.observers()>=100&&after.pages()-before.pages()<=64
                            &&after.bytes()<=InfraredDisplayCache.MAX_BYTES,
                    "Identical world Pages are shared rather than copied for each observer");
            helper.succeed();
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void encodedCacheEvictionReleasesSharedOwnership(GameTestHelper helper) {
        var cache=new InfraredDisplayCache(8192,2);
        var pages=new InfraredDisplayCache.Page[729];
        pages[0]=cache.page(helper.getLevel().dimension(),0,new byte[]{0,0,1,0,80},null);
        UUID a=new UUID(1,1),b=new UUID(1,2);
        cache.put(a,new InfraredDisplayCache.Display(helper.getLevel().dimension(),0,0,1,pages));
        cache.put(b,new InfraredDisplayCache.Display(helper.getLevel().dimension(),0,0,2,pages));
        helper.assertTrue(cache.find(a,helper.getLevel().dimension(),0,0,1)==null
                        &&cache.find(b,helper.getLevel().dimension(),0,0,2)!=null,
                "Budget pressure discards the oldest observer's acceleration data");
        helper.assertTrue(cache.statistics().bytes()<=8192&&cache.statistics().pages()==1,
                "A remaining observer retains exactly one shared encoded Page");
        cache.remove(b);
        helper.assertTrue(cache.statistics().bytes()==0&&cache.statistics().pages()==0,
                "The final reference releases all cached Page accounting");
        var dimensions=new InfraredDisplayCache(65536,4);
        dimensions.put(a,new InfraredDisplayCache.Display(helper.getLevel().dimension(),0,0,1,pages));
        var otherPages=new InfraredDisplayCache.Page[729];
        otherPages[0]=dimensions.page(net.minecraft.world.level.Level.NETHER,0,new byte[]{0,0,1,0,80},null);
        dimensions.put(b,new InfraredDisplayCache.Display(net.minecraft.world.level.Level.NETHER,0,0,2,otherPages));
        dimensions.removeDimension(helper.getLevel().dimension());
        helper.assertTrue(dimensions.find(a,helper.getLevel().dimension(),0,0,1)==null
                        &&dimensions.find(b,net.minecraft.world.level.Level.NETHER,0,0,2)!=null,
                "Dimension unload releases only displays owned by that dimension");
        dimensions.clear();
        helper.assertTrue(dimensions.statistics().bytes()==0,"Server shutdown clears the display cache");
        helper.succeed();
    }

    public static void pairedConstantField(GameTestHelper helper) {
        try(var scene=new Scene(helper)) {
            var cached=new View(scene.player); var reference=new View(scene.player);
            poll(cached,true,new Traffic()); poll(reference,true,new Traffic(),false);
            for(int i=0;i<32;i++) {poll(cached,false,new Traffic());poll(reference,false,new Traffic(),false);}
            var cachedTraffic=new Traffic(); var referenceTraffic=new Traffic();
            for(int i=0;i<64;i++) {
                if((i&1)==0){poll(cached,false,cachedTraffic);poll(reference,false,referenceTraffic,false);}
                else {poll(reference,false,referenceTraffic,false);poll(cached,false,cachedTraffic);}
            }
            referenceTraffic.report("paired-uncached"); cachedTraffic.report("paired-cached");
            helper.assertTrue(cachedTraffic.dataParts==0&&referenceTraffic.dataParts==64,
                    "The same final colors are served with acknowledgements only on the cached path");
            helper.succeed();
        }
    }

    private static Short temperature(InfraredSnapshot snapshot,BlockPos p) {
        if(snapshot==null)return null;
        int x=(p.getX()>>4)-(snapshot.centerChunkX()-4),z=(p.getZ()>>4)-(snapshot.centerChunkZ()-4),
                y=(p.getY()>>4)-(snapshot.centerSectionY()-4);
        if(x<0||x>=9||y<0||y>=9||z<0||z>=9)return null;
        int page=x+9*(z+9*y);
        int address=page*64+((p.getX()&15)>>>2)+(((p.getZ()&15)>>>2)<<2)+(((p.getY()&15)>>>2)<<4);
        int block=(p.getX()&3)|((p.getZ()&3)<<2)|((p.getY()&3)<<4);
        short[] decoded=new short[64]; var decoder=new InfraredBrickCodec.Decoder();
        for(byte[] part:snapshot.brickRecords()) {
            var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
            try { int brick; while((brick=decoder.readRecord(buffer,decoded))>=0) if(brick==address)return decoded[block]; }
            finally {buffer.release();}
        }
        return null;
    }

    private static InfraredSnapshot poll(View view, boolean full, Traffic traffic) {
        return poll(view,full,traffic,true);
    }

    private static InfraredSnapshot poll(View view, boolean full, Traffic traffic, boolean cached) {
        var old = view.snapshot;
        var request = new FHRequestInfraredViewDataSyncPacket(++view.sequence, full, old == null ? 0 : old.generation(),
                old == null ? 0 : SectionPos.asLong(old.centerChunkX(), old.centerSectionY(), old.centerChunkZ()),
                old == null ? 0 : old.infraredEpoch(), view.presence, old != null && old.readable(),
                old == null ? new long[0] : old.fieldPages(), old == null ? 0 : old.storedEpoch(),
                old == null ? 0 : old.storedSampleTick(), view.committed);
        traffic.packet(request, true);
        long allocated=traffic.allocated();
        long start = System.nanoTime();
        var result = cached ? MinecraftThermalInput.gameplayInfraredSnapshot(view.player, request)
                : MinecraftThermalInput.gameplayInfraredSnapshot(view.player,request.forceFull(),request.knownGeneration(),
                        request.knownCenter(),request.lastEpoch(),request.knownPresence(),request.knownReadable(),
                        request.knownFieldPages(),request.knownStoredEpoch(),request.knownStoredSampleTick());
        traffic.nanos[traffic.polls++] = System.nanoTime() - start;
        if(allocated>=0)traffic.queryAllocated+=traffic.allocated()-allocated;
        if (result == null) {
            traffic.packet(FHResponseInfraredViewDataSyncPacket.acknowledge(request), false);
            traffic.acknowledgements++;
        } else {
            for (int i = 0; i < Math.max(1, result.brickRecords().length); i++) {
                traffic.packet(new FHResponseInfraredViewDataSyncPacket(request.requestId(), result, i), false);
                traffic.dataParts++;
            }
            if (old != null && !result.full()) {
                long[] moved = new long[12];
                InfraredWindow.remap(view.presence,moved,
                        SectionPos.asLong(old.centerChunkX(),old.centerSectionY(),old.centerChunkZ()),
                        SectionPos.asLong(result.centerChunkX(),result.centerSectionY(),result.centerChunkZ()));
                view.presence=moved;
            }
            view.snapshot = result;
            view.committed = request.requestId();
            if (result.presence().length != 0) view.presence = result.presence();
        }
        return result;
    }

    private static final class View {
        final ServerPlayer player;
        InfraredSnapshot snapshot;
        long[] presence = new long[12];
        int sequence, committed = -1;
        View(ServerPlayer player) { this.player = player; }
    }

    private static final class Scene implements AutoCloseable {
        final ServerLevel level;
        final BlockPos p;
        final ServerPlayer player;
        final ThermalFieldKey key;
        ThermalAnalyticField.CombineMode mode=ThermalAnalyticField.CombineMode.OVERRIDE;
        Scene(GameTestHelper helper) {
            level = helper.getLevel();
            BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
            p = new BlockPos(8192 + (anchor.getX() >> 4) * 256 + 8, 168,
                    8192 + (anchor.getZ() >> 4) * 256 + 8);
            for (int z = -2; z <= 2; z++) for (int x = -2; x <= 2; x++)
                level.getChunk((p.getX() >> 4) + x, (p.getZ() >> 4) + z);
            player = FakePlayerFactory.get(level, new GameProfile(new UUID(0x1234, p.asLong()), "ir_bandwidth"));
            player.setPos(p.getX() + .5, p.getY(), p.getZ() + .5);
            key = new ThermalFieldKey(PROVIDER, 0, p.asLong(), 1);
            temperature(20);
        }
        void temperature(double value) {
            MinecraftGameplayFields.upsert(level, new ThermalAnalyticField(key, 0,
                    mode, ThermalAnalyticField.Shape.CUBE,
                    p.getX() + .5, p.getY() + .5, p.getZ() + .5, 24, 24, 24, value));
        }
        @Override public void close() { MinecraftGameplayFields.remove(level, key); }
    }

    private static final class Traffic {
        private static final java.lang.management.ThreadMXBean BEAN=java.lang.management.ManagementFactory.getThreadMXBean();
        final long[] nanos = new long[128];
        long requestBytes, responseBytes, requestFrames, responseFrames;
        int polls, dataParts, acknowledgements;
        long queryAllocated;

        long allocated() {
            return BEAN instanceof com.sun.management.ThreadMXBean bean&&bean.isThreadAllocatedMemoryEnabled()
                    ? bean.getThreadAllocatedBytes(Thread.currentThread().getId()):-1;
        }

        void packet(CMessage message, boolean toServer) {
            var body = new FriendlyByteBuf(Unpooled.buffer());
            try {
                message.encode(body);
                if (toServer) requestBytes += body.readableBytes(); else responseBytes += body.readableBytes();
            } finally { body.release(); }
            var direction = toServer ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT;
            var flow = toServer ? PacketFlow.SERVERBOUND : PacketFlow.CLIENTBOUND;
            var packet = FHNetwork.INSTANCE.get().toVanillaPacket(message, direction);
            var encoded = new FriendlyByteBuf(Unpooled.buffer());
            var channel = new EmbeddedChannel(new Varint21LengthFieldPrepender(), new CompressionEncoder(256));
            try {
                encoded.writeVarInt(ConnectionProtocol.PLAY.getPacketId(flow, packet));
                packet.write(encoded);
                channel.writeOutbound(encoded);
                encoded = null;
                ByteBuf frame;
                while ((frame = channel.readOutbound()) != null) {
                    try {
                        if (toServer) requestFrames += frame.readableBytes(); else responseFrames += frame.readableBytes();
                    } finally { frame.release(); }
                }
            } finally {
                if (encoded != null) encoded.release();
                if (packet instanceof ClientboundCustomPayloadPacket p) p.getData().release();
                if (packet instanceof ServerboundCustomPayloadPacket p) p.getData().release();
                channel.finishAndReleaseAll();
            }
        }

        void report(String scene) {
            long[] sorted = Arrays.copyOf(nanos, polls);
            Arrays.sort(sorted);
            FHMain.LOGGER.info("IR_BANDWIDTH,scene={},polls={},c2sBody={},s2cBody={},c2sFrames={},s2cFrames={},dataParts={},acks={},p50us={},p95us={},queryAllocatedBytes={},compressionThreshold=256",
                    scene, polls, requestBytes, responseBytes, requestFrames, responseFrames, dataParts, acknowledgements,
                    sorted[polls / 2] / 1000.0, sorted[Math.min(polls - 1, (int) Math.ceil(polls * .95) - 1)] / 1000.0,queryAllocated);
        }
    }
}
