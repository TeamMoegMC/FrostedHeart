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
            CodecUtil.defaultValue(Codec.INT, 50).fieldOf("health").forGetter(o->o.health),
            CodecUtil.defaultValue(Codec.INT, 50).fieldOf("mental").forGetter(o->o.mental),
            CodecUtil.defaultValue(Codec.INT, 50).fieldOf("social").forGetter(o->o.social),
            CodecUtil.defaultValue(Codec.INT, 50).fieldOf("wealth").forGetter(o->o.wealth),
            CodecUtil.defaultValue(Codec.INT, 50).fieldOf("trust").forGetter(o->o.trust),
            CodecUtil.defaultValue(Codec.INT, 50).fieldOf("culture").forGetter(o->o.culture),
            CodecUtil.defaultValue(Codec.INT, 0).fieldOf("educationLevel").forGetter(o->o.educationLevel),
            CodecUtil.defaultValue(CodecUtil.mapCodec("type", TownWorkerType.CODEC, "proficiency", Codec.INT), null).fieldOf("workProficiency").forGetter(o->o.workProficiency),
            CodecUtil.defaultValue(BlockPos.CODEC, null).fieldOf("housePos").forGetter(o->o.housePos),
            CodecUtil.defaultValue(BlockPos.CODEC, null).fieldOf("workPos").forGetter(o->o.workPos)
		).apply(t, Resident::new));

    private UUID uuid;
    private String firstName = "Steve";
    private String lastName = "Alexander";
    /** Stats range from 0 to 100 */
    // physical
    private int health = 50;
    // psychological
    private int mental = 50;
    // social
    private int social = 50;
    // economic
    private int wealth = 50;
    // political
    private int trust = 50;
    // cultural
    private int culture = 50;
    // educational
    private int educationLevel = 0;
    //work proficiency.
    // If the number is negative, this type is considered as unworkable type.
    private final EnumMap<TownWorkerType, Integer> workProficiency = new EnumMap<>(TownWorkerType.class);
    //the pos of the HouseBlock that the resident is living in
    private BlockPos housePos;
    //the pos of the worker block that the resident is working in
    private BlockPos workPos;

    public Resident(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.uuid = UUID.randomUUID();
        workProficiency.forEach((k, v) -> {
            if(k.needsResident()) workProficiency.put(k, CMath.RANDOM.nextInt(20)-2);
        });//have 20% chance to be unworkable type
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

    public Resident(String firstName, String lastName, UUID uuid, int health, int mental, int social, int wealth, int trust, int culture, int educationLevel, Map<TownWorkerType, Integer> workProficiency, BlockPos housePos, BlockPos workPos) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.uuid = uuid;
        this.health = health;
        this.mental = mental;
        this.wealth = wealth;
        this.social = social;
        this.trust = trust;
        this.culture = culture;
        this.educationLevel = educationLevel;
        if(workProficiency!=null){
            this.workProficiency.putAll(workProficiency);
        }
        this.housePos = housePos;
        this.workPos = workPos;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UUID getUUID(){
        return uuid;
    }

    public int getHealth() {
        return health;
    }
    public int getMental() {
        return mental;
    }
    public int getSocial() {
        return social;
    }
    public int getWealth() {
        return wealth;
    }
    public int getTrust() {
        return trust;
    }
    public int getCulture() {
        return culture;
    }
    public int getEducationLevel() {
        return educationLevel;
    }

    public EnumMap<TownWorkerType, Integer> getWorkProficiency() {
        return workProficiency;
    }

    public int getWorkProficiency(TownWorkerType type) {
        return workProficiency.getOrDefault(type, -1);
    }

    public Double getWorkScore(TownWorkerType type){
        int proficiency = getWorkProficiency(type);
        if(health < 20 || mental < 20 || housePos == null || workPos != null){
            return Double.NEGATIVE_INFINITY;
        }
        if(proficiency <= 0) return 0.0;
        return CalculatingFunction2(health) * Math.pow(CalculatingFunction1(proficiency), 0.5) * Math.pow(CalculatingFunction1(mental), 0.3) * Math.pow(type.getResidentExtraScore(this), 0.5);
    }

    /**
     * Compute the productivity multiplier of the resident.
     * A linear map from [0,100] to [0.0,2.0].
     * The input is average of every stat.
     * @return the productivity multiplier
     */
    public double getProductivityMultiplier() {
        return (health + mental + social + wealth + trust + culture) / 300.0;
    }

    // serialization
    public CompoundTag serialize() {
        CompoundTag data = new CompoundTag();
        data.putString("uuid", uuid.toString());
        data.putString("firstName", firstName);
        data.putString("lastName", lastName);
        data.putInt("health", health);
        data.putInt("happiness", mental);
        data.putInt("social", social);
        data.putInt("wealth", wealth);
        data.putInt("trust", trust);
        data.putInt("culture", culture);
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
        social = data.getInt("social");
        wealth = data.getInt("wealth");
        trust = data.getInt("trust");
        culture = data.getInt("culture");
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

    public void setHousePos(BlockPos pos){
        this.housePos = pos;
    }

    public BlockPos getWorkPos(){
        return workPos;
    }

    public BlockPos getHousePos(){
        return housePos;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resident) {
            Resident other = (Resident) obj;
            return other.uuid.equals(uuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    //用于调整数据
    //1-exp型，在x大概为20时，数值达到一半
    public static double CalculatingFunction1(int num){
        if(num <= 0){
            return Double.NEGATIVE_INFINITY;
        }
        return 1-Math.exp(-num*0.04);
    }
    //S型曲线，关于点(50,0.5)对称
    public static double CalculatingFunction2(int num){
        return 1/(1+Math.exp(-num*0.1+5));
    }
}
