package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.ScatteredStructurePiece;
import net.minecraft.world.gen.feature.structure.SwampHutPiece;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;

@Mixin(SwampHutPiece.class)
public abstract class MixinSwampHutPiece extends ScatteredStructurePiece {

    public MixinSwampHutPiece(IStructurePieceType structurePieceTypeIn, Random rand, int xIn, int yIn, int zIn,
                              int widthIn, int heightIn, int depthIn) {
        super(structurePieceTypeIn, rand, xIn, yIn, zIn, widthIn, heightIn, depthIn);
    }

    @Override
    protected void setBlockState(ISeedReader worldIn, BlockState blockstateIn, int x, int y, int z,
                                 MutableBoundingBox boundingboxIn) {
        if (blockstateIn != null && blockstateIn.getBlock() instanceof CraftingTableBlock) return;

        super.setBlockState(worldIn, blockstateIn, x, y, z, boundingboxIn);
    }

    protected MixinSwampHutPiece(IStructurePieceType structurePieceTypeIn, CompoundNBT nbt) {
        super(structurePieceTypeIn, nbt);
    }

}
