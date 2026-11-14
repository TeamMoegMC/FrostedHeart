// FHShaders prepends the version line and defines VERTEX_SHADER or FRAGMENT_SHADER, and CELL_ATLAS for the pass that fills the cell atlas.
// Light pillars as in calculated-sky.md. pillarRadiance approximates LightPillarOptics.dustRadiance and widestLateralSpread with 4 instead of 8 points over 3 instead of 4 tilt sigmas.
// Each pillar has one ribbon per plate kind that covers it on the screen, and the fragment shader computes its light from the crystals in front of the scene.
// With cells, the atlas pass computes each cell of each ribbon once without the scene, and the fragment shader reads its cell and keeps the share of crystals in front of the scene.
// Takes positions relative to the eye, which view bobbing moves away from the camera:
uniform mat4 viewProjection;
// Light positions relative to the eye, then the horizon around the light toward the eye in radians:
uniform vec4 lights[48];
// The ceiling and the top around the light toward the eye in radians, as LightPillarHorizons has them:
uniform vec2 overhangs[48];
// Colors times strength and brightness, then the opacity:
uniform vec4 colors[48];
uniform vec2 tiltSigmas;
uniform vec2 shares;
uniform float refractiveIndex;
uniform float extinction;
uniform float dustBottomHeight;
uniform float scaleHeight;
uniform float lateralBlur;
uniform float marginAngle;
uniform float visibleValue;
uniform float glintValue;
uniform float highestElevation;
// Crystals nearer to the eye than this many blocks count less, and none count nearer than a quarter of it, unless it is 0:
uniform float nearFade;
// Turns the crystal count of pillarRadiance times the face area per volume into crystals per pixel or per cell, or 0 for smooth pillars:
uniform float crystalCountScale;
// Above 0, the fragment stage leaves out a part to measure its cost, as LightPillarRenderer.setDebugMode describes:
uniform int debugMode;
// Above 0, the side of the square cells that pillars are drawn in, on each ribbon's own grid as LightPillarCells lays it out:
uniform float cellAngle;
// The rows of each ribbon in the cell atlas:
uniform int atlasRows;

const float PI = 3.14159265358979;
const float INVERSE_SQUARE_ROOT_OF_TWO_PI = 0.3989422804014327;
// A scene distance beyond every crystal, for directions where no scene stands:
const float NO_SCENE_DISTANCE = 1.0e30;
const int NODE_COUNT = 4;
const float NODES[4] = float[4](-0.8611363115940526, -0.3399810435848563, 0.3399810435848563, 0.8611363115940526);
const float WEIGHTS[4] = float[4](0.3478548451374538, 0.6521451548625461, 0.6521451548625461, 0.3478548451374538);
const float TILT_RANGE_SIGMAS = 3.0;
const float SMALLEST_RISE = 0.01;
const float SMALLEST_SINE_AT_CRYSTAL = 1.0e-6;
const float MOST_RIBBON_SPREADS = 8.0;
const float NEAR_FADE_START_SHARE = 0.25;
const float SMALLEST_SINE_OF_GRAZING = 0.02;
const float MOST_SPARKLE = 64.0;
const float RIBBON_GLINT_CHANCE = 0.01;
const float RIBBON_GLINT_SIGMAS = 2.326;
const float RIBBON_GLINT_MARGIN = 0.1;
const int DEBUG_CONSTANT_INTEGRAL = 1;
const int DEBUG_NO_GLITTER = 2;
const int DEBUG_NO_SCENE_DEPTH = 3;
const int DEBUG_RIBBONS_ONLY = 4;
// The cell atlas holds both ribbons of SLOTS_ACROSS slots side by side in each band of atlasRows rows, the plates first, as LightPillarCells:
const int SLOTS_ACROSS = 16;
const int PLATE_HALF_COLUMNS = 16;
const int WOBBLING_HALF_COLUMNS = 64;
const int PAIR_COLUMNS = 2 * PLATE_HALF_COLUMNS + 2 * WOBBLING_HALF_COLUMNS + 2;
const int ATLAS_COLUMNS = SLOTS_ACROSS * PAIR_COLUMNS;
const int SLOT_BANDS = 48 / SLOTS_ACROSS;

int atlasHalfColumns(int kind) {
    return kind == 0 ? PLATE_HALF_COLUMNS : WOBBLING_HALF_COLUMNS;
}

// The atlas texel of the ribbon's cell on the pillar's axis in its bottom row, as LightPillarCells.axisColumn and bottomRow:
ivec2 atlasAxis(int ribbon) {
    int slot = ribbon / 2;
    int kind = ribbon - 2 * slot;
    int column = (slot % SLOTS_ACROSS) * PAIR_COLUMNS + (kind == 0 ? 0 : 2 * PLATE_HALF_COLUMNS + 1)
            + atlasHalfColumns(kind);
    return ivec2(column, (slot / SLOTS_ACROSS) * atlasRows);
}

