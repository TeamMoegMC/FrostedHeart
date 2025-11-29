/*
 * Copyright (c) 2021-2024 KilaBash (https://github.com/Yefancy) Licensed to TeamMoeg
 * License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 */

#version 150

struct HeatArea {
    vec4 position; // [3 float for position, 1 float for mode]
    vec4 data; // [1 float for value, 1 float for radius, 2 float for pillar (upper, lower)]
};

layout (std140) uniform Adjusts {
    HeatArea adjusts[512];     //  16  8 * 512
};

uniform vec2 iResolution;

uniform float radius; // scanning radius
uniform int adjust_num; // number of adjucts

uniform sampler2D mainTexture; // main target texture
uniform sampler2D depthTexture; // depth texture
uniform sampler2D noHandDepthTexture; // no hand depth texture - used for iris
uniform sampler2D noTranslucentDepthTexture; // no translucent depth texture - used for iris

uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;
uniform vec3 u_CameraPosition;

in vec2 texCoord;
out vec4 FragColor;

const float SCANNING_WIDTH = 3.0;
const float MIN_TEMP = -20;
const float MAX_TEMP = 20;

vec3 temperatureToColor(float temp) {
    float normalizedTemp = (temp - MIN_TEMP) / (MAX_TEMP - MIN_TEMP);
    normalizedTemp = clamp(normalizedTemp, 0.0, 1.0);

    vec3 color;
    if (normalizedTemp < 0.33) {
        float t = normalizedTemp / 0.33;
        color = mix(vec3(0.0, 0.0, 1.0), vec3(1.0, 0.5, 0.0), t);
    } else if (normalizedTemp < 0.66) {
        float t = (normalizedTemp - 0.33) / 0.33;
        color = mix(vec3(1.0, 0.5, 0.0), vec3(1.0, 1.0, 0.0), t);
    } else {
        float t = (normalizedTemp - 0.66) / 0.34;
        color = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), t);
    }

    return color;
}

vec3 rgbaToGrayscale(vec3 color) {
    // Luminance weighting factors
    float luminance = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
    return vec3(luminance);
}

void main() {
    vec4 color = texture(mainTexture, texCoord);
    float depth = texture(depthTexture, texCoord).r;
    float noHandDepth = texture(noHandDepthTexture, texCoord).r;
    float noTranslucentDepthTexture = texture(noTranslucentDepthTexture, texCoord).r;

    if (depth == 1.0 || noHandDepth != noTranslucentDepthTexture) {
        FragColor = color;
        return;
    }
//    color.xyz = rgbaToGrayscale(color.xyz);

    vec3 ndc;
    ndc.xy = texCoord.xy * 2.0 - 1.0;
    ndc.z = depth * 2.0 - 1.0;
    vec4 clipSpacePos = vec4(ndc, 1.0);
    vec4 viewSpacePos = u_InverseProjectionMatrix * clipSpacePos;
    viewSpacePos /= viewSpacePos.w;
    vec4 worldSpacePos = u_InverseViewMatrix * viewSpacePos;
    worldSpacePos /= worldSpacePos.w;
    float distToCamera = length(worldSpacePos.xyz);
    worldSpacePos.xyz += u_CameraPosition;

    vec3 heatColor = vec3(0.0);
    float heat = MIN_TEMP;
    for (int i = 0; i < adjust_num; i++) {
        HeatArea adjust = adjusts[i];
        float mode = adjust.position.w;
        float radius = adjust.data.y;
        float heatValue = adjust.data.x;
        if (mode < 0.5) {
            // cubric
            float dist = max(max(abs(worldSpacePos.x - adjust.position.x), abs(worldSpacePos.y - adjust.position.y)), abs(worldSpacePos.z - adjust.position.z));
            heatValue = mix(MIN_TEMP, heatValue, step(dist, radius));
        } else if (mode < 1.5) {
            // pillar
            float upper = adjust.data.z;
            float lower = adjust.data.w;
            if (upper < worldSpacePos.y || lower > worldSpacePos.y) {
                continue;
            }
            float dist = length(worldSpacePos.xz - adjust.position.xz);
            heatValue = mix(MIN_TEMP, heatValue, step(dist, radius));
        } else if (mode < 2.5){
            // sphere
            float dist = length(worldSpacePos.xyz - adjust.position.xyz);
            heatValue = mix(MIN_TEMP, heatValue, step(dist, radius));
        }
        heat = max(heatValue, heat);
    }

    heatColor = temperatureToColor(heat);

    if (distToCamera < radius) {
        if (distToCamera > radius - SCANNING_WIDTH) {
            float step = smoothstep(radius - SCANNING_WIDTH, radius, distToCamera);
            FragColor = color + vec4(step, step, step, 0.);
        } else{
            FragColor = vec4(mix(color.xyz, heatColor, 0.43), color.a);
        }
    } else {
        FragColor = color;
    }
}
