/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import static com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.TRACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.LocalAirShape;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialStateAccess;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.CutLoadBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirGeometryView;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FrozenAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirFieldCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirShapeStore;
import com.teammoeg.frostedheart.content.climate.thermal.topology.PreparedTopologyChange;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.Phase;

/**
 * Persistent numerical regions over the original captured topology. Regions reuse
 * the existing solver on the bounded worker pool and publish immutable regional
 * snapshots as advances finish. Structural candidates retain their parent fields
 * until installation or cancellation while independent regions keep advancing.
 */
public final class RegionalAirRuntime implements MaterialStateAccess {
    private final ThermalCellArena arena;
    private final WorkerPageStore pages;
    private final ThermalSolver geometry;
    private final ThermalTopologyParameters parameters;
    private final MaterialBoundaryRegistry materials;
    private final AirLoadTable airLoads;
    private final ThermalSignatureTable signatures;
    private final double toleranceC;
    private List<ThermalRegion> regions = List.of();
    private final Long2ObjectOpenHashMap<OwnerPage> ownerPages = new Long2ObjectOpenHashMap<>();
    private static final class OwnerPage {
        final long section;
        final Int2ObjectOpenHashMap<ThermalRegion> cells = new Int2ObjectOpenHashMap<>();
        final Long2ObjectOpenHashMap<ArrayList<ThermalRegion>> air = new Long2ObjectOpenHashMap<>();
        com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token;
        OwnerPage previous;
        OwnerPage(long section) { this.section = section; }
        OwnerPage visible() { return token == null || token.committed() ? this : previous; }
    }
    private ThermalRegion ownerAtSlot(int slot) {
        if (slot < 0 || !(arena.isLive(slot) || arena.isStagedCell(slot) || arena.isRetainedGeometryCell(slot))) return null;
        long section = SectionPos.asLong(arena.minimum(slot, 0) >> 4, arena.minimum(slot, 1) >> 4, arena.minimum(slot, 2) >> 4);
        var page = ownerPages.get(section); page = page == null ? null : page.visible();
        return page == null ? null : page.cells.get(slot);
    }
    private boolean hasOwner(int slot) { return ownerAtSlot(slot) != null; }

    private long nextRegionId = 1;
    private long regionalSolveSumNanos;
    private final QueryPublication queries;

    private final RegionalWorkQueue work = new RegionalWorkQueue(this);
    private final RegionalQualityControl quality;
    public RegionalQualityControl qualityControl() { return quality; }
    long regionWorkAllowance(ThermalRegion region) { return workAllowance(region.workWeight(), installedWorkWeight); }

    public void whenRoundReady(Runnable continuation) {
        work.whenReady(continuation);
    }

    private final SpatialHeatStore heatInputs = new SpatialHeatStore();
    private ThermalMemoryBudget inputBudget;
    private ThermalMemoryBudget.Reservation inputRetention, solverRetention;
    private ThermalSourceLedger sourceLedger;
    private long receivedThrough;
    private long commitRevision;
    private long routingRevision;
    public long routingRevision() { return routingRevision; }

    private volatile long independentCommits;
    private java.util.function.LongSupplier oldestUninstalledTick = () -> Long.MAX_VALUE;
    public void oldestUninstalledTick(java.util.function.LongSupplier supplier) { oldestUninstalledTick = supplier; }
    private volatile LongOpenHashSet independentRegions = new LongOpenHashSet();
    public long independentCommits() { return independentCommits; }
    public boolean regionCommittedIndependently(long id) { return independentRegions.contains(id); }
    private final Long2ObjectOpenHashMap<ThermalInputBatch.DormantAirCut> restorations = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap restorationConsumedBricks = new Long2LongOpenHashMap();
    private long installedWorkWeight;

    private long workAllowance(long weight, long totalWeight) {
        var pool = ThermalWorkerPool.shared();
        // The existing whole-pool envelope is shared by actual captured work,
        // independent of how many small material packets the partition creates.
        long available = pool.regionalWorkAllowanceNanos() / pool.dimensionCount();
        return Math.max(1_000_000L, (long) (Math.max(0, available)
                * ((double) weight / Math.max(weight, totalWeight))));
    }
    static long buildWorkLimit(long allowance) {
        // Construction may spend four ordinary active-time advancement allowances.
        return Math.max(1_000_000L, Math.multiplyExact(4, allowance));
    }

    public RegionalAirRuntime(ThermalCellArena arena, WorkerPageStore pages, ThermalSolver geometry,
            ThermalTopologyParameters parameters, MaterialBoundaryRegistry materials, AirLoadTable airLoads,
            ThermalSignatureTable signatures, double toleranceC, QueryPublication queries) {
        this.arena = arena;
        this.pages = pages;
        quality = new RegionalQualityControl(this, work, pages);
        this.geometry = geometry;
        this.parameters = parameters;
        this.materials = materials;
        this.airLoads = airLoads;
        this.signatures = signatures;
        this.toleranceC = toleranceC;
        this.queries = queries;
    }

    /** No candidate topology becomes visible until its complete query state is admitted. */
    public boolean publishGeometry(Rebuild rebuild, ThermalInputBatch input, long topology, Runnable installTopology) {
        if (rebuild == null) return queries.publishEnvironmentBoundary(topology, input.targetTick(), installTopology);
        var installation = rebuild.queryInstallation;
        if (installation == null || !installation.dependenciesCurrent()) return false;
        var membership = rebuild.prepareMembership();
        return queries.installRegions(installation, topology, input.targetTick(), frames -> {
            installTopology.run();
            installRebuild(rebuild, input, frames, membership);
        });
    }

    /** Prepared outside the publication monitor and consumed in this same coordinator call. */
    private record RegionMembership(List<ThermalRegion> regions, long[] ids,
            List<ThermalRegion> removed, List<ThermalRegion> ready, LongOpenHashSet independent) { }

    private final java.util.LinkedHashSet<ThermalRegion> materialControlRegions = new java.util.LinkedHashSet<>();

    public void materialControlChanged(int slot) {
        var region = ownerAtSlot(slot);
        if (region != null) {
            region.changedMaterials.add(slot);
            materialControlRegions.add(region);
            if (region.job != null) work.cancel(region.job);
        }
    }

    @Override
    public void readMaterial(int slot, MaterialSample target) {
        ThermalRegion region = arena.isLive(slot) ? ownerAtSlot(slot) : null;
        if (region != null && region.accepted().field() != null) {
            if (region.materialControls != null && region.materialControls.readMaterial(slot,
                    arena.lifecycleGeneration(slot), arena.materialIncarnation(slot), target)) return;
            if (region.accepted().field().readMaterial(slot, arena.lifecycleGeneration(slot), arena.materialIncarnation(slot), target)) return;
        }
        // New/staged bodies have no accepted owner yet; compilation initializes them.
        arena.readMaterial(slot, target);
    }

    @Override
    public void applyMaterialControl(int slot, double enthalpyJ, byte branch) {
        ThermalRegion region = ownerAtSlot(slot);
        if (region == null) { arena.applyMaterialControl(slot, enthalpyJ, branch); return; }
        var field = region.materialControls == null ? region.accepted().field() : region.materialControls;
        boolean phase = arena.hasMaterialTransition(slot);
        region.materialControls = field.withMaterialControl(slot, enthalpyJ, branch,
                phase && arena.materialTransitionWaiting(slot), phase ? arena.phaseRequestSequence(slot) : 0);
        materialControlChanged(slot);
    }

    public boolean commitMaterialControls(RegionalContinuationBoundary boundary) {
        boolean complete = true;
        for (var region : java.util.List.copyOf(materialControlRegions))
            if (boundaryTouches(region, boundary)) complete &= importMaterialControls(region);
        return complete;
    }

    private boolean importMaterialControls(ThermalRegion region) {
        if (region.changedMaterials.isEmpty() && region.materialControls == null || region.accepted().field() == null) return true;
        if (region.snapshotSeals != 0) return false;
        if (region.job != null) { work.cancel(region.job); return false; }
        var field = region.materialControls == null ? region.accepted().field() : region.materialControls;
        var sample = new MaterialSample();
        int remaining = 32;
        var changed = region.changedMaterials.iterator();
        while (remaining-- > 0 && changed.hasNext()) {
            int slot = changed.nextInt(); changed.remove();
            if (!arena.isMaterialCell(slot)) continue;
            if (field.readMaterial(slot, arena.lifecycleGeneration(slot), arena.materialIncarnation(slot), sample)) {
                boolean phase = arena.hasMaterialTransition(slot);
                field = field.withMaterialControl(slot, sample.enthalpyJ(), sample.branch(),
                        phase && arena.materialTransitionWaiting(slot), phase ? arena.phaseRequestSequence(slot) : 0);
            }
        }
        region.materialControls = field;
        if (!region.changedMaterials.isEmpty()) return false;
        var snapshot = field;
        Runnable accept = () -> region.accept(snapshot, region.sampleTick(), region.consumedSequence());
        if (!queries.publishRegion(snapshot, accept)) return false;
        if (!snapshot.minimumQuality()) region.materialRestoration = region.solver.prepareAcceptedRestoration();
        region.materialControls = null;
        materialControlRegions.remove(region);
        work.offer(region);
        return true;
    }

    public void acceptAdmissions(ThermalInputBatch batch) {
        for (var retirement : batch.retirements()) {
            restorations.remove(retirement.page().sectionKey());
            restorationConsumedBricks.remove(retirement.page().sectionKey());
        }
        for (var admission : batch.admissions()) if (admission.dormantAir() != null) {
            restorations.put(admission.page().sectionKey(), admission.dormantAir());
            restorationConsumedBricks.remove(admission.page().sectionKey());
        }
        for (var residency : batch.residencyUpdates()) if (residency.dormantAir() != null) {
            restorations.put(residency.page().sectionKey(), residency.dormantAir());
            restorationConsumedBricks.remove(residency.page().sectionKey());
        }
        for (ThermalRegion region : regions) region.solver.acceptAdmissions(batch);
    }

    int automaticStep(ThermalRegion region, long tick, long allowance) {
        if (region.solver.gridStep() == 1 && !region.workCoarseningUseful()) return 1;
        // An independent region may already have accepted later input. A quality
        // replacement must wait for that accepted physical time, never relabel it.
        return region.workPolicy == null || region.sampleTick() > tick ? region.solver.gridStep()
                : region.workPolicy.select(region.solver.gridStep(), tick, buildWorkLimit(allowance));
    }
    private Runnable backingReleased = () -> { };
    public void onBackingReleased(Runnable notification) { backingReleased = notification; }

    private boolean acceptMinimumLayout(RegionJob job) {
        var region = job.region;
        if (region.snapshotPauses != 0) return false;
        if (!region.changedMaterials.isEmpty() || region.materialControls != null) return false;
        var current = region.accepted().field();
        if (current == null) return false;
        var replacement = job.publication;
        if (!replacement.minimumQuality()) return false;
        if (!job.transportAdmitted && solverRetention != null) {
            job.transportBytes = replacement.minimumState().transportBackingBytes();
            if (job.prepaidWorkspace) {
                long required = Math.max(job.reservation.bytes(), job.workspaceFloor + job.transportBytes);
                job.reservation.completeResize(required);
            }
            job.transportAdmitted = true;
        }
        Runnable accept = () -> {
            if (solverRetention != null) job.reservation.transferTo(solverRetention, job.transportBytes);
            if (ThermalDiagnostics.observer != null) ThermalDiagnostics.observer.qualityPublished(region, current, replacement);
            region.accept(replacement, region.sampleTick(), region.consumedSequence());
            region.materialRestoration = null;
            region.solver.releaseDetailedWorkspaces();
            if (region.solver.gridStep() != 4) installedCoarseRegionCount++;
            region.solver.gridStep(4);
            if (region.workPolicy != null) region.workPolicy.installed(4, region.sampleTick());
            noteAcceptedTick(region); commitRevision++;
            refreshContactDescription();
        };
        if (!queries.publishRegion(replacement, accept)) return false;
        ThermalWorkBudget.recordWork(region.sampleTick(), region.id, region.epoch, "minimum_layout", 4,
                "accepted", job.account == null ? 0 : job.account.elapsedNanos(), job.account);
        job.release(); region.job = null;
        backingReleased.run();
        return true;
    }

    // Retain active builds until their tasks and parent-frame leases are released.
    private final Set<Rebuild> builds = Collections.newSetFromMap(new IdentityHashMap<>());

    public RegionalContinuationBoundary expandBoundary(RegionalContinuationBoundary boundary) {
        var sections = new LongOpenHashSet(boundary.sections());
        boolean changed;
        do {
            changed = false;
            for (var region : regions) {
                if (!sectionsTouch(region, sections)) continue;
                changed |= sections.addAll(region.scope.sections);
                if (region.dependencySections != null) changed |= sections.addAll(region.dependencySections);
            }
        } while (changed);
        return boundary.including(sections);
    }

    /** Local partition/membership metadata; numerical state and graphs are admitted separately. */
    public long preparationMetadataBytes(FrozenAirGeometry candidate, PreparedTopologyChange change,
            RegionalContinuationBoundary boundary, int ports) {
        long parents = 0, fragments = change.fragmentIndexes().length;
        for (var region : regions) if (boundaryTouches(region, boundary)) {
            parents++; fragments += region.scope.fragmentIndexes.length;
        }
        if (regions.isEmpty()) for (int index = geometry.nextFragmentIndex(0); index >= 0; index = geometry.nextFragmentIndex(index + 1))
            fragments++;
        // LocalParents, partition maps/lists, and Scope membership columns share
        // this per-cell bound. Fragment directories and parent/port references
        // have independent bounds. There is no arena-sized partition array.
        return 32768L + 256L * candidate.capturedCellCount() + 128L * fragments + 256L * parents + 128L * ports;
    }

    /** Captured parent arrays remain real query readers until the last construction helper exits. */
    private final class ParentFrames implements AutoCloseable {
        final ThermalMemoryBudget.Reservation credit;
        final IdentityHashMap<RegionalQuerySnapshot, QueryPublication.FrameLease> held = new IdentityHashMap<>();
        ParentFrames(ThermalMemoryBudget.Reservation credit) { this.credit = credit; }
        void hold(Iterable<ThermalRegion> parents) {
            for (var parent : parents) {
                var field = parent.accepted().field();
                if (field == null || held.containsKey(field)) continue;
                var lease = queries.borrowFrame(field, credit);
                if (lease == null) throw new ContinuousAirSolver.PreparationLimitedException(256);
                held.put(field, lease);
            }
        }
        @Override public void close() { for (var lease : held.values()) lease.close(); held.clear(); }
    }

