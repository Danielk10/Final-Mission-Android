package com.diamon.finalmission.world;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Caja;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Explosion;
import com.diamon.finalmission.entities.Player;
import com.diamon.finalmission.entities.Poder;
import com.diamon.finalmission.entities.PowerUp;
import com.diamon.finalmission.entities.enemies.AntiAereo;
import com.diamon.finalmission.entities.enemies.LanzaMisiles;
import com.diamon.finalmission.entities.enemies.MaquinaFinal;
import com.diamon.finalmission.entities.enemies.MaquinaPared;
import com.diamon.finalmission.entities.enemies.Robot;
import com.diamon.finalmission.entities.enemies.Saltador;
import com.diamon.finalmission.entities.enemies.Volador;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de Nivel y Campaña para Final Mission en Modo Horizontal Panorámico (Widescreen).
 * Soporta las 8 fases completas con metadatos de StageConfig,
 * apariciones auténticas de contenedores de poder (Caja), cápsulas (Poder),
 * torretas de pared acorazadas (MaquinaPared) y combate contra MaquinaFinal.
 */
public class Level {

    public static final int TOTAL_SCREENS = 21;
    public static final float SCREEN_WIDTH = 256f;
    public static final float SCROLL_SPEED = 28f;

    private Context context;
    private LevelDataParser dataParser;

    public int currentStage = 1;
    public StageConfig stageConfig;

    public float virtualWidth = 400f;
    public float virtualHeight = 224f;
    public float worldScrollX = 0f;
    public boolean bossEncounter = false;
    public float stageTime = 0f;

    public final Player player = new Player();
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Bullet> bullets = new ArrayList<>();
    public final List<PowerUp> powerUps = new ArrayList<>();
    public final List<Explosion> explosions = new ArrayList<>();
    public MaquinaFinal boss = null;

    public int score = 0;
    public int highScore = 50000;

    // Bonificaciones de fin de fase y conteo (Tally)
    public int stageClearBonus = 0;
    public int stageLifeBonus = 0;
    public int stageStockBonus = 0;
    public float tallyProgress = 0f;
    public boolean tallyFinished = false;

    // Efecto de sacudida de pantalla (Screen Shake)
    public float shakeIntensity = 0f;
    public float shakeDuration = 0f;
    public float shakeOffsetX = 0f;
    public float shakeOffsetY = 0f;

    // Oleadas programadas
    private final List<SpawnTrigger> spawnTriggers = new ArrayList<>();
    private int nextSpawnIndex = 0;

    private static class SpawnTrigger {
        float triggerScrollX;
        Runnable spawnAction;

        SpawnTrigger(float triggerScrollX, Runnable spawnAction) {
            this.triggerScrollX = triggerScrollX;
            this.spawnAction = spawnAction;
        }
    }

    public Level() {
        this(null);
    }

    public Level(Context context) {
        this.context = context;
        if (context != null) {
            this.dataParser = new LevelDataParser(context);
        }
        initStage(1);
    }

    public void setVirtualDimensions(int width, int height) {
        this.virtualWidth = width;
        this.virtualHeight = height;
        player.screenWidth = width;
    }

    public void setVirtualWidth(float width) {
        this.virtualWidth = width;
        player.screenWidth = width;
    }

    public void initLevel(int characterId) {
        player.characterId = characterId;
        restartGame();
    }

    public void restartAfterContinue() {
        player.health = Player.MAX_HEALTH;
        player.lives = Player.STARTING_LIVES;
        player.alive = true;
        player.immuneTimer = 3.0f;
        player.x = 40f;
        player.y = 100f;
        bullets.clear();
        shakeIntensity = 0f;
    }

    public void restartGame() {
        score = 0;
        player.reset();
        initStage(1);
    }

    public void initStage(int stageNumber) {
        this.currentStage = stageNumber;
        this.stageConfig = StageConfig.getForStage(stageNumber);

        worldScrollX = 0f;
        bossEncounter = false;
        stageTime = 0f;
        shakeIntensity = 0f;
        shakeDuration = 0f;

        stageClearBonus = 0;
        stageLifeBonus = 0;
        stageStockBonus = 0;
        tallyProgress = 0f;
        tallyFinished = false;

        player.screenWidth = virtualWidth;
        player.x = 40f;
        player.y = 100f;
        player.immuneTimer = 2.0f;

        enemies.clear();
        bullets.clear();
        powerUps.clear();
        explosions.clear();
        boss = null;

        initSpawnSchedule();
    }

    public void calculateStageClearBonus() {
        stageClearBonus = currentStage * 10000;
        stageLifeBonus = player.health * 1000;
        stageStockBonus = player.lives * 2000;
        tallyProgress = 0f;
        tallyFinished = false;
    }

