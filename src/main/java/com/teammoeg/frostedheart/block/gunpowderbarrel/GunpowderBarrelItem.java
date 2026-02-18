package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunpowderBarrelItem extends BlockItem {

    public GunpowderBarrelItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @SubscribeEvent
    public static void handlePlayerUsingItem(LivingEntityUseItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            var stack = event.getItem();
            var level = player.level();
            if (stack.getItem() instanceof GunpowderBarrelItem barrel) {
                int max = barrel.getUseDuration(stack);
                int use = max - event.getDuration();
                if (use == 18) player.playSound(SoundEvents.FLINTANDSTEEL_USE);
                if (use < 50 && use%5 == 0 && level.random.nextInt(200) < use)
                    player.playSound(SoundEvents.FLINTANDSTEEL_USE);
                if (use == 55) player.playSound(SoundEvents.FLINTANDSTEEL_USE);
                if (use == 59) player.playSound(SoundEvents.FLINTANDSTEEL_USE);
                if (use == 60) player.playSound(SoundEvents.TNT_PRIMED);
                if (use == max-1) {
                    GunpowderBarrelBlock.explode(level, player.blockPosition(), stack, player, false);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void handleExplosion(ExplosionEvent.Detonate event) {
        // 防止摧毁掉落物
        if (!event.getExplosion().interactsWithBlocks()) {
            event.getAffectedEntities().removeIf(entity -> entity instanceof ItemEntity);
            var e = event.getExplosion().getIndirectSourceEntity();
            if (e != null) event.getAffectedEntities().add(e);
        }
        // 触发炸药桶
        var level = event.getLevel();
        var blocks = event.getAffectedBlocks();
        for (Iterator<BlockPos> iterator = blocks.iterator(); iterator.hasNext();) {
            BlockPos pos = iterator.next();
            if (level.getBlockState(pos).getBlock() instanceof GunpowderBarrelBlock && GunpowderBarrelBlock.willFall(level, pos)) {
                iterator.remove();
                event.getAffectedEntities().add(GunpowderBarrelEntity.fall(level, pos,
                        GunpowderBarrelBlock.getRange(level, pos),
                        GunpowderBarrelBlock.getFortuneLevel(level, pos),
                        event.getExplosion().getIndirectSourceEntity()));
                level.removeBlock(pos, false);
            }
        }
    }

    public static ItemStack create(int range, int fortuneLevel, boolean willFall) {
        var stack = FHBlocks.GUNPOWDER_BARREL.asStack();
        var tag = new CompoundTag();
        if (range != 1) tag.putInt(GunpowderBarrelBlock.RANGE, range);
        if (fortuneLevel != 0) tag.putInt(GunpowderBarrelBlock.FORTUNE, fortuneLevel);
        if (willFall) tag.putBoolean(GunpowderBarrelBlock.WILL_FALL, true);
        if (!tag.isEmpty()) stack.setTag(tag);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity pLivingEntity, int timeCharged) {
        if (pLivingEntity instanceof Player player) {
            int use = getUseDuration(stack) - timeCharged;
            if (use < 60) {
                player.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 10F);
                return;
            }
            if (use >= getUseDuration(stack)-1) {
                return;
            }
            if (!level.isClientSide()) {
                var barrel = new GunpowderBarrelEntity(level, player);
                barrel.setItem(stack);
                barrel.shootFromRotation(player, Math.max(player.getXRot()-10, -90), player.getYRot(), 0.0F, 3 * (float)use/getUseDuration(stack), 1.0F);
                level.addFreshEntity(barrel);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1, 0.1F);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        pPlayer.startUsingItem(pUsedHand);
        return InteractionResultHolder.consume(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        int f = GunpowderBarrelBlock.getFortuneLevel(pStack);
        if (f > 0) {
            pTooltip.add(Enchantments.BLOCK_FORTUNE.getFullname(f));
        }

        int range = GunpowderBarrelBlock.getRange(pStack)*2+1;
        pTooltip.add(Component.translatable("tooltip.frostedheart.range")
                .append(range + "x" + range + "x" + range)
                .withStyle(ChatFormatting.GRAY));

        if (GunpowderBarrelBlock.willFall(pStack)) {
            pTooltip.add(Component.translatable("tooltip.frostedheart.gunpowder_barrel.will_fall")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return pStack.getTag() != null && pStack.getTag().contains(GunpowderBarrelBlock.FORTUNE);
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 160;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.SPEAR;
    }
}
