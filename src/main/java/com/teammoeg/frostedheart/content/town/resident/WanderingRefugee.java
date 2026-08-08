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

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.climate.AttractedByGeneratorGoal;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.trade.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WanderingRefugee extends AbstractVillager implements NeutralMob, VillagerDataHolder {
//    private static final EntityDataAccessor<Boolean> HIRED = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.BOOLEAN);
//    private static final EntityDataAccessor<Integer> AMOUNT_NEEDED = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.INT);
//
//    // first and last names
//    private static final EntityDataAccessor<String> FIRST_NAME = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.STRING);
//    private static final EntityDataAccessor<String> LAST_NAME = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.STRING);
    // age group, synced so the client can scale the model and label the recruit dialog
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.INT);

    // Random pool of last names
    public static final String[] LAST_NAMES = new String[] {
        "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor",
        "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson",
        "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen", "Young", "Hernandez", "King", "Wright",
        "Lopez", "Hill", "Scott", "Green", "Adams", "Baker", "Gonzalez", "Nelson", "Carter", "Mitchell", "Perez",
        "Roberts", "Turner", "Phillips", "Campbell", "Parker", "Evans", "Edwards", "Collins", "Stewart", "Sanchez",
        "Morris", "Rogers", "Reed", "Cook", "Morgan", "Bell", "Murphy", "Bailey", "Rivera", "Cooper", "Richardson",
        "Cox", "Howard", "Ward", "Torres", "Peterson", "Gray", "Ramirez", "James", "Watson", "Brooks", "Kelly",
        "Sanders", "Price", "Bennett", "Wood", "Barnes", "Ross", "Henderson", "Coleman", "Jenkins", "Perry", "Powell",
        "Long", "Patterson", "Hughes", "Flores", "Washington", "Butler", "Simmons", "Foster", "Gonzales", "Bryant",
        "Alexander", "Russell", "Griffin", "Diaz", "Hayes"
    };

    // Random pool of first names
    public static final String[] FIRST_NAMES = new String[] {
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles",
        "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald", "Steven", "Paul", "Andrew", "Joshua",
        "Kenneth", "Kevin", "Brian", "George", "Edward", "Ronald", "Timothy", "Jason", "Jeffrey", "Ryan", "Jacob",
        "Gary", "Nicholas", "Eric", "Stephen", "Jonathan", "Larry", "Justin", "Scott", "Brandon", "Frank", "Benjamin",
        "Gregory", "Samuel", "Raymond", "Patrick", "Alexander", "Jack", "Dennis", "Jerry", "Tyler", "Aaron", "Jose",
        "Henry", "Adam", "Douglas", "Nathan", "Peter", "Zachary", "Kyle", "Walter", "Harold", "Jeremy", "Ethan",
        "Carl", "Keith", "Roger", "Gerald", "Christian", "Terry", "Sean", "Arthur", "Austin", "Noah", "Lawrence",
        "Jesse", "Joe", "Bryan", "Billy", "Jordan", "Albert", "Dylan", "Bruce", "Willie", "Gabriel", "Alan", "Juan",
    };
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);

    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;
    //duck_egg: 我不知道这个hired是做什么的，暂且保留
    private boolean hired = false;
    // town-spawned refugees: server-side only, persisted via NBT, never synced
    private boolean townSpawned = false;
    private int waitingDays = 0;
    /** 上次结算等待天数的世界日；-1 表示尚未初始化（新刷/旧存档），加载后首日宽限 */
    private long lastWaitingCheckDay = -1L;
    @Nullable
    private UUID townOwner = null;
    private boolean coldSurvivor = false;
    private int amountNeeded = 3 + (int) (getRandom().nextFloat() * 5);
    @Getter
    private String lastName = LAST_NAMES[(int) (Math.random() * LAST_NAMES.length)];
    @Getter
    private String firstName = FIRST_NAMES[(int) (Math.random() * FIRST_NAMES.length)];
    FHVillagerData fh$data = new FHVillagerData(this);

    public WanderingRefugee(EntityType<? extends AbstractVillager> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public int getAgeGroup() {
        return this.entityData.get(AGE);
    }

    public void setAgeGroup(int ageGroup) {
        this.entityData.set(AGE, ageGroup);
    }

    /**
     * Model scale of the age group: infants 0.4, children 0.5, everyone else 1.0.
     */
    public float getAgeScale() {
        return switch (this.getAgeGroup()) {
            case Resident.AGE_INFANT -> 0.4F;
            case Resident.AGE_CHILD -> 0.5F;
            default -> 1.0F;
        };
    }

    public boolean isTownSpawned() {
        return townSpawned;
    }

    public void markTownSpawned(UUID owner) {
        this.townSpawned = true;
        this.townOwner = owner;
    }

    public UUID getTownOwner() {
        return townOwner;
    }

    public int getWaitingDays() {
        return waitingDays;
    }

    public void increaseWaitingDays() {
        this.waitingDays++;
    }

    @Override
    public void tick() {
        super.tick();
        // 等待结算只在服务端、且仅对城镇刷出的难民生效；每刻只做一次布尔+整数比较
        if (!this.level().isClientSide && this.townSpawned) {
            this.tickRefugeeWaitingCheck();
        }
    }

    /**
     * 城镇刷出难民的每日清场（由实体自理，无队伍侧登记/扫描）：
     * 按真实经过的游戏日补算等待天数（同一天只结算一次），等待超时或城镇无空房位/数据缺失时离开。
     * 区块卸载/未加载期间的天数在重载时一次性补齐，等待天数始终真实。
     * 使用 WorldClockSource 的逻辑日期：睡觉跳时会推进日期，/time set 回退不会让日期倒退。
     */
    private void tickRefugeeWaitingCheck() {
        if (this.townOwner == null) return;
        long day = WorldClimate.getWorldDay(this.level());
        if (this.lastWaitingCheckDay == -1L) {
            // 新刷出/旧存档：当天宽限，次日起按日界结算
            this.lastWaitingCheckDay = day;
            return;
        }
        long elapsedDays = day - this.lastWaitingCheckDay;
        if (elapsedDays <= 0) return;
        this.lastWaitingCheckDay = day;
        this.waitingDays = (int) Math.min(Integer.MAX_VALUE, (long) this.waitingDays + elapsedDays);
        if (this.waitingDays >= FHConfig.SERVER.TOWN.REFUGEE_SPAWN.maxWaitDays.get() || !this.canTownStillHost()) {
            this.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * 本队城镇是否还有空房位：按 townOwner（= TeamDataHolder.getId()）反查队伍数据。
     * 队伍数据查不到（队伍解散等）视为无房位，难民次日离开，不会永久滞留。
     */
    private boolean canTownStillHost() {
        TeamDataHolder holder = CTeamDataManager.getDataByResearchID(this.townOwner);
        if (holder == null) return false;
        return holder.getOptional(FHSpecialDataTypes.TOWN_DATA)
                .map(townData -> townData.createTeamTown().canAddResident())
                .orElse(false);
    }

    @Override
    public boolean isChildTrader() {
        return this.getAgeGroup() == Resident.AGE_CHILD;
    }

    public boolean isColdSurvivor() {
        return coldSurvivor;
    }

    public void setColdSurvivor(boolean coldSurvivor) {
        this.coldSurvivor = coldSurvivor;
    }

    /**
     * Scales the hitbox with the age group. The eye height follows automatically
     * because the base eye height derives from {@link #getDimensions(Pose)}.
     */
    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return super.getDimensions(pPose).scale(this.getAgeScale());
    }

    /*
    @Override
    protected @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemStack = player.getItemInHand(hand);
            if (hired) {
                player.displayClientMessage(Lang.translateMessage("refugee.hired", player.getName().getString()), false);
                return InteractionResult.CONSUME;
            } else if (itemStack.is(FHTags.Items.REFUGEE_NEEDS.tag)) {
                // try shrink stack and decrement amount needed
                int amountNeeded = this.amountNeeded;
                if (amountNeeded > 0) {
                    amountNeeded--;
                    this.amountNeeded = amountNeeded;
                    itemStack.shrink(1);
                    if (amountNeeded > 0) {
                        player.displayClientMessage(Lang.translateMessage("refugee.unsatisfied"), false);
                        return InteractionResult.CONSUME;
                    } else {
                        player.displayClientMessage(Lang.translateMessage("refugee.satisfied"), false);
                        // Get town of player
                        TeamTown town = TeamTown.from(player);
                        // Add resident
                        Resident resident = new Resident(firstName, lastName);
                        town.addResident(resident);
                        // hire
                        hired = true;
                        return InteractionResult.CONSUME;
                    }
                }
            }
            // tell player they need to give the refugee something
            player.displayClientMessage(Lang.translateMessage("refugee.needs", firstName, lastName), false);
            return InteractionResult.CONSUME;
        }
    }
    */

    public @NotNull InteractionResult mobInteract(Player playerIn, @NotNull InteractionHand hand) {
        // FHMain.LOGGER.info("Villager mobInteract side = {}", level().isClientSide ? "CLIENT" : "SERVER");
        ItemStack itemstack = playerIn.getItemInHand(hand);
        if (itemstack.getItem() == Items.VILLAGER_SPAWN_EGG || !this.isAlive() || this.isTrading() || this.isSleeping() || playerIn.isSecondaryUseActive()) {
            return super.mobInteract(playerIn, hand);
        }
        if (this.isBaby()) {
            // this.setUnhappy();
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if(this.level().isClientSide){
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                WanderingRefugeeClientHelper.openScreen(this);
            });
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /**
     *  Trade with player.
     *  maybe only run on server side?<br>
     * need to check some precondition, you can see{@link #mobInteract(Player, InteractionHand)}<br>
     * This method is different with {@link #openTradingScreen(Player, Component, int)} , just have same name.
     * @return if trade successfully worked
     */
    public boolean openTradingScreen(ServerPlayer playerIn){
        // 幼儿不会交易；守卫放在 update 之前，拒绝交易不触发关系衰减
        if (this.getAgeGroup() == Resident.AGE_INFANT) {
            playerIn.displayClientMessage(Component.translatable("message.frostedheart.trade.too_young"), false);
            return false;
        }
        fh$data.update((ServerLevel) super.level(), playerIn);
        RelationList list = fh$data.getRelationShip(playerIn);
        int unknownLanguage = list.get(RelationModifier.UNKNOWN_LANGUAGE);
        if (list.sum() < TradeConstants.RELATION_TO_TRADE) {
            //this.setUnhappy();
            if (unknownLanguage < 0) {
                playerIn.displayClientMessage(Component.translatable("message.frostedheart.trade.language_barrier"), false);
            } else {
                playerIn.displayClientMessage(Component.translatable("message.frostedheart.trade.bad_relation"), false);
            }
            return false;
        } else if (list.sum() < TradeConstants.RELATION_TO_BARGAIN) {
            playerIn.displayClientMessage(Component.translatable("message.frostedheart.trade.normal_relation"), false);
        } else {
            playerIn.displayClientMessage(Component.translatable("message.frostedheart.trade.great_relation"), false);
        }
        float t = WorldTemperature.block(level(), blockPosition());
        if (t < 0) {
            playerIn.displayClientMessage(Component.translatable("message.frostedheart.trade.low_temp"), false);
        }
        playerIn.awardStat(Stats.TALKED_TO_VILLAGER);
        setTradingPlayer(playerIn);
        TradeHandler.openTradeScreen(playerIn, fh$data);
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AGE, Resident.AGE_ADULT);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer pOffer) {

    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("hired", this.hired);
        pCompound.putInt("amountNeeded", this.amountNeeded);
        pCompound.putString("lastName", this.lastName);
        pCompound.putString("firstName", this.firstName);
        pCompound.putInt("age", this.getAgeGroup());
        pCompound.putBoolean("townSpawned", this.townSpawned);
        pCompound.putInt("waitingDays", this.waitingDays);
        pCompound.putLong("lastWaitingCheckDay", this.lastWaitingCheckDay);
        if (this.townOwner != null) {
            pCompound.putUUID("townOwner", this.townOwner);
        }
        pCompound.putBoolean("coldSurvivor", this.coldSurvivor);
        CompoundTag cnbt = new CompoundTag();
        fh$data.serialize(cnbt);
        pCompound.put("fhdata", cnbt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if ((pCompound.contains("amountNeeded", Tag.TAG_INT))) {
            amountNeeded = pCompound.getInt("amountNeeded");
        }
        if ((pCompound.contains("lastName", Tag.TAG_STRING))) {
            lastName = pCompound.getString("lastName");
        }
        if ((pCompound.contains("firstName", Tag.TAG_STRING))) {
            firstName = pCompound.getString("firstName");
        }
        if ((pCompound.contains("hired", Tag.TAG_BYTE))) {
            hired = pCompound.getBoolean("hired");
        }
        if ((pCompound.contains("age", Tag.TAG_INT))) {
            this.setAgeGroup(pCompound.getInt("age"));
        }
        if ((pCompound.contains("townSpawned", Tag.TAG_BYTE))) {
            townSpawned = pCompound.getBoolean("townSpawned");
        }
        if ((pCompound.contains("waitingDays", Tag.TAG_INT))) {
            waitingDays = pCompound.getInt("waitingDays");
        }
        if ((pCompound.contains("lastWaitingCheckDay", Tag.TAG_ANY_NUMERIC))) {
            // getLong 同时兼容旧存档中的 TAG_INT。
            lastWaitingCheckDay = pCompound.getLong("lastWaitingCheckDay");
        }
        if ((pCompound.contains("townOwner", Tag.TAG_INT_ARRAY))) {
            townOwner = pCompound.getUUID("townOwner");
        }
        if ((pCompound.contains("coldSurvivor", Tag.TAG_BYTE))) {
            coldSurvivor = pCompound.getBoolean("coldSurvivor");
        }
        if ((pCompound.contains("fhdata", Tag.TAG_COMPOUND))) {
            fh$data.deserialize(pCompound.getCompound("fhdata"));
        }

    }

    @Override
    protected void updateTrades() {

    }

    @Override
    public void registerGoals() {
        // todo: add move toward higher temperature goal
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Evoker.class, 12.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vindicator.class, 8.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vex.class, 8.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Pillager.class, 15.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Illusioner.class, 12.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zoglin.class, 10.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Wolf.class, 10.0F, 0.5D, 0.5D));
//        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 1.25D, 16.0F));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.25D, Ingredient.of(FHTags.Items.REFUGEE_NEEDS.tag), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new AttractedByGeneratorGoal(this,1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, (entity) -> {
            return entity instanceof Enemy && !(entity instanceof Creeper);
        }));
        // todo: add hunting goal
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    @Override
    public Component getDisplayName() {
        if (hired) {
            return Components.str(this.firstName + " " + this.lastName);
        }
        return super.getDisplayName();
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int pRemainingPersistentAngerTime) {
        this.remainingPersistentAngerTime = pRemainingPersistentAngerTime;
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID pPersistentAngerTarget) {
        this.persistentAngerTarget = pPersistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public FHVillagerData getFHData() {
        return fh$data;
    }


}
