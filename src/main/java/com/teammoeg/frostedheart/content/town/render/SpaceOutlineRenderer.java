package com.teammoeg.frostedheart.content.town.render;

import java.util.Objects;
import java.util.Set;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpaceOutlineRenderer<T extends BlockEntity&TownBlockEntity<? extends ITownSpaceOccupiedBuilding>> implements BlockEntityRenderer<T> {
	Set<OccupiedCell> oldSpace;
	//VoxelOuterSurface.Result cache;

    public SpaceOutlineRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
    }
	@Override
	public void render(T pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
		var data=pBlockEntity.getBuilding();
		if(data.isPresent()) {
			Set<OccupiedCell> space=data.get().getOccupiedVolume();
			if(space!=null) {
				if(/*cache==null||*/oldSpace==null||!Objects.equals(space, oldSpace)) {
					oldSpace=Set.copyOf(space);
					/*Set<BlockPos> poss=new HashSet<>();
					for(OccupiedCell oc:space)
						for(BlockPos cpos:oc) {
							poss.add(cpos.subtract(pBlockEntity.getBlockPos()));
						}*/
					//cache=VoxelOuterSurface.compute(poss);
				}
				if(oldSpace!=null) {
					Matrix4f matrix=pPoseStack.last().pose();
					BlockPos bpos=pBlockEntity.getBlockPos();
					try(var tesselator=TesselateHelper.getShape3DTesslator()){
						for(OccupiedCell oc:space)
							for(MutableBlockPos pos:oc.pathIterable()) {
								pos.move(-bpos.getX(), -bpos.getY(), -bpos.getZ());
								//bottom face
								tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()  , 0xaa88ff88);
								tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()  , 0xaa88ff88);
								tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()+1, 0xaa88ff88);
								tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()+1, 0xaa88ff88);
								//top face
								tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()  , 0xaa88ff88);
								tesselator.vertex(matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()+1, 0xaa88ff88);
								tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()+1, 0xaa88ff88);
								tesselator.vertex(matrix, pos.getX()+1, pos.getY()+0.05f, pos.getZ()  , 0xaa88ff88);
								
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

}
