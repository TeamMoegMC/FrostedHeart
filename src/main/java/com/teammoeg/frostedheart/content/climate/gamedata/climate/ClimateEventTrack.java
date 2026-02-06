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

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import java.util.LinkedList;
import java.util.Optional;
import java.util.function.LongFunction;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.registry.TypedCodecRegistry;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class ClimateEventTrack {
	public static final TypedCodecRegistry<ClimateEvent> REGISTRY=new TypedCodecRegistry<>();
	public static final Codec<ClimateEvent> CODEC=REGISTRY.byNameCodec();
	static {
		REGISTRY.register(EmptyClimateEvent.class, "empty",EmptyClimateEvent.CODEC);
		REGISTRY.register(InterpolationClimateEvent.class, "intrp",InterpolationClimateEvent.CODEC);
	}
    protected LinkedList<ClimateEvent> tempEventStream = new LinkedList<>();
	public ClimateEventTrack() {
	}
    public void appendTempEvent(LongFunction<ClimateEvent> generator) {
        ClimateEvent head = tempEventStream.getLast();
        tempEventStream.add(generator.apply(head.getCalmEndTime()));
    }
    public void appendTempEvent(ClimateEvent event) {
        tempEventStream.add(event);
    }
    public void clear() {
    	tempEventStream.clear();
    }
    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     *
     * @param time given in absolute seconds relative to clock source.
     */
    protected void rebuildTempEventStream(long currentTime,long time) {
        // If tempEventStream becomes empty for some reason,
        // start generating TempEvent from current time.
        FHMain.LOGGER.error("Temperature Data corrupted, rebuilding temperature data");
        if (tempEventStream.isEmpty() || tempEventStream.getFirst().getStartTime() > currentTime) {
            tempEventStream.clear();
            tempEventStream.add(InterpolationClimateEvent.getClimateEvent(currentTime));
        }

        ClimateEvent head = tempEventStream.getFirst();
        tempEventStream.clear();
        tempEventStream.add(head);
        while (head.getCalmEndTime() < time) {
            tempEventStream.add(head = InterpolationClimateEvent.getClimateEvent(head.getCalmEndTime()));
        }
    }
    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     *
     * @param time given in absolute seconds relative to clock source.
     */
    protected void tempEventStreamGrow(long currentTime,long time) {
        // If tempEventStream becomes empty for some reason,
        // start generating TempEvent from current time.
        if (tempEventStream.isEmpty()) {
            tempEventStream.add(InterpolationClimateEvent.getClimateEvent(currentTime));
        }

        ClimateEvent head = tempEventStream.getLast();
        while (head.getCalmEndTime() < time) {
            tempEventStream.add(head = InterpolationClimateEvent.getClimateEvent(head.getCalmEndTime()));
        }
    }

    /**
     * Trims all TempEvents that end before given time.
     *
     * @param time given in absolute seconds relative to clock source.
     */
    protected void tempEventStreamTrim(long time) {
        ClimateEvent head = tempEventStream.peek();
        if (head != null) {
            while (head.getCalmEndTime() < time) {
                // Protection mechanism:
                // it would be a disaster if the stream is trimmed to empty
                if (tempEventStream.size() <= 1) {
                    break;
                }
                tempEventStream.remove();
                head = tempEventStream.peek();
            }
        }
    }
    /**
     * Get temperature at given time.
     * Grow tempEventStream as needed.
     * No trimming will be performed.
     * To perform trimming,
     * use {@link #tempEventStreamTrim(long) tempEventStreamTrim}.
     *
     * @param time given in absolute seconds relative to clock source.
     * @return temperature at given time
     */
    public ClimateResult computeTemp(long currentTime,long time) {
        if (time < currentTime) return ClimateResult.EMPTY;
        tempEventStreamGrow(currentTime,time);
        while (true) {
            Optional<ClimateResult> f = tempEventStream
                    .stream()
                    .filter(e -> time <= e.getCalmEndTime() && time >= e.getStartTime())
                    .findFirst()
                    .map(e -> e.getHourClimate(time));
            if (f.isPresent())
                return f.get();
            rebuildTempEventStream(currentTime,time);
        }
    }
	public void save(CompoundTag nbt) {
        nbt.put("tempEventStream", CodecUtil.toNBTList(tempEventStream, ClimateEventTrack.CODEC));
	}

	public void load(CompoundTag nbt) {
        tempEventStream.clear();
        tempEventStream.addAll(CodecUtil.fromNBTList(nbt.getList("tempEventStream", Tag.TAG_COMPOUND), ClimateEventTrack.CODEC));

	}

    @Override
    public String toString() {
        return "{TempEventTrack=\n" + tempEventStream.stream().map(Object::toString).collect(Collectors.joining(",")) +"}";
    }

}