// As IceMirror.slabReflectance. Each polarization reflects r / (1 + r) with r = ((a - b) / (a + b))^2, which is (a - b)^2 / (2 a^2 + 2 b^2):
float slabReflectance(float sineOfGrazingAngle, float inverseSquaredIndex) {
    float cosineOfIncidence = clamp(sineOfGrazingAngle, 0.0, 1.0);
    float squaredSineOfIncidence = 1.0 - cosineOfIncidence * cosineOfIncidence;
    float cosineOfRefraction = sqrt(1.0 - squaredSineOfIncidence * inverseSquaredIndex);
    float perpendicularFar = refractiveIndex * cosineOfRefraction;
    float parallelNear = refractiveIndex * cosineOfIncidence;
    float perpendicularDifference = cosineOfIncidence - perpendicularFar;
    float parallelDifference = parallelNear - cosineOfRefraction;
    float perpendicularSquares = cosineOfIncidence * cosineOfIncidence + perpendicularFar * perpendicularFar;
    float parallelSquares = parallelNear * parallelNear + cosineOfRefraction * cosineOfRefraction;
    return 0.5 * (perpendicularDifference * perpendicularDifference * parallelSquares
            + parallelDifference * parallelDifference * perpendicularSquares) / (perpendicularSquares * parallelSquares);
}

float dustColumn(float height) {
    float above = height - dustBottomHeight;
    return above <= 0.0 ? above : scaleHeight * (1.0 - exp(-above / scaleHeight));
}

// The dust density at a height less than SMALLEST_RISE from a crystal, from the density at the crystal, which is linear there to well within float precision:
float densityNearCrystal(float crystalDensity, float crystalAboveBottom, float aboveBottom, float inverseScaleHeight) {
    return crystalDensity * (1.0 - (max(0.0, aboveBottom) - max(0.0, crystalAboveBottom)) * inverseScaleHeight);
}

// The tilt of the crystals on the view ray at the distance from the eye. The distance falls as the tilt rises:
float tiltAtDistance(float elevation, float lightElevation, float lightDistance, float lightToRay, float distance) {
    return elevation - 0.5 * atan(lightToRay, lightDistance * cos(elevation - lightElevation) - distance);
}

