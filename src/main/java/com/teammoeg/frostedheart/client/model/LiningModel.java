/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.client.model;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

public class LiningModel implements IBakedModel {

    private IBakedModel baseArmorModel;
    private LiningItemOverrideList overrideList;

    public LiningModel(IBakedModel i_baseChessboardModel) {
        baseArmorModel = i_baseChessboardModel;
        overrideList = new LiningItemOverrideList();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseArmorModel.getItemCameraTransforms();
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

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseArmorModel.getParticleTexture();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
        return baseArmorModel.getQuads(state, side, rand);
    }

    // This is a forge extension that is expected for blocks only.
    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
        throw new AssertionError("LiningModel::getQuads(IModelData) should never be called");
    }

    @Override
    public boolean isAmbientOcclusion() {
        return baseArmorModel.isAmbientOcclusion();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return baseArmorModel.isBuiltInRenderer();
    }

    @Override
    public boolean isGui3d() {
        return baseArmorModel.isGui3d();
    }

    @Override
    public boolean isSideLit() {
        return baseArmorModel.isSideLit();
    }
}
