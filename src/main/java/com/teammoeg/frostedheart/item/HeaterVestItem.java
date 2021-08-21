package com.teammoeg.frostedheart.item;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.util.EnergyHelper;
import com.teammoeg.frostedheart.client.model.HeaterVestModel;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Heater Vest: wear it to warm yourself from the coldness.
 * 加温背心：穿戴抵御寒冷
 */
public class HeaterVestItem extends FHBaseItem implements EnergyHelper.IIEEnergyItem {

    public HeaterVestItem(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return 100000;
    }

    @Nullable
    @Override
    public EquipmentSlotType getEquipmentSlot(ItemStack stack) {
        return EquipmentSlotType.CHEST;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
        return "immersiveengineering:textures/models/powerpack.png";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BipedModel getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot, BipedModel _default) {
        return HeaterVestModel.getModel();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        String stored = this.getEnergyStored(stack) + "/" + this.getMaxEnergyStored(stack);
        list.add(new TranslationTextComponent(Lib.DESC + "info.energyStored", stored));
    }


}
