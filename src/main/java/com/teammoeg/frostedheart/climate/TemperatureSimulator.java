package com.teammoeg.frostedheart.climate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.teammoeg.frostedheart.data.BlockTempData;
import com.teammoeg.frostedheart.data.FHDataManager;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.server.ServerWorld;

/**
 * A simulator built on Alphagem618s' heat conducting model
 * This simulates heat conduction in a small area around player
 * And would take the area out of minecraft logic to optimize calculations.
 * 
 * @author khjxiaogu
 * @author Alphagem618
 */
public class TemperatureSimulator {
	public ChunkSection[] sections = new ChunkSection[8];// sectors(xz): - -/- +/+ -/+ + and y -/+
	public static final int range = 8;// through max range is 8, to avoid some rare issues, set it to 7 to keep count
										// of sections is 8
	BlockPos origin;
	ServerWorld world;
	Random rnd;
	private static final int n = 4168;
	private static final int rdiff = 10;
	private static final float v0 = .4f;
	private static final VoxelShape EMPTY=VoxelShapes.empty();
	private static final VoxelShape FULL=VoxelShapes.fullCube();
	private double[] qx = new double[n], qy = new double[n], qz = new double[n];// Qpos, position of particle.
	private static float[] vx = new float[n], vy = new float[n], vz = new float[n];// Vp, speed vector list, this list
																					// is considered a distributed ball
																					// mesh.
	private int[] vid = new int[n];// IDv, particle speed index in speed vector list, this lower random cost.
	private static final int num_rounds = 20;
	static {// generate speed vector list
		int o = 0;
		for (int i = -rdiff; i <= rdiff; ++i)
			for (int j = -rdiff; j <= rdiff; ++j)
				for (int k = -rdiff; k <= rdiff; ++k) {
					if (i == 0 && j == 0 && k == 0)
						continue; // ignore zero vector
					float x = i * 1f / rdiff, y = j * 1f / rdiff, z = k * 1f / rdiff;
					float r = MathHelper.sqrt(x * x + y * y + z * z);
					if (r > 1)
						continue; // ignore vectors out of the unit ball
					vx[o] = x / r * v0; // normalized x
					vy[o] = y / r * v0; // normalized y
					vz[o] = z / r * v0; // normalized z
					o++;
				}
	}

	public static void init() {
	}

	/**
	 * Extract block data into shape and temperature, other data are disposed.
	 */
	private static class CachedBlockInfo {
		VoxelShape shape;
		float temperature;

		public CachedBlockInfo(VoxelShape shape, float temperature) {
			super();
			this.shape = shape;
			this.temperature = temperature;
		}

		public CachedBlockInfo(VoxelShape shape) {
			super();
			this.shape = shape;
		}
	}

	public Map<BlockState, CachedBlockInfo> info = new HashMap<>();// state to info cache
	public Map<BlockPos, CachedBlockInfo> posinfo = new HashMap<>();// position to info cache

	public TemperatureSimulator(ServerPlayerEntity player) {
		int sourceX = (int) player.getPosX(), sourceY = (int) player.getPosY(), sourceZ = (int) player.getPosZ();
		// System.out.println(sourceX+","+sourceY+","+sourceZ);
		// these are block position offset
		int offsetN = sourceZ - range;
		int offsetW = sourceX - range;
		int offsetD = sourceY - range;

		// these are chunk position offset
		int chunkOffsetW = offsetW >> 4;
		int chunkOffsetN = offsetN >> 4;
		int chunkOffsetD = offsetD >> 4;
		// get origin point(center of 8 sections)
		origin = new BlockPos((chunkOffsetW+1) << 4, (chunkOffsetD+1) << 4, (chunkOffsetN+1) << 4);
		// fetch all sections to lower calculation cost
		int i = 0;
		world = player.getServerWorld();
		for (int x = chunkOffsetW; x <= chunkOffsetW+1; x++)
			for (int z = chunkOffsetN; z <= chunkOffsetN+1; z++) {
				ChunkSection[] css = world.getChunk(x, z).getSections();
				for (ChunkSection cs : css) {
					if (cs == null)
						continue;
					int ynum = cs.getYLocation() >> 4;
					if (ynum == chunkOffsetD) {
						sections[i] = cs;
					}
					if (ynum == chunkOffsetD+1) {
						sections[i + 1] = cs;
					}
				}
				i += 2;
			}
		rnd = new Random(player.getPosition().toLong());
	}

