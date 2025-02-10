package com.teammoeg.frostedheart.content.climate.block.wardrobe;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.block.LiningSlot;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.IItemHandler;

public class WardrobeMenu extends CBlockEntityMenu<WardrobeBlockEntity> {
	int page;
	protected class PagedLiningSlot extends LiningSlot{
		final int slotPage;

		public PagedLiningSlot(Player owner, BodyPart part, IItemHandler pContainer, int pSlot, int pX, int pY,
				int slotPage) {
			super(owner, part, pContainer, pSlot, pX, pY);
			this.slotPage = slotPage;
		}

		@Override
		public boolean isActive() {
			return slotPage==page;
		}
		
	}
	public WardrobeMenu(int id, Inventory inventoryPlayer, WardrobeBlockEntity tile) {
		super(FHMenuTypes.WARDROBE.get(),tile,id, inventoryPlayer.player,37);
		blockEntity=tile;
		/*for(int p=0;p<3;p++) {
	        for(int k=0;k<1;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.HEAD,blockEntity, 8*p+0+k, 100+k*18, 7,p));
	        }
	        for(int k=0;k<3;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.TORSO,blockEntity, 8*p+1+k, 100+k*18, 30,p));
	        }
	        for(int k=0;k<3;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.LEGS,blockEntity, 8*p+4+k, 100+k*18, 53,p));
	        }
	        for(int k=0;k<1;++k) {
	            this.addSlot(new PagedLiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.FEET,blockEntity, 8*p+7+k, 100+k*18, 76,p));
	        }
		}*/
		super.addPlayerInventory(inventoryPlayer, 8, 120, 178);
	}
	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
		if (pPlayer instanceof ServerPlayer) {
			WardrobeBlock.setOpened(blockEntity.getLevel(),blockEntity.getBlockPos(),blockEntity.getBlockState(),pPlayer,false);
		}
	}
}
