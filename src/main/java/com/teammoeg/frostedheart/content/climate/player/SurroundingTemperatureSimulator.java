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

package com.teammoeg.frostedheart.content.climate.player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.FHDataManager;
import com.teammoeg.frostedheart.content.climate.data.BlockTempData;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerWorld;

/**
 * A simulator built on Alphagem618's heat conducting model
 * This simulates heat conduction in a small area around player
 * And would take the area out of minecraft logic to optimize calculations.
 *
 * @author khjxiaogu
 * @author Alphagem618
 */
public class SurroundingTemperatureSimulator {
    /**
     * Extract block data into shape and temperature, other data are disposed.
     */
    private static class CachedBlockInfo {
        VoxelShape shape;
        
        float temperature;
        boolean exposeToAir;
        BlockState bs;
        public CachedBlockInfo(VoxelShape shape, float temperature, boolean exposeToAir, BlockState bs) {
			super();
			this.shape = shape;
			this.temperature = temperature;
			this.exposeToAir = exposeToAir;
			this.bs = bs;
		}

		public CachedBlockInfo(VoxelShape shape, boolean exposeToAir, BlockState bs) {
			super();
			this.shape = shape;
			this.exposeToAir = exposeToAir;
			this.bs = bs;
		}
    }
    public static final int range = 8;// through max range is 8, to avoid some rare issues, set it to 7 to keep count
    private static final int n = 4168;//number of particles
    private static final int rdiff = 10;//division of the unit square, changing this value would have no effect but improve precision
    private static final float v0 = .4f;//initial particle speed
    private static final VoxelShape EMPTY = VoxelShapes.empty();
    private static final VoxelShape FULL = VoxelShapes.fullCube();
    private static Vector3d[] speedVectors;// Vp, speed vector list, this list is constant and considered a distributed ball mesh.
    private static final int num_rounds = 20;//propagate time-to-live for each particles
    private static int[][] speedVectorByDirection=new int[6][];// index: ordinal value of outbounding facing
    static {// generate speed vector list
    	Map<Direction,List<Integer>> lis=new EnumMap<>(Direction.class);
    	List<Vector3d> v3fs=new ArrayList<>();
    	for(Direction dr:Direction.values()) {
    		lis.put(dr, new ArrayList<>());
    	}
    	int o=0;
        for (int i = -rdiff; i <= rdiff; ++i)
            for (int j = -rdiff; j <= rdiff; ++j)
                for (int k = -rdiff; k <= rdiff; ++k) {
                    if (i == 0 && j == 0 && k == 0)
                        continue; // ignore zero vector
                    float x = i * 1f / rdiff, y = j * 1f / rdiff, z = k * 1f / rdiff;
                    float r = MathHelper.sqrt(x * x + y * y + z * z);
                    if (r > 1)
                        continue; // ignore vectors out of the unit ball
                    Vector3d v3 = new Vector3d(x / r * v0, y / r * v0, z / r * v0);
                    v3fs.add(v3);
                    if(v3.x>+0) 
                    	lis.get(Direction.EAST).add(o);
                    if(v3.x<-0) 
                    	lis.get(Direction.WEST).add(o);
                    if(v3.y>+0) 
                    	lis.get(Direction.UP).add(o);
                    if(v3.y<-0) 
                    	lis.get(Direction.DOWN).add(o);
                    if(v3.z>+0) 
                    	lis.get(Direction.SOUTH).add(o);
                    if(v3.z<-0) 
                    	lis.get(Direction.NORTH).add(o);
                    o++;
                }
        speedVectors=v3fs.toArray(new Vector3d[o]);
        for(Direction dr:Direction.values()) {
        	speedVectorByDirection[dr.ordinal()]=lis.get(dr).stream().mapToInt(t->t).toArray();
        }
    }
    public ChunkSection[] sections = new ChunkSection[8];// index: bitset of xzy(1 stands for +)
    public Heightmap[] maps=new Heightmap[4]; // index: bitset of xz(1 stands for +)
    BlockPos origin;
    ServerWorld world;
    Random rnd;
    //RandomSequence rrnd;
    private Vector3d[] Qpos = new Vector3d[n];// Qpos, position of particle.
    private int[] vid = new int[n];// IDv, particle speed index in speed vector list, this lower random cost.
    //private double[] factor=
    

    public Map<BlockState, CachedBlockInfo> info = new HashMap<>();// state to info cache

    public Map<BlockPos, CachedBlockInfo> posinfo = new HashMap<>();// position to info cache
    public static void init() {
    	
    }

