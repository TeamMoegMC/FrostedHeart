/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.*;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import net.minecraft.core.SectionPos;
import java.util.Arrays;

/** Whole-block connectivity compiler with one-node full-Air fast paths. */
public final class BrickTopologyCompiler {
    private final ThermalCellArena arena;
    private final ThermalSignatureTable signatures;
    private final MaterialBoundaryRegistry materials;
    private final ThermalTopologyParameters parameters;
    private final FarFieldSettings farField;
    private final int maximumArenaSlots;
    private final ThermalBrickCellLayout cells = new ThermalBrickCellLayout();
    private final int[] ids = new int[64], exposure = new int[64], phaseIds = new int[64];
    private final byte[] mapping = new byte[64];
    private final long[] masks = new long[64], phaseMasks = new long[64];
    private final PrimitiveTopologyScratch.LongPairDouble airPairs = new PrimitiveTopologyScratch.LongPairDouble();
    private final PrimitiveTopologyScratch.LongPairDouble materialPairs = new PrimitiveTopologyScratch.LongPairDouble();
    private final PrimitiveTopologyScratch.LongPairDouble phasePairs = new PrimitiveTopologyScratch.LongPairDouble();
    private final PrimitiveTopologyScratch.LongPairDouble farBoundaries = new PrimitiveTopologyScratch.LongPairDouble();
    private boolean fragmentResolved;
    private static final long[] NEIGHBORS = new long[64];
    static {
        for (int b=0;b<64;b++) {
            long m=0;
            if ((b&3)>0) m|=1L<<(b-1); if ((b&3)<3) m|=1L<<(b+1);
            if ((b>>>2&3)>0) m|=1L<<(b-4); if ((b>>>2&3)<3) m|=1L<<(b+4);
            if ((b>>>4)>0) m|=1L<<(b-16); if ((b>>>4)<3) m|=1L<<(b+16);
            NEIGHBORS[b]=m;
        }
    }
    public BrickTopologyCompiler(ThermalCellArena arena, ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials, ThermalTopologyParameters parameters,
            FarFieldSettings farField, int maximumArenaSlots) {
        this.arena=arena; this.signatures=signatures; this.materials=materials;
        this.parameters=parameters; this.farField=farField; this.maximumArenaSlots=maximumArenaSlots;
    }

