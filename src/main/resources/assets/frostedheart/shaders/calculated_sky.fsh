#version 150

// Table lookups match SkyView.sample, IceHaloProfile.phase, and IceHaloMap.phase.
// Sky texels are the cells of an equi-angular cube map aligned with the world axes, as in calculated-sky.md.
// Views exist for two ice depths and two sun elevations, and the radiance is blended linearly between them.
uniform sampler2D lowDepthLowSunSky;
uniform sampler2D lowDepthHighSunSky;
uniform sampler2D highDepthLowSunSky;
uniform sampler2D highDepthHighSunSky;
uniform sampler2D lowDepthLowSunIce;
uniform sampler2D lowDepthHighSunIce;
uniform sampler2D highDepthLowSunIce;
uniform sampler2D highDepthHighSunIce;
uniform sampler2D lowHaloMap;
uniform sampler2D highHaloMap;
uniform sampler2D haloProfile;
// Columns hold the sun's radiance at the low and the high depth, then the diffraction weights at the low and the high depth:
uniform sampler2D rowTables;
uniform mat4 inverseViewProjection;
uniform vec3 sunDirection;
uniform float sunSide;
uniform vec3 lightScale;
uniform float sunScale;
uniform float sunHalfTangent;
uniform float horizonDip;
uniform float exposure;
uniform float texelFaceSize;
uniform float viewBlend;
uniform float depthBlend;
uniform float haloBlend;
uniform float haloLowestElevation;
uniform float haloElevationSpan;
uniform vec3 fogColor;
uniform float rainLevel;
uniform float thunderLevel;
// Linear per channel, as NightSkyDimming.floorColor gives it:
uniform vec3 nightFloor;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265358979;
const float HORIZON_FADE = 0.08;

vec3 encodeSrgb(vec3 linear) {
    vec3 clamped = clamp(linear, 0.0, 1.0);
    return mix(clamped * 12.92, 1.055 * pow(clamped, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, clamped));
}

// Same as SkyDisplay.withFloor, so a black sky shows the night floor and a white one stays white:
vec3 withFloor(vec3 toneMapped) {
    return 1.0 - (1.0 - toneMapped) * (1.0 - nightFloor);
}

float interleavedGradientNoise(vec2 pixel) {
    return fract(52.9829189 * fract(dot(pixel, vec2(0.06711056, 0.00583715))));
}

// Snaps the tangents of a direction on one cube face to its texel center and writes the texel's cell:
vec2 snapOnFace(vec2 tangents, out vec2 cell) {
    vec2 coordinate = atan(tangents) * (4.0 / PI);
    cell = clamp(floor((coordinate + 1.0) * 0.5 * texelFaceSize), 0.0, texelFaceSize - 1.0);
    return tan(((cell + 0.5) / texelFaceSize * 2.0 - 1.0) * (PI / 4.0));
}

// Returns the direction through the center of the sky texel and writes a position that is different for each texel:
vec3 texelCenter(vec3 direction, out vec2 texelPosition) {
    vec3 absolute = abs(direction);
    vec2 cell;
    if (absolute.x >= absolute.y && absolute.x >= absolute.z) {
        float face = direction.x > 0.0 ? 0.0 : 1.0;
        vec2 center = snapOnFace(direction.yz / absolute.x, cell);
        texelPosition = cell + vec2(face * texelFaceSize, 0.0);
        return normalize(vec3(direction.x > 0.0 ? 1.0 : -1.0, center));
    }
    if (absolute.y >= absolute.z) {
        float face = direction.y > 0.0 ? 2.0 : 3.0;
        vec2 center = snapOnFace(direction.xz / absolute.y, cell);
        texelPosition = cell + vec2(face * texelFaceSize, 0.0);
        return normalize(vec3(center.x, direction.y > 0.0 ? 1.0 : -1.0, center.y));
    }
    float face = direction.z > 0.0 ? 4.0 : 5.0;
    vec2 center = snapOnFace(direction.xy / absolute.z, cell);
    texelPosition = cell + vec2(face * texelFaceSize, 0.0);
    return normalize(vec3(center, direction.z > 0.0 ? 1.0 : -1.0));
}

// Returns how much of the pixel the sun's square covers, with edges along the sun's path as vanilla draws it:
float sunCoverage(vec3 direction, float pixelAngle) {
    float forward = dot(direction, sunDirection);
    if (forward <= 0.0) {
        return 0.0;
    }
    vec3 alongPath = vec3(sunDirection.y, -sunDirection.x, 0.0);
    vec2 plane = vec2(dot(direction, alongPath), direction.z) / forward;
    vec2 coverage = clamp((sunHalfTangent - abs(plane)) / pixelAngle + 0.5, 0.0, 1.0);
    return coverage.x * coverage.y;
}

