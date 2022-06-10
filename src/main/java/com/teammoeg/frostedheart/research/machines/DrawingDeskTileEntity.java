package com.teammoeg.frostedheart.research.machines;

import java.util.Random;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.GenerateInfo;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ResearchGame;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;

public class DrawingDeskTileEntity extends IEBaseTileEntity implements IInteractionObjectIE,IIEInventory{
	protected NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
	ResearchGame game=new ResearchGame();
    public DrawingDeskTileEntity() {
        super(FHContent.FHTileTypes.DRAWING_DESK.get());
    }
	@Override
	public boolean canUseGui(PlayerEntity arg0) {
		return true;
	}
	@Override
	public IInteractionObjectIE getGuiMaster() {
		return this;
	}
	@Override
	public void doGraphicalUpdates() {
	}
	@Override
	public NonNullList<ItemStack> getInventory() {
		return inventory;
	}
	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}
	@Override
	public boolean isStackValid(int slot, ItemStack item) {
		return true;
	}
	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
		if(nbt.contains("gamedata"))
			game.load(nbt.getCompound("gamedata"));
		if(!descPacket) {
			
			ItemStackHelper.loadAllItems(nbt, inventory);
		}
			
		
	}
	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
		nbt.put("gamedata",game.serialize());
		if(!descPacket) {
			
			ItemStackHelper.saveAllItems(nbt, inventory);
		}
	}
	public ResearchGame getGame() {
		return game;
	}
	public void initGame(ServerPlayerEntity player) {
		int lvl=ResearchListeners.fetchGameLevel(player);
		if(lvl<0)return;
		game.init(GenerateInfo.all[lvl],new Random());
		game.setLvl(lvl);
	}
	public void updateGame(ServerPlayerEntity player) {
		if(game.isFinished()) {
			
			ResearchListeners.commitGameLevel(player,game.getLvl());
			game.reset();
		}
	}
	public void submitItem(ServerPlayerEntity sender) {
		ResearchListeners.submitItem(sender,inventory.get(0));
	}

}
