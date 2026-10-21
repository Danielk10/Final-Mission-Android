package com.diamon.finalmission.entities.enemies;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Player;

import java.util.List;

/**
 * Anti-Air Turret enemy from Final Mission NES:
 * Stationary turrets anchored to floor, ceiling, or diagonal terrain.
 * Dynamically angles its heavy cannon toward the player's position,
 * exhibits authentic barrel recoil upon firing, and launches targeted plasma shots.
 */
public class AntiAirTurret extends Enemy {

    public enum Mount {
        GROUND,  // Floor (antiAreoH / antiAreoV1 / antiAreoD)
        CEILING, // Ceiling (antiAreoV2 / inverted diagonal)
        DIAGONAL // Wall / Corner (antiAreoD1 / antiAreoD2)
    }

    public final Mount mount;
    public float vy = 0f;
    private float shootTimer = 1.0f;
    public float recoilTimer = 0f;

    private String currentSpriteName = "antiAreoH1.png";
    private boolean flipVertical = false;
    private float muzzleX = 0f;
    private float muzzleY = 0f;

    public AntiAirTurret(float x, float y, Mount mount) {
        this(x, y, mount, 0f);
    }

    public AntiAirTurret(float x, float y, Mount mount, float vy) {
        super(x, y, 15f, 16f, 3f, 300);
        this.mount = mount;
        this.vy = vy;
        this.shootTimer = 0.8f + (float) (Math.random() * 0.8f);
        updateAim(x - 50f, y); // Initial aim facing incoming player
    }

    public static AntiAirTurret create(float x, float y, Mount mount, float vy) {
        return new AntiAirTurret(x, y, mount, vy);
    }

    public static AntiAirTurret create(float x, float y, Mount mount) {
        return new AntiAirTurret(x, y, mount, 0f);
    }

    private void updateAim(float playerCenterX, float playerCenterY) {
        float turretCenterX = x + width / 2f;
        float turretCenterY = y + height / 2f;
        float dx = playerCenterX - turretCenterX;
        float dy = playerCenterY - turretCenterY;

        switch (mount) {
            case CEILING:
                // Mounted on ceiling, base at top
                if (Math.abs(dx) < 22f && dy > 12f) {
                    // Straight down (antiAreoV2 has base at top pointing down)
                    currentSpriteName = "antiAreoV2.png";
                    flipVertical = false;
                    muzzleX = turretCenterX;
                    muzzleY = y + height + 2f;
                } else if (dx < -15f && dy > 15f) {
                    // Diagonal down-left
                    currentSpriteName = "antiAreoD1.png";
                    flipVertical = true;
                    muzzleX = x;
                    muzzleY = y + height;
                } else if (dx > 15f && dy > 15f) {
                    // Diagonal down-right
                    currentSpriteName = "antiAreoD2.png";
                    flipVertical = true;
                    muzzleX = x + width;
                    muzzleY = y + height;
                } else if (dx >= 0) {
                    // Shallow right
                    currentSpriteName = "antiAreoH2.png";
                    flipVertical = true;
                    muzzleX = x + width + 2f;
                    muzzleY = y + height - 3f;
                } else {
                    // Shallow left
                    currentSpriteName = "antiAreoH1.png";
                    flipVertical = true;
                    muzzleX = x - 2f;
                    muzzleY = y + height - 3f;
                }
                break;

            case DIAGONAL:
                if (dy < -18f && Math.abs(dx) < 16f) {
                    // Straight up
                    currentSpriteName = "antiAreoV1.png";
                    flipVertical = false;
                    muzzleX = turretCenterX;
                    muzzleY = y - 2f;
                } else if (dy > 18f && Math.abs(dx) < 16f) {
                    // Straight down
                    currentSpriteName = "antiAreoV2.png";
                    flipVertical = false;
                    muzzleX = turretCenterX;
                    muzzleY = y + height + 2f;
                } else if (dx >= 0 && dy < 0) {
                    // Diagonal up-right
                    currentSpriteName = "antiAreoD2.png";
                    flipVertical = false;
                    muzzleX = x + width;
                    muzzleY = y;
                } else if (dx < 0 && dy < 0) {
                    // Diagonal up-left
                    currentSpriteName = "antiAreoD1.png";
                    flipVertical = false;
                    muzzleX = x;
                    muzzleY = y;
                } else if (dx >= 0 && dy >= 0) {
                    // Diagonal down-right
                    currentSpriteName = "antiAreoD2.png";
                    flipVertical = true;
                    muzzleX = x + width;
                    muzzleY = y + height;
                } else {
                    // Diagonal down-left
                    currentSpriteName = "antiAreoD1.png";
                    flipVertical = true;
                    muzzleX = x;
                    muzzleY = y + height;
                }
                break;

            case GROUND:
            default:
                // Mounted on floor, base at bottom
                flipVertical = false;
                if (Math.abs(dx) < 22f && dy < -12f) {
                    // Straight up (antiAreoV1 has base at bottom pointing up)
                    currentSpriteName = "antiAreoV1.png";
                    muzzleX = turretCenterX;
                    muzzleY = y - 2f;
                } else if (dx < -15f && dy < -15f) {
                    // Diagonal up-left
                    currentSpriteName = "antiAreoD1.png";
                    muzzleX = x;
                    muzzleY = y;
                } else if (dx > 15f && dy < -15f) {
                    // Diagonal up-right
                    currentSpriteName = "antiAreoD2.png";
                    muzzleX = x + width;
                    muzzleY = y;
                } else if (dx >= 0) {
                    // Shallow right
                    currentSpriteName = "antiAreoH2.png";
                    muzzleX = x + width + 2f;
                    muzzleY = y + 4f;
                } else {
                    // Shallow left
                    currentSpriteName = "antiAreoH1.png";
                    muzzleX = x - 2f;
                    muzzleY = y + 4f;
                }
                break;
        }
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        // Moves with level scroll and vertical shaft descent
        x -= scrollSpeed * dt;
        if (vy != 0f) {
            y += vy * dt;
        }

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        if (recoilTimer > 0f) {
            recoilTimer -= dt;
        }

        if (player.alive) {
            updateAim(player.x + player.width / 2f, player.y + player.height / 2f);
        }

        // Periodic aiming and firing at player position
        shootTimer -= dt;
        if (shootTimer <= 0f) {
            shootTimer = 1.6f + (float) (Math.random() * 0.6f);
            if (x > -15 && x < 550 && y > -40 && y < 280 && player.alive) {
                recoilTimer = 0.16f; // Trigger barrel recoil animation
                float targetX = player.x + player.width / 2f;
                float targetY = player.y + player.height / 2f;
                enemyBullets.add(Bullet.createEnemyNormal(muzzleX, muzzleY, targetX, targetY, 115f));
            }
        }

        if (x < -60 || (vy != 0f && (y > 350 || y < -1000))) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        Bitmap bmp = loader.getBitmap(currentSpriteName);
        if (bmp != null) {
            if (flipVertical) {
                bmp = loader.getFlippedBitmap(bmp, false, true);
            }
            if (bmp != null) {
                // Apply barrel recoil kickback animation into the base
                float drawY = y;
                if (recoilTimer > 0f) {
                    drawY += (mount == Mount.CEILING) ? -1.5f : 1.5f;
                }
                canvas.drawBitmap(bmp, x, drawY, getActivePaint(paint));
            }
        }
    }
}
