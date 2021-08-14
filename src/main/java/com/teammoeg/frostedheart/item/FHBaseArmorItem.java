package com.teammoeg.frostedheart.item;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.FHContent;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;

public class FHBaseArmorItem extends ArmorItem {
    public FHBaseArmorItem(String name, IArmorMaterial materialIn, EquipmentSlotType slot, Properties builderIn) {
        super(materialIn, slot, builderIn);
        setRegistryName(FHMain.MODID, name);
        FHContent.registeredFHItems.add(this);
    }
}
