package com.diamon.finalmission.entities.enemies;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Explosion;
import com.diamon.finalmission.entities.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 1 Final Boss: Final Machine (FinalMachineBoss / MaquinaFinal).
 * Biomechanical mega-fortress anchored at segment 21 (fondo21MGF.png):
 * - Armored plating and integrated destructible wall turrets (maquinas.png, WallTurret).
 * - Rotating plasma defensive orbs (bola1..16.png) shielding the core and deflecting shots.
 * - 3 dynamic combat phases:
 *   * Phase 1 (HP > 50): 3-way aimed laser volleys.
 *   * Phase 2 (20 < HP <= 50): Barrages of destructible energy spheres.
 *   * Phase 3 (HP <= 20): Desperation overload spread barrages.
 * - Multi-phase chain explosion finale upon defeat (explosionB1..5.png).
 */
public class FinalMachineBoss extends Enemy {

    public static final float BOSS_MAX_HP = 80f;

    // Integrated destructible wall turrets
    public final WallTurret turretTop;
    public final WallTurret turretBottom;

    // Revolving defensive plasma orbs (bola1..16.png)
    public static class DefensiveOrb {
        public float angle;
        public float radius = 34f;
        public float x;
        public float y;
        public float animTimer = 0f;

        public DefensiveOrb(float startAngle) {
            this.angle = startAngle;
        }

        public void update(float dt, float centerX, float centerY, float rotSpeed) {
            angle += rotSpeed * dt;
            animTimer += dt;
            x = centerX + (float) Math.cos(angle) * radius;
            y = centerY + (float) Math.sin(angle) * radius;
        }

        public RectF getBounds() {
            return new RectF(x - 6f, y - 6f, x + 6f, y + 6f);
        }

        public void render(Canvas canvas, AssetLoader loader, Paint paint) {
            int frame = ((int) (animTimer * 16f) % 16) + 1;
            Bitmap orbBmp = loader.getBitmap("bola" + frame + ".png");
            if (orbBmp != null) {
                float drawX = x - orbBmp.getWidth() / 2f;
                float drawY = y - orbBmp.getHeight() / 2f;
                canvas.drawBitmap(orbBmp, drawX, drawY, paint);
            }
        }
    }

    public final List<DefensiveOrb> orbs = new ArrayList<>();

    // Hover oscillation & combat timing
    private float hoverTimer = 0f;
    private float attackTimer = 1.2f;
    private float coreAnimTimer = 0f;
    public final float startY;

    // Defeat sequence state & chain explosions
    public boolean isDefeated = false;
    public float defeatTimer = 0f;
    private float explosionInterval = 0.15f;
    private float nextExplosionTimer = 0f;
    public String bossName = "FINAL MACHINE";

    public FinalMachineBoss(float x, float y) {
        this(x, y, "FINAL MACHINE", BOSS_MAX_HP);
    }

    public FinalMachineBoss(float x, float y, String name, float maxHp) {
        super(x, y, 36f, 44f, maxHp, 20000);
        this.startY = y;
        this.bossName = (name != null && !name.isEmpty()) ? name : "FINAL MACHINE";
        this.maxHp = maxHp;
        this.hp = maxHp;

        // Instantiate top and bottom wall turrets
        this.turretTop = new WallTurret(x + 8f, 26f, true);
        this.turretBottom = new WallTurret(x + 8f, 172f, true);

        // Initialize 3 defensive orbs equidistant at 120-degree intervals
        for (int i = 0; i < 3; i++) {
            float orbAngle = (float) (i * 2.0 * Math.PI / 3.0);
            orbs.add(new DefensiveOrb(orbAngle));
        }
    }

    public int getPhase() {
        if (hp > 50f) return 1;
        if (hp > 20f) return 2;
        return 3;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        coreAnimTimer += dt;

        // Update wall turrets
        if (turretTop.alive) {
            turretTop.update(dt, player, enemyBullets, scrollSpeed);
        }
        if (turretBottom.alive) {
            turretBottom.update(dt, player, enemyBullets, scrollSpeed);
        }

        // When defeated, run chain explosion sequence
        if (isDefeated) {
            defeatTimer += dt;
            nextExplosionTimer -= dt;
            return;
        }

        // 1. Biomechanical vertical floating oscillation
        hoverTimer += dt * 2.0f;
        float hoverRange = (getPhase() == 3) ? 30f : 18f;
        y = startY + (float) Math.sin(hoverTimer) * hoverRange;

        // 2. Update defensive plasma orbs
        float coreCenterX = x + 16f;
        float coreCenterY = y + height / 2f;
        float orbSpeed = (getPhase() == 3) ? 3.6f : 2.2f;

        for (DefensiveOrb orb : orbs) {
            orb.update(dt, coreCenterX, coreCenterY, orbSpeed);
        }

        // 3. Attack phase execution
        attackTimer -= dt;
        if (attackTimer <= 0f) {
            executeAttackPhase(player, enemyBullets, coreCenterX, coreCenterY);
        }
    }

