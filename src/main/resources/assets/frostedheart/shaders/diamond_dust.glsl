// FHShaders prepends the version line and defines VERTEX_SHADER or FRAGMENT_SHADER.
// The glints of diamond dust near the eye, one point per vertex with no vertex data, as DustGlitterField places, culls, and lights them.
// A point's index gives it a fixed position in a cube of cellSize blocks that repeats over the world, and it shows at its copy nearest the eye.
// Its light is the light's beam times the sky's ice phase toward it, as calculated_sky.fsh looks it up, without the diffraction peak.
// The vertex stage computes each glint's light times the exposure, and the fragment stage spreads it over a round sprite with a Gaussian core and displays it for a screen blend.
#ifdef VERTEX_SHADER
// Takes positions relative to the camera, with view bobbing in the projection:
uniform mat4 viewProjection;
// The offsets of the eye and of the drift in their cubes, and the eye's cube less the drift's cube:
uniform vec3 eyeOffset;
uniform vec3 driftOffset;
uniform ivec3 copyBase;
// The clock over its wrap, from 0 up to 1:
uniform float clockShare;
// The share of points the dust's extinction draws, and the dust's bottom relative to the eye, above which glints thin:
uniform float extinctionShare;
uniform float dustBottomHeight;
uniform float scaleHeight;
// The sprite's width in pixels:
uniform float pointSize;
// The light's beam at the eye per channel times the gain, and the floor of the phase:
uniform vec3 light;
uniform vec3 floorPhase;
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
uniform float swayRadius;
uniform float twinkleDuty;
uniform int leastTwinkleCycles;
uniform int twinkleCycleRange;
uniform int leastSwayCycles;
uniform int swayCycleRange;

flat out vec3 glintLight;

const float PI = 3.14159265358979;
const float INVERSE_SQUARE_ROOT_OF_TWO_PI = 0.3989422804014327;
const float SMALLEST_SIGMA = 1.0e-9;
const uint POINT_SEED = 0x2545f491u;
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

// As DustGlitterField.cyclePhase:
float cyclePhase(uint hashed, int leastCycles, int cycleRange) {
    uint cycles = uint(leastCycles) + hashOf(hashed) % uint(cycleRange);
    return fract(clockShare * float(cycles) + uniformOf(hashed));
}

// As DustGlitterField.twinkle:
float twinkle(float phase) {
    if (!(phase < twinkleDuty)) {
        return 0.0;
    }
    float sine = sin(PI * phase / twinkleDuty);
    return 2.0 / twinkleDuty * sine * sine;
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

void hide() {
    gl_Position = OUTSIDE_CLIP;
    gl_PointSize = 1.0;
    glintLight = vec3(0.0);
}

void main() {
    uint xHash = hashOf(uint(gl_VertexID) ^ POINT_SEED);
    uint yHash = hashOf(xHash);
    uint zHash = hashOf(yHash);
    uint densityHash = hashOf(zHash);
    uint swayHash = hashOf(densityHash);
    uint twinkleHash = hashOf(swayHash);
    vec3 relative = vec3(uniformOf(xHash), uniformOf(yHash), uniformOf(zHash)) * cellSize + driftOffset - eyeOffset;
    vec3 copyShift = floor(relative / cellSize + 0.5);
    vec3 offset = relative - cellSize * copyShift;
    float fade = 1.0 - smoothstep(fadeStart, fadeEnd, length(offset));
    if (!(fade > 0.0) || !(uniformOf(densityHash) < extinctionShare)) {
        hide();
        return;
    }
    ivec3 copyCell = copyBase - ivec3(copyShift);
    uint copyHash = hashOf(twinkleHash ^ hashOf(uint(copyCell.x) ^ hashOf(uint(copyCell.y) ^ hashOf(uint(copyCell.z)))));
    float flash = twinkle(cyclePhase(copyHash, leastTwinkleCycles, twinkleCycleRange));
    if (!(flash > 0.0)) {
        hide();
        return;
    }
    // As DustGlitterField.heightWeight:
    float heightWeight = exp(-max(0.0, offset.y - dustBottomHeight) / scaleHeight);
    float swayAngle = 2.0 * PI * cyclePhase(swayHash, leastSwayCycles, swayCycleRange);
    offset.xz += swayRadius * vec2(cos(swayAngle), sin(swayAngle));

    float eyeDistance = max(length(offset), 1.0e-4);
    vec3 direction = offset / eyeDistance;
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
    vec3 phase = floorPhase + texture(haloProfile, vec2(lightAngle / PI, 0.25)).rgb + mapPhase;
    float ratio = eyeDistance / falloffDistance;
    vec3 value = light * phase * (flash * fade * heightWeight / (1.0 + ratio * ratio));
    glintLight = exposure * max(value, vec3(0.0));
    gl_Position = viewProjection * vec4(offset, 1.0);
    gl_PointSize = pointSize;
}
#endif

#ifdef FRAGMENT_SHADER
// Half the squared ratio of the sprite's radius to its core's standard deviation:
uniform float spriteSpread;

flat in vec3 glintLight;
out vec4 fragColor;

vec3 encodeSrgb(vec3 linear) {
    vec3 clamped = clamp(linear, 0.0, 1.0);
    return mix(clamped * 12.92, 1.055 * pow(clamped, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, clamped));
}

// The core's Gaussian, tapered to 0 at the sprite's rim, so a glint whose core saturates shows wider:
void main() {
    vec2 fromCenter = 2.0 * gl_PointCoord - 1.0;
    float squaredRadius = dot(fromCenter, fromCenter);
    if (!(squaredRadius < 1.0)) {
        discard;
    }
    float taper = 1.0 - squaredRadius;
    float shape = exp(-spriteSpread * squaredRadius) * taper * taper;
    fragColor = vec4(encodeSrgb(1.0 - exp(-glintLight * shape)), 1.0);
}
#endif
