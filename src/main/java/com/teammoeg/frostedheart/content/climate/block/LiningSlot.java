package com.teammoeg.frostedheart.content.climate.block;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class LiningSlot extends SlotItemHandler{
	   public static final ResourceLocation EMPTY_LINING_SLOT_HELMET = FHMain.rl("item/empty_lining_slot_head");
	   public static final ResourceLocation EMPTY_LINING_SLOT_CHESTPLATE = FHMain.rl("item/empty_lining_slot_body");
	   public static final ResourceLocation EMPTY_LINING_SLOT_LEGGINGS = FHMain.rl("item/empty_lining_slot_leg");
	   public static final ResourceLocation EMPTY_LINING_SLOT_BOOTS = FHMain.rl("item/empty_lining_slot_feet");
	   public static final ResourceLocation EMPTY_LINING_SLOT_HAND = FHMain.rl("item/empty_lining_slot_hands");
	public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{EMPTY_LINING_SLOT_HELMET,EMPTY_LINING_SLOT_CHESTPLATE, EMPTY_LINING_SLOT_HAND, EMPTY_LINING_SLOT_LEGGINGS ,EMPTY_LINING_SLOT_BOOTS};
	
	Player owner;
	BodyPart part;
	public LiningSlot(Player owner, BodyPart part, IItemHandler pContainer, int pSlot, int pX,
			int pY) {
		super(pContainer, pSlot, pX, pY);
		this.owner=owner;
		this.part=part;
	}

	public int getMaxStackSize() {
		return 1;
	}
	public boolean mayPlace(ItemStack p_39746_) {
		return (part.slot.getType()==EquipmentSlot.Type.ARMOR&&p_39746_.canEquip(part.slot, owner))||ArmorTempData.getData(p_39746_, part)!=null;
	}
	public boolean mayPickup(Player p_39744_) {
		ItemStack itemstack = this.getItem();
		return !itemstack.isEmpty() && !p_39744_.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false
				: super.mayPickup(p_39744_);
	}
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
		return Pair.of(InventoryMenu.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[part.ordinal()]);
	}
}