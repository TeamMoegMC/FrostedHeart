package com.teammoeg.frostedheart.client.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class LiningModel implements IBakedModel {

    private IBakedModel baseArmorModel;
    private LiningItemOverrideList overrideList;

    public LiningModel(IBakedModel i_baseChessboardModel) {
        baseArmorModel = i_baseChessboardModel;
        overrideList = new LiningItemOverrideList();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
        return baseArmorModel.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return baseArmorModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseArmorModel.isGui3d();
    }

    @Override
    public boolean isSideLit() {
        return baseArmorModel.isSideLit();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return baseArmorModel.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseArmorModel.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseArmorModel.getItemCameraTransforms();
    }

    // This is a forge extension that is expected for blocks only.
    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
        throw new AssertionError("LiningModel::getQuads(IModelData) should never be called");
    }

    // This is a forge extension that is expected for blocks only.
    @Override
    @Nonnull
    public IModelData getModelData(@Nonnull IBlockDisplayReader world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData tileData) {
        throw new AssertionError("LiningModel::getModelData should never be called");
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrideList;
    }
}
