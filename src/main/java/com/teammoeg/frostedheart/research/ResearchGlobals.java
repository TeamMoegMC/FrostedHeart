package com.teammoeg.frostedheart.research;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import net.minecraft.block.Block;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class ResearchGlobals {
	public static class RecipeUnlockList extends UnlockList<IRecipe<?>>{

		public RecipeUnlockList() {
			super();
		}

		public RecipeUnlockList(ListNBT nbt) {
			super(nbt);
		}

		@Override
		public String getString(IRecipe<?> item) {
			return item.getId().toString();
		}

		@Override
		public IRecipe<?> getObject(String s) {
			return ResearchDataManager.server.getRecipeManager().getRecipe(new ResourceLocation(s)).orElse(null);
		}
		
	}
	public static class MultiblockUnlockList extends UnlockList<IMultiblock>{

		public MultiblockUnlockList() {
			super();
		}

		public MultiblockUnlockList(ListNBT nbt) {
			super(nbt);
		}

		@Override
		public String getString(IMultiblock item) {
			return item.getUniqueName().toString();
		}

		@Override
		public IMultiblock getObject(String s) {
			return MultiblockHandler.getByUniqueName(new ResourceLocation(s));
		}


		
	}
	public static class BlockUnlockList extends UnlockList<Block>{

		public BlockUnlockList() {
			super();
		}

		public BlockUnlockList(ListNBT nbt) {
			super(nbt);
		}

		@Override
		public String getString(Block item) {
			return item.getRegistryName().toString();
		}

		@Override
		public Block getObject(String s) {
			return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(s));
		}

		
	}
	public static RecipeUnlockList recipe=new RecipeUnlockList();
	public static MultiblockUnlockList multiblock=new MultiblockUnlockList();
	public static BlockUnlockList block=new BlockUnlockList();
	private ResearchGlobals() {
	}

}
