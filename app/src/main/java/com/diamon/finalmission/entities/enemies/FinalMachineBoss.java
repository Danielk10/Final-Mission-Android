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
 * Authentic biomechanical mega-fortress anchored at segment 21 (fondo21MGF.png):
 * - Shielded by rotating protective plasma spheres (bola1.png..bola16.png) orbiting the core.
 * - Plasma spheres deflect player bullets (checkOrbDeflection) UNLESS the vulnerability window is open.
 * - Cannon opens to fire multi-directional plasma bursts in 3 combat phases:
 *   * Phase 1 (HP > 50%): 3-way aimed plasma volleys.
 *   * Phase 2 (20% < HP <= 50%): Dual destructible energy spheres + center laser.
 *   * Phase 3 (HP <= 20%): Overload desperation 5-way spread barrage.
 * - Defeat sequence: multi-explosion chain across the machine structure (checkSpawnChainExplosion),
 *   sound effects, and stage clear transition.
 * - Support stage scaling (bossHp, bossName, difficulty scaling).
 */
public class FinalMachineBoss extends Enemy {

    public static final float BOSS_MAX_HP = 80f;

    // Integrated destructible wall turrets on the fortress hull
    public final WallTurret turretTop;
    public final WallTurret turretBottom;

    // Revolving defensive plasma orbs in authentic 3D perspective orbit
    public static class DefensiveOrb {
        public float angle;
        public float radiusX = 30f;
        public float radiusY = 24f;
        public float x;
        public float y;

        public DefensiveOrb(float startAngle) {
            this.angle = startAngle;
        }

        public void update(float dt, float centerX, float centerY, float rotSpeed, boolean expanded) {
            angle += rotSpeed * dt;
            float targetRx = expanded ? 42f : 30f;
            float targetRy = expanded ? 34f : 24f;
            radiusX += (targetRx - radiusX) * 0.1f;
            radiusY += (targetRy - radiusY) * 0.1f;

            x = centerX + (float) Math.cos(angle) * radiusX;
            y = centerY + (float) Math.sin(angle) * radiusY;
        }

        public RectF getBounds() {
            return new RectF(x - 6f, y - 6f, x + 6f, y + 6f);
        }

