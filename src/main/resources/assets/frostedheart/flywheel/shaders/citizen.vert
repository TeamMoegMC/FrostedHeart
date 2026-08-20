struct Citizen {
    vec2 light;
    vec3 pos0;
    vec3 pos1;
    vec2 timing;
    vec2 velocity;
    vec3 yaw;
    vec4 flags;
};

const float CITIZEN_TIME_PERIOD = 1728000.0;
const float CITIZEN_PI2 = 6.28318530718;
const float CITIZEN_WALK_PHASE_PER_BLOCK = 0.6662;
const float CITIZEN_LEG_SWING_SCALE = 1.4;
const float CITIZEN_SLEEP_SCALE = 0.8;
const float CITIZEN_SLEEP_ORIGIN = -1.18;
const float CITIZEN_SLEEP_SURFACE_Y = 0.58;

float citizenElapsed(float start) {
    return mod(uTime - start + CITIZEN_TIME_PERIOD, CITIZEN_TIME_PERIOD);
}

void citizenRotateX(inout vec3 pos, inout vec3 normal, vec3 pivot, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    pos -= pivot;
    pos.yz = mat2(c, s, -s, c) * pos.yz;
    normal.yz = mat2(c, s, -s, c) * normal.yz;
    pos += pivot;
}

void vertex(inout Vertex v, Citizen citizen) {
    float snapshotElapsed = citizenElapsed(citizen.timing.x);
    float duration = max(citizen.timing.y, 1.0);
    float blend = clamp(snapshotElapsed / duration, 0.0, 1.0);
    vec3 base = mix(citizen.pos0, citizen.pos1, blend);
    float extrapolation = clamp(snapshotElapsed - duration, 0.0, 30.0);
    base.xz += citizen.velocity * extrapolation;
    float snapshotDistance = length(citizen.pos1.xz - citizen.pos0.xz);
    float snapshotSpeed = snapshotDistance / duration;
    float extrapolationSpeed = length(citizen.velocity);
    float walkSpeed = extrapolation > 0.0 ? extrapolationSpeed : snapshotSpeed;
    float walkPhase = citizen.flags.z * CITIZEN_PI2 / 256.0
        + blend * snapshotDistance * CITIZEN_WALK_PHASE_PER_BLOCK
        + extrapolation * extrapolationSpeed * CITIZEN_WALK_PHASE_PER_BLOCK;

    float yawElapsed = citizenElapsed(citizen.yaw.z);
    float yawDistance = abs(citizen.yaw.y);
    float fastTicks = max(yawDistance - 32.0, 0.0) / 12.0;
    float yawTravel = min(yawDistance,
        min(yawElapsed, fastTicks) * 12.0 + max(yawElapsed - fastTicks, 0.0) * 3.0);
    float yaw8 = mod(citizen.yaw.x + sign(citizen.yaw.y) * yawTravel + 256.0, 256.0);
    float angle = yaw8 * CITIZEN_PI2 / 256.0;
    vec3 forward = vec3(-sin(angle), 0.0, cos(angle));
    vec3 right = vec3(forward.z, 0.0, -forward.x);

    float part = floor(v.color.r * 255.0 + 0.5);
    bool sleeping = citizen.flags.y > 0.5;
    bool moving = citizen.flags.x > 0.5 || snapshotSpeed > 0.001;
    float ageScale = citizen.flags.w < 0.5 ? 0.4
        : (citizen.flags.w < 1.5 ? 0.5 : 1.0);
    v.color = vec4(1.0);
    v.light = citizen.light;

    if (part > 5.5) {
        if (sleeping) {
            float lengthOffset = mix(0.38, -1.18, v.pos.y) * ageScale;
            v.pos = base + right * (v.pos.x * 0.27 * ageScale)
                + forward * lengthOffset + vec3(0.0, 0.60, 0.0);
            v.normal = vec3(0.0, 1.0, 0.0);
            v.texCoords.y = (part > 6.5 ? 0.375 : 0.8125) - v.texCoords.y;
        } else {
            vec3 toCamera = uCameraPos - base;
            float cameraLength = length(toCamera);
            vec3 facing = cameraLength > 0.0001 ? toCamera / cameraLength : vec3(0.0, 0.0, 1.0);
            vec3 cameraRight = cross(vec3(0.0, 1.0, 0.0), facing);
            float rightLength = length(cameraRight);
            cameraRight = rightLength > 0.0001 ? cameraRight / rightLength : vec3(1.0, 0.0, 0.0);
            vec3 cameraUp = normalize(cross(facing, cameraRight));
            v.pos = base + cameraRight * (v.pos.x * 0.30 * ageScale)
                + cameraUp * (v.pos.y * 1.80 * ageScale);
            v.normal = facing;
        }
        return;
    }

    vec3 localPos = v.pos;
    vec3 localNormal = v.normal;
    if (moving && !sleeping && part > 1.5) {
        float limbAmplitude = part > 3.5 ? walkSpeed * CITIZEN_LEG_SWING_SCALE : walkSpeed;
        float swing = sin(walkPhase) * limbAmplitude;
        if (part > 2.5 && part < 4.5)
            swing = -swing;
        vec3 pivot;
        if (part < 3.5)
            pivot = vec3(part < 2.5 ? -0.375 : 0.375, -1.5, 0.0);
        else
            pivot = vec3(part < 4.5 ? -0.125 : 0.125, -0.75, 0.0);
        citizenRotateX(localPos, localNormal, pivot, swing);
    }
    localPos *= ageScale;

    if (sleeping) {
        float depth = part > 0.5 && part < 1.5 ? 0.5 : 0.25;
        v.pos = base + right * (localPos.x * CITIZEN_SLEEP_SCALE)
            + forward * (CITIZEN_SLEEP_ORIGIN * ageScale - localPos.y * CITIZEN_SLEEP_SCALE)
            + vec3(0.0, CITIZEN_SLEEP_SURFACE_Y + depth * CITIZEN_SLEEP_SCALE * ageScale * 0.5
                - localPos.z * CITIZEN_SLEEP_SCALE, 0.0);
        v.normal = normalize(right * localNormal.x - forward * localNormal.y
            - vec3(0.0, 1.0, 0.0) * localNormal.z);
    } else {
        v.pos = base + right * localPos.x + vec3(0.0, -localPos.y, 0.0)
            + vec3(-forward.x, 0.0, -forward.z) * localPos.z;
        v.normal = normalize(right * localNormal.x - vec3(0.0, 1.0, 0.0) * localNormal.y
            + vec3(-forward.x, 0.0, -forward.z) * localNormal.z);
    }
}
