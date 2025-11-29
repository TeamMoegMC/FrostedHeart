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

import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.AttractedByGeneratorGoal;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.trade.*;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
    private int amountNeeded = 3 + (int) (getRandom().nextFloat() * 5);
    @Getter
    private String lastName = LAST_NAMES[(int) (Math.random() * LAST_NAMES.length)];
    @Getter
    private String firstName = FIRST_NAMES[(int) (Math.random() * FIRST_NAMES.length)];
    FHVillagerData fh$data = new FHVillagerData(this);

    public WanderingRefugee(EntityType<? extends AbstractVillager> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
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
            Minecraft.getInstance().setScreen(new WanderingRefugeeScreen(this));
            return InteractionResult.sidedSuccess(this.level().isClientSide);
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

//    @Override
//    protected void defineSynchedData() {
//        super.defineSynchedData();
//        this.entityData.define(HIRED, false);
//        this.entityData.define(AMOUNT_NEEDED,  3 + (int) (getRandom().nextFloat() * 5));
//        this.entityData.define(LAST_NAME, LAST_NAMES[(int) (Math.random() * LAST_NAMES.length)]);
//        this.entityData.define(FIRST_NAME, FIRST_NAMES[(int) (Math.random() * FIRST_NAMES.length)]);
//    }

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
