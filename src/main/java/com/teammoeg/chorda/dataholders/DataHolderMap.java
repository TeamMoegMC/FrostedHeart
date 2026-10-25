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

package com.teammoeg.chorda.dataholders;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.teammoeg.chorda.io.NBTSerializable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * Component slots keyed by registered type identity. All installation paths share
 * one lifecycle hook. The lock protects slots, not the mutable component contents;
 * server gameplay and snapshot capture still belong to the server thread.
 */
public class DataHolderMap<T extends DataHolderMap<T>> implements SpecialDataHolder<T>, NBTSerializable {
    public enum InstallReason { LOAD, CREATE, REPLACE, REMOVE, RESTORE }

    public final Marker marker;
    private final Map<SpecialDataType<?>, SpecialData> data = new IdentityHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private CompoundTag unknownComponents = new CompoundTag();

    public DataHolderMap(String markerName) {
        marker = MarkerManager.getMarker(markerName);
    }

    /** A failed component encoding must never silently produce a partial record. */
    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        lock.lock();
        try {
            CompoundTag encoded = unknownComponents.copy();
            for (var entry : data.entrySet()) {
                encoded.put(entry.getKey().getId(), encodeComponent(entry.getKey(), entry.getValue()));
            }
            nbt.put("data", encoded);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final Tag encodeComponent(SpecialDataType<?> type, SpecialData value) {
        try {
            return ((Tag) ((SpecialDataType) type).saveData(NbtOps.INSTANCE, value)).copy();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encode component " + type.getId(), ex);
        }
    }

    @Override
    public void load(CompoundTag root, boolean isPacket) {
        CompoundTag encoded = root.getCompound("data");
        // Decode all candidates before exposing any of this input through slots.
        Map<SpecialDataType<?>, SpecialData> candidates = new IdentityHashMap<>();
        CompoundTag unknown = new CompoundTag();
        for (String key : encoded.getAllKeys()) {
            if (SpecialDataType.getType(key) == null) unknown.put(key, encoded.get(key).copy());
        }
        for (SpecialDataType<?> type : SpecialDataType.TYPE_REGISTRY) {
            if (!encoded.contains(type.getId())) continue;
            try {
                candidates.put(type, type.loadData(NbtOps.INSTANCE, encoded.get(type.getId())));
            } catch (Exception ex) {
                throw new DataLoadException("Failed to load " + type.getId(), ex);
            }
        }
        lock.lock();
        try {
            unknownComponents = unknown;
            for (var entry : candidates.entrySet()) {
                installComponent(entry.getKey(), entry.getValue(), InstallReason.LOAD);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <U extends SpecialData> U getData(SpecialDataType<U> type) {
        lock.lock();
        try {
            U existing = getOptional(type).orElse(null);
            if (existing != null) return existing;
            U created = type.create(this);
            installComponent(type, created, InstallReason.CREATE);
            return created;
        } finally {
            lock.unlock();
        }
    }

    public SpecialData getDataRaw(SpecialDataType<?> type) {
        return getData(type);
    }

    public <U extends SpecialData> U setData(SpecialDataType<U> type, U value) {
        Objects.requireNonNull(value, "component");
        installComponent(type, value, InstallReason.REPLACE);
        return value;
    }

    public <U extends SpecialData> Optional<U> removeData(SpecialDataType<U> type) {
        lock.lock();
        try {
            Optional<U> previous = getOptional(type);
            installComponent(type, null, InstallReason.REMOVE);
            return previous;
        } finally {
            lock.unlock();
        }
    }

    private void installComponent(SpecialDataType<?> type, SpecialData next, InstallReason reason) {
        lock.lock();
        try {
            SpecialData previous = data.get(type);
            if (previous == next) return;
            if (reason != InstallReason.LOAD && reason != InstallReason.RESTORE && deferComponentInstallation(type, next, reason)) return;
            if (next == null) data.remove(type);
            else data.put(type, next);
            unknownComponents.remove(type.getId());
            try {
                onComponentInstalled(type, previous, next, reason);
            } catch (RuntimeException failure) {
                // The server hook only throws before its business commit. Restore
                // the pointer before asking it to reconnect the old consumers.
                if (previous == null) data.remove(type);
                else data.put(type, previous);
                try {
                    onComponentInstalled(type, next, previous, InstallReason.RESTORE);
                } catch (RuntimeException restorationFailure) {
                    failure.addSuppressed(restorationFailure);
                }
                throw failure;
            }
        } finally {
            lock.unlock();
        }
    }

    /** Default/client holders have no server persistence or runtime binding. */
    protected void onComponentInstalled(SpecialDataType<?> type, SpecialData previous,
                                        SpecialData next, InstallReason reason) {
    }

    protected boolean deferComponentInstallation(SpecialDataType<?> type, SpecialData next, InstallReason reason) {
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Collection<SpecialDataType> getTypes() {
        return (Collection) Collections.unmodifiableSet(data.keySet());
    }

    protected final CompoundTag copyUnknownComponents() {
        return unknownComponents.copy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends SpecialData> Optional<U> getOptional(SpecialDataType<U> type) {
        return Optional.ofNullable((U) data.get(type));
    }
}
