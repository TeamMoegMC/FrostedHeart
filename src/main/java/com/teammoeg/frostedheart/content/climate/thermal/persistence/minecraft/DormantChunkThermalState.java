/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongPredicate;

/**
 * LevelChunk 自有的、有界休眠温度 checkpoint。
 *
 * <p>只保存重新进入时需要的 Brick/Air component 温度残差与一次性 source
 * 支持位；不保存 topology、arena slot、source 历史或离线 solver 状态。</p>
 */
public final class DormantChunkThermalState {
    private static final String ROOT_TAG = "FrostedHeartThermal";
    private static final int FORMAT_VERSION = 2;
    private static final int BRICKS = ThermalPageHandle.BASE_BRICK_COUNT;
    private static final int MAX_VALUES = 320;
    private static final int RESIDUAL_SCALE = 16;
    private static final int PRUNE_RESIDUAL = 4;
    private static final long CACHE_INTERVAL_TICKS = 20L;
    private static final AtomicLong INFRARED_REVISIONS = new AtomicLong();

    private final int minimumSectionY;
    private final SectionEntry[] entries;
    private InfraredSection[] infraredSections;
    private long[] cachedDecayTicks;
    private double[] cachedDecayFactors;
    private double[] cachedNaturalTemperatures;

    public DormantChunkThermalState(int minimumSectionY, int sectionCount) {
        if (sectionCount <= 0) {
            throw new IllegalArgumentException("sectionCount must be positive");
        }
        this.minimumSectionY = minimumSectionY;
        entries = new SectionEntry[sectionCount];
    }

    public static DormantChunkThermalState decode(
            CompoundTag chunkTag,
            int minimumSectionY,
            int sectionCount
    ) {
        if (!chunkTag.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag root = chunkTag.getCompound(ROOT_TAG);
        if (root.getInt("version") != FORMAT_VERSION) {
            return null;
        }
        DormantChunkThermalState result = new DormantChunkThermalState(
                minimumSectionY, sectionCount);
        ListTag sections = root.getList("sections", Tag.TAG_COMPOUND);
        for (Tag tag : sections) {
            CompoundTag section = (CompoundTag) tag;
            int sectionY = section.getInt("y");
            int index = sectionY - minimumSectionY;
            if (index < 0 || index >= sectionCount || result.entries[index] != null) {
                continue;
            }
            SectionEntry entry = SectionEntry.decode(section);
            if (entry != null) {
                result.entries[index] = entry;
            }
        }
        return result.isEmpty() ? null : result;
    }

    public void encode(CompoundTag chunkTag) {
        if (isEmpty()) {
            chunkTag.remove(ROOT_TAG);
            return;
        }
        CompoundTag root = new CompoundTag();
        root.putInt("version", FORMAT_VERSION);
        ListTag sections = new ListTag();
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            if (entry != null) {
                sections.add(entry.encode(minimumSectionY + index));
            }
        }
        root.put("sections", sections);
        chunkTag.put(ROOT_TAG, root);
    }

    public boolean replace(int sectionY, SectionEntry entry) {
        int index = index(sectionY);
        SectionEntry previous = entries[index];
        if (SectionEntry.contentEquals(previous, entry)) {
            return false;
        }
        entries[index] = entry;
        clearDecayCache(index);
        return true;
    }