// Returns the reflections for the tilts from the lowest to the highest, adds their crystals to the crystal sum, and widens the widest spread across the pillar to theirs:
float tiltSum(float lowestTilt, float highestTilt, float elevation, float lateralAngle, float lightHeight,
        float lightElevation, float lightDistance, float lightToRay, float tiltSigma, inout float widestSpread,
        inout float crystalSum) {
    if (!(highestTilt > lowestTilt)) {
        return 0.0;
    }
    float crystals = 0.0;
    float middleTilt = 0.5 * (highestTilt + lowestTilt);
    float halfRange = 0.5 * (highestTilt - lowestTilt);
    // The sines at each point come from the sine and cosine of its tilt by the angle sum formulas:
    float sineOfElevation = sin(elevation);
    float cosineOfElevation = cos(elevation);
    float sineOfDoubleElevation = 2.0 * sineOfElevation * cosineOfElevation;
    float cosineOfDoubleElevation = cosineOfElevation * cosineOfElevation - sineOfElevation * sineOfElevation;
    float sineAboveLight = sin(elevation + lightElevation);
    float cosineAboveLight = cos(elevation + lightElevation);
    float legRatioScale = lightDistance / lightToRay;
    // The eye's leg times its mean density is the dust column it rises through over the sine of the elevation:
    float inverseSineOfElevation = 1.0 / sineOfElevation;
    float tiltVariance = tiltSigma * tiltSigma;
    float halfInverseTiltVariance = 0.5 / tiltVariance;
    float normalization = INVERSE_SQUARE_ROOT_OF_TWO_PI * INVERSE_SQUARE_ROOT_OF_TWO_PI / tiltSigma;
    float blurVariance = lateralBlur * lateralBlur;
    float inverseSquaredIndex = 1.0 / (refractiveIndex * refractiveIndex);
    float inverseScaleHeight = 1.0 / scaleHeight;
    float lightColumn = dustColumn(lightHeight);
    float eyeColumn = dustColumn(0.0);
    // The near weight is the smoothstep of nearShare, which stays 1 without a near fade:
    float inverseNearRange = nearFade > 0.0 ? 1.0 / ((1.0 - NEAR_FADE_START_SHARE) * nearFade) : 0.0;
    float nearOffset = nearFade > 0.0 ? -NEAR_FADE_START_SHARE / (1.0 - NEAR_FADE_START_SHARE) : 1.0;
    bool countsCrystals = crystalCountScale > 0.0;
#ifdef FRAGMENT_SHADER
    countsCrystals = countsCrystals && debugMode != DEBUG_NO_GLITTER;
#endif
    float sum = 0.0;
    for (int node = 0; node < NODE_COUNT; node++) {
        float alongTilt = middleTilt + halfRange * NODES[node];
        float sineOfTilt = sin(alongTilt);
        float cosineOfTilt = cos(alongTilt);
        float sineOfDoubleTilt = 2.0 * sineOfTilt * cosineOfTilt;
        float cosineOfDoubleTilt = cosineOfTilt * cosineOfTilt - sineOfTilt * sineOfTilt;
        // The light depression is the elevation minus twice the tilt:
        float sineOfLightDepression = sineOfElevation * cosineOfDoubleTilt - cosineOfElevation * sineOfDoubleTilt;
        float sineAtCrystal = max(sineOfDoubleElevation * cosineOfDoubleTilt
                - cosineOfDoubleElevation * sineOfDoubleTilt, SMALLEST_SINE_AT_CRYSTAL);
        float sineOfDepressionAboveLight = sineAboveLight * cosineOfDoubleTilt - cosineAboveLight * sineOfDoubleTilt;
        float inverseSineAtCrystal = 1.0 / sineAtCrystal;
        float eyeLeg = lightDistance * sineOfDepressionAboveLight * inverseSineAtCrystal;
        float lightLeg = lightToRay * inverseSineAtCrystal;
        float crystalHeight = eyeLeg * sineOfElevation;
        float acrossScale = (1.0 + legRatioScale * sineOfDepressionAboveLight)
                / (sineOfLightDepression + sineOfElevation);
        float acrossVariance = tiltVariance + blurVariance * acrossScale * acrossScale;
        float inverseAcrossSpread = inversesqrt(acrossVariance);
#ifdef VERTEX_SHADER
        widestSpread = max(widestSpread, 1.0 / (inverseAcrossSpread * acrossScale));
#endif
        float crystalAboveBottom = crystalHeight - dustBottomHeight;
        float crystalDensity = exp(-max(0.0, crystalAboveBottom) * inverseScaleHeight);
        float crystalColumn = crystalAboveBottom <= 0.0 ? crystalAboveBottom : scaleHeight * (1.0 - crystalDensity);
        float lightRise = crystalHeight - lightHeight;
        float lightMeanDensity = abs(lightRise) > SMALLEST_RISE ? (crystalColumn - lightColumn) / lightRise
                : densityNearCrystal(crystalDensity, crystalAboveBottom,
                        0.5 * (lightHeight + crystalHeight) - dustBottomHeight, inverseScaleHeight);
        float eyeDepth = abs(crystalHeight) > SMALLEST_RISE ? (crystalColumn - eyeColumn) * inverseSineOfElevation
                : eyeLeg * densityNearCrystal(crystalDensity, crystalAboveBottom,
                        0.5 * crystalHeight - dustBottomHeight, inverseScaleHeight);
        float opticalDepth = extinction * (lightLeg * lightMeanDensity + eyeDepth);
        float sineOfGrazing = sineOfElevation * cosineOfTilt - cosineOfElevation * sineOfTilt;
        float acrossOffset = lateralAngle * acrossScale * inverseAcrossSpread;
        float exponent = -alongTilt * alongTilt * halfInverseTiltVariance - 0.5 * acrossOffset * acrossOffset;
        float nearShare = clamp(eyeLeg * inverseNearRange + nearOffset, 0.0, 1.0);
        float weight = WEIGHTS[node] * normalization * inverseAcrossSpread * crystalDensity
                * nearShare * nearShare * (3.0 - 2.0 * nearShare);
        sum += weight * exp(exponent - opticalDepth) * slabReflectance(sineOfGrazing, inverseSquaredIndex);
        if (countsCrystals) {
            float legsOverSpread = eyeLeg * lightLeg / max(sineOfGrazing, SMALLEST_SINE_OF_GRAZING);
            crystals += weight * exp(exponent) * legsOverSpread * legsOverSpread;
        }
    }
    crystalSum += crystals * halfRange;
    return sum * halfRange;
}

// The most that the crystals of a sky cell show, as a multiple of their mean value, as LightPillarOptics.sparkleCap:
float sparkleCap(float meanCount) {
    return meanCount > 0.0 ? min(MOST_SPARKLE, 1.0 + 3.0 / sqrt(meanCount) + 1.0 / meanCount) : MOST_SPARKLE;
}

