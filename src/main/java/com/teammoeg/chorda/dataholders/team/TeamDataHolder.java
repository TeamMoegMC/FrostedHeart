/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/*
 * 
 */
package com.teammoeg.chorda.dataholders.team;

import com.teammoeg.chorda.dataholders.DataHolderMap;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.chorda.dataholders.PersistentComponent;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator;
import com.teammoeg.chorda.events.TeamLoadedEvent;
import com.teammoeg.chorda.network.CBaseNetwork;
import com.teammoeg.chorda.network.CMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.function.Consumer;

/**
 * 团队的数据持有者，继承自 {@link DataHolderMap}。
 * 存储团队的所有数据组件，并提供成员管理、网络同步和序列化功能。
 * <p>
 * Data holder for teams, extending {@link DataHolderMap}.
 * Stores all data components for a team and provides member management, network synchronization, and serialization capabilities.
 */
public class TeamDataHolder extends DataHolderMap<TeamDataHolder> {
    
    /** Frosted Heart 团队 ID。 / The Frosted Heart team ID. */
    private UUID id;
    
    /** 所有者的玩家名称。 / The player name of the owner. */
    private String ownerName;
    
    /** 关联的模组团队实例。 / The associated modded team instance. */
    private AbstractTeam team;

    // Kept even while the external provider cannot resolve this team.
    private UUID externalTeamId;
    private CompoundTag unknownRoot = new CompoundTag();
    private final Set<UUID> supersededHolderIds = new LinkedHashSet<>();
    private PersistenceCoordinator.RecordState recordState;
    private TeamRecordExtension extension = TeamRecordExtension.NONE;
    private final Map<SpecialDataType<?>, ComponentSlot> componentSlots = new IdentityHashMap<>();

    private static final class ComponentSlot {
        long revision = 1;
        long encodedRevision;
        Tag frozen;
    }
	
    /** 标记此团队是否已正确加载。 / Flag indicating whether this team has been correctly loaded. */
    private boolean isLoaded;
	/**
	 * 创建一个新的团队数据持有者。
	 * <p>
	 * Creates a new team data holder.
	 *
	 * @param id Frosted Heart 团队 ID / the Frosted Heart team ID
	 * @param team 关联的团队 / the associated team
	 */
	public TeamDataHolder(UUID id,AbstractTeam team) {
		super("TeamData");
		this.team=team;
		this.id=id;
		this.externalTeamId = team == null ? null : team.getId();
	}
	
