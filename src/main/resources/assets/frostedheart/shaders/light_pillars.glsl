// FHShaders prepends the version line and defines VERTEX_SHADER or FRAGMENT_SHADER, and CELL_ATLAS for the pass that fills the cell atlas.
// Light pillars from the closed form of the pillar of level plates in calculated-sky.md, as LightPillarOptics.closedFormRadiance and closedFormCrystalCount.
// Each pillar has one ribbon per plate kind that covers it on the screen, and shows above the rim's angle and below the cap of LightPillarShelter on a vertical sheet through the light.
// The fragment shader hides the pillar where the scene stands in front of that sheet.
// With cells, the atlas pass computes each cell of each ribbon once without the scene, and the fragment shader reads its cell.
// Takes positions relative to the eye, which view bobbing moves away from the camera:
uniform mat4 viewProjection;
// Light positions relative to the eye, then 0:
uniform vec4 lights[48];
// The rim of LightPillarShelter toward the eye as an angle in radians, and its cap relative to the eye with a large stand in where it is open:
uniform vec2 shelters[48];
// The half width of each light's cluster across the view in blocks, and the count of its lights:
uniform vec2 spans[48];
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
// Pillars fade in over this many tilt sigmas above their lowest elevation, from the base share of their light up:
uniform float fadeInSigmas;
uniform float baseShare;
// Crystals nearer to the eye than this many blocks show less, none from a quarter of it in, and all where it is 0:
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
// The pillar fades out over this many blocks of the sheet's height below the cap:
const float SHELTER_BAND = 0.5;
// The cells stand on the lowest elevation that faces tilted by this many sigmas would mirror the light to, as LightPillarCells.cellOrigin:
const float CELL_ORIGIN_SIGMAS = 6.0;
// Below this ratio of a light's half angle to its spread, the pillar takes a Gaussian as wide as its smeared profile, as LightPillarOptics.SMEAR_SHARE:
const float SMEAR_SHARE = 0.5;
const float NEAR_FADE_START_SHARE = 0.25;
const float MOST_RIBBON_SPREADS = 8.0;
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

// The share of the pillar's light at an elevation above its lowest elevation where it fades in, as LightPillarOptics.fadeIn:
float fadeIn(float elevation, float lowestElevation, float tiltSigma) {
    if (!(fadeInSigmas * tiltSigma > 0.0)) {
        return 1.0;
    }
    float ramp = smoothstep(lowestElevation, lowestElevation + fadeInSigmas * tiltSigma, elevation);
    return ramp * mix(baseShare, 1.0, ramp);
}

// The share of the pillar's light that shows below the cap at the height of the sheet above the eye, as LightPillarOptics.capShare:
float capShare(float sheetHeight, float cap) {
    return 1.0 - smoothstep(cap - SHELTER_BAND, cap, sheetHeight);
}

// The share of the pillar's light that crystals at the distance from the eye show, as LightPillarOptics.nearWeight:
float nearWeight(float eyeLeg) {
    if (!(nearFade > 0.0)) {
        return 1.0;
    }
    return smoothstep(NEAR_FADE_START_SHARE * nearFade, nearFade, eyeLeg);
}

// The chance that a standard normal deviate lies above the deviation, from the complementary error function of Abramowitz and Stegun 7.1.26, as LightPillarOptics.upperNormalTail:
float upperNormalTail(float deviation) {
    float scaled = abs(deviation) * 0.7071067811865476;
    float reciprocal = 1.0 / (1.0 + 0.3275911 * scaled);
    float polynomial = reciprocal * (0.254829592 + reciprocal * (-0.284496736 + reciprocal * (1.421413741
            + reciprocal * (-1.453152027 + reciprocal * 1.061405429))));
    float tail = 0.5 * polynomial * exp(-scaled * scaled);
    return deviation >= 0.0 ? tail : 1.0 - tail;
}

// The share of the pillar's light per radian across it, the Gaussian of the spread smeared over the half angle of the light's width on each side, as LightPillarOptics.lateralProfile:
float lateralProfile(float lateralAngle, float spread, float halfAngle) {
    if (halfAngle <= SMEAR_SHARE * spread) {
        float widened = sqrt(spread * spread + halfAngle * halfAngle / 3.0);
        float acrossOffset = lateralAngle / widened;
        return exp(-0.5 * acrossOffset * acrossOffset) * INVERSE_SQUARE_ROOT_OF_TWO_PI / widened;
    }
    float offset = abs(lateralAngle);
    return (upperNormalTail((offset - halfAngle) / spread) - upperNormalTail((offset + halfAngle) / spread))
            / (2.0 * halfAngle);
}

