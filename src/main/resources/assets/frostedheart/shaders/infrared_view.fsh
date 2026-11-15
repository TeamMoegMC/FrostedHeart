/*
 * Copyright (c) 2021-2024 KilaBash (https://github.com/Yefancy)
 * Licensed to TeamMoeg under CC BY-NC-SA 3.0.
 */
#version 150

uniform float radius;
uniform sampler2D depthTexture;
uniform isampler2D surfaceTemperature;
uniform sampler2D terrainDepthTexture;
uniform bool hasTerrainDepth;
uniform isampler3D environmentTemperature;
uniform bool hasEnvironmentTemperature;
uniform vec3 cameraToTemperatureOrigin;
uniform mat4 u_InverseViewProjectionMatrix;
out vec4 FragColor;
const float SCANNING_WIDTH = 3.0;
const float MIN_TEMP = -50.0;
const float MAX_TEMP = 50.0;
const int INVALID_TEMPERATURE = -32768;
const vec3 VIOLET = vec3(0.5, 0.0, 1.0);
const vec3 BLUE = vec3(0.0, 0.0, 1.0);
const vec3 CYAN = vec3(0.0, 1.0, 1.0);
const vec3 GREEN = vec3(0.0, 1.0, 0.0);
const vec3 YELLOW = vec3(1.0, 1.0, 0.0);
const vec3 RED = vec3(1.0, 0.0, 0.0);

// Violet at -50 C through blue (-30), cyan (0), green (25, comfortable), yellow (37.5) to red at 50 C. Neighbouring
// stops are one hue sector apart, so each linear mix is a hue sweep and the hue never turns back.
vec3 temperatureToColor(float temp) {
    float t = clamp(temp, MIN_TEMP, MAX_TEMP);
    if (t < -30.0) return mix(VIOLET, BLUE, (t + 50.0) / 20.0);
    if (t < 0.0) return mix(BLUE, CYAN, (t + 30.0) / 30.0);
    if (t < 25.0) return mix(CYAN, GREEN, t / 25.0);
    if (t < 37.5) return mix(GREEN, YELLOW, (t - 25.0) / 12.5);
    return mix(YELLOW, RED, (t - 37.5) / 12.5);
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    float depth = texelFetch(depthTexture, pixel, 0).r;
    if (depth == 1.0) discard;
    vec2 uv = (vec2(pixel) + 0.5) / vec2(textureSize(depthTexture, 0));
    vec4 position = u_InverseViewProjectionMatrix * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    float distance = length(position.xyz / position.w);
    if (!(distance < radius)) discard;
    if (distance > radius - SCANNING_WIDTH) {
        float edge = smoothstep(radius - SCANNING_WIDTH, radius, distance);
        FragColor = vec4(vec3(edge), 0.0);
        return;
    }
    // A later depth writer occludes the captured terrain. Its body must not inherit that terrain's heat pattern.
    bool terrainVisible = hasTerrainDepth && depth == texelFetch(terrainDepthTexture, pixel, 0).r;
    int value = terrainVisible ? texelFetch(surfaceTemperature, pixel, 0).r : INVALID_TEMPERATURE;
    if (!terrainVisible && hasEnvironmentTemperature) {
        ivec3 cell = ivec3(floor(position.xyz / position.w + cameraToTemperatureOrigin));
        if (all(greaterThanEqual(cell, ivec3(0))) && all(lessThan(cell, textureSize(environmentTemperature, 0))))
            value = texelFetch(environmentTemperature, cell, 0).r;
    }
    float temperature = value == INVALID_TEMPERATURE ? MIN_TEMP : float(value) * 0.25;
    // Premultiplied blending preserves the original mix and destination alpha.
    FragColor = vec4(temperatureToColor(temperature) * 0.43, 0.43);
}