    public final class Rebuild implements AutoCloseable {
        final FrozenSources bindings;
        long targetTick;
        boolean recovery, projectionResumed;
        int continuityRegion, continuityComponent;
        long continuityBlocks;
        double continuityErrorC;
        final ArrayList<ThermalRegion> nextRegions, prepared;
        final ArrayList<RegionBuild> tasks;
        final List<ThermalRegion> parents;
        final ParentFrames parentFrames;
        final ThermalMemoryBudget.Reservation graphCredit;
        final PreparedTopologyChange change;
        final AirGeometryView inputGeometry;
        Pending result;
        QueryPublication.RegionInstallation queryInstallation;
        OwnershipPreparation ownership;
        final ArrayList<WorkerPageStore.SupportChange> supportChanges = new ArrayList<>();
        int supportPrepared, supportApplied, supportReleased;
        boolean installed, closed;
        Rebuild(long targetTick,
                ArrayList<ThermalRegion> nextRegions, ArrayList<ThermalRegion> prepared,
                FrozenSources bindings, PreparedTopologyChange change, List<ThermalRegion> parents,
                AirGeometryView view, ParentFrames parentFrames,
                ThermalMemoryBudget.Reservation graphCredit) {
            this.targetTick = targetTick;
            this.bindings = bindings;
            this.nextRegions = nextRegions; this.prepared = prepared;
            this.parents = parents; this.change = change; inputGeometry = view;
            this.parentFrames = parentFrames;
            this.graphCredit = graphCredit;
            tasks = new ArrayList<>(prepared.size());
        }
        boolean initializeJobs() {
            int end = Math.min(prepared.size(), tasks.size() + 16);
            for (int index = tasks.size(); index < end; index++) {
                var region = prepared.get(index);
                parentFrames.hold(region.parents);
                var job = new RegionBuild(region, bindings, change, graphCredit);
                tasks.add(job); work.offerBuild(job);
            }
            return tasks.size() == prepared.size();
        }
        public boolean ready() {
            if (tasks.size() < prepared.size()) return true;
            for (var job : tasks) if (job.task != null || !job.ready
                    && !(recovery && job.region.solver.candidateProjectionHeld())) return false;
            return true;
        }
        public boolean staticRecoveryReady() { return recovery && !projectionResumed && tasks.size() == prepared.size() && ready(); }
        public boolean recoveryAborted() {
            if (!recovery) return false;
            for (var region : prepared) if (!region.scope.air.isEmpty() && region.solver.gridStep() != 1) return true;
            return false;
        }
        public boolean resumeRecovery(long tick) {
            // A quality candidate retains one complete physical owner. A changed
            // membership cancels this optional attempt while the coarse owner continues.
            for (var region : prepared) if (region.parents.size() != 1
                    || !region.scope.sameGeometryInputs(region.parents.get(0).scope)) return false;
            for (var region : prepared) parentFrames.hold(region.parents);
            targetTick = tick; projectionResumed = true;
            for (var region : prepared) {
                var parent = region.parents.get(0);
                ThermalRegion air = region.scope.air.isEmpty() ? null : parent;
                var material = new RegionalQuerySnapshot[] {parent.accepted().field()};
                region.publication.publish(null, tick, parent.consumedSequence());
                for (var build : tasks) if (build.region == region) {
                    build.resumeProjection(air == null ? null : air.solver.layout(),
                            air == null ? new double[0] : air.accepted().field().borrowedAirCoefficients(), material);
                    work.offerBuild(build);
                }
            }
            return true;
        }
        public boolean validateRecovery(long deadline) {
            if (!recovery) return true;
            do {
                if (continuityRegion == prepared.size()) return true;
                var region = prepared.get(continuityRegion);
                var layout = region.solver.layout();
                if (continuityComponent == layout.componentCount()) {
                    continuityRegion++; continuityComponent = 0; continuityBlocks = 0; continue;
                }
                var component = layout.component(continuityComponent);
                if (continuityBlocks == 0) continuityBlocks = component.airBlocks;
                int block = Long.numberOfTrailingZeros(continuityBlocks); continuityBlocks &= continuityBlocks - 1;
                int x = component.minX + (block & 3), y = component.minY + (block >>> 4), z = component.minZ + (block >>> 2 & 3);
                ThermalRegion parent = null;
                for (var previous : region.parents) if (previous.scope.containsAirAt(BlockPos.asLong(x, y, z))) { parent = previous; break; }
                if (parent != null) {
                    var before = parent.accepted().field();
                    double[] after = region.accepted().field().borrowedAirCoefficients();
                    for (int corner = 0; corner < 8; corner++) {
                        double px = Math.nextAfter(x + (corner & 1), x + .5);
                        double py = Math.nextAfter(y + (corner >>> 2), y + .5);
                        double pz = Math.nextAfter(z + (corner >>> 1 & 1), z + .5);
                        double delta = Math.abs(component.temperatureOffsetAtAirCell(block, px, py, pz, after)
                                + parameters.referenceTemperatureC() - before.airTemperatureC(px, py, pz));
                        continuityErrorC = Math.max(continuityErrorC, delta);
                    }
                }
                if (continuityBlocks == 0) continuityComponent++;
            } while (System.nanoTime() < deadline);
            return false;
        }
        public double recoveryErrorC() { return continuityErrorC; }
        public boolean helpersReturned() {
            for (var job : tasks) if (job.task != null) return false;
            return true;
        }
        public boolean advance() { return result != null || advanceRebuild(this); }
        public boolean parentsCurrent() {
            for (var parent : parents) if (regionsById.get(parent.id) != parent) return false;
            return true;
        }
        public boolean prepareQueryInstallation(QueryPublication publication, long deadline) {
            if (queryInstallation == null || !queryInstallation.dependenciesCurrent()) {
                if (queryInstallation != null) queryInstallation.close();
                var removed = new ArrayList<RegionalQuerySnapshot>(parents.size());
                for (var parent : parents) removed.add(parent.accepted().field());
                var added = new ArrayList<RegionalQuerySnapshot>(result.regions.size());
                for (var region : result.regions) added.add(region.accepted().field());
                queryInstallation = publication.prepareRegionInstallation(removed, added);
            }
            return queryInstallation != null && queryInstallation.run(deadline);
        }
        private RegionMembership prepareMembership() {
            var parentSet = new ReferenceOpenHashSet<>(parents);
            var retained = new ReferenceOpenHashSet<>(result.regions);
            var merged = new ArrayList<ThermalRegion>(regions.size() + result.regions.size());
            for (var region : regions) if (!parentSet.contains(region)) merged.add(region);
            merged.addAll(result.regions);
            var removed = new ArrayList<ThermalRegion>();
            for (var parent : parents) if (!retained.contains(parent)) removed.add(parent);
            long[] ids = new long[merged.size()];
            var independent = new LongOpenHashSet();
            for (int index = 0; index < merged.size(); index++) {
                long id = ids[index] = merged.get(index).id;
                if (independentRegions.contains(id)) independent.add(id);
            }
            var ready = new ArrayList<>(prepared);
            ready.sort(ADVANCEMENT_ORDER);
            return new RegionMembership(List.copyOf(merged), ids, removed, ready, independent);
        }
        public boolean prepareOwnership(com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token, long deadline) {
            if (ownership == null) ownership = new OwnershipPreparation(this, token);
            return ownership.run(deadline);
        }
        public boolean cleanupOwnership(long deadline) {
            if (ownership == null) return true;
            return ownership.finish(deadline);
        }
        public boolean prepareSupportChanges(long deadline) {
            do {
                int index = supportPrepared++;
                if (index < prepared.size()) {
                    var region = prepared.get(index);
                    var requests = new Long2LongOpenHashMap(); region.solver.shapeStore().appendSupportRequests(requests);
                    supportChanges.add(pages.prepareAirSupport(region.id, requests));
                } else if (index < prepared.size() + parents.size()) {
                    var parent = parents.get(index - prepared.size());
                    boolean retained = false;
                    for (var region : result.regions) if (region.id == parent.id) { retained = true; break; }
                    if (!retained) supportChanges.add(pages.prepareAirSupport(parent.id, new Long2LongOpenHashMap()));
                } else return true;
            } while (System.nanoTime() < deadline);
            return false;
        }
        public boolean applySupportChanges(long deadline) {
            while (supportApplied < supportChanges.size()) {
                if (!supportChanges.get(supportApplied).run(deadline)) return false;
                supportApplied++;
                if (System.nanoTime() >= deadline) return false;
            }
            return true;
        }
        public boolean releaseSupportChanges(long deadline) {
            while (supportReleased < supportChanges.size()) {
                supportChanges.get(supportReleased++).close();
                if (System.nanoTime() >= deadline) return false;
            }
            return true;
        }
        /** Prepay parent-history overlap and new pending owners before installation. */
        public void prepareInputHandoff() {
            long parentBytes = 0;
            for (var parent : parents) parentBytes += parent.pending.retainedBytes();
            long required = retainedInputBytes() + 2 * parentBytes + 1024L * result.prepared.size();
            inputRetention.completeResize(Math.max(required, inputRetention.bytes()));
        }
        public boolean tryClose() {
            if (closed) return true;
            if (!installed && !work.cancelBuilds(tasks)) return false;
            closed = true;
            if (queryInstallation != null) { queryInstallation.close(); queryInstallation = null; }
            builds.remove(this);
            parentFrames.close();
            if (inputGeometry instanceof FrozenAirGeometry frozen) frozen.close();
            return true;
        }
        @Override public void close() {
            tryClose();
        }
    }

    private boolean advanceRebuild(Rebuild round) {
        long targetTick = round.targetTick;

        try {
            if (!round.initializeJobs()) return false;
            if (!round.ready()) return false;
            for (var job : round.tasks) if (job.failure != null) {
                if (job.failure instanceof RuntimeException failure) throw failure;
                if (job.failure instanceof Error failure) throw failure;
                throw new CompletionException(job.failure);
            }
            for (var job : round.tasks) if (job.publication != null && job.region.accepted().field() == null)
                job.region.accept(job.publication, job.region.sampleTick(), job.region.consumedSequence());
            if (TRACE) FHMain.LOGGER.info("THERMAL_REGION_PREPARATION_WINDOW,stage=end,targetTick={},nanos={}",
                    targetTick, System.nanoTime());
            var nextRegions = round.nextRegions;
            var prepared = round.prepared;
            // Build this index before the complete candidate backing is measured
            // and reserved; installation itself only transfers its ownership.
            for (var region : prepared) indexDependencies(region);
            round.result = new Pending(nextRegions, prepared);
            if (TRACE) for (ThermalRegion region : prepared) {
                FHMain.LOGGER.info("THERMAL_REGION_PREPARE,targetTick={},regionId={},epoch={},minX={},maxX={},cells={},airCoefficients={},staticBuilds={},projections={},scopeSame={},localAddressesSame={}",
                        targetTick, region.id, region.epoch, region.scope.minimumX, region.scope.maximumX, region.scope.cells.length,
                        region.solver.layout().coefficientCount(), 1, 1, false, false);
                FHMain.LOGGER.info("THERMAL_REGION_BUILD,targetTick={},regionId={},{}", targetTick, region.id, region.solver.lastRebuildTiming());
            }
            return true;
        } catch (RuntimeException | Error failure) {
            round.close();
            throw failure;
        }

    }

    public RebuildPreparation prepareLocalRebuild(FrozenSources bindings, PreparedTopologyChange change,
            AirGeometryView view, long tick, ThermalMemoryBudget.Reservation scratch, RegionalContinuationBoundary boundary) {
        return new RebuildPreparation(bindings, change, view, tick, scratch, boundary);
    }
    public RebuildPreparation prepareLocalQuality(FrozenSources bindings, PreparedTopologyChange change,
            AirGeometryView view, long tick, ThermalMemoryBudget.Reservation scratch, RegionalContinuationBoundary boundary,
            boolean recovering) {
        var preparation = new RebuildPreparation(bindings, change, view, tick, scratch, boundary);
        preparation.qualityOnly = true; preparation.continuingParents = recovering;
        return preparation;
    }

    /** One local build owner prepares its partition and children before any assembly helper is offered. */
    public final class RebuildPreparation implements AutoCloseable {
        final FrozenSources bindings;
        final PreparedTopologyChange change;
        final AirGeometryView view;
        final long targetTick;
        final ThermalMemoryBudget.Reservation scratch;
        final ParentFrames parentFrames;
        final RegionalContinuationBoundary rebuildBoundary;
        final long[] sourceRegionIds = installedRegionIds;
        final ArrayList<ThermalRegion> previousRegions = new ArrayList<>(), nextRegions = new ArrayList<>(), prepared = new ArrayList<>();
        final Int2ObjectOpenHashMap<ThermalFragment> nextFragments = new Int2ObjectOpenHashMap<>();
        final Set<ThermalRegion> retained = Collections.newSetFromMap(new IdentityHashMap<>());
        final java.util.BitSet readingCells = new java.util.BitSet();
        int phase, cursor, parentCursor, fragmentCursor, scopeCursor, cellCursor, graphCursor;
        long graphBytes, largestFineBytes, selectionWeight;
        ThermalRegion largestFine;
        Set<ThermalRegion> previous;
        PartitionPreparation partition;
        List<Scope> scopes;
        boolean automatic, closed, continuingParents, qualityOnly;
        Rebuild result;

        RebuildPreparation(FrozenSources bindings, PreparedTopologyChange change, AirGeometryView view,
                long tick, ThermalMemoryBudget.Reservation scratch, RegionalContinuationBoundary boundary) {
            this.bindings = bindings; this.change = change; this.view = view; targetTick = tick;
            this.scratch = scratch; rebuildBoundary = boundary;
            parentFrames = new ParentFrames(scratch);
        }
        public Rebuild result() {
            if (result == null) throw new IllegalStateException("Region preparation is unfinished");
            return result;
        }
        public boolean run(long deadline) {
            if (closed) throw new IllegalStateException("Region preparation has retired");
            do {
                switch (phase) {
                    case 0 -> {
                        int end = Math.min(sourceRegionIds.length, cursor + 16);
                        for (; cursor < end; cursor++) {
                            var region = regionsById.get(sourceRegionIds[cursor]);
                            if (region != null && boundaryTouches(region, rebuildBoundary)) previousRegions.add(region);
                        }
                        if (cursor == sourceRegionIds.length) { phase++; cursor = 0; }
                    }
                    case 1 -> {
                        int remaining = 512;
                        while (remaining-- > 0 && parentCursor < previousRegions.size()) {
                            var parent = previousRegions.get(parentCursor);
                            if (fragmentCursor == parent.scope.fragmentIndexes.length) { parentCursor++; fragmentCursor = 0; continue; }
                            int index = parent.scope.fragmentIndexes[fragmentCursor++];
                            nextFragments.put(index, geometry.fragment(index));
                        }
                        if (parentCursor == previousRegions.size()) { phase++; cursor = 0; }
                    }
                    case 2 -> {
                        if (sourceRegionIds.length == 0) {
                            int remaining = 256;
                            if (cursor == 0) fragmentCursor = geometry.nextFragmentIndex(0);
                            while (remaining-- > 0 && fragmentCursor >= 0) {
                                nextFragments.put(fragmentCursor, geometry.fragment(fragmentCursor));
                                fragmentCursor = geometry.nextFragmentIndex(fragmentCursor + 1); cursor++;
                            }
                            if (fragmentCursor >= 0) break;
                        }
                        phase++; cursor = 0;
                    }
                    case 3 -> {
                        int end = Math.min(change.fragmentIndexes().length, cursor + 256);
                        for (; cursor < end; cursor++) nextFragments.put(change.fragmentIndexes()[cursor], change.fragments()[cursor]);
                        if (cursor == change.fragmentIndexes().length) {
                            if (qualityOnly) {
                                // Quality changes keep every physical owner intact. Repartitioning
                                // a material fringe here could retire its parent's remaining Air.
                                scopes = new ArrayList<>(previousRegions.size());
                                for (var parent : previousRegions) scopes.add(new Scope(parent.scope, view));
                            } else partition = new PartitionPreparation(view, nextFragments, bindings, change);
                            phase++; cursor = 0;
                        }
                    }
                    case 4 -> {
                        if (partition != null) {
                            if (!partition.run(deadline)) return false;
                            scopes = partition.scopes;
                        }
                        automatic = (ThermalWorkBudget.automatic());
                        selectionWeight = installedWorkWeight;
                        for (var parent : previousRegions) selectionWeight -= parent.workWeight();
                        for (var scope : scopes) selectionWeight += Math.max(1, scope.fineVolumes + scope.cells.length - scope.airComponents);
                        partition = null; phase++;
                    }
                    case 5 -> {
                        if (scopeCursor == scopes.size()) { phase++; break; }
                        Scope scope = scopes.get(scopeCursor);
                        if (previous == null) previous = Collections.newSetFromMap(new IdentityHashMap<>());
                        int end = Math.min(scope.cells.length, cellCursor + 128);
                        for (; cellCursor < end; cellCursor++) {
                            int cell = scope.cells[cellCursor];
                            var owner = ownerAtSlot(cell); if (owner != null) previous.add(owner);
                            if (!arena.isAirCell(cell)) {
                                int oldSlot = pages.materialSlot(BlockPos.asLong(arena.minimum(cell, 0), arena.minimum(cell, 1), arena.minimum(cell, 2)),
                                        arena.materialProfileId(cell));
                                var materialOwner = ownerAtSlot(oldSlot); if (materialOwner != null) previous.add(materialOwner);
                            } else {
                                long blocks = arena.airBlockMask(cell);
                                while (blocks != 0) {
                                    int block = Long.numberOfTrailingZeros(blocks); blocks &= blocks - 1;
                                    var physicalOwner = airOwnerAt(arena.minimum(cell, 0) + (block & 3), arena.minimum(cell, 1) + (block >>> 4),
                                            arena.minimum(cell, 2) + (block >>> 2 & 3));
                                    if (physicalOwner != null) previous.add(physicalOwner);
                                }
                            }
                        }
                        if (cellCursor == scope.cells.length) {
                            prepareScope(scope, previous); previous = null; cellCursor = 0; scopeCursor++;
                        }
                    }
                    case 6 -> {
                        int end = Math.min(prepared.size(), graphCursor + 128);
                        for (; graphCursor < end; graphCursor++) {
                            var region = prepared.get(graphCursor);
                            long bytes = region.solver.preparationStateBytes();
                            graphBytes = Math.addExact(graphBytes, bytes);
                            if (region.solver.gridStep() == 1 && bytes > largestFineBytes) { largestFineBytes = bytes; largestFine = region; }
                        }
                        if (graphCursor != prepared.size()) break;
                        try { ContinuousAirSolver.reservePreparationGraph(scratch, graphBytes); phase++; cursor = parentCursor = 0; }
                        catch (ContinuousAirSolver.PreparationLimitedException refused) {
                            if (!automatic || largestFine == null) throw refused;
                            quality.prepareCandidateModel(largestFine);
                            graphCursor = 0; graphBytes = largestFineBytes = 0; largestFine = null;
                            return false;
                        }
                    }
                    case 7 -> {
                        int remaining = 512;
                        while (remaining-- > 0 && parentCursor < prepared.size()) {
                            var region = prepared.get(parentCursor);
                            if (cursor == region.scope.cells.length) { cursor = 0; parentCursor++; continue; }
                            readingCells.set(region.scope.cells[cursor++]);
                        }
                        if (parentCursor == prepared.size()) {
                            if (view instanceof FrozenAirGeometry frozen) frozen.retainCells(readingCells);
                            phase++;
                        }
                    }
                    default -> {
                        result = new Rebuild(targetTick, nextRegions, prepared,
                                bindings, change, previousRegions, view, parentFrames, scratch);
                        result.recovery = continuingParents;
                        builds.add(result); return true;
                    }
                }
            } while (System.nanoTime() < deadline);
            return false;
        }

