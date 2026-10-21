package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.diamon.finalmission.engine.AssetLoader;

/**
 * Authentic orbital satellite pod entity from Final Mission NES:
 * Orbits continuously 360-degrees around the player character, or locks its aim
 * angle into a fixed direction via touch controls / on-screen lock toggle.
 */
public class Satellite {

    public final int podIndex;
    public float x;
    public float y;
    public float orbitAngle = 0f;
    public float orbitRadius = 22f;
    public float orbitSpeed = 3.6f;

    public boolean isLocked = false;
    public float aimAngle = 0f;
    public Float lockedAngle = null;

    private float animTimer = 0f;
    private int animFrame = 0;

    public Satellite(int podIndex) {
        this.podIndex = podIndex;
        // Base orbit angle starts at -PI/2 (top). Pod 0 is top, Pod 1 is bottom (separated by PI).
        this.orbitAngle = (float) (-Math.PI / 2.0);
        this.aimAngle = (float) (-Math.PI / 2.0 + podIndex * Math.PI);
    }

    public void update(float dt, float playerCenterX, float playerCenterY, boolean lockActive, Float targetAngle) {
        update(dt, playerCenterX, playerCenterY, lockActive, targetAngle, 0f, 0f, false, 0f);
    }

    public void update(float dt, float playerCenterX, float playerCenterY, boolean lockActive, Float targetAngle,
                       boolean isMoving, float moveDist) {
        update(dt, playerCenterX, playerCenterY, lockActive, targetAngle, isMoving ? moveDist : 0f, 0f, isMoving, moveDist);
    }

    public void update(float dt, float playerCenterX, float playerCenterY, boolean lockActive, Float targetAngle,
                       float dx, float dy, boolean isMoving, float moveDist) {
        animTimer += dt;
        if (animTimer >= 0.10f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 2;
        }

        this.isLocked = lockActive;

        if (lockActive) {
            if (targetAngle != null) {
                this.lockedAngle = targetAngle;
                this.aimAngle = targetAngle;
                float flankOffset = (podIndex == 0 ? 1f : -1f) * (float) (Math.PI / 2.0);
                float posAngle = aimAngle + flankOffset;
                this.x = playerCenterX + (float) Math.cos(posAngle) * orbitRadius;
                this.y = playerCenterY + (float) Math.sin(posAngle) * orbitRadius;
            } else {
                // Statically locked in current orbital position
                if (lockedAngle == null) {
                    lockedAngle = (float) (orbitAngle + (podIndex * Math.PI));
                }
                this.aimAngle = lockedAngle;
                this.x = playerCenterX + (float) Math.cos(lockedAngle) * orbitRadius;
                this.y = playerCenterY + (float) Math.sin(lockedAngle) * orbitRadius;
            }
        } else {
            // Unlocked: In authentic NES Final Mission (ROM Bank 7 $E004 / $E065),
            // satellites DO NOT auto-spin when the player is stationary!
            // Orbit rotation is driven by player movement direction:
            // - Moving RIGHT: counter-clockwise (orbitAngle -= step)
            // - Moving LEFT: clockwise (orbitAngle += step)
            // - Moving UP: counter-clockwise (orbitAngle -= step)
            // - Moving DOWN: clockwise (orbitAngle += step)
            // - Clamped step prevents rapid spin during touch dragging
            this.lockedAngle = null;
            if (isMoving && (Math.abs(dx) > 0.05f || Math.abs(dy) > 0.05f || moveDist > 0.05f)) {
                float rawStep = (moveDist > 0.001f) ? Math.max(orbitSpeed * dt, moveDist * 0.06f) : (orbitSpeed * dt);
                float step = Math.min(0.15f, rawStep);

                if (Math.abs(dx) >= Math.abs(dy)) {
                    if (dx > 0.05f) {
                        orbitAngle -= step; // RIGHT -> CCW
                    } else if (dx < -0.05f) {
                        orbitAngle += step; // LEFT -> CW
                    } else if (moveDist > 0.05f) {
                        orbitAngle -= step;
                    }
                } else {
                    if (dy < -0.05f) {
                        orbitAngle -= step; // UP (dy < 0) -> CCW
                    } else if (dy > 0.05f) {
                        orbitAngle += step; // DOWN (dy > 0) -> CW
                    }
                }
            }
            float currentAngle = (float) (orbitAngle + (podIndex * Math.PI));
            this.aimAngle = currentAngle;
            this.x = playerCenterX + (float) Math.cos(currentAngle) * orbitRadius;
            this.y = playerCenterY + (float) Math.sin(currentAngle) * orbitRadius;
        }
    }

    public Bullet fire() {
        return fire(WeaponType.NORMAL);
    }

    public Bullet fire(WeaponType weaponType) {
        if (weaponType == WeaponType.WIDE) {
            return Bullet.createSatelliteWide(x, y, aimAngle);
        } else if (weaponType == WeaponType.LASER) {
            return Bullet.createSatelliteLaser(x, y, aimAngle);
        } else if (weaponType == WeaponType.BOMB) {
            return Bullet.createSatelliteBomb(x, y, aimAngle);
        } else {
            return Bullet.createSatellite(x, y, aimAngle);
        }
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        // Normalize angle to -PI..PI
        double normAngle = Math.atan2(Math.sin(aimAngle), Math.cos(aimAngle));
        double deg = Math.toDegrees(normAngle);

        int frame = animFrame + 1; // 1 or 2
        String spriteName = "sateliteHD" + frame + ".png";
        boolean flipY = false;

        if (deg >= -22.5 && deg < 22.5) {
            spriteName = "sateliteHD" + frame + ".png";
            flipY = false;
        } else if (deg >= 22.5 && deg < 67.5) {
            spriteName = "sateliteDD" + frame + ".png";
            flipY = true; // DD slopes up-right naturally; flip vertically to aim down-right
        } else if (deg >= 67.5 && deg < 112.5) {
            spriteName = "sateliteVB" + frame + ".png";
            flipY = false;
        } else if (deg >= 112.5 && deg < 157.5) {
            spriteName = "sateliteDI" + frame + ".png";
            flipY = true; // DI slopes up-left naturally; flip vertically to aim down-left
        } else if (deg >= 157.5 || deg < -157.5) {
            spriteName = "sateliteHI" + frame + ".png";
            flipY = false;
        } else if (deg >= -157.5 && deg < -112.5) {
            spriteName = "sateliteDI" + frame + ".png";
            flipY = false;
        } else if (deg >= -112.5 && deg < -67.5) {
            spriteName = "sateliteVA" + frame + ".png";
            flipY = false;
        } else if (deg >= -67.5 && deg < -22.5) {
            spriteName = "sateliteDD" + frame + ".png";
            flipY = false;
        }

        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            if (flipY) {
                bmp = loader.getFlippedBitmap(bmp, false, true);
            }
            float drawX = x - bmp.getWidth() / 2f;
            float drawY = y - bmp.getHeight() / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }
}
