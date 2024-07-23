package com.teammoeg.frostedheart.content.steamenergy;

import net.minecraft.world.level.block.Block;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;

public class EndPointData {
	Block blk;
	BlockPos pos;
	public EndPointData(Block blk,BlockPos pos) {
		super();
		this.blk = blk;
		this.pos = pos;
	}
	float intake=-1;
	float output=-1;
	float avgIntake=-1;
	float avgOutput=-1;
	float maxIntake=-1;
	boolean canCostMore;
	public void pushData() {
		if(avgIntake<0)
			avgIntake=intake;
		else
			avgIntake=avgIntake*.95f+Math.max(0, intake)*.05f;
		if(avgOutput<0)
			avgOutput=output;
		else
			avgOutput=avgOutput*.95f+Math.max(0, output)*.05f;
		canCostMore=Math.round(avgOutput*10)/10f<maxIntake;
		intake=-1;
		output=-1;
		maxIntake=-1;
	}
	public void writeNetwork(FriendlyByteBuf pb) {
		pb.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS,blk);
		pb.writeBlockPos(pos);
		pb.writeFloat(avgIntake);
		pb.writeFloat(avgOutput);
		pb.writeBoolean(canCostMore);
	}
	public static EndPointData readNetwork(FriendlyByteBuf pb) {
		EndPointData dat=new EndPointData(pb.readRegistryIdUnsafe(ForgeRegistries.BLOCKS),pb.readBlockPos());
		dat.avgIntake=pb.readFloat();
		dat.avgOutput=pb.readFloat();
		dat.canCostMore=pb.readBoolean();
		return dat;
	}
	public void applyOutput(float f, float g) {
		if(output==-1)
			output=f;
		else
			output+=f;
		maxIntake=g;
	}
}
