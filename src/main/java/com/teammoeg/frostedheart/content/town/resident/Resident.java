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
import java.util.Optional;
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
            Codec.DOUBLE.optionalFieldOf("health",50.0).forGetter(o->o.health),
            Codec.DOUBLE.optionalFieldOf("mental",50.0).forGetter(o->o.mental),
            Codec.DOUBLE.optionalFieldOf("strength",50.0).forGetter(o->o.strength),
            Codec.DOUBLE.optionalFieldOf("intelligence",50.0).forGetter(o->o.intelligence),
            Codec.INT.optionalFieldOf("educationLevel",0).forGetter(o->o.educationLevel),
            CodecUtil.mapCodec("type", TownWorkerType.CODEC, "proficiency", Codec.INT).optionalFieldOf("workProficiency",Map.of()).forGetter(o->o.workProficiency),
            BlockPos.CODEC.optionalFieldOf("housePos").forGetter(o-> Optional.ofNullable(o.housePos)),
            BlockPos.CODEC.optionalFieldOf("workPos").forGetter(o-> Optional.ofNullable(o.workPos))
		).apply(t, Resident::new));

    public Resident(String firstName, String lastName, UUID uuid, double health, double mental, double strength, double intelligence, int educationLevel, Map<TownWorkerType, Integer> workProficiency, Optional<BlockPos> housePos, Optional<BlockPos> workPos) {
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
        this.housePos = housePos.orElse(null);
        this.workPos = workPos.orElse(null);
    }

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
    private double health = 50.0;
    // psychological, well-being, 幸福度
    @Getter
    private double mental = 50.0;
    /** Stats range from 0 to 100 end*/
    // educational
    // more than 0
    @Getter
    private int educationLevel = 0;
    //strength
    @Getter
    private double strength = 50.0;
    // intelligence, decides max educationLevel and the studying speed(the growth speed of educational level)
    @Getter
    private double intelligence = 50.0;
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

    public Resident(String firstName, String lastName, UUID uuid, double health, double mental, double strength, double intelligence, int educationLevel, Map<TownWorkerType, Integer> workProficiency, BlockPos housePos, BlockPos workPos) {
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
        data.putDouble("health", health);
        data.putDouble("happiness", mental);
        data.putDouble("strength", strength);
        data.putDouble("intelligence", intelligence);
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
        health = data.getDouble("health");
        mental = data.getDouble("happiness");
        strength = data.getDouble("strength");
        intelligence = data.getDouble("intelligence");
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

    public void setHealth(double health) {
        if (health < 0 || health > 100) {
            throw new IllegalArgumentException("Health must be between 0 and 100");
        }
        this.health = health;
    }

    public void costHealth(double amount) {
        this.health = Math.max(0, this.health - amount);
    }

    public void addHealth(double amount) {
        this.health = Math.min(100, this.health + amount);
    }

    public void setMental(double mental) {
        if (mental < 0 || mental > 100) {
            throw new IllegalArgumentException("Mental must be between 0 and 100");
        }
        this.mental = mental;
    }

    public void costMental(double amount) {
        this.mental = Math.max(0, this.mental - amount);
    }

    public void addMental(double amount) {
        this.mental = Math.min(100, this.mental + amount);
    }

    public void setStrength(double strength) {
        if (strength < 0 || strength > 100) {
            throw new IllegalArgumentException("Strength must be between 0 and 100");
        }
        this.strength = strength;
    }

    public void costStrength(double amount) {
        this.strength = Math.max(0, this.strength - amount);
    }

    public void addStrength(double amount) {
        this.strength = Math.min(100, this.strength + amount);
    }

    public void setIntelligence(double intelligence) {
        if (intelligence < 0 || intelligence > 100) {
            throw new IllegalArgumentException("Intelligence must be between 0 and 100");
        }
        this.intelligence = intelligence;
    }

    public void costIntelligence(double amount) {
        this.intelligence = Math.max(0, this.intelligence - amount);
    }

    public void addIntelligence(double amount) {
        this.intelligence = Math.min(100, this.intelligence + amount);
    }

    public void setEducationLevel(int educationLevel) {
        if (educationLevel < 0) {
            throw new IllegalArgumentException("Education level must be non-negative");
        }
        this.educationLevel = educationLevel;
    }

    public void costEducationLevel(int amount) {
        this.educationLevel = Math.max(0, this.educationLevel - amount);
    }

    public void addEducationLevel(int amount) {
        this.educationLevel = this.educationLevel + amount;
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
