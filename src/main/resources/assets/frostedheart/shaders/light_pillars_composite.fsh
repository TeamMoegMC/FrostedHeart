#version 150
// Shows the light pillars that light_pillars.glsl summed in linear light, tone mapped and encoded once, for LightPillarRenderer to screen blend onto the frame.
// The summed linear light, the size of the main target:
uniform sampler2D pillarLight;
// The share of the pillars that shows by the daylight and the rain level:
uniform float opacity;
out vec4 fragColor;

vec3 encodeSrgb(vec3 linear) {
    vec3 clamped = clamp(linear, 0.0, 1.0);
    return mix(clamped * 12.92, 1.055 * pow(clamped, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, clamped));
}

void main() {
    vec3 summed = texelFetch(pillarLight, ivec2(gl_FragCoord.xy), 0).rgb;
    if (!(max(summed.r, max(summed.g, summed.b)) > 0.0)) {
        discard;
    }
    fragColor = vec4(encodeSrgb(1.0 - exp(-summed)) * opacity, 1.0);
}
