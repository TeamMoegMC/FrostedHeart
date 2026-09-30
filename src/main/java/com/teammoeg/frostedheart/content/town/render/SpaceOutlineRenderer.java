package com.teammoeg.frostedheart.content.town.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpaceOutlineRenderer<T extends BlockEntity&TownBlockEntity<? extends ITownSpaceOccupiedBuilding>> implements BlockEntityRenderer<T> {
	public static boolean show;
    public SpaceOutlineRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
    }
    public boolean shouldRenderOffScreen(T pBlockEntity) {
        return show;
     }
    static int overLappedIndex;
    
	@Override
	public void render(T pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
		if(!show)return;
		var data=pBlockEntity.getBuilding();
		if(data.isPresent()) {
			OccupiedZoneInfo space=data.get().getOccupiedVolume();
			if(space.isValid()) {
				Matrix4f matrix=pPoseStack.last().pose();
				BlockPos bpos=pBlockEntity.getBlockPos();
				try(var tesselator=TesselateHelper.getShape3DTesslator()){
					int color=space.isOverlapped()?0xaaff8888:0xaa88ff88;
					if(space.isOverlapped())
					for(OccupiedCell oc:space.getOccupiedCells())
						for(MutableBlockPos pos:oc.pathIterable()) {
							pos.move(-bpos.getX(), -bpos.getY(), -bpos.getZ());
							//bottom face
							tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()  , color);
							tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()  , color);
							tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()+1, color);
							tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()+1, color);
							//top face
							tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()  , color);
							tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()+1, color);
							tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()+1, color);
							tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()  , color);
							
						}
					/*for(Face face:cache.faces) {
						if(face.dir()==Direction.DOWN) {
							Vec3i[] v3is=face.points();
							for(int i=3;i>=0;i--) {
								Vec3i vec=v3is[i];
								tesselator.vertex(matrix, vec.getX()+face.voxel().getX(), vec.getY()+face.voxel().getY(), vec.getZ()+face.voxel().getZ(), 0xaa88ff88);
							}
						}else for(Vec3i vec:face.points())
							tesselator.vertex(matrix, vec.getX()+face.voxel().getX(), vec.getY()+face.voxel().getY()+0.05f, vec.getZ()+face.voxel().getZ(), 0xaaff8888);
						
					}*/
					
				}
				
			}
		}
	}

}
