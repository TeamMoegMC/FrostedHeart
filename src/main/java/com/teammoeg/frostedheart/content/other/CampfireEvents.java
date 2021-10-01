package com.teammoeg.frostedheart.content.other;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bridge.ICampfireExtra;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.CampfireTileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CampfireEvents {
    @SubscribeEvent
    public void fireStarterFlint(PlayerInteractEvent.RightClickBlock event) {

        World world = event.getWorld();
        PlayerEntity player = event.getPlayer();
        BlockPos blockPos = event.getPos();
        BlockState blockState = event.getWorld().getBlockState(event.getPos());

        if (event.getHand() == Hand.MAIN_HAND)
            if (player.getHeldItemMainhand().getItem() == Items.FLINT && player.getHeldItemOffhand().getItem() == Items.FLINT) {

                if (!CampfireBlock.canBeLit(blockState))
                    return;

                Random rand = event.getWorld().rand;
                player.swingArm(Hand.MAIN_HAND);

                if (rand.nextFloat() < 0.33 && !world.isRemote) {
                    world.setBlockState(blockPos, blockState.with(BlockStateProperties.LIT, Boolean.valueOf(true)), 3);
                }

                world.playSound(null, blockPos, SoundEvents.BLOCK_STONE_STEP, SoundCategory.BLOCKS, 1.0F, 2F + rand.nextFloat() * 0.4F);

                if (world.isRemote) {
                    for (int i = 0; i < 5; i++) {
                        world.addParticle(ParticleTypes.SMOKE, player.getPosX() + player.getLookVec().getX() + rand.nextFloat() * 0.25, player.getPosY() + 0.5f + rand.nextFloat() * 0.25, player.getPosZ() + player.getLookVec().getZ() + rand.nextFloat() * 0.25, 0, 0.01, 0);
                    }
                    world.addParticle(ParticleTypes.FLAME, player.getPosX() + player.getLookVec().getX() + rand.nextFloat() * 0.25, player.getPosY() + 0.5f + rand.nextFloat() * 0.25, player.getPosZ() + player.getLookVec().getZ() + rand.nextFloat() * 0.25, 0, 0.01, 0);

                }
            } else if (player.getHeldItemMainhand().getItem() == Items.DRAGON_BREATH) {

                if (!world.isRemote) {
                    world.setBlockState(blockPos, blockState.with(BlockStateProperties.LIT, Boolean.valueOf(true)), 3);
                    ((ICampfireExtra) world.getTileEntity(blockPos)).setLifeTime(-1337);
                }
                world.playSound(null, blockPos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, 1F);
            }
    }


    @SubscribeEvent
    public void checkCampfireLifeTime(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        BlockState blockState = world.getBlockState(pos);

        if (!(blockState.getBlock() instanceof CampfireBlock) || !player.getHeldItemMainhand().isEmpty())
            return;

        if (event.getHand() == Hand.MAIN_HAND) {
            ICampfireExtra info = (ICampfireExtra) world.getTileEntity(pos);

            if (!world.isRemote && info != null)
                if (blockState.get(CampfireBlock.LIT)) {
                    if (info.getLifeTime() != -1337)
                        player.sendMessage(GuiUtils.translateMessage("campfire.remaining", Integer.toString(info.getLifeTime() / 20)), player.getUniqueID());
                    else
                        player.sendMessage(GuiUtils.translateMessage("campfire.forever"), player.getUniqueID());
                } else {
                    player.sendMessage(GuiUtils.translateMessage("campfire.fuel"), player.getUniqueID());
                }
        }
    }

    private static boolean isSoul(BlockState state) {
        return (state.getBlock().getRegistryName().toString().indexOf("soul") != -1) || (state.getBlock().getRegistryName().toString().indexOf("ender") != -1);
    }

    @SubscribeEvent
    public void campfireSet(BlockEvent.EntityPlaceEvent event) {
        BlockState placed = event.getPlacedBlock();
        BlockPos pos = event.getPos();

        if (!(event.getWorld().getTileEntity(pos) instanceof CampfireTileEntity))
            return;

        CampfireTileEntity tileEntity = (CampfireTileEntity) event.getWorld().getTileEntity(pos);
        ICampfireExtra lifeTime = (ICampfireExtra) tileEntity;

        if (isSoul(placed)) {
            lifeTime.setLifeTime(4000);
        } else {
            lifeTime.setLifeTime(2000);
        }

    }
}
