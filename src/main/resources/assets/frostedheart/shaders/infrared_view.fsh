/*
 * Copyright (c) 2021-2024 KilaBash (https://github.com/Yefancy)
 * Licensed to TeamMoeg under CC BY-NC-SA 3.0.
 */
#version 150

uniform vec2 iResolution;
uniform float radius;

uniform sampler2D mainTexture;
uniform sampler2D depthTexture;
uniform sampler2D noHandDepthTexture;
uniform sampler2D noTranslucentDepthTexture;
uniform isampler3D temperatureTexture;

uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;
uniform vec3 temperatureCameraOffset;

in vec2 texCoord;
out vec4 FragColor;

const float SCANNING_WIDTH = 3.0;
const float MIN_TEMP = -20.0;
const float MAX_TEMP = 20.0;
const int TEXTURE_SIZE = 144;
const int INVALID_TEMPERATURE = -32768;
const float SURFACE_SAMPLE_SCALE = 2047.0 / 2048.0;

vec3 temperatureToColor(float temp) {
    float normalizedTemp = clamp(
        (temp - MIN_TEMP) / (MAX_TEMP - MIN_TEMP), 0.0, 1.0);
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
    vec4 color = texture(mainTexture, texCoord);
    float depth = texture(depthTexture, texCoord).r;
    float noHandDepth = texture(noHandDepthTexture, texCoord).r;
    float noTranslucentDepth = texture(
        noTranslucentDepthTexture, texCoord).r;

    if (depth == 1.0 || noHandDepth != noTranslucentDepth) {
        FragColor = color;
        return;
    }

    vec3 ndc = vec3(texCoord.xy * 2.0 - 1.0, depth * 2.0 - 1.0);
    vec4 viewSpacePos = u_InverseProjectionMatrix * vec4(ndc, 1.0);
    viewSpacePos /= viewSpacePos.w;
    vec4 relativeWorldPos = u_InverseViewMatrix * viewSpacePos;
    relativeWorldPos /= relativeWorldPos.w;
    float distToCamera = length(relativeWorldPos.xyz);
    vec3 temperatureSamplePos = temperatureCameraOffset
        + relativeWorldPos.xyz * SURFACE_SAMPLE_SCALE;

    ivec3 temperatureTexel = ivec3(floor(temperatureSamplePos));
    if (distToCamera >= radius
            || any(lessThan(temperatureTexel, ivec3(0)))
            || any(greaterThanEqual(
                temperatureTexel, ivec3(TEXTURE_SIZE)))) {
        FragColor = color;
        return;
    }

    int encodedTemperature = texelFetch(
        temperatureTexture, temperatureTexel, 0).r;

    if (distToCamera > radius - SCANNING_WIDTH) {
        float edge = smoothstep(
            radius - SCANNING_WIDTH, radius, distToCamera);
        FragColor = color + vec4(edge, edge, edge, 0.0);
        return;
    }

    float temperatureC = encodedTemperature == INVALID_TEMPERATURE
        ? MIN_TEMP
        : float(encodedTemperature) * 0.25;
    vec3 heatColor = temperatureToColor(temperatureC);
    FragColor = vec4(mix(color.xyz, heatColor, 0.43), color.a);
}