    WorkerBrickTopology compileCells(WorkerPageStore.PageState page, PageSignatures next,
            int brick, TopologyView view) {
        int x=brickMinX(page,brick), y=brickMinY(page,brick), z=brickMinZ(page,brick);
        cells.reset(x,y,z);
        long mergeable=0;
        for (int b=0;b<64;b++) {
            ids[b]=next.get(BlockBrickLayout.pageBlock(brick,b));
            if (!signatures.valid(ids[b])) return WorkerBrickTopology.EMPTY;
            if (signatures.mergeable(ids[b])) mergeable|=1L<<b;
        }
        int nodes=0, transport=0, phases=0;
        BlockBrickLayout layout=null;
        if (mergeable == -1L) {
            cells.setRegularAir(parameters.effectiveAirCapacityJPerBlockK());
            transport=1;
        } else {
            Arrays.fill(mapping,(byte)255);
            while (mergeable!=0) {
                long pending=Long.lowestOneBit(mergeable), members=0;
                while (pending!=0) {
                    int b=Long.numberOfTrailingZeros(pending);
                    pending &= pending-1;
                    if ((mergeable & 1L<<b)==0) continue;
                    mergeable &= ~(1L<<b); members|=1L<<b;
                    pending |= NEIGHBORS[b] & mergeable;
                }
                mapNode(nodes,members);
                cells.setTransportCapacity(nodes++,Long.bitCount(members)*parameters.effectiveAirCapacityJPerBlockK());
            }
            for (int b=0;b<64;b++) {
                int profile=signatures.materialProfileId(ids[b]);
                exposure[b]=profile==0 ? 0 : exposedFaces(b,x,y,z,view);
                if (signatures.ventilation(ids[b])==0 || mapping[b]!=(byte)255) continue;
                mapNode(nodes,1L<<b);
                double capacity=profile==0 || exposure[b]==0 ? parameters.effectiveAirCapacityJPerBlockK()
                        : materials.profileOrNull(profile).surfaceCapacityJPerK()*exposure[b];
                cells.setTransportCapacity(nodes++,capacity);
            }
            transport=nodes;
            for (int b=0;b<64;b++) {
                int id=signatures.materialProfileId(ids[b]);
                if (id==0 || exposure[b]==0 || signatures.ventilation(ids[b])>0) continue;
                var profile=materials.profileOrNull(id);
                if (profile.model()==MaterialBoundaryRegistry.Model.PHASE_RESERVOIR) {
                    int i=0; while (i<phases && phaseIds[i]!=id) i++;
                    if (i==phases) { phaseIds[phases]=id; phaseMasks[phases++]=0; }
                    phaseMasks[i]|=1L<<b;
                } else {
                    mapNode(nodes++,1L<<b);
                    cells.addMaterialPole(x+(b&3),y+(b>>>4),z+(b>>>2&3),
                            profile.surfaceCapacityJPerK()*exposure[b],view.naturalTemperature(page));
                }
            }
            for (int i=0;i<phases;i++) {
                var profile=materials.profileOrNull(phaseIds[i]);
                mapNode(nodes++,phaseMasks[i]);
                cells.addPhaseReservoir(x,y,z,profile.id(),phaseMasks[i],
                        profile.transitionTemperatureC(),profile.transitionEnergyJPerUnit());
            }
            if (nodes>0) {
                layout=new BlockBrickLayout(mapping.clone(),Arrays.copyOf(masks,nodes),transport);
                if (transport>0) cells.setMixedAir(layout,parameters.effectiveAirCapacityJPerBlockK());
            }
        }
        var allocation=arena.stageBrickCells(page.pageSlot,page.lifecycleGeneration,cells,
                view.naturalTemperature(page),parameters.referenceTemperatureC(),maximumArenaSlots);
        if (allocation==null) throw new TopologyPlan.WorkLimitedException("thermal arena slot limit reached");
        try {
            int coverage=transport==0 ? -1 : allocation.cellSpan().firstSlot();
            var candidates=phases==0 ? PagePublication.PhaseCandidates.EMPTY
                    : PagePublication.PhaseCandidates.owned(Arrays.copyOf(phaseIds,phases),Arrays.copyOf(phaseMasks,phases));
            return new WorkerBrickTopology(allocation.cellSpan(),coverage,coverage<0?0:arena.lifecycleGeneration(coverage),
                    layout,transport,candidates,allocation.phaseReservoirSlots(),true,true);
        } catch (RuntimeException | Error failure) {
            arena.discardStagedCells(allocation.cellSpan()); throw failure;
        }
    }
    private void mapNode(int node,long members) {
        masks[node]=members;
        while(members!=0) { int b=Long.numberOfTrailingZeros(members); members&=members-1; mapping[b]=(byte)node; }
    }

    private int exposedFaces(int block, int x, int y, int z, TopologyView view) {
        int count = 0;
        long neighbors = NEIGHBORS[block];
        while (neighbors != 0) {
            int neighbor = Long.numberOfTrailingZeros(neighbors);
            neighbors &= neighbors - 1;
            if (signatures.ventilation(ids[neighbor]) > 0) count++;
        }
        int bx = x + (block & 3), by = y + (block >>> 4), bz = z + (block >>> 2 & 3);
        if ((block & 3) == 0 && signatures.ventilation(view.signatureAtWorld(bx-1,by,bz)) > 0) count++;
        if ((block & 3) == 3 && signatures.ventilation(view.signatureAtWorld(bx+1,by,bz)) > 0) count++;
        if ((block >>> 4) == 0 && signatures.ventilation(view.signatureAtWorld(bx,by-1,bz)) > 0) count++;
        if ((block >>> 4) == 3 && signatures.ventilation(view.signatureAtWorld(bx,by+1,bz)) > 0) count++;
        if ((block >>> 2 & 3) == 0 && signatures.ventilation(view.signatureAtWorld(bx,by,bz-1)) > 0) count++;
        if ((block >>> 2 & 3) == 3 && signatures.ventilation(view.signatureAtWorld(bx,by,bz+1)) > 0) count++;
        return count;
    }