        public void render(Canvas canvas, AssetLoader loader, Paint paint) {
            // Perspective angle mapping to frames 1..16
            double normAngle = (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
            int frame = (int) ((normAngle / (2 * Math.PI)) * 16) + 1;
            frame = Math.max(1, Math.min(16, frame));

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
    public final float startY;
    public String bossName = "FINAL MACHINE";
    public float difficultyScaling = 1.0f;

    // Cannon opening and vulnerability state
    public boolean isVulnerable = false;
    public float cannonOpenProgress = 0f;
    private float attackTimer = 1.4f;
    private float vulnerableTimer = 0f;
    private boolean isFiringBurst = false;
    private float burstShotTimer = 0f;
    private int burstShotsFired = 0;

    // Defeat sequence state & chain explosions
    public boolean isDefeated = false;
    public float defeatTimer = 0f;
    private float nextExplosionTimer = 0f;
    private final float explosionInterval = 0.12f;

    public FinalMachineBoss(float x, float y) {
        this(x, y, "FINAL MACHINE", BOSS_MAX_HP, 1.0f);
    }

    public FinalMachineBoss(float x, float y, String name, float maxHp) {
        this(x, y, name, maxHp, 1.0f);
    }

    public FinalMachineBoss(float x, float y, String name, float maxHp, float difficultyScaling) {
        super(x, y, 28f, 32f, maxHp, 20000);
        this.startY = y;
        this.bossName = (name != null && !name.isEmpty()) ? name : "FINAL MACHINE";
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.difficultyScaling = Math.max(0.5f, difficultyScaling);

        // Integrated upper and lower fortress wall turrets (facing left towards player)
        this.turretTop = new WallTurret(x + 4f, 26f, true);
        this.turretBottom = new WallTurret(x + 4f, 172f, true);

        // Initialize 3 defensive plasma orbs at 120-degree intervals
        for (int i = 0; i < 3; i++) {
            float orbAngle = (float) (i * 2.0 * Math.PI / 3.0);
            orbs.add(new DefensiveOrb(orbAngle));
        }
    }

    public int getPhase() {
        if (hp > maxHp * 0.6f) return 1;
        if (hp > maxHp * 0.25f) return 2;
        return 3;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // When defeated, run chain explosion sequence
        if (isDefeated) {
            defeatTimer += dt;
            nextExplosionTimer -= dt;
            return;
        }

        // 1. Biomechanical vertical floating oscillation
        hoverTimer += dt * 2.0f;
        float hoverRange = (getPhase() == 3) ? 26f : 16f;
        y = startY + (float) Math.sin(hoverTimer) * hoverRange;

        // 2. Update defensive plasma orbs
        float coreCenterX = x + width / 2f;
        float coreCenterY = y + height / 2f;
        float orbSpeed = (getPhase() == 3) ? 3.8f : 2.4f;
        orbSpeed *= difficultyScaling;

        for (DefensiveOrb orb : orbs) {
            orb.update(dt, coreCenterX, coreCenterY, orbSpeed, isVulnerable);
        }

        // 3. Vulnerability and cannon opening cycle
        if (!isVulnerable) {
            attackTimer -= dt * difficultyScaling;

            // Cannon begins opening 0.4s before firing
            if (attackTimer <= 0.4f) {
                cannonOpenProgress = Math.min(1.0f, (0.4f - attackTimer) / 0.4f);
            }

            if (attackTimer <= 0f) {
                // Cannon fully opened: initiate vulnerability window
                isVulnerable = true;
                cannonOpenProgress = 1.0f;
                vulnerableTimer = 1.4f;
                isFiringBurst = true;
                burstShotsFired = 0;
                burstShotTimer = 0f;
                if (player.alive) {
                    executePhaseFire(player, enemyBullets, coreCenterX, coreCenterY);
                }
            }
        } else {
            // Vulnerability window active
            vulnerableTimer -= dt;

            // Cannon firing bursts
            if (isFiringBurst && player.alive) {
                burstShotTimer -= dt;
                if (burstShotTimer <= 0f) {
                    executePhaseFire(player, enemyBullets, coreCenterX, coreCenterY);
                }
            }

            if (vulnerableTimer <= 0f) {
                // Cannon closes and shield reactivates
                isVulnerable = false;
                cannonOpenProgress = 0f;
                attackTimer = 1.8f / difficultyScaling + (float) (Math.random() * 0.6f);
            }
        }
    }

    private void executePhaseFire(Player player, List<Bullet> enemyBullets, float coreX, float coreY) {
        int phase = getPhase();
        burstShotsFired++;

        switch (phase) {
            case 1: {
                // PHASE 1: 3-way aimed plasma burst
                float baseAngle = (float) Math.atan2(player.y + player.height / 2f - coreY,
                        player.x + player.width / 2f - coreX);

                enemyBullets.add(Bullet.createBossPlasma(coreX - 6f, coreY, baseAngle, 160f));
                enemyBullets.add(Bullet.createBossPlasma(coreX - 6f, coreY, baseAngle - 0.28f, 155f));
                enemyBullets.add(Bullet.createBossPlasma(coreX - 6f, coreY, baseAngle + 0.28f, 155f));

                if (burstShotsFired < 2) {
                    burstShotTimer = 0.24f;
                } else {
                    isFiringBurst = false;
                }
                break;
            }
            case 2: {
                // PHASE 2: Dual energy spheres + central plasma laser
                enemyBullets.add(Bullet.createBossEnergySphere(coreX - 8f, coreY - 18f, -125f, -22f));
                enemyBullets.add(Bullet.createBossEnergySphere(coreX - 8f, coreY + 18f, -125f, 22f));

                float angleToPlayer = (float) Math.atan2(player.y - coreY, player.x - coreX);
                enemyBullets.add(Bullet.createBossLaser(coreX - 6f, coreY, angleToPlayer, 150f));

                if (burstShotsFired < 2) {
                    burstShotTimer = 0.30f;
                } else {
                    isFiringBurst = false;
                }
                break;
            }
            case 3: {
                // PHASE 3 (OVERLOAD): 5-way desperation spread barrage
                float baseAngle = (float) Math.atan2(player.y - coreY, player.x - coreX);
                for (int i = -2; i <= 2; i++) {
                    float a = baseAngle + (i * 0.22f);
                    enemyBullets.add(Bullet.createBossBarrage(coreX - 6f, coreY, a, 155f));
                }

                if (burstShotsFired < 3) {
                    burstShotTimer = 0.22f;
                } else {
                    isFiringBurst = false;
                }
                break;
            }
        }
    }

    /**
     * Checks if a player bullet is intercepted and deflected by rotating defensive orbs.
     * DEFLECTS player bullets UNLESS the vulnerability window is open.
     */
    public boolean checkOrbDeflection(Bullet bullet, AudioManager audio) {
        if (isDefeated || orbs.isEmpty()) return false;

        // When the vulnerability window is open, player bullets pass through to damage the core
        if (isVulnerable) {
            return false;
        }

        RectF bulletRect = bullet.getBounds();
        for (DefensiveOrb orb : orbs) {
            if (RectF.intersects(bulletRect, orb.getBounds())) {
                bullet.deflect();
                if (audio != null) {
                    audio.playSound(AudioManager.SFX_DISPARO_L);
                }
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
            alive = true; // Remains active during defeat sequence
            defeatTimer = 0f;
            nextExplosionTimer = 0f;
            orbs.clear(); // Defensive orbs dissipate
            return true;
        }
        return false;
    }

    /**
     * Defeat sequence: multi-explosion chain across the machine structure.
     */
    public Explosion checkSpawnChainExplosion(AudioManager audio) {
        if (!isDefeated || defeatTimer >= 3.4f) return null;

        if (nextExplosionTimer <= 0f) {
            nextExplosionTimer = explosionInterval;
            if (audio != null) {
                audio.playSound(AudioManager.SFX_EXPLOSION);
            }

            // Staggered explosion coordinates across the fortress machine hull
            float offsetX = (float) ((Math.random() - 0.5) * 44f);
            float offsetY = (float) ((Math.random() - 0.5) * 80f);

            return new Explosion(x + width / 2f + offsetX, y + height / 2f + offsetY, Explosion.Type.BOSS);
        }
        return null;
    }

    @Override
    public RectF getBounds() {
        // Central biomechanical core hitbox
        return new RectF(x + 2f, y + 2f, x + width - 2f, y + height - 2f);
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        Paint activePaint = getActivePaint(paint);

        // 1. Central biomechanical core (maquinaFinal.png)
        Bitmap core = loader.getBitmap("maquinaFinal.png");
        if (core != null) {
            float drawX = x + (width - core.getWidth()) / 2f;
            float drawY = y + (height - core.getHeight()) / 2f;
            canvas.drawBitmap(core, drawX, drawY, activePaint);
        }

        // 2. Rotating plasma defensive orbs (bola1.png..bola16.png)
        if (!isDefeated) {
            for (DefensiveOrb orb : orbs) {
                orb.render(canvas, loader, paint);
            }
        }
    }
}
