package com.teammoeg.frostedheart.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;

public class StructureUtils {
	static List<Block> baned=new ArrayList<>();
	static Map<Block,Block> remap=new HashMap<>();
	public static void addBanedBlocks() {
		baned.add(Blocks.CRAFTING_TABLE);
		baned.add(Blocks.FURNACE);
		baned.add(Blocks.SMOKER);
		baned.add(Blocks.BLAST_FURNACE);
		
	}
	public static void handlePalette(List<Template.Palette> p) {
		p.forEach(q->q.func_237157_a_().replaceAll(r->{
			if(baned.contains(r.state.getBlock()))
				return new BlockInfo(r.pos,Blocks.AIR.getDefaultState(),null);
			else if(remap.containsKey(r.state.getBlock()))
				return new BlockInfo(r.pos,remap.get(r.state.getBlock()).getDefaultState(),r.nbt);
			return r;
		}));
	}
}
