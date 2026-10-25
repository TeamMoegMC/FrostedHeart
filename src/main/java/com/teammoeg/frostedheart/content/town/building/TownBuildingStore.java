package com.teammoeg.frostedheart.content.town.building;

import com.teammoeg.frostedheart.content.town.event.ITownBuildingChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.TownBuildingChangeEvent;
import com.teammoeg.frostedheart.content.town.event.TownBuildingChangeEvent.ChangeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns building objects, recovery records and one lazy encoding cache per building. */
public final class TownBuildingStore {
    public record Issue(BlockPos pos, String type, String reason) {}

    private enum Output {
        PERSISTENCE,
        MANAGEMENT
    }

    private final Map<BlockPos, Entry> records = new LinkedHashMap<>();
    private final Map<BlockPos, AbstractTownBuilding> resolved = new LinkedHashMap<>();
    private final Map<BlockPos, AbstractTownBuilding> readOnlyBuildings = Collections.unmodifiableMap(resolved);
    private final List<CompoundTag> retained = new ArrayList<>();
    private final Set<BlockPos> pendingPositions = new HashSet<>();
    private final List<Issue> viewIssues = new ArrayList<>();
    private ResourceKey<Level> dimension;
    private ITownBuildingChangeEventListener listener;
    private Runnable metadataListener = () -> {};
    private boolean needsInitialSave;
    private boolean hasUnreadableContainer;
    private long metadataVersion;
    private long codecCalls;
    private final Set<BlockPos> restoredPositions = new HashSet<>();

    private static final class Entry {
        AbstractTownBuilding building;
        CompoundTag unknownRecordFields = new CompoundTag();
        CompoundTag unknownBuildingFields = new CompoundTag();
        CompoundTag cachedRecord;
        long changeVersion = 1;
        long encodedVersion;
    }