        private void prepareScope(Scope scope, Set<ThermalRegion> previous) {
            parentFrames.hold(previous);
            // Newly captured material joins an Air region from the material batch.
            // It has no previous Air field and must not discard that Air region's
            // volume/contact reuse. Only merging distinct Air fields needs the
            // dimension publication as the projection source.
            ThermalRegion old = null;
            int airOwners = 0;
            for (ThermalRegion source : previous) if (source.solver.layout().coefficientCount() > 0) {
                old = source;
                airOwners++;
            }
            if (scope.air.isEmpty()) {
                old = null;
                for (ThermalRegion source : previous) if (source.solver.layout().coefficientCount() == 0) { old = source; break; }
            } else if (airOwners != 1) old = null;
            int gridStep = old == null ? 1 : old.solver.gridStep();
            if (old == null) for (ThermalRegion parent : previous)
                if (scope.air.isEmpty() == parent.scope.air.isEmpty()) gridStep = Math.max(gridStep, parent.solver.gridStep());
            int requestedStep = 0;
            for (ThermalRegion parent : previous) if (scope.air.isEmpty() == parent.scope.air.isEmpty()
                    && RegionalQualityControl.requestsChange(parent)) {
                if (parent.qualityIntent.target() == RegionalQualityControl.Model.MINIMUM) requestedStep = 4;
                else if (continuingParents && RegionalQualityControl.recovering(parent)) requestedStep = Math.max(requestedStep, 1);
            }
            // Structure keeps the committed model or coarsens it. Only the sole
            // quality recovery owner may build fine state while its parents advance.
            if (requestedStep != 0) gridStep = requestedStep;
            else if (automatic && old != null && gridStep == 1 && old.workPolicy != null)
                gridStep = automaticStep(old, targetTick, workAllowance(old.workWeight(), selectionWeight));
            boolean affected = rebuildBoundary == null || previous.isEmpty();
            if (rebuildBoundary != null) for (var parent : previous) affected |= boundaryTouches(parent, rebuildBoundary);
            boolean unchanged = old != null && gridStep == old.solver.gridStep()
                    && !retained.contains(old) && scope.sameGeometryInputs(old.scope)
                    && (!affected || scope.ports.equals(old.scope.ports))
                    && dependenciesCurrent(old.solver.layout(), view)
                    && (!affected || !old.solver.hasPendingSourceResponse() && !scope.mixingChanged(bindings));
            if (unchanged) {
                retained.add(old);
                nextRegions.add(old);
                return;
            }
            for (var parent : previous) if (!continuingParents && parent.sampleTick() != targetTick)
                throw new IllegalStateException("Regional handoff is not aligned: region=" + parent.id
                        + ",sample=" + parent.sampleTick() + ",boundary=" + targetTick);
            boolean exclusivePrevious = old != null && !retained.contains(old);
            long id = exclusivePrevious ? old.id : nextRegionId++;
            if (TRACE) FHMain.LOGGER.info("THERMAL_REGION_REBUILD_CAUSE,targetTick={},regionId={},oldRegionId={},parentCount={},airParentCount={},previousAlreadyClaimed={},cells={},oldCells={},oldAirCoefficients={},dependencyRevisionChanged={},sourceResponsePending={},mixingChanged={},{}",
                    targetTick, id, old == null ? -1 : old.id, previous.size(), airOwners,
                    old != null && !exclusivePrevious, scope.cells.length, old == null ? 0 : old.scope.cells.length,
                    old == null ? 0 : old.solver.layout().coefficientCount(),
                    old != null && !dependenciesCurrent(old.solver.layout(), view),
                    old != null && old.solver.hasPendingSourceResponse(), scope.mixingChanged(bindings),
                    scope.inputDifferenceSummary(old == null ? null : old.scope));
            if (old != null) retained.add(old);
            var solver = new ContinuousAirSolver(arena, pages, geometry, parameters, materials,
                    airLoads, signatures, toleranceC, scope, RegionalAirRuntime.this);
            solver.gridStep(gridStep);
            var inherited = new AirShapeStore(signatures);
            for (ThermalRegion source : previous) inherited.inherit(source.solver.shapeStore(), scope::containsAirAt, scope::containsShape);
            // Minimum construction projects from the retained parent snapshots.
            // It needs neither a merged fine field nor a parent's fine workspace.
            if (gridStep == 4) solver.seed(null, new double[0], inherited);
            else if (old != null) solver.seedFrom(old.solver, inherited, exclusivePrevious && !continuingParents);
            else if (!scope.air.isEmpty() && !previous.isEmpty()) {
                var parents = new ArrayList<ContinuousAirSolver>(previous.size());
                ThermalRegion donor = null;
                for (ThermalRegion source : previous) {
                    parents.add(source.solver);
                    if (!retained.contains(source) && (donor == null
                            || source.solver.layout().coefficientCount() > donor.solver.layout().coefficientCount()
                            || source.solver.layout().coefficientCount() == donor.solver.layout().coefficientCount() && source.id < donor.id))
                        donor = source;
                }
                // A merge changes ownership, not every local integral. The field
                // still comes from all parents; only mutable workspaces have one donor.
                if (donor != null) retained.add(donor);
                var parentLayouts = new ArrayList<AirFieldLayout>(parents.size());
                for (var parent : parents) parentLayouts.add(parent.layout());
                var parentField = AirFieldLayout.combine(parentLayouts);
                double[] parentC = new double[parentField.layout().coefficientCount()];
                for (int index = 0; index < parents.size(); index++) {
                    double[] values = parents.get(index).committedCoefficientsC();
                    int[] addresses = parentField.localToCombined()[index];
                    for (int row = 0; row < Math.min(addresses.length, values.length); row++) parentC[addresses[row]] = values[row];
                }
                solver.seedMerged(parentField.layout(), parentC, inherited, donor == null || continuingParents ? null : donor.solver);
                if (TRACE) FHMain.LOGGER.info("THERMAL_REGION_INHERIT,targetTick={},regionId={},previousAirRegions={},workspaceDonorId={}",
                        targetTick, id, airOwners, donor == null ? -1 : donor.id);
            }
            for (var entry : restorations.long2ObjectEntrySet())
                if (scope.containsSection(entry.getLongKey())) solver.restore(entry.getLongKey(), entry.getValue());
            ThermalRegion region = new ThermalRegion(id, old == null ? 1 : old.epoch + 1, scope, solver);
            region.buildWorkAllowance = workAllowance(region.workWeight(), selectionWeight);
            region.pending.retentionListener(delta -> {
                retainedPendingBytes += delta; markChunkProgress(region);
                boolean pending = region.pending.hasPending();
                if (pending != region.countedPending) { pendingRegionCount += pending ? 1 : -1; region.countedPending = pending; }
            });
            region.parents = List.copyOf(previous);
            if (automatic || old != null && old.workPolicy != null) {
                region.workPolicy = old != null && old.workPolicy != null
                        ? exclusivePrevious && !continuingParents ? old.workPolicy : new RegionWorkPolicy(old.workPolicy) : new RegionWorkPolicy();
                if (gridStep == 4 && (old == null || old.solver.gridStep() == 1)) region.workPolicy.coarse(targetTick, false);
                if (old != null && gridStep != old.solver.gridStep())
                    FHMain.LOGGER.info("THERMAL_REGION_POLICY,targetTick={},regionId={},event=preselect,fromStep={},toStep={},allowanceNs={},buildLimitNs={},{}",
                            targetTick, id, old.solver.gridStep(), gridStep, region.buildWorkAllowance,
                            buildWorkLimit(region.buildWorkAllowance), region.workPolicy.summary());
            }
            long minimumParentTick = Long.MAX_VALUE, maximumParentTick = -1, consumedSequence = 0;
            for (ThermalRegion parent : previous) {
                consumedSequence = Math.max(consumedSequence, parent.consumedSequence());
                minimumParentTick = Math.min(minimumParentTick, parent.sampleTick());
                maximumParentTick = Math.max(maximumParentTick, parent.sampleTick());
            }
            region.publication.publish(null, targetTick, consumedSequence);
            if (continuingParents) solver.holdCandidateProjection();
            if (previous.isEmpty()) minimumParentTick = maximumParentTick = targetTick;
            if (TRACE || minimumParentTick != targetTick || maximumParentTick != targetTick)
                FHMain.LOGGER.info("THERMAL_REGION_TRANSFER,targetTick={},regionId={},parentCount={},parentMinTick={},parentMaxTick={},parentTimeAligned={},completedSequence={}",
                    targetTick, id, previous.size(), minimumParentTick, maximumParentTick,
                    minimumParentTick == targetTick && maximumParentTick == targetTick, region.consumedSequence());
            prepared.add(region);
            nextRegions.add(region);
        }

        @Override public void close() {
            if (closed) return; closed = true;
            if (result != null) return; // The returned Rebuild has taken these owners.
            for (var region : prepared) {
                region.solver.abortPreparedRebuild(); region.pending.clear(); region.pending.retireRetention(); region.releaseMinimumWorkspace();
            }
            prepared.clear(); nextRegions.clear();
            parentFrames.close();
        }
    }

    private void installRebuild(Rebuild rebuild, ThermalInputBatch input, RegionalQuerySnapshot[] frames,
            RegionMembership membership) {
        Pending accepted = rebuild.result;
        if (ThermalDiagnostics.observer != null) ThermalDiagnostics.observer.installing(this, rebuild.parents, accepted.prepared);
        var previousRegions = rebuild.parents;
        work.installedBuilds(rebuild.tasks);
        // The publisher and solver adopt the same pre-admitted frame object.
        // Already-installed owners in the local result keep their existing numerical workspace.
        for (int index = 0; index < accepted.regions.size(); index++) {
            var region = accepted.regions.get(index);
            boolean prepared = !region.installed;
            if (prepared) {
                region.solver.installPreparedRebuild();
                region.scope.delegate = pages;
            }
            region.accept(frames[index], region.sampleTick(), region.consumedSequence());
            if (prepared && region.accepted().field().minimumQuality()) region.solver.releaseDetailedWorkspaces();
            if (prepared && region.workPolicy != null) {
                boolean changed = region.workPolicy.installed(region.solver.gridStep(), region.sampleTick());
                if (changed || TRACE) FHMain.LOGGER.info("THERMAL_REGION_POLICY,targetTick={},regionId={},event=installed,gridStep={},{}",
                        region.sampleTick(), region.id, region.solver.gridStep(), region.workPolicy.summary());
            }
        }
        for (var parent : previousRegions) quality.retired(parent);
        regions = membership.regions;
        for (var parent : membership.removed) unregisterRegion(parent);
        for (var region : accepted.prepared) registerRegion(region);
        installedRegionIds = membership.ids;
        independentRegions = membership.independent;
        handoffRegionalControls(previousRegions, accepted.prepared, input);
        work.replace(membership.removed, membership.ready);
        var restored = restorations.long2ObjectEntrySet().iterator();
        while (restored.hasNext()) {
            var entry = restored.next();
            long section = entry.getLongKey();
            boolean ready = true;
            for (ThermalRegion region : regions) if (region.scope.containsSection(section)
                    && !region.solver.shapeStore().canConsumeRestoration(section)) { ready = false; break; }
            if (!ready) continue;
            long consumed = restorationConsumedBricks.get(section) | pages.capturedBrickMask(section);
            restorationConsumedBricks.put(section, consumed);
            if ((entry.getValue().entry().brickMask() & ~consumed) == 0) {
                restored.remove(); restorationConsumedBricks.remove(section);
            }
        }
        rebuild.installed = true; rebuild.close();
        commitRevision++;
        if (ThermalDiagnostics.observer != null) ThermalDiagnostics.observer.installed(this);
    }

    private ThermalRegion airOwnerAt(int x, int y, int z) {
        var page = ownerPages.get(SectionPos.asLong(x >> 4, y >> 4, z >> 4)); page = page == null ? null : page.visible();
        var candidates = page == null ? null : page.air.get(BlockPos.asLong(x & ~3, y & ~3, z & ~3));
        if (candidates != null) for (var candidate : candidates)
            if (candidate.solver.layout().componentAt(x, y, z) != null) return candidate;
        return null;
    }

    /** Parents have reached the handoff tick. Remaining world inputs move exactly once. */
    /** Only environment/time metadata is handed off; heat stays in its world target stream. */
    private void handoffRegionalControls(List<ThermalRegion> previous, List<ThermalRegion> installed,
            ThermalInputBatch boundaryInput) {
        for (var region : installed) {
            initializeEnvironment(region);
            long through = region.sampleTick();
            for (var parent : region.parents) {
                through = Math.max(through, parent.pending.throughTick(parent.sampleTick()));
                region.environmentWindScale = parent.environmentWindScale;
                for (long section : region.scope.sections) {
                    var page = pages.pageAt(section);
                    if (page != null && parent.environmentNaturalC.containsKey(page.workerPageSlot()))
                        region.environmentNaturalC.put(page.workerPageSlot(), parent.environmentNaturalC.get(page.workerPageSlot()));
                }
                for (var input : parent.pending.snapshotInputs()) {
                    if (input.toTick <= region.sampleTick()) continue;
                    long begin = Math.max(region.sampleTick(), input.fromTick);
                    region.pending.receiveTransferred(new RegionPending.Input(input.sequence, begin, input.toTick,
                            input.environment, input.aggregated));
                }
            }
            if (boundaryInput != null && boundaryInput.hasFarFieldConductanceScale())
                region.environmentWindScale = boundaryInput.farFieldConductanceScale();
            long sealed = region.pending.throughTick(region.sampleTick());
            if (sealed < through) region.pending.receive(++spatialInputRevision, sealed, through,
                    RegionPending.Environment.UNCHANGED);
            region.parents = List.of();
        }
        for (var parent : previous) {
            heatInputs.remapOwner(parent.id, target -> {
                var next = sourceOwner(target); return next == null ? 0 : next.id;
            }, target -> {
                long block = target.position();
                var page = pages.pageAt(SectionPos.asLong(BlockPos.getX(block) >> 4, BlockPos.getY(block) >> 4, BlockPos.getZ(block) >> 4));
                return page != null ? target : new SpatialHeatInput.Target(block, target.face(), -1, 0, 0, 0, 0, 0,
                        target.materialStateId(), Math.max(target.minimumPageGeneration(), 1));
            });
            if (regionsById.get(parent.id) != parent) { parent.pending.clear(); parent.pending.retireRetention(); }
        }
        heatInputs.geometryChanged();
    }

    /** Order only newly installed owners; unrelated queued work keeps its place. */
    private static final java.util.Comparator<ThermalRegion> ADVANCEMENT_ORDER =
            java.util.Comparator.comparingInt((ThermalRegion region) -> region.scope.air.isEmpty() ? 2
                : region.solver.layout().isAggregated() ? 1 : 0)
                // The coordinator claims the first task itself. Prefer a small
                // ready field before an expensive inline task on a one-worker pool.
                .thenComparingLong(region -> (long) region.scope.cells.length + region.solver.layout().coefficientCount())
                .thenComparingLong(region -> region.id);

