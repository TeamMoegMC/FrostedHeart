#version 430 core

in vec3 v_World;
in vec3 v_Normal;
flat in ivec3 v_Box;
flat in int   v_IsTop;

out vec4 fragColor;

// 斜条纹: t 为投影到条纹法向的坐标, freq 为频率
float stripe(float t, float freq) {
    return fract(t * freq);
}

void main() {
    float by = float(v_Box.x);
    float h1 = float(v_Box.y);
    float h2 = float(v_Box.z);

    vec4 color;

    // ==========================================================
    // 1. (x,y,z) 那一格的顶面 : 红蓝斜条纹
    // ==========================================================
    if (v_IsTop == 1) {
        float s = stripe(v_World.x + v_World.z, 0.5);
        vec3 red  = vec3(0.90, 0.15, 0.15);
        vec3 blue = vec3(0.15, 0.30, 0.95);
        color = vec4((s < 0.5) ? red : blue, 1.0);
    }
    // ==========================================================
    // 2. y ~ h1 : 淡色斜条纹
    // ==========================================================
    else if (v_World.y < h1) {
        // 用 x+y+z 作为投影方向, 形成 45° 斜条纹
        float s = stripe(v_World.x + v_World.y + v_World.z, 0.5);
        vec3 c1 = vec3(0.98, 0.90, 0.90);   // 淡红
        vec3 c2 = vec3(0.90, 0.92, 0.98);   // 淡蓝
        color = vec4((s < 0.5) ? c1 : c2, 0.65);
    }
    // ==========================================================
    // 3. h1 ~ h2 : 淡黄绿色格子
    // ==========================================================
    else {
        vec2 uv = floor(v_World.xz);
        float c = mod(uv.x + uv.y, 2.0);
        vec3 g1 = vec3(0.82, 0.96, 0.58);
        vec3 g2 = vec3(0.68, 0.88, 0.44);
        color = vec4((c < 0.5) ? g1 : g2, 0.65);
    }

    fragColor = color;
}