    public Map<BlockPos, AbstractTownBuilding> resolvedMap() {
        return readOnlyBuildings;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public boolean isPending(BlockPos pos) {
        return pendingPositions.contains(pos);
    }
    public boolean preservesReference(BlockPos pos) {
        return !resolved.containsKey(pos) && (isPending(pos) || hasUnreadableContainer);
    }

    public boolean isRestored(BlockPos pos) {
        return restoredPositions.contains(pos);
    }

    public void finishRestore() {
        restoredPositions.clear();
    }

    public boolean hasRetainedData() {
        return !retained.isEmpty() || !viewIssues.isEmpty();
    }

    public boolean isTrulyEmpty() {
        return records.isEmpty() && !hasRetainedData();
    }

    public long codecCalls() {
        return codecCalls;
    }

    public long metadataVersion() {
        return metadataVersion;
    }

    public List<Issue> issues() {
        List<Issue> result = new ArrayList<>(viewIssues);
        for (CompoundTag tag : retained) {
            CompoundTag raw = tag.getCompound("raw");
            result.add(new Issue(retainedPosition(tag), raw.getCompound("building").getString("type"), tag.getString("reason")));
        }
        return List.copyOf(result);
    }

    public void setListeners(ITownBuildingChangeEventListener listener, Runnable metadataListener) {
        this.listener = listener;
        this.metadataListener = metadataListener == null ? () -> {} : metadataListener;
        records.forEach((pos, entry) -> wire(pos, entry));
    }
    public void reportInitialChanges() {
        if (needsInitialSave) {
            metadataListener.run();
            needsInitialSave = false;
        }
    }
    public boolean setDimension(ResourceKey<Level> next) {
        if (next == null || dimension != null) {
            return false;
        }
        dimension = next;
        metadataChanged();
        return true;
    }

    void loadDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    void markMigrated() {
        needsInitialSave = true;
    }


    public void install(AbstractTownBuilding building) {
        BlockPos pos = building.getPos().immutable();
        if (resolved.get(pos) == building) {
            return;
        }
        removeAt(pos);
        installDecoded(pos, building, new CompoundTag(), new CompoundTag());
        metadataChanged();
        notifyRegistration(building, pos);
    }

    /** Repairs a missing loaded core without discarding its plans or archival evidence. */
    public boolean installMissing(AbstractTownBuilding building) {
        BlockPos pos = building.getPos().immutable();
        if (resolved.containsKey(pos) || isPending(pos)) {
            return false;
        }
        installDecoded(pos, building, new CompoundTag(), new CompoundTag());
        metadataChanged();
        notifyRegistration(building, pos);
        return true;
    }

    void installDecoded(BlockPos pos, AbstractTownBuilding building, CompoundTag outer, CompoundTag payload) {
        Entry entry = new Entry();
        entry.building = building;
        entry.unknownRecordFields = outer.copy();
        entry.unknownBuildingFields = payload.copy();
        records.put(pos, entry);
        resolved.put(pos, building);
        wire(pos, entry);
    }

    public boolean removeAt(BlockPos pos) {
        Entry previous = records.remove(pos);
        if (previous != null) {
            previous.building.setChangeEventListener(null);
        }
        resolved.remove(pos);
        boolean removedRaw = retained.removeIf(tag -> pos.equals(retainedPosition(tag)));
        pendingPositions.remove(pos);
        boolean removedSummary = viewIssues.removeIf(issue -> pos.equals(issue.pos()));
        if (previous == null && !removedRaw && !removedSummary) {
            return false;
        }
        metadataChanged();
        notifyRegistration(this, pos);
        return true;
    }

    private void wire(BlockPos pos, Entry entry) {
        entry.building.setChangeEventListener(new ITownBuildingChangeEventListener() {
            @Override
            public void onBuildingChange(TownBuildingChangeEvent event) {
                if (records.get(pos) != entry || event.getSource() != entry.building) {
                    return;
                }
                entry.changeVersion++;
                if (listener != null) {
                    listener.onBuildingChange(event);
                }
            }
        });
    }

    private void notifyRegistration(Object source, BlockPos pos) {
        if (listener != null) {
            listener.onBuildingChange(new TownBuildingChangeEvent(source, pos, ChangeKind.REGISTRATION));
        }
    }

    private void metadataChanged() {
        metadataVersion++;
        needsInitialSave = true;
        metadataListener.run();
    }

    void addViewIssue(Issue issue) {
        viewIssues.add(issue);
    }

    void retain(CompoundTag tag) {
        if (!retained.contains(tag)) {
            retained.add(tag.copy());
        }
        if ("container".equals(tag.getString("kind"))) {
            hasUnreadableContainer = true;
        }
        BlockPos pos = retainedPosition(tag);
        if (pos != null && "pending".equals(tag.getString("mode"))) {
            pendingPositions.add(pos);
        }
    }
    void archivePendingAt(BlockPos pos) {
        for (CompoundTag tag : retained) {
            if (pos.equals(retainedPosition(tag))) {
                tag.putString("mode", "archive");
            }
        }
        pendingPositions.remove(pos);
        markMigrated();
    }

    public int retainedCount() {
        return retained.size();
    }

    static BlockPos retainedPosition(CompoundTag tag) {
        return BuildingRecordsCodec.decodePosition(tag.get("pos"));
    }

    CompoundTag capturePersistenceFields() {
        return captureFields(Output.PERSISTENCE);
    }

    CompoundTag captureManagementFields() {
        return captureFields(Output.MANAGEMENT);
    }

    private CompoundTag captureFields(Output output) {
        CompoundTag result = new CompoundTag();
        if (dimension != null) {
            result.putString("buildingDimension", dimension.location().toString());
        }
        ListTag blocks = new ListTag();
        records.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(record -> {
            Entry entry = record.getValue();
            if (output == Output.MANAGEMENT) {
                blocks.add(encode(record.getKey(), entry, Output.MANAGEMENT));
            } else {
                if (entry.cachedRecord == null || entry.encodedVersion != entry.changeVersion) {
                    CompoundTag cachedRecord = encode(record.getKey(), entry, Output.PERSISTENCE);
                    entry.cachedRecord = cachedRecord;
                    entry.encodedVersion = entry.changeVersion;
                    codecCalls++;
                }
                blocks.add(entry.cachedRecord);
            }
        });
        result.put("blocks", blocks);
        if (output == Output.MANAGEMENT) {
            ListTag issues = new ListTag();
            for (Issue issue : issues()) {
                CompoundTag tag = new CompoundTag();
                if (issue.pos() != null) {
                    tag.put("pos", BuildingRecordsCodec.encodePosition(issue.pos()));
                }
                tag.putString("type", issue.type());
                tag.putString("reason", issue.reason());
                issues.add(tag);
            }
            result.put("buildingLoadIssues", issues);
        } else {
            ListTag raw = new ListTag();
            retained.forEach(raw::add);
            result.put("retainedBuildingData", raw);
        }
        return result;
    }

    private CompoundTag encode(BlockPos pos, Entry entry, Output output) {
        TownBuildingTypes.Type<?> type = TownBuildingTypes.of(entry.building);
        CompoundTag body = output == Output.PERSISTENCE ? entry.unknownBuildingFields.copy() : new CompoundTag();
        CompoundTag encoded = (CompoundTag) type.encode(entry.building);
        encoded.getAllKeys().forEach(key -> body.put(key, encoded.get(key)));
        body.putString("type", type.id());
        body.put("pos", BuildingRecordsCodec.encodePosition(pos));
        CompoundTag record = output == Output.PERSISTENCE ? entry.unknownRecordFields.copy() : new CompoundTag();
        record.put("pos", BuildingRecordsCodec.encodePosition(pos));
        record.putInt("version", TownBuildingTypes.VERSION);
        record.put("building", body);
        return record;
    }

    public boolean retryPending() {
        List<CompoundTag> pending = retained.stream()
                .filter(tag -> "pending".equals(tag.getString("mode")))
                .map(CompoundTag::copy)
                .toList();
        boolean changed = false;
        for (CompoundTag tag : pending) {
            BlockPos pos = retainedPosition(tag);
            if (pos == null || resolved.containsKey(pos)) {
                continue;
            }
            TownBuildingStore candidate = new TownBuildingStore();
            BuildingRecordsCodec.readEntry(candidate, tag.get("raw"));
            Entry decoded = candidate.records.get(pos);
            if (decoded == null) {
                continue;
            }
            retained.remove(tag);
            restoredPositions.add(pos);
            pendingPositions.remove(pos);
            installDecoded(pos, decoded.building, decoded.unknownRecordFields, decoded.unknownBuildingFields);
            candidate.retained.forEach(this::retain);
            changed = true;
            notifyRegistration(decoded.building, pos);
        }
        if (changed) {
            metadataChanged();
        }
        return changed;
    }
}