    public void attachIntake(ThermalMemoryBudget budget, ThermalMemoryBudget.Reservation solverBacking,
            ThermalSourceLedger ledger, long initialTick) {
        inputBudget = budget; solverRetention = solverBacking; sourceLedger = ledger; receivedThrough = initialTick;
        heatInputs.onPendingChanged(this::markChunkDirty);
        heatInputs.onClaimInvalidated(owner -> {
            var region = regionsById.get(owner); if (region != null && region.job != null) work.cancel(region.job);
        });
        inputRetention = budget.reserveCompletion(4096);
        pages.setRegionalAirField(slot -> {
            var owner = ownerAtSlot(slot); return owner == null ? null : owner.solver.layout().componentForSlot(slot);
        }, slot -> {
            var owner = ownerAtSlot(slot); return owner == null ? null : owner.accepted().field().borrowedAirCoefficients();
        });
        pages.setRegionalSnapshots(slot -> {
            var owner = ownerAtSlot(slot); return owner == null ? null : owner.accepted().field();
        }, queries);
    }

    private final java.util.TreeMap<Long, Integer> sampleTimes = new java.util.TreeMap<>();
    private int installedCoefficientCount, installedCoarseRegionCount;
    private String lastRegionalContact = "none";
    private String progressContact = "runtime=regional,regionCount=0,degradedRegions=0,airCoefficients=0";
    public String progressContactSummary() { return progressContact; }
    private void refreshContactDescription() {
        progressContact = "runtime=regional,regionCount=" + regions.size() + ",degradedRegions=" + installedCoarseRegionCount
                + ",airCoefficients=" + installedCoefficientCount + ",materialTrial=local,advancement=symmetric_pairwise," + lastRegionalContact;
    }
    private final Long2ObjectOpenHashMap<java.util.Set<ThermalRegion>> chunkOwners = new Long2ObjectOpenHashMap<>();
    private final it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet dirtyChunks = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
    private record ChunkProgress(long tick, long pendingTick, boolean retainedInput) { }
    private static final ChunkProgress PROGRESS_PENDING = new ChunkProgress(-1, -1, true);
    private final java.util.concurrent.ConcurrentHashMap<Long, ChunkProgress> publishedChunks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Long2ObjectOpenHashMap<java.util.Set<ThermalRegion>> sourceOwners = new Long2ObjectOpenHashMap<>();
    // Repeated boundary notifications must not move an already waiting region ahead of its peers.
    private final java.util.Set<ThermalRegion> sealReady = new java.util.LinkedHashSet<>();
    private final Long2ObjectOpenHashMap<ThermalRegion> regionsById = new Long2ObjectOpenHashMap<>();
    private long[] installedRegionIds = new long[0], heartbeatIds = new long[0];
    private int heartbeatCursor;
    private long heartbeatTick = -1;

    private void noteSourceOwner(ThermalRegion region, long sourceId, int generation) {
        if (generation <= 0) return;
        region.inputSources.put(sourceId, Math.max(generation, region.inputSources.get(sourceId)));
        sourceOwners.computeIfAbsent(sourceId, ignored -> java.util.Collections.newSetFromMap(new IdentityHashMap<>())).add(region);
        sealReady.add(region);
    }
    public void sourceBound(long sourceId, LongSet sections) {
        var owners = sourceOwners.get(sourceId);
        if (owners != null) sealReady.addAll(owners);
        else for (long section : sections) {
            var nearby = chunkOwners.get(chunkForSection(section));
            if (nearby != null) sealReady.addAll(nearby);
        }
    }

