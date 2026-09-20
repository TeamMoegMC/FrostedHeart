/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.LocalAirShape;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.source.CutLoadBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirFieldCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirShapeStore;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;

import java.util.Arrays;

/**
 * One worker's continuous Air and material trial. The engine alone commits the result.
 * Spatial operators are compiled when geometry changes; stable steps reuse all vectors.
 */
public final class ContinuousAirSolver {
    private static final double LOW = 0.5 - 0.5 / Math.sqrt(3);
    private static final double HIGH = 1 - LOW;
    private static final int[] DX = {-1, 1, 0, 0, 0, 0};
    private static final int[] DY = {0, 0, -1, 1, 0, 0};
    private static final int[] DZ = {0, 0, 0, 0, -1, 1};
    private final ThermalCellArena arena;
    private final WorkerPageStore pages;
    private final ThermalSolver geometry;
    private final ThermalTopologyParameters parameters;
    private final MaterialBoundaryRegistry materials;
    private final AirLoadTable airLoads;
    private final AirFieldCompiler compiler = new AirFieldCompiler();
    private final AirShapeStore shapes;
    private final PcgSolver linear = new PcgSolver();
    private AirFieldLayout layout;
    private CoupledThermalOperator operator;
    private int[] materialSlots = new int[0];
    private int[] materialUnknownBySlot = new int[0];
    private double[] stateC = new double[0], candidateC = new double[0], rhsJ = new double[0];
    private double[] powerW = new double[0], contactRatesW = new double[0];
    private double[] materialH = new double[0], candidateH = new double[0];
    private byte[] branches = new byte[0], candidateBranches = new byte[0];
    private int[] loadEvents = new int[0];
    private double airPowerW;
    private double deliveredJ, unacceptedJ;
    private long linearIterations;
    private final Long2ObjectOpenHashMap<RestoredSection> restorations = new Long2ObjectOpenHashMap<>();

    public void acceptAdmissions(ThermalInputBatch batch) {
        for (var retirement : batch.retirements()) restorations.remove(retirement.page().sectionKey());
        for (var admission : batch.admissions()) {
            if (admission.dormantAir() == null) continue;
            restorations.put(admission.page().sectionKey(), new RestoredSection(admission.dormantAir()));
            shapes.restore(admission.dormantAir());
        }
    }

    public ContinuousAirSolver(ThermalCellArena arena, WorkerPageStore pages, ThermalSolver geometry,
            ThermalTopologyParameters parameters, MaterialBoundaryRegistry materials, AirLoadTable airLoads,
            ThermalSignatureTable signatures) {
        this.arena = arena;
        this.pages = pages;
        this.geometry = geometry;
        this.parameters = parameters;
        this.materials = materials;
        this.airLoads = airLoads;
        shapes = new AirShapeStore(signatures);
    }

    public AirFieldLayout layout() { return layout; }
    public double[] coefficientsC() { return stateC; }
    public double deliveredEnergyJ() { return deliveredJ; }
    public double unacceptedEnergyJ() { return unacceptedJ; }
    public long linearIterations() { return linearIterations; }