// Returns the radiance per unit intensity and unit face area per volume at the dust bottom from the crystals nearer than the scene distance whose light gets past the blocks around the light, and writes the widest spread across the pillar.
// Also writes the crystals that mirror the light per unit face area per volume and unit crystalCountScale, as LightPillarOptics.dustCrystalCount.
// The light stands at the height above the eye, seen at the light elevation, at the light distance from the eye.
// The horizons are the horizon, the ceiling, and the top of LightPillarHorizons:
float pillarRadiance(float elevation, float lateralAngle, float lightHeight, float lightElevation, float lightDistance,
        float tiltSigma, float sceneDistance, vec3 horizons, out float widestSpread, out float crystalCount) {
    widestSpread = 0.0;
    crystalCount = 0.0;
    float sineOfOffset = sin(elevation - lightElevation);
    if (!(sineOfOffset > 0.0)) {
        return 0.0;
    }
    float lightToRay = lightDistance * sineOfOffset;
    float lowestTilt = max(-TILT_RANGE_SIGMAS * tiltSigma,
            tiltAtDistance(elevation, lightElevation, lightDistance, lightToRay, sceneDistance));
    float highestTilt = min(TILT_RANGE_SIGMAS * tiltSigma, 0.5 * (elevation + lightElevation));
    if (nearFade > 0.0) {
        highestTilt = min(highestTilt, tiltAtDistance(elevation, lightElevation, lightDistance, lightToRay,
                NEAR_FADE_START_SHARE * nearFade));
    }
    // The light leaves the light rising at the elevation minus twice the tilt:
    float crystalSum = 0.0;
    float sum = tiltSum(lowestTilt, min(highestTilt, 0.5 * (elevation - horizons.z)), elevation, lateralAngle,
            lightHeight, lightElevation, lightDistance, lightToRay, tiltSigma, widestSpread, crystalSum)
            + tiltSum(max(lowestTilt, 0.5 * (elevation - horizons.y)), min(highestTilt, 0.5 * (elevation - horizons.x)),
            elevation, lateralAngle, lightHeight, lightElevation, lightDistance, lightToRay, tiltSigma, widestSpread,
            crystalSum);
    crystalCount = 2.0 * crystalSum / lightToRay;
    return sum / (2.0 * lightToRay);
}

#ifdef VERTEX_SHADER
// The ribbon index is slot * 2 + plate kind, then the row from the bottom and the side, -1 or 1, doubled at the top of each quad:
in vec3 Position;
out vec3 relativePosition;
flat out float pillarCellOrigin;
flat out vec3 pillarLight;
flat out float pillarLightElevation;
flat out float pillarLightDistance;
flat out vec3 pillarColor;
flat out float pillarOpacity;
flat out float pillarTiltSigma;
flat out float pillarShare;
flat out vec3 pillarHorizons;
flat out float pillarRibbon;

const float SEGMENTS = 16.0;
// Ribbons only cover the pillar on the screen, so they all stand at this distance from the eye:
const float RIBBON_DISTANCE = 1.0;
const float NEAREST_HORIZONTAL_DISTANCE = 0.5;
const float WIDEST_RIBBON_ANGLE = 1.4;

// Rows are closer together near the bottom of the ribbon:
float rowElevation(float row, float firstRow) {
    float rowFraction = row / SEGMENTS;
    return firstRow + (highestElevation - firstRow) * rowFraction * rowFraction;
}

// The mean count of crystals per sky cell below which the cell shows the glint value with less than RIBBON_GLINT_CHANCE, as LightPillarOptics.leastVisibleCount:
float leastVisibleCount(float crystalOverGlint) {
    float ratio = 1.0 / ((1.0 + RIBBON_GLINT_MARGIN) * crystalOverGlint);
    float needed = max(1.0, ceil(ratio));
    float chanceCount;
    if (needed <= 2.0) {
        chanceCount = pow(RIBBON_GLINT_CHANCE * needed, 1.0 / needed);
    } else {
        float root = max(0.0, 1.0 - 1.0 / (9.0 * needed) - RIBBON_GLINT_SIGMAS / (3.0 * sqrt(needed)));
        chanceCount = needed * root * root * root;
    }
    float capRoot = 0.5 * (sqrt(4.0 * ratio + 5.0) - 3.0);
    float capCount = ratio > 1.0 ? capRoot * capRoot : 0.0;
    return max(chanceCount, max(capCount, ratio / MOST_SPARKLE));
}

// Returns the angle off the center beyond which the pillar stays below the visible value, plus the margin.
// Where crystals glitter, beyond it a pixel also shows the glint value with less than RIBBON_GLINT_CHANCE, as LightPillarOptics.ribbonHalfWidth:
float halfWidthAt(float elevation, float lightHeight, float lightElevation, float lightDistance, float tiltSigma,
        vec3 horizons, float valueScale, float countScale) {
    float widestSpread;
    float crystalCount;
    float center = valueScale * pillarRadiance(elevation, 0.0, lightHeight, lightElevation, lightDistance, tiltSigma,
            NO_SCENE_DISTANCE, horizons, widestSpread, crystalCount);
    float meanCount = countScale * crystalCount;
    bool shows = false;
    float spreads = 0.0;
    if (center > visibleValue) {
        shows = true;
        spreads = sqrt(2.0 * log(center / visibleValue));
    }
    if (meanCount > 0.0) {
        float leastCount = leastVisibleCount(center / (meanCount * glintValue));
        if (meanCount > leastCount) {
            shows = true;
            spreads = max(spreads, sqrt(2.0 * log(meanCount / leastCount)));
        }
    }
    if (!shows) {
        return 0.0;
    }
    return min(min(MOST_RIBBON_SPREADS, spreads) * widestSpread + marginAngle, WIDEST_RIBBON_ANGLE);
}