    public long sourceBindingLimitFor(long sourceId) {
        long through = Long.MAX_VALUE;
        var indexed = sourceOwners.get(sourceId);
        if (indexed != null) for (var region : indexed) {
            through = Math.min(through, region.executionLimit());
            for (long section : region.scope.sections) through = Math.min(through, geometryBoundary.applyAsLong(section));
            if (region.dependencySections != null) for (long section : region.dependencySections)
                through = Math.min(through, geometryBoundary.applyAsLong(section));
        }
        return through;
    }
    public long sourceBindingLimitAt(long block) {
        var owner = airOwnerAt(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
        if (owner == null) return Long.MAX_VALUE;
        long through = owner.executionLimit();
        for (long section : owner.scope.sections) through = Math.min(through, geometryBoundary.applyAsLong(section));
        return through;
    }
    public long recoveryHandoffTick(RegionalContinuationBoundary boundary) {
        long tick = 0;
        for (var region : regions) if (boundaryTouches(region, boundary)) {
            tick = Math.max(tick, region.sampleTick());
            if (region.job != null && !region.job.cancelled) tick = Math.max(tick, region.job.input.toTick);
        }
        return tick;
    }

    private static long chunkForSection(long section) {
        return net.minecraft.world.level.ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section));
    }
    private void markChunkProgress(ThermalRegion region) {
        for (long section : region.scope.sections) markChunkDirty(chunkForSection(section));
    }
    private void markChunkDirty(long chunk) {
        if (dirtyChunks.add(chunk)) publishedChunks.put(chunk, PROGRESS_PENDING);
    }
    private void sampleCount(long tick, int delta) {
        int count = sampleTimes.getOrDefault(tick, 0) + delta;
        if (count == 0) sampleTimes.remove(tick); else sampleTimes.put(tick, count);
    }
    private void noteAcceptedTick(ThermalRegion region) {
        if (region.installed) { sampleCount(region.indexedTick, -1); sampleCount(region.sampleTick(), 1); }
        region.indexedTick = region.sampleTick(); markChunkProgress(region);
    }
    private void indexDependencies(ThermalRegion region) {
        region.dependencySections = new LongOpenHashSet();
        var layout = region.solver.layout();
        for (int component = 0; component < layout.componentCount(); component++) {
            var air = layout.component(component);
            for (int dependency = 0; dependency < air.dependencyCount(); dependency++)
                region.dependencySections.add(air.dependency(dependency).sectionKey());
        }
    }
    private void registerRegion(ThermalRegion region) {
        if (region.installed) return;
        region.installed = true; region.indexedTick = region.sampleTick();
        installedWorkWeight += region.workWeight();
        regionsById.put(region.id, region);
        sampleCount(region.indexedTick, 1); installedCoefficientCount += region.solver.layout().coefficientCount();
        sealReady.add(region);
        if (region.solver.gridStep() == 4) installedCoarseRegionCount++;
        for (long section : region.scope.sections) {
            long chunk = chunkForSection(section);
            chunkOwners.computeIfAbsent(chunk, ignored -> java.util.Collections.newSetFromMap(new IdentityHashMap<>())).add(region);
            markChunkDirty(chunk);
        }
        refreshContactDescription();
    }
    private void unregisterRegion(ThermalRegion region) {
        if (region.minimumInputs != null) { retainedMappingBytes -= region.minimumInputs.retainedBytes(); region.minimumInputs = null; }
        region.releaseMinimumWorkspace(); materialControlRegions.remove(region);
        region.materialControls = null; region.materialRestoration = null; region.changedMaterials.clear();
        if (!region.installed) return;
        installedWorkWeight -= region.workWeight();
        region.installed = false; sampleCount(region.indexedTick, -1);
        if (regionsById.get(region.id) == region) regionsById.remove(region.id);
        installedCoefficientCount -= region.solver.layout().coefficientCount();
        if (region.solver.gridStep() == 4) installedCoarseRegionCount--;
        quality.retired(region);
        sealReady.remove(region);
        for (var target : region.routingTargets) routingOwners.remove(target, region);
        region.routingTargets.clear();
        refreshContactDescription();
        for (long source : region.inputSources.keySet()) {
            var owners = sourceOwners.get(source);
            if (owners != null) { owners.remove(region); if (owners.isEmpty()) sourceOwners.remove(source); }
        }
        for (long section : region.scope.sections) {
            long chunk = chunkForSection(section);
            var owners = chunkOwners.get(chunk);
            if (owners != null) { owners.remove(region); if (owners.isEmpty()) chunkOwners.remove(chunk); }
            markChunkDirty(chunk);
        }
    }
    public boolean hasProgressWork() {
        if (!dirtyChunks.isEmpty() || heatInputs.hasCleanup()) return true;
        for (var region : materialControlRegions) if (materialControlReady(region)) return true;
        return false;
    }
    private boolean materialControlReady(ThermalRegion region) {
        return region.job == null && region.snapshotSeals == 0;
    }
    public boolean hasSealWork() { return !sealReady.isEmpty() || heartbeatCursor < heartbeatIds.length; }
    public void publishChunkProgress(int maximum) {
        heatInputs.advanceCleanup(maximum);
        if (inputRetention != null) inputRetention.tryResize(Math.max(4096L, retainedInputBytes()));
        for (var region : java.util.List.copyOf(materialControlRegions)) if (materialControlReady(region)) {
            importMaterialControls(region); break;
        }
        while (maximum-- > 0 && !dirtyChunks.isEmpty()) {
            long chunk = dirtyChunks.removeFirstLong(), tick = Long.MAX_VALUE,
                    pending = heatInputs.firstPendingInChunk(chunk);
            var owners = chunkOwners.get(chunk);
            if (owners != null) for (var region : owners) {
                tick = Math.min(tick, region.sampleTick());
                pending = Math.min(pending, Math.min(region.pending.firstPendingTick(), region.blockedAfterTick));
                if (region.routingPending) pending = Math.min(pending, region.sampleTick());
            }
            if (owners == null && pending == Long.MAX_VALUE) publishedChunks.remove(chunk);
            else publishedChunks.put(chunk, new ChunkProgress(tick, pending,
                    heatInputs.targetsInChunk(chunk) != 0));
        }
    }
    public int busyChunkCount() { return publishedChunks.size(); }
    public boolean hasRetainedInputInChunk(long chunk) {
        var progress = publishedChunks.get(chunk);
        return progress != null && progress.retainedInput;
    }
    private long spatialInputRevision;
    private long retainedPendingBytes;
    private long retainedMappingBytes;
    private int pendingRegionCount;
    public boolean hasPendingInputs() { return pendingRegionCount != 0 || !materialControlRegions.isEmpty()
            || heatInputs.targetCount() != 0; }
    public SpatialHeatStore inputStore() { return heatInputs; }
    public void prepareSourceWindow(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Window window,
            SpatialHeatInput.Target[] origins) {
        int routes = 0;
        for (var origin : origins) if (origin != null) routes++;
        long bytes = com.teammoeg.frostedheart.content.climate.thermal.source.SourceInputWindow.storageBytes(
                window.size, origins.length, routes);
        // Production already owns this finite window. Complete its handoff using
        // actual record/route counts, rather than two maximum windows per source.
        inputRetention.completeResize(Math.max(inputRetention.bytes(), retainedInputBytes() + bytes));
    }

    /** Source data is owned before source recording; all producer intervals already contain J. */
    public void prepareSpatialInput(int maximumRecords) {
        inputRetention.completeResize(Math.max(inputRetention.bytes(),
                retainedInputBytes() + 4096L + 2304L * maximumRecords));
    }
    public void receiveRestoredHeat(com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint.Restored restored,
            ThermalMemoryBudget.Reservation allowance) {
        long required = Math.max(inputRetention.bytes(), retainedInputBytes() + 8192L + 2048L * restored.heat().size());
        long growth = required - inputRetention.bytes();
        if (growth > 0) allowance.transferTo(inputRetention, growth);
        sourceLedger.restorePendingEnergy(restored.originalJ());
        sourceLedger.confirmRecordedLoss(restored.coolingLossJ(), false);
        sourceLedger.confirmRecordedDelivery(0, restored.unacceptedJ());
        receiveSpatialHeat(restored.heat());
    }

    public void receiveSpatialHeat(SpatialHeatInput input) {
        if (input.size() == 0) return;
        for (int index = 0; index < input.size(); index++) {
            var target = resolveTarget(input.target(index));
            var owner = target == null ? null : sourceOwner(target);
            var active = owner == null ? null : owner.job;
            if (active != null && (active.prepared() || active.heatClaim != null)
                    && (input.fromTick(index) < active.input.toTick
                        || input.impulse(index) && input.fromTick(index) == active.input.toTick))
                work.cancel(active);
        }
        heatInputs.receiveUnrouted(input);
        var affected = new ReferenceOpenHashSet<ThermalRegion>(4);
        try {
            for (int index = 0; index < input.size(); index++) {
                var target = input.target(index);
                var resolved = resolveTarget(target);
                var owner = resolved == null ? null : sourceOwner(resolved);
                if (owner != null) {
                    affected.add(owner);
                    noteSourceOwner(owner, target.sourceId(), target.sourceGeneration());
                    if (heatInputs.containsUnrouted(target)) {
                        var previous = updateRoutingOwner(target, owner);
                        if (previous != null) affected.add(previous);
                    }
                }
            }
        } finally {
            // No routing state survives this call without its region being refreshed.
            for (var region : affected) {
                refreshRouting(region);
                markChunkProgress(region);
            }
        }
    }

    /** A clean Page heartbeat cannot overtake a source that actually feeds this physical region. */
    public void sealSpatialInput(long through, double wind) {
        receivedThrough = Math.max(receivedThrough, through);
        if (heartbeatTick != through) { heartbeatTick = through; heartbeatIds = installedRegionIds; heartbeatCursor = 0; }
        int remaining = 16;
        while (remaining-- > 0 && (!sealReady.isEmpty() || heartbeatCursor < heartbeatIds.length)) {
            ThermalRegion region;
            if (!sealReady.isEmpty()) {
                var iterator = sealReady.iterator(); region = iterator.next(); iterator.remove();
            } else region = regionsById.get(heartbeatIds[heartbeatCursor++]);
            if (region == null || !region.installed) continue;
            long sealed = Math.min(through, executionLimit(region));
            long from = region.pending.throughTick(region.sampleTick());
            if (sealed <= from) continue;
            region.pending.receive(++spatialInputRevision, from, sealed,
                    Double.isNaN(wind) ? RegionPending.Environment.UNCHANGED : new RegionPending.Environment(wind, new long[0], new double[0]));
            refreshRouting(region);
            work.offer(region);
            for (long section : region.scope.sections) heatInputs.retryChunk(chunkForSection(section));
        }
    }

    public long receivedThrough() { return receivedThrough; }
    public long commitRevision() { return commitRevision; }
    public long minimumSampleTick(long emptyTick) {
        return sampleTimes.isEmpty() ? emptyTick : Math.min(emptyTick, sampleTimes.firstKey());
    }
    public int coefficientCount() {
        return installedCoefficientCount;
    }
    List<ThermalRegion> observedRegions() { return regions; }
    long capacityRevision() { return inputBudget == null ? 0 : inputBudget.capacityRevision(); }
    void whenCapacityChanges(long revision, Runnable wake) { if (inputBudget != null) inputBudget.whenCapacityChanges(revision, wake); }
    void cancelCapacityWait(Runnable wake) { if (inputBudget != null) inputBudget.cancelCapacityWait(wake); }
    public boolean hasReadyWork() {
        return work.hasReady() || quality.hasReadyWork() || heatInputs.hasReadyRouting();
    }
    public boolean hasRunningWork() { return work.hasRunning(); }
    private java.util.function.LongUnaryOperator geometryBoundary = ignored -> Long.MAX_VALUE;
    public void geometryBoundary(java.util.function.LongUnaryOperator boundary) { geometryBoundary = boundary; }
    private java.util.function.LongUnaryOperator sourceIntakeBoundary = ignored -> Long.MAX_VALUE;
    public void sourceIntakeBoundary(java.util.function.LongUnaryOperator boundary) { sourceIntakeBoundary = boundary; }
    @FunctionalInterface public interface IncomingSourceBoundary {
        long through(LongSet sections, java.util.function.LongPredicate knownSource, java.util.function.LongPredicate ownsTarget);
    }
    private IncomingSourceBoundary incomingSourceBoundary = (sections, known, owns) -> Long.MAX_VALUE;
    public void incomingSourceBoundary(IncomingSourceBoundary boundary) { incomingSourceBoundary = boundary; }

    /** One current limit for heartbeat, claiming, dispatch and acceptance of either numerical kernel. */
    long executionLimit(ThermalRegion region) {
        long through = region.executionLimit();
        for (long section : region.scope.sections) through = Math.min(through, geometryBoundary.applyAsLong(section));
        if (region.dependencySections != null) for (long section : region.dependencySections)
            through = Math.min(through, geometryBoundary.applyAsLong(section));
        through = Math.min(through, incomingSourceBoundary.through(region.scope.sections, region.inputSources::containsKey,
                position -> airOwnerAt(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position)) == region));
        for (var source : region.inputSources.long2IntEntrySet())
            through = Math.min(through, Math.min(sourceIntakeBoundary.applyAsLong(source.getLongKey()),
                    sourceLedger.spatialBoundThrough(source.getLongKey(), source.getIntValue())));
        return through;
    }


    public final class PageInputRetirement {
        private final ThermalInputBatch input;
        private int page;
        private SpatialHeatStore.Retarget retarget;
        private PageInputRetirement(ThermalInputBatch input) { this.input = input; }
        public boolean run() {
            if (page == input.retirements().length) return true;
            var retirement = input.retirements()[page];
            if (retarget == null) retarget = heatInputs.new Retarget(retirement.page().sectionKey(), retirement.page().lifecycleGeneration());
            if (retarget.run(32)) { retarget = null; page++; }
            return page == input.retirements().length;
        }
    }
    public PageInputRetirement preparePageInputRetirement(ThermalInputBatch input) { return new PageInputRetirement(input); }

    private long retainedInputBytes() {
        return 4096 + heatInputs.retainedBytes() + retainedPendingBytes + retainedMappingBytes;
    }

    private MinimumThermalModel.InputPlan prepareMinimumInputs(ThermalRegion region) {
        long current = region.minimumInputs == null ? 0 : region.minimumInputs.retainedBytes();
        int targets = heatInputs.targets(region.id);
        long next = Math.max(current, MinimumThermalModel.InputPlan.bytes(targets));
        long required = Math.max(4096L, retainedInputBytes() + next - current);
        // Caching is optional. A refused cache expansion never stalls the bounded numerical model.
        if (required > inputRetention.bytes() && !inputRetention.tryResize(required)) return region.minimumInputs;
        if (region.minimumInputs == null) region.minimumInputs = new MinimumThermalModel.InputPlan();
        region.minimumInputs.reserve(targets); retainedMappingBytes += next - current;
        return region.minimumInputs;
    }

    private ThermalRegion sourceOwner(SpatialHeatInput.Target target) {
        if (target.face() >= 6) return null;
        if (target.face() >= 0) {
            long position = target.position();
            var owner = airOwnerAt(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position));
            if (owner == null) return null;
            var component = owner.solver.layout().componentAt(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position));
            return component != null && (target.generation() == 0 || component.arenaGeneration == target.generation()) ? owner : null;
        }
        int slot = sourceMaterialSlot(target.position(), target.materialSlot(), target.generation(), target.materialProfile(), target.incarnation());
        return ownerAtSlot(slot);
    }

    private int sourceMaterialSlot(long block, int original, int generation, int profile, long incarnation) {
        if (original >= 0 && arena.isLive(original) && hasOwner(original)
                && arena.lifecycleGeneration(original) == generation && arena.materialIncarnation(original) == incarnation
                && matchesMaterial(original, block, profile)) return original;
        int migrated = pages.materialSlot(block, profile);
        if (migrated >= 0 && hasOwner(migrated) && arena.materialIncarnation(migrated) == incarnation
                && matchesMaterial(migrated, block, profile)) return migrated;
        return -1;
    }

    private boolean matchesMaterial(int slot, long block, int profile) {
        return arena.isMaterialCell(slot) && arena.materialProfileId(slot) == profile
                && arena.minimum(slot, 0) == BlockPos.getX(block) && arena.minimum(slot, 1) == BlockPos.getY(block)
                && arena.minimum(slot, 2) == BlockPos.getZ(block);
    }

    public void blockBoundary(RegionalContinuationBoundary boundary, long tick, long sequence) {
        holdBoundary(boundary, tick, sequence);
        for (var region : regions) if (boundaryTouches(region, boundary)) {
            refreshRouting(region);
            work.boundaryChanged(region);
        }
    }

    /** Accumulate the complete set before freezing any numerical prefix. */
    public void holdBoundary(RegionalContinuationBoundary boundary, long tick, long sequence) {
        for (var region : regions) if (boundaryTouches(region, boundary)) {
            if (tick < region.blockedAfterTick) { region.blockedAfterTick = tick; region.blockedAfterSequence = sequence; }
            else if (tick == region.blockedAfterTick) region.blockedAfterSequence = Math.min(region.blockedAfterSequence, sequence);
            markChunkProgress(region);
        }
        if (ThermalDiagnostics.observer != null) ThermalDiagnostics.observer.blocked(boundary, tick, regions);
    }

    public boolean boundaryReady(RegionalContinuationBoundary boundary, long tick, long sequence) {
        for (var region : regions) if (boundaryTouches(region, boundary) && (region.sampleTick() < tick
                || region.routingPending || region.job != null && (region.job.task != null || region.job.cancelled)
                || region.pending.hasControlThrough(tick, sequence))) return false;
        return true;
    }

    public String boundaryWaitDescription(RegionalContinuationBoundary boundary, long tick, long sequence) {
        for (var region : regions) if (boundaryTouches(region, boundary) && (region.sampleTick() < tick
                || region.routingPending || region.job != null && (region.job.task != null || region.job.cancelled)
                || region.pending.hasControlThrough(tick, sequence)))
            return "region=" + region.id + ":sample=" + region.sampleTick() + ":limit=" + region.executionLimit()
                    + ":routing=" + region.routingPending + ":job=" + (region.job != null)
                    + ":controls=" + region.pending.hasControlThrough(tick, sequence)
                    + (region.routingPending ? describeRoutingWait(region, tick) : "");
        return "ready";
    }

    private String describeRoutingWait(ThermalRegion region, long tick) {
        var detail = new StringBuilder();
        int remaining = 2;
        for (var target : region.routingTargets) if (heatInputs.pendingThrough(target, tick)) {
            long block = target.position();
            long section = SectionPos.asLong(BlockPos.getX(block) >> 4, BlockPos.getY(block) >> 4, BlockPos.getZ(block) >> 4);
            var page = pages.pageAt(section); var resolved = resolveTarget(target);
            var owner = resolved == null ? null : sourceOwner(resolved);
            detail.append(":target=").append(target).append(":pending=").append(heatInputs.describePending(target, tick))
                    .append(":pageLifetime=").append(page == null ? -1 : page.handle.lifecycleGeneration())
                    .append(":geometryBoundary=").append(geometryBoundary.applyAsLong(section))
                    .append(":sealed=").append(chunkSealed(chunkForSection(section)))
                    .append(":resolvedOwner=").append(owner == null ? -1 : owner.id);
            if (--remaining == 0) break;
        }
        return detail.isEmpty() ? ":no_input_due_at_boundary" : detail.toString();
    }

    boolean boundaryTouches(ThermalRegion region, RegionalContinuationBoundary boundary) {
        return sectionsTouch(region, boundary.sections());
    }
    private boolean sectionsTouch(ThermalRegion region, LongSet sections) {
        for (long section : region.scope.sections) if (sections.contains(section)) return true;
        if (region.dependencySections != null)
            for (long section : region.dependencySections) if (sections.contains(section)) return true;
        return false;
    }

    public void resetBoundaryHolds() {
        for (var region : regions) {
            if (region.blockedAfterTick != Long.MAX_VALUE) markChunkProgress(region);
            region.blockedAfterTick = Long.MAX_VALUE; region.blockedAfterSequence = Long.MAX_VALUE;
        }
    }

    public void boundaryHoldsChanged() {
        for (var region : regions) {
            sealReady.add(region);
            refreshRouting(region);
            work.boundaryChanged(region);
        }
    }

    public void resumeAfterBoundaries() {
        heatInputs.geometryChanged(this::holdForRouting);
        for (var region : regions) work.offer(region);
        work.retryMemory();
    }

    private void holdForRouting(SpatialHeatInput.Target target) {
        var resolved = resolveTarget(target);
        var owner = resolved == null ? null : sourceOwner(resolved);
        var previous = updateRoutingOwner(target, owner);
        if (previous != null && previous != owner) refreshRouting(previous);
        if (owner != null) refreshRouting(owner);
    }

    private final java.util.HashMap<SpatialHeatInput.Target, ThermalRegion> routingOwners = new java.util.HashMap<>();
    /** Membership only; the caller refreshes affected regions before offering work. */
    private ThermalRegion updateRoutingOwner(SpatialHeatInput.Target target, ThermalRegion owner) {
        var previous = owner == null ? routingOwners.remove(target) : routingOwners.put(target, owner);
        if (previous != null && previous != owner) previous.routingTargets.remove(target);
        if (owner != null) owner.routingTargets.add(target);
        return previous;
    }
    private void refreshRouting(ThermalRegion region) {
        long through = Math.min(region.pending.throughTick(region.sampleTick()), region.executionLimit());
        boolean pending = false;
        for (var target : region.routingTargets) if (heatInputs.pendingThrough(target, through)) { pending = true; break; }
        if (pending == region.routingPending) return;
        region.routingPending = pending; markChunkProgress(region); routingRevision++;
        if (!pending) work.offer(region);
    }

    public void runWorkRound(long deadline) {
        routeUnowned();
        if (System.nanoTime() < deadline) work.runRound(deadline);
    }

    private SpatialHeatInput.Target resolveTarget(SpatialHeatInput.Target target) {
        long block = target.position();
        long section = SectionPos.asLong(BlockPos.getX(block) >> 4, BlockPos.getY(block) >> 4, BlockPos.getZ(block) >> 4);
        var page = pages.pageAt(section);
        if (target.minimumPageGeneration() > 0 && (page == null || page.handle.lifecycleGeneration() < target.minimumPageGeneration())) return null;
        if (target.face() == SpatialHeatInput.MATERIAL && target.incarnation() == 0 && target.materialSlot() < 0) {
            if (page == null || target.materialStateId() < 0) return null;
            int signature = pages.signatureAtWorld(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
            if (!signatures.valid(signature) || signatures.materialStateId(signature) != target.materialStateId()) return null;
            int profile = signatures.materialProfileId(signature), slot = pages.materialSlot(block, profile);
            if (slot < 0 || !hasOwner(slot)) return null;
            return new SpatialHeatInput.Target(block, target.face(), slot, arena.lifecycleGeneration(slot), profile,
                    arena.materialIncarnation(slot), target.sourceId(), target.sourceGeneration(), target.materialStateId(), target.minimumPageGeneration());
        }
        if (target.face() < 6) {
            if (target.face() >= 0 && target.generation() == 0) {
                var owner = airOwnerAt(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
                var component = owner == null ? null : owner.solver.layout().componentAt(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
                return component == null ? null : new SpatialHeatInput.Target(block, target.face(), -1, component.arenaGeneration,
                        0, 0, target.sourceId(), target.sourceGeneration(), -1, target.minimumPageGeneration());
            }
            return target;
        }
        var resolved = new WorkerPageStore.MutableAirTarget();
        int slot = pages.resolveAirFaceTarget(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block),
                com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace.fromOrdinal(target.face() - 6), signatures, resolved);
        if (slot < 0) return null;
        return new SpatialHeatInput.Target(BlockPos.asLong(resolved.blockX(), resolved.blockY(), resolved.blockZ()),
                (byte) resolved.face().ordinal(), -1, resolved.generation(), 0, 0, target.sourceId(), target.sourceGeneration(), -1, target.minimumPageGeneration());
    }

    private void routeUnowned() {
        if (!heatInputs.hasReadyRouting()) return;
        var affected = new ReferenceOpenHashSet<ThermalRegion>(4);
        var receiver = new SpatialHeatStore.Receiver() {
            private SpatialHeatInput.Target key, resolved;
            private ThermalRegion owner;
            private int loss;
            private long boundary;
            private void resolve(SpatialHeatInput.Target target) {
                if (target.equals(key)) return;
                key = target; resolved = null; owner = null; loss = 0;
                long block = target.position();
                long section = SectionPos.asLong(BlockPos.getX(block) >> 4, BlockPos.getY(block) >> 4, BlockPos.getZ(block) >> 4);
                boundary = geometryBoundary.applyAsLong(section);
                var page = pages.pageAt(section);
                if (target.minimumPageGeneration() > 0
                        && (page == null || page.handle.lifecycleGeneration() < target.minimumPageGeneration())) return;
                if (target.face() == SpatialHeatInput.MATERIAL && target.materialStateId() >= 0 && page != null) {
                    int signature = pages.signatureAtWorld(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
                    if (signatures.valid(signature) && signatures.materialStateId(signature) != target.materialStateId()) { loss = 3; return; }
                }
                if (page != null && target.face() >= 0 && target.generation() != 0
                        && page.handle.lifecycleGeneration() != target.generation()) { loss = 2; return; }
                if (page != null && target.face() >= 0 && target.face() < 6 && target.generation() == 0) {
                    int signature = pages.signatureAtWorld(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
                    if (signatures.valid(signature) && !signatures.isAir(signature)) { loss = 1; return; }
                }
                if (target.face() >= 6) {
                    var outlet = new WorkerPageStore.MutableAirTarget();
                    int slot = pages.resolveAirFaceTarget(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block),
                            BlockFace.fromOrdinal(target.face() - 6), signatures, outlet);
                    if (slot == -1) { loss = 1; return; }
                    if (slot < 0) return;
                    resolved = new SpatialHeatInput.Target(BlockPos.asLong(outlet.blockX(), outlet.blockY(), outlet.blockZ()),
                            (byte) outlet.face().ordinal(), -1, outlet.generation(), 0, 0, target.sourceId(), target.sourceGeneration(), -1,
                            target.minimumPageGeneration());
                } else resolved = resolveTarget(target);
                owner = resolved == null ? null : sourceOwner(resolved);
                if (resolved != null && target.face() == SpatialHeatInput.MATERIAL && resolved.materialStateId() < 0) {
                    int signature = pages.signatureAtWorld(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
                    if (signatures.valid(signature)) resolved = new SpatialHeatInput.Target(resolved.position(), resolved.face(), resolved.materialSlot(),
                            resolved.generation(), resolved.materialProfile(), resolved.incarnation(), resolved.sourceId(), resolved.sourceGeneration(),
                            signatures.materialStateId(signature), resolved.minimumPageGeneration());
                }
                if (owner == null && target.face() == SpatialHeatInput.MATERIAL && page != null && target.incarnation() != 0) loss = 3;
            }
            public long availableThrough(SpatialHeatInput.Target target) {
                resolve(target);
                if (chunkSealed(net.minecraft.world.level.ChunkPos.asLong(BlockPos.getX(target.position()) >> 4,
                        BlockPos.getZ(target.position()) >> 4))) return -1;
                if (resolved != null && chunkSealed(net.minecraft.world.level.ChunkPos.asLong(BlockPos.getX(resolved.position()) >> 4,
                        BlockPos.getZ(resolved.position()) >> 4))) return -1;
                if (loss != 0) return Math.min(receivedThrough, boundary);
                return owner == null ? -1 : Math.min(owner.pending.throughTick(owner.sampleTick()),
                        Math.min(owner.executionLimit(), boundary));
            }
            public boolean accept(SpatialHeatInput.Target target, long from, long to, double joules) {
                resolve(target);
                if (loss != 0) {
                    if (loss == 3 || target.face() == SpatialHeatInput.MATERIAL) sourceLedger.confirmRecordedDelivery(0, joules);
                    else sourceLedger.confirmRecordedLoss(joules, loss == 2);
                    return true;
                }
                var region = owner;
                if (region == null) return false;
                affected.add(region);
                var previous = updateRoutingOwner(target, region);
                if (previous != null) affected.add(previous);
                noteSourceOwner(region, target.sourceId(), target.sourceGeneration());
                var active = region.job;
                if (active != null && (active.prepared() || active.heatClaim != null)
                        && (from < active.input.toTick || from == to && from == active.input.toTick)) {
                    if (!active.blockedInputs.contains(target)) active.blockedInputs.add(target);
                    work.cancel(active);
                    return false;
                }
                if (from == to && from == region.sampleTick() && !region.pending.hasPending())
                    region.pending.receive(++spatialInputRevision, from, to, RegionPending.Environment.UNCHANGED);
                prepareSpatialInput(1);
                // Recorded J no longer depends on the emitter's registry lifetime.
                // Only the world target identity owns the stable stream.
                var world = new SpatialHeatInput.Target(resolved.position(), resolved.face(), resolved.materialSlot(),
                        resolved.face() >= 0 ? 0 : resolved.generation(), resolved.materialProfile(), resolved.incarnation(), 0, 0,
                        resolved.materialStateId(), Math.max(resolved.minimumPageGeneration(),
                                pages.pageAt(SectionPos.asLong(BlockPos.getX(resolved.position()) >> 4, BlockPos.getY(resolved.position()) >> 4,
                                        BlockPos.getZ(resolved.position()) >> 4)).handle.lifecycleGeneration()));
                heatInputs.receive(region.id, world, from, to, joules);
                return true;
            }
            public void afterAttempt(SpatialHeatInput.Target target) {
                var region = heatInputs.containsUnrouted(target) ? routingOwners.get(target) : updateRoutingOwner(target, null);
                if (region != null) affected.add(region);
            }
        };
        try {
            heatInputs.route(receiver, 32);
        } finally {
            for (var region : affected) refreshRouting(region);
            // offer can freeze inputs. All routing membership changes must be visible first,
            // including received J whose region's routingPending flag stayed false.
            for (var region : affected) work.offer(region);
        }
    }

    boolean prepareJob(RegionJob job, long deadline) {
        var region = job.region;
        job.waiting = RegionWork.Wait.NONE;
        // Controls wait for the actual helper exit and publish before a new prefix can run.
        if (!region.changedMaterials.isEmpty() || region.materialControls != null) {
            work.cancel(job); return false;
        }
        if (job.qualityOnly()) {
            if (job.reservation == null) {
                long bytes = region.solver.minimumTransportBytes() + 4096;
                if (region.minimumWorkspace == null) region.minimumWorkspace = inputBudget.reserveCompletion(
                        MinimumRegionStep.workspaceBytes(region.accepted().field()));
                var workspace = region.minimumWorkspace;
                job.workspaceFloor = workspace.bytes();
                if (workspace.bytes() < bytes) workspace.completeResize(bytes);
                job.reservation = workspace; job.prepaidWorkspace = true;
                job.account = ThermalWorkBudget.newAccount();
            }
            job.inputsReady = true;
            return true;
        }
        if (region.materialRestoration != null) {
            if (!region.materialRestoration.run(deadline)) return false;
            region.materialRestoration = null;
        }
        boolean minimum = region.accepted().field().minimumQuality();
        if (job.reservation == null) {
            int records = heatInputs.records(region.id);
            long bytes = RegionalQuerySnapshot.Preparation.backingBytes(region.template) + MinimumThermalModel.projectionBytes()
                    + 4096L + 1024L * records;
            // A private fine build can select minimum after its initial metadata
            // preparation. Acquire that installed model's slot on first use too.
            if (minimum && region.minimumWorkspace == null) region.minimumWorkspace = inputBudget.reserveCompletion(
                    MinimumRegionStep.workspaceBytes(region.accepted().field()));
            if (minimum) region.minimumWorkspace.completeResize(Math.max(region.minimumWorkspace.bytes(),
                    MinimumRegionStep.workspaceBytes(region.accepted().field())));
            var reservation = minimum ? region.minimumWorkspace : inputBudget.tryReserve(bytes);
            job.prepaidWorkspace = minimum;
            if (reservation == null && region.minimumWorkspace != null && region.minimumWorkspace.bytes() >= bytes
                    && region.solver.inputArraysFit(records)) {
                reservation = region.minimumWorkspace; job.prepaidWorkspace = true;
            }
            if (reservation == null) {
                if (!minimum && ThermalWorkBudget.automatic()) quality.resourcePressure(region);
                else job.waiting = RegionWork.Wait.MEMORY;
                return false;
            }
            job.reservation = reservation; job.account = ThermalWorkBudget.newAccount();
            if (minimum) {
                initializeEnvironment(region);
                var background = region.accepted().field().minimumState().layout().background();
                double shift = background.environmentShiftC(page -> region.environmentNaturalC.getOrDefault(page, geometry.naturalTemperatureC(page)));
                double[] environment = new double[com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState.MAXIMUM_NODES];
                for (int node = 0; node < environment.length; node++) environment[node] = background.nodeC(node) + shift;
                job.minimum = new MinimumRegionStep(region.accepted().field(), job.input, geometry.structuralVersion(),
                        environment, target -> sourceMaterialSlot(target.position(), target.materialSlot(), target.generation(),
                                target.materialProfile(), target.incarnation()), prepareMinimumInputs(region));
                job.heatClaim = heatInputs.claim(region.id, job.input.fromTick, job.input.toTick, job.minimum::accept);
            } else job.heatClaim = heatInputs.claim(region.id, job.input.fromTick, job.input.toTick);
        }
        if (job.heat == null) {
            try (var accounted = ThermalWorkBudget.enter(job.account, ThermalWorkBudget.Phase.TRANSPORT)) {
                if (!job.heatClaim.prepare(deadline)) return false;
            }
            job.heat = job.heatClaim.input();
            // Input summaries already contain their temporal approximation.
            // Spatial precision continues to follow actual work/memory costs.
        }
        if (minimum) {
            job.minimum.sealInput(); job.inputsReady = true;
            return true;
        }
        job.loads = CutLoadBuffer.compile(job.heat, region.solver.layout(), arena, this::sourceMaterialSlot);
        if (job.loads == null) { job.release(); return false; }
        job.topologyGeneration = geometry.structuralVersion();
        initializeEnvironment(region);
        java.util.function.IntToDoubleFunction natural = region.environmentNaturalC.isEmpty()
                ? geometry::naturalTemperatureC
                : page -> region.environmentNaturalC.getOrDefault(page, geometry.naturalTemperatureC(page));
        region.solver.freezeStepInputs(region.environmentWindScale, natural);
        region.solver.reserveInputArrays(job.loads.size(), job.reservation, solverRetention);
        job.cursor = region.solver.beginCut(job.input.fromTick, job.input.toTick, job.loads);
        job.inputsReady = true;
        return true;
    }

    private void initializeEnvironment(ThermalRegion region) {
        if (region.environmentTick >= 0) return;
        region.environmentWindScale = geometry.windScale(); region.environmentTick = region.sampleTick();
        for (long section : region.scope.sections) {
            var page = pages.pageAt(section);
            if (page != null) region.environmentNaturalC.put(page.workerPageSlot(), geometry.naturalTemperatureC(page.workerPageSlot()));
        }
    }

    private void updateEnvironment(ThermalRegion region, RegionPending.Environment environment, long tick) {
        if (environment == null || region.environmentTick > tick) return;
        initializeEnvironment(region);
        if (!Double.isNaN(environment.windScale())) region.environmentWindScale = environment.windScale();
        for (int index = 0; index < environment.sections().length; index++) {
            var page = pages.pageAt(environment.sections()[index]);
            if (page != null) region.environmentNaturalC.put(page.workerPageSlot(), environment.naturalC()[index]);
        }
        region.environmentTick = tick;
    }

    void discardJob(ThermalRegion region) {
        var job = region.job;
        if (job == null) return;
        if (job.cursor != null) region.solver.restoreAcceptedState();
        for (var target : job.blockedInputs) heatInputs.retry(target);
        job.release(); region.job = null;
    }
    boolean discardJobSlice(RegionJob job, long deadline) {
        var region = job.region;
        if (region.job != job) { job.release(); return true; }
        if (job.cursor != null && !RegionalQualityControl.minimumPending(region) && !region.retired && region.snapshotPauses == 0) {
            if (job.restoration == null) {
                job.restoration = region.solver.prepareAcceptedRestoration();
            }
            if (job.restoration != null && !job.restoration.run(deadline)) return false;
        } else if (job.cursor != null) {
            if (!RegionalQualityControl.minimumPending(region) && !region.retired) region.materialRestoration = region.solver.prepareAcceptedRestoration();
        }
        for (var target : job.blockedInputs) heatInputs.retry(target);
        job.release(); region.job = null;
        routingRevision++;
        return true;
    }

    boolean acceptJob(RegionJob job) {
        var region = job.region;
        if (region.job != job) return true;
        if (job.qualityOnly()) return acceptMinimumLayout(job);
        if (ThermalDiagnostics.observer != null && !ThermalDiagnostics.observer.beforeAccept(this, job)) {
            work.cancel(job); return false;
        }
        var input = job.input;
        var snapshot = job.publication;
        Runnable accept = () -> {
            sourceLedger.confirmRecordedDelivery(job.deliveredEnergyJ(), job.unacceptedEnergyJ());
            regionalSolveSumNanos += job.durationNanos;
            region.pending.accepted(input);
            if (job.heatClaim != null) job.heatClaim.accepted();
            region.accept(snapshot, input.toTick, input.sequence);
            noteAcceptedTick(region);
            commitRevision++;
        };
        if (!queries.publishRegion(snapshot, accept)) return false;
        if (input.toTick > input.fromTick && input.toTick > oldestUninstalledTick.getAsLong()) {
            independentCommits++;
            if (!independentRegions.contains(region.id)) {
                var observed = new LongOpenHashSet(independentRegions); observed.add(region.id); independentRegions = observed;
            }
        }
        updateEnvironment(region, input.environment, input.toTick);
        lastRegionalContact = region.solver.contactModeSummary();
        refreshContactDescription();
        ThermalWorkBudget.recordWork(input.toTick, region.id, region.epoch, "advance", region.solver.gridStep(),
                "accepted", job.account == null ? 0 : job.account.elapsedNanos(), job.account);
        if (job.account != null && region.workPolicy != null) {
            region.workPolicy.advanced(region.solver.gridStep(), job.account.activeNanos(),
                    region.workCoarseningUseful() ? job.workAllowance : Long.MAX_VALUE, input.toTick);
            quality.reconsider(region);
        }
        if (TRACE) FHMain.LOGGER.info("THERMAL_REGION_ADVANCE,stage=published,sequence={},regionId={},targetTick={},nanos={}",
                input.sequence, region.id, input.toTick, System.nanoTime());
        job.release(); region.job = null;
        for (long section : region.scope.sections) pages.markMaintenanceDirty(section);
        if (ThermalDiagnostics.observer != null) ThermalDiagnostics.observer.accepted(this, job, regions);
        return true;
    }

    public double pendingEnergyJ() {
        return heatInputs.pendingEnergyJ();
    }

    private static final class SnapshotHold {
        final long chunk;
        final List<ThermalRegion> regions;
        final RegionalCheckpoint.Purpose purpose;
        boolean sealed;
        SnapshotHold(long chunk, List<ThermalRegion> regions, RegionalCheckpoint.Purpose purpose) {
            this.chunk = chunk; this.regions = regions; this.purpose = purpose;
        }
        boolean waitsForJob(ThermalRegion region) {
            return region.job != null && (purpose == RegionalCheckpoint.Purpose.UNLOAD
                    || !region.changedMaterials.isEmpty() || region.materialControls != null);
        }
    }
    private final IdentityHashMap<Object, SnapshotHold> snapshots = new IdentityHashMap<>();
    private boolean chunkSealed(long chunk) {
        for (var held : snapshots.values()) if (held.chunk == chunk) return true;
        return false;
    }
    public void sealSnapshot(Object identity) {
        var held = snapshots.get(identity);
        if (held == null || held.sealed) return;
        held.sealed = true;
        for (var region : held.regions) region.snapshotSeals++;
    }
    public void beginSnapshot(Object identity, long chunk, RegionalCheckpoint.Purpose purpose) {
        if (snapshots.containsKey(identity)) return;
        var indexed = chunkOwners.get(chunk);
        var held = indexed == null ? List.<ThermalRegion>of() : List.copyOf(indexed);
        snapshots.put(identity, new SnapshotHold(chunk, held, purpose));
        if (purpose == RegionalCheckpoint.Purpose.UNLOAD) for (var region : held) {
            region.snapshotPauses++;
            if (region.job != null) work.cancel(region.job);
        }
    }
    public boolean snapshotReady(Object identity) {
        var held = snapshots.get(identity);
        if (held == null) return false;
        boolean ready = true;
        for (var region : held.regions) {
            if (held.waitsForJob(region)) { work.cancel(region.job); ready = false; }
            else ready &= importMaterialControls(region);
        }
        return ready;
    }
    public boolean snapshotHasJobs(Object identity) {
        var held = snapshots.get(identity);
        if (held != null) for (var region : held.regions) if (held.waitsForJob(region)) return true;
        return false;
    }
    public boolean snapshotTouches(Object identity, RegionalContinuationBoundary boundary) {
        var held = snapshots.get(identity);
        if (held != null) for (var region : held.regions) if (boundaryTouches(region, boundary)) return true;
        return false;
    }
    public boolean sourceTouchesSnapshot(Object identity, long source) {
        var held = snapshots.get(identity); var owners = sourceOwners.get(source);
        if (held != null && owners != null) for (var region : held.regions) if (owners.contains(region)) return true;
        return false;
    }
    public void resumeSnapshot(Object identity) {
        var held = snapshots.remove(identity);
        if (held == null) return;
        heatInputs.retryChunk(held.chunk);
        for (var region : held.regions) {
            if (held.purpose == RegionalCheckpoint.Purpose.UNLOAD) region.snapshotPauses--;
            if (held.sealed) region.snapshotSeals--;
            quality.resourcesReturned(region); work.resourcesReturned(region); work.offer(region);
        }
    }
    public long checkpointBytes(long chunk) {
        var owners = chunkOwners.get(chunk);
        long records = heatInputs.checkpointRecordsInChunk(chunk);
        long bytes = 32768L + 512L * records;
        if (owners != null) for (var region : owners) if (region.accepted().field() != null)
            bytes += region.accepted().field().checkpointBytes(chunk);
        return bytes;
    }
    public SpatialHeatInput.Builder collectCheckpointInputs(long chunk) {
        var summary = new SpatialHeatInput.Builder();
        heatInputs.collectChunk(chunk, summary::appendAggregate);
        return summary;
    }
    public double exportChunkPending(long chunk) {
        double[] total = {0};
        java.util.function.ObjDoubleConsumer<SpatialHeatInput.Target> transfer = (target, joules) -> {
            total[0] += joules;
            var previous = updateRoutingOwner(target, null);
            if (previous != null) refreshRouting(previous);
        };
        heatInputs.retireChunk(chunk, transfer);
        return total[0];
    }

    public com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint
            checkpoint(long chunk, SpatialHeatInput.Builder pending,
                    ThermalMemoryBudget.Reservation credit,
                    com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint coldPending) {
        var fields = new ArrayList<RegionalQuerySnapshot>();
        var leases = new ArrayList<QueryPublication.FrameLease>();
        var owners = chunkOwners.get(chunk);
        if (owners != null) for (var region : owners) {
            var field = region.accepted().field();
            if (field == null) continue;
            var lease = queries.borrowFrame(field, credit);
            if (lease == null) { for (var previous : leases) previous.close(); return null; }
            fields.add(field); leases.add(lease);
        }
        var sections = RegionalCheckpoint.indexSections(chunk, fields, section -> {
            var page = pages.pageAt(section);
            return page == null ? -1 : pages.geometryRevision(page);
        });
        return new RegionalCheckpoint(fields, sections, pending.freeze(), leases, coldPending, credit);
    }

    public void clearRegionalInputs() {
        if (!work.settle()) throw new IllegalStateException("Regional helpers must retire before input backing");
        heatInputs.clear();
        quality.clear();
        routingOwners.clear();
        for (var region : regions) { region.pending.clear(); region.minimumInputs = null; region.releaseMinimumWorkspace(); }
        retainedMappingBytes = 0;
        if (inputRetention != null) { inputRetention.close(); inputRetention = null; }
    }

    /** A terminal coordinator stops acceptance but retains immutable fields and every input owner for sealing. */
    public void suspendAfterFailure() {
        for (var region : regions) { region.snapshotPauses++; if (region.job != null) work.cancel(region.job); }
        work.cancelAll();
    }

    public boolean hasHelperCleanup() { return work.hasReady(); }
    public boolean settleJobs() { return work.settle(); }

    public void abortPreparedSteps() {
        for (ThermalRegion region : regions) {
            if (region.job != null) discardJob(region);
        }
    }

    public void recordBacking(AirBackingMeter meter, StringBuilder description) {
        recordBacking(meter, description, null, true);
    }

    /** Other candidates keep their own scratch owner; this install cannot adopt their backing. */
    public void recordHandoffBacking(AirBackingMeter meter, Set<Rebuild> admitted) {
        recordBacking(meter, null, admitted, false);
    }

    private void recordBacking(AirBackingMeter meter, StringBuilder description, Set<Rebuild> admitted, boolean allCandidates) {
        Set<ThermalRegion> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        recordRegions(regions, meter, seen, description, "installed");
        meter.container(ownerPages, ownerPages.size(), 16);
        for (var page : ownerPages.values()) recordOwnerPage(page, meter);

        meter.container(restorations, restorations.size(), 16);
        meter.container(restorationConsumedBricks, restorationConsumedBricks.size(), 16);
        meter.container(independentRegions, independentRegions.size(), 8);
        for (var build : builds) if (build.result != null && (allCandidates || admitted.contains(build))) {
            recordRegions(build.prepared, meter, seen, description, "candidate");
            if (build.ownership != null) for (var page : build.ownership.next.values()) recordOwnerPage(page, meter);
        }
    }

    private static void recordOwnerPage(OwnerPage page, AirBackingMeter meter) {
        if (page == null || !meter.objectOnce(page, 64)) return;
        meter.container(page.cells, page.cells.size(), 16); meter.container(page.air, page.air.size(), 16);
        for (var owners : page.air.values()) meter.container(owners, owners.size(), 8);
        recordOwnerPage(page.previous, meter);
    }

    /** Replaces only affected Page membership; unrelated Page buckets stay installed. */
    private final class OwnershipPreparation {
        final Rebuild rebuild;
        final com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token;
        final Long2ObjectOpenHashMap<OwnerPage> next = new Long2ObjectOpenHashMap<>();
        final Set<ThermalRegion> replaced = Collections.newSetFromMap(new IdentityHashMap<>());
        int phase, region, cell, brick, node, finishedPage;
        long[] sections;
        OwnershipPreparation(Rebuild rebuild, com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
            this.rebuild = rebuild; this.token = token;
            for (var parent : rebuild.parents) if (!rebuild.result.regions.contains(parent)) replaced.add(parent);
        }
        boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int remaining = 512;
                    while (remaining-- > 0 && region < rebuild.prepared.size()) {
                        var owner = rebuild.prepared.get(region);
                        if (cell == owner.scope.cells.length) { region++; cell = 0; continue; }
                        int slot = owner.scope.cells[cell++];
                        long section = SectionPos.asLong(arena.minimum(slot, 0) >> 4, arena.minimum(slot, 1) >> 4, arena.minimum(slot, 2) >> 4);
                        next.computeIfAbsent(section, OwnerPage::new).cells.put(slot, owner);
                    }
                    if (region == rebuild.prepared.size()) {
                        for (var parent : replaced) for (long section : parent.scope.sections) next.computeIfAbsent(section, OwnerPage::new);
                        sections = next.keySet().toLongArray(); phase++; region = cell = 0;
                    }
                } else if (phase == 1) {
                    if (region == sections.length) { phase++; region = 0; continue; }
                    var into = next.get(sections[region]); var existing = pages.pageAt(sections[region]);
                    var publication = existing == null ? null : existing.handle.lastPublication();
                    if (publication == null || brick == 64) { region++; brick = node = 0; continue; }
                    var payload = publication.brick(brick);
                    int count = payload.airNodeCount() + (payload.blockLayout() == null ? 0 : Long.bitCount(payload.blockLayout().materialNodeMask()));
                    int end = Math.min(count, node + 128);
                    for (; node < end; node++) {
                        int slot = payload.firstSlot() + node;
                        var owner = ownerAtSlot(slot);
                        if (owner != null && !replaced.contains(owner)) into.cells.put(slot, owner);
                    }
                    if (node == count) { brick++; node = 0; }
                } else if (phase == 2) {
                    int remaining = 128;
                    while (remaining-- > 0 && region < rebuild.prepared.size()) {
                        var owner = rebuild.prepared.get(region); var layout = owner.solver.layout();
                        if (cell == layout.componentCount()) { region++; cell = 0; continue; }
                        var component = layout.component(cell++);
                        var page = next.get(SectionPos.asLong(component.minX >> 4, component.minY >> 4, component.minZ >> 4));
                        var at = page.air.computeIfAbsent(BlockPos.asLong(component.minX, component.minY, component.minZ), ignored -> new ArrayList<>());
                        if (at.isEmpty() || at.get(at.size() - 1) != owner) at.add(owner);
                    }
                    if (region == rebuild.prepared.size()) { phase++; region = cell = 0; }
                } else if (phase == 3) {
                    if (region == sections.length) { phase++; region = 0; continue; }
                    var page = next.get(sections[region]); var old = ownerPages.get(page.section); old = old == null ? null : old.visible();
                    if (old != null) {
                        for (var entry : old.air.long2ObjectEntrySet()) for (var owner : entry.getValue()) if (!replaced.contains(owner)) {
                            var at = page.air.computeIfAbsent(entry.getLongKey(), ignored -> new ArrayList<>());
                            if (!at.contains(owner)) at.add(owner);
                        }
                    }
                    region++;
                } else if (phase == 4) {
                    int end = Math.min(sections.length, region + 16);
                    for (; region < end; region++) {
                        var page = next.get(sections[region]); page.previous = ownerPages.get(page.section); page.token = token;
                        ownerPages.put(page.section, page);
                    }
                    if (region == sections.length) { phase++; region = 0; return true; }
                } else return true;
            } while (System.nanoTime() < deadline);
            return false;
        }
        boolean finish(long deadline) {
            if (sections == null) return true;
            do {
                if (finishedPage == sections.length) return true;
                var page = next.get(sections[finishedPage++]);
                if (ownerPages.get(page.section) != page) continue;
                if (!token.committed()) {
                    if (page.previous == null) ownerPages.remove(page.section); else ownerPages.put(page.section, page.previous);
                } else if (page.cells.isEmpty()) ownerPages.remove(page.section);
                page.previous = null; page.token = null;
            } while (System.nanoTime() < deadline);
            return finishedPage == sections.length;
        }
    }

    private static void recordRegions(List<ThermalRegion> regions, AirBackingMeter meter, Set<ThermalRegion> seen,
            StringBuilder description, String stage) {
        meter.container(regions, regions.size(), 8);
        for (ThermalRegion region : regions) if (seen.add(region)) {
            region.scope.recordBacking(meter);
            if (description == null) region.solver.recordBacking(meter);
            else {
                if (description.length() != 0) description.append(',');
                var local = new StringBuilder();
                region.solver.recordBacking(meter, local);
                description.append(stage).append("Region").append(region.id).append("=[").append(local).append(']');
            }
            // One queued/running job (including callback/future bookkeeping) is prepaid
            // on installation. Environment capacity is bounded by this region's Pages.
            meter.bytes(512);
            // Pending input/receipt metadata and one environment endpoint per
            // Page, for the running prefix plus the two bounded waiting prefixes.
            meter.bytes(256L + 16L * region.scope.sections.size());
            meter.container(region.environmentNaturalC, region.scope.sections.size(), 16);
            if (region.workPolicy != null) meter.container(region.workPolicy, 1, 16);
            if (region.dependencySections != null) meter.container(region.dependencySections, region.dependencySections.size(), 8);
        }
    }

    /** Parent storage is local to captured cells. Global arena addresses remain external identities. */
    private static final class LocalParents {
        final IntArrayList globals;
        final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap local;
        final int[] parent;
        int initialized;
        LocalParents(IntArrayList globals) {
            this.globals = globals; local = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap(globals.size());
            local.defaultReturnValue(-1); parent = new int[globals.size()];
        }
        boolean initialize() {
            int end = Math.min(parent.length, initialized + 512);
            for (; initialized < end; initialized++) { parent[initialized] = initialized; local.put(globals.getInt(initialized), initialized); }
            return initialized == parent.length;
        }
        int localRoot(int slot) {
            int index = local.get(slot);
            if (index < 0) return -1;
            while (parent[index] != index) { parent[index] = parent[parent[index]]; index = parent[index]; }
            return index;
        }
        int root(int slot) { int index = localRoot(slot); return index < 0 ? -1 : globals.getInt(index); }
        void join(int first, int second) {
            int a = localRoot(first), b = localRoot(second);
            if (a >= 0 && b >= 0) parent[Math.max(a, b)] = Math.min(a, b);
        }
    }

    private final class PartitionPreparation {
        final PreparedTopologyChange change;
        final AirGeometryView view;
        final Int2ObjectOpenHashMap<ThermalFragment> fragments;
        final FrozenSources bindings;
        final IntArrayList cells = new IntArrayList();
        final Long2ObjectOpenHashMap<IntArrayList> air = new Long2ObjectOpenHashMap<>();
        final Int2ObjectOpenHashMap<IntArrayList> groups = new Int2ObjectOpenHashMap<>();
        final Int2ObjectOpenHashMap<Scope> byRoot = new Int2ObjectOpenHashMap<>();
        final ArrayList<Scope> scopes = new ArrayList<>();
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap materialPackets = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        final IntOpenHashSet touched = new IntOpenHashSet();
        final ArrayList<LocalAirShape> shapes = new ArrayList<>();
        final WorkerPageStore.MutableAirTarget portTarget = new WorkerPageStore.MutableAirTarget();
        AirGeometryView.CellCursor cellCursor;
        LocalParents parent;
        boolean[] hasAir;
        java.util.Iterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<IntArrayList>> brickIterator;
        java.util.Iterator<ThermalFragment> fragmentIterator;
        it.unimi.dsi.fastutil.ints.IntIterator keyIterator;
        IntArrayList firstCells, otherCells;
        long brick;
        int phase, cursor, axis, firstIndex, secondIndex, edge, fragmentPart, traceSide, tracePatch, assignPart;
        ThermalFragment fragment;
        int[] keys, fragmentKeys;
        com.teammoeg.frostedheart.content.climate.thermal.runtime.IntSortCursor sort;
        ScopePreparation scopePreparation;

        PartitionPreparation(AirGeometryView view, Int2ObjectOpenHashMap<ThermalFragment> fragments,
                FrozenSources bindings,
                PreparedTopologyChange change) {
            this.change = change;
            this.view = view; this.fragments = fragments; this.bindings = bindings;
            cellCursor = view.cellCursor(arena); materialPackets.defaultReturnValue(-1);
        }

        boolean run(long deadline) {
            do {
                switch (phase) {
                    case 0 -> {
                        int remaining = 512;
                        while (remaining-- > 0 && cellCursor.hasNext()) {
                            int slot = cellCursor.nextSlot(); if (slot < 0) continue;
                            cells.add(slot);
                            if (arena.isAirCell(slot)) air.computeIfAbsent(BlockPos.asLong(arena.minimum(slot, 0),
                                    arena.minimum(slot, 1), arena.minimum(slot, 2)), ignored -> new IntArrayList()).add(slot);
                        }
                        if (!cellCursor.hasNext()) { parent = new LocalParents(cells); phase++; }
                    }
                    case 1 -> {
                        if (parent.initialize()) { brickIterator = air.long2ObjectEntrySet().iterator(); phase++; }
                    }
                    case 2 -> {
                        int remaining = 256;
                        while (remaining-- > 0) {
                            if (firstCells == null) {
                                if (!brickIterator.hasNext()) { phase++; fragmentIterator = fragments.values().iterator(); break; }
                                var entry = brickIterator.next(); brick = entry.getLongKey(); firstCells = entry.getValue();
                                axis = firstIndex = secondIndex = 0; otherCells = null;
                            }
                            if (axis == 3) { firstCells = null; continue; }
                            if (otherCells == null) {
                                otherCells = air.get(BlockPos.asLong(BlockPos.getX(brick) + (axis == 0 ? 4 : 0),
                                        BlockPos.getY(brick) + (axis == 1 ? 4 : 0), BlockPos.getZ(brick) + (axis == 2 ? 4 : 0)));
                                if (otherCells == null) { axis++; continue; }
                            }
                            int first = firstCells.getInt(firstIndex), second = otherCells.getInt(secondIndex);
                            if (AirFieldCompiler.touches(arena.airBlockMask(first), arena.airBlockMask(second), axis)) parent.join(first, second);
                            if (++secondIndex == otherCells.size()) {
                                secondIndex = 0;
                                if (++firstIndex == firstCells.size()) { firstIndex = 0; otherCells = null; axis++; }
                            }
                        }
                    }
                    case 3 -> joinFragments();
                    case 4 -> {
                        int end = Math.min(cells.size(), cursor + 128);
                        for (; cursor < end; cursor++) {
                            int cell = cells.getInt(cursor);
                            if (!arena.isMaterialCell(cell) || materials.profileOrNull(arena.materialProfileId(cell)).faceConductanceWPerK() == 0) continue;
                            int x = arena.minimum(cell, 0), y = arena.minimum(cell, 1), z = arena.minimum(cell, 2);
                            for (int ordinal = 0; ordinal < BlockFace.COUNT; ordinal++) {
                                var face = BlockFace.fromOrdinal(ordinal);
                                parent.join(cell, airAt(air, arena, x + face.stepX(), y + face.stepY(), z + face.stepZ()));
                            }
                        }
                        if (cursor == cells.size()) { phase++; cursor = 0; }
                    }
                    case 5 -> {
                        for (var entry : restorations.long2ObjectEntrySet()) if (view.pageAt(entry.getLongKey()) != null) {
                            var checkpoint = entry.getValue().entry().spatialAir();
                            if (checkpoint != null) for (int shape = 0; shape < checkpoint.shapeCount(); shape++) shapes.add(checkpoint.shape(shape));
                        }
                        phase++; cursor = 0;
                    }
                    case 6 -> {
                        if (cursor < shapes.size()) joinRetainedShape(shapes.get(cursor++));
                        if (cursor == shapes.size()) { phase++; cursor = 0; hasAir = new boolean[cells.size()]; }
                    }
                    case 7 -> {
                        int end = Math.min(cells.size(), cursor + 512);
                        for (; cursor < end; cursor++) { int slot = cells.getInt(cursor); if (arena.isAirCell(slot)) hasAir[parent.localRoot(slot)] = true; }
                        if (cursor == cells.size()) { phase++; cursor = 0; }
                    }
                    case 8 -> {
                        int end = Math.min(cells.size(), cursor + 512);
                        for (; cursor < end; cursor++) {
                            int slot = cells.getInt(cursor), representative = parent.root(slot);
                            if (representative != slot || hasAir[parent.localRoot(slot)]) continue;
                            long section = SectionPos.asLong(arena.minimum(slot, 0) >> 4, arena.minimum(slot, 1) >> 4, arena.minimum(slot, 2) >> 4);
                            int packet = materialPackets.putIfAbsent(section, representative);
                            if (packet >= 0) parent.join(packet, representative);
                        }
                        if (cursor == cells.size()) { phase++; cursor = 0; }
                    }
                    case 9 -> {
                        int end = Math.min(cells.size(), cursor + 512);
                        for (; cursor < end; cursor++) {
                            int slot = cells.getInt(cursor);
                            groups.computeIfAbsent(parent.root(slot), ignored -> new IntArrayList()).add(slot);
                        }
                        if (cursor == cells.size()) {
                            keys = groups.keySet().toIntArray();
                            sort = new com.teammoeg.frostedheart.content.climate.thermal.runtime.IntSortCursor(keys); phase++;
                        }
                    }
                    case 10 -> { if (sort.run(deadline)) { phase++; cursor = 0; sort = null; } }
                    case 11 -> {
                        if (cursor == keys.length) { phase++; cursor = 0; keyIterator = fragments.keySet().iterator(); continue; }
                        if (scopePreparation == null) scopePreparation = new ScopePreparation(arena, view, groups.get(keys[cursor]).toIntArray());
                        if (!scopePreparation.run(deadline)) return false;
                        var scope = scopePreparation.result; scopes.add(scope); byRoot.put(keys[cursor++], scope); scopePreparation = null;
                    }
                    case 12 -> {
                        if (fragmentKeys == null) fragmentKeys = new int[fragments.size()];
                        int remaining = 512;
                        while (remaining-- > 0 && keyIterator.hasNext()) fragmentKeys[cursor++] = keyIterator.nextInt();
                        if (!keyIterator.hasNext()) {
                            sort = new com.teammoeg.frostedheart.content.climate.thermal.runtime.IntSortCursor(fragmentKeys);
                            phase++; cursor = 0;
                        }
                    }
                    case 13 -> { if (sort.run(deadline)) { phase++; cursor = edge = assignPart = 0; sort = null; } }
                    case 14 -> assignFragments();
                    case 15 -> {
                        int remaining = 32;
                        while (remaining-- > 0 && cursor < bindings.activeAirPortCount()) {
                            if (bindings.resolveAirPort(cursor++, view, portTarget)) {
                                int slot = airAt(air, arena, portTarget.blockX(), portTarget.blockY(), portTarget.blockZ());
                                int root = parent.root(slot);
                                if (root >= 0) byRoot.get(root).ports.merge(BlockPos.asLong(portTarget.blockX(), portTarget.blockY(), portTarget.blockZ()),
                                        (byte) (1 << portTarget.face().ordinal()), (first, second) -> (byte) (first | second));
                            }
                        }
                        if (cursor == bindings.activeAirPortCount()) { phase++; cursor = 0; }
                    }
                    case 16 -> {
                        if (cursor == scopes.size()) return true;
                        if (!scopes.get(cursor).finishSlice(deadline)) return false;
                        cursor++;
                    }
                    default -> throw new IllegalStateException("Unknown regional partition phase");
                }
            } while (System.nanoTime() < deadline);
            return false;
        }

        private void joinFragments() {
            int remaining = 256;
            while (remaining-- > 0) {
                if (fragment == null) {
                    if (!fragmentIterator.hasNext()) { phase++; cursor = 0; return; }
                    fragment = fragmentIterator.next(); edge = fragmentPart = traceSide = tracePatch = 0;
                }
                if (fragmentPart == 0) {
                    var pairs = fragment.airPairs();
                    if (edge == pairs.size()) { fragmentPart = 1; edge = 0; continue; }
                    parent.join(pairs.first(edge), pairs.second(edge)); edge++; continue;
                }
                if (fragmentPart == 2) {
                    var material = fragment.materialContacts();
                    if (edge == material.size()) { fragment = null; continue; }
                    if (material.conductance(edge) != 0) parent.join(material.first(edge), material.second(edge));
                    edge++; continue;
                }
                var routed = fragment.routedContacts();
                if (edge == routed.size()) { fragmentPart = 2; edge = 0; continue; }
                if (routed.conductance(edge) == 0 || routed.routes()[edge].retired()
                        || change != null && change.retiresRoute(routed.routes()[edge])) { edge++; continue; }
                if (traceSide == 0) { parent.join(routed.first(edge), routed.second(edge)); traceSide = 1; }
                var trace = traceSide == 1 ? routed.firstTrace(edge) : routed.secondTrace(edge);
                if (trace == null || tracePatch == trace.size()) {
                    tracePatch = 0;
                    if (++traceSide == 3) { traceSide = 0; edge++; }
                    continue;
                }
                long block = trace.block(tracePatch++);
                parent.join(routed.first(edge), airAt(air, arena, BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block)));
            }
        }

        private void joinRetainedShape(LocalAirShape shape) {
            int first = -1;
            for (int x = shape.minX & ~3; x < shape.minX + shape.sizeX; x += 4)
                for (int y = shape.minY & ~3; y < shape.minY + shape.sizeY; y += 4)
                    for (int z = shape.minZ & ~3; z < shape.minZ + shape.sizeZ; z += 4) {
                        var slots = air.get(BlockPos.asLong(x, y, z)); if (slots == null) continue;
                        for (int slot : slots) if (intersects(arena, slot, shape)) {
                            if (first < 0) first = slot; else parent.join(first, slot);
                        }
                    }
        }

        private void addTouched(int slot) { int root = parent.root(slot); if (root >= 0) touched.add(root); }

        private void assignFragments() {
            int remaining = 256;
            while (remaining-- > 0) {
                if (cursor == fragmentKeys.length) { phase++; cursor = 0; return; }
                int key = fragmentKeys[cursor];
                var fragment = fragments.getOrDefault(key, ThermalFragment.EMPTY);
                var material = fragment.materialContacts();
                if (assignPart == 0) {
                    var pairs = fragment.airPairs();
                    if (edge == pairs.size()) { assignPart++; edge = 0; continue; }
                    addTouched(pairs.first(edge)); addTouched(pairs.second(edge++));
                } else if (assignPart == 1) {
                    var routed = fragment.routedContacts();
                    if (edge == routed.size()) { assignPart++; edge = 0; continue; }
                    addTouched(routed.first(edge)); addTouched(routed.second(edge++));
                } else if (assignPart == 2) {
                    var boundary = fragment.farBoundaries();
                    if (edge == boundary.patchCount()) { assignPart++; edge = 0; continue; }
                    addTouched(boundary.patchCell(edge++));
                } else if (assignPart == 3) {
                    if (edge == material.size()) { assignPart++; edge = 0; continue; }
                    addTouched(material.first(edge)); addTouched(material.second(edge++));
                } else {
                    for (int root : touched) byRoot.get(root).addFragment(key, fragment);
                    touched.clear(); cursor++; assignPart = edge = 0;
                }
            }
        }
    }

    private static boolean dependenciesCurrent(AirFieldLayout layout, AirGeometryView view) {
        for (int index = 0; index < layout.componentCount(); index++) {
            var component = layout.component(index);
            for (int dependency = 0; dependency < component.dependencyCount(); dependency++) {
                var page = view.pageAt(component.dependency(dependency).sectionKey());
                if (page == null || page.handle != component.dependency(dependency)
                        || view.geometryRevision(page) != component.geometryRevision(dependency)) return false;
            }
        }
        return true;
    }

    private static boolean intersects(ThermalCellArena arena, int slot, LocalAirShape shape) {
        long blocks = arena.airBlockMask(slot);
        while (blocks != 0) {
            int block = Long.numberOfTrailingZeros(blocks);
            blocks &= blocks - 1;
            if (shape.containsCell(arena.minimum(slot, 0) + (block & 3), arena.minimum(slot, 1) + (block >>> 4),
                    arena.minimum(slot, 2) + (block >>> 2 & 3))) return true;
        }
        return false;
    }

    private static int airAt(Long2ObjectOpenHashMap<IntArrayList> index, ThermalCellArena arena, int x, int y, int z) {
        var slots = index.get(BlockPos.asLong(x & ~3, y & ~3, z & ~3));
        if (slots == null) return -1;
        int block = (x & 3) | (z & 3) << 2 | (y & 3) << 4;
        for (var iterator = slots.iterator(); iterator.hasNext();) {
            int slot = iterator.nextInt();
            if ((arena.airBlockMask(slot) & 1L << block) != 0) return slot;
        }
        return -1;
    }

    private record Pending(List<ThermalRegion> regions, List<ThermalRegion> prepared) {}

    /** Compact membership and original topology references; no per-region arena-sized arrays. */
    private static final class ScopePreparation {
        long fineVolumes, fineEdges, airComponents;
        final ThermalCellArena arena;
        final AirGeometryView view;
        final int[] cells, generations, profiles;
        final long[] airMasks;
        final MaterialThermalLaw[] laws;
        final LongOpenHashSet sections = new LongOpenHashSet();
        final Long2ObjectOpenHashMap<IntArrayList> air = new Long2ObjectOpenHashMap<>();
        int cursor, minimum = Integer.MAX_VALUE, maximum = Integer.MIN_VALUE;
        Scope result;

        ScopePreparation(ThermalCellArena arena, AirGeometryView view, int[] cells) {
            this.arena = arena; this.view = view; this.cells = cells;
            generations = new int[cells.length]; profiles = new int[cells.length];
            airMasks = new long[cells.length]; laws = new MaterialThermalLaw[cells.length];
        }
        boolean run(long deadline) {
            do {
                int end = Math.min(cells.length, cursor + 512);
                for (; cursor < end; cursor++) {
                    int cell = cells[cursor];
                    sections.add(SectionPos.asLong(arena.minimum(cell, 0) >> 4, arena.minimum(cell, 1) >> 4, arena.minimum(cell, 2) >> 4));
                    generations[cursor] = arena.lifecycleGeneration(cell);
                    profiles[cursor] = arena.isMaterialCell(cell) ? arena.materialProfileId(cell) : -1;
                    if (arena.isMaterialCell(cell)) laws[cursor] = arena.materialLaw(cell);
                    else {
                        airMasks[cursor] = arena.airBlockMask(cell);
                        long mask = airMasks[cursor];
                        int blocks = Long.bitCount(mask); fineVolumes += blocks; airComponents++;
                        fineEdges += mask == -1L ? 300 : 12L * blocks;
                        air.computeIfAbsent(BlockPos.asLong(arena.minimum(cell, 0), arena.minimum(cell, 1), arena.minimum(cell, 2)),
                                ignored -> new IntArrayList()).add(cell);
                    }
                    minimum = Math.min(minimum, arena.minimum(cell, 0));
                    maximum = Math.max(maximum, arena.minimum(cell, 0) + (arena.isAirCell(cell) ? 4 : 1));
                }
                if (cursor == cells.length) { result = new Scope(this); return true; }
            } while (System.nanoTime() < deadline);
            return false;
        }
    }

    static final class Scope implements AirGeometryView {
        private final ThermalCellArena arena;
        private AirGeometryView delegate;
        final int[] cells;
        private final int[] generations, profiles;
        private final long[] airMasks;
        private final MaterialThermalLaw[] laws;
        private final Long2ObjectOpenHashMap<IntArrayList> air;
        private final Long2ByteOpenHashMap ports = new Long2ByteOpenHashMap();
        private final LongOpenHashSet sections;
        LongSet sections() { return sections; }
        private IntArrayList fragmentBuilder = new IntArrayList();
        private ArrayList<ThermalFragment> fragmentValues = new ArrayList<>();
        private int finishCursor;
        int[] fragmentIndexes;
        final long fineVolumes, fineEdges, airComponents;
        long materialContactCount;
        ThermalFragment[] fragments;
        final int minimumX, maximumX;

        private Scope(ScopePreparation prepared) {
            arena = prepared.arena; delegate = prepared.view; cells = prepared.cells;
            generations = prepared.generations; profiles = prepared.profiles;
            airMasks = prepared.airMasks; laws = prepared.laws; sections = prepared.sections; air = prepared.air;
            minimumX = prepared.minimum; maximumX = prepared.maximum;
            fineVolumes = prepared.fineVolumes; fineEdges = prepared.fineEdges; airComponents = prepared.airComponents;
        }

        /** Immutable installed membership with a private, frozen geometry view for quality work. */
        private Scope(Scope original, AirGeometryView view) {
            arena = original.arena; delegate = view; cells = original.cells;
            generations = original.generations; profiles = original.profiles;
            airMasks = original.airMasks; laws = original.laws; sections = original.sections; air = original.air;
            minimumX = original.minimumX; maximumX = original.maximumX;
            fineVolumes = original.fineVolumes; fineEdges = original.fineEdges; airComponents = original.airComponents;
            materialContactCount = original.materialContactCount; ports.putAll(original.ports);
            fragmentIndexes = original.fragmentIndexes; fragments = original.fragments;
            fragmentBuilder = null; fragmentValues = null;
        }

        int capturedSignatureAtWorld(int x, int y, int z) { return delegate.signatureAtWorld(x, y, z); }

        void addFragment(int index, ThermalFragment fragment) {
            fragmentBuilder.add(index); fragmentValues.add(fragment);
        }

        boolean finishSlice(long deadline) {
            if (fragmentBuilder == null) return true;
            if (fragmentIndexes == null) {
                fragmentIndexes = new int[fragmentBuilder.size()];
                fragments = new ThermalFragment[fragmentIndexes.length];
            }
            do {
                int end = Math.min(fragmentIndexes.length, finishCursor + 4096);
                for (; finishCursor < end; finishCursor++) {
                    fragmentIndexes[finishCursor] = fragmentBuilder.getInt(finishCursor);
                    fragments[finishCursor] = fragmentValues.get(finishCursor);
                    materialContactCount += fragments[finishCursor].materialContacts().size();
                }
                if (finishCursor == fragmentIndexes.length) {
                    fragmentBuilder = null; fragmentValues = null;
                    return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }

        boolean sameGeometryInputs(Scope previous) {
            return Arrays.equals(cells, previous.cells) && Arrays.equals(generations, previous.generations)
                    && Arrays.equals(profiles, previous.profiles) && Arrays.equals(airMasks, previous.airMasks)
                    && Arrays.equals(laws, previous.laws) && Arrays.equals(fragmentIndexes, previous.fragmentIndexes)
                    && Arrays.equals(fragments, previous.fragments);
        }

        /** Diagnostic decomposition of the original reuse predicate, never a weaker reuse rule. */
        String inputDifferenceSummary(Scope previous) {
            if (previous == null) return "scopeComparison=unavailable";
            return "scopeComparison=available,airMembershipChanged=" + !sameCellKindInputs(previous, true)
                    + ",materialMembershipChanged=" + !sameCellKindInputs(previous, false)
                    + ",commonMaterialLawChanged=" + commonMaterialLawsDiffer(previous)
                    + ",fragmentSetChanged=" + !Arrays.equals(fragmentIndexes, previous.fragmentIndexes)
                    + ",fragmentIdentityChanged=" + !Arrays.equals(fragments, previous.fragments)
                    + ",portsChanged=" + !ports.equals(previous.ports);
        }

        private boolean sameCellKindInputs(Scope previous, boolean airCells) {
            int first = 0, second = 0;
            while (true) {
                while (first < cells.length && (profiles[first] < 0) != airCells) first++;
                while (second < previous.cells.length && (previous.profiles[second] < 0) != airCells) second++;
                if (first == cells.length || second == previous.cells.length)
                    return first == cells.length && second == previous.cells.length;
                if (cells[first] != previous.cells[second] || generations[first] != previous.generations[second]
                        || airCells && airMasks[first] != previous.airMasks[second]) return false;
                first++; second++;
            }
        }

        private boolean commonMaterialLawsDiffer(Scope previous) {
            int first = 0, second = 0;
            while (first < cells.length && second < previous.cells.length) {
                if (cells[first] < previous.cells[second]) { first++; continue; }
                if (cells[first] > previous.cells[second]) { second++; continue; }
                if (generations[first] == previous.generations[second] && profiles[first] >= 0 && previous.profiles[second] >= 0
                        && (profiles[first] != previous.profiles[second]
                            || !java.util.Objects.equals(laws[first], previous.laws[second]))) return true;
                first++; second++;
            }
            return false;
        }

        boolean mixingChanged(FrozenSources bindings) {
            for (long brick : bindings.mixingChanges()) if (air.containsKey(brick)) return true;
            return false;
        }

        boolean contains(int slot) { return Arrays.binarySearch(cells, slot) >= 0; }
        boolean containsAirAt(long block) { return airAt(air, arena, BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block)) >= 0; }
        boolean containsSection(long section) {
            return sections.contains(section);
        }
        boolean containsShape(LocalAirShape shape) {
            for (int x = shape.minX & ~3; x < shape.minX + shape.sizeX; x += 4)
                for (int y = shape.minY & ~3; y < shape.minY + shape.sizeY; y += 4)
                    for (int z = shape.minZ & ~3; z < shape.minZ + shape.sizeZ; z += 4) {
                        var slots = air.get(BlockPos.asLong(x, y, z));
                        if (slots != null) for (int cell : slots) if (intersects(arena, cell, shape)) return true;
                    }
            return false;
        }

        @Override public boolean ownsCell(ThermalCellArena arena, int slot) { return contains(slot) && delegate.ownsCell(arena, slot); }
        @Override public CellCursor cellCursor(ThermalCellArena arena) {
            return new CellCursor() {
                int cursor;
                @Override public boolean hasNext() { return cursor < cells.length; }
                @Override public int nextSlot() { return cells[cursor++]; }
            };
        }
        @Override public WorkerPageStore.PageState pageAt(long key) { return delegate.pageAt(key); }
        @Override public WorkerPageStore.PageState pageAtSlot(int slot) { return delegate.pageAtSlot(slot); }
        @Override public long geometryRevision(WorkerPageStore.PageState page) { return delegate.geometryRevision(page); }
        @Override public int signatureAtWorld(int x, int y, int z) { return delegate.signatureAtWorld(x, y, z); }
        @Override public int resolveAirFaceTarget(int x, int y, int z, BlockFace face, ThermalSignatureTable signatures,
                WorkerPageStore.MutableAirTarget out) {
            int cell = delegate.resolveAirFaceTarget(x, y, z, face, signatures, out);
            return contains(cell) ? cell : -1;
        }

        void recordBacking(AirBackingMeter meter) {
            meter.array(cells); meter.array(generations); meter.array(profiles); meter.array(airMasks); meter.array(laws);
            meter.array(fragmentIndexes); meter.array(fragments);
            meter.container(air, air.size(), 16); meter.container(ports, ports.size(), 16);
            meter.container(sections, sections.size(), 8);
            for (var slots : air.values()) meter.array(slots.elements());
        }
    }
}