    public SurroundingTemperatureSimulator(ServerPlayerEntity player) {
        int sourceX = MathHelper.floor(player.getPosX()), sourceY = MathHelper.floor(player.getPosYEye()), sourceZ = MathHelper.floor( player.getPosZ());
        // these are block position offset
        int offsetN = sourceZ - range;
        int offsetW = sourceX - range;
        int offsetD = sourceY - range;
        // these are chunk position offset
        int chunkOffsetW = offsetW >> 4;
        int chunkOffsetN = offsetN >> 4;
        int chunkOffsetD = offsetD >> 4;
        // get origin point(center of 8 sections)
        origin = new BlockPos((chunkOffsetW + 1) << 4, (chunkOffsetD + 1) << 4, (chunkOffsetN + 1) << 4);
        // fetch all sections to lower calculation cost
        int i = 0;
        world = player.getServerWorld();
        for (int x = chunkOffsetW; x <= chunkOffsetW + 1; x++)
            for (int z = chunkOffsetN; z <= chunkOffsetN + 1; z++) {
            	Chunk cnk=world.getChunk(x, z);
                ChunkSection[] css = cnk.getSections();
                maps[i/2]=cnk.getHeightmap(Type.MOTION_BLOCKING_NO_LEAVES);
                if(css.length>chunkOffsetD&&chunkOffsetD>0)
                	sections[i]=css[chunkOffsetD];
                if(css.length>chunkOffsetD+1&&chunkOffsetD+1>0)
                	sections[i + 1]=css[chunkOffsetD + 1];
                i += 2;
            }
        rnd = new Random(player.getPosition().toLong()^(world.getGameTime()>>6));
    }

    /**
     * This fetch block in a delta location to origin,
     * x,y,z must be in range [-16,16)
     */
    public BlockState getBlock(int x, int y, int z) {
        if (x >= 16 || y >= 16 || z >= 16 || x < -16 || y < -16 || z < -16) // out of bounds
            return Blocks.AIR.getDefaultState();
        int i = 0;
        if (x >= 0)
            i += 4;
        if (z >= 0)
            i += 2;
        if (y >= 0)
            i += 1;
        ChunkSection current = sections[i];
        if (current == null)
            return Blocks.AIR.getDefaultState();
        try {
            return current.getBlockState(x&15, y&15, z&15);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get block at" + x + "," + y + "," + z);
        }
    }
    public int getTopY(int x, int z) {
        if (x >= 16 || z >= 16 || x < -16|| z < -16) // out of bounds
            return 0;
        int i = 0;
        if (x >= 0) 
            i += 2;
        if (z >= 0) 
            i += 1;
        return maps[i].getHeight(x&15, z&15);
    }
    public int getOutboundSpeedFrom(Direction dir) {
    	if(dir==null)
    		return rnd.nextInt(speedVectors.length);
    	int[] iis=speedVectorByDirection[dir.ordinal()];
    	return iis[rnd.nextInt(iis.length)];
    }
    public Pair<Float,Float> getBlockTemperatureAndWind(double qx0, double qy0, double qz0) {
    	float wind=0;
    	Vector3d q0=new Vector3d(qx0, qy0, qz0);
        for (int i = 0; i < n; ++i) // initialize position as the player's position and the speed (index)
        {
            Qpos[i] = q0;
            vid[i] = i ;
        }
        /*System.out.println("=========start=========");
        for(int i=-1;i<=1;i++) {
        	StringBuilder sb=new StringBuilder();
        	 for(int j=-1;j<=1;j++)
        		 sb.append(getInfo(new BlockPos(qx0+i,qy0-2,qz0+j)).bs.getBlock().getRegistryName()).append(" , ");
        	 System.out.println(sb.toString());
        }*/
        float heat = 0;
        for (int round = 0; round < num_rounds; ++round) // time-to-live for each particle is `num_rounds`
        {
            for (int i = 0; i < n; ++i) // for all particles:
            {
            	int nid=vid[i];
            	Vector3d curspeed=speedVectors[vid[i]];
            	Vector3d svec=Qpos[i];
            	Vector3d dvec=svec.add(curspeed);
            	BlockPos bpos=new BlockPos(dvec);
                CachedBlockInfo info = getInfoCached(bpos);
                if (info.shape != EMPTY &&(info.shape == FULL||info.shape.contains(MathHelper.frac(dvec.x),MathHelper.frac(dvec.y),MathHelper.frac(dvec.z)))) {
                	BlockRayTraceResult brtr=AxisAlignedBB.rayTrace(info.shape.toBoundingBoxList(), svec, dvec, bpos);
                    if(brtr!=null) {
                    	if(rnd.nextDouble()<0.33f) {
                    		nid = rnd.nextInt(speedVectors.length);
                    	} else {
                    		nid = getOutboundSpeedFrom(brtr.getFace());
                    	}
                    }else {
                    	nid = rnd.nextInt(speedVectors.length);
                    }
                }
                Qpos[i] = dvec;
                vid[i] = nid;
                heat += (float) (getHeat(bpos) * MathHelper.lerp(MathHelper.clamp(-curspeed.getY(), 0, 0.4) * 2.5, 1, 0.5)); // add heat
                wind += getAir(bpos)? (float) MathHelper.lerp((MathHelper.clamp(Math.abs(curspeed.getY()), 0.2, 0.8) - 0.2) / 0.6, 2, 0.5) :0;
            }
        }
        return Pair.of(heat / n, wind/n);
    }

