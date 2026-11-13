package com.teammoeg.frostedheart.content.utility.seld;

/** Runtime ground parameters; airborne and water physics bypass interpolation. */
final class SledGroundTransition {
    private boolean grounded;
    private float friction;
    private float acceleration;
    private float targetFriction;
    private float targetAcceleration;
    private int remaining;
    private int duration;

    void update(boolean onGround, float nextFriction, float nextAcceleration, int ticks) {
        if (!onGround || !grounded || ticks == 0) {
            friction = nextFriction;
            acceleration = nextAcceleration;
            remaining = 0;
        } else {
            if (nextFriction != targetFriction || nextAcceleration != targetAcceleration || ticks != duration) {
                remaining = ticks;
            }
            if (remaining > 0) {
                friction += (nextFriction - friction) / remaining;
                acceleration += (nextAcceleration - acceleration) / remaining;
                --remaining;
            }
        }
        grounded = onGround;
        targetFriction = nextFriction;
        targetAcceleration = nextAcceleration;
        duration = ticks;
    }

    float friction() { return friction; }
    float acceleration() { return acceleration; }
}
