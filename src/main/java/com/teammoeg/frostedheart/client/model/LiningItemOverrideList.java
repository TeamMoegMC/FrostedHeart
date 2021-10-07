package com.teammoeg.frostedheart.client.model;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class LiningItemOverrideList extends ItemOverrideList {

    public LiningItemOverrideList() {
        super();
    }

    @Override
    public IBakedModel getOverrideModel(IBakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity)
    {
        //TODO: Use lining nbt, this is jsut from example
        int numberOfChessPieces = 0;
        if (stack != null) {
            numberOfChessPieces = stack.getCount();
        }
        return new LiningFinalizedModel(originalModel, numberOfChessPieces);
    }
}
