package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.block.Blocks;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.IglooPieces;
import net.minecraft.world.gen.feature.structure.TemplateStructurePiece;
@Mixin(IglooPieces.Piece.class)
public abstract class MixinIglooPiece extends TemplateStructurePiece {

	public MixinIglooPiece(IStructurePieceType structurePieceTypeIn, int componentTypeIn) {
		super(structurePieceTypeIn, componentTypeIn);
	}

	public MixinIglooPiece(IStructurePieceType structurePieceTypeIn, CompoundNBT nbt) {
		super(structurePieceTypeIn, nbt);
	}
	/**
	 * @author khjxiaogu
	 * @reason fix chest type to fit our structure system
	 */
	@Overwrite
    protected void handleDataMarker(String function, BlockPos pos, IServerWorld worldIn, Random rand, MutableBoundingBox sbb) {
        if ("chest".equals(function)) {
           worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
           TileEntity tileentity = worldIn.getTileEntity(pos.down());
           if (tileentity instanceof LockableLootTileEntity) {
              ((LockableLootTileEntity)tileentity).setLootTable(LootTables.CHESTS_IGLOO_CHEST, rand.nextLong());
           }

        }
     }

}
