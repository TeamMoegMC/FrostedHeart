package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.util.IMilkable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CowEntity.class)
public abstract class CowEntityMixin extends AnimalEntity implements IMilkable {
    private final static ResourceLocation cow_feed = new ResourceLocation(FHMain.MODID, "cow_feed");
    private EatGrassGoal eatGrassGoal;

    @Override
    public byte getMilk() {
        return milk;
    }

    @Override
    public void setMilk(byte milk) {
        this.milk = milk;
    }

    @Override
    public void eatGrassBonus() {
        if (feeded < 2)
            feeded++;
    }

    byte feeded;
    int digestTimer;
    byte milk;
    short hxteTimer;

    protected CowEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
        super(type, worldIn);
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
            float temp = ChunkData.getTemperature(this.getEntityWorld(), this.getPosition());
            if (temp < WorldClimate.ANIMAL_ALIVE_TEMPERATURE || temp > WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (hxteTimer < 100) {
                    hxteTimer++;
                } else {
                    hxteTimer = 0;
                    this.attackEntityFrom(temp > 0 ? FHDamageSources.HYPERTHERMIA : FHDamageSources.HYPOTHERMIA, 2);
                }
            } else if (hxteTimer > 0)
                hxteTimer--;
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

    /**
     * @author khjxiaogu
     * @reason change to our own milk logic
     */
    @Override
    @Overwrite
    public ActionResultType getEntityInteractionResult(PlayerEntity playerIn, Hand hand) {
        ItemStack itemstack = playerIn.getHeldItem(hand);

        if (!this.isChild() && !itemstack.isEmpty() && itemstack.getItem().getTags().contains(cow_feed)) {
            if (feeded < 2) {
                ActionResultType parent = ActionResultType.PASS;
                if (this.isBreedingItem(itemstack))
                    parent = super.getEntityInteractionResult(playerIn, hand);
                if (!parent.isSuccessOrConsume() && !this.world.isRemote)
                    this.consumeItemFromStack(playerIn, itemstack);
                feeded++;
                return ActionResultType.func_233537_a_(this.world.isRemote);
            }
        }

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
                    playerIn.sendMessage(GuiUtils.translateMessage("cow.nomilk.hungry"), playerIn.getUniqueID());
                else
                    playerIn.sendMessage(GuiUtils.translateMessage("cow.nomilk.digest"), playerIn.getUniqueID());
            }
        }
        return super.getEntityInteractionResult(playerIn, hand);
    }
}
