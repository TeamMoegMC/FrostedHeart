package com.teammoeg.frostedheart.mixin.rankine;

import com.cannolicatfish.rankine.items.MortarItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;
import java.util.List;
@Mixin(MortarItem.class)
public class MixinMortarItem extends Item {

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