// The lowest elevation of the pillar, above the crystals at the eye and the light and above the rim's angle:
float lowestPillarElevation(float lightHeight, float horizontalDistance, float rim) {
    return max(abs(atan(lightHeight, horizontalDistance)), rim);
}

// Returns the radiance per unit intensity and unit face area per volume at the dust bottom of the pillar of level faces, faded in above its lowest elevation, faded near the eye, and shown below the cap, and writes its spread across the pillar.
// Also writes the crystals that mirror one light per unit face area per volume and unit crystalCountScale, which the fades and the cap leave alone, so glints dim instead of thinning out.
// The light stands at the height above the eye and the horizontal distance from it, and its width takes up the half angle across the view:
float pillarRadiance(float elevation, float lateralAngle, float lightHeight, float horizontalDistance, float halfAngle,
        float tiltSigma, vec2 shelter, out float spread, out float crystalCount) {
    spread = 0.0;
    crystalCount = 0.0;
    float lowestElevation = lowestPillarElevation(lightHeight, horizontalDistance, shelter.x);
    if (!(elevation > lowestElevation)) {
        return 0.0;
    }
    float sineOfElevation = sin(elevation);
    float inverseSineOfElevation = 1.0 / sineOfElevation;
    float sheetHeight = horizontalDistance * tan(elevation);
    float crystalHeight = 0.5 * (sheetHeight + lightHeight);
    float eyeLeg = crystalHeight * inverseSineOfElevation;
    float lightLeg = (crystalHeight - lightHeight) * inverseSineOfElevation;
    float tiltSpread = 2.0 * tiltSigma * sineOfElevation * lightLeg / (lightLeg + eyeLeg);
    spread = sqrt(tiltSpread * tiltSpread + lateralBlur * lateralBlur);
    float density = exp(-max(0.0, crystalHeight - dustBottomHeight) / scaleHeight);
    float crystalsOverDistance = density * lateralProfile(lateralAngle, spread, halfAngle) / horizontalDistance;
    bool countsCrystals = crystalCountScale > 0.0;
#ifdef FRAGMENT_SHADER
    countsCrystals = countsCrystals && debugMode != DEBUG_NO_GLITTER;
#endif
    if (countsCrystals) {
        float legsOverSpread = eyeLeg * lightLeg / max(sineOfElevation, SMALLEST_SINE_OF_GRAZING);
        crystalCount = 2.0 * crystalsOverDistance * legsOverSpread * legsOverSpread;
    }
    float weight = fadeIn(elevation, lowestElevation, tiltSigma) * capShare(sheetHeight, shelter.y)
            * nearWeight(eyeLeg);
    if (!(weight > 0.0)) {
        return 0.0;
    }
    // Both legs are straight, so the dust they pass through is the column between their ends over the sine:
    float opticalDepth = extinction * (2.0 * dustColumn(crystalHeight) - dustColumn(lightHeight) - dustColumn(0.0))
            * inverseSineOfElevation;
    return 0.5 * weight * crystalsOverDistance * exp(-opticalDepth)
            * slabReflectance(sineOfElevation, 1.0 / (refractiveIndex * refractiveIndex));
}

// The most that the crystals of a sky cell show, as a multiple of their mean value, as LightPillarOptics.sparkleCap:
float sparkleCap(float meanCount) {
    return meanCount > 0.0 ? min(MOST_SPARKLE, 1.0 + 3.0 / sqrt(meanCount) + 1.0 / meanCount) : MOST_SPARKLE;
}

#ifdef VERTEX_SHADER
// The ribbon index is slot * 2 + plate kind, then the row from the bottom and the side, -1 or 1, doubled at the top of each quad:
in vec3 Position;
out vec3 relativePosition;
flat out float pillarCellOrigin;
flat out vec3 pillarLight;
flat out float pillarHorizontalDistance;
flat out vec3 pillarColor;
flat out float pillarOpacity;
flat out float pillarTiltSigma;
flat out float pillarShare;
flat out vec2 pillarShelter;
flat out float pillarHalfAngle;
flat out float pillarLightCount;
flat out float pillarRibbon;

const float SEGMENTS = 16.0;
// Ribbons only cover the pillar on the screen, so they all stand at this distance from the eye:
const float RIBBON_DISTANCE = 1.0;
const float NEAREST_HORIZONTAL_DISTANCE = 0.5;
const float WIDEST_RIBBON_ANGLE = 1.4;

