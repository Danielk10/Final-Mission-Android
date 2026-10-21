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
 * Enemigo LanzaMisiles de Final Mission NES:
 * Plataforma blindada que abre su escotilla y dispara misiles guiados de alta potencia.
 */
public class LanzaMisiles extends Enemy {

    private final boolean facingRight;
    private float fireTimer = 1.8f;
    private int hatchFrame = 1;
    private float hatchTimer = 0f;
    private boolean isLaunching = false;

    public LanzaMisiles(float x, float y, boolean facingRight) {
        super(x, y, 24f, 16f, 5f, 600);
        this.facingRight = facingRight;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        x -= scrollSpeed * dt;

        fireTimer -= dt;
        if (fireTimer <= 0.6f && !isLaunching) {
            isLaunching = true;
            hatchFrame = 2; // Apertura de compuerta
        }

        if (fireTimer <= 0.3f && hatchFrame == 2) {
            hatchFrame = 3; // Totalmente abierto
        }

        if (fireTimer <= 0f) {
            fireTimer = 2.4f + (float) (Math.random() * 0.8f);
            isLaunching = false;
            hatchFrame = 1;

            if (x > 0 && x < 250 && player.alive) {
                float launchX = facingRight ? (x + width - 4) : (x + 4);
                float launchY = y - 4f;
                enemyBullets.add(Bullet.createEnemyMissile(launchX, launchY, facingRight));
            }
        }

        if (x < -40) {
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
