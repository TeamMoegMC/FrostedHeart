package com.teammoeg.chorda.util;
import java.util.*;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public class VoxelOuterSurface {

    // 外表面：由一个体素坐标 + 方向唯一确定
    public static record Face(Vec3i voxel, Direction dir){
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Face)) return false;
            Face f = (Face) o;
            return voxel.equals(f.voxel) && dir == f.dir;
        }

        @Override
        public int hashCode() {
            return Objects.hash(voxel, dir);
        }
        public Vec3i[] points() {
        	return faceVertices(dir);
        }
        @Override
        public String toString() {
            return "Face[voxel=" + voxel + ", dir=" + dir + "]";
        }
    }

    // 无向边，两个端点排序后存储，保证去重
    public static class Edge implements Comparable<Edge> {
        final Vec3i a, b;

        Edge(Vec3i a, Vec3i b) {
            if (a.compareTo(b) <= 0) {
                this.a = a;
                this.b = b;
            } else {
                this.a = b;
                this.b = a;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge)) return false;
            Edge e = (Edge) o;
            return a.equals(e.a) && b.equals(e.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }

        @Override
        public int compareTo(Edge o) {
            int c = a.compareTo(o.a);
            if (c != 0) return c;
            return b.compareTo(o.b);
        }

        @Override
        public String toString() {
            return "Edge[" + a + " -> " + b + "]";
        }
    }

    static class EdgeInfo {
        int count = 0;
        Set<Direction> dirs = EnumSet.noneOf(Direction.class);
    }

    public static class Result {
    	public Set<Face> faces = new LinkedHashSet<>();
    	public Set<Edge> outlineEdges = new LinkedHashSet<>();
    }
    static final Vec3i[][] vertices=new Vec3i[][] {
    	new Vec3i[]{
                new Vec3i(0 + 1, 0, 0),
                new Vec3i(0 + 1, 0 + 1, 0),
                new Vec3i(0 + 1, 0 + 1, 0 + 1),
                new Vec3i(0 + 1, 0, 0 + 1)
        },new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, 0 + 1),
                new Vec3i(0, 0 + 1, 0 + 1),
                new Vec3i(0, 0 + 1, 0)
        },new Vec3i[]{
                new Vec3i(0, 0 + 1, 0),
                new Vec3i(0, 0 + 1, 0 + 1),
                new Vec3i(0 + 1, 0 + 1, 0 + 1),
                new Vec3i(0 + 1, 0 + 1, 0)
        },new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(0 + 1, 0, 0),
                new Vec3i(0 + 1, 0, 0 + 1),
                new Vec3i(0, 0, 0 + 1)
        },new Vec3i[]{
                new Vec3i(0, 0, 0 + 1),
                new Vec3i(0 + 1, 0, 0 + 1),
                new Vec3i(0 + 1, 0 + 1, 0 + 1),
                new Vec3i(0, 0 + 1, 0 + 1)
        },new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0 + 1, 0),
                new Vec3i(0 + 1, 0 + 1, 0),
                new Vec3i(0 + 1, 0, 0)
        }
    };
    // 获取某个体素某个方向的面的四个顶点，按逆时针顺序
    static Vec3i[] faceVertices(Direction dir) {

        switch (dir) {
            case EAST:
                return vertices[0];
            case WEST:
                return vertices[1];
            case UP:
                return vertices[2];
            case DOWN:
                return vertices[3];
            case SOUTH:
                return vertices[4];
            case NORTH:
                return vertices[5];
            default:
                throw new IllegalArgumentException("Unknown dir: " + dir);
        }
    }

    public static Result compute(Set<? extends Vec3i> solid) {
        Result result = new Result();
        Map<Edge, EdgeInfo> edgeInfoMap = new HashMap<>();

        for (Vec3i p : solid) {
            for (Direction dir : Direction.values()) {
                Vec3i neighbor = p.relative(dir);

                // 相邻位置也是实心，则该面不是外表面
                if (solid.contains(neighbor)) {
                    continue;
                }

                Face face = new Face(p, dir);
                result.faces.add(face);

                // 取该外表面的四条边
                Vec3i[] vs = faceVertices(dir);
                for (int i = 0; i < 4; i++) {
                    Edge edge = new Edge(vs[i].offset(p), vs[(i + 1) % 4].offset(p));

                    EdgeInfo info = edgeInfoMap.get(edge);
                    if (info == null) {
                        info = new EdgeInfo();
                        edgeInfoMap.put(edge, info);
                    }

                    info.count++;
                    info.dirs.add(dir);
                }
            }
        }

        // 外轮廓棱边：
        // 1. 只被一个外表面使用的边；
        // 2. 被不同法向外表面共享的边。
        // 如果一条边只被同法向的面共享，说明它是共面内部边，不算外轮廓棱边。
        for (Map.Entry<Edge, EdgeInfo> entry : edgeInfoMap.entrySet()) {
            EdgeInfo info = entry.getValue();
            if (info.count == 1 || info.dirs.size() > 1) {
                result.outlineEdges.add(entry.getKey());
            }
        }

        return result;
    }
}