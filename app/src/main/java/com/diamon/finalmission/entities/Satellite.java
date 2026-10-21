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
        this.orbitAngle = (float) (podIndex * Math.PI);
        this.aimAngle = 0f;
    }

    public void update(float dt, float playerCenterX, float playerCenterY, boolean lockActive, Float targetAngle) {
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
            // Unlocked: clear lock memory and orbit fluidly 360 degrees
            this.lockedAngle = null;
            orbitAngle += orbitSpeed * dt;
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
        } else if (deg >= 22.5 && deg < 67.5) {
            spriteName = "sateliteDD" + frame + ".png";
        } else if (deg >= 67.5 && deg < 112.5) {
            spriteName = "sateliteVB" + frame + ".png";
        } else if (deg >= 112.5 && deg < 157.5) {
            spriteName = "sateliteDI" + frame + ".png";
        } else if (deg >= 157.5 || deg < -157.5) {
            spriteName = "sateliteHI" + frame + ".png";
        } else if (deg >= -157.5 && deg < -112.5) {
            spriteName = "sateliteDI" + frame + ".png";
            flipY = true;
        } else if (deg >= -112.5 && deg < -67.5) {
            spriteName = "sateliteVA" + frame + ".png";
        } else if (deg >= -67.5 && deg < -22.5) {
            spriteName = "sateliteDD" + frame + ".png";
            flipY = true;
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