    public boolean activateLoaded(long gameTick, double halfLifeSeconds) {
        boolean changed = false;
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            if (entry == null || !entry.sourceSustained) {
                continue;
            }
            entries[index] = entry.rebase(
                    gameTick,
                    decayFactor(entry.savedGameTick, gameTick, halfLifeSeconds),
                    true);
            clearDecayCache(index);
            changed = true;
        }
        return changed;
    }

    public boolean rebaseForSave(long gameTick, double halfLifeSeconds) {
        boolean changed = false;
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            if (entry == null) {
                continue;
            }
            SectionEntry rebased = entry.rebase(
                    gameTick,
                    decayFactor(entry.savedGameTick, gameTick, halfLifeSeconds),
                    false);
            if (!SectionEntry.contentEquals(entry, rebased)) {
                entries[index] = rebased;
                clearDecayCache(index);
                changed = true;
            }
        }
        return changed;
    }

    public boolean refreshSourceSupport(
            int sectionX,
            int sectionZ,
            LongPredicate supported
    ) {
        boolean changed = false;
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            if (entry == null) {
                continue;
            }
            long sectionKey = net.minecraft.core.SectionPos.asLong(
                    sectionX, minimumSectionY + index, sectionZ);
            boolean next = supported.test(sectionKey);
            if (entry.sourceSustained != next) {
                entries[index] = entry.withSourceSustained(next);
                changed = true;
            }
        }
        return changed;
    }

    public boolean updateSourceSupport(int sectionY, boolean supported) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length || entries[index] == null) {
            return false;
        }
        SectionEntry entry = entries[index];
        if (entry.sourceSustained == supported) {
            return false;
        }
        entries[index] = entry.withSourceSustained(supported);
        return true;
    }

    public long storedBrickMask(int sectionY) {
        int index = sectionY - minimumSectionY;
        return index < 0 || index >= entries.length || entries[index] == null
                ? 0L : entries[index].brickMask;
    }

    /** Main-thread, read-only cached view; populated only by an infrared request. */
    public InfraredSection infraredSection(
            int sectionY,
            long gameTick,
            double halfLifeSeconds,
            ServerLevel level,
            int sectionX,
            int sectionZ,
            BlockPos.MutableBlockPos naturalPosition
    ) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length
                || entries[index] == null) {
            return null;
        }
        if (infraredSections == null) {
            infraredSections = new InfraredSection[entries.length];
        }
        InfraredSection snapshot = infraredSections[index];
        if (snapshot == null) {
            snapshot = new InfraredSection();
            infraredSections[index] = snapshot;
        }
        long boundary = Math.floorDiv(gameTick, CACHE_INTERVAL_TICKS)
                * CACHE_INTERVAL_TICKS;
        if (snapshot.sampleTick == boundary) {
            return snapshot;
        }
        SectionEntry entry = entries[index];
        double factor = cachedDecayFactor(
                index, entry, gameTick, halfLifeSeconds, level,
                sectionX, sectionY, sectionZ, naturalPosition);
        long changed = snapshot.brickMask ^ entry.brickMask;
        long removed = snapshot.brickMask & ~entry.brickMask;
        while (removed != 0L) {
            snapshot.temperatures[Long.numberOfTrailingZeros(removed)] = Short.MIN_VALUE;
            removed &= removed - 1L;
        }
        long remaining = entry.brickMask;
        while (remaining != 0L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            long value = Math.round(entry.meanTemperatureC(
                    brick, cachedNaturalTemperatures[index], factor) * 4.0D);
            short quantized = (short) Math.max(-32767L, Math.min(32767L, value));
            if (snapshot.temperatures[brick] != quantized) {
                changed |= 1L << brick;
            }
            snapshot.temperatures[brick] = quantized;
            remaining &= remaining - 1L;
        }
        snapshot.brickMask = entry.brickMask;
        snapshot.sampleTick = boundary;
        if (snapshot.revision == 0L || changed != 0L) {
            snapshot.previousRevision = snapshot.revision;
            snapshot.changedBrickMask = changed;
            snapshot.revision = INFRARED_REVISIONS.incrementAndGet();
        }
        return snapshot;
    }

    /** Shared section-level change record, never serialized or updated by a tick sweep. */
    public static final class InfraredSection {
        private final short[] temperatures = new short[BRICKS];
        private long sampleTick = Long.MIN_VALUE;
        private long revision;
        private long previousRevision;
        private long changedBrickMask;
        private long brickMask;

        public long revision() { return revision; }
        public long previousRevision() { return previousRevision; }
        public long changedBrickMask() { return changedBrickMask; }
        public short[] temperatures() { return temperatures; }
    }

    public double sample(
            int sectionY,
            int brick,
            long gameTick,
            double halfLifeSeconds,
            ServerLevel level,
            int sectionX,
            int sectionZ,
            BlockPos.MutableBlockPos naturalPosition
    ) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length || brick < 0 || brick >= BRICKS) {
            return Double.NaN;
        }
        SectionEntry entry = entries[index];
        if (entry == null || !entry.hasBrick(brick)) {
            return Double.NaN;
        }
        double factor = cachedDecayFactor(
                index, entry, gameTick, halfLifeSeconds,
                level, sectionX, sectionY, sectionZ, naturalPosition);
        return cachedNaturalTemperatures[index]
                + entry.warmestResidual(brick) / (double) RESIDUAL_SCALE * factor;
    }

    public ThermalInputBatch.DormantAirCut admissionCut(
            int sectionY,
            long gameTick,
            double halfLifeSeconds,
            double currentNaturalTemperatureC
    ) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length || entries[index] == null) {
            return null;
        }
        SectionEntry entry = entries[index];
        return new ThermalInputBatch.DormantAirCut(
                entry,
                currentNaturalTemperatureC,
                decayFactor(entry.savedGameTick, gameTick, halfLifeSeconds));
    }

    public boolean isEmpty() {
        for (SectionEntry entry : entries) {
            if (entry != null) {
                return false;
            }
        }
        return true;
    }

    private double cachedDecayFactor(
            int index,
            SectionEntry entry,
            long gameTick,
            double halfLifeSeconds,
            ServerLevel level,
            int sectionX,
            int sectionY,
            int sectionZ,
            BlockPos.MutableBlockPos naturalPosition
    ) {
        long boundary = Math.floorDiv(gameTick, CACHE_INTERVAL_TICKS)
                * CACHE_INTERVAL_TICKS;
        if (cachedDecayTicks == null) {
            cachedDecayTicks = new long[entries.length];
            cachedDecayFactors = new double[entries.length];
            cachedNaturalTemperatures = new double[entries.length];
            Arrays.fill(cachedDecayTicks, Long.MIN_VALUE);
        }
        if (cachedDecayTicks[index] != boundary) {
            cachedDecayTicks[index] = boundary;
            cachedDecayFactors[index] = decayFactor(
                    entry.savedGameTick, boundary, halfLifeSeconds);
            naturalPosition.set(
                    SectionPos.sectionToBlockCoord(sectionX) + 8,
                    SectionPos.sectionToBlockCoord(sectionY) + 8,
                    SectionPos.sectionToBlockCoord(sectionZ) + 8);
            cachedNaturalTemperatures[index] = WorldTemperature.naturalAir(
                    level, naturalPosition);
        }
        return cachedDecayFactors[index];
    }

    private void clearDecayCache(int index) {
        if (cachedDecayTicks != null) {
            cachedDecayTicks[index] = Long.MIN_VALUE;
        }
        if (infraredSections != null && infraredSections[index] != null) {
            if (entries[index] == null) {
                infraredSections[index] = null;
            } else {
                infraredSections[index].sampleTick = Long.MIN_VALUE;
            }
        }
    }

    private int index(int sectionY) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length) {
            throw new IllegalArgumentException("sectionY is outside the owning chunk");
        }
        return index;
    }

    private static double decayFactor(
            long savedGameTick,
            long currentGameTick,
            double halfLifeSeconds
    ) {
        if (!Double.isFinite(halfLifeSeconds) || halfLifeSeconds <= 0.0D) {
            throw new IllegalArgumentException("halfLifeSeconds must be positive");
        }
        long elapsed = Math.max(0L, currentGameTick - savedGameTick);
        return Math.pow(2.0D, -elapsed / (halfLifeSeconds * 20.0D));
    }

    public static CaptureResult capture(PagePublication publication, QueryPublication queries,
            QueryPublication.MutableSample sample, double naturalTemperatureC, CaptureScratch scratch) {
        long brickMask=0, sampleTick=-1;
        int exactNodes=0;
        for(int brick=0;brick<64;brick++) {
            var payload=publication.brick(brick);
            int n=payload.transportNodeCount();
            scratch.counts[brick]=0;
            if(payload.coverageSlot()<0 || n==0) continue;
            double sum=0; int blocks=0; boolean different=false, retained=false;
            short firstResidual=0;
            for(int node=0;node<n;node++) {
                if(!queries.tryRead(payload.coverageSlot()+node,payload.arenaGeneration(),publication.topologyGeneration(),sample))
                    return CaptureResult.FAILED;
                if(sampleTick<0) sampleTick=sample.sampleTick();
                else if(sampleTick!=sample.sampleTick()) return CaptureResult.FAILED;
                long mask=payload.blockLayout()==null ? -1L : payload.blockLayout().nodeBlockMask(node);
                int weight=Long.bitCount(mask);
                short residual=quantizeResidual(sample.temperatureC()-naturalTemperatureC);
                int index=brick*64+node;
                scratch.nodeMasks[index]=mask; scratch.nodeResiduals[index]=residual;
                if(node==0) firstResidual=residual; else different|=residual!=firstResidual;
                retained|=Math.abs(residual)>PRUNE_RESIDUAL;
                sum+=sample.temperatureC()*weight; blocks+=weight;
            }
            if(!retained) continue;
            brickMask|=1L<<brick;
            scratch.means[brick]=quantizeResidual(sum/blocks-naturalTemperatureC);
            if(different) { scratch.counts[brick]=(byte)n; exactNodes+=n; }
        }
        if(brickMask==0) return new CaptureResult(true,null);
        int means=Long.bitCount(brickMask);
        boolean exact=8*((means+exactNodes+3)/4)+8*exactNodes<=640;
        short[] residuals=new short[means+(exact?exactNodes:0)];
        long[] masks=new long[exact?exactNodes:0];
        byte[] counts=new byte[means];
        int value=0,entry=0,maskIndex=0;
        for(int brick=0;brick<64;brick++) {
            if((brickMask & 1L<<brick)==0) continue;
            residuals[value++]=scratch.means[brick];
            int count=exact?Byte.toUnsignedInt(scratch.counts[brick]):0;
            counts[entry++]=(byte)count;
            for(int node=0;node<count;node++) {
                masks[maskIndex++]=scratch.nodeMasks[brick*64+node];
                residuals[value++]=scratch.nodeResiduals[brick*64+node];
            }
        }
        return new CaptureResult(true,new SectionEntry(sampleTick,false,brickMask,counts,pack(residuals),masks));
    }
    public static final class CaptureScratch {
        final long[] nodeMasks=new long[4096];
        final short[] nodeResiduals=new short[4096], means=new short[64];
        final byte[] counts=new byte[64];
    }
    public record CaptureResult(boolean valid,SectionEntry entry) {
        private static final CaptureResult FAILED=new CaptureResult(false,null);
    }
    /** Spatial temperatures, independent of transient node ordinals. */
    public static final class SectionEntry {
        private final long savedGameTick, brickMask;
        private final boolean sourceSustained;
        private final byte[] exactCounts;
        private final long[] residuals, blockMasks;
        private final short[] valueOffsets=new short[64], maskOffsets=new short[64];
        public SectionEntry(long savedGameTick,boolean sourceSustained,long brickMask,
                byte[] exactCounts,long[] residuals,long[] blockMasks) {
            this.savedGameTick=Math.max(0,savedGameTick); this.sourceSustained=sourceSustained;
            this.brickMask=brickMask; this.exactCounts=exactCounts; this.residuals=residuals; this.blockMasks=blockMasks;
            int value=0,mask=0,rank=0;
            for(int brick=0;brick<64;brick++) if(hasBrick(brick)) {
                valueOffsets[brick]=(short)value; maskOffsets[brick]=(short)mask;
                int n=Byte.toUnsignedInt(exactCounts[rank++]); value+=1+n; mask+=n;
            }
        }
        private static SectionEntry decode(CompoundTag tag) {
            long bricks=tag.getLong("bricks"); byte[] counts=tag.getByteArray("counts");
            long[] values=tag.getLongArray("residuals"), masks=tag.getLongArray("blocks");
            if(bricks==0 || counts.length!=Long.bitCount(bricks)) return null;
            int exact=0;
            for(byte count:counts) { int n=Byte.toUnsignedInt(count); if(n>64) return null; exact+=n; }
            if(masks.length!=exact || values.length!=(counts.length+exact+3)/4 || 8*(values.length+masks.length)>640) return null;
            int at=0;
            for(byte count:counts) {
                long seen=0;
                for(int n=0;n<Byte.toUnsignedInt(count);n++) {
                    long m=masks[at++]; if(m==0 || (seen&m)!=0) return null; seen|=m;
                }
            }
            return new SectionEntry(tag.getLong("tick"),tag.getBoolean("supported"),bricks,counts,values,masks);
        }
        private CompoundTag encode(int sectionY) {
            CompoundTag tag=new CompoundTag(); tag.putInt("y",sectionY); tag.putLong("tick",savedGameTick);
            tag.putBoolean("supported",sourceSustained); tag.putLong("bricks",brickMask);
            tag.putByteArray("counts",exactCounts); tag.putLongArray("residuals",residuals); tag.putLongArray("blocks",blockMasks);
            return tag;
        }
        public boolean hasBrick(int brick) { return (brickMask & 1L<<brick)!=0; }
        private int exactCount(int brick) { return Byte.toUnsignedInt(exactCounts[Long.bitCount(brickMask & lowerBits(brick))]); }
        private short meanResidual(int brick) { return residualAt(residuals,valueOffsets[brick]); }
        public double meanTemperatureC(int brick,double natural,double factor) {
            return natural+meanResidual(brick)/(double)RESIDUAL_SCALE*factor;
        }
        public void fillBlockTemperatures(int brick,double natural,double factor,double[] target) {
            Arrays.fill(target,0,64,meanTemperatureC(brick,natural,factor));
            int count=exactCount(brick), offset=valueOffsets[brick]+1, masks=maskOffsets[brick];
            for(int i=0;i<count;i++) {
                double t=natural+residualAt(residuals,offset+i)/(double)RESIDUAL_SCALE*factor;
                long mask=blockMasks[masks+i];
                while(mask!=0) { int b=Long.numberOfTrailingZeros(mask); mask&=mask-1; target[b]=t; }
            }
        }
        public short warmestResidual(int brick) {
            int n=exactCount(brick); short warmest=n==0?meanResidual(brick):Short.MIN_VALUE;
            for(int i=0;i<n;i++) warmest=(short)Math.max(warmest,residualAt(residuals,valueOffsets[brick]+1+i));
            return warmest;
        }
        private SectionEntry withSourceSustained(boolean value) {
            return value==sourceSustained?this:new SectionEntry(savedGameTick,value,brickMask,exactCounts,residuals,blockMasks);
        }
        private SectionEntry rebase(long tick,double factor,boolean support) {
            short[] output=new short[MAX_VALUES]; byte[] counts=new byte[64]; long[] masks=new long[blockMasks.length];
            int values=0,entries=0,maskCount=0; long retainedBricks=0;
            for(int brick=0;brick<64;brick++) {
                if(!hasBrick(brick)) continue;
                int n=exactCount(brick); double scale=support && warmestResidual(brick)>0?1:factor;
                boolean retain=false;
                for(int i=0;i<=n;i++) {
                    short v=scaleResidual(residualAt(residuals,valueOffsets[brick]+i),scale);
                    output[values+i]=v; retain|=Math.abs(v)>PRUNE_RESIDUAL;
                }
                if(!retain) continue;
                retainedBricks|=1L<<brick; counts[entries++]=(byte)n; values+=1+n;
                System.arraycopy(blockMasks,maskOffsets[brick],masks,maskCount,n); maskCount+=n;
            }
            return retainedBricks==0?null:new SectionEntry(tick,false,retainedBricks,Arrays.copyOf(counts,entries),
                    pack(Arrays.copyOf(output,values)),Arrays.copyOf(masks,maskCount));
        }
        private static boolean contentEquals(SectionEntry a,SectionEntry b) {
            return a==b || a!=null && b!=null && a.savedGameTick==b.savedGameTick
                    && a.sourceSustained==b.sourceSustained && a.brickMask==b.brickMask
                    && Arrays.equals(a.exactCounts,b.exactCounts) && Arrays.equals(a.residuals,b.residuals)
                    && Arrays.equals(a.blockMasks,b.blockMasks);
        }
    }
    private static long[] pack(short[] values) {
        long[] result=new long[(values.length+3)/4];
        for(int i=0;i<values.length;i++) putResidual(result,i,values[i]);
        return result;
    }

    private static short quantizeResidual(double residualC) {
        if (!Double.isFinite(residualC)) {
            throw new IllegalArgumentException("temperature residual must be finite");
        }
        return (short) Math.max(
                Short.MIN_VALUE,
                Math.min(Short.MAX_VALUE, Math.round(residualC * RESIDUAL_SCALE)));
    }

    private static short scaleResidual(short residual, double factor) {
        return (short) Math.max(
                Short.MIN_VALUE,
                Math.min(Short.MAX_VALUE, Math.round(residual * factor)));
    }

    private static long lowerBits(int bit) {
        return bit == 0 ? 0L : -1L >>> (Long.SIZE - bit);
    }

    private static short residualAt(long[] packed, int index) {
        return (short) (packed[index >>> 2] >>> ((index & 3) << 4));
    }

    private static void putResidual(long[] packed, int index, short value) {
        packed[index >>> 2] |= ((long) value & 0xffffL) << ((index & 3) << 4);
    }
}
