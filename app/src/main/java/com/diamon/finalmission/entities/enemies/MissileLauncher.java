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
 * Missile Launcher enemy from Final Mission NES:
 * Armored mobile ground launcher that opens its mechanical hatch
 * (lanzaMisil 1 -> 2 -> 3) and launches vertical rising or directional
 * high-threat missiles (misilV / misilH / misilD) accompanied by trailing smoke
 * and secondary explosive impact.
 */
public class MissileLauncher extends Enemy {

    public final boolean facingRight;
    private float fireTimer = 1.8f;
    private int hatchFrame = 1;
    private boolean isLaunching = false;
    private int launchCount = 0;

    public MissileLauncher(float x, float y, boolean facingRight) {
        super(x, y, 24f, 16f, 5f, 600);
        this.facingRight = facingRight;
        this.fireTimer = 1.2f + (float) (Math.random() * 1.0f);
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Horizontal scroll with level progression
        x -= scrollSpeed * dt;

        fireTimer -= dt;

        // Progressive hatch opening animation: Frame 1 (closed) -> 2 (opening) -> 3 (open)
        if (fireTimer <= 0.65f && fireTimer > 0.35f) {
            isLaunching = true;
            hatchFrame = 2;
        } else if (fireTimer <= 0.35f && fireTimer > 0f) {
            hatchFrame = 3;
        }

        // Launch missile
        if (fireTimer <= 0f) {
            fireTimer = 2.5f + (float) (Math.random() * 0.8f);
            isLaunching = false;
            hatchFrame = 1;
            launchCount++;

            if (x > -20f && x < 550f && player.alive) {
                float launchX = facingRight ? (x + width - 6f) : (x + 6f);
                float launchY = y - 4f;

                // Alternate between vertical rising arc missile and forward directional missile
                boolean useVertical = (launchCount % 2 == 1) || (player.y < y - 40f);
                if (useVertical) {
                    enemyBullets.add(Bullet.createEnemyMissileVertical(launchX, launchY, facingRight));
                } else {
                    enemyBullets.add(Bullet.createEnemyMissileDirectional(launchX, launchY, facingRight));
                }
            }
        }

        if (x < -60f) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        String spriteName = (facingRight ? "lanzaMisilD" : "lanzaMisilI") + hatchFrame + ".png";
        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            canvas.drawBitmap(bmp, x, y, getActivePaint(paint));
        }
    }
}
