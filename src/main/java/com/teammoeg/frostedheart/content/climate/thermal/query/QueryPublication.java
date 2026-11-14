/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public final class QueryPublication implements AutoCloseable {
   private final ThermalMemoryBudget budget;
   private final int maximumPages;
   private int[] pageChangeEpochs;
   private int[] brickChangeEpochs;
   private long[] pendingInfraredBrickMasks;
   private int[] dirtyInfraredPages;
   private int dirtyInfraredCount;
   private ThermalMemoryBudget.Reservation pageReservation;
   private boolean acceptingPublications = true;
   private boolean valid;
   private long topologyGeneration = -1L;
   private long sampleTick = -1L;
   private int infraredEpoch;
   private long infraredActiveUntilTick = -1L;
   private volatile TiledThermalField fixedField;
   private Long2ObjectOpenHashMap<QueryPublication.SectionPublication> fixedSections;
   private IdentityHashMap<PagePublication, QueryPublication.SectionPublication> fixedGeometry;
   private TiledThermalField.Sample fixedSample;
   private long fixedVersion;
   private int fixedReaders;
   private ThermalMemoryBudget.Reservation closedReaderPageReservation;
   private final Long2BooleanOpenHashMap infraredRegionFreshness = new Long2BooleanOpenHashMap();

   private boolean hasPendingInfraredChangesSafe() {
      return this.pendingInfraredBrickMasks != null && this.hasPendingInfraredChanges();
   }

   private QueryPublication(ThermalMemoryBudget budget, int maximumPages, TiledThermalField field) {
      this.budget = budget;
      this.maximumPages = maximumPages;
      this.fixedField = Objects.requireNonNull(field);
      long bytes = projectedPagePayloadBytes(maximumPages) + 4096L + 192L * maximumPages;
      pageReservation = budget.tryReserve(bytes);
      if (pageReservation == null) throw new ThermalMemoryBudget.AllocationRefused(bytes);
      try {
         fixedSections = new Long2ObjectOpenHashMap<>(maximumPages);
         fixedGeometry = new IdentityHashMap<>(maximumPages);
         fixedSample = new TiledThermalField.Sample();
      } catch (RuntimeException | Error failure) { pageReservation.close(); throw failure; }
   }

   public static QueryPublication createFixed(ThermalMemoryBudget budget, int maximumPages, TiledThermalField field) {
      return new QueryPublication(budget, maximumPages, (TiledThermalField)Objects.requireNonNull(field));
   }

   public synchronized TiledThermalField fixedState() {
      return this.fixedField;
   }

   public synchronized boolean publishFixed(QueryPublication.SectionPublication[] changed) {
      return this.publishFixed(changed, () -> {
      });
   }

   public synchronized boolean publishFixed(QueryPublication.SectionPublication[] changed, Runnable geometryCommit) {
      if (this.acceptingPublications && this.fixedField != null && this.fixedField.tileCount() <= this.maximumPages) {
         for(QueryPublication.SectionPublication section : changed) {
            int slot = section.geometry.workerPageSlot();
            if (slot < 0 || slot >= this.maximumPages || !this.fixedField.hasSection(section.handle.sectionKey())) {
               return false;
            }
         }

         if (!this.fixedField.commit()) {
            return false;
         } else {
            geometryCommit.run();
            ObjectIterator<QueryPublication.SectionPublication> sections = this.fixedSections.values().iterator();

            while(sections.hasNext()) {
               QueryPublication.SectionPublication previous = (QueryPublication.SectionPublication)sections.next();
               if (!this.fixedField.hasSection(previous.handle.sectionKey())) {
                  int slot = previous.geometry.workerPageSlot();
                  this.fixedGeometry.remove(previous.geometry);
                  this.infraredRegionFreshness.remove(previous.handle.sectionKey());
                  if (this.pendingInfraredBrickMasks != null) {
                     this.addInfraredChange(slot, -1L);
                  }

                  sections.remove();
               }
            }

            for(QueryPublication.SectionPublication section : changed) {
               QueryPublication.SectionPublication previous = (QueryPublication.SectionPublication)this.fixedSections.put(section.handle.sectionKey(), section);
               if (previous != null) {
                  this.fixedGeometry.remove(previous.geometry);
               }

               this.fixedGeometry.put(section.geometry, section);
               this.topologyGeneration = Math.max(this.topologyGeneration, section.geometry.topologyGeneration());
            }

            this.sampleTick = this.fixedField.acceptedTick();
            ++this.fixedVersion;
            this.valid = true;
            if (this.pendingInfraredBrickMasks != null) {
               ObjectIterator var12 = this.fixedSections.values().iterator();

               while(var12.hasNext()) {
                  QueryPublication.SectionPublication section = (QueryPublication.SectionPublication)var12.next();
                  this.addInfraredChange(section.geometry.workerPageSlot(), this.fixedField.activeBricks(section.handle.sectionKey()));
               }

               if (this.hasPendingInfraredChangesSafe()) {
                  this.commitInfraredChanges();
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static long projectedPagePayloadBytes(int maximumPages) {
      return Math.multiplyExact((long)maximumPages, 296L);
   }

   public synchronized void recordAirLayoutBacking(AirBackingMeter meter) {
        if (fixedField == null) return;
        for (var section : fixedSections.values()) {
            section.geometry.recordBacking(meter);
            var previous = section.handle.previousPublication();
            if (previous != null) previous.recordBacking(meter);
        }
}

   public synchronized int retainedReaderCount() {
      return this.fixedReaders;
   }

   public synchronized long retainedReaderPayloadBytes() {
        return 0L;
}

   public synchronized String describeReaderMemory() {
        return "queryUsedBytes=" + budget.usedBytes() + ",queryLimitBytes=" + budget.limitBytes() + ",retainedReaders=" + fixedReaders;
}

   public synchronized long closedReaderAirBackingBytes() {
        return 0L;
}

   public synchronized long closedReaderPageBackingBytes() {
      return this.closedReaderPageReservation == null ? 0L : this.closedReaderPageReservation.bytes();
   }

   public synchronized long regionAt(double x, double y, double z) {
        if (fixedField == null) return -1;
        var section = fixedSections.get(SectionPos.asLong(BlockPos.containing(x, y, z)));
        return section == null ? -1 : section.geometry.workerPageSlot() + 1L;
}

   public boolean tryReadAir(double x, double y, double z, QueryPublication.MutableSample out) {
      return this.readAir(x, y, z, -1L, 0, out, false);
   }

   public boolean tryReadCapturedAir(double x, double y, double z, long gameTick, int maximumAgeTicks, QueryPublication.MutableSample out) {
      return this.readAir(x, y, z, gameTick, maximumAgeTicks, out, true);
   }

   private boolean readAir(double x, double y, double z, long gameTick, int maximumAgeTicks, QueryPublication.MutableSample out, boolean captured) {
        out.clear();
        var field = fixedField;
        if (field == null || !field.hasSection(SectionPos.asLong((int) Math.floor(x) >> 4, (int) Math.floor(y) >> 4, (int) Math.floor(z) >> 4))) return false;
        synchronized (this) { return fixedField != null && readFixedAir(x, y, z, gameTick, maximumAgeTicks, out, captured); }
}

   private boolean readFixedAir(double x, double y, double z, long tick, int maximumAge, QueryPublication.MutableSample out, boolean captured) {
      int blockX = (int)Math.floor(x);
      int blockY = (int)Math.floor(y);
      int blockZ = (int)Math.floor(z);
      QueryPublication.SectionPublication section = (QueryPublication.SectionPublication)this.fixedSections.get(SectionPos.asLong(blockX >> 4, blockY >> 4, blockZ >> 4));
      if (section == null) {
         return false;
      } else {
         int attempt = 0;

         while(attempt < 2) {
            if (!captured && !section.handle.hasCurrentAirGeometry(section.geometry.geometryRevision())) {
               return false;
            }

            if (!this.fixedField.sampleAir(x, y, z, this.fixedSample)) {
               return false;
            }

            long age = tick - this.fixedSample.sampleTick();
            if (!captured || this.fixedSample.sampleTick() >= 0L && age >= 0L && age <= (long)maximumAge) {
               if (!captured && !section.handle.hasCurrentAirGeometry(section.geometry.geometryRevision())) {
                  ++attempt;
                  continue;
               }

               out.set(this.fixedSample.temperatureC(), this.fixedSample.sampleTick());
               out.airBasisTerms = 8;
               return true;
            }

            return false;
         }

         return false;
      }
   }

   public synchronized String describeAirRead(double x, double y, double z) {
        if (fixedField == null) return "component_missing,tick=-1";
        var position = BlockPos.containing(x, y, z);
        var section = fixedSections.get(SectionPos.asLong(position));
        if (section == null || !fixedField.read(position.asLong(), false, fixedSample)) return "component_missing,tick=-1";
        return (section.handle.hasCurrentAirGeometry(section.geometry.geometryRevision()) ? "geometry_current" : "geometry_mismatch")
                + ",tick=" + fixedSample.sampleTick() + ",basis=8,section=" + section.handle.sectionKey() + ",minimum=false";
}

   public synchronized boolean noteInfraredRequest(long gameTick, int activeTicks) {
      requireNonNegative("gameTick", gameTick);
      if (activeTicks <= 0) {
         throw new IllegalArgumentException("activeTicks must be positive");
      } else if (!this.acceptingPublications) {
         return false;
      } else {
         long deadline = Math.addExact(gameTick, (long)activeTicks);
         boolean reactivated = gameTick > this.infraredActiveUntilTick;
         this.infraredActiveUntilTick = Math.max(this.infraredActiveUntilTick, deadline);
         int[] newPageEpochs = null;
         int[] newBrickEpochs = null;
         long[] newPendingMasks = null;
         if (reactivated && this.pageChangeEpochs == null) {
            newPageEpochs = new int[this.maximumPages];
            newBrickEpochs = new int[Math.multiplyExact(this.maximumPages, 64)];
            newPendingMasks = new long[this.maximumPages];
         }

         if (!reactivated) {
            this.updateRegionalInfraredFreshness(gameTick);
            return false;
         } else {
            if (newPageEpochs != null) {
               this.pageChangeEpochs = newPageEpochs;
               this.brickChangeEpochs = newBrickEpochs;
               this.pendingInfraredBrickMasks = newPendingMasks;
               this.dirtyInfraredPages = new int[this.maximumPages];
            }

            int next = this.nextInfraredEpoch();
            Arrays.fill(this.pageChangeEpochs, next);
            Arrays.fill(this.brickChangeEpochs, next);
            Arrays.fill(this.pendingInfraredBrickMasks, 0L);
            this.dirtyInfraredCount = 0;
            this.infraredEpoch = next;
            this.updateRegionalInfraredFreshness(gameTick);
            return true;
         }
      }
   }

   private void updateRegionalInfraredFreshness(long gameTick) {
        if (fixedField == null) return;
        for (var section : fixedSections.values()) {
            long key = section.handle.sectionKey(), tick = fixedField.sectionSampleTick(key), age = gameTick - tick;
            boolean fresh = tick >= 0 && age >= 0 && age <= 40;
            if (!infraredRegionFreshness.containsKey(key) || infraredRegionFreshness.get(key) != fresh)
                addInfraredChange(section.geometry.workerPageSlot(), fixedField.activeBricks(key));
            infraredRegionFreshness.put(key, fresh);
        }
        if (hasPendingInfraredChangesSafe()) commitInfraredChanges();
}

   public boolean beginRead(QueryPublication.ReadCursor out) {
      if (out == null) {
         throw new IllegalArgumentException("out is required");
      } else {
         out.clear();
         synchronized(this) {
            if (this.acceptingPublications && this.valid) {
               if (this.fixedField != null) {
                  ++this.fixedReaders;
                  out.owner = this;
                  out.fixedVersion = this.fixedVersion;
                  out.valid = true;
                  out.topologyGeneration = this.topologyGeneration;
                  out.sampleTick = this.sampleTick;
                  out.infraredEpoch = this.infraredEpoch;
                  out.pageChangeEpochs = this.pageChangeEpochs;
                  out.brickChangeEpochs = this.brickChangeEpochs;
                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   public synchronized boolean tryReadMaterial(PagePublication page, int blockIndex, int expectedStateId, MaterialSample out) {
        out.clear();
        if (fixedField == null) return false;
        var section = fixedGeometry.get(page);
        if (section == null) return false;
        long key = section.handle.sectionKey();
        long position = BlockPos.asLong((SectionPos.x(key) << 4) + (blockIndex & 15), (SectionPos.y(key) << 4) + (blockIndex >>> 8), (SectionPos.z(key) << 4) + (blockIndex >>> 4 & 15));
        if (!fixedField.read(position, true, fixedSample) || fixedSample.materialStateId() != expectedStateId) return false;
        out.setLive(fixedSample.enthalpyJ(), fixedSample.law(), fixedSample.branch(), fixedSample.sampleTick(), fixedSample.requestSequence());
        return true;
}

   public synchronized boolean publishFixedMaterialControl(PagePublication page, int blockIndex, int materialGeneration, double enthalpyJ, byte branch, boolean waiting, long requestSequence) {
      if (this.acceptingPublications && this.fixedField != null) {
         QueryPublication.SectionPublication section = (QueryPublication.SectionPublication)this.fixedGeometry.get(page);
         if (section == null) {
            return false;
         } else {
            long key = section.handle.sectionKey();
            long position = BlockPos.asLong((SectionPos.x(key) << 4) + (blockIndex & 15), (SectionPos.y(key) << 4) + (blockIndex >>> 8), (SectionPos.z(key) << 4) + (blockIndex >>> 4 & 15));
            if (!this.fixedField.applyMaterialControl(position, materialGeneration, enthalpyJ, branch, waiting, requestSequence)) {
               return false;
            } else {
               ++this.fixedVersion;
               if (this.pendingInfraredBrickMasks != null) {
                  int brick = (blockIndex & 15) >>> 2 | (blockIndex >>> 4 & 15) >>> 2 << 2 | blockIndex >>> 10 << 4;
                  this.addInfraredChange(page.workerPageSlot(), 1L << brick);
                  this.commitInfraredChanges();
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean readFixedCell(PagePublication page, int blockIndex) {
      QueryPublication.SectionPublication section = (QueryPublication.SectionPublication)this.fixedGeometry.get(page);
      if (section == null) {
         return false;
      } else {
         long key = section.handle.sectionKey();
         long position = BlockPos.asLong((SectionPos.x(key) << 4) + (blockIndex & 15), (SectionPos.y(key) << 4) + (blockIndex >>> 8), (SectionPos.z(key) << 4) + (blockIndex >>> 4 & 15));
         return this.fixedField.read(position, true, this.fixedSample) || this.fixedField.read(position, false, this.fixedSample);
      }
   }

   private boolean currentFixedReader(QueryPublication.ReadCursor cursor) {
      return this.acceptingPublications && this.fixedField != null && cursor.valid && cursor.fixedVersion == this.fixedVersion;
   }

   private synchronized void releaseFixedReader() {
      --this.fixedReaders;
      if (this.fixedReaders == 0 && this.closedReaderPageReservation != null) {
         this.closedReaderPageReservation.close();
         this.closedReaderPageReservation = null;
      }

   }

   private AirFieldCheckpoint captureFixedAir(QueryPublication.ReadCursor cursor, PagePublication page, long mask, double naturalC) {
      if (!this.currentFixedReader(cursor)) {
         return null;
      } else {
         QueryPublication.SectionPublication section = (QueryPublication.SectionPublication)this.fixedGeometry.get(page);
         if (section == null) {
            return null;
         } else {
            long key = section.handle.sectionKey();
            return this.fixedField.captureAir(key, mask, naturalC);
         }
      }
   }

   public synchronized void close() {
      this.acceptingPublications = false;
      this.clearEnvelope();

      if (this.pageReservation != null) {
         if (this.fixedReaders == 0) {
            this.pageReservation.close();
         } else {
            this.closedReaderPageReservation = this.pageReservation;
         }
      }

      this.pageReservation = null;
      this.pageChangeEpochs = null;
      this.brickChangeEpochs = null;
      this.pendingInfraredBrickMasks = null;
      this.dirtyInfraredPages = null;
      this.dirtyInfraredCount = 0;
   }

   private void clearEnvelope() {
        fixedField = null; fixedSections = null; fixedGeometry = null; fixedSample = null;
        valid = false; topologyGeneration = -1; sampleTick = -1;
        infraredRegionFreshness.clear();
}

   private void addInfraredChange(int page, long mask) {
      if (mask != 0L) {
         if (this.pendingInfraredBrickMasks[page] == 0L) {
            this.dirtyInfraredPages[this.dirtyInfraredCount++] = page;
         }

         this.pendingInfraredBrickMasks[page] |= mask;
      }
   }

   private boolean hasPendingInfraredChanges() {
      return this.dirtyInfraredCount != 0;
   }

   private void commitInfraredChanges() {
      int next = this.nextInfraredEpoch();

      for(int index = 0; index < this.dirtyInfraredCount; ++index) {
         int pageSlot = this.dirtyInfraredPages[index];
         long remaining = this.pendingInfraredBrickMasks[pageSlot];
         this.pageChangeEpochs[pageSlot] = next;

         for(int firstBrick = pageSlot << 6; remaining != 0L; remaining &= remaining - 1L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            this.brickChangeEpochs[firstBrick + brick] = next;
         }

         this.pendingInfraredBrickMasks[pageSlot] = 0L;
      }

      this.dirtyInfraredCount = 0;
      this.infraredEpoch = next;
   }

   private int nextInfraredEpoch() {
      if (this.infraredEpoch != Integer.MAX_VALUE) {
         return this.infraredEpoch + 1;
      } else {
         Arrays.fill(this.pageChangeEpochs, 0);
         Arrays.fill(this.brickChangeEpochs, 0);

         return 1;
      }
   }

   static short quantizedInfrared(double temperatureC) {
      long value = Math.round(temperatureC * 4.0D);
      return (short)((int)Math.max(-32767L, Math.min(32767L, value)));
   }

   private static void requireNonNegative(String name, long value) {
      if (value < 0L) {
         throw new IllegalArgumentException(name + " must be non-negative");
      }
   }

   public static final class MutableSample {
      private double temperatureC;
      private long sampleTick;
      private int airBasisTerms;
      private boolean minimumQuality;

      public int airBasisTerms() {
         return this.airBasisTerms;
      }

      public boolean minimumQuality() {
         return this.minimumQuality;
      }

      public double temperatureC() {
         return this.temperatureC;
      }

      public long sampleTick() {
         return this.sampleTick;
      }

      private void set(double temperatureC, long sampleTick) {
         this.temperatureC = temperatureC;
         this.sampleTick = sampleTick;
         this.airBasisTerms = 1;
         this.minimumQuality = false;
      }

      private void clear() {
         this.temperatureC = Double.NaN;
         this.sampleTick = -1L;
         this.airBasisTerms = 0;
         this.minimumQuality = false;
      }
   }

   public static final class ReadCursor {
      private QueryPublication owner;
      private boolean valid;
      private long topologyGeneration;
      private long sampleTick;
      private int infraredEpoch;
      private int[] pageChangeEpochs;
      private int[] brickChangeEpochs;
      private long fixedVersion;

      public int airAggregationSpanAt(double x, double y, double z) {
         if (!this.valid) {
            return -1;
         } else {
            synchronized(this.owner) {
               if (!this.owner.currentFixedReader(this)) {
                  return -1;
               } else {
                  int bx = (int)Math.floor(x);
                  int by = (int)Math.floor(y);
                  int bz = (int)Math.floor(z);
                  QueryPublication.SectionPublication section = (QueryPublication.SectionPublication)this.owner.fixedSections.get(SectionPos.asLong(bx >> 4, by >> 4, bz >> 4));
                  return section != null && section.handle.hasCurrentAirGeometry(section.geometry.geometryRevision()) && this.owner.fixedField.read(BlockPos.asLong(bx, by, bz), false, this.owner.fixedSample) ? 1 : -1;
               }
            }
         }
      }

      public AirFieldCheckpoint captureValidatedAir(PagePublication page, double naturalC) {
         return this.captureValidatedAir(page, naturalC, -1L, false);
      }

      public AirFieldCheckpoint captureValidatedAir(PagePublication page, double naturalC, long brickMask, boolean retiring) {
         if (!this.valid) {
            return null;
         } else {
            synchronized(this.owner) {
               return this.owner.captureFixedAir(this, page, brickMask, naturalC);
            }
         }
      }

      public boolean valid() {
         return this.valid && (this.isCurrent());
      }

      public boolean hasFreshSample(long gameTick, int maximumAgeTicks) {
         if (!this.valid) {
            return false;
         } else {
            synchronized(this.owner) {
               return this.owner.currentFixedReader(this) && this.owner.fixedField.hasFreshSample(gameTick, maximumAgeTicks);
            }
         }
      }

      public long pageAirSampleTick(PagePublication page) {
         if (!this.valid) {
            return -1L;
         } else {
            synchronized(this.owner) {
               QueryPublication.SectionPublication section = this.owner.currentFixedReader(this) ? (QueryPublication.SectionPublication)this.owner.fixedGeometry.get(page) : null;
               return section == null ? -1L : this.owner.fixedField.sectionSampleTick(section.handle.sectionKey());
            }
         }
      }

      public int infraredEpoch() {
         return this.infraredEpoch;
      }

      public long topologyGeneration() {
         return this.topologyGeneration;
      }

      public int pageChangeEpoch(int pageSlot) {
         if (pageSlot >= 0 && this.pageChangeEpochs != null && pageSlot < this.pageChangeEpochs.length) {
            {
               synchronized(this.owner) {
                  return this.owner.currentFixedReader(this) ? this.pageChangeEpochs[pageSlot] : 0;
               }
            }
         } else {
            throw new IllegalArgumentException("Page slot is outside the read cursor");
         }
      }

      public int brickChangeEpoch(int pageSlot, int brickIndex) {
         if (pageSlot >= 0 && this.pageChangeEpochs != null && pageSlot < this.pageChangeEpochs.length && brickIndex >= 0 && brickIndex < 64) {
            {
               synchronized(this.owner) {
                  return this.owner.currentFixedReader(this) ? this.brickChangeEpochs[(pageSlot << 6) + brickIndex] : 0;
               }
            }
         } else {
            throw new IllegalArgumentException("Brick is outside the read cursor");
         }
      }

      public boolean isCurrent() {
        if (owner == null) return false;
        synchronized (owner) { return owner.currentFixedReader(this); }
}

      public boolean tryReadMaterial(PagePublication page, int blockIndex, int stateId, MaterialSample out) {
         out.clear();
         if (!this.valid) {
            return false;
         } else {
            synchronized(this.owner) {
               return this.owner.currentFixedReader(this) && this.owner.tryReadMaterial(page, blockIndex, stateId, out);
            }
         }
      }

      public boolean tryRead(PagePublication page, int blockIndex, long minimumTopology, QueryPublication.MutableSample out) {
         out.clear();
         if (this.valid && page.topologyGeneration() >= minimumTopology) {
            {
               synchronized(this.owner) {
                  if (this.owner.currentFixedReader(this) && this.owner.readFixedCell(page, blockIndex)) {
                     out.set(this.owner.fixedSample.temperatureC(), this.owner.fixedSample.sampleTick());
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         } else {
            return false;
         }
      }

      public boolean freshCell(PagePublication page, int blockIndex, long minimumTopology, long gameTick, int maximumAge) {
         if (this.valid && page.topologyGeneration() >= minimumTopology) {
            {
               synchronized(this.owner) {
                  if (this.owner.currentFixedReader(this) && this.owner.readFixedCell(page, blockIndex)) {
                     long age = gameTick - this.owner.fixedSample.sampleTick();
                     return this.owner.fixedSample.sampleTick() >= 0L && age >= 0L && age <= (long)maximumAge;
                  } else {
                     return false;
                  }
               }
            }
         } else {
            return false;
         }
      }

      public boolean pageBeforeTopology(PagePublication page, long maximumTopologyExclusive) {
         if (!this.valid) {
            return false;
         } else {
            synchronized(this.owner) {
               return this.owner.currentFixedReader(this) && this.owner.fixedGeometry.containsKey(page) && page.topologyGeneration() < maximumTopologyExclusive;
            }
         }
      }

      public void clear() {
        var previous = owner;
        owner = null; valid = false; fixedVersion = 0;
        topologyGeneration = -1; sampleTick = -1; infraredEpoch = 0;
        pageChangeEpochs = null; brickChangeEpochs = null;
        if (previous != null) previous.releaseFixedReader();
}
   }

   public static record SectionPublication(ThermalPageHandle handle, PagePublication geometry) {
   }
}
