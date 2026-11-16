// FHShaders prepends the version line and defines VERTEX_SHADER or FRAGMENT_SHADER.
// The glints of diamond dust near the eye, one instance of four corners per point with no vertex data, as DustGlitterField places, culls, and lights them.
// Each point lives one life after another, each a crystal that starts at a hashed place in a cube of cellSize blocks repeating over the world, falls in a straight line, and is lit once.
// A life shows only where its crystal could have fallen, at or above the first open height of its column at the middle of its lit window and where it is now.
// The sky's ice phase toward a glint, as calculated_sky.fsh looks it up without the diffraction peak, sets how many lives show there and tints them.
// The vertex stage computes each glint's light times the exposure and spans its sprite, and the fragment stage spreads the light over a round sprite with a Gaussian core and displays it for a screen blend.
#ifdef VERTEX_SHADER
// Takes positions relative to the camera, with view bobbing in the projection:
uniform mat4 viewProjection;
// The offset of the eye in its cube:
uniform vec3 eyeOffset;
// The clock's share of its wrap, as a whole number of 1 / 4096 steps and the rest:
uniform float clockHigh;
uniform float clockLow;
uniform float clockWrap;
uniform int leastLifeCycles;
uniform int lifeCycleRange;
// The share of the reference count of lives, and how much longer than at the reference lit windows last:
uniform float lifeRate;
uniform float timeScale;
uniform float fallSpeed;
uniform float slowestFallShare;
uniform float fallShareRange;
uniform float widestTilt;
uniform float litTimeBase;
uniform float litTimeSlope;
uniform float litTimeTail;
uniform float litTimeTailPower;
uniform float litRamp;
// The share of points the dust's extinction draws, and the dust's bottom relative to the eye, above which glints thin:
uniform float extinctionShare;
uniform float dustBottomHeight;
uniform float scaleHeight;
// The share of lives shown away from the halos, the phase on the 22 degree halo, and the margin of a shown share:
uniform float plainShare;
uniform float ringPhase;
uniform float shownMargin;
uniform float tintShare;
uniform float nearShare;
uniform float nearShareDistance;
uniform float nearDistance;
uniform float widestNear;
uniform float nearestDistance;
uniform float nearFadeEnd;
// The size at a distance, as DustGlitterField.sizeFactor, the sprite's radius in pixels at dustGlitterSize, its least radius, and the size of a pixel in clip space over w:
uniform float sizeDistance;
uniform float sizeSpread;
uniform float smallestSize;
uniform float largestSize;
uniform float halfWidth;
uniform float smallestRadius;
uniform vec2 pixelToClip;
// The light's beam at the eye per channel times the gain:
uniform vec3 light;
uniform float exposure;
uniform vec3 lightDirection;
uniform float lightSide;
uniform float lightElevation;
uniform sampler2D haloProfile;
uniform sampler2D lowHaloMap;
uniform sampler2D highHaloMap;
uniform float haloBlend;
uniform float haloLowestElevation;
uniform float haloElevationSpan;
// Per plate kind, the tilt spread in radians, and the blended mirror lobe scale over the kind's cross section:
uniform vec2 tiltSigmas;
uniform vec3 lobeScales[2];
uniform vec3 refractiveIndices;
uniform float blurVariance;
uniform float rowFadeAngle;
uniform float cellSize;
uniform float fadeStart;
uniform float fadeEnd;
uniform float falloffDistance;
// The first open height of each column around the eye at its world column modulo windowSize, the texel of the window's first column, the eye's column counted from it, the eye's place in its block, and its height:
uniform sampler2D columnHeights;
uniform int windowSize;
uniform ivec2 windowOrigin;
uniform ivec2 eyeInWindow;
uniform vec2 eyeFraction;
uniform float eyeY;

flat out vec3 glintLight;
noperspective out vec2 spriteCorner;

