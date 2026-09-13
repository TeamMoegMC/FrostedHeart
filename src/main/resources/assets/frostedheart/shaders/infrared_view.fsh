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
const float MIN_TEMP = -20.0;
const float MAX_TEMP = 20.0;
const int INVALID_TEMPERATURE = -32768;

vec3 temperatureToColor(float temp) {
    float normalizedTemp = clamp((temp - MIN_TEMP) / (MAX_TEMP - MIN_TEMP), 0.0, 1.0);
    if (normalizedTemp < 0.33) {
        float t = normalizedTemp / 0.33;
        return mix(vec3(0.0, 0.0, 1.0), vec3(1.0, 0.5, 0.0), t);
    }
    if (normalizedTemp < 0.66) {
        float t = (normalizedTemp - 0.33) / 0.33;
        return mix(vec3(1.0, 0.5, 0.0), vec3(1.0, 1.0, 0.0), t);
    }
    float t = (normalizedTemp - 0.66) / 0.34;
    return mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), t);
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
