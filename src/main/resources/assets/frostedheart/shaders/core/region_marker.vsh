#version 430 core

// ---- 顶点属性 --------------------------------------------------
// 顶点 0..35  : 单位立方体 (位置 0..1, 法线朝外)
// 顶点 36..41 : "格子顶面" 四边形 (只用 x/z, 范围 0..1, 法线 +Y)
layout(location = 0) in vec3 a_Pos;
layout(location = 1) in vec3 a_Normal;

// ---- 变换矩阵 --------------------------------------------------
uniform mat4 u_MVP;

// ---- 柱体数据 (SSBO) -------------------------------------------
// 布局 (全部小端):
//   字节 0..1  : uint16  柱体数量 n
//   字节 2..   : n 个柱体, 每个 12 字节:
//       +0  int32   x
//       +4  int16   y
//       +6  int32   z
//       +10 int8    h1
//       +11 int8    h2
layout(std430, binding = 0) readonly buffer CylinderData {
    uint u_Data[];
};

out vec3 v_World;
out vec3 v_Normal;
flat out ivec3 v_Box;     // (by, h1, h2)
flat out int   v_IsTop;   // 1 = 格子顶面

// ---------- 小端字节读取 ----------
uint rU8(uint off) {
    return (u_Data[off >> 2] >> ((off & 3u) << 3)) & 0xFFu;
}
uint rU16(uint off) {
    return rU8(off) | (rU8(off + 1u) << 8);
}
uint rU32(uint off) {
    return rU8(off)
         | (rU8(off + 1u) << 8)
         | (rU8(off + 2u) << 16)
         | (rU8(off + 3u) << 24);
}
int rI8 (uint off) { return int(rU8 (off) << 24) >> 24; }
int rI16(uint off) { return int(rU16(off) << 16) >> 16; }
int rI32(uint off) { return int(rU32(off)); }

void main() {
    uint base = 2u + uint(gl_InstanceID) * 12u;

    int bx  = rI32(base +  0u);
    int by  = rI16(base +  4u);
    int bz  = rI32(base +  6u);
    int bh1 = rI8 (base + 10u);
    int bh2 = rI8 (base + 11u);

    float x0 = float(bx),      x1 = float(bx + 1);
    float z0 = float(bz),      z1 = float(bz + 1);
    float y0 = float(by);
    float y1 = float(bh2);

    // 防止 h2 <= y 时塌陷成零体积
    if (y1 <= y0) y1 = y0 + 0.001;

    vec3 world;
    vec3 nrm;
    int  isTop = 0;

    if (gl_VertexID >= 36) {
        // ---------- (x,y,z) 那一格的顶面 ----------
        // 世界高度固定在 y + 1
        world = vec3(
            mix(x0, x1, a_Pos.x),
            float(by + 1) + 0.002,   // 微量抬升，避免 Z-Fighting
            mix(z0, z1, a_Pos.z)
        );
        nrm   = vec3(0.0, 1.0, 0.0);
        isTop = 1;
    } else {
        // ---------- 柱体外壳 ----------
        world = vec3(
            mix(x0, x1, a_Pos.x),
            mix(y0, y1, a_Pos.y),
            mix(z0, z1, a_Pos.z)
        );
        nrm = a_Normal;
    }

    v_World  = world;
    v_Normal = nrm;
    v_Box    = ivec3(by, bh1, bh2);
    v_IsTop  = isTop;

    gl_Position = u_MVP * vec4(world, 1.0);
}
