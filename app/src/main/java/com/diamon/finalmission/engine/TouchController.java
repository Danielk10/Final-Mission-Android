package com.diamon.finalmission.engine;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * Ergonomic multi-touch controller optimized for widescreen landscape arcade gameplay in Final Mission:
 * 1. Left Half (Movement & Jetpack):
 *    - 1:1 direct relative follow (touchDeltaX, touchDeltaY): dragging anywhere on the left half moves
 *      the soldier smoothly in 8 directions without the player's thumb ever obscuring the sprite.
 *    - Smooth finger handoff if player swaps fingers.
 * 2. Rapid Facing Direction Controls (Left / Right turn):
 *    - Directional flick: quick horizontal swipe flips player facing direction instantly.
 *    - Double-tap: quick consecutive tap instantly toggles or turns player direction.
 * 3. Right Half (Aiming & Satellite Lock):
 *    - Dragging on the right half aims orbital satellites 360-degrees in that direction and locks them.
 *    - Quick tap on right half toggles satellite lock mode on/off.
 *    - On-screen Satellite Lock button hit-testing.
 * 4. Responsive Arcade Autofire:
 *    - Continuous automatic fire whenever any finger is touching the screen.
 */
public class TouchController {

    public int primaryId = -1;
    public int secondaryId = -1;

    // Movement tracking
    public float touchDeltaX = 0f;
    public float touchDeltaY = 0f;
    // Backward compatibility aliases
    public float deltaXTactil = 0f;
    public float deltaYTactil = 0f;
    public boolean touchInitialized = false;

    public float primaryStartX = 0f;
    public float primaryStartY = 0f;
    public float primaryTouchTime = 0f;

    public float primaryX = 0f;
    public float primaryY = 0f;
    public float secondaryX = 0f;
    public float secondaryY = 0f;

    public float secondaryOriginX = 0f;
    public float secondaryOriginY = 0f;
    public float secondaryStartX = 0f;
    public float secondaryStartY = 0f;
    public float secondaryTouchTime = 0f;
    public boolean secondaryHasDragged = false;

    public boolean isTouching = false;
    public boolean isFiring = false;

    // Satellite aim & lock
    public boolean satelliteLock = false;
    public Float satelliteTargetAngle = null;

    // Facing direction change indicator: true = Right, false = Left, null = no change
    public Boolean flickFacing = null;

    // Double-tap turnaround tracking
    private long lastTapTime = 0;
    private float lastTapX = 0f;
    private float lastTapY = 0f;
    private static final long DOUBLE_TAP_TIMEOUT_MS = 280;

    // On-screen Satellite Lock button bounds (in virtual coordinates)
    public final RectF lockButtonBounds = new RectF();

    public TouchController() {
        reset();
        updateLockButtonBounds(400f, 224f);
    }

    public void updateLockButtonBounds(float virtualWidth, float virtualHeight) {
        // Positioned neatly in bottom-right corner of widescreen view
        float btnW = 54f;
        float btnH = 26f;
        float marginX = 8f;
        float marginY = 8f;
        lockButtonBounds.left = virtualWidth - btnW - marginX;
        lockButtonBounds.top = virtualHeight - btnH - marginY;
        lockButtonBounds.right = virtualWidth - marginX;
        lockButtonBounds.bottom = virtualHeight - marginY;
    }

    public boolean isTouchOnLockButton(float x, float y) {
        // Generous padding for comfortable capacitive touch hitting
        return (x >= lockButtonBounds.left - 6f && x <= lockButtonBounds.right + 6f &&
                y >= lockButtonBounds.top - 6f && y <= lockButtonBounds.bottom + 6f);
    }

    public void reset() {
        primaryId = -1;
        secondaryId = -1;
        touchDeltaX = 0f;
        touchDeltaY = 0f;
        deltaXTactil = 0f;
        deltaYTactil = 0f;
        touchInitialized = false;
        primaryStartX = 0f;
        primaryStartY = 0f;
        primaryTouchTime = 0f;
        primaryX = 0f;
        primaryY = 0f;
        secondaryX = 0f;
        secondaryY = 0f;
        secondaryOriginX = 0f;
        secondaryOriginY = 0f;
        secondaryStartX = 0f;
        secondaryStartY = 0f;
        secondaryTouchTime = 0f;
        secondaryHasDragged = false;
        isTouching = false;
        isFiring = false;
        satelliteLock = false;
        satelliteTargetAngle = null;
        flickFacing = null;
    }

    /**
     * Resynchronizes the relative touch drag offset when the player entity
     * is clamped against scenery, ceilings, or level boundaries.
     * Eliminates touch deadzones and sticky movement when changing drag direction away from obstacles.
     *
     * @param playerX The current clamped X position of the player.
     * @param playerY The current clamped Y position of the player.
     */
    public void resync(float playerX, float playerY) {
        if (primaryId != -1 && touchInitialized) {
            touchDeltaX = primaryX - playerX;
            touchDeltaY = primaryY - playerY;
            deltaXTactil = touchDeltaX;
            deltaYTactil = touchDeltaY;
        }
    }

    public void update(float dt) {
        if (primaryId != -1) {
            primaryTouchTime += dt;
        }
        if (secondaryId != -1) {
            secondaryTouchTime += dt;
        }
    }