    CompiledFragment compileFragment(WorkerPageStore.PageState page,int brick,TopologyView view) {
        airPairs.reset(); materialPairs.reset(); phasePairs.reset(); farBoundaries.reset(); fragmentResolved=true;
        var owner=view.brick(page,brick);
        if (!owner.cellsResolved) return new CompiledFragment(ThermalFragment.EMPTY,false);
        int x=brickMinX(page,brick), y=brickMinY(page,brick), z=brickMinZ(page,brick);
        PageSignatures cut = view.signatures(page);
        if (owner.blockLayout!=null) {
            for (int b=0;b<64;b++) ids[b]=cut.get(BlockBrickLayout.pageBlock(brick,b));
            for(int b=0;b<64;b++) {
                if((b&3)<3) face(owner,b,owner,b+1,0,x+(b&3)+1,ids[b],ids[b+1]);
                if((b>>>4)<3) face(owner,b,owner,b+16,1,y+(b>>>4)+1,ids[b],ids[b+16]);
                if((b>>>2&3)<3) face(owner,b,owner,b+4,2,z+(b>>>2&3)+1,ids[b],ids[b+4]);
            }
        }
        for (int axis = 0; axis < 3; axis++) {
            int nx = x + (axis == 0 ? 4 : 0);
            int ny = y + (axis == 1 ? 4 : 0);
            int nz = z + (axis == 2 ? 4 : 0);
            var neighborPage = view.page(SectionPos.asLong(
                    SectionPos.blockToSectionCoord(nx), SectionPos.blockToSectionCoord(ny),
                    SectionPos.blockToSectionCoord(nz)));
            if (neighborPage == null) continue;
            int neighborBrick = (nx & 15) >>> 2 | ((nz & 15) >>> 2) << 2 | ((ny & 15) >>> 2) << 4;
            if (!view.resident(neighborPage, neighborBrick)) continue;
            var neighbor = view.brick(neighborPage, neighborBrick);
            if (!neighbor.cellsResolved) {
                fragmentResolved = false;
                continue;
            }
            int plane = axis == 0 ? nx : axis == 1 ? ny : nz;
            if (owner.blockLayout == null && neighbor.blockLayout == null
                    && owner.coverageSlot >= 0 && neighbor.coverageSlot >= 0) {
                addAirPair(owner.coverageSlot, neighbor.coverageSlot, axis, plane, 16, 100, 100);
                continue;
            }
            PageSignatures neighborCut = neighborPage == page ? cut : view.signatures(neighborPage);
            for (int i = 0; i < 16; i++) {
                int left = BlockBrickLayout.faceBlock(axis, 3, i);
                int right = BlockBrickLayout.faceBlock(axis, 0, i);
                int leftId = cut.get(BlockBrickLayout.pageBlock(brick, left));
                int rightId = neighborCut.get(BlockBrickLayout.pageBlock(neighborBrick, right));
                face(owner, left, neighbor, right, axis, plane, leftId, rightId);
            }
        }
        // Existing FarField eligibility is direct sky at the absent upper Page.
        if ((brick>>>4)==3 && view.brickAtWorld(x,y+4,z)==null && owner.coverageSlot>=0) {
            for(int i=0;i<16;i++) {
                int b=BlockBrickLayout.faceBlock(1,3,i), slot=owner.transportSlot(b);
                int column=((x+(b&3))&15)|((z+(b>>>2&3))&15)<<4;
                if(slot<0 || view.firstExposedLocalY(page,column)>15) continue;
                int v=signatures.ventilation(cut.get(BlockBrickLayout.pageBlock(brick,b)));
                farBoundaries.add(slot,0,farField.conductanceForPatches(16,true)*v/100.0);
            }
        }
        return new CompiledFragment(new ThermalFragment(Integer.toUnsignedLong(page.fragmentIndex(brick)),
                freezeAirPairs(),freezeMaterialPairs(),freezePhasePairs(),freezeFarBoundaries(page.pageSlot)),fragmentResolved);
    }
    private void face(WorkerBrickTopology a,int ba,WorkerBrickTopology b,int bb,int axis,int plane,int ia,int ib) {
        int sa=a.slotAt(ba), sb=b.slotAt(bb);
        if(sa<0 || sb<0 || sa==sb) return;
        int va=signatures.ventilation(ia), vb=signatures.ventilation(ib);
        if(va>0 && vb>0) { addAirPair(sa,sb,axis,plane,1,va,vb); return; }
        if(va==0 && vb==0) return;
        int solid=va==0?sa:sb, air=va==0?sb:sa;
        var profile=materials.profileOrNull(signatures.materialProfileId(va==0?ia:ib));
        if(profile==null) return;
        if(profile.model()==MaterialBoundaryRegistry.Model.PHASE_RESERVOIR)
            phasePairs.add(air,solid,profile.faceConductanceWPerK());
        else materialPairs.add(Math.min(air,solid),Math.max(air,solid),profile.faceConductanceWPerK());
    }
    private void addAirPair(int first,int second,int axis,int plane,double area,int va,int vb) {
        double da=Math.max(0.5,plane-arena.center(first,axis));
        double db=Math.max(0.5,arena.center(second,axis)-plane);
        airPairs.add(Math.min(first,second),Math.max(first,second),area/(da*100.0/va+db*100.0/vb));
    }
    private ThermalFragment.MaterialContributions freezeMaterialPairs() {
        int n=materialPairs.size(); if(n==0) return ThermalFragment.MaterialContributions.EMPTY;
        int[] a=new int[n],b=new int[n]; double[] g=new double[n];
        for(int i=0;i<n;i++){ a[i]=(int)materialPairs.first(i); b[i]=(int)materialPairs.second(i); g[i]=materialPairs.value(i); }
        return new ThermalFragment.MaterialContributions(a,b,g);
    }
    private ThermalFragment.PhaseContacts freezePhasePairs() {
        int n=phasePairs.size(); if(n==0) return ThermalFragment.PhaseContacts.EMPTY;
        int[] a=new int[n],b=new int[n]; double[] g=new double[n];
        for(int i=0;i<n;i++){ a[i]=(int)phasePairs.first(i); b[i]=(int)phasePairs.second(i); g[i]=phasePairs.value(i); }
        return new ThermalFragment.PhaseContacts(a,b,g);
    }
    private ThermalFragment.AirPairs freezeAirPairs() {
        int count = airPairs.size();
        if (count == 0) {
            return ThermalFragment.AirPairs.EMPTY;
        }
        int[] first = new int[count];
        int[] second = new int[count];
        double[] conductance = new double[count];
        double[] firstY = new double[count];
        double[] secondY = new double[count];
        for (int index = 0; index < count; index++) {
            first[index] = (int) airPairs.first(index);
            second[index] = (int) airPairs.second(index);
            conductance[index] = parameters.effectiveMixingWPerBlockK()
                    * airPairs.value(index);
            firstY[index] = arena.center(first[index], 1);
            secondY[index] = arena.center(second[index], 1);
        }
        return new ThermalFragment.AirPairs(
                first, second, conductance, firstY, secondY);
    }

    private ThermalFragment.FarBoundaries freezeFarBoundaries(
            int ownerPageSlot
    ) {
        int count = farBoundaries.size();
        if (count == 0) {
            return ThermalFragment.FarBoundaries.EMPTY;
        }
        int[] cell = new int[count];
        double[] conductance = new double[count];
        double[] coefficient = new double[count];
        for (int index = 0; index < count; index++) {
            cell[index] = (int) farBoundaries.first(index);
            conductance[index] = farBoundaries.value(index);
        }
        return new ThermalFragment.FarBoundaries(
                cell, ownerPageSlot, conductance, coefficient);
    }

    private static int brickMinX(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.x(page.handle.sectionKey()))
                + ((brick & 3) << 2);
    }

    private static int brickMinY(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.y(page.handle.sectionKey()))
                + ((brick >>> 4 & 3) << 2);
    }

    private static int brickMinZ(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.z(page.handle.sectionKey()))
                + ((brick >>> 2 & 3) << 2);
    }

    record CompiledFragment(
            ThermalFragment fragment,
            boolean resolved
    ) {
    }
}