    public void updateTally(float dt, AudioManager audio) {
        if (tallyFinished) return;

        tallyProgress += dt * 1.5f;
        if (tallyProgress >= 1.0f) {
            tallyProgress = 1.0f;
            tallyFinished = true;
            score += stageClearBonus + stageLifeBonus + stageStockBonus;
            audio.playSound(AudioManager.SFX_PODER);
        }
    }

    private void initSpawnSchedule() {
        spawnTriggers.clear();
        nextSpawnIndex = 0;

        float spawnX = virtualWidth + 30f;

        // Oleadas progresivas con Cajas de poder (Caja) y torretas (MaquinaPared)
        addTrigger(60f, () -> spawnVoladorWave(spawnX, 60f, 3));
        addTrigger(180f, () -> enemies.add(new AntiAereo(spawnX, 185f, AntiAereo.Mount.GROUND)));
        addTrigger(260f, () -> powerUps.add(new Caja(spawnX, 95f, PowerUp.Type.WIDE)));
        addTrigger(340f, () -> spawnVoladorWave(spawnX, 90f, 4));

        addTrigger(520f, () -> enemies.add(new Saltador(spawnX, 175f)));
        addTrigger(680f, () -> enemies.add(new AntiAereo(spawnX, 25f, AntiAereo.Mount.CEILING)));
        addTrigger(760f, () -> enemies.add(new AntiAereo(spawnX, 185f, AntiAereo.Mount.GROUND)));
        addTrigger(880f, () -> spawnVoladorWave(spawnX, 70f, 4));
        addTrigger(980f, () -> powerUps.add(new Caja(spawnX, 80f, PowerUp.Type.SPEED)));

        addTrigger(1100f, () -> enemies.add(new Robot(spawnX, 165f)));
        addTrigger(1250f, () -> enemies.add(new MaquinaPared(spawnX, 35f, true)));
        addTrigger(1350f, () -> powerUps.add(new Caja(spawnX, 100f, PowerUp.Type.LASER)));
        addTrigger(1480f, () -> spawnVoladorWave(spawnX, 120f, 4));
        addTrigger(1600f, () -> enemies.add(new Saltador(spawnX, 175f)));

        addTrigger(1800f, () -> enemies.add(new LanzaMisiles(spawnX, 180f, false)));
        addTrigger(1950f, () -> enemies.add(new AntiAereo(spawnX, 140f, AntiAereo.Mount.DIAGONAL)));
        addTrigger(2100f, () -> spawnVoladorWave(spawnX, 50f, 4));
        addTrigger(2250f, () -> powerUps.add(new Caja(spawnX, 110f, PowerUp.Type.LIFE)));

        addTrigger(2600f, () -> enemies.add(new Robot(spawnX, 165f)));
        addTrigger(2750f, () -> enemies.add(new MaquinaPared(spawnX, 170f, true)));
        addTrigger(2900f, () -> enemies.add(new LanzaMisiles(spawnX, 180f, false)));
        addTrigger(3050f, () -> powerUps.add(new Caja(spawnX, 90f, PowerUp.Type.WIDE)));
        addTrigger(3250f, () -> spawnVoladorWave(spawnX, 130f, 4));
        addTrigger(3450f, () -> enemies.add(new Saltador(spawnX, 175f)));

        addTrigger(3800f, () -> enemies.add(new AntiAereo(spawnX, 25f, AntiAereo.Mount.CEILING)));
        addTrigger(3950f, () -> enemies.add(new LanzaMisiles(spawnX, 180f, false)));
        addTrigger(4150f, () -> enemies.add(new Robot(spawnX, 165f)));
        addTrigger(4300f, () -> powerUps.add(new Caja(spawnX, 100f, PowerUp.Type.LIFE)));
        addTrigger(4550f, () -> spawnVoladorWave(spawnX, 80f, 5));
        addTrigger(4800f, () -> enemies.add(new Saltador(spawnX, 175f)));

        // Pantalla 21: Llegada a la fortaleza y combate contra MaquinaFinal
        addTrigger(5050f, () -> {
            bossEncounter = true;
            float bossAnchorX = virtualWidth - 55f;
            boss = new MaquinaFinal(bossAnchorX, 112f);
            enemies.add(boss);
            enemies.add(boss.turretTop);
            enemies.add(boss.turretBottom);
        });
    }

    private void addTrigger(float scrollX, Runnable action) {
        spawnTriggers.add(new SpawnTrigger(scrollX, action));
    }

    private void spawnVoladorWave(float startX, float startY, int count) {
        for (int i = 0; i < count; i++) {
            final float delayOffset = i * 22f;
            enemies.add(new Volador(startX + delayOffset + 60f, startY, true));
        }
    }

