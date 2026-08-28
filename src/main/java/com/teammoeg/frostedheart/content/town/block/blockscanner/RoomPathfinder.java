package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.*;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractWorld.BlockType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;

public class RoomPathfinder {

    private static final int MAX_XZ_DISTANCE = 16;
    private static final int MAX_Y_DISTANCE = 32;
    public static class OccupiedCell implements Iterable<MutableBlockPos>{
    	final BlockPos pos;
    	final int height;
    	final int reachableHeight;
		public OccupiedCell(BlockPos pos, int height) {
			this(pos,height,2);
		}
		public OccupiedCell(BlockPos pos, int height, int reachableHeight) {

			this.pos = pos;
			this.height = height;
			this.reachableHeight=reachableHeight;
		}
		public boolean isOverlap(OccupiedCell another) {
			if(another.pos.getX()!=this.pos.getX()||another.pos.getZ()!=this.pos.getZ())
				return false;
		    int aEnd = another.pos.getY() + another.height - 1; // 段 a 的最高 y 坐标
		    int bEnd = this.pos.getY() + this.height - 1; // 段 b 的最高 y 坐标

		    // 两个区间 [a.y, aEnd] 和 [b.y, bEnd] 有交集
		    return Math.max(another.pos.getY(),this.pos.getY()) <= Math.min(aEnd, bEnd);
			
		}
		public BlockPos getPos() {
			return pos;
		}
		public int height() {
			return height;
		}
		@Override
		public int hashCode() {
			return Objects.hash(pos, height);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			OccupiedCell other = (OccupiedCell) obj;
			return Objects.equals(pos, other.pos) && height == other.height;
		}
		@Override
		public Iterator<MutableBlockPos> iterator() {
			return new Iterator<>() {
				int cy=0;
				MutableBlockPos mbp=new MutableBlockPos();
				@Override
				public boolean hasNext() {
					return cy<height;
				}

				@Override
				public MutableBlockPos next() {
					mbp.setWithOffset(pos, 0, cy, 0);
					cy++;
					return mbp;
				}
				
			};
		}
		private Iterable<MutableBlockPos> reachableIterator;
		public Iterable<MutableBlockPos> reachableIterable() {
			if(reachableIterator==null) {
				reachableIterator=()->new Iterator<>() {
					int cy=0;
					MutableBlockPos mbp=new MutableBlockPos();
					@Override
					public boolean hasNext() {
						return cy<reachableHeight;
					}

					@Override
					public MutableBlockPos next() {
						mbp.setWithOffset(pos, 0, cy, 0);
						cy++;
						return mbp;
					}
					
				};
			}
			return reachableIterator;
		}
		@Override
		public String toString() {
			return "OccupiedCell [pos=" + pos + ", height=" + height + "]";
		}
    }
    /**
     * 搜索结果统计（静态内部类）
     */
    public static class ReachabilityResult {
        public final Set<OccupiedCell> occupiedCells;  // 占据格子（从地板上方到天花板下方）
        public final Set<BlockPos> neighborCells;  // 邻居格子（占据格子向外一格，不包含占据格子）

        public ReachabilityResult(Set<OccupiedCell> occupiedCells,
                                  Set<BlockPos> neighborCells) {
            this.occupiedCells = occupiedCells;
            this.neighborCells = neighborCells;
        }

		@Override
		public String toString() {
			return "ReachabilityResult [occupiedCells=" + occupiedCells + ", neighborCells=" + neighborCells + "]";
		}
    }

    @Nullable
    public static ReachabilityResult findReachable(AbstractWorld world, BlockPos start) {
        Set<BlockPos> reachable = new HashSet<>();
        Set<BlockPos> occupiedCells = new HashSet<>();

        Set<OccupiedCell> occupiedSegCells = new HashSet<>();
        // DFS 栈
        Deque<BlockPos> stack = new ArrayDeque<>();

        if (!canStandAt(world, start)) {
            return null;
        }

        reachable.add(start);
        stack.push(start);

        // 计算起点的占据格子
        addOccupiedCells(world, start, start, occupiedCells, occupiedSegCells);

        while (!stack.isEmpty()) {
            BlockPos current = stack.pop();
            List<BlockPos> neighbors = getNeighbors(world, start, current);

            for (BlockPos next : neighbors) {
            	if(!isWithinRange(start,next))
            		return null;
                if (!reachable.contains(next)) {
                    reachable.add(next);
                    stack.push(next);
                    addOccupiedCells(world, start, next, occupiedCells,occupiedSegCells);
                }
            }
        }

        // 基于占据格子计算邻居格子
        Set<BlockPos> neighborCells = computeNeighborCells(occupiedCells, start);

        return new ReachabilityResult(occupiedSegCells, neighborCells);
    }