    /**
     * Get location temperature
     */
    private float getHeat(BlockPos bp) {
        return getInfoCached(bp).temperature;
    }
    private boolean getAir(BlockPos bp) {
        return getInfoCached(bp).exposeToAir;
    }

    /***
     * fetch without position cache, but with blockstate cache, blocks with the same
     * state should have same collider and heat.
     *
     */
    private CachedBlockInfo getInfo(BlockPos pos) {
        BlockPos ofregion = pos.subtract(origin);
        BlockState bs = getBlock(ofregion.getX(), ofregion.getY(), ofregion.getZ());
        return info.computeIfAbsent(bs, s -> getInfo(pos, s));
    }

    /**
     * Just fetch block temperature and collision without cache.
     * Position is only for getCollisionShape method, to avoid some TE based shape.
     */
    private CachedBlockInfo getInfo(BlockPos pos, BlockState bs) {
    	boolean isExpose=getTopY(pos.getX(),pos.getZ())<pos.getY();
        BlockTempData b = FHDataManager.getBlockData(bs.getBlock());
        if (b == null)
            return new CachedBlockInfo(bs.getCollisionShape(world, pos),isExpose,bs);
        float cblocktemp = 0;
        if (b.isLit()) {
            boolean litOrActive = bs.hasProperty(BlockStateProperties.LIT) && bs.get(BlockStateProperties.LIT);
            if (litOrActive)
                cblocktemp += b.getTemp();
        } else
            cblocktemp += b.getTemp();
        if (b.isLevel()) {
            if (bs.hasProperty(BlockStateProperties.LEVEL_0_15)) {
                cblocktemp *= (float) (bs.get(BlockStateProperties.LEVEL_0_15) + 1) / 16;
            } else if (bs.hasProperty(BlockStateProperties.LEVEL_0_8)) {
                cblocktemp *= (float) (bs.get(BlockStateProperties.LEVEL_0_8) + 1) / 9;
            } else if (bs.hasProperty(BlockStateProperties.LEVEL_1_8)) {
                cblocktemp *= (float) (bs.get(BlockStateProperties.LEVEL_1_8)) / 8;
            } else if (bs.hasProperty(BlockStateProperties.LEVEL_0_3)) {
                cblocktemp *= (float) (bs.get(BlockStateProperties.LEVEL_0_3) + 1) / 4;
            }
        }
        return new CachedBlockInfo(bs.getCollisionShape(world, pos), cblocktemp,isExpose,bs);
    }

    /***
     * Since a position is highly possible to be fetched for multiple times, add
     * cache in normal fetch
     */
    private CachedBlockInfo getInfoCached(BlockPos pos) {
        return posinfo.computeIfAbsent(pos, this::getInfo);
    }

    /**
     * Check if this location collides with block.
     */
    private Direction getHitingFace(double sx, double sy, double sz,double vx,double vy,double vz) {
    	BlockPos bpos=new BlockPos(sx+vx, sy+vy, sz+vz);
        CachedBlockInfo info = getInfoCached(bpos);
        if (info.shape == EMPTY)
            return null;
        Vector3d svec=new Vector3d(sx, sy, sz);
        Vector3d vvec=new Vector3d(sx+vx, sy+vy, sz+vz);
        BlockRayTraceResult brtr=AxisAlignedBB.rayTrace(info.shape.toBoundingBoxList(), svec, vvec, bpos);
        if(brtr!=null)
        	return brtr.getFace();
        return null;
    }
    private boolean isBlockade(double x, double y, double z) {
        CachedBlockInfo info = getInfoCached(new BlockPos(x, y, z));
        if (info.shape == FULL)
            return true;
        if (info.shape == EMPTY)
            return false;
        double nx=MathHelper.frac(x),ny=MathHelper.frac(y),nz=MathHelper.frac(z);
        return info.shape.contains(nx,ny,nz);
    }
}
