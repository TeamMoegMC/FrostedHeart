/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.mixin.IFeedStore;
import com.teammoeg.frostedheart.util.mixin.IMilkable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.BreedGoal;
import net.minecraft.entity.ai.goal.EatGrassGoal;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.PanicGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DrinkHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

@Mixin(CowEntity.class)
public abstract class CowEntityMixin extends AnimalEntity implements IMilkable, IFeedStore {
    private final static ResourceLocation cow_feed = new ResourceLocation(FHMain.MODID, "cow_feed");
    private EatGrassGoal eatGrassGoal;

    byte feeded;

    int digestTimer;

    byte milk;

    short hxteTimer;
    protected CowEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
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
    public void eatGrassBonus() {

        if (this.isChild()) {
            this.addGrowth(60);
        } else if (feeded < 2)
            feeded++;

    }

    /**
     * @author khjxiaogu
     * @reason change to our own milk logic
     */
    @Overwrite
    public ActionResultType getEntityInteractionResult(PlayerEntity playerIn, Hand hand) {
        ItemStack itemstack = playerIn.getHeldItem(hand);
        //FHMain.LOGGER.info("start feed"+this.isInLove());
        if (!this.isChild() && !itemstack.isEmpty() && itemstack.getItem().getTags().contains(cow_feed)) {
            if (feeded < 2) {
                feeded++;
                //FHMain.LOGGER.info("yield feed"+this.isInLove());
                if (!this.world.isRemote)
                    this.consumeItemFromStack(playerIn, itemstack);
                //FHMain.LOGGER.info("ret feed"+this.isInLove());
                return ActionResultType.func_233537_a_(this.world.isRemote);
            }
        }
        //FHMain.LOGGER.info("end feed"+this.isInLove());
        if (itemstack.getItem() == Items.BUCKET) {
            if (milk > 0 && !this.isChild()) {
                playerIn.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
                ItemStack itemstack1 = DrinkHelper.fill(itemstack, playerIn, Items.MILK_BUCKET.getDefaultInstance());
                playerIn.setHeldItem(hand, itemstack1);
                milk--;
                return ActionResultType.func_233537_a_(this.world.isRemote);
            }
            if (!world.isRemote) {
                if (feeded <= 0)
                    playerIn.sendStatusMessage(TranslateUtils.translateMessage("cow.nomilk.hungry"), true);
                else
                    playerIn.sendStatusMessage(TranslateUtils.translateMessage("cow.nomilk.digest"), true);
            }
        }
        return super.getEntityInteractionResult(playerIn, hand);
    }

    @Override
    public byte getMilk() {
        return milk;
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
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
        eatGrassGoal = new EatGrassGoal(this);
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.fromItems(Items.WHEAT), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, eatGrassGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
    }

    @Override
    public void setMilk(byte milk) {
        this.milk = milk;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isRemote) {
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
            if (FHUtils.isBlizzardHarming(world, this.getPosition())) {
                if (hxteTimer < 20) {
                    hxteTimer++;
                } else {
                    this.attackEntityFrom(FHDamageSources.BLIZZARD, 1);
                }
            } else {
                float temp = ChunkHeatData.getTemperature(this.getEntityWorld(), this.getPosition());
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
                        this.attackEntityFrom(temp > 0 ? FHDamageSources.HYPERTHERMIA : FHDamageSources.HYPOTHERMIA, 2);
                    }
                } else if (hxteTimer > 0)
                    hxteTimer--;
            }
        }
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putByte("milk_stored", milk);
        compound.putByte("feed_stored", feeded);
        compound.putInt("feed_digest", digestTimer);
        compound.putShort("hxthermia", hxteTimer);
    }
}