const float PI = 3.14159265358979;
const float INVERSE_SQUARE_ROOT_OF_TWO_PI = 0.3989422804014327;
const float SMALLEST_SIGMA = 1.0e-9;
const uint POINT_SEED = 0x2545f491u;
const uint LIFE_SALT = 0x6c8e9cf5u;
const vec4 OUTSIDE_CLIP = vec4(2.0, 2.0, 2.0, 1.0);

// The lowbias32 integer hash by Chris Wellons, as DustGlitterField.hashOf:
uint hashOf(uint value) {
    uint mixed = value;
    mixed ^= mixed >> 16;
    mixed *= 0x7feb352du;
    mixed ^= mixed >> 15;
    mixed *= 0x846ca68bu;
    mixed ^= mixed >> 16;
    return mixed;
}

float uniformOf(uint hashed) {
    return float(hashed >> 8) / 16777216.0;
}

// As DustGlitterField.shownWeight:
float shownWeight(float shownShare, float hashShare) {
    return clamp((shownShare - hashShare) / shownMargin + 0.5, 0.0, 1.0);
}

float gaussian(float offset, float sigma) {
    float ratio = offset / sigma;
    return INVERSE_SQUARE_ROOT_OF_TWO_PI / sigma * exp(-0.5 * ratio * ratio);
}

// As IceMirror.slabReflectance for each channel's refractive index:
vec3 slabReflectance(float sineOfGrazingAngle) {
    float cosineOfIncidence = clamp(sineOfGrazingAngle, 0.0, 1.0);
    float squaredSineOfIncidence = 1.0 - cosineOfIncidence * cosineOfIncidence;
    vec3 cosineOfRefraction = sqrt(1.0 - squaredSineOfIncidence / (refractiveIndices * refractiveIndices));
    vec3 perpendicular = (cosineOfIncidence - refractiveIndices * cosineOfRefraction)
            / (cosineOfIncidence + refractiveIndices * cosineOfRefraction);
    vec3 parallel = (refractiveIndices * cosineOfIncidence - cosineOfRefraction)
            / (refractiveIndices * cosineOfIncidence + cosineOfRefraction);
    vec3 perpendicularFace = perpendicular * perpendicular;
    vec3 parallelFace = parallel * parallel;
    return perpendicularFace / (1.0 + perpendicularFace) + parallelFace / (1.0 + parallelFace);
}

// The plates' mirror lobe per steradian, as DustGlitterField.mirrorLobe with each kind's cross section in its scale:
vec3 mirrorLobe(float elevation, float sideAngle) {
    float grazingSine = abs(sin(0.5 * (elevation - lightElevation)));
    vec3 lobe = vec3(0.0);
    for (int kind = 0; kind < 2; kind++) {
        float tiltSigma = tiltSigmas[kind];
        float heightSigma = max(SMALLEST_SIGMA, 2.0 * tiltSigma);
        float widthSigma = max(SMALLEST_SIGMA, 2.0 * tiltSigma * grazingSine);
        lobe += lobeScales[kind] * gaussian(elevation + lightElevation, sqrt(heightSigma * heightSigma + blurVariance))
                * gaussian(sideAngle, sqrt(widthSigma * widthSigma + blurVariance));
    }
    return grazingSine * slabReflectance(grazingSine) * lobe;
}

// The sky's ice phase toward a direction, the profile's first row and the halo maps, and below the maps the plates' lobe:
vec3 icePhase(vec3 direction) {
    float elevation = asin(clamp(direction.y, -1.0, 1.0));
    float azimuth = atan(direction.z, direction.x * lightSide);
    float lightAngle = acos(clamp(dot(direction, lightDirection), -1.0, 1.0));
    vec2 haloCoordinate = vec2(abs(azimuth) / PI, (elevation - haloLowestElevation) / haloElevationSpan);
    vec3 mapPhase = mix(texture(lowHaloMap, haloCoordinate).rgb, texture(highHaloMap, haloCoordinate).rgb, haloBlend);
    if (elevation < haloLowestElevation) {
        // As DustGlitterField.belowMapPhase, where the map's texture clamps to its lowest row:
        float sideAngle = abs(azimuth) * cos(elevation);
        float rowFade = smoothstep(haloLowestElevation - rowFadeAngle, haloLowestElevation, elevation);
        mapPhase = max(mirrorLobe(elevation, sideAngle)
                + (mapPhase - mirrorLobe(haloLowestElevation, sideAngle)) * rowFade, vec3(0.0));
    }
    return texture(haloProfile, vec2(lightAngle / PI, 0.25)).rgb + mapPhase;
}