void main() {
    vec4 farPoint = inverseViewProjection * vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
    vec3 direction = normalize(farPoint.xyz / farPoint.w);
    vec3 pixelDirection = direction;
    float pixelAngle = max(length(dFdx(direction)), length(dFdy(direction)));
    vec2 noisePosition = gl_FragCoord.xy;
    if (texelFaceSize > 0.0) {
        direction = texelCenter(direction, noisePosition);
    }

    float elevation = asin(clamp(direction.y, -1.0, 1.0));
    float azimuth = atan(direction.z, direction.x * sunSide);
    float offset = elevation + horizonDip;
    float latitude = offset >= 0.0
            ? offset / (1.0 + horizonDip * 2.0 / PI)
            : offset / (1.0 - horizonDip * 2.0 / PI);
    float position = sign(latitude) * sqrt(abs(latitude) * 2.0 / PI);
    vec2 tableCoordinate = vec2((azimuth + PI) / (2.0 * PI), 0.5 + 0.5 * position);
    vec3 lowDepthSky = mix(texture(lowDepthLowSunSky, tableCoordinate).rgb,
            texture(lowDepthHighSunSky, tableCoordinate).rgb, viewBlend);
    vec3 highDepthSky = mix(texture(highDepthLowSunSky, tableCoordinate).rgb,
            texture(highDepthHighSunSky, tableCoordinate).rgb, viewBlend);
    vec3 lowDepthIce = mix(texture(lowDepthLowSunIce, tableCoordinate).rgb,
            texture(lowDepthHighSunIce, tableCoordinate).rgb, viewBlend);
    vec3 highDepthIce = mix(texture(highDepthLowSunIce, tableCoordinate).rgb,
            texture(highDepthHighSunIce, tableCoordinate).rgb, viewBlend);
    vec3 lowDepthSun = texture(rowTables, vec2(0.125, tableCoordinate.y)).rgb;
    vec3 highDepthSun = texture(rowTables, vec2(0.375, tableCoordinate.y)).rgb;
    vec3 lowDepthDiffraction = texture(rowTables, vec2(0.625, tableCoordinate.y)).rgb;
    vec3 highDepthDiffraction = texture(rowTables, vec2(0.875, tableCoordinate.y)).rgb;
    vec3 radiance = mix(lowDepthSky, highDepthSky, depthBlend);
    vec3 ice = mix(lowDepthIce, highDepthIce, depthBlend);
    // Each depth's diffraction weight scales its own ice view, so the blend matches the blend of whole radiances:
    vec3 diffractedIce = mix(lowDepthDiffraction * lowDepthIce, highDepthDiffraction * highDepthIce, depthBlend);

    float sunAngle = acos(clamp(dot(direction, sunDirection), -1.0, 1.0));
    // The profile holds the tumbling crystals in its first row and the diffraction peak in its second:
    vec3 crystalPhase = texture(haloProfile, vec2(sunAngle / PI, 0.25)).rgb;
    vec2 haloCoordinate = vec2(abs(azimuth) / PI, (elevation - haloLowestElevation) / haloElevationSpan);
    crystalPhase += mix(texture(lowHaloMap, haloCoordinate).rgb, texture(highHaloMap, haloCoordinate).rgb, haloBlend);
    radiance += crystalPhase * ice + texture(haloProfile, vec2(sunAngle / PI, 0.75)).rgb * diffractedIce;
    // The sun's tables also draw the moon's sky, dimmed and tinted by the light's scale:
    radiance *= lightScale;
    vec3 sunRadiance = sunScale * sunCoverage(pixelDirection, pixelAngle) * mix(lowDepthSun, highDepthSun, depthBlend)
            * lightScale;

    vec3 color = encodeSrgb(withFloor(1.0 - exp(-exposure * max(radiance, vec3(0.0)))));
    vec3 sunAdded = vec3(0.0);
    if (any(greaterThan(sunRadiance, vec3(0.0)))) {
        // In clear weather the sun adds what it adds to the sky's radiance, and as the rain level rises its own display, as vanilla adds its sun:
        vec3 litColor = encodeSrgb(withFloor(1.0 - exp(-exposure * max(radiance + sunRadiance, vec3(0.0)))));
        vec3 sunColor = encodeSrgb(1.0 - exp(-exposure * sunRadiance));
        sunAdded = mix(litColor - color, sunColor, rainLevel);
    }

    // Same weather graying as ClientLevel.getSkyColor and SkyViewSet.horizonDisplayColor:
    float rainGray = dot(color, vec3(0.3, 0.59, 0.11)) * 0.6;
    color = mix(vec3(rainGray), color, 1.0 - rainLevel * 0.75);
    float thunderGray = dot(color, vec3(0.3, 0.59, 0.11)) * 0.2;
    color = mix(vec3(thunderGray), color, 1.0 - thunderLevel * 0.75);

    color = mix(color, fogColor, rainLevel);
    float horizonFade = smoothstep(0.0, HORIZON_FADE, direction.y);
    color = mix(fogColor, color, horizonFade);
    // The sun goes over the weather faded by the rain level, as vanilla's sun does, so it shows through snow:
    color += sunAdded * ((1.0 - rainLevel) * horizonFade);
    color += (interleavedGradientNoise(noisePosition) - 0.5) / 255.0;
    fragColor = vec4(color, 1.0);
}
