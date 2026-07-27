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

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.event.ITownResidentChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.TownResidentChangeEvent;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.math.CMath;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A resident of the town.
 * <p>
 * This is an abstract data type used in the town simulation.
 * For the actual entity, see {@link ResidentEntity}.
 */
public class Resident {
    public static final double MAX_WORK_PROFICIENCY = 100.0;
	public static final Codec<Resident> CODEC=RecordCodecBuilder.create(t->t.group(
            Codec.STRING.fieldOf("firstName").forGetter(o->o.firstName),
            Codec.STRING.fieldOf("lastName").forGetter(o->o.lastName),
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(o->o.uuid),
            Codec.DOUBLE.optionalFieldOf("health",50.0).forGetter(o->o.health),
            Codec.DOUBLE.optionalFieldOf("mental",50.0).forGetter(o->o.mental),
            Codec.DOUBLE.optionalFieldOf("strength",50.0).forGetter(o->o.strength),
            Codec.DOUBLE.optionalFieldOf("intelligence",50.0).forGetter(o->o.intelligence),
            Codec.INT.optionalFieldOf("educationLevel",0).forGetter(o->o.educationLevel),
            CodecUtil.mapCodec("type", Codec.STRING, "proficiency", Codec.DOUBLE).optionalFieldOf("workProficiency",Map.of()).forGetter(o->o.workProficiency),
            BlockPos.CODEC.optionalFieldOf("housePos").forGetter(o-> Optional.ofNullable(o.housePos)),
            BlockPos.CODEC.optionalFieldOf("workPos").forGetter(o-> Optional.ofNullable(o.workPos))
		).apply(t, Resident::new));

    public Resident(String firstName, String lastName, UUID uuid, double health, double mental, double strength, double intelligence, int educationLevel, Map<String, Double> workProficiency, Optional<BlockPos> housePos, Optional<BlockPos> workPos) {
        setFirstName(firstName);
        setLastName(lastName);
        setUuid(uuid);
        setHealth(health);
        setMental(mental);
        setStrength(strength);
        setIntelligence(intelligence);
        setEducationLevel(educationLevel);
        if(workProficiency!=null){
            workProficiency.forEach((key, value) ->
                    this.workProficiency.put(key, normalizeWorkProficiency(value)));
        }
        initializeMissingWorkProficiencies();
        setHousePos(housePos.orElse(null));
        setWorkPos(workPos.orElse(null));
    }

    // ===== 增量同步：变化监听器（transient，不被 codec 序列化）=====
    @Getter(AccessLevel.NONE)
    private transient ITownResidentChangeEventListener changeListener;

    public void setChangeEventListener(ITownResidentChangeEventListener listener) {
        this.changeListener = listener;
    }

    /** 内部字段变更时触发增量同步事件；listener 为 null（解码/构造阶段）时为 no-op，不会误标脏。 */
    private void fireChange() {
        if (this.changeListener != null) {
            this.changeListener.onResidentChange(new TownResidentChangeEvent(this, this.uuid));
        }
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        fireChange();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        fireChange();
    }

    private UUID uuid;
    @Getter
    private String firstName = "Steve";
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
     *  must be positive.
     *  Key: clazz.getSimpleName() of Building class
     */
    @Getter
    private final Map<String, Double> workProficiency = new HashMap<>();
    private transient final Set<String> proficiencyGainedToday = new HashSet<>();
    //the pos of the HouseBlock that the resident is living in
    @Nullable
    @Getter
    private BlockPos housePos;
    //the pos of the worker block that the resident is working in
    @Nullable
    @Getter
    private BlockPos workPos;

    public Resident(String firstName, String lastName) {
        this(firstName, lastName, UUID.randomUUID());
    }

    //public Resident() {
    //}

    public Resident(Tag inbt){
        this.deserialize((CompoundTag)inbt);
    }

    public Resident(String firstName, String lastName, UUID uuid){
        setFirstName(firstName);
        setLastName(lastName);
        setUuid(uuid);
        initializeAdultAttributesAndExperience();
    }

    public Resident (String firstName, String lastName, String uuid){
        this(firstName,lastName,UUID.fromString(uuid));
    }

    public Resident(String firstName, String lastName, UUID uuid, double health, double mental, double strength, double intelligence, int educationLevel, Map<String, Double> workProficiency, BlockPos housePos, BlockPos workPos) {
        setFirstName(firstName);
        setLastName(lastName);
        setUuid(uuid);
        setHealth(health);
        setMental(mental);
        setStrength(strength);
        setIntelligence(intelligence);
        setEducationLevel(educationLevel);
        if(workProficiency!=null){
            workProficiency.forEach((key, value) ->
                    this.workProficiency.put(key, normalizeWorkProficiency(value)));
        }
        initializeMissingWorkProficiencies();
        setHousePos(housePos);
        setWorkPos(workPos);
    }

