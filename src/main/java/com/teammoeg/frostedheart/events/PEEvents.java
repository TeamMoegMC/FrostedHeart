package com.teammoeg.frostedheart.events;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class PEEvents {
	@SuppressWarnings("resource")
	public static void onRC(PlayerInteractEvent.RightClickItem rci) {
		if (!rci.getWorld().isRemote&&rci.getItemStack().getItem().getRegistryName().getNamespace().equals("projecte")) {
			rci.setCancellationResult(ActionResultType.SUCCESS);
			rci.setCanceled(true);
			System.out.println("faq");
			World world = rci.getWorld();
			PlayerEntity player = rci.getPlayer();
			BlockPos pos = rci.getPos();
			ServerWorld serverWorld = (ServerWorld) world;
			ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;

			serverPlayerEntity.addPotionEffect(
					new EffectInstance(Effects.BLINDNESS, (int) (100 * (world.rand.nextDouble() + 0.5)), 3));
			serverPlayerEntity.addPotionEffect(
					new EffectInstance(Effects.NAUSEA, (int) (1000 * (world.rand.nextDouble() + 0.5)), 5));

			serverPlayerEntity.connection.sendPacket(
					new STitlePacket(STitlePacket.Type.TITLE, GuiUtils.translateMessage("too_cold_to_transmute")));
			serverPlayerEntity.connection.sendPacket(
					new STitlePacket(STitlePacket.Type.SUBTITLE, GuiUtils.translateMessage("magical_backslash")));

			double posX = pos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * 4.5D;
			double posY = pos.getY() + world.rand.nextInt(3) - 1;
			double posZ = pos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * 4.5D;
			if (world.hasNoCollisions(EntityType.WITCH.getBoundingBoxWithSizeApplied(posX, posY, posZ))
					&& EntitySpawnPlacementRegistry.canSpawnEntity(EntityType.WITCH, serverWorld, SpawnReason.NATURAL,
							new BlockPos(posX, posY, posZ), world.getRandom())) {
				FHUtils.spawnMob(serverWorld, new BlockPos(posX, posY, posZ), new CompoundNBT(),
						new ResourceLocation("minecraft", "witch"));
			}
		}
	}
}