    public void triggerScreenShake(float intensity, float duration) {
        this.shakeIntensity = intensity;
        this.shakeDuration = duration;
    }

    public void update(float dt, boolean isTouching, boolean lockSatellites, Float satelliteAimAngle,
                       AudioManager audio) {
        stageTime += dt;

        if (shakeDuration > 0f) {
            shakeDuration -= dt;
            shakeOffsetX = (float) ((Math.random() - 0.5) * 2.0 * shakeIntensity);
            shakeOffsetY = (float) ((Math.random() - 0.5) * 2.0 * shakeIntensity);
        } else {
            shakeOffsetX = 0f;
            shakeOffsetY = 0f;
        }

        float currentScrollSpeed = SCROLL_SPEED;
        float maxScroll = (TOTAL_SCREENS * SCREEN_WIDTH) - virtualWidth;
        if (!bossEncounter) {
            worldScrollX += currentScrollSpeed * dt;
            if (worldScrollX >= maxScroll) {
                worldScrollX = maxScroll;
            }
        } else {
            currentScrollSpeed = 0f;
            if (worldScrollX < maxScroll) {
                worldScrollX = Math.min(maxScroll, worldScrollX + SCROLL_SPEED * 1.5f * dt);
            }
        }

        while (nextSpawnIndex < spawnTriggers.size() &&
                worldScrollX >= spawnTriggers.get(nextSpawnIndex).triggerScrollX) {
            spawnTriggers.get(nextSpawnIndex).spawnAction.run();
            nextSpawnIndex++;
        }

        player.update(dt, isTouching, lockSatellites, satelliteAimAngle, bullets, audio);

        for (Enemy e : enemies) {
            e.update(dt, player, bullets, currentScrollSpeed);
        }

        // Si el jefe está derrotado, generar la secuencia de explosiones en cadena
        if (boss != null && boss.isDefeated) {
            Explosion chainExp = boss.checkSpawnChainExplosion(audio);
            if (chainExp != null) {
                explosions.add(chainExp);
                triggerScreenShake(4.0f, 0.2f);
            }
        }

        for (Bullet b : bullets) {
            b.update(dt);
        }

        for (PowerUp p : powerUps) {
            p.update(dt);
        }

        for (Explosion exp : explosions) {
            exp.update(dt);
        }

        handleCollisions(audio);
        cleanupEntities();

        if (score > highScore) {
            highScore = score;
        }
    }