	/**
	 * This fetch block in a delta location to origin,
	 * x,y,z must be in range [-16,16)
	 * 
	 */
	public BlockState getBlock(int x, int y, int z) {
		int i = 0;

		if (x >= 0) {
			i += 4;
		} else {
			x += 16;
		}
		if (z >= 0) {
			i += 2;
		} else {
			z += 16;
		}
		if (y >= 0) {
			i += 1;
		} else {
			y += 16;
		}
		if (x >= 16 || y >= 16 || z >= 16 || x < 0 || y < 0 || z < 0) {// out of bounds
			return Blocks.AIR.getDefaultState();
		}
		ChunkSection current = sections[i];
		if (current == null)
			return Blocks.AIR.getDefaultState();
		try {
			return current.getBlockState(x, y, z);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get block at" + x + "," + y + "," + z);
		}
	}

	/***
	 * Since a position is highly possible to be fetched for multiple times, add
	 * cache in normal fetch
	 */
	private CachedBlockInfo getInfoCached(BlockPos pos) {
		return posinfo.computeIfAbsent(pos, p -> getInfo(p));
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

		BlockTempData b = FHDataManager.getBlockData(bs.getBlock());
		if (b == null)
			return new CachedBlockInfo(bs.getCollisionShape(world, pos));

		float cblocktemp = 0;
		if (b.isLit()) {
			boolean litOrActive = false;
			if (bs.hasProperty(BlockStateProperties.LIT) && bs.get(BlockStateProperties.LIT))
				litOrActive = true;
			if (litOrActive)
				cblocktemp += b.getTemp();
		} else
			cblocktemp += b.getTemp();
		if (b.isLevel()) {
			if (bs.hasProperty(BlockStateProperties.LEVEL_0_15)) {
				cblocktemp *= (bs.get(BlockStateProperties.LEVEL_0_15) + 1) / 16;
			} else if (bs.hasProperty(BlockStateProperties.LEVEL_0_8)) {
				cblocktemp *= (bs.get(BlockStateProperties.LEVEL_0_8) + 1) / 9;
			} else if (bs.hasProperty(BlockStateProperties.LEVEL_1_8)) {
				cblocktemp *= (bs.get(BlockStateProperties.LEVEL_1_8)) / 8;
			} else if (bs.hasProperty(BlockStateProperties.LEVEL_0_3)) {
				cblocktemp *= (bs.get(BlockStateProperties.LEVEL_0_3) + 1) / 4;
			}
		}
		return new CachedBlockInfo(bs.getCollisionShape(world, pos), cblocktemp);
	}

	/**
	 * Check if this location collides with block.
	 */
	private boolean isBlockade(double x, double y, double z) {
		CachedBlockInfo info = getInfoCached(new BlockPos(x, y, z));
		if (info.shape == FULL)
			return true;
		if (info.shape == EMPTY)
			return false;
		return info.shape.contains(MathHelper.frac(x), MathHelper.frac(y), MathHelper.frac(z));

	}

	/**
	 * Get location temperature
	 */
	private float getHeat(double x, double y, double z) {

		return getInfoCached(new BlockPos(x, y, z)).temperature;
	}

	public float getBlockTemperature(double qx0, double qy0, double qz0) {
		for (int i = 0; i < n; ++i) // initialize position as the player's position and the speed (index)
		{
			qx[i] = qx0;
			qy[i] = qy0;
			qz[i] = qz0;
			vid[i] = i;
		}
		float heat = 0;
		for (int round = 0; round < num_rounds; ++round) // time-to-live for each particle is `num_rounds`
		{
			for (int i = 0; i < n; ++i) // for all particles:
			{
				if (isBlockade(qx[i] + vx[vid[i]], qy[i] + vy[vid[i]], qz[i] + vz[vid[i]])) // if running into a block
				{
					vid[i] = rnd.nextInt(n); // re-choose a speed direction randomly (from the pre-generated pool)
				}
				qx[i] = qx[i] + vx[vid[i]]; // move x
				qy[i] = qy[i] + vy[vid[i]]; // move y
				qz[i] = qz[i] + vz[vid[i]]; // move z
				heat += getHeat(qx[i], qy[i], qz[i])
						* MathHelper.lerp(MathHelper.clamp(vy[vid[i]], 0, 0.4) * 2.5, 1, 0.5); // add heat
			}
		}
		return heat / n;
	}
}
