/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.math.CMath;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A resident of the town.
 * <p>
 * This is an abstract data type used in the town simulation.
 * For the actual entity, see {@link ResidentEntity}.
 */
public class Resident {
	public static final Codec<Resident> CODEC=RecordCodecBuilder.create(t->t.group(
            Codec.STRING.fieldOf("firstName").forGetter(o->o.firstName),
            Codec.STRING.fieldOf("lastName").forGetter(o->o.lastName),
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(o->o.uuid),
            Codec.INT.optionalFieldOf("health",50).forGetter(o->o.health),
            Codec.INT.optionalFieldOf("mental",50).forGetter(o->o.mental),
            Codec.INT.optionalFieldOf("strength",50).forGetter(o->o.strength),
            Codec.INT.optionalFieldOf("intelligence",50).forGetter(o->o.intelligence),
            Codec.INT.optionalFieldOf("educationLevel",0).forGetter(o->o.educationLevel),
            CodecUtil.mapCodec("type", TownWorkerType.CODEC, "proficiency", Codec.INT).optionalFieldOf("workProficiency",Map.of()).forGetter(o->o.workProficiency),
            BlockPos.CODEC.optionalFieldOf("housePos", null).forGetter(o->o.housePos),
            BlockPos.CODEC.optionalFieldOf("workPos", null).forGetter(o->o.workPos)
		).apply(t, Resident::new));
    private UUID uuid;
    @Setter
    @Getter
    private String firstName = "Steve";
    @Setter
    @Getter
    private String lastName = "Alexander";
    /** Stats range from 0 to 100 start*/
    // physical
    @Getter
    private int health = 50;
    // psychological, well-being, 幸福度
    @Getter
    private int mental = 50;
    /** Stats range from 0 to 100 end*/
    // educational
    // more than 0
    @Getter
    private int educationLevel = 0;
    //strength
    @Getter
    private int strength = 50;
    // intelligence, decides max educationLevel and the studying speed(the growth speed of educational level)
    @Getter
    private int intelligence = 50;
    /**
     *  work proficiency.
     *  If the number is negative, this type is considered as unworkable type.
     */
    @Getter
    private final EnumMap<TownWorkerType, Integer> workProficiency = new EnumMap<>(TownWorkerType.class);
    //the pos of the HouseBlock that the resident is living in
    @Getter
    @Setter
    private BlockPos housePos;
    //the pos of the worker block that the resident is working in
    @Getter
    private BlockPos workPos;

    public Resident(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.uuid = UUID.randomUUID();
        workProficiency.forEach((k, v) -> {
            if(k.needsResident()) workProficiency.put(k, CMath.RANDOM.nextInt(20));
        });
    }

    //public Resident() {
    //}

    public Resident(Tag inbt){
        this.deserialize((CompoundTag)inbt);
    }

    public Resident(String firstName, String lastName, UUID uuid){
        this.firstName = firstName;
        this.lastName = lastName;
        this.uuid = uuid;
    }

    public Resident (String firstName, String lastName, String uuid){
        this(firstName,lastName,UUID.fromString(uuid));
    }

    public Resident(String firstName, String lastName, UUID uuid, int health, int mental, int strength, int intelligence, int educationLevel, Map<TownWorkerType, Integer> workProficiency, BlockPos housePos, BlockPos workPos) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.uuid = uuid;
        this.health = health;
        this.mental = mental;
        this.strength = strength;
        this.intelligence = intelligence;
        this.educationLevel = educationLevel;
        if(workProficiency!=null){
            this.workProficiency.putAll(workProficiency);
        }
        this.housePos = housePos;
        this.workPos = workPos;
    }

    public UUID getUUID(){
        return uuid;
    }

    public int getWorkProficiency(TownWorkerType type) {
        return workProficiency.getOrDefault(type, 0);
    }

    // serialization
    public CompoundTag serialize() {
        CompoundTag data = new CompoundTag();
        data.putString("uuid", uuid.toString());
        data.putString("firstName", firstName);
        data.putString("lastName", lastName);
        data.putInt("health", health);
        data.putInt("happiness", mental);
        data.putInt("strength", strength);
        data.putInt("intelligence", intelligence);
        data.putInt("educationLevel", educationLevel);
        data.put("workProficiency", SerializeUtil.toNBTMap(workProficiency.entrySet(), (entry, compoundNBTBuilder) -> compoundNBTBuilder.putInt(entry.getKey().getKey(), entry.getValue())));
        data.putLong("workPos", workPos.asLong());
        data.putLong("housePos", housePos.asLong());
        return data;
    }

    public Resident deserialize(CompoundTag data) {
        uuid = UUID.fromString(data.getString("uuid"));
        firstName = data.getString("firstName");
        lastName = data.getString("lastName");
        health = data.getInt("health");
        mental = data.getInt("happiness");
        strength = data.getInt("strength");
        intelligence = data.getInt("intelligence");
        educationLevel = data.getInt("educationLevel");
        CompoundTag workProficiencyNBT = data.getCompound("workProficiency");
        workProficiency.keySet().forEach(key/*TownWorkerType*/ -> workProficiency.put(key, workProficiencyNBT.getInt(key.getKey())));
        workPos = BlockPos.of(data.getLong("workPos"));
        housePos = BlockPos.of(data.getLong("housePos"));
        return null;
    }

    public void setWorkPos(BlockPos pos){
        this.housePos = pos;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resident other) {
            return other.uuid.equals(uuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

}
