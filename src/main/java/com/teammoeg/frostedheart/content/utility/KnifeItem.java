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

package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.base.item.rankine.enchantment.PoisonAspectEnchantment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class KnifeItem extends SwordItem {
    private final float attackDamage;
    private final float attackSpeed;

    public KnifeItem(Tier tier, int attackDamageIn, float attackSpeedIn, Properties properties) {
        super(tier, attackDamageIn, attackSpeedIn, properties);
        this.attackSpeed = attackSpeedIn;
        this.attackDamage = (float)attackDamageIn + tier.getAttackDamageBonus();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(Blocks.COBWEB)) {
            return 20.0F;
        } else if (state.is(BlockTags.LEAVES)) {
            return 10.0F;
        } else {
            //TODO 判断材质给予不同速度
            return 20.0F;
        }
    }


    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        if (entityLiving instanceof Player && ((Player)entityLiving).getOffhandItem().getItem() == this) {
            int i = this.getUseDuration(stack) - timeLeft;
            if (i < 0) return;
            ((Player) entityLiving).getCooldowns().addCooldown(this, 10);
        }
        super.releaseUsing(stack, worldIn, entityLiving, timeLeft);
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        if (handIn == InteractionHand.OFF_HAND) {
            playerIn.startUsingItem(handIn);
            return InteractionResultHolder.consume(itemstack);
        } else {
            return InteractionResultHolder.pass(playerIn.getItemInHand(handIn));
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        applyHitEffects(stack,target,attacker);
        return super.hurtEnemy(stack, target, attacker);
    }

    protected void applyHitEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {


        int i = EnchantmentHelper.getItemEnchantmentLevel(RankineEnchantments.POISON_ASPECT.get(),stack);
        if (i > 0) {
            if (target.getMobType() == MobType.UNDEAD) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,i * 60));
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.POISON,i * 60));
            }

        }
    }

    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player playerIn, LivingEntity entity, InteractionHand hand) {
        if (entity.level().isClientSide) return net.minecraft.world.InteractionResult.PASS;
        if (entity instanceof net.minecraftforge.common.IForgeShearable) {
            net.minecraftforge.common.IForgeShearable target = (net.minecraftforge.common.IForgeShearable)entity;
            BlockPos pos = new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
            if (target.isShearable(stack, entity.level(), pos)) {
                java.util.List<ItemStack> drops = target.onSheared(playerIn, stack, entity.level(), pos,
                        EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, stack));
                java.util.Random rand = new java.util.Random();
                drops.forEach(d -> {
                    ItemEntity ent = entity.spawnAtLocation(d, 1.0F);
                    ent.setDeltaMovement(ent.getDeltaMovement().add((double)((rand.nextFloat() - rand.nextFloat()) * 0.1F), (double)(rand.nextFloat() * 0.05F), (double)((rand.nextFloat() - rand.nextFloat()) * 0.1F)));
                });
                entity.hurt(playerIn.level().damageSources().generic(),2);
                stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(hand));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        if(worldIn.getBlockState(pos).getBlock() instanceof LeavesBlock && EnchantmentHelper.getItemEnchantmentLevel(RankineEnchantments.GRAFTING.get(),stack) >= 1 && !worldIn.isClientSide && worldIn.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                && !worldIn.restoringBlockSnapshots) {
            ResourceLocation orig = ForgeRegistries.BLOCKS.getKey(worldIn.getBlockState(pos).getBlock());
            if (orig != null) {
                ResourceLocation rs = new ResourceLocation(orig.getNamespace(), orig.getPath().split("_leaves")[0] + "_sapling");
                Block sapling = ForgeRegistries.BLOCKS.getValue(rs);
                if (sapling != null) {
                    double d0 = (double) (worldIn.random.nextFloat() * 0.5F) + 0.25D;
                    double d1 = (double) (worldIn.random.nextFloat() * 0.5F) + 0.25D;
                    double d2 = (double) (worldIn.random.nextFloat() * 0.5F) + 0.25D;
                    ItemEntity itementity = new ItemEntity(worldIn, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, new ItemStack(sapling));
                    itementity.setDefaultPickUpDelay();
                    worldIn.addFreshEntity(itementity);
                }
            }
        }
        return super.mineBlock(stack, worldIn, state, pos, entityLiving);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.SWEEPING_EDGE || enchantment == Enchantments.FIRE_ASPECT || enchantment == Enchantments.KNOCKBACK) {
            return false;
        }
        return super.canApplyAtEnchantingTable(stack,enchantment);
    }
}