    public void setDeath(ITownWithResidents town){
        town.removeResident(this.uuid);
    }

    public UUID getUUID(){
        return uuid;
    }

    public void setUuid(UUID uuid){
        this.uuid = uuid;
    }

    public double getWorkProficiency(Class<? extends ITownResidentWorkBuilding> type) {
        String key = type.getSimpleName();
        Double storedProficiency = workProficiency.get(key);
        if (storedProficiency == null) {
            double generatedProficiency = generateRandomProficiency();
            workProficiency.put(key, generatedProficiency);
            fireChange();
            return generatedProficiency;
        }

        double proficiency = normalizeWorkProficiency(storedProficiency);
        if (Double.compare(storedProficiency, proficiency) != 0) {
            workProficiency.put(key, proficiency);
            fireChange();
        }
        return proficiency;
    }

    /**
     * Applies at most one proficiency gain for this profession in the current
     * town workday.
     */
    public double gainDailyWorkProficiency(
            Class<? extends ITownResidentWorkBuilding> type,
            double growthAtZero,
            double minimumGrowth
    ) {
        String key = type.getSimpleName();
        double currentProficiency = getWorkProficiency(type);
        if (!proficiencyGainedToday.add(key)) {
            return currentProficiency;
        }

        double gain = ResidentAttributeModel.calculateDailyProficiencyGain(
                currentProficiency, growthAtZero, minimumGrowth);
        if (gain <= 0.0) {
            return currentProficiency;
        }
        return addWorkProficiency(type, gain);
    }

    public void resetDailyProficiencyGrowth() {
        proficiencyGainedToday.clear();
    }

    public double addWorkProficiency(Class<? extends ITownResidentWorkBuilding> type, double amount){
        if(!Double.isFinite(amount) || amount < 0){
            amount = 0;
            FHMain.LOGGER.error("Resident.addWorkProficiency:Trying to add invalid work proficiency amount!");
        }
        double result = normalizeWorkProficiency(getWorkProficiency(type) + amount);
        workProficiency.put(type.getSimpleName(), result);
        fireChange();
        return result;
    }

    public double setWorkProficiency(Class<? extends ITownResidentWorkBuilding> type,double amount){
        if(amount < 0){
            amount = 0;
            FHMain.LOGGER.error("Resident.setWorkProficiency:Trying to set work proficiency to negative amount!");
        }
        amount = normalizeWorkProficiency(amount);
        workProficiency.put(type.getSimpleName(), amount);
        fireChange();
        return amount;
    }

    /**
     * 增加 1 点职业熟练度。熟练度范围固定为 0 到 100。
     *
     * @return 增加后的熟练度
     */
    public double addWorkProficiency(Class<? extends ITownResidentWorkBuilding> type){
        return addWorkProficiency(type, 1);

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
        data.put("workProficiency", SerializeUtil.toNBTMap(workProficiency.entrySet(), (entry, compoundNBTBuilder) -> compoundNBTBuilder.putDouble(entry.getKey(), entry.getValue())));
        if (workPos != null) {
            data.putLong("workPos", workPos.asLong());
        }
        if (housePos != null) {
            data.putLong("housePos", housePos.asLong());
        }
        return data;
    }

