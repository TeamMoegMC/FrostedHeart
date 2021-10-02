package com.teammoeg.frostedheart.content.generator;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class UnlitHeatedGeneratorMultiBlock extends HeatedGeneratorMultiBlock {

	public UnlitHeatedGeneratorMultiBlock(String name, RegistryObject type) {
		super(name,  Properties.create(Material.ROCK).hardnessAndResistance(2.0F, 20.0F).notSolid().setLightLevel(s->0),type);
	}

	@Override
	public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		super.animateTick(stateIn, worldIn, pos, rand);
	}

}