    // ---------- 以下为辅助方法 ----------

    /**
     * 判断角色脚底在 foot 处时是否合法。
     */
    public static boolean canStandAt(AbstractWorld world, BlockPos foot) {
        BlockType footType = world.getBlockType(foot);
        BlockType headType = world.getBlockType(foot.above());

        if (footType == BlockType.WALL || headType == BlockType.WALL) {
            return false;
        }

        if (footType == BlockType.LADDER || headType == BlockType.LADDER) {
            return true;
        }

        BlockType belowType = world.getBlockType(foot.below());
        return belowType != BlockType.AIR ;
    }

    public static boolean isOnStair(AbstractWorld world, BlockPos foot) {
        BlockType footType = world.getBlockType(foot);
        BlockType headType = world.getBlockType(foot.above());
        return footType == BlockType.LADDER || headType == BlockType.LADDER;
    }

    /**
     * 生成某个可达脚底位置的相邻可达脚底位置
     */
    private static List<BlockPos> getNeighbors(AbstractWorld world, BlockPos start, BlockPos foot) {
        LinkedHashSet<BlockPos> result = new LinkedHashSet<>();
        BlockPos.MutableBlockPos mbp=new MutableBlockPos();
        // 水平移动（含高度差 -1, 0, +1）
        for (Direction dir : Direction.Plane.HORIZONTAL) {
        	mbp.setWithOffset(foot,dir);
        	int y=mbp.getY();
            for (int dy = -1; dy <= 1; dy++) {
            	mbp.setY(y+dy);
                if (canStandAt(world, mbp)) {
                    result.add(mbp.immutable());
                    break;
                }
            }
        }
        mbp.set(foot);
        int y=mbp.getY();
        // 楼梯垂直移动
        if (isOnStair(world, foot)) {
        	mbp.setY(y+1);
            if (canStandAt(world, mbp)) {
                result.add(mbp.immutable());
            }

            mbp.setY(y-1);
            if (canStandAt(world, mbp)) {
                result.add(mbp.immutable());
            }
        }

        return new ArrayList<>(result);
    }

    private static boolean isWithinRange(BlockPos start, BlockPos pos) {
        return Math.abs(pos.getX() - start.getX()) <= MAX_XZ_DISTANCE &&
               Math.abs(pos.getZ() - start.getZ()) <= MAX_XZ_DISTANCE &&
               Math.abs(pos.getY() - start.getY()) <= MAX_Y_DISTANCE;
    }

    /**
     * 为某个可达脚底位置计算并添加其“占据格子”
     * 占据格子范围：从 foot.y+1 开始，向上直到第一个实体方块（WALL/STAIR）下方的格子；
     * 若没有实体方块，则直到 start.y + MAX_Y_DISTANCE（包含该高度）
     */
    private static void addOccupiedCells(AbstractWorld world, BlockPos start, BlockPos foot, Set<BlockPos> occupied,Set<OccupiedCell> occupiedSegs) {
        int ceilingY = Math.min(start.getY() + MAX_Y_DISTANCE, world.getMaxY(foot)); // 判定区域顶部
        BlockPos.MutableBlockPos mbp=new MutableBlockPos();
        mbp.set(foot);
        boolean isLadder= isOnStair(world,mbp);
        int reachableY=foot.getY()+2;
        boolean isFirst=true;
        for (int y = foot.getY() ; y <= ceilingY; y++) {
            mbp.setY(y);
            BlockType type = world.getBlockType(mbp);
            if(type == BlockType.LADDER) {
            	if(isLadder)
            		reachableY=y;
            	else
            		break;
            }else if(isLadder&&!isFirst) {
            	isLadder=false;
            }
            if(isFirst)
            	isFirst=false;
            if (type == BlockType.WALL) {
                // 遇到天花板，停止（不包含该实体方块）
                break;
            }
            occupied.add(mbp.immutable());
        }
        occupiedSegs.add(new OccupiedCell(foot,mbp.getY()-foot.getY(),reachableY-foot.getY()));

    }

    /**
     * 基于占据格子计算邻居格子（6方向相邻，排除占据格子自身，限制在搜索范围内）
     */
    private static Set<BlockPos> computeNeighborCells(Set<BlockPos> occupiedCells, BlockPos start) {
        Set<BlockPos> neighbors = new HashSet<>();

        for (BlockPos cell : occupiedCells) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = cell.relative(dir);
                if (!occupiedCells.contains(neighbor)) {
                    neighbors.add(neighbor);
                }
            }
        }

        return neighbors;
    }
}