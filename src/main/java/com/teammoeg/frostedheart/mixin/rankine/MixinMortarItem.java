package com.teammoeg.frostedheart.mixin.rankine;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.items.MortarItem;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
@Mixin(MortarItem.class)
public class MixinMortarItem extends MortarItem {

	public MixinMortarItem(Properties properties) {
		super(properties);
	}
	/**
	 * @author khjxiaogu
	 * @reason removed for crafting
	 */
    @Overwrite
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
    }
}