void main() {
    int ribbon = int(Position.x + 0.5);
    int slot = ribbon / 2;
    int kind = ribbon - 2 * slot;
    vec3 light = lights[slot].xyz;
    vec3 horizons = vec3(lights[slot].w, overhangs[slot]);
    vec4 color = colors[slot];
    float tiltSigma = tiltSigmas[kind];
    float share = shares[kind];
    pillarLight = light;
    pillarLightElevation = 0.0;
    pillarLightDistance = 0.0;
    pillarColor = color.rgb;
    pillarOpacity = color.a;
    pillarTiltSigma = tiltSigma;
    pillarShare = share;
    pillarHorizons = horizons;
    pillarRibbon = float(ribbon);
    pillarCellOrigin = 0.0;
    relativePosition = vec3(0.0);
    gl_Position = vec4(0.0, 0.0, 2.0, 1.0);

    float horizontalDistance = length(light.xz);
    float valueScale = max(color.r, max(color.g, color.b)) * color.a * share * extinction;
    float countScale = share * extinction * crystalCountScale;
    if (!(valueScale > 0.0) || horizontalDistance < NEAREST_HORIZONTAL_DISTANCE) {
        return;
    }
    float lightElevation = atan(light.y, horizontalDistance);
    float lightDistance = length(light);
    pillarLightElevation = lightElevation;
    pillarLightDistance = lightDistance;
    // The cells stand on the lowest elevation that the most tilted faces mirror the light to without blocks:
    float cellOrigin = max(lightElevation, -lightElevation - 2.0 * TILT_RANGE_SIGMAS * tiltSigma);
    pillarCellOrigin = cellOrigin;
    // Below the horizon minus the most tilted faces, no crystal gets light past the blocks around the light:
    float lowest = max(cellOrigin, horizons.x - 2.0 * TILT_RANGE_SIGMAS * tiltSigma);
    if (!(highestElevation > lowest)) {
        return;
    }
    float firstRow = lowest - marginAngle;
    float lowestEvaluated = min(lowest + 1.0e-3, highestElevation);
    float elevation = rowElevation(Position.y, firstRow);
    // Each row is also as wide as the rows next to it and the half rows between, so the straight edges between rows
    // hold the pillar:
    float halfWidth = 0.0;
    for (float row = max(Position.y - 1.0, 0.0); row <= min(Position.y + 1.0, SEGMENTS); row += 0.5) {
        float rowEvaluated = clamp(rowElevation(row, firstRow), lowestEvaluated, highestElevation);
        halfWidth = max(halfWidth, halfWidthAt(rowEvaluated, light.y, lightElevation, lightDistance, tiltSigma,
                horizons, valueScale, countScale));
    }
    // The pillar can show again above the top in a band narrower than a half row:
    float reappearance = horizons.z + 2.0 * tiltSigma;
    if (horizons.y < horizons.z && reappearance > rowElevation(max(Position.y - 1.0, 0.0), firstRow)
            && reappearance < rowElevation(min(Position.y + 1.0, SEGMENTS), firstRow)) {
        halfWidth = max(halfWidth, halfWidthAt(clamp(reappearance, lowestEvaluated, highestElevation), light.y,
                lightElevation, lightDistance, tiltSigma, horizons, valueScale, countScale));
    }

    float side = sign(Position.z);
#ifdef CELL_ATLAS
    // The sides of the ribbon's quads on the screen lie nearer the horizon than their middles. Each quad here stands on
    // the lower of the two, so its sides follow the screen quad's sides and the next quad covers the middle above them.
    // The outline of the ribbon reaches one cell further:
    float atlasHalfWidth = halfWidth > 0.0 ? halfWidth + cellAngle : 0.0;
    float sideElevation = asin(sin(elevation) * cos(halfWidth));
    float atlasElevation = min(elevation, sideElevation);
    if (abs(Position.z) > 1.5 && Position.y > SEGMENTS - 0.5) {
        atlasElevation = max(elevation, sideElevation) + cellAngle;
    } else if (abs(Position.z) < 1.5 && Position.y < 0.5) {
        atlasElevation -= cellAngle;
    }
    vec2 texel = vec2(atlasAxis(ribbon))
            + vec2(0.5 + side * atlasHalfWidth / cellAngle, (atlasElevation - cellOrigin) / cellAngle);
    gl_Position = vec4(2.0 * texel / vec2(float(ATLAS_COLUMNS), float(SLOT_BANDS * atlasRows)) - 1.0, 0.0, 1.0);
#else
    vec2 toward = light.xz / horizontalDistance;
    vec3 along = vec3(toward.x * cos(elevation), sin(elevation), toward.y * cos(elevation));
    vec3 across = vec3(-toward.y, 0.0, toward.x);
    vec3 position = RIBBON_DISTANCE * (along + side * tan(halfWidth) * across);
    relativePosition = position;
    gl_Position = viewProjection * vec4(position, 1.0);
#endif
}
#endif

