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
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.event.ITownResidentChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.TownResidentChangeEvent;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.math.CMath;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

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
    /** 年龄组：0 幼儿（无生产力，不参与劳动） */
    public static final int AGE_INFANT = 0;
    /** 年龄组：1 儿童（可劳动，属性可成长到高于成年难民的上限） */
    public static final int AGE_CHILD = 1;
    /** 年龄组：2 青壮年（劳动力，属性极慢增长），默认值，兼容旧存档 */
    public static final int AGE_ADULT = 2;
    /** 年龄组：3 老人（力量萎缩、智力更高、初始熟练度更高），只天然刷新不会成长而来 */
    public static final int AGE_ELDER = 3;
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
            BlockPos.CODEC.optionalFieldOf("workPos").forGetter(o-> Optional.ofNullable(o.workPos)),
            Codec.INT.optionalFieldOf("age",AGE_ADULT).forGetter(o->o.age),
            Codec.INT.optionalFieldOf("ageDays",0).forGetter(o->o.ageDays),
            ResidentNutrition.CODEC.optionalFieldOf("nutrition", ResidentNutrition.DEFAULT_VALUE)
                    .forGetter(o -> o.nutrition)
		).apply(t, Resident::new));

    public Resident(String firstName, String lastName, UUID uuid, double health, double mental, double strength, double intelligence, int educationLevel, Map<String, Double> workProficiency, Optional<BlockPos> housePos, Optional<BlockPos> workPos, int age, int ageDays, ResidentNutrition nutrition) {
        setFirstName(firstName);
        setLastName(lastName);
        setUuid(uuid);
        setHealth(health);
        setMental(mental);
        setStrength(strength);
        setIntelligence(intelligence);
        setEducationLevel(educationLevel);
        setAge(age);
        setAgeDays(ageDays);
        setNutrition(nutrition);
        if(workProficiency!=null){
            workProficiency.forEach((key, value) ->
                    this.workProficiency.put(key, normalizeWorkProficiency(value)));
        }
        initializeMissingWorkProficiencies();
        setHousePos(housePos.orElse(null));
        setWorkPos(workPos.orElse(null));
    }

    /** Source-compatible persistent constructor used before resident nutrition existed. */
    public Resident(
            String firstName,
            String lastName,
            UUID uuid,
            double health,
            double mental,
            double strength,
            double intelligence,
            int educationLevel,
            Map<String, Double> workProficiency,
            Optional<BlockPos> housePos,
            Optional<BlockPos> workPos,
            int age,
            int ageDays
    ) {
        this(firstName, lastName, uuid, health, mental, strength, intelligence,
                educationLevel, workProficiency, housePos, workPos, age, ageDays,
                ResidentNutrition.DEFAULT_VALUE);
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

    // ===== 居民模拟绑定（transient，不被 codec 序列化；重启后由城镇接管 rebind 重建）=====
    // (transient, not codec-serialized; rebuilt by the town takeover rebind after restart)
    @Getter(AccessLevel.NONE)
    private transient int simId = -1;

    /**
     * 该居民绑定的模拟条目 id；-1 = 未绑定（尚未被模拟系统接管）。
     * 单一居民概念：城镇数据里的 Resident 就是模拟居民，此 id 是它在
     * 模拟中的会话/网络键（反向绑定在模拟条目的 uuidHi/uuidLo）。
     * <p>
     * The sim entry id this resident is bound to; -1 = unbound.
     * One resident concept: the Resident in town data IS the simulated citizen;
     * this id is its session/wire key in the simulation (the reverse binding
     * lives in the sim entry's uuidHi/uuidLo).
     *
     * @return 模拟条目 id / sim entry id
     */
    public int getSimId() {
        return this.simId;
    }

    /**
     * 设置模拟绑定（仅由城镇模拟数据 TownSimData 的事件回调与接管恢复调用）。
     * <p>
     * Sets the sim binding (called only by the town sim data's event callbacks
     * and takeover restore).
     *
     * @param simId 模拟条目 id；-1 解除 / sim entry id; -1 unbinds
     */
    public void setSimId(int simId) {
        this.simId = simId;
    }

    public void setFirstName(String firstName) {
        if (Objects.equals(this.firstName, firstName)) return;
        this.firstName = firstName;
        fireChange();
    }

    public void setLastName(String lastName) {
        if (Objects.equals(this.lastName, lastName)) return;
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
    private final Object2DoubleOpenHashMap<String> workProficiency = new Object2DoubleOpenHashMap<>();
    private transient final Set<String> proficiencyGainedToday = new HashSet<>();
    //the pos of the HouseBlock that the resident is living in
    @Nullable
    @Getter
    private BlockPos housePos;
    //the pos of the worker block that the resident is working in
    @Nullable
    @Getter
    private BlockPos workPos;
    /** 年龄组：0 幼儿 / 1 儿童 / 2 青壮年 / 3 老人。默认青壮年，兼容旧存档。 */
    @Getter
    private int age = AGE_ADULT;
    /** 出生后经过的天数，每日结算 +1，用于幼儿→儿童→青壮年的成长判定。 */
    @Getter
    private int ageDays = 0;
    /** Four persistent resident nutrition reserves, normalized to 0..100. */
    @Getter
    private ResidentNutrition nutrition = ResidentNutrition.DEFAULT_VALUE;

    public Resident(String firstName, String lastName) {
        this(firstName, lastName, UUID.randomUUID());
    }

    public Resident(String firstName, String lastName, int age, int ageDays) {
        this(firstName, lastName, UUID.randomUUID(), age, ageDays);
    }

    public Resident(String firstName, String lastName, UUID uuid, int age, int ageDays) {
        setFirstName(firstName);
        setLastName(lastName);
        setUuid(uuid);
        setAge(age);
        setAgeDays(ageDays);
        initializeAttributesForAge(age);
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
        initializeAttributesForAge(AGE_ADULT);
    }

    public Resident (String firstName, String lastName, String uuid){
        this(firstName,lastName,UUID.fromString(uuid));
    }

    public Resident(String firstName, String lastName, UUID uuid, double health, double mental, double strength, double intelligence, int educationLevel, Map<String, Double> workProficiency, BlockPos housePos, BlockPos workPos, int age, int ageDays) {
        setFirstName(firstName);
        setLastName(lastName);
        setUuid(uuid);
        setHealth(health);
        setMental(mental);
        setStrength(strength);
        setIntelligence(intelligence);
        setEducationLevel(educationLevel);
        setAge(age);
        setAgeDays(ageDays);
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
        if (!workProficiency.containsKey(key)) {
            double generatedProficiency = generateRandomProficiencyForAge();
            workProficiency.put(key, generatedProficiency);
            fireChange();
            return generatedProficiency;
        }

        double storedProficiency = workProficiency.getDouble(key);
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
                currentProficiency,
                growthAtZero,
                minimumGrowth,
                FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION.maximumWorkProficiency.get());
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
        data.putInt("age", age);
        data.putInt("ageDays", ageDays);
        data.putDouble("nutritionFat", nutrition.fat());
        data.putDouble("nutritionCarbohydrate", nutrition.carbohydrate());
        data.putDouble("nutritionProtein", nutrition.protein());
        data.putDouble("nutritionVegetable", nutrition.vegetable());
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
        int rawAge = data.contains("age", Tag.TAG_ANY_NUMERIC)
                ? data.getInt("age")
                : AGE_ADULT;
        int rawAgeDays = data.contains("ageDays", Tag.TAG_ANY_NUMERIC)
                ? data.getInt("ageDays")
                : 0;
        ResidentNutrition rawNutrition = new ResidentNutrition(
                data.contains("nutritionFat", Tag.TAG_ANY_NUMERIC)
                        ? data.getDouble("nutritionFat") : ResidentNutrition.DEFAULT,
                data.contains("nutritionCarbohydrate", Tag.TAG_ANY_NUMERIC)
                        ? data.getDouble("nutritionCarbohydrate") : ResidentNutrition.DEFAULT,
                data.contains("nutritionProtein", Tag.TAG_ANY_NUMERIC)
                        ? data.getDouble("nutritionProtein") : ResidentNutrition.DEFAULT,
                data.contains("nutritionVegetable", Tag.TAG_ANY_NUMERIC)
                        ? data.getDouble("nutritionVegetable") : ResidentNutrition.DEFAULT);
        // 先应用年龄，使下方的 initializeMissingWorkProficiencies 能按年龄生成熟练度
        setAge(rawAge);
        setAgeDays(rawAgeDays);

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
        if (rawAge < AGE_INFANT || rawAge > AGE_ELDER) {
            FHMain.LOGGER.error("Resident.deserialize: Invalid age value {} for resident {} {}, setting to 2", rawAge, firstName, lastName);
            rawAge = AGE_ADULT;
        }
        if (rawAgeDays < 0) {
            FHMain.LOGGER.error("Resident.deserialize: Invalid ageDays value {} for resident {} {}, setting to 0", rawAgeDays, firstName, lastName);
            rawAgeDays = 0;
        }

        setAge(rawAge);
        setAgeDays(rawAgeDays);
        setHealth(rawHealth);
        setMental(rawMental);
        setStrength(rawStrength);
        setIntelligence(rawIntelligence);
        setEducationLevel(rawEducationLevel);
        setNutrition(rawNutrition);

        return null;
    }

    public void setHousePos(BlockPos pos){
        if (Objects.equals(this.housePos, pos)) return;
        this.housePos = pos;
        fireChange();
    }

    public void setWorkPos(BlockPos pos){
        if (Objects.equals(this.workPos, pos)) return;
        this.workPos = pos;
        fireChange();
    }

    public void setAge(int age) {
        if (this.age == age) return;
        this.age = age;
        fireChange();
    }

    public void setAgeDays(int ageDays) {
        if (this.ageDays == ageDays) return;
        this.ageDays = ageDays;
        fireChange();
    }

    public void setNutrition(ResidentNutrition nutrition) {
        ResidentNutrition safe = nutrition == null
                ? ResidentNutrition.DEFAULT_VALUE : nutrition;
        if (Objects.equals(this.nutrition, safe)) return;
        this.nutrition = safe;
        fireChange();
    }

    public void setHealth(double health) {
        if (health < 0 || health > 100) {
            throw new IllegalArgumentException("Health must be between 0 and 100");
        }
        if (this.health == health) return;
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
        if (this.mental == mental) return;
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
        if (this.strength == strength) return;
        this.strength = strength;
        fireChange();
    }

    public void costStrength(double amount) {
        setStrength(Math.max(0, strength - amount));
    }

    public void addStrength(double amount) {
        setStrength(Math.min(100, strength + amount));
    }

    /**
     * 每日属性成长：低于上限时按日增长量增加，封顶在 cap（≤100）。
     */
    public void growStrengthDaily(double gain, double cap) {
        if (gain > 0 && this.strength < cap) {
            setStrength(Math.min(cap, strength + gain));
        }
    }

    /**
     * 老人每日力量萎缩：不低于 floor。
     */
    public void decayStrengthDaily(double decay, double floor) {
        if (decay > 0 && this.strength > floor) {
            setStrength(Math.max(floor, strength - decay));
        }
    }

    public void setIntelligence(double intelligence) {
        if (intelligence < 0 || intelligence > 100) {
            throw new IllegalArgumentException("Intelligence must be between 0 and 100");
        }
        if (this.intelligence == intelligence) return;
        this.intelligence = intelligence;
        fireChange();
    }

    public void costIntelligence(double amount) {
        setIntelligence(Math.max(0, intelligence - amount));
    }

    public void addIntelligence(double amount) {
        setIntelligence(Math.min(100, intelligence + amount));
    }

    /**
     * 每日属性成长：低于上限时按日增长量增加，封顶在 cap（≤100）。
     */
    public void growIntelligenceDaily(double gain, double cap) {
        if (gain > 0 && this.intelligence < cap) {
            setIntelligence(Math.min(cap, intelligence + gain));
        }
    }

    public void setEducationLevel(int educationLevel) {
        if (educationLevel < 0) {
            throw new IllegalArgumentException("Education level must be non-negative");
        }
        if (this.educationLevel == educationLevel) return;
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
        return lastName.isEmpty() ? firstName : firstName + " " + lastName;
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

    /**
     * 按年龄组初始化力量/智商（幼儿低、儿童中等、老人力量低智力高）与初始工作熟练度。
     * 青壮年沿用原有成人属性生成，分布不变。
     */
    private void initializeAttributesForAge(int age) {
        ResidentGenerationModel.Parameters parameters = generationParametersFromConfig();
        ResidentGenerationModel.AttributeCenters centers = parameters.centers(age);
        double spread = age == AGE_ADULT
                ? parameters.adultAttributeSpread() : parameters.nonAdultAttributeSpread();
        setHealth(parameters.initialHealth());
        setMental(parameters.initialMental());
        setStrength(ResidentAttributeModel.generateAttribute(
                CMath.RANDOM::nextDouble, centers.strength(), spread,
                parameters.attributeSampleCount()));
        setIntelligence(ResidentAttributeModel.generateAttribute(
                CMath.RANDOM::nextDouble, centers.intelligence(), spread,
                parameters.attributeSampleCount()));
        initializeMissingWorkProficiencies();
    }

    private void initializeMissingWorkProficiencies() {
        // fastutil 的原语 computeIfAbsent 重载会无条件插入且与 Map 重载构成歧义，改用显式判键
        if (!workProficiency.containsKey(HuntingBaseBuilding.class.getSimpleName())) {
            workProficiency.put(HuntingBaseBuilding.class.getSimpleName(), generateRandomProficiencyForAge());
        }
        if (!workProficiency.containsKey(MineBaseBuilding.class.getSimpleName())) {
            workProficiency.put(MineBaseBuilding.class.getSimpleName(), generateRandomProficiencyForAge());
        }
    }

    /**
     * 初始熟练度按年龄分发：幼儿 0、儿童上限 25、老人 [50,100]、青壮年 [0,50]。
     */
    private double generateRandomProficiencyForAge() {
        return ResidentGenerationModel.generateProficiency(
                age, CMath.RANDOM::nextDouble, generationParametersFromConfig());
    }

    private static double normalizeWorkProficiency(double proficiency) {
        if (!Double.isFinite(proficiency)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(
                FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION.maximumWorkProficiency.get(),
                proficiency));
    }

    /**
     * 招募时按年龄组在其合法天数区间内随机取一个年龄，使成长进度自然：
     * 幼儿 [0, infantToChildDays)、儿童 [infantToChildDays, childToAdultDays)、
     * 青壮年/老人 childToAdultDays 后再多 0-10 年。
     */
    public static int randomAgeDaysForAge(int age) {
        return ResidentGenerationModel.randomAgeDays(
                age, CMath.RANDOM::nextInt, generationParametersFromConfig());
    }

    /**
     * 年龄组对应的语言文件键，供 UI 显示年龄。
     */
    public static String ageLangKey(int age) {
        return switch (age) {
            case AGE_INFANT -> "gui.frostedheart.resident_age.infant";
            case AGE_CHILD -> "gui.frostedheart.resident_age.child";
            case AGE_ELDER -> "gui.frostedheart.resident_age.elder";
            default -> "gui.frostedheart.resident_age.adult";
        };
    }

    /**
     * 寒流天气下刷新的"高质量低血量"难民招募后应用加成：
     * 力量/智商更高、初始工作熟练度更高，但血量更低（20-40）。
     */
    public void applyColdSurvivorBuffs() {
        FHConfig.Server.Town.ResidentGeneration generation =
                FHConfig.SERVER.TOWN.RESIDENT_GENERATION;
        double minimumHealth = Math.min(
                generation.coldSurvivorHealthMinimum.get(),
                generation.coldSurvivorHealthMaximum.get());
        double maximumHealth = Math.max(
                generation.coldSurvivorHealthMinimum.get(),
                generation.coldSurvivorHealthMaximum.get());
        setHealth(minimumHealth
                + (maximumHealth - minimumHealth) * CMath.RANDOM.nextDouble());
        addStrength(generation.coldSurvivorAttributeBonus.get());
        addIntelligence(generation.coldSurvivorAttributeBonus.get());
        workProficiency.replaceAll((key, value) -> normalizeWorkProficiency(
                value * generation.coldSurvivorProficiencyMultiplier.get()));
        fireChange();
    }

    private static ResidentGenerationModel.Parameters generationParametersFromConfig() {
        FHConfig.Server.Town.ResidentGeneration generation =
                FHConfig.SERVER.TOWN.RESIDENT_GENERATION;
        FHConfig.Server.Town.ResidentAging aging = FHConfig.SERVER.TOWN.RESIDENT_AGING;
        FHConfig.Server.Town.RefugeeSpawn spawn = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        return new ResidentGenerationModel.Parameters(
                generation.initialHealth.get(), generation.initialMental.get(),
                generation.attributeSampleCount.get(),
                new ResidentGenerationModel.AttributeCenters(
                        generation.infantStrengthCenter.get(), generation.infantIntelligenceCenter.get()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.childStrengthCenter.get(), generation.childIntelligenceCenter.get()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.adultStrengthCenter.get(), generation.adultIntelligenceCenter.get()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.elderStrengthCenter.get(), generation.elderIntelligenceCenter.get()),
                generation.nonAdultAttributeSpread.get(), generation.adultAttributeSpread.get(),
                generation.infantInitialProficiency.get(),
                generation.childMaximumInitialProficiency.get(),
                generation.adultMaximumInitialProficiency.get(),
                generation.elderMinimumInitialProficiency.get(),
                generation.elderMaximumInitialProficiency.get(),
                aging.infantToChildDays.get(), aging.childToAdultDays.get(),
                generation.adultAgeRangeDaysExclusive.get(),
                new ResidentGenerationModel.AgeWeights(
                        spawn.weightInfant.get(), spawn.weightChild.get(),
                        spawn.weightAdult.get(), spawn.weightElder.get()),
                new ResidentGenerationModel.AgeWeights(
                        generation.fallbackWeightInfant.get(), generation.fallbackWeightChild.get(),
                        generation.fallbackWeightAdult.get(), generation.fallbackWeightElder.get()));
    }

}
