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

package com.teammoeg.frostedheart.content.town.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;
import net.minecraft.core.BlockPos;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import static com.teammoeg.frostedheart.content.town.building.BuildingRecordsCodec.strict;

/** Immutable scan result. Only the owning building replaces the applied value. */
public final class OccupiedZoneInfo {
    public static final Codec<OccupiedZoneInfo> CODEC = RecordCodecBuilder.create(t -> t.group(
            strict(OccupiedCell.SET_CODEC).optionalFieldOf("occupiedVolume").forGetter(o -> Optional.of(o.occupiedCells)),
            strict(BlockPos.CODEC.listOf()).optionalFieldOf("error").forGetter(o -> Optional.ofNullable(o.errorPos).map(List::copyOf)),
            strict(Codec.BOOL).optionalFieldOf("overlapped", false).forGetter(o -> o.overlapped),
            strict(Codec.BOOL).optionalFieldOf("valid", true).forGetter(o -> o.valid)
    ).apply(t, OccupiedZoneInfo::new));
    public static final OccupiedZoneInfo EMPTY = new OccupiedZoneInfo(Set.of(), null, false, false);
    private final Set<OccupiedCell> occupiedCells;
    private final Set<BlockPos> errorPos;
    private final boolean overlapped;
    private final boolean valid;
    private final BlockPos signature;

    public OccupiedZoneInfo(OccupiedCell... cells) {
        this(Set.of(cells));
    }
    public OccupiedZoneInfo(Set<OccupiedCell> cells) {
        this(cells, null);
    }
    public OccupiedZoneInfo(BlockPos error, OccupiedCell... cells) {
        this(Set.of(cells), Set.of(error));
    }
    public OccupiedZoneInfo(Set<OccupiedCell> cells, Set<BlockPos> errors) {
        this(cells, errors, false, true);
    }
    public OccupiedZoneInfo(Optional<Set<OccupiedCell>> cells, Optional<List<BlockPos>> errors, boolean overlap, boolean valid) {
        this(cells.orElse(Set.of()), errors.map(Set::copyOf).orElse(null), overlap, cells.isPresent() && errors.isEmpty() && valid);
    }
    public OccupiedZoneInfo(Set<OccupiedCell> cells, Set<BlockPos> errors, boolean overlap, boolean valid) {
        occupiedCells = Set.copyOf(cells);
        errorPos = errors == null ? null : Set.copyOf(errors);
        overlapped = overlap;
        this.valid = valid;
        signature = occupiedCells.stream().map(OccupiedCell::getPos).min(BlockPos::compareTo)
                .orElse(new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
    public static OccupiedZoneInfo empty() {
        return EMPTY;
    }
    public Set<OccupiedCell> getOccupiedCells() {
        return occupiedCells;
    }
    public Set<BlockPos> getErrorPos() {
        return errorPos;
    }
    public BlockPos getSignature() {
        return signature;
    }
    public boolean isOverlapped() {
        return overlapped;
    }
    public boolean isValid() {
        return valid;
    }
    public OccupiedZoneInfo withValid(boolean value) {
        return value == valid ? this : new OccupiedZoneInfo(occupiedCells, errorPos, overlapped, value);
    }
    public OccupiedZoneInfo withOverlapped(boolean value) {
        return value == overlapped ? this : new OccupiedZoneInfo(occupiedCells, errorPos, value, valid);
    }
    public boolean sameGeometry(OccupiedZoneInfo other) {
        return occupiedCells.equals(other.occupiedCells);
    }
    @Override
    public boolean equals(Object value) {
        return value instanceof OccupiedZoneInfo other
                && sameGeometry(other) && valid == other.valid && overlapped == other.overlapped
                && Objects.equals(errorPos, other.errorPos);
    }
    @Override
    public int hashCode() { return Objects.hash(occupiedCells, errorPos, valid, overlapped); }
}