// Rows are closer together near the bottom of the ribbon:
float rowElevation(float row, float firstRow, float lastRow) {
    float rowFraction = row / SEGMENTS;
    return firstRow + (lastRow - firstRow) * rowFraction * rowFraction;
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

// Returns the angle off the center beyond which the pillar stays below the visible value, plus the half angle of the light's width and the margin.
// Where crystals glitter, beyond it a pixel also shows the glint value with less than RIBBON_GLINT_CHANCE, as LightPillarOptics.ribbonHalfWidth:
float halfWidthAt(float elevation, float lightHeight, float horizontalDistance, float halfAngle, float tiltSigma,
        vec2 shelter, float valueScale, float countScale) {
    float spread;
    float crystalCount;
    float center = valueScale * pillarRadiance(elevation, 0.0, lightHeight, horizontalDistance, halfAngle, tiltSigma,
            shelter, spread, crystalCount);
    float meanCount = countScale * crystalCount;
    bool shows = false;
    float spreads = 0.0;
    if (center > visibleValue) {
        shows = true;
        spreads = sqrt(2.0 * log(center / visibleValue));
    }
    if (meanCount > 0.0 && center > 0.0) {
        float leastCount = leastVisibleCount(center / (meanCount * glintValue));
        if (meanCount > leastCount) {
            shows = true;
            spreads = max(spreads, sqrt(2.0 * log(meanCount / leastCount)));
        }
    }
    if (!shows) {
        return 0.0;
    }
    return min(halfAngle + min(MOST_RIBBON_SPREADS, spreads) * spread + marginAngle, WIDEST_RIBBON_ANGLE);
}

void main() {
    int ribbon = int(Position.x + 0.5);
    int slot = ribbon / 2;
    int kind = ribbon - 2 * slot;
    vec3 light = lights[slot].xyz;
    vec2 shelter = shelters[slot];
    vec2 span = spans[slot];
    vec4 color = colors[slot];
    float tiltSigma = tiltSigmas[kind];
    float share = shares[kind];
    float horizontalDistance = length(light.xz);
    pillarLight = light;
    pillarHorizontalDistance = horizontalDistance;
    pillarColor = color.rgb;
    pillarOpacity = color.a;
    pillarTiltSigma = tiltSigma;
    pillarShare = share;
    pillarShelter = shelter;
    pillarHalfAngle = 0.0;
    pillarLightCount = span.y;
    pillarRibbon = float(ribbon);
    pillarCellOrigin = 0.0;
    relativePosition = vec3(0.0);
    gl_Position = vec4(0.0, 0.0, 2.0, 1.0);

    float valueScale = max(color.r, max(color.g, color.b)) * color.a * share * extinction;
    float countScale = share * extinction * crystalCountScale * span.y;
    if (!(valueScale > 0.0) || horizontalDistance < NEAREST_HORIZONTAL_DISTANCE) {
        return;
    }
    float halfAngle = span.x / horizontalDistance;
    pillarHalfAngle = halfAngle;
    float lightElevation = atan(light.y, horizontalDistance);
    // The cells stand on the light, or below the eye on the lowest elevation that faces tilted by CELL_ORIGIN_SIGMAS mirror it to, whatever the shelter:
    float cellOrigin = max(lightElevation, -lightElevation - CELL_ORIGIN_SIGMAS * tiltSigma);
    pillarCellOrigin = cellOrigin;
    // The pillar shows from its lowest elevation up to the cap on the sheet:
    float lowest = lowestPillarElevation(light.y, horizontalDistance, shelter.x);
    float highest = min(highestElevation, atan(shelter.y, horizontalDistance));
    if (!(highest > lowest)) {
        return;
    }
    float firstRow = lowest - marginAngle;
    float lowestEvaluated = min(lowest + 1.0e-3, highest);
    float elevation = rowElevation(Position.y, firstRow, highest);
    // Each row is also as wide as the rows next to it and the half rows between, so the straight edges between rows
    // hold the pillar:
    float halfWidth = 0.0;
    for (float row = max(Position.y - 1.0, 0.0); row <= min(Position.y + 1.0, SEGMENTS); row += 0.5) {
        float rowEvaluated = clamp(rowElevation(row, firstRow, highest), lowestEvaluated, highest);
        halfWidth = max(halfWidth, halfWidthAt(rowEvaluated, light.y, horizontalDistance, halfAngle, tiltSigma,
                shelter, valueScale, countScale));
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
// Scene points within this many blocks of the light's horizontal distance, along the level direction to the light, hide part of the pillar:
uniform float depthBand;
// The cloud height relative to the eye, at and above which scene points are clouds, which do not hide pillars:
uniform float cloudHeight;
in vec3 relativePosition;
flat in float pillarCellOrigin;
flat in vec3 pillarLight;
flat in float pillarHorizontalDistance;
flat in vec3 pillarColor;
flat in float pillarOpacity;
flat in float pillarTiltSigma;
flat in float pillarShare;
flat in vec2 pillarShelter;
flat in float pillarHalfAngle;
flat in float pillarLightCount;
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
const float SMALLEST_DEPTH_BAND = 1.0e-3;
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
    float spread;
    float crystalCount;
    float radiance = pillarShare * extinction * pillarRadiance(elevation, float(cell.x) * cellAngle, pillarLight.y,
            pillarHorizontalDistance, pillarHalfAngle, pillarTiltSigma, pillarShelter, spread, crystalCount);
    if (crystalCountScale > 0.0 && debugMode != DEBUG_NO_GLITTER) {
        radiance *= sparkle(pillarShare * extinction * crystalCountScale * pillarLightCount * crystalCount, cell);
    }
    return radiance * brightestChannel();
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
// The share of the pillar that shows at the pixel, which falls from all of it to none as the scene comes nearer than the vertical sheet through the light toward the eye.
// Directions without a scene and scene points in the clouds show all of it:
float sheetShare(vec2 toward) {
    if (debugMode == DEBUG_NO_SCENE_DEPTH) {
        return 1.0;
    }
    float depth = texelFetch(sceneDepth, ivec2(gl_FragCoord.xy), 0).r;
    if (!(depth < 1.0)) {
        return 1.0;
    }
    vec4 scene = inverseViewProjection * vec4(gl_FragCoord.xy / screenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 point = scene.xyz / scene.w;
    if (point.y >= cloudHeight) {
        return 1.0;
    }
    float band = max(depthBand, SMALLEST_DEPTH_BAND);
    return smoothstep(pillarHorizontalDistance - band, pillarHorizontalDistance + band, dot(point.xz, toward));
}

void main() {
    if (debugMode == DEBUG_RIBBONS_ONLY) {
        fragColor = vec4(DEBUG_SHADE * pillarOpacity, 1.0);
        return;
    }
    vec3 direction = normalize(relativePosition);
    vec2 toward = pillarLight.xz / pillarHorizontalDistance;
    float shown = dot(direction.xz, toward) > 0.0 ? sheetShare(toward) : 0.0;
    if (!(shown > 0.0)) {
        if (fillView > 0.0) {
            fragColor = vec4(FILL_WASTED, 1.0);
            return;
        }
        discard;
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
            value *= shown;
        }
        vec3 linear = pillarColor * (value / brightestChannel());
        if (fillView > 0.0) {
            vec3 encoded = encodeSrgb(1.0 - exp(-linear)) * pillarOpacity;
            vec3 fill = max(encoded.r, max(encoded.g, encoded.b)) >= SHOWN_STEP ? FILL_SHOWN : FILL_WASTED;
            fragColor = vec4(inAtlas || cell.y < 0 ? fill : FILL_COMPUTED, 1.0);
            return;
        }
        if (!(value > 0.0)) {
            discard;
        }
        fragColor = vec4(encodeSrgb(1.0 - exp(-linear)) * pillarOpacity, 1.0);
        return;
    }
    if (debugMode == DEBUG_CONSTANT_INTEGRAL) {
        fragColor = vec4(DEBUG_SHADE * shown * pillarOpacity, 1.0);
        return;
    }
    float spread;
    float crystalCount;
    float radiance = pillarShare * extinction * pillarRadiance(elevation, lateralAngle, pillarLight.y,
            pillarHorizontalDistance, pillarHalfAngle, pillarTiltSigma, pillarShelter, spread, crystalCount);
    if (crystalCountScale > 0.0 && debugMode != DEBUG_NO_GLITTER) {
        radiance *= sparkle(pillarShare * extinction * crystalCountScale * pillarLightCount * crystalCount,
                ivec2(floor(vec2(lateralAngle, elevation) / marginAngle)));
    }
    radiance *= shown;
    if (fillView > 0.0) {
        vec3 encoded = encodeSrgb(1.0 - exp(-pillarColor * radiance)) * pillarOpacity;
        fragColor = vec4(max(encoded.r, max(encoded.g, encoded.b)) >= SHOWN_STEP ? FILL_SHOWN : FILL_WASTED, 1.0);
        return;
    }
    if (!(radiance > 0.0)) {
        discard;
    }
    fragColor = vec4(encodeSrgb(1.0 - exp(-pillarColor * radiance)) * pillarOpacity, 1.0);
}
#endif
#endif
