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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.bootstrap.reference.FHDamageSources;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.frostedheart.util.mixin.IFeedStore;
import com.teammoeg.frostedheart.util.mixin.IMilkable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHDamageTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;

@Mixin(Cow.class)
public abstract class CowMixin extends Animal implements IMilkable, IFeedStore {
    private final static TagKey<Item> cow_feed = ItemTags.create(new ResourceLocation(FHMain.MODID, "cow_feed"));
    private EatBlockGoal eatGrassGoal;

    byte feeded;

    int digestTimer;

    byte milk;

    short hxteTimer;
    protected CowMixin(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
    }/*
	@Override
	public void setInLove(PlayerEntity player) {
		new Exception().printStackTrace();
		super.setInLove(player);
	}
	@Override
	public void setInLove(int player) {
		new Exception().printStackTrace();
		super.setInLove(player);
	}*/
    @Override
    public boolean consumeFeed() {
        if (feeded > 0) {
            feeded--;
            return true;
        }
        if (milk > 0) {
            milk--;
            return true;
        }
        return false;
    }
    @Override
    public void ate() {

        if (this.isBaby()) {
            this.ageUp(60);
        } else if (feeded < 2)
            feeded++;

    }

    /**
     * @author khjxiaogu
     * @reason change to our own milk logic
     */
    @Overwrite
    public InteractionResult mobInteract(Player playerIn, InteractionHand hand) {
        ItemStack itemstack = playerIn.getItemInHand(hand);
        //FHMain.LOGGER.info("start feed"+this.isInLove());
        if (!this.isBaby() && !itemstack.isEmpty() && itemstack.is(cow_feed)) {
            if (feeded < 2) {
                feeded++;
                //FHMain.LOGGER.info("yield feed"+this.isInLove());
                if (!this.level().isClientSide)
                    this.usePlayerItem(playerIn,hand, itemstack);
                //FHMain.LOGGER.info("ret feed"+this.isInLove());
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        //FHMain.LOGGER.info("end feed"+this.isInLove());
        if (itemstack.getItem() == Items.BUCKET) {
            if (milk > 0 && !this.isBaby()) {
                playerIn.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
                ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, playerIn, Items.MILK_BUCKET.getDefaultInstance());
                playerIn.setItemInHand(hand, itemstack1);
                milk--;
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (!level().isClientSide) {
                if (feeded <= 0)
                    playerIn.displayClientMessage(Lang.translateMessage("cow.nomilk.hungry"), true);
                else
                    playerIn.displayClientMessage(Lang.translateMessage("cow.nomilk.digest"), true);
            }
        }
        return super.mobInteract(playerIn, hand);
    }

    @Override
    public byte getMilk() {
        return milk;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        milk = compound.getByte("milk_stored");
        feeded = compound.getByte("feed_stored");
        digestTimer = compound.getInt("feed_digest");
        hxteTimer = compound.getShort("hxthermia");
    }

    /**
     * @author khjxiaogu
     * @reason make cow eat grass
     */
    @Overwrite
    @Override
    protected void registerGoals() {
        eatGrassGoal = new EatBlockGoal(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.of(Items.WHEAT), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, eatGrassGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void setMilk(byte milk) {
        this.milk = milk;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (digestTimer > 0) {
                digestTimer--;
                if (digestTimer == 0) {
                    if (feeded > 0) {
                        feeded--;
                        if (milk < 2)
                            milk++;
                    }
                }
            } else if (feeded > 0) {
                digestTimer = 14400;
            }
            if (WorldTemperature.isBlizzardHarming(level(), this.blockPosition())) {
                if (hxteTimer < 20) {
                    hxteTimer++;
                } else {
                	 this.hurt(FHDamageSources.blizzard(level()), 1);
                }
            } else {
                float temp = WorldTemperature.block(this.getCommandSenderWorld(), this.blockPosition());
                if (temp < WorldTemperature.ANIMAL_ALIVE_TEMPERATURE
                        || temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                    if (hxteTimer < 100) {
                        hxteTimer++;
                    } else {
                        if (temp > WorldTemperature.FEEDED_ANIMAL_ALIVE_TEMPERATURE)
                            if (((IFeedStore) this).consumeFeed()) {
                                hxteTimer = -7900;
                                return;
                            }

                        hxteTimer = 0;
                        if (temp > 0) {
                            this.hurt(FHDamageSources.hyperthermia(level()), 2);
                        } else {
                            this.hurt(FHDamageSources.hypothermia(level()), 2);
                        }
                    }
                } else if (hxteTimer > 0)
                    hxteTimer--;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("milk_stored", milk);
        compound.putByte("feed_stored", feeded);
        compound.putInt("feed_digest", digestTimer);
        compound.putShort("hxthermia", hxteTimer);
    }
}