    public Resident deserialize(CompoundTag data) {
        setUuid(UUID.fromString(data.getString("uuid")));
        setFirstName(data.getString("firstName"));
        setLastName(data.getString("lastName"));

        // Read raw values first, validate, then apply via setters
        // to avoid setter validation exceptions on corrupt data
        double rawHealth = data.getDouble("health");
        double rawMental = data.getDouble("happiness");
        double rawStrength = data.contains("strength", Tag.TAG_ANY_NUMERIC)
                ? data.getDouble("strength")
                : 50.0;
        double rawIntelligence = data.contains("intelligence", Tag.TAG_ANY_NUMERIC)
                ? data.getDouble("intelligence")
                : 50.0;
        int rawEducationLevel = data.getInt("educationLevel");

        CompoundTag workProficiencyNBT = data.getCompound("workProficiency");
        workProficiency.clear();
        workProficiencyNBT.getAllKeys().forEach(key ->
                workProficiency.put(key, normalizeWorkProficiency(workProficiencyNBT.getDouble(key))));
        initializeMissingWorkProficiencies();

        if (data.contains("workPos")) {
            setWorkPos(BlockPos.of(data.getLong("workPos")));
        } else {
            setWorkPos(null);
        }
        if (data.contains("housePos")) {
            setHousePos(BlockPos.of(data.getLong("housePos")));
        } else {
            setHousePos(null);
        }

        //  添加边界检查
        if (rawHealth < 0 || rawHealth > 100) {
            FHMain.LOGGER.error("Resident.deserialize: Invalid health value {} for resident {} {}, setting to 50", rawHealth, firstName, lastName);
            rawHealth = 50.0;
        }
        if (rawMental < 0 || rawMental > 100) {
            FHMain.LOGGER.error("Resident.deserialize: Invalid mental value {} for resident {} {}, setting to 50", rawMental, firstName, lastName);
            rawMental = 50.0;
        }
        if (rawStrength < 0 || rawStrength > 100) {
            FHMain.LOGGER.error("Resident.deserialize: Invalid strength value {} for resident {} {}, setting to 50", rawStrength, firstName, lastName);
            rawStrength = 50.0;
        }
        if (rawIntelligence < 0 || rawIntelligence > 100) {
            FHMain.LOGGER.error("Resident.deserialize: Invalid intelligence value {} for resident {} {}, setting to 50", rawIntelligence, firstName, lastName);
            rawIntelligence = 50.0;
        }
        if (rawEducationLevel < 0) {
            FHMain.LOGGER.error("Resident.deserialize: Invalid educationLevel value {} for resident {} {}, setting to 0", rawEducationLevel, firstName, lastName);
            rawEducationLevel = 0;
        }

        setHealth(rawHealth);
        setMental(rawMental);
        setStrength(rawStrength);
        setIntelligence(rawIntelligence);
        setEducationLevel(rawEducationLevel);

        return null;
    }

    public void setHousePos(BlockPos pos){
        this.housePos = pos;
        fireChange();
    }

    public void setWorkPos(BlockPos pos){
        this.workPos = pos;
        fireChange();
    }

    public void setHealth(double health) {
        if (health < 0 || health > 100) {
            throw new IllegalArgumentException("Health must be between 0 and 100");
        }
        this.health = health;
        fireChange();
    }

    public void costHealth(double amount) {
        setHealth(Math.max(0, health - amount));
    }

    public void addHealth(double amount) {
        setHealth(Math.min(100, health + amount));
    }

    public void setMental(double mental) {
        if (mental < 0 || mental > 100) {
            throw new IllegalArgumentException("Mental must be between 0 and 100");
        }
        this.mental = mental;
        fireChange();
    }

    public void costMental(double amount) {
        setMental(Math.max(0, mental - amount));
    }

    public void addMental(double amount) {
        setMental(Math.min(100, mental + amount));
    }

    public void setStrength(double strength) {
        if (strength < 0 || strength > 100) {
            throw new IllegalArgumentException("Strength must be between 0 and 100");
        }
        this.strength = strength;
        fireChange();
    }

    public void costStrength(double amount) {
        setStrength(Math.max(0, strength - amount));
    }

    public void addStrength(double amount) {
        setStrength(Math.min(100, strength + amount));
    }

    public void setIntelligence(double intelligence) {
        if (intelligence < 0 || intelligence > 100) {
            throw new IllegalArgumentException("Intelligence must be between 0 and 100");
        }
        this.intelligence = intelligence;
        fireChange();
    }

    public void costIntelligence(double amount) {
        setIntelligence(Math.max(0, intelligence - amount));
    }

    public void addIntelligence(double amount) {
        setIntelligence(Math.min(100, intelligence + amount));
    }

    public void setEducationLevel(int educationLevel) {
        if (educationLevel < 0) {
            throw new IllegalArgumentException("Education level must be non-negative");
        }
        this.educationLevel = educationLevel;
        fireChange();
    }

    public void costEducationLevel(int amount) {
        setEducationLevel(Math.max(0, educationLevel - amount));
    }

    public void addEducationLevel(int amount) {
        setEducationLevel(educationLevel + amount);
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

    private void initializeAdultAttributesAndExperience() {
        setStrength(ResidentAttributeModel.generateAdultAttribute(CMath.RANDOM::nextDouble));
        setIntelligence(ResidentAttributeModel.generateAdultAttribute(CMath.RANDOM::nextDouble));
        initializeMissingWorkProficiencies();
    }

    private void initializeMissingWorkProficiencies() {
        workProficiency.computeIfAbsent(
                HuntingBaseBuilding.class.getSimpleName(),
                ignored -> generateRandomProficiency());
        workProficiency.computeIfAbsent(
                MineBaseBuilding.class.getSimpleName(),
                ignored -> generateRandomProficiency());
    }

    private static double generateRandomProficiency() {
        return ResidentAttributeModel.generateInitialWorkProficiency(CMath.RANDOM::nextDouble);
    }

    private static double normalizeWorkProficiency(double proficiency) {
        if (!Double.isFinite(proficiency)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(MAX_WORK_PROFICIENCY, proficiency));
    }

}