    /** Consumes the last full spatial field, never the old Brick's scalar display value. */
    public void rebuild(WorkerPhysicalSourceBindings bindings) {
        AirFieldLayout previousLayout = layout;
        double[] previousC = stateC;
        AirOperatorFragment[] previousVolumes = operator == null ? null : operator.volumes;
        AirFieldLayout coarse = compiler.compile(arena, pages);
        layout = coarse.withLocalShapes(shapes.prepare(coarse, previousLayout, previousC, pages, bindings));
        AirOperatorFragment[] volumes = new AirOperatorFragment[layout.componentCount()];
        AirMixingRegion mixing = new AirMixingRegion();
        for (int index = 0; index < volumes.length; index++) {
            var component = layout.component(index);
            mixing.reset(component.minX, component.minY, component.minZ);
            bindings.collectMixingSources(mixing);
            AirOperatorFragment previousVolume = findPreviousVolume(previousLayout, previousVolumes, component);
            volumes[index] = new AirOperatorFragment(component, parameters.effectiveAirCapacityJPerBlockK(),
                    parameters.effectiveMixingWPerBlockK(), mixing, previousVolume);
        }
        IntArrayList bodies = new IntArrayList();
        materialUnknownBySlot = new int[arena.highWaterMark()];
        Arrays.fill(materialUnknownBySlot, -1);
        for (int slot = arena.nextLiveSlot(0); slot >= 0; slot = arena.nextLiveSlot(slot + 1)) {
            if (!arena.isMaterialCell(slot) || !pages.ownsCommittedCell(arena, slot)) continue;
            materialUnknownBySlot[slot] = layout.coefficientCount() + bodies.size();
            bodies.add(slot);
        }
        materialSlots = bodies.toIntArray();
        int size = layout.coefficientCount() + materialSlots.length;
        CoupledThermalOperator.Contacts contacts = compileContacts();
        operator = new CoupledThermalOperator(size, layout, volumes, contacts);
        stateC = new double[size];
        candidateC = new double[size];
        rhsJ = new double[size];
        powerW = new double[size];
        contactRatesW = new double[size];
        materialH = new double[materialSlots.length];
        candidateH = new double[materialSlots.length];
        branches = new byte[materialSlots.length];
        candidateBranches = new byte[materialSlots.length];
        warmStartProjection(previousLayout, previousC);
        readMaterials();
        for (int body = 0; body < materialSlots.length; body++) {
            int index = layout.coefficientCount() + body;
            operator.massJPerK[index] = arena.materialLaw(materialSlots[body]).capacityJPerK();
            rhsJ[index] = operator.massJPerK[index] * stateC[index];
        }
        for (int index = 0; index < layout.componentCount(); index++) {
            var component = layout.component(index);
            long blocks = component.airBlocks;
            while (blocks != 0) {
                int block = Long.numberOfTrailingZeros(blocks);
                blocks &= blocks - 1;
                for (int point = 0; point < 8; point++) {
                    double x = component.minX + (block & 3) + ((point & 1) == 0 ? LOW : HIGH);
                    double y = component.minY + (block >>> 4) + ((point & 4) == 0 ? LOW : HIGH);
                    double z = component.minZ + (block >>> 2 & 3) + ((point & 2) == 0 ? LOW : HIGH);
                    double offsetC = previousLayout == null ? Double.NaN : previousLayout.temperatureOffsetC(x, y, z, previousC);
                    if (!Double.isFinite(offsetC)) {
                        var restored = restorations.get(SectionPos.asLong((int) Math.floor(x) >> 4, (int) Math.floor(y) >> 4, (int) Math.floor(z) >> 4));
                        int brick = (component.minX & 15) >>> 2 | ((component.minZ & 15) >>> 2) << 2 | ((component.minY & 15) >>> 2) << 4;
                        if (restored != null && (restored.consumedBricks & 1L << brick) == 0) {
                            offsetC = restored.cut.temperatureC(x, y, z) - parameters.referenceTemperatureC();
                        }
                    }
                    if (!Double.isFinite(offsetC)) offsetC = arena.temperatureC(component.arenaSlot, parameters.referenceTemperatureC()) - parameters.referenceTemperatureC();
                    for (int basis = 0; basis < component.basisCount(); basis++) rhsJ[component.coefficientIndex(basis)] +=
                            parameters.effectiveAirCapacityJPerBlockK() * 0.125 * component.weight(basis, x, y, z) * offsetC;
                }
            }
        }
        operator.prepareProjection();
        if (!linear.solve(operator, rhsJ, stateC)) throw new IllegalStateException("Continuous Air topology projection did not converge");
        airLoads.installLayout(layout);
        var restoredIterator = restorations.long2ObjectEntrySet().iterator();
        while (restoredIterator.hasNext()) {
            var entry = restoredIterator.next();
            RestoredSection restored = entry.getValue();
            restored.consumedBricks |= pages.capturedBrickMask(entry.getLongKey());
            boolean remaining = false;
            for (int brick = 0; brick < 64; brick++) if (restored.cut.hasBrick(brick) && (restored.consumedBricks & 1L << brick) == 0) { remaining = true; break; }
            if (!remaining) restoredIterator.remove();
        }
    }

    private static final class RestoredSection {
        final ThermalInputBatch.DormantAirCut cut;
        long consumedBricks;
        RestoredSection(ThermalInputBatch.DormantAirCut cut) { this.cut = cut; }
    }