#ifdef FRAGMENT_SHADER
// The depth of the scene drawn so far, read at each pixel:
uniform sampler2D sceneDepth;
// Takes clip positions back to positions relative to the eye:
uniform mat4 inverseViewProjection;
uniform vec2 screenSize;
// Twinkle periods passed, which also run on as the eye moves:
uniform float twinkleClock;
// Above 0, every pixel a ribbon covers shows FILL_SHOWN where the pillar adds to the display and FILL_WASTED elsewhere, and FILL_COMPUTED where its cell lies beyond the atlas:
uniform float fillView;
// The value of each cell, as cellValue gives it, which the atlas pass writes and the pixels of the cells read:
uniform sampler2D cellAtlas;
in vec3 relativePosition;
flat in float pillarCellOrigin;
flat in vec3 pillarLight;
flat in float pillarLightElevation;
flat in float pillarLightDistance;
flat in vec3 pillarColor;
flat in float pillarOpacity;
flat in float pillarTiltSigma;
flat in float pillarShare;
flat in vec3 pillarHorizons;
flat in float pillarRibbon;
out vec4 fragColor;

const float LARGEST_COUNTED_MEAN = 3.0;
const float MOST_DRAWN = 48.0;
const uint GOLDEN_RATIO_STEP = 0x9e3779b9u;
const vec3 FILL_SHOWN = vec3(0.0, 0.25, 0.0);
const vec3 FILL_WASTED = vec3(0.25, 0.0, 0.0);
const vec3 FILL_COMPUTED = vec3(0.0, 0.0, 0.25);
const vec3 DEBUG_SHADE = vec3(0.1);
const float DEBUG_CELL_VALUE = 0.1;
// Half a step of 255, below which the display shows nothing:
const float SHOWN_STEP = 0.5 / 255.0;

// The lowbias32 integer hash by Chris Wellons:
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

// Draws a count of crystals for their mean count above LARGEST_COUNTED_MEAN, as LightPillarOptics.crystalsDrawn:
float crystalsDrawn(float meanCount, uint seed) {
    float chance = uniformOf(seed);
    float angle = 2.0 * PI * uniformOf(hashOf(seed));
    float deviate = sqrt(-2.0 * log(max(chance, 1.0e-7))) * cos(angle);
    float root = sqrt(meanCount);
    float quantile = meanCount + root * deviate + (deviate * deviate - 1.0) / 6.0
            + deviate * (1.0 - deviate * deviate) / (72.0 * root);
    return max(0.0, floor(quantile + 0.5));
}

// Draws the counts of crystals for two chances at the same mean count up to LARGEST_COUNTED_MEAN in one pass, each as LightPillarOptics.crystalsDrawn:
vec2 crystalsCounted(float meanCount, vec2 chances) {
    float lowerChance = min(chances.x, chances.y);
    float upperChance = max(chances.x, chances.y);
    float term = exp(-meanCount);
    float cumulative = term;
    float count = 0.0;
    float lowerCount = lowerChance > cumulative ? MOST_DRAWN : 0.0;
    while (upperChance > cumulative && count < MOST_DRAWN) {
        count += 1.0;
        term *= meanCount / count;
        cumulative += term;
        if (lowerChance <= cumulative) {
            lowerCount = min(lowerCount, count);
        }
    }
    return chances.x <= chances.y ? vec2(lowerCount, count) : vec2(count, lowerCount);
}

// Returns the share of its mean value that the pillar shows in the sky cell, a pixel along the lateral angle and the elevation or a cell of the grid, as LightPillarOptics.sparkle:
float sparkle(float meanCount, ivec2 cell) {
    if (!(meanCount > 0.0)) {
        return 1.0;
    }
    uint cellSeed = hashOf(uint(cell.x) ^ hashOf(uint(cell.y) ^ hashOf(uint(pillarRibbon))));
    float time = twinkleClock + uniformOf(cellSeed);
    float frame = floor(time);
    uint frameNumber = uint(frame);
    float cap = sparkleCap(meanCount);
    uint beforeSeed = hashOf(cellSeed + frameNumber * GOLDEN_RATIO_STEP);
    uint afterSeed = hashOf(cellSeed + (frameNumber + 1u) * GOLDEN_RATIO_STEP);
    vec2 counts;
    if (meanCount > LARGEST_COUNTED_MEAN) {
        counts = vec2(crystalsDrawn(meanCount, beforeSeed), crystalsDrawn(meanCount, afterSeed));
    } else {
        counts = crystalsCounted(meanCount, vec2(uniformOf(beforeSeed), uniformOf(afterSeed)));
    }
    vec2 shares = min(counts / meanCount, vec2(cap));
    return mix(shares.x, shares.y, smoothstep(0.25, 0.75, time - frame));
}

vec3 encodeSrgb(vec3 linear) {
    vec3 clamped = clamp(linear, 0.0, 1.0);
    return mix(clamped * 12.92, 1.055 * pow(clamped, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, clamped));
}

float brightestChannel() {
    return max(pillarColor.r, max(pillarColor.g, pillarColor.b));
}

