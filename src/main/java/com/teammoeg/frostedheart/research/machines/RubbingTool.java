package com.teammoeg.frostedheart.research.machines;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;

public class RubbingTool extends FHBaseItem{

    public RubbingTool(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        playerIn.setActiveHand(handIn);
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getHeldItem(handIn));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (worldIn.isRemote) return stack;
        if(stack.getDamage()>=stack.getMaxDamage())return  stack;
        
        PlayerEntity entityplayer = entityLiving instanceof PlayerEntity ? (PlayerEntity) entityLiving : null;
        if (entityplayer instanceof ServerPlayerEntity) {
            BlockRayTraceResult brtr = rayTrace(worldIn, entityplayer, FluidMode.NONE);
            if (brtr.getType() == Type.MISS) return stack;
            TileEntity te=Utils.getExistingTileEntity(worldIn,brtr.getPos());
            if(te instanceof MechCalcTileEntity) {
            	MechCalcTileEntity mcte=(MechCalcTileEntity) te;
            	int crp=mcte.currentPoints;
            	mcte.currentPoints=0;
            	mcte.updatePoints();
            	if(crp>0) {
            		stack.damageItem(1,entityplayer, ex -> {});
            		crp+=getPoint(stack);
            		setPoint(stack,crp);
            	}
            }
        }
        return stack;
    }
    public int getPoint(ItemStack stack) {
    	return stack.getOrCreateTag().getInt("points");
    }
    public void setPoint(ItemStack stack,int val) {
    	stack.getOrCreateTag().putInt("points",val);
    }
    public void setResearch(ItemStack stack,String rs) {
    	stack.getOrCreateTag().putString("research",rs);
    }
    public String getResearch(ItemStack stack) {
    	return stack.getOrCreateTag().getString("research");
    }
    @Override
    public int getUseDuration(ItemStack stack) {
        return 20;
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

}