	/**
	 * {@inheritDoc}
	 * 序列化团队数据到 NBT，包括所有者名称、团队 UUID 和团队 ID。
	 * <p>
	 * Serializes team data to NBT, including owner name, team UUID, and team ID.
	 */
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		for (String key : unknownRoot.getAllKeys()) nbt.put(key, unknownRoot.get(key).copy());
		super.save(nbt, isPacket);
        saveIdentity(nbt);
        if (recordState != null) {
            extension.capturePart(this, nbt);
            for (String key : extension.ownedRootKeys()) {
                if (nbt.contains(key)) nbt.put(key, nbt.get(key).copy());
            }
        }
	}
	
	/**
	 * {@inheritDoc}
	 * 从 NBT 反序列化团队数据，包括所有者名称和团队 UUID。
	 * <p>
	 * Deserializes team data from NBT, including owner name and team UUID.
	 */
	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
        unknownRoot = new CompoundTag();
        Set<String> known = Set.of("data", "uuid", "owner", "teamId", "supersededHolderIds", "storageVersion", "deleted", "captureTick");
        for (String key : nbt.getAllKeys()) {
            if (!known.contains(key) && !extension.ownedRootKeys().contains(key)) unknownRoot.put(key, nbt.get(key).copy());
        }
        if (nbt.contains("owner"))
            ownerName = nbt.getString("owner");
        if (nbt.contains("uuid"))
            id = nbt.getUUID("uuid");
        if (nbt.hasUUID("teamId")) externalTeamId = nbt.getUUID("teamId");
        supersededHolderIds.clear();
        for (Tag tag : nbt.getList("supersededHolderIds", Tag.TAG_INT_ARRAY)) {
            UUID superseded = NbtUtils.loadUUID(tag);
            if (!id.equals(superseded)) supersededHolderIds.add(superseded);
        }
        super.load(nbt, isPacket);
	}
	/**
	 * 如果尚未加载，则触发加载事件。仅在首次调用时触发 {@link TeamLoadedEvent}。
	 * <p>
	 * Triggers the load event if not yet loaded. Only fires {@link TeamLoadedEvent} on the first call.
	 */
	public void loadIfNeeded() {
		if(!isLoaded) {
			isLoaded=true;
			MinecraftForge.EVENT_BUS.post(new TeamLoadedEvent(this));
		}
	}
	/**
	 * 对每个在线成员执行指定操作。
	 * <p>
	 * Executes the specified action for each online member.
	 *
	 * @param consumer 对每个在线玩家执行的操作 / the action to perform on each online player
	 */
	public void forEachOnline(Consumer<ServerPlayer> consumer) {
		for (ServerPlayer spe : getOnlineMembers())
        	consumer.accept(spe);
	}
	
	/**
	 * 向所有在线成员发送网络数据包。
	 * <p>
	 * Sends a network packet to all online members.
	 *
	 * @param network 网络实例 / the network instance
	 * @param packet 要发送的数据包 / the packet to send
	 */
	public void sendToOnline(CBaseNetwork network,CMessage packet) {
        for (ServerPlayer spe : getOnlineMembers())
        	network.sendPlayer(spe, packet);
	}
    /**
     * 获取 Frosted Heart 团队 ID。
     * <p>
     * Gets the Frosted Heart team ID.
     *
     * @return 团队 ID / the team ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取团队所有者的玩家名称。
     * <p>
     * Gets the player name of the team owner.
     *
     * @return 所有者名称 / the owner's name
     */
    public String getOwnerName() {
        return ownerName;
    }
    /**
     * 获取关联的团队实例。
     * <p>
     * Gets the associated team instance.
     *
     * @return 团队实例 / the team instance
     */
    public AbstractTeam getTeam() {
        return team;
    }
    
    /**
     * 设置团队所有者的玩家名称。
     * <p>
     * Sets the player name of the team owner.
     *
     * @param ownerName 所有者名称 / the owner's name
     */
    public void setOwnerName(String ownerName) {
        if (Objects.equals(this.ownerName, ownerName)) return;
        this.ownerName = ownerName;
        markPersistentChanged();
    }

    /**
     * 设置关联的团队实例。
     * <p>
     * Sets the associated team instance.
     *
     * @param team 团队实例 / the team instance
     */
    public void setTeam(AbstractTeam team) {
        this.team = team;
        if (team != null && !Objects.equals(externalTeamId, team.getId())) {
            externalTeamId = team.getId();
            markPersistentChanged();
        }
    }
	
	/**
	 * 获取所有在线成员。
	 * <p>
	 * Gets all online members.
	 *
	 * @return 在线成员集合 / the collection of online members
	 */
	public Collection<ServerPlayer> getOnlineMembers() {
		return team == null ? List.of() : team.getOnlineMembers();
	}
	Map<SpecialDataType,TeamDataClosure> dataHolderCache=new HashMap<>();
	/**
	 * 获取指定数据类型的 {@link TeamDataClosure}，用于延迟获取数据。使用缓存避免重复创建。
	 * <p>
	 * Gets a {@link TeamDataClosure} for the specified data type, for lazy data retrieval. Uses caching to avoid redundant creation.
	 *
	 * @param <U> 数据组件类型 / the data component type
	 * @param cap 数据组件的类型定义 / the data component type definition
	 * @return 团队数据闭包 / the team data closure
	 */
	public synchronized <U extends SpecialData> TeamDataClosure<U> getDataHolder(SpecialDataType<U> cap){
		return dataHolderCache.computeIfAbsent(cap, t->new TeamDataClosure<>(this,t));
	}

    public UUID getExternalTeamId() { return externalTeamId; }
    void rememberExternalTeamId(UUID id) { externalTeamId = id; }
    public boolean isServerBound() { return recordState != null; }
    public PersistenceCoordinator.RecordState recordState() { return recordState; }

    /** Called only by the server manager, before loading or posting creation events. */
    void bindServer(PersistenceCoordinator.RecordState state, TeamRecordExtension extension) {
        this.recordState = state;
        this.extension = extension;
    }

    public void markPersistentChanged() {
        if (recordState != null) recordState.markChanged();
    }

    public void invalidateComponent(SpecialDataType<?> type) {
        if (recordState == null) return;
        componentSlots.computeIfAbsent(type, ignored -> new ComponentSlot()).revision++;
        markPersistentChanged();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected boolean deferComponentInstallation(SpecialDataType<?> type, SpecialData next, InstallReason reason) {
        return recordState != null && extension.deferIfBusy(this, () -> {
            if (next == null) removeData(type);
            else setData((SpecialDataType) type, next);
        });
    }

    @Override
    protected void onComponentInstalled(SpecialDataType<?> type, SpecialData previous,
                                        SpecialData next, InstallReason reason) {
        if (recordState == null) return;
        if (previous instanceof PersistentComponent tracked) tracked.setPersistenceListener(null);
        componentSlots.remove(type);
        if (next != null) {
            componentSlots.put(type, new ComponentSlot());
            if (next instanceof PersistentComponent tracked) {
                tracked.setPersistenceListener(() -> {
                    if (recordState != null && getOptional(type).orElse(null) == next) invalidateComponent(type);
                });
            }
        }
        markPersistentChanged();
        // LOAD is bound once the complete holder and extension state are present.
        if (reason != InstallReason.LOAD) extension.componentInstalled(this, type, previous, next, reason);
    }

    void prepareCheckpoint() {
        for (SpecialDataType<?> type : getTypes()) {
            SpecialData component = getOptional(type).orElse(null);
            if (!(component instanceof PersistentComponent)) {
                // Compatibility components may expose mutable nested data. Capture
                // them each checkpoint, but do not rewrite an equal frozen value.
                ComponentSlot slot = componentSlots.computeIfAbsent(type, ignored -> new ComponentSlot());
                Tag encoded = encodeComponent(type, component);
                if (!encoded.equals(slot.frozen)) {
                    slot.revision++;
                    slot.frozen = encoded;
                    slot.encodedRevision = slot.revision;
                    markPersistentChanged();
                }
            }
        }
    }

    CompoundTag captureRecord() {
        CompoundTag root = unknownRoot.copy();
        CompoundTag encoded = copyUnknownComponents();
        for (SpecialDataType<?> type : getTypes()) {
            ComponentSlot slot = componentSlots.computeIfAbsent(type, ignored -> new ComponentSlot());
            if (slot.frozen == null || slot.encodedRevision != slot.revision) {
                slot.frozen = encodeComponent(type, getOptional(type).orElseThrow());
                slot.encodedRevision = slot.revision;
            }
            // Frozen component trees are private to persistence and never edited.
            encoded.put(type.getId(), slot.frozen);
        }
        root.put("data", encoded);
        saveIdentity(root);
        extension.capturePart(this, root);
        return root;
    }

    private void saveIdentity(CompoundTag root) {
        root.putUUID("uuid", id);
        if (externalTeamId != null) root.putUUID("teamId", externalTeamId);
        if (ownerName != null) root.putString("owner", ownerName);
        ListTag superseded = new ListTag();
        for (UUID previous : supersededHolderIds) superseded.add(NbtUtils.createUUID(previous));
        root.put("supersededHolderIds", superseded);
    }

    void supersede(TeamDataHolder previous) {
        supersededHolderIds.add(previous.getId());
        supersededHolderIds.addAll(previous.supersededHolderIds);
        supersededHolderIds.remove(id);
        markPersistentChanged();
    }

    CompoundTag deletionRecord() {
        CompoundTag root = new CompoundTag();
        saveIdentity(root);
        root.putBoolean("deleted", true);
        return root;
    }

    void unbindServer() {
        for (SpecialDataType<?> type : getTypes()) {
            if (getOptional(type).orElse(null) instanceof PersistentComponent tracked) tracked.setPersistenceListener(null);
        }
        recordState = null;
        extension = TeamRecordExtension.NONE;
        componentSlots.clear();
    }

}