// The radiance of the pillar at the center of the cell with the cell's glitter and without a scene, times the brightest channel of its color, which keeps it within half float range:
float cellValue(ivec2 cell) {
    if (debugMode == DEBUG_CONSTANT_INTEGRAL) {
        return DEBUG_CELL_VALUE;
    }
    float elevation = pillarCellOrigin + (float(cell.y) + 0.5) * cellAngle;
    float widestSpread;
    float crystalCount;
    float radiance = pillarShare * extinction * pillarRadiance(elevation, float(cell.x) * cellAngle, pillarLight.y,
            pillarLightElevation, pillarLightDistance, pillarTiltSigma, NO_SCENE_DISTANCE, pillarHorizons, widestSpread,
            crystalCount);
    if (crystalCountScale > 0.0 && debugMode != DEBUG_NO_GLITTER) {
        radiance *= sparkle(pillarShare * extinction * crystalCountScale * crystalCount, cell);
    }
    return radiance * brightestChannel();
}

// The standard normal distribution function from a logistic curve, within about 2e-4, as LightPillarOptics.normalShare:
float normalShare(float deviation) {
    return 1.0 / (1.0 + exp(-1.5976 * deviation * (1.0 + 0.04417 * deviation * deviation)));
}

float tiltMass(float lowestTilt, float highestTilt, float inverseTiltSigma) {
    if (!(highestTilt > lowestTilt)) {
        return 0.0;
    }
    return normalShare(highestTilt * inverseTiltSigma) - normalShare(lowestTilt * inverseTiltSigma);
}

// The mass of the tilt Gaussian from the lowest to the highest tilt, weighed by the near weight, which falls from 1 at the full tilt to 0 at the start tilt, here linearly in the tilt, as LightPillarOptics.nearTiltMass:
float nearTiltMass(float lowestTilt, float highestTilt, float fullTilt, float startTilt, float tiltSigma) {
    float inverseTiltSigma = 1.0 / tiltSigma;
    float mass = tiltMass(lowestTilt, min(highestTilt, fullTilt), inverseTiltSigma);
    float rampLowest = max(lowestTilt, fullTilt);
    float rampHighest = min(highestTilt, startTilt);
    if (rampHighest > rampLowest) {
        // The integral of the start tilt less the tilt, times the Gaussian:
        float rampMass = tiltMass(rampLowest, rampHighest, inverseTiltSigma);
        float weighed = startTilt * rampMass + tiltSigma * INVERSE_SQUARE_ROOT_OF_TWO_PI
                * (exp(-0.5 * rampHighest * rampHighest * inverseTiltSigma * inverseTiltSigma)
                - exp(-0.5 * rampLowest * rampLowest * inverseTiltSigma * inverseTiltSigma));
        mass += clamp(weighed / (startTilt - fullTilt), 0.0, rampMass);
    }
    return mass;
}

// The share of the pillar's light at the elevation that comes from crystals nearer than the scene distance, with the tilts that pillarRadiance sums weighed by their Gaussian and the near weight alone, as LightPillarOptics.sceneShare:
float sceneShare(float elevation, float sceneDistance) {
    if (!(sceneDistance < NO_SCENE_DISTANCE)) {
        return 1.0;
    }
    float sineOfOffset = sin(elevation - pillarLightElevation);
    if (!(sineOfOffset > 0.0)) {
        return 0.0;
    }
    float lightToRay = pillarLightDistance * sineOfOffset;
    float lowestTilt = max(-TILT_RANGE_SIGMAS * pillarTiltSigma, elevation - 0.5 * PI);
    float cutTilt = tiltAtDistance(elevation, pillarLightElevation, pillarLightDistance, lightToRay, sceneDistance);
    if (!(cutTilt > lowestTilt)) {
        return 1.0;
    }
    float highestTilt = min(TILT_RANGE_SIGMAS * pillarTiltSigma, 0.5 * (elevation + pillarLightElevation));
    float fullTilt = highestTilt;
    float startTilt = highestTilt;
    if (nearFade > 0.0) {
        fullTilt = tiltAtDistance(elevation, pillarLightElevation, pillarLightDistance, lightToRay, nearFade);
        startTilt = tiltAtDistance(elevation, pillarLightElevation, pillarLightDistance, lightToRay,
                NEAR_FADE_START_SHARE * nearFade);
        highestTilt = min(highestTilt, startTilt);
    }
    float aboveTop = min(highestTilt, 0.5 * (elevation - pillarHorizons.z));
    float belowCeiling = max(lowestTilt, 0.5 * (elevation - pillarHorizons.y));
    float aboveHorizon = min(highestTilt, 0.5 * (elevation - pillarHorizons.x));
    float whole = nearTiltMass(lowestTilt, aboveTop, fullTilt, startTilt, pillarTiltSigma)
            + nearTiltMass(belowCeiling, aboveHorizon, fullTilt, startTilt, pillarTiltSigma);
    if (!(whole > 0.0)) {
        return 0.0;
    }
    float nearer = nearTiltMass(cutTilt, aboveTop, fullTilt, startTilt, pillarTiltSigma)
            + nearTiltMass(max(belowCeiling, cutTilt), aboveHorizon, fullTilt, startTilt, pillarTiltSigma);
    return clamp(nearer / whole, 0.0, 1.0);
}