    private static AirOperatorFragment findPreviousVolume(AirFieldLayout previous, AirOperatorFragment[] volumes,
            AirFieldLayout.Component component) {
        if (previous == null) return null;
        int block = Long.numberOfTrailingZeros(component.airBlocks);
        var old = previous.componentAt(component.minX + (block & 3), component.minY + (block >>> 4), component.minZ + (block >>> 2 & 3));
        if (old == null) return null;
        int low = 0, high = volumes.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int slot = volumes[middle].component.arenaSlot;
            if (slot == old.arenaSlot) return volumes[middle];
            if (slot < old.arenaSlot) low = middle + 1;
            else high = middle - 1;
        }
        return null;
    }

    private void warmStartProjection(AirFieldLayout previous, double[] previousC) {
        if (previous == null) return;
        int[] contributions = new int[layout.coarseCount()];
        for (int index = 0; index < layout.componentCount(); index++) {
            var component = layout.component(index);
            int block = Long.numberOfTrailingZeros(component.airBlocks);
            var old = previous.componentAt(component.minX + (block & 3), component.minY + (block >>> 4), component.minZ + (block >>> 2 & 3));
            if (old == null) continue;
            for (int corner = 0; corner < 8; corner++) {
                int coefficient = component.coefficientIndex(corner);
                stateC[coefficient] += previousC[old.coefficientIndex(corner)];
                contributions[coefficient]++;
            }
        }
        for (int index = 0; index < contributions.length; index++) if (contributions[index] > 0) stateC[index] /= contributions[index];
        int oldIndex = 0;
        for (int index = 0; index < layout.localShapeCount(); index++) {
            var shape = layout.localShape(index);
            while (oldIndex < previous.localShapeCount() && compareShape(previous.localShape(oldIndex), shape) < 0) oldIndex++;
            if (oldIndex < previous.localShapeCount() && compareShape(previous.localShape(oldIndex), shape) == 0) {
                stateC[layout.coarseCount() + index] = previousC[previous.coarseCount() + oldIndex];
            }
        }
    }

    private static int compareShape(LocalAirShape first, LocalAirShape second) {
        int position = Long.compare(first.sourceBlock, second.sourceBlock);
        if (position != 0) return position;
        int face = Byte.compare(first.sourceFace, second.sourceFace);
        if (face != 0) return face;
        int branch = Long.compare(first.branch, second.branch);
        return branch != 0 ? branch : Integer.compare(first.scale, second.scale);
    }

    private CoupledThermalOperator.Contacts compileContacts() {
        var contacts = new CoupledThermalOperator.Contacts();
        for (int slot : materialSlots) {
            int bodyIndex = materialUnknownBySlot[slot];
            int x = arena.minimum(slot, 0), y = arena.minimum(slot, 1), z = arena.minimum(slot, 2);
            double conductance = materials.profileOrNull(arena.materialProfileId(slot)).faceConductanceWPerK();
            for (int face = 0; face < 6; face++) {
                var air = layout.componentAt(x + DX[face], y + DY[face], z + DZ[face]);
                if (air == null) continue;
                int axis = face / 2;
                for (int point = 0; point < 4; point++) {
                    double u = (point & 1) == 0 ? LOW : HIGH, v = (point & 2) == 0 ? LOW : HIGH;
                    double px = x + (axis == 0 ? face % 2 : u);
                    double py = y + (axis == 1 ? face % 2 : axis == 0 ? u : v);
                    double pz = z + (axis == 2 ? face % 2 : v);
                    airTerms(contacts, air, px, py, pz, 1);
                    contacts.term(bodyIndex, -1);
                    contacts.finish(conductance * 0.25, bodyIndex, -1, -1, null);
                }
            }
        }
        for (int fragmentIndex = geometry.nextFragmentIndex(0); fragmentIndex >= 0;
                fragmentIndex = geometry.nextFragmentIndex(fragmentIndex + 1)) {
            var materialEdges = geometry.materialExecution(fragmentIndex);
            for (int edge = 0; edge < materialEdges.size(); edge++) {
                int first = materialIndex(materialEdges.first(edge)), second = materialIndex(materialEdges.second(edge));
                if (first < 0 || second < 0) continue;
                contacts.term(first, 1);
                contacts.term(second, -1);
                contacts.finish(materialEdges.conductance(edge), first, second, -1, null);
            }
            ThermalFragment fragment = geometry.fragment(fragmentIndex);
            var routed = fragment.routedContacts();
            for (int edge = 0; edge < routed.size(); edge++) {
                nodeTerms(contacts, routed.first(edge), routed.firstTrace(edge), 1);
                nodeTerms(contacts, routed.second(edge), routed.secondTrace(edge), -1);
                contacts.finish(routed.conductance(edge), materialIndex(routed.first(edge)),
                        materialIndex(routed.second(edge)), -1, routed.routes()[edge]);
            }
            var boundaries = fragment.farBoundaries();
            for (int boundary = 0; boundary < boundaries.patchCount(); boundary++) {
                var component = layout.componentForSlot(boundaries.patchCell(boundary));
                if (component == null) continue;
                long block = boundaries.patchBlock(boundary);
                int x = BlockPos.getX(block), y = BlockPos.getY(block), z = BlockPos.getZ(block);
                for (int point = 0; point < 4; point++) {
                    airTerms(contacts, component, x + ((point & 1) == 0 ? LOW : HIGH), y + 1,
                            z + ((point & 2) == 0 ? LOW : HIGH), 1);
                    contacts.finish(boundaries.patchConductance(boundary) * 0.25, -1, -1, boundaries.pageSlot(), null);
                }
            }
        }
        return contacts;
    }

    private int materialIndex(int slot) {
        return slot >= 0 && slot < materialUnknownBySlot.length ? materialUnknownBySlot[slot] : -1;
    }

    private static void airTerms(CoupledThermalOperator.Contacts contacts, AirFieldLayout.Component component,
            double x, double y, double z, double sign) {
        for (int basis = 0; basis < component.basisCount(); basis++) contacts.term(component.coefficientIndex(basis), sign * component.weight(basis, x, y, z));
    }

    private void nodeTerms(CoupledThermalOperator.Contacts contacts, int slot, ThermalFragment.AirFaceTrace trace, double sign) {
        int material = materialIndex(slot);
        if (material >= 0) contacts.term(material, sign);
        else {
            if (trace == null || trace.size() == 0) throw new IllegalStateException("Continuous route lacks its physical outlet faces");
            double faceWeight = sign / trace.size();
            for (int patch = 0; patch < trace.size(); patch++) {
                long block = trace.block(patch);
                int x = BlockPos.getX(block), y = BlockPos.getY(block), z = BlockPos.getZ(block);
                var component = layout.componentAt(x, y, z);
                if (component == null) throw new IllegalStateException("Continuous route outlet has no captured Air");
                int face = trace.face(patch);
                airTerms(contacts, component, x + 0.5 + DX[face] * 0.5,
                        y + 0.5 + DY[face] * 0.5, z + 0.5 + DZ[face] * 0.5, faceWeight);
            }
        }
    }

    /** Event boundaries are sorted once; each source change scatters only its affected load. */
    public void solveCut(long fromTick, long toTick, CutLoadBuffer loads) {
        deliveredJ = 0;
        unacceptedJ = 0;
        linearIterations = 0;
        if (operator == null || operator.size() == 0) {
            unacceptedJ = loads.pendingEnergyJ();
            return;
        }
        readMaterials();
        Arrays.fill(powerW, 0);
        airPowerW = 0;
        if (loadEvents.length < loads.size() * 2) loadEvents = new int[loads.size() * 2];
        int eventCount = 0;
        for (int record = 0; record < loads.size(); record++) {
            loadEvents[eventCount++] = record * 2;
            if (!loads.impulse(record)) loadEvents[eventCount++] = record * 2 + 1;
        }
        IntArrays.quickSort(loadEvents, 0, eventCount, (first, second) -> {
            int time = Long.compare(eventTick(loads, first), eventTick(loads, second));
            return time != 0 ? time : Integer.compare(first, second);
        });
        long tick = fromTick;
        int event = 0;
        while (tick <= toTick) {
            while (event < eventCount && eventTick(loads, loadEvents[event]) == tick) {
                int encoded = loadEvents[event++], record = encoded >>> 1;
                if (loads.impulse(record)) applyImpulse(loads, record);
                else changePower(loads, record, (encoded & 1) == 0 ? 1 : -1);
            }
            if (tick == toTick) break;
            long next = Math.min(toTick, tick + 20);
            if (event < eventCount) next = Math.min(next, eventTick(loads, loadEvents[event]));
            advanceSeconds((next - tick) / 20.0);
            tick = next;
        }
    }

    private static long eventTick(CutLoadBuffer loads, int event) {
        return (event & 1) == 0 ? loads.startTick(event >>> 1) : loads.endTick(event >>> 1);
    }

    private void changePower(CutLoadBuffer loads, int record, double sign) {
        double watts = sign * loads.amount(record);
        long target = loads.target(record);
        if (SourceBinding.isAirTarget(target)) {
            airLoads.get(SourceBinding.targetIndex(target), loads.generation(record)).add(watts, powerW);
            airPowerW += watts;
        } else {
            int index = materialIndex(SourceBinding.targetIndex(target));
            if (index < 0) throw new IllegalStateException("Material load lost its committed target");
            powerW[index] += watts;
        }
    }

    private void applyImpulse(CutLoadBuffer loads, int record) {
        long target = loads.target(record);
        double joules = loads.amount(record);
        if (SourceBinding.isAirTarget(target)) {
            Arrays.fill(rhsJ, 0);
            airLoads.get(SourceBinding.targetIndex(target), loads.generation(record)).add(joules, rhsJ);
            Arrays.fill(candidateC, 0);
            operator.prepareMass();
            if (!linear.solve(operator, rhsJ, candidateC)) throw new IllegalStateException("Continuous Air impulse did not converge");
            for (int index = 0; index < layout.coefficientCount(); index++) stateC[index] += candidateC[index];
            deliveredJ += joules;
        } else {
            int index = materialIndex(SourceBinding.targetIndex(target));
            int body = index - layout.coefficientCount();
            MaterialThermalLaw law = arena.materialLaw(materialSlots[body]);
            double remaining = joules;
            for (int segment = 0; segment < 3 && remaining != 0 && !operator.waiting[index]; segment++) {
                double direction = Math.signum(remaining);
                branches[body] = law.selectBranch(materialH[body], branches[body], direction);
                double accepted = direction * Math.min(Math.abs(remaining), law.energyLimitJ(materialH[body], branches[body], direction));
                materialH[body] += accepted;
                remaining -= accepted;
                var transition = law.transition(branches[body]);
                operator.waiting[index] |= transition != null && transition.complete(materialH[body]);
                if (accepted == 0) break;
            }
            stateC[index] = law.temperatureC(materialH[body], branches[body]) - parameters.referenceTemperatureC();
            deliveredJ += joules - remaining;
            unacceptedJ += remaining;
        }
    }

    private void readMaterials() {
        int airCount = layout.coefficientCount();
        for (int body = 0; body < materialSlots.length; body++) {
            int slot = materialSlots[body], index = airCount + body;
            materialH[body] = arena.enthalpyJ(slot);
            branches[body] = arena.materialBranch(slot);
            operator.waiting[index] = arena.materialTransitionWaiting(slot);
            stateC[index] = arena.temperatureC(slot, parameters.referenceTemperatureC()) - parameters.referenceTemperatureC();
        }
    }

    private void advanceSeconds(double elapsedSeconds) {
        double remaining = elapsedSeconds;
        double step = elapsedSeconds;
        while (remaining > 1e-12) {
            step = Math.min(step, remaining);
            if (!tryStep(step)) {
                step *= 0.5;
                if (step < 1e-10) throw new IllegalStateException("Continuous Air could not advance its material segment");
                continue;
            }
            remaining -= step;
            step = Math.min(remaining, step * 2);
        }
    }

    private boolean tryStep(double dtSeconds) {
        int airCount = layout.coefficientCount();
        System.arraycopy(stateC, 0, candidateC, 0, stateC.length);
        System.arraycopy(branches, 0, candidateBranches, 0, branches.length);
        operator.refreshContacts(geometry, parameters.referenceTemperatureC());
        for (int outer = 0; outer < 12; outer++) {
            for (int volume = 0; volume < operator.volumes.length; volume++) {
                operator.buoyancyFactors[volume] = operator.volumes[volume].buoyancyFactor(candidateC, parameters.buoyancyParameters());
            }
            operator.materialContactRates(candidateC, contactRatesW);
            for (int body = 0; body < materialSlots.length; body++) {
                int index = airCount + body;
                MaterialThermalLaw law = arena.materialLaw(materialSlots[body]);
                double rateW = contactRatesW[index] + (operator.waiting[index] ? 0 : powerW[index]);
                if (rateW != 0 && !operator.waiting[index]) candidateBranches[body] = law.selectBranch(materialH[body], candidateBranches[body], Math.signum(rateW));
                double slope = law.slopeKPerJ(materialH[body], candidateBranches[body]);
                operator.fixedTemperature[index] = slope == 0 || operator.waiting[index];
                operator.fixedOffsetC[index] = law.temperatureC(materialH[body], candidateBranches[body]) - parameters.referenceTemperatureC();
                operator.massJPerK[index] = slope == 0 ? 0 : 1 / slope;
            }
            operator.prepare(dtSeconds);
            operator.applyMass(stateC, rhsJ);
            for (int index = 0; index < airCount; index++) rhsJ[index] += dtSeconds * powerW[index];
            for (int body = 0; body < materialSlots.length; body++) {
                int index = airCount + body;
                double betaJ = materialH[body] - operator.massJPerK[index] * operator.fixedOffsetC[index];
                rhsJ[index] = materialH[body] - betaJ + (operator.waiting[index] ? 0 : dtSeconds * powerW[index]);
            }
            operator.addKnownTemperaturesToRhs(rhsJ);
            if (!linear.solve(operator, rhsJ, candidateC)) return false;
            linearIterations += linear.iterations();
            operator.materialContactRates(candidateC, contactRatesW);
            boolean branchesChanged = false;
            for (int body = 0; body < materialSlots.length; body++) {
                int index = airCount + body;
                MaterialThermalLaw law = arena.materialLaw(materialSlots[body]);
                double energyJ = operator.waiting[index] ? 0 : dtSeconds * (powerW[index] + contactRatesW[index]);
                byte branch = energyJ == 0 ? candidateBranches[body] : law.selectBranch(materialH[body], candidateBranches[body], Math.signum(energyJ));
                branchesChanged |= branch != candidateBranches[body];
                candidateBranches[body] = branch;
                double limitJ = energyJ == 0 ? Double.POSITIVE_INFINITY : law.energyLimitJ(materialH[body], branch, Math.signum(energyJ));
                double roundingJ = PcgSolver.CORRECTION_TOLERANCE_C * Math.max(1, law.capacityJPerK());
                if (Math.abs(energyJ) > limitJ + roundingJ) return false;
                candidateH[body] = materialH[body] + energyJ;
                // Canonicalize only solver-scale rounding at an endpoint. A macroscopic
                // crossing above already rejected the whole coupled substep.
                if (Double.isFinite(limitJ) && Math.abs(Math.abs(energyJ) - limitJ) <= roundingJ) {
                    candidateH[body] = materialH[body] + Math.signum(energyJ) * limitJ;
                }
            }
            if (branchesChanged) continue;
            double factorChange = 0;
            for (int volume = 0; volume < operator.volumes.length; volume++) factorChange = Math.max(factorChange,
                    Math.abs(operator.volumes[volume].buoyancyFactor(candidateC, parameters.buoyancyParameters()) - operator.buoyancyFactors[volume]));
            if (factorChange > 1e-6) continue;
            deliveredJ += airPowerW * dtSeconds;
            for (int body = 0; body < materialSlots.length; body++) {
                int index = airCount + body;
                if (operator.waiting[index]) unacceptedJ += powerW[index] * dtSeconds;
                else deliveredJ += powerW[index] * dtSeconds;
                materialH[body] = candidateH[body];
                branches[body] = candidateBranches[body];
                MaterialThermalLaw law = arena.materialLaw(materialSlots[body]);
                var transition = law.transition(branches[body]);
                operator.waiting[index] |= transition != null && transition.complete(materialH[body]);
            }
            System.arraycopy(candidateC, 0, stateC, 0, stateC.length);
            return true;
        }
        return false;
    }

    /** Official writes occur only after every time interval in this cut was accepted. */
    public void commit() {
        for (int body = 0; body < materialSlots.length; body++) {
            arena.commitMaterialState(materialSlots[body], materialH[body], branches[body]);
        }
        // Existing residency and geometry migration consume component means. Queries read the
        // continuous coefficients; these means are derived summaries, never an Air authority.
        if (layout == null) return;
        pages.setAirField(layout, stateC);
        for (int index = 0; index < layout.componentCount(); index++) {
            var component = layout.component(index);
            double offsetSumC = 0;
            long blocks = component.airBlocks;
            while (blocks != 0) {
                int block = Long.numberOfTrailingZeros(blocks);
                blocks &= blocks - 1;
                offsetSumC += component.temperatureOffsetC(component.minX + (block & 3) + 0.5,
                        component.minY + (block >>> 4) + 0.5, component.minZ + (block >>> 2 & 3) + 0.5, stateC);
            }
            arena.setEnthalpyJ(component.arenaSlot, parameters.effectiveAirCapacityJPerBlockK() * offsetSumC);
        }
    }
}
