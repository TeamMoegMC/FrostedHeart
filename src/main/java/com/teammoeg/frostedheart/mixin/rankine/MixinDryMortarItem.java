package com.teammoeg.frostedheart.mixin.rankine;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.items.DryMortarItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

@Mixin(DryMortarItem.class)
public abstract class MixinDryMortarItem extends Item{

	public MixinDryMortarItem(Properties properties) {
		super(properties);
	}
	/**
	 * @author khjxiaogu
	 * @reason i18n for rankine
	 */
    @Overwrite
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(GuiUtils.translateTooltip("rankine.dry_mortar").mergeStyle(TextFormatting.GRAY));
    }
}