    private void executeAttackPhase(Player player, List<Bullet> enemyBullets, float coreX, float coreY) {
        if (!player.alive) return;

        int phase = getPhase();

        switch (phase) {
            case 1: {
                // PHASE 1: 3-way aimed laser volleys
                attackTimer = 1.5f;
                float angleToPlayer = (float) Math.atan2(player.y - coreY, player.x - coreX);

                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer, 160f));
                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer - 0.28f, 155f));
                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer + 0.28f, 155f));
                break;
            }
            case 2: {
                // PHASE 2: Barrages of destructible energy spheres
                attackTimer = 1.7f;
                // Dual spherical projectiles from upper and lower ports
                enemyBullets.add(Bullet.createBossEnergySphere(coreX - 10f, coreY - 20f, -120f, -25f));
                enemyBullets.add(Bullet.createBossEnergySphere(coreX - 10f, coreY + 20f, -120f, 25f));

                // Supporting center laser
                float angleToPlayer = (float) Math.atan2(player.y - coreY, player.x - coreX);
                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer, 150f));
                break;
            }
            case 3: {
                // PHASE 3 (OVERLOAD): Dense desperation arc barrage
                attackTimer = 0.85f;
                float baseAngle = (float) Math.atan2(player.y - coreY, player.x - coreX);

                for (int i = -2; i <= 2; i++) {
                    float a = baseAngle + (i * 0.22f);
                    enemyBullets.add(Bullet.createBossBarrage(coreX - 6f, coreY, a, 150f));
                }
                break;
            }
        }
    }

    /**
     * Checks if a player bullet is intercepted and deflected by rotating defensive orbs.
     * Returns true if orb deflected the shot, protecting the boss core.
     */
    public boolean checkOrbDeflection(Bullet bullet, AudioManager audio) {
        if (isDefeated || orbs.isEmpty()) return false;

        RectF bulletRect = bullet.getBounds();
        for (DefensiveOrb orb : orbs) {
            if (RectF.intersects(bulletRect, orb.getBounds())) {
                bullet.deflect();
                audio.playSound(AudioManager.SFX_DISPARO_L);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean takeDamage(float amount) {
        if (isDefeated) return false;
        hp -= amount;
        hitFlashTimer = 0.08f;
        if (hp <= 0f) {
            hp = 0f;
            isDefeated = true;
            alive = true; // Stays active during the explosion sequence
            defeatTimer = 0f;
            nextExplosionTimer = 0f;
            orbs.clear(); // Dissipate defensive orbs
            return true;
        }
        return false;
    }

    public Explosion checkSpawnChainExplosion(AudioManager audio) {
        if (!isDefeated || defeatTimer >= 3.4f) return null;

        if (nextExplosionTimer <= 0f) {
            nextExplosionTimer = explosionInterval;
            audio.playSound(AudioManager.SFX_EXPLOSION);

            // Staggered explosion coordinates across the fortress hull
            float offsetX = (float) ((Math.random() - 0.5) * 44f);
            float offsetY = (float) ((Math.random() - 0.5) * 80f);

            return new Explosion(x + 16f + offsetX, y + height / 2f + offsetY, Explosion.Type.BOSS);
        }
        return null;
    }

    @Override
    public RectF getBounds() {
        // Vulnerable central core hitbox
        return new RectF(x + 4f, y + 4f, x + width, y + height - 4f);
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        Paint activePaint = getActivePaint(paint);

        // 1. Armored hull and fortress structure (maquinas.png)
        Bitmap plating = loader.getBitmap("maquinas.png");
        if (plating != null) {
            float plateX = x - 24f;
            float plateY = y - 40f;
            canvas.drawBitmap(plating, plateX, plateY, activePaint);
        }

        // 2. Top and bottom wall turrets
        if (turretTop.alive) {
            turretTop.render(canvas, loader, paint);
        }
        if (turretBottom.alive) {
            turretBottom.render(canvas, loader, paint);
        }

        // 3. Central biomechanical core (maquinaFinal.png)
        Bitmap core = loader.getBitmap("maquinaFinal.png");
        if (core != null) {
            float drawX = x + (width - core.getWidth()) / 2f;
            float drawY = y + (height - core.getHeight()) / 2f;
            canvas.drawBitmap(core, drawX, drawY, activePaint);
        }

        // 4. Rotating plasma defensive orbs (bola1..16.png)
        if (!isDefeated) {
            for (DefensiveOrb orb : orbs) {
                orb.render(canvas, loader, paint);
            }
        }
    }
}
