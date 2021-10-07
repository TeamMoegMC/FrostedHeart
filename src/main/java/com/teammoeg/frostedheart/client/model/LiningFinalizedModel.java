package com.teammoeg.frostedheart.client.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LiningFinalizedModel implements IBakedModel {

    private int numberOfChessPieces;
    private IBakedModel parentModel;

    public LiningFinalizedModel(IBakedModel i_parentModel, int i_numberOfChessPieces)
    {
        parentModel = i_parentModel;
        numberOfChessPieces = i_numberOfChessPieces;
    }

    /**
     * We return a list of quads here which is used to draw the chessboard.
     * We do this by getting the list of quads for the base model (the chessboard itself), then adding an extra quad for
     *   every piece on the chessboard.  The number of pieces was provided to the constructor of the finalised model.
     *
     * @param state
     * @param side  which side: north, east, south, west, up, down, or null.  NULL is a different kind to the others
     *   see here for more information: http://minecraft.gamepedia.com/Block_models#Item_models
     * @param rand
     * @return the list of quads to be rendered
     */

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
        // our chess pieces are only drawn when side is NULL.
        if (side != null) {
            return parentModel.getQuads(state, side, rand);
        }

        List<BakedQuad> combinedQuadsList = new ArrayList(parentModel.getQuads(state, side, rand));
        //TODO
//        combinedQuadsList.addAll(getChessPiecesQuads(numberOfChessPieces));
        return combinedQuadsList;
//    FaceBakery.makeBakedQuad() can also be useful for generating quads.  See mbe04: AltimeterBakedModel
    }

    //TODO
//    private List<BakedQuad> getChessPiecesQuads(int numberOfPieces) {
//
//    }

    /**
     * Converts the vertex information to the int array format expected by BakedQuads.  Useful if you don't know
     *   in advance what it should be.
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @param color RGBA colour format - white for no effect, non-white to tint the face with the specified colour
     * @param texture the texture to use for the face
     * @param u u-coordinate of the texture (0 - 16) corresponding to [x,y,z]
     * @param v v-coordinate of the texture (0 - 16) corresponding to [x,y,z]
     * @param lightmapvalue the blocklight+skylight packed light map value (generally: set this to maximum for items)
     *                      http://greyminecraftcoder.blogspot.com/2020/04/lighting-1144.html
     * @param normal the packed representation of the normal vector, see calculatePackedNormal().  Used for lighting item.
     * @return
     */
    private int[] vertexToInts(float x, float y, float z, int color, TextureAtlasSprite texture, float u, float v, int lightmapvalue, int normal)
    {
        // based on FaceBakery::storeVertexData and FaceBakery::fillVertexData

        final int DUMMY_LIGHTMAP_VALUE = 0xffff;

        return new int[] {
                Float.floatToRawIntBits(x),
                Float.floatToRawIntBits(y),
                Float.floatToRawIntBits(z),
                color,
                Float.floatToRawIntBits(texture.getInterpolatedU(u)),
                Float.floatToRawIntBits(texture.getInterpolatedV(v)),
                lightmapvalue,
                normal
        };
    }

    @Override
    public boolean isAmbientOcclusion() {
        return parentModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return parentModel.isGui3d();
    }

    @Override
    public boolean isSideLit() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return parentModel.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return parentModel.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return parentModel.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        throw new UnsupportedOperationException("The finalised model does not have an override list.");
    }
}