    public void onTouchDown(int pointerId, float x, float y, float playerX, float playerY) {
        long now = System.currentTimeMillis();

        // 1. Check if tapped the dedicated on-screen Satellite Lock button
        if (isTouchOnLockButton(x, y)) {
            satelliteLock = !satelliteLock;
            isTouching = true;
            isFiring = true;
            return;
        }

        // 2. Double-tap detection for instant turnaround
        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT_MS) {
            float distSq = (x - lastTapX) * (x - lastTapX) + (y - lastTapY) * (y - lastTapY);
            if (distSq < 1600f) { // Within 40px radius
                // Instant turnaround: if tapped to the left of player -> face left; if right -> face right
                flickFacing = (x >= playerX);
            }
        }
        lastTapTime = now;
        lastTapX = x;
        lastTapY = y;

        // 3. Pointer Assignment
        if (primaryId == -1) {
            // Primary touch: 1:1 direct follow & jetpack flight
            primaryId = pointerId;
            primaryX = x;
            primaryY = y;
            primaryStartX = x;
            primaryStartY = y;
            primaryTouchTime = 0f;

            touchDeltaX = x - playerX;
            touchDeltaY = y - playerY;
            deltaXTactil = touchDeltaX;
            deltaYTactil = touchDeltaY;
            touchInitialized = true;

            isTouching = true;
            isFiring = true;
        } else if (secondaryId == -1 && pointerId != primaryId) {
            // Secondary touch: satellite aiming & flick gestures
            secondaryId = pointerId;
            secondaryX = x;
            secondaryY = y;
            secondaryOriginX = x;
            secondaryOriginY = y;
            secondaryStartX = x;
            secondaryStartY = y;
            secondaryTouchTime = 0f;
            secondaryHasDragged = false;

            isTouching = true;
            isFiring = true;
        }
    }

    public PointF onTouchMove(int pointerId, float x, float y) {
        if (primaryId == pointerId) {
            float moveDx = x - primaryX;
            primaryX = x;
            primaryY = y;
            isTouching = true;
            isFiring = true;

            // Directional facing on touch drag
            if (moveDx < -1.5f) {
                flickFacing = false; // Turn left
            } else if (moveDx > 1.5f) {
                flickFacing = true; // Turn right
            }

            if (touchInitialized) {
                float newX = x - touchDeltaX;
                float newY = y - touchDeltaY;
                return new PointF(newX, newY);
            }
        } else if (secondaryId == pointerId) {
            secondaryX = x;
            secondaryY = y;

            float dx = x - secondaryStartX;
            float dy = y - secondaryStartY;
            float distSq = dx * dx + dy * dy;

            // Horizontal flick during motion
            if (!secondaryHasDragged && Math.abs(dx) >= 18f && Math.abs(dx) >= Math.abs(dy) * 1.3f && secondaryTouchTime < 0.35f) {
                flickFacing = dx > 0f;
            }

            // 360-degree satellite aiming and lock
            if (distSq > 80f) { // ~9px deadzone
                secondaryHasDragged = true;
                satelliteLock = true;

                // Virtual drag vector clamping
                float dist = (float) Math.sqrt(distSq);
                float maxRadius = 60f;
                if (dist > maxRadius) {
                    secondaryStartX = x - (dx / dist) * maxRadius;
                    secondaryStartY = y - (dy / dist) * maxRadius;
                    dx = x - secondaryStartX;
                    dy = y - secondaryStartY;
                }

                satelliteTargetAngle = (float) Math.atan2(dy, dx);
            }
        }
        return null;
    }

    public void onTouchUp(int pointerId, float x, float y, float playerX, float playerY) {
        if (primaryId == pointerId) {
            float pDx = x - primaryStartX;
            float pDy = y - primaryStartY;
            if (primaryTouchTime < 0.35f && Math.abs(pDx) >= 18f && Math.abs(pDx) >= Math.abs(pDy) * 1.3f) {
                // Quick horizontal flick with primary finger
                flickFacing = pDx > 0f;
            }

            if (secondaryId != -1) {
                // Smooth handoff: secondary finger seamlessly becomes primary
                primaryId = secondaryId;
                primaryX = secondaryX;
                primaryY = secondaryY;
                primaryStartX = secondaryX;
                primaryStartY = secondaryY;
                primaryTouchTime = 0f;

                touchDeltaX = primaryX - playerX;
                touchDeltaY = primaryY - playerY;
                deltaXTactil = touchDeltaX;
                deltaYTactil = touchDeltaY;
                touchInitialized = true;

                secondaryId = -1;
                secondaryHasDragged = false;
                secondaryTouchTime = 0f;
            } else {
                primaryId = -1;
                touchInitialized = false;
                isTouching = false;
                isFiring = false;
            }
        } else if (secondaryId == pointerId) {
            float totalDx = x - secondaryOriginX;
            float totalDy = y - secondaryOriginY;
            float totalDistSq = totalDx * totalDx + totalDy * totalDy;
            float touchTime = secondaryTouchTime;

            if (touchTime < 0.40f) {
                if (Math.abs(totalDx) >= 18f && Math.abs(totalDx) >= Math.abs(totalDy) * 1.1f) {
                    // Quick flick turnaround
                    flickFacing = totalDx > 0f;
                } else if (totalDistSq < 400f) {
                    // Quick tap without dragging -> toggle satellite lock
                    satelliteLock = !satelliteLock;
                }
            }

            secondaryId = -1;
            secondaryHasDragged = false;
            secondaryTouchTime = 0f;
        }
    }

    public void onTouchCancel(float playerX, float playerY) {
        reset();
    }
}
