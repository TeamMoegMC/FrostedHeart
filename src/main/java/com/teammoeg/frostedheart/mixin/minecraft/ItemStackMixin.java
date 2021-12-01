package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import com.teammoeg.frostedheart.util.FHEffects;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.UnbreakingEnchantment;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.INameable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

@Mixin(PlayerInventory.class)
public abstract class ItemStackMixin implements IInventory, INameable {
	@Shadow
	public NonNullList<ItemStack> armorInventory;
	@Shadow
	public PlayerEntity player;

	@Inject(at = @At("HEAD"), method = "func_234563_a_")
	public void func_234563_a_(DamageSource p_234563_1_, float p_234563_2_, CallbackInfo cbi) {
		if (p_234563_2_ > 0) {
			if (player.world.isRemote || !(player instanceof ServerPlayerEntity))
				return;
			ServerPlayerEntity player = (ServerPlayerEntity) this.player;
			p_234563_2_ = p_234563_2_ / 4.0F;
			if (p_234563_1_.isFireDamage())// fire damage more
				p_234563_2_ *= 2;
			else if (p_234563_1_.isExplosion())// explode add a lot
				p_234563_2_ *= 4;
			if (p_234563_2_ < 1.0F) {
				p_234563_2_ = 1.0F;
			}
			int amount = (int) p_234563_2_;
			for (ItemStack itemstack : this.armorInventory) {
				CompoundNBT cn = itemstack.getTag();
				if (cn == null)
					return;
				if (amount > 0) {
					String inner = cn.getString("inner_cover");
					if (inner == null || cn.getBoolean("inner_bounded"))
						return;
					int i = getEnchantmentLevel(Enchantments.UNBREAKING,cn);
					int j = 0;

					for (int k = 0; i > 0 && k < amount; ++k) {
						if (UnbreakingEnchantment.negateDamage(itemstack, i, player.getRNG())) {
							++j;
						}
					}

					amount -= j;
					if (amount <= 0) {
						return;
					}
					CompoundNBT cnbt = cn.getCompound("inner_cover_tag");
					if (cnbt == null)
						cnbt = new CompoundNBT();
					int crdmg = cnbt.getInt("Damage");
					crdmg += amount;
					RecipeInner ri = RecipeInner.recipeList.get(new ResourceLocation(inner));

					if (ri != null && ri.getDurability() <= crdmg) {// damaged
						cn.remove("inner_cover");
						cn.remove("inner_cover_tag");
						cn.remove("inner_bounded");
						player.sendBreakAnimation(MobEntity.getSlotForItemStack(itemstack));
					} else {
						cnbt.putInt("Damage", crdmg);
						cn.put("inner_cover_tag", cnbt);
					}
				}
			}

		}
	}

	public static int getEnchantmentLevel(Enchantment enchID,CompoundNBT tags) {
		ResourceLocation resourcelocation = Registry.ENCHANTMENT.getKey(enchID);
		ListNBT listnbt = tags.getList("Enchantments", 10);

		for (int i = 0; i < listnbt.size(); ++i) {
			CompoundNBT compoundnbt = listnbt.getCompound(i);
			ResourceLocation resourcelocation1 = ResourceLocation.tryCreate(compoundnbt.getString("id"));
			if (resourcelocation1 != null && resourcelocation1.equals(resourcelocation)) {
				return MathHelper.clamp(compoundnbt.getInt("lvl"), 0, 255);
			}
		}

		return 0;
	}

}
