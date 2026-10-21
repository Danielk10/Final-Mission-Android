package com.diamon.finalmission.entities;

/**
 * Floating power-up capsule for Final Mission NES:
 * - 'W' = Wide spread wave shot (poderW.png)
 * - 'L' = Piercing laser beam (poderL.png)
 * - 'S' = Speed booster (poderS.png)
 * - 'B' = Life energy recovery (poderB.png)
 * - Strictly NO BOMBS (authentic to original NES Final Mission).
 * - Floats and bobs with the authentic physics ported from the original engine.
 */
public class PowerUpCapsule extends PowerUp {

    public PowerUpCapsule(float x, float y, Type type) {
        super(x, y, type);
    }
}
