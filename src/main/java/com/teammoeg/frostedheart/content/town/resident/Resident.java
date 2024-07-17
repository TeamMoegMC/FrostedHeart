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
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.UUIDCodec;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
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
            UUIDCodec.CODEC.fieldOf("uuid").forGetter(o->o.uuid)
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
    private EnumMap<TownWorkerType, Integer> workProficiency = new EnumMap<>(TownWorkerType.class);
    //the pos of the HouseBlock that the resident is living in
    private BlockPos housePos;
    //the pos of the worker block that the resident is working in
    private BlockPos workPos;

    public Resident(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.uuid = UUID.randomUUID();
        Random random = new Random();
        workProficiency.forEach((k, v) -> workProficiency.put(k, random.nextInt(20)-2));//have 20% chance to be unworkable type
    }

    //public Resident() {
    //}

    public Resident(INBT inbt){
        this.deserialize((CompoundNBT)inbt);
    }

    public Resident(String firstName, String lastName, UUID uuid){
        this.firstName = firstName;
        this.lastName = lastName;
        this.uuid = uuid;
    }

    public Resident (String firstName, String lastName, String uuid){
        this(firstName,lastName,UUID.fromString(uuid));
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
    public CompoundNBT serialize() {
        CompoundNBT data = new CompoundNBT();
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
        data.putLong("workPos", workPos.toLong());
        data.putLong("housePos", housePos.toLong());
        return data;
    }

    public Resident deserialize(CompoundNBT data) {
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
        CompoundNBT workProficiencyNBT = data.getCompound("workProficiency");
        workProficiency.keySet().forEach(key/*TownWorkerType*/ -> workProficiency.put(key, workProficiencyNBT.getInt(key.getKey())));
        workPos = BlockPos.fromLong(data.getLong("workPos"));
        housePos = BlockPos.fromLong(data.getLong("housePos"));
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
