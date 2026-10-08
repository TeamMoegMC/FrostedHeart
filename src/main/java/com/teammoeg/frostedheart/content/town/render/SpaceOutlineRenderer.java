package com.teammoeg.frostedheart.content.town.render;

import java.util.concurrent.atomic.AtomicInteger;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.TesselateHelper.Shape3DTesslator;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class SpaceOutlineRenderer<T extends BlockEntity&TownBlockEntity<? extends ITownSpaceOccupiedBuilding>> implements BlockEntityRenderer<T> {
	
    public SpaceOutlineRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
    }
    int currentOverlappedIndex=-1;
    private static final int BLINKS_INTERVAL=4;
    static AtomicInteger globalOverlappedIndex=new AtomicInteger();
    private int allocOverlappedIndex() {
    	if(currentOverlappedIndex==-1)
    		currentOverlappedIndex=globalOverlappedIndex.incrementAndGet()%BLINKS_INTERVAL;
    	return currentOverlappedIndex;
    }
    private static void renderDoubleFloorFace(Shape3DTesslator tesselator,Matrix4f matrix,float x,float y,float z,int color) {

		tesselator.vertex(matrix, x  , y, z  , color);
		tesselator.vertex(matrix, x+1, y, z  , color);
		tesselator.vertex(matrix, x+1, y, z+1, color);
		tesselator.vertex(matrix, x  , y, z+1, color);
		//top face
		tesselator.vertex(matrix, x  , y, z  , color);
		tesselator.vertex(matrix, x  , y, z+1, color);
		tesselator.vertex(matrix, x+1, y, z+1, color);
		tesselator.vertex(matrix, x+1, y, z  , color);
    }
	@Override
	public void render(T pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
		var data=pBlockEntity.getBuilding();
		if(data.isPresent()) {
			OccupiedZoneInfo space=data.get().getOccupiedVolume();
			if(space.getErrorPos()!=null||space.getOccupiedCells().size()>0) {
				Matrix4f matrix=pPoseStack.last().pose();
				BlockPos bpos=pBlockEntity.getBlockPos();
				try(var tesselator=TesselateHelper.getShape3DTesslator()){
					if(space.getErrorPos()!=null)
						for(BlockPos pos:space.getErrorPos()) {
							tesselator.cube(matrix, new AABB(pos).move(-bpos.getX(), -bpos.getY(), -bpos.getZ()).inflate(0.05f), 0xaaff8888);
						}
					int color=0xaa88ff88;
					if(space.isOverlapped()) {
						int index=allocOverlappedIndex();
						color=0xaaff8888;
						long timeIndex=System.currentTimeMillis()/125;
						if((timeIndex&1)!=0)
							return;
						if((timeIndex/2)%BLINKS_INTERVAL!=index)
							return;
					}
						
					for(OccupiedCell oc:space.getOccupiedCells())
						for(MutableBlockPos pos:oc.pathIterable()) {
							pos.move(-bpos.getX(), -bpos.getY(), -bpos.getZ());
							renderDoubleFloorFace(tesselator,matrix, pos.getX()  , pos.getY()+0.05f, pos.getZ()  , color);
							
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