#ifdef CELL_ATLAS
// Each texel of a ribbon's part of the atlas holds the value of one cell, and the parts of other ribbons are left alone:
void main() {
    int ribbon = int(pillarRibbon + 0.5);
    ivec2 cell = ivec2(gl_FragCoord.xy) - atlasAxis(ribbon);
    if (abs(cell.x) > atlasHalfColumns(ribbon - 2 * (ribbon / 2)) || cell.y < 0 || cell.y >= atlasRows) {
        discard;
    }
    fragColor = vec4(cellValue(cell), 0.0, 0.0, 1.0);
}
#else
void main() {
    if (debugMode == DEBUG_RIBBONS_ONLY) {
        fragColor = vec4(DEBUG_SHADE * pillarOpacity, 1.0);
        return;
    }
    vec3 direction = normalize(relativePosition);
    float horizontalDistance = length(pillarLight.xz);
    vec2 toward = pillarLight.xz / horizontalDistance;
    if (dot(direction.xz, toward) <= 0.0) {
        if (fillView > 0.0) {
            fragColor = vec4(FILL_WASTED, 1.0);
            return;
        }
        discard;
    }
    float sceneDistance = NO_SCENE_DISTANCE;
    if (debugMode != DEBUG_NO_SCENE_DEPTH) {
        float depth = texelFetch(sceneDepth, ivec2(gl_FragCoord.xy), 0).r;
        if (depth < 1.0) {
            vec4 scene = inverseViewProjection
                    * vec4(gl_FragCoord.xy / screenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
            sceneDistance = length(scene.xyz / scene.w);
        }
    }
    float elevation = asin(clamp(direction.y, -1.0, 1.0));
    float lateralAngle = asin(clamp(dot(direction.xz, vec2(-toward.y, toward.x)), -1.0, 1.0));
    if (cellAngle > 0.0) {
        // The middle column of cells lies on the pillar's axis:
        ivec2 cell = ivec2(int(floor(lateralAngle / cellAngle + 0.5)),
                int(floor((elevation - pillarCellOrigin) / cellAngle)));
        int ribbon = int(pillarRibbon + 0.5);
        bool inAtlas = abs(cell.x) <= atlasHalfColumns(ribbon - 2 * (ribbon / 2)) && cell.y < atlasRows;
        float value = 0.0;
        if (cell.y >= 0) {
            if (inAtlas) {
                value = texelFetch(cellAtlas, atlasAxis(ribbon) + cell, 0).r;
            } else {
                value = cellValue(cell);
            }
            value *= sceneShare(pillarCellOrigin + (float(cell.y) + 0.5) * cellAngle, sceneDistance);
        }
        vec3 linear = pillarColor * (value / brightestChannel());
        if (fillView > 0.0) {
            vec3 shown = encodeSrgb(1.0 - exp(-linear)) * pillarOpacity;
            vec3 fill = max(shown.r, max(shown.g, shown.b)) >= SHOWN_STEP ? FILL_SHOWN : FILL_WASTED;
            fragColor = vec4(inAtlas || cell.y < 0 ? fill : FILL_COMPUTED, 1.0);
            return;
        }
        if (!(value > 0.0)) {
            discard;
        }
        fragColor = vec4(encodeSrgb(1.0 - exp(-linear)) * pillarOpacity, 1.0);
        return;
    }
    // The scene distance still decides the shade, so the depth read stays:
    if (debugMode == DEBUG_CONSTANT_INTEGRAL) {
        if (!(sceneDistance > pillarLightDistance)) {
            discard;
        }
        fragColor = vec4(DEBUG_SHADE * pillarOpacity, 1.0);
        return;
    }
    float widestSpread;
    float crystalCount;
    float radiance = pillarShare * extinction * pillarRadiance(elevation, lateralAngle, pillarLight.y,
            pillarLightElevation, pillarLightDistance, pillarTiltSigma, sceneDistance, pillarHorizons, widestSpread,
            crystalCount);
    if (crystalCountScale > 0.0 && debugMode != DEBUG_NO_GLITTER) {
        radiance *= sparkle(pillarShare * extinction * crystalCountScale * crystalCount,
                ivec2(floor(vec2(lateralAngle, elevation) / marginAngle)));
    }
    if (fillView > 0.0) {
        vec3 shown = encodeSrgb(1.0 - exp(-pillarColor * radiance)) * pillarOpacity;
        fragColor = vec4(max(shown.r, max(shown.g, shown.b)) >= SHOWN_STEP ? FILL_SHOWN : FILL_WASTED, 1.0);
        return;
    }
    if (!(radiance > 0.0)) {
        discard;
    }
    fragColor = vec4(encodeSrgb(1.0 - exp(-pillarColor * radiance)) * pillarOpacity, 1.0);
}
#endif
#endif