    private void handleCollisions(AudioManager audio) {
        RectF playerRect = player.getBounds();

        // 1. Proyectiles del jugador vs Enemigos, Orbes defensivos y Cajas
        for (Bullet b : bullets) {
            if (!b.alive || !b.isPlayerBullet()) continue;
            RectF bulletRect = b.getBounds();

            // A. Comprobar si el proyectil es desviado por los orbes de plasma del jefe
            if (boss != null && boss.alive && !boss.isDefeated) {
                if (boss.checkOrbDeflection(b, audio)) {
                    triggerScreenShake(1.5f, 0.1f);
                    continue;
                }
            }

            // B. Proyectil vs Enemigos (incluye núcleo del jefe y torretas de pared)
            for (Enemy e : enemies) {
                if (!e.alive) continue;
                if (RectF.intersects(bulletRect, e.getBounds())) {
                    boolean killed = e.takeDamage(b.damage);
                    if (!b.penetrating) {
                        b.alive = false;
                    }

                    if (killed) {
                        score += e.scoreValue;
                        audio.playSound(AudioManager.SFX_EXPLOSION);
                        explosions.add(new Explosion(e.x + e.width / 2f, e.y + e.height / 2f,
                                (e instanceof MaquinaFinal) ? Explosion.Type.BOSS : Explosion.Type.NORMAL));
                        triggerScreenShake(3.5f, 0.25f);
                    }
                    if (!b.penetrating) break;
                }
            }

            // C. Proyectil del jugador vs Misiles destructibles
            for (Bullet eb : bullets) {
                if (!eb.alive || eb.type != Bullet.Type.ENEMY_MISSILE) continue;
                if (RectF.intersects(bulletRect, eb.getBounds())) {
                    eb.missileHp--;
                    if (!b.penetrating) b.alive = false;
                    if (eb.missileHp <= 0) {
                        eb.alive = false;
                        score += 150;
                        audio.playSound(AudioManager.SFX_EXPLOSION);
                        explosions.add(new Explosion(eb.x, eb.y, Explosion.Type.MISSILE));
                    }
                    if (!b.penetrating) break;
                }
            }

            // D. Proyectil del jugador vs Esferas de energía destructibles del jefe (Fase 2)
            for (Bullet eb : bullets) {
                if (!eb.alive || eb.type != Bullet.Type.BOSS_ENERGY_SPHERE) continue;
                if (RectF.intersects(bulletRect, eb.getBounds())) {
                    eb.sphereHp--;
                    if (!b.penetrating) b.alive = false;
                    if (eb.sphereHp <= 0) {
                        eb.alive = false;
                        score += 100;
                        audio.playSound(AudioManager.SFX_EXPLOSION);
                        explosions.add(new Explosion(eb.x, eb.y, Explosion.Type.NORMAL));
                    }
                    if (!b.penetrating) break;
                }
            }

            // E. Proyectil del jugador vs Caja de poder (Canister)
            for (PowerUp p : powerUps) {
                if (!p.alive || !p.isContainer()) continue;
                if (RectF.intersects(bulletRect, p.getBounds())) {
                    if (!b.penetrating) b.alive = false;
                    boolean broken = p.takeHit();
                    if (broken) {
                        p.alive = false;
                        score += 200;
                        audio.playSound(AudioManager.SFX_EXPLOSION);
                        explosions.add(new Explosion(p.x, p.y, Explosion.Type.NORMAL));

                        // Liberar cápsula de poder oscilatoria auténtica (W, L, S o B)
                        powerUps.add(new Poder(p.x, p.y, p.dropType));
                    }
                    if (!b.penetrating) break;
                }
            }
        }

        // 2. Proyectiles enemigos vs Jugador
        if (player.alive && player.immuneTimer <= 0f) {
            for (Bullet b : bullets) {
                if (!b.alive || !b.isEnemyBullet()) continue;
                if (RectF.intersects(playerRect, b.getBounds())) {
                    b.alive = false;
                    player.takeDamage(1, audio);
                    triggerScreenShake(4f, 0.35f);
                    explosions.add(new Explosion(player.x + player.width / 2f, player.y + player.height / 2f, Explosion.Type.NORMAL));
                    break;
                }
            }
        }

        // 3. Colisión física Jugador vs Enemigos
        if (player.alive && player.immuneTimer <= 0f) {
            for (Enemy e : enemies) {
                if (!e.alive) continue;
                if (RectF.intersects(playerRect, e.getBounds())) {
                    player.takeDamage(1, audio);
                    triggerScreenShake(4f, 0.35f);
                    explosions.add(new Explosion(player.x + player.width / 2f, player.y + player.height / 2f, Explosion.Type.NORMAL));
                    break;
                }
            }
        }

        // 4. Jugador vs Cápsulas de poder (Poder)
        if (player.alive) {
            for (PowerUp p : powerUps) {
                if (!p.alive || p.isContainer()) continue;
                if (RectF.intersects(playerRect, p.getBounds())) {
                    p.applyTo(player, audio);
                    score += 500;
                    break;
                }
            }
        }
    }

    private void cleanupEntities() {
        enemies.removeIf(e -> {
            if (e instanceof MaquinaFinal) {
                MaquinaFinal b = (MaquinaFinal) e;
                return b.isDefeated && b.defeatTimer >= 3.6f;
            }
            return !e.alive || e.isOffscreen();
        });
        bullets.removeIf(b -> !b.alive);
        powerUps.removeIf(p -> !p.alive);
        explosions.removeIf(exp -> !exp.alive);
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        canvas.save();
        canvas.translate(shakeOffsetX, shakeOffsetY);

        renderBackgrounds(canvas, loader, paint);

        for (PowerUp p : powerUps) {
            p.render(canvas, loader, paint);
        }

        for (Enemy e : enemies) {
            e.render(canvas, loader, paint);
        }

        player.render(canvas, loader, paint);

        for (Bullet b : bullets) {
            b.render(canvas, loader, paint);
        }

        for (Explosion exp : explosions) {
            exp.render(canvas, loader, paint);
        }

        canvas.restore();
    }

    private void renderBackgrounds(Canvas canvas, AssetLoader loader, Paint paint) {
        int screenIndex = (int) (worldScrollX / SCREEN_WIDTH);
        if (screenIndex < 0) screenIndex = 0;

        int numScreensToDraw = (int) Math.ceil(virtualWidth / SCREEN_WIDTH) + 1;

        for (int i = screenIndex; i <= screenIndex + numScreensToDraw && i < TOTAL_SCREENS; i++) {
            String fondoName = (i == TOTAL_SCREENS - 1) ? "fondo21MGF.png" : "fondo" + (i + 1) + ".png";
            Bitmap fondoBmp = loader.getBitmap(fondoName);

            if (fondoBmp != null) {
                float drawX = (i * SCREEN_WIDTH) - worldScrollX;
                canvas.drawBitmap(fondoBmp, drawX, -8f, paint);
            }
        }
    }
}
