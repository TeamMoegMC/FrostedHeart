package com.teammoeg.frostedheart.client.model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.teammoeg.frostedheart.block.FluidPipeBlock;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelDataMap.Builder;
import net.minecraftforge.client.model.data.ModelProperty;

public class PipeAttachmentModel<T extends FluidPipeBlock<T>> extends BakedModelWrapperWithData {

	private ModelProperty<PipeModelData> PIPE_PROPERTY = new ModelProperty<>();
	FluidPipeBlock<T> block;
	public PipeAttachmentModel(FluidPipeBlock<T> fb,IBakedModel template) {
		super(template);
		block=fb;
	}

	@Override
	protected Builder gatherModelData(Builder builder, IBlockDisplayReader world, BlockPos pos, BlockState state) {
		PipeModelData data = new PipeModelData();

		for (Direction d :Direction.values())
			data.putRim(d,block.shouldDrawRim(world, pos, state,d));

		data.setEncased(block.shouldDrawCasing(world, pos, state));
		return builder.withInitial(PIPE_PROPERTY, data);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData data) {
		List<BakedQuad> quads = super.getQuads(state, side, rand, data);
		if (data instanceof ModelDataMap) {
			ModelDataMap modelDataMap = (ModelDataMap) data;
			if (modelDataMap.hasProperty(PIPE_PROPERTY)) {
				quads = new ArrayList<>(quads);
				addQuads(quads, state, side, rand, modelDataMap, modelDataMap.getData(PIPE_PROPERTY));
			}
		}
		return quads;
	}

	private void addQuads(List<BakedQuad> quads, BlockState state, Direction side, Random rand, IModelData data,
		PipeModelData pipeData) {
		for (Direction d :Direction.values())
			if (pipeData.hasRim(d))
				quads.addAll(PartialModels.PIPE_ATTACHMENTS.get(pipeData.getRim(d))
					.get(d)
					.get()
					.getQuads(state, side, rand, data));
		if (pipeData.isEncased())
			quads.addAll(PartialModels.FLUID_PIPE_CASING.get()
				.getQuads(state, side, rand, data));
	}

	private class PipeModelData {
		boolean[] rims;
		boolean encased;
		IBakedModel bracket;

		public PipeModelData() {
			rims = new boolean[6];
			Arrays.fill(rims,false);
		}

		public void putBracket(BlockState state) {
			this.bracket = Minecraft.getInstance()
				.getBlockRendererDispatcher()
				.getModelForState(state);
		}

		public IBakedModel getBracket() {
			return bracket;
		}

		public void putRim(Direction face,boolean rim) {
			rims[face.getIndex()] = rim;
		}

		public void setEncased(boolean encased) {
			this.encased = encased;
		}

		public boolean hasRim(Direction face) {
			return rims[face.getIndex()];
		}

		public boolean getRim(Direction face) {
			return rims[face.getIndex()];
		}

		public boolean isEncased() {
			return encased;
		}
	}

}