// Whether a crystal at an offset from the eye is at or above the first open height of its column, as DustColumnWindow.columnInWindow, texelOfColumn, and isOpenAbove:
bool isOpenAbove(vec3 offsetFromEye) {
    ivec2 column = clamp(eyeInWindow + ivec2(floor(eyeFraction + offsetFromEye.xz)), 0, windowSize - 1);
    float firstOpenY = texelFetch(columnHeights, (windowOrigin + column) & ivec2(windowSize - 1), 0).r;
    return eyeY + offsetFromEye.y >= firstOpenY;
}

void hide() {
    gl_Position = OUTSIDE_CLIP;
    glintLight = vec3(0.0);
    spriteCorner = vec2(0.0);
}

void main() {
    uint pointSeed = hashOf(uint(gl_InstanceID) ^ POINT_SEED);
    uint densityHash = hashOf(pointSeed);
    if (!(uniformOf(densityHash) < extinctionShare)) {
        hide();
        return;
    }
    // The life's number and age, as DustGlitterField.lifeNumber and lifeAge:
    uint lifeClockHash = hashOf(densityHash);
    uint referenceCycles = uint(leastLifeCycles) + hashOf(lifeClockHash) % uint(lifeCycleRange);
    float cycleCount = max(1.0, floor(float(referenceCycles) * lifeRate));
    uint cycles = uint(cycleCount);
    float scaled = clockHigh * cycleCount;
    float whole = floor(scaled);
    float rest = (scaled - whole) + clockLow * cycleCount + uniformOf(lifeClockHash);
    float restWhole = floor(rest);
    uint life = (uint(whole) + uint(restWhole)) % cycles;
    float lifeSeconds = clockWrap / cycleCount;
    float age = (rest - restWhole) * lifeSeconds;

    // The life's numbers, as DustGlitterField.lifeHash, from LIT_TIME_HASH to NEAR_HASH:
    uint litTimeHash = hashOf(pointSeed ^ hashOf(life ^ LIFE_SALT));
    uint litStartHash = hashOf(litTimeHash);
    float litTimeShare = uniformOf(litTimeHash);
    float litSeconds = timeScale
            * (litTimeBase + litTimeSlope * litTimeShare + litTimeTail * pow(litTimeShare, litTimeTailPower));
    float litStart = uniformOf(litStartHash) * (lifeSeconds - litSeconds);
    float ramp = litRamp * timeScale;
    float envelope = smoothstep(0.0, ramp, age - litStart) * smoothstep(0.0, ramp, litStart + litSeconds - age);
    if (!(envelope > 0.0)) {
        hide();
        return;
    }
    uint startXHash = hashOf(litStartHash);
    uint startYHash = hashOf(startXHash);
    uint startZHash = hashOf(startYHash);
    uint speedHash = hashOf(startZHash);
    uint tiltHash = hashOf(speedHash);
    uint azimuthHash = hashOf(tiltHash);
    uint directionHash = hashOf(azimuthHash);
    uint nearHash = hashOf(directionHash);

    // As DustGlitterField.velocity, lifeStart, and nearestOffset:
    float speed = fallSpeed * (slowestFallShare + fallShareRange * uniformOf(speedHash));
    float tilt = widestTilt * uniformOf(tiltHash);
    float fallAzimuth = 2.0 * PI * uniformOf(azimuthHash);
    vec3 velocity = speed * vec3(sin(tilt) * cos(fallAzimuth), -cos(tilt), sin(tilt) * sin(fallAzimuth));
    vec3 start = vec3(uniformOf(startXHash), uniformOf(startYHash), uniformOf(startZHash)) * cellSize;
    vec3 relative = start + velocity * age - eyeOffset;
    vec3 offset = relative - cellSize * floor(relative / cellSize + 0.5);
    float eyeDistance = length(offset);
    float fade = 1.0 - smoothstep(fadeStart, fadeEnd, eyeDistance);
    float nearFade = smoothstep(nearestDistance, nearFadeEnd, eyeDistance);
    if (!(fade > 0.0) || !(nearFade > 0.0)) {
        hide();
        return;
    }
    // Where the glint is at the middle of its lit window, as DustGlitterField.middleOffset:
    vec3 middleOffset = offset + velocity * (litStart + 0.5 * litSeconds - age);
    float middleDistance = max(length(middleOffset), 1.0e-4);
    // As DustGlitterField.nearShare:
    float nearWeight = shownWeight(nearShare + (1.0 + 0.5 * shownMargin - nearShare)
            * smoothstep(nearShareDistance, nearDistance, middleDistance), uniformOf(nearHash));
    if (!(nearWeight > 0.0)) {
        hide();
        return;
    }
    // A crystal can only have fallen where nothing above stops falling things, which the middle of its lit window decides, and it goes out where it falls onto a roof or the ground:
    if (!isOpenAbove(middleOffset) || !isOpenAbove(offset)) {
        hide();
        return;
    }

    vec3 phase = max(icePhase(middleOffset / middleDistance), vec3(0.0));
    float largestPhase = max(phase.r, max(phase.g, phase.b));
    // As DustGlitterField.ringShare, shownShare, and tint:
    float ringShare = ringPhase > 0.0 ? min(1.0, largestPhase / ringPhase) : 1.0;
    float shown = shownWeight(plainShare + (1.0 + 0.5 * shownMargin - plainShare) * ringShare,
            uniformOf(directionHash));
    if (!(shown > 0.0)) {
        hide();
        return;
    }
    vec3 tint = largestPhase > 0.0 ? mix(vec3(1.0), phase / largestPhase, tintShare * ringShare) : vec3(1.0);
    // As DustGlitterField.heightWeight, nearBlur, sizeFactor, spriteRadius, and peakFactor:
    float heightWeight = exp(-max(0.0, offset.y - dustBottomHeight) / scaleHeight);
    float blur = clamp(nearDistance / eyeDistance, 1.0, widestNear);
    float size = clamp(pow(sizeDistance / eyeDistance, sizeSpread), smallestSize, largestSize);
    float radius = max(smallestRadius, halfWidth * size);
    float widening = radius / halfWidth;
    float ratio = eyeDistance / falloffDistance;
    float weight = envelope * fade * nearFade * nearWeight * shown * heightWeight
            / ((1.0 + ratio * ratio) * blur * blur * widening * widening);
    glintLight = exposure * max(light * tint * weight, vec3(0.0));

    vec2 corner = vec2(float(gl_VertexID & 1), float(gl_VertexID >> 1)) * 2.0 - 1.0;
    vec4 center = viewProjection * vec4(offset, 1.0);
    center.xy += corner * radius * pixelToClip * center.w;
    gl_Position = center;
    spriteCorner = corner;
}
#endif

#ifdef FRAGMENT_SHADER
// Half the squared ratio of the sprite's radius to its core's standard deviation:
uniform float spriteSpread;

flat in vec3 glintLight;
noperspective in vec2 spriteCorner;
out vec4 fragColor;

vec3 encodeSrgb(vec3 linear) {
    vec3 clamped = clamp(linear, 0.0, 1.0);
    return mix(clamped * 12.92, 1.055 * pow(clamped, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, clamped));
}

// The core's Gaussian, tapered to 0 at the sprite's rim, so a glint whose core saturates shows wider:
void main() {
    float squaredRadius = dot(spriteCorner, spriteCorner);
    if (!(squaredRadius < 1.0)) {
        discard;
    }
    float taper = 1.0 - squaredRadius;
    float shape = exp(-spriteSpread * squaredRadius) * taper * taper;
    fragColor = vec4(encodeSrgb(1.0 - exp(-glintLight * shape)), 1.0);
}
#endif
