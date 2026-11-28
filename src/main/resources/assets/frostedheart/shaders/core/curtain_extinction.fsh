#version 150

uniform sampler2D Sampler0; // opaque scene depth
uniform sampler2D Sampler1; // snow material
uniform mat4 InverseClip;
uniform vec3 BoxMin;
uniform vec3 BoxMax;
uniform vec4 StormColor; // rgb, full-density visibility in blocks
uniform vec4 SurfaceUV; // camera-relative horizontal projection and material phase
uniform float ShelterDistance;
in vec2 clipXY;
out vec4 fragColor;

// Returns distances in blocks along the eye ray. Parallel rays outside a face miss it.
vec2 interval(vec3 invRay, vec3 lo, vec3 hi, float sceneDistance) {
    vec3 a = lo * invRay;
    vec3 b = hi * invRay;
    vec3 near = min(a, b);
    vec3 far = max(a, b);
    return vec2(max(ShelterDistance, max(near.x, max(near.y, near.z))),
                min(sceneDistance, min(far.x, min(far.y, far.z))));
}

void main() {
    float depth = texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).r;
    vec4 surface = InverseClip * vec4(clipXY, depth * 2.0 - 1.0, 1.0);
    vec3 endpoint = surface.xyz / surface.w;
    float sceneDistance = max(length(endpoint), 0.0001);
    vec3 ray = endpoint / sceneDistance;
    vec3 invRay = mix(vec3(-1.0), vec3(1.0), greaterThanEqual(ray, vec3(0.0))) / max(abs(ray), vec3(1e-7));
    vec2 outer = interval(invRay, BoxMin, BoxMax, sceneDistance);
    float outerLength = outer.y - outer.x;
    if (outerLength <= 0.0) discard;

    // A feathered shell and a dense core use the same integral when the eye crosses any face.
    vec3 inset = min(vec3(4.0, 16.0, 4.0), (BoxMax - BoxMin) * 0.25);
    vec2 inner = interval(invRay, BoxMin + inset, BoxMax - inset, sceneDistance);
    float column = 0.28 * outerLength + 0.72 * max(inner.y - inner.x, 0.0);
    float transmission = max(0.0, (exp2(-6.643856 * column / StormColor.a) - 0.01) / 0.99);

    vec3 hit = ray * outer.x;
    vec2 uv = vec2(dot(hit.xz, SurfaceUV.xy) / 16.0, hit.y / 24.0) + SurfaceUV.zw;
    float detail = (texture(Sampler1, uv).r - 0.85) * 0.30 * smoothstep(0.0, 8.0, outer.x);
    fragColor = vec4(StormColor.rgb * (1.0 + detail), 1.0 - transmission);
}
