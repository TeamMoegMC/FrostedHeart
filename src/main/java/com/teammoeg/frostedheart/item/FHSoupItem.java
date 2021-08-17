package com.teammoeg.frostedheart.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;

public class FHSoupItem extends FHBaseItem {

    private final boolean isPoor;

    public FHSoupItem(String name, Properties properties, boolean isPoorlyMade) {
        super(name, properties);
        isPoor = isPoorlyMade;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        ItemStack itemstack = super.onItemUseFinish(stack, worldIn, entityLiving);

        // Punish the player since soup item is not properly made
        if (this.isPoor && entityLiving instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entityLiving;
            if (worldIn.getRandom().nextInt(3) == 0) {
                player.addPotionEffect(new EffectInstance(Effects.HUNGER, 100, 1));
                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 40, 1));
                if (ModList.get().isLoaded("diet") && player.getServer() != null) {
                    player.getServer().getCommandManager().handleCommand(player.getCommandSource(), "/diet subtract @s proteins 0.01");
                }
            }
        }

        return entityLiving instanceof PlayerEntity && ((PlayerEntity)entityLiving).abilities.isCreativeMode ? itemstack : new ItemStack(Items.BOWL);
    }

    public boolean isPoor() {
        return isPoor;
    }
}
