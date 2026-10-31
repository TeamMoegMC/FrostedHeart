/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;

/** One position-owned transport fragment. Its physical consumers have independent watermarks. */
public record ThermalPageInput(long section, ThermalInputBatch payload) {
    public long boundaryTick() { return payload.targetTick(); }
    public ThermalPageHandle handle() {
        if (payload.admissions().length != 0) return payload.admissions()[0].page();
        if (payload.retirements().length != 0) return payload.retirements()[0].page();
        if (payload.residencyUpdates().length != 0) return payload.residencyUpdates()[0].page();
        if (payload.environmentUpdates().length != 0) return payload.environmentUpdates()[0].page();
        if (payload.geometry().size() != 0) return payload.geometry().page(0);
        if (payload.geometry().materialChanges().size() != 0) return payload.geometry().materialChanges().page(0);
        if (payload.geometry().halos().length != 0) return payload.geometry().halos()[0].page();
        if (payload.phaseIntents().length != 0) return payload.phaseIntents()[0].page();
        return null;
    }
    public boolean structural() {
        return payload.admissions().length != 0 || payload.retirements().length != 0 || payload.residencyUpdates().length != 0
                || !payload.geometry().isEmpty() || payload.environmentUpdates().length != 0
                || payload.phaseAcks().length != 0 || payload.phaseIntents().length != 0;
    }
    public static ThermalPageInput[] split(ThermalInputBatch input) {
        var builders = new Long2ObjectLinkedOpenHashMap<Builder>();
        for (var item : input.admissions()) builder(builders, item.page().sectionKey()).admissions.add(item);
        for (var item : input.retirements()) builder(builders, item.page().sectionKey()).retirements.add(item);
        for (var item : input.residencyUpdates()) builder(builders, item.page().sectionKey()).residencies.add(item);
        for (var item : input.environmentUpdates()) builder(builders, item.page().sectionKey()).environments.add(item);
        for (var item : input.phaseIntents()) builder(builders, item.page().sectionKey()).intents.add(item);
        for (var item : input.phaseAcks()) {
            var request = item.request();
            builder(builders, SectionPos.asLong(request.blockX() >> 4, request.blockY() >> 4, request.blockZ() >> 4)).acks.add(item);
        }
        var geometry = input.geometry();
        for (int index = 0; index < geometry.size(); index++) {
            var into = builder(builders, geometry.page(index).sectionKey()).geometry;
            if (geometry.kind(index) == ResolvedGeometryBatch.Kind.RESOLVED_CENTER)
                into.addResolvedCenter(geometry.page(index), geometry.geometryRevision(index), geometry.blockIndex(index), geometry.signatureId(index));
            else into.addFullResync(geometry.page(index), geometry.geometryRevision(index), geometry.geometryResyncReason(index), geometry.fullPageSignatures(index));
        }
        var materials = geometry.materialChanges();
        for (int index = 0; index < materials.size(); index++)
            builder(builders, materials.page(index).sectionKey()).geometry.addMaterialChange(materials.page(index),
                    materials.blockIndex(index), materials.previousSignature(index), materials.nextSignature(index), materials.cause(index));
        for (var halo : geometry.halos()) builder(builders, halo.page().sectionKey()).geometry.addHalo(halo.page(), halo.geometryRevision(), halo.halo());
        var result = new ThermalPageInput[builders.size()];
        int index = 0;
        for (var entry : builders.long2ObjectEntrySet()) result[index++] = new ThermalPageInput(entry.getLongKey(), entry.getValue().build(input));
        return result;
    }

    private static Builder builder(Long2ObjectLinkedOpenHashMap<Builder> builders, long section) {
        return builders.computeIfAbsent(section, ignored -> new Builder());
    }

    private static final class Builder {
        final ArrayList<ThermalInputBatch.PageAdmission> admissions = new ArrayList<>();
        final ArrayList<ThermalInputBatch.PageRetirement> retirements = new ArrayList<>();
        final ArrayList<ThermalInputBatch.PageResidencyUpdate> residencies = new ArrayList<>();
        final ArrayList<ThermalInputBatch.PageEnvironmentUpdate> environments = new ArrayList<>();
        final ArrayList<ThermalInputBatch.PhaseIntent> intents = new ArrayList<>();
        final ArrayList<ThermalInputBatch.PhaseAck> acks = new ArrayList<>();
        final ResolvedGeometryBatch.Builder geometry = new ResolvedGeometryBatch.Builder();

        ThermalInputBatch build(ThermalInputBatch input) {
            return new ThermalInputBatch(input.dimensionGeneration(), input.sequence(), input.targetTick(),
                    admissions.toArray(ThermalInputBatch.NO_ADMISSIONS), retirements.toArray(ThermalInputBatch.NO_RETIREMENTS),
                    residencies.toArray(ThermalInputBatch.NO_RESIDENCY_UPDATES), geometry.buildAndReset(),
                    environments.toArray(ThermalInputBatch.NO_ENVIRONMENT_UPDATES), acks.toArray(ThermalInputBatch.NO_PHASE_ACKS),
                    input.farFieldConductanceScale(), intents.toArray(ThermalInputBatch.NO_PHASE_INTENTS));
        }
    }
}
