package com.diamon.finalmission.world;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Explosion;
import com.diamon.finalmission.entities.ItemBox;
import com.diamon.finalmission.entities.Player;
import com.diamon.finalmission.entities.PowerUp;
import com.diamon.finalmission.entities.enemies.AntiAirTurret;
import com.diamon.finalmission.entities.enemies.DroneFlyer;
import com.diamon.finalmission.entities.enemies.FinalMachineBoss;
import com.diamon.finalmission.entities.enemies.JumperMech;
import com.diamon.finalmission.entities.enemies.MissileLauncher;
import com.diamon.finalmission.entities.enemies.WalkerRobot;
import com.diamon.finalmission.entities.enemies.WallTurret;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Level Manager for Final Mission Android with strict fidelity to the original NES game:
 * - Multidirectional scrolling camera progression replicating Stage 1:
 *   * Phase 0: Horizontal scrolling over destroyed cityscape (Fondos 1 to 5)
 *   * Phase 1: Vertical downward elevator shaft descent (Fondos 5 to 8)
 *   * Phase 2: Horizontal scrolling through upper base corridor (Fondos 8 to 11)
 *   * Phase 3: Vertical downward descent through intermediate shaft (Fondos 11 to 12)
 *   * Phase 4: Horizontal scrolling through subterranean junction (Fondos 12 to 14)
 *   * Phase 5: Vertical downward descent to deep mother base (Fondos 14 to 16)
 *   * Phase 6: Horizontal scrolling approach to the boss fortress (Fondos 16 to 21)
 *   * Phase 7: Boss arena lock at Fondo 21 (fondo21MGF.png) and boss battle with FinalMachineBoss
 * - Dynamic widescreen adaptation: Centered 256px elevator shafts during vertical descent
 *   with original machinery metal wall texture padding (maquinas.png) filling screen margins.
 * - Screen shake effects, atmospheric lightning bolts synchronized with SFX, and boss intro lock.
 * - Stage configurations, stage clear bonus tally calculation, and multi-stage support (Stages 1 - 5).
 */
public class Level {

    public static final int TOTAL_SCREENS = 21;
    public static final float SEGMENT_W = 256f;
    public static final float SEGMENT_H = 240f;
    public static final float BASE_SCROLL_SPEED = 28f;

    // Scroll Direction Constants
    public static final int DIR_HORIZONTAL_LEFT = 1;
    public static final int DIR_VERTICAL_DOWN = 2; // Camera descends down shaft, backgrounds move up

    // Multidirectional Scrolling Phase Identifiers
    public static final int PHASE_0_OUTER_RUINS_HORIZ = 0;
    public static final int PHASE_1_ELEVATOR_SHAFT_DESCENT = 1;
    public static final int PHASE_2_UPPER_CORRIDOR_HORIZ = 2;
    public static final int PHASE_3_SECOND_SHAFT_DESCENT = 3;
    public static final int PHASE_4_INTERMEDIATE_CORRIDOR_HORIZ = 4;
    public static final int PHASE_5_DEEP_BASE_DESCENT = 5;
    public static final int PHASE_6_FINAL_APPROACH_HORIZ = 6;
    public static final int PHASE_7_BOSS_ARENA_LOCK = 7;

    /**
     * Represents a single 256x240 NES background segment.
     */
    public static class BackgroundPiece {
        public final int index; // 0..20 corresponds to fondo1..20 and fondo21MGF
        public float x;
        public float y;
        public int direction;
        public boolean active;
        public boolean locked;

        public BackgroundPiece(int index) {
            this.index = index;
            this.x = 0f;
            this.y = 0f;
            this.direction = DIR_HORIZONTAL_LEFT;
            this.active = false;
            this.locked = false;
        }

        public String getFileName() {
            return (index == 20) ? "fondo21MGF.png" : ("fondo" + (index + 1) + ".png");
        }
    }

    // Alias for backwards compatibility
    public static class FondoPiece extends BackgroundPiece {
        public FondoPiece(int index) {
            super(index);
        }
    }

    public final FondoPiece[] fondos = new FondoPiece[TOTAL_SCREENS];
    public int fondosPhase = PHASE_0_OUTER_RUINS_HORIZ;

    public float virtualWidth = 400f;
    public float virtualHeight = 224f;
    public float scrollSpeed = BASE_SCROLL_SPEED;

    public boolean bossEncounter = false;
    public float stageTime = 0f;

    public final Player player = new Player();
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Bullet> bullets = new ArrayList<>();
    public final List<PowerUp> powerUps = new ArrayList<>();
    public final List<Explosion> explosions = new ArrayList<>();
    public FinalMachineBoss boss = null;

    public int score = 0;
    public int highScore = 50000;
    public int currentStage = 1;

    // Stage configuration and bonus tracking
    public StageConfig stageConfig = StageConfig.getForStage(1);
    public int stageClearBonus = 5000;
    public int stageLifeBonus = 0;
    public int stageStockBonus = 0;
    public float tallyProgress = 1.0f;
    public boolean tallyFinished = true;

    // Screen Shake System
    public float shakeIntensity = 0f;
    public float shakeDuration = 0f;
    public float shakeOffsetX = 0f;
    public float shakeOffsetY = 0f;

    // Atmospheric Lightning System
    private float lightningCooldown = 4.0f;
    private float lightningFlashTimer = 0f;
    private final List<PointF> lightningPoints = new ArrayList<>();
    private final Paint lightningPaint = new Paint();

    // Spawning Timers
    private float flyerWaveTimer = 3.0f;
    private float itemBoxSpawnTimer = 6.0f;
    private float groundEnemyTimer = 4.0f;

    public Level() {
        initLightningPaint();
        initStage(1);
    }

    public Level(Context context) {
        this();
    }

    private void initLightningPaint() {
        lightningPaint.setColor(0xFFE0F8FF);
        lightningPaint.setStrokeWidth(2.0f);
        lightningPaint.setStyle(Paint.Style.STROKE);
        lightningPaint.setAntiAlias(false);
    }

    public void setVirtualDimensions(int width, int height) {
        this.virtualWidth = (float) width;
        this.virtualHeight = (float) height;
        player.screenWidth = this.virtualWidth;
    }

    public void setVirtualWidth(float width) {
        setVirtualDimensions((int) width, (int) virtualHeight);
    }

    public void setScreenWidth(float width) {
        setVirtualWidth(width);
    }

    public void initLevel() {
        initStage(currentStage);
    }

    public void initLevel(int characterId) {
        player.characterId = characterId;
        initStage(currentStage);
    }

    public void restartGame() {
        currentStage = 1;
        score = 0;
        player.reset();
        initStage(1);
    }

    public void restartAfterContinue() {
        player.health = Player.MAX_HEALTH;
        player.lives = Player.STARTING_LIVES;
        player.alive = true;
        player.immuneTimer = 3.0f;
        player.x = 40f;
        player.y = 100f;
        bullets.clear();
        explosions.clear();
    }

    public void initStage(int stageNumber) {
        this.currentStage = stageNumber;
        this.stageConfig = StageConfig.getForStage(stageNumber);
        this.scrollSpeed = this.stageConfig.scrollSpeed;

        bossEncounter = false;
        stageTime = 0f;
        shakeIntensity = 0f;
        shakeDuration = 0f;

        enemies.clear();
        bullets.clear();
        powerUps.clear();
        explosions.clear();
        boss = null;

        // Initialize all 21 background segments
        for (int i = 0; i < TOTAL_SCREENS; i++) {
            fondos[i] = new FondoPiece(i);
        }

        // Phase 0: Fondos 0..4 laid out horizontally
        for (int i = 0; i < 5; i++) {
            fondos[i].x = i * SEGMENT_W;
            fondos[i].y = 0f;
            fondos[i].direction = DIR_HORIZONTAL_LEFT;
            fondos[i].active = true;
        }
        fondosPhase = PHASE_0_OUTER_RUINS_HORIZ;

        spawnInitialDefenses();

        flyerWaveTimer = 2.5f;
        itemBoxSpawnTimer = 5.0f;
        groundEnemyTimer = 4.0f;
        lightningCooldown = 3.5f;
    }

    private void spawnInitialDefenses() {
        // Initial anti-aircraft batteries along the outer line
        enemies.add(new AntiAirTurret(380f, 185f, AntiAirTurret.Mount.GROUND));
        enemies.add(new AntiAirTurret(520f, 22f, AntiAirTurret.Mount.CEILING));
        enemies.add(new AntiAirTurret(760f, 185f, AntiAirTurret.Mount.GROUND));
        enemies.add(new AntiAirTurret(940f, 185f, AntiAirTurret.Mount.GROUND));
        enemies.add(new AntiAirTurret(1100f, 22f, AntiAirTurret.Mount.CEILING));

        // Missile launcher platform
        enemies.add(new MissileLauncher(650f, 180f, false));

        // Armored wall defense turrets
        enemies.add(new WallTurret(460f, 50f, WallTurret.Side.LEFT));
        enemies.add(new WallTurret(820f, 160f, WallTurret.Side.RIGHT));
    }

    public int scoreBeforeTally = 0;

    public void calculateStageClearBonus() {
        stageClearBonus = currentStage * 5000;
        stageLifeBonus = player.health * 1000;
        stageStockBonus = Math.max(0, player.lives) * 2000;
        scoreBeforeTally = score;
        score += stageClearBonus + stageLifeBonus + stageStockBonus;
        tallyFinished = false;
        tallyProgress = 0f;
    }

    public void updateTally(float dt, AudioManager audio) {
        if (!tallyFinished) {
            float prevProgress = tallyProgress;
            tallyProgress = Math.min(1.0f, tallyProgress + dt * 1.5f);
            if ((int) (prevProgress * 12) != (int) (tallyProgress * 12)) {
                audio.playSound(AudioManager.SFX_DISPARO);
            }
            if (tallyProgress >= 1.0f) {
                tallyProgress = 1.0f;
                tallyFinished = true;
                audio.playSound(AudioManager.SFX_PODER);
            }
        }
    }

    public void triggerScreenShake(float intensity, float duration) {
        this.shakeIntensity = Math.max(this.shakeIntensity, intensity);
        this.shakeDuration = Math.max(this.shakeDuration, duration);
    }

    public boolean isVerticalPhase() {
        return fondosPhase == PHASE_1_ELEVATOR_SHAFT_DESCENT
                || fondosPhase == PHASE_3_SECOND_SHAFT_DESCENT
                || fondosPhase == PHASE_5_DEEP_BASE_DESCENT;
    }

    public float getShaftX() {
        return Math.max(0f, (virtualWidth - SEGMENT_W) / 2f);
    }

    public void update(float dt, boolean isTouching, boolean lockSatellites, Float satelliteAimAngle,
                       AudioManager audio) {
        stageTime += dt;

        // Update screen shake decay
        if (shakeDuration > 0f) {
            shakeDuration -= dt;
            shakeOffsetX = (float) ((Math.random() - 0.5) * 2.0 * shakeIntensity);
            shakeOffsetY = (float) ((Math.random() - 0.5) * 2.0 * shakeIntensity);
        } else {
            shakeIntensity = 0f;
            shakeOffsetX = 0f;
            shakeOffsetY = 0f;
        }

        // 1. Update multidirectional camera and background scrolling
        updateFondos(dt, audio);

        // 2. Atmospheric lightning effects in open sky phases
        updateLightning(dt, audio);

        // 3. Periodic enemy and power capsule spawns
        updateSpawns(dt);

        // 4. Update player entity
        player.update(dt, isTouching, lockSatellites, satelliteAimAngle, bullets, audio);

        // Restrict player inside elevator shaft during vertical segments
        float shaftX = getShaftX();
        if (isVerticalPhase()) {
            float minX = shaftX + 6f;
            float maxX = shaftX + SEGMENT_W - player.width - 6f;
            if (player.x < minX) player.x = minX;
            if (player.x > maxX) player.x = maxX;
        } else {
            float minX = 4f;
            float maxX = virtualWidth - player.width - 4f;
            if (player.x < minX) player.x = minX;
            if (player.x > maxX) player.x = maxX;
        }

        float minY = 10f;
        float maxY = virtualHeight - player.height - 10f;
        if (player.y < minY) player.y = minY;
        if (player.y > maxY) player.y = maxY;

        // 5. Update active enemies
        float currentScrollVx = (!isVerticalPhase() && !bossEncounter) ? scrollSpeed : 0f;
        for (Enemy e : enemies) {
            e.update(dt, player, bullets, currentScrollVx);
        }

        // Boss defeat chain explosion sequence
        if (boss != null && boss.isDefeated) {
            Explosion chainExp = boss.checkSpawnChainExplosion(audio);
            if (chainExp != null) {
                explosions.add(chainExp);
                triggerScreenShake(4.0f, 0.2f);
            }
        }

        // 6. Update bullets, power-ups, and explosions
        for (Bullet b : bullets) {
            b.update(dt);
        }
        for (PowerUp p : powerUps) {
            p.update(dt);
        }
        for (Explosion exp : explosions) {
            exp.update(dt);
        }

        // 7. Collisions and cleanups
        handleCollisions(audio);
        cleanupEntities();

        if (score > highScore) {
            highScore = score;
        }
    }

    /**
     * Multidirectional scrolling state machine replicating NES Final Mission Stage 1.
     */
    private void updateFondos(float dt, AudioManager audio) {
        float step = scrollSpeed * dt;
        float shaftX = getShaftX();

        switch (fondosPhase) {
            case PHASE_0_OUTER_RUINS_HORIZ: // Horizontal scrolling over outer ruins (Fondos 0 to 4)
                for (int i = 0; i < 5; i++) {
                    fondos[i].x -= step;
                }
                if (fondos[4].x <= shaftX) {
                    fondos[4].x = shaftX;
                    fondosPhase = PHASE_1_ELEVATOR_SHAFT_DESCENT;

                    // Setup vertical downward elevator shaft descent (Fondos 4 to 7)
                    // The camera descends downward into the shaft, so backgrounds move UPWARDS (y -= step)
                    fondos[4].x = shaftX; fondos[4].y = 0f; fondos[4].direction = DIR_VERTICAL_DOWN;
                    fondos[5].x = shaftX; fondos[5].y = SEGMENT_H; fondos[5].direction = DIR_VERTICAL_DOWN; fondos[5].active = true;
                    fondos[6].x = shaftX; fondos[6].y = SEGMENT_H * 2f; fondos[6].direction = DIR_VERTICAL_DOWN; fondos[6].active = true;
                    fondos[7].x = shaftX; fondos[7].y = SEGMENT_H * 3f; fondos[7].direction = DIR_VERTICAL_DOWN; fondos[7].active = true;

                    // Shaft defenses
                    enemies.add(new WallTurret(shaftX + 16f, 60f, WallTurret.Side.LEFT, -scrollSpeed));
                    enemies.add(new WallTurret(shaftX + 224f, 140f, WallTurret.Side.RIGHT, -scrollSpeed));
                }
                break;

            case PHASE_1_ELEVATOR_SHAFT_DESCENT: // Vertical downward descent through elevator shaft
                for (int i = 4; i <= 7; i++) {
                    fondos[i].y -= step;
                }
                if (fondos[7].y <= 0f) {
                    fondos[7].y = 0f;
                    fondosPhase = PHASE_2_UPPER_CORRIDOR_HORIZ;

                    // Transition to upper horizontal corridor (Fondos 7 to 10)
                    fondos[7].x = shaftX; fondos[7].y = 0f; fondos[7].direction = DIR_HORIZONTAL_LEFT;
                    fondos[8].x = shaftX + SEGMENT_W; fondos[8].y = 0f; fondos[8].direction = DIR_HORIZONTAL_LEFT; fondos[8].active = true;
                    fondos[9].x = shaftX + SEGMENT_W * 2f; fondos[9].y = 0f; fondos[9].direction = DIR_HORIZONTAL_LEFT; fondos[9].active = true;
                    fondos[10].x = shaftX + SEGMENT_W * 3f; fondos[10].y = 0f; fondos[10].direction = DIR_HORIZONTAL_LEFT; fondos[10].active = true;

                    enemies.add(new AntiAirTurret(virtualWidth + 60f, 185f, AntiAirTurret.Mount.GROUND));
                    enemies.add(new WalkerRobot(virtualWidth + 120f, 168f));
                }
                break;

            case PHASE_2_UPPER_CORRIDOR_HORIZ: // Horizontal corridor scrolling (Fondos 7 to 10)
                for (int i = 7; i <= 10; i++) {
                    fondos[i].x -= step;
                }
                if (fondos[10].x <= shaftX) {
                    fondos[10].x = shaftX;
                    fondosPhase = PHASE_3_SECOND_SHAFT_DESCENT;

                    // Transition to vertical descent into intermediate shaft (Fondos 10 and 11)
                    fondos[10].x = shaftX; fondos[10].y = 0f; fondos[10].direction = DIR_VERTICAL_DOWN;
                    fondos[11].x = shaftX; fondos[11].y = SEGMENT_H; fondos[11].direction = DIR_VERTICAL_DOWN; fondos[11].active = true;

                    enemies.add(new WallTurret(shaftX + 16f, 120f, WallTurret.Side.LEFT, -scrollSpeed));
                }
                break;

            case PHASE_3_SECOND_SHAFT_DESCENT: // Vertical descent (Fondos 10 and 11)
                for (int i = 10; i <= 11; i++) {
                    fondos[i].y -= step;
                }
                if (fondos[11].y <= 0f) {
                    fondos[11].y = 0f;
                    fondosPhase = PHASE_4_INTERMEDIATE_CORRIDOR_HORIZ;

                    // Transition to horizontal corridor (Fondos 11 to 13)
                    fondos[11].x = shaftX; fondos[11].y = 0f; fondos[11].direction = DIR_HORIZONTAL_LEFT;
                    fondos[12].x = shaftX + SEGMENT_W; fondos[12].y = 0f; fondos[12].direction = DIR_HORIZONTAL_LEFT; fondos[12].active = true;
                    fondos[13].x = shaftX + SEGMENT_W * 2f; fondos[13].y = 0f; fondos[13].direction = DIR_HORIZONTAL_LEFT; fondos[13].active = true;

                    enemies.add(new JumperMech(virtualWidth + 40f, 160f));
                    enemies.add(new AntiAirTurret(virtualWidth + 140f, 22f, AntiAirTurret.Mount.CEILING));
                }
                break;

            case PHASE_4_INTERMEDIATE_CORRIDOR_HORIZ: // Horizontal scrolling (Fondos 11 to 13)
                for (int i = 11; i <= 13; i++) {
                    fondos[i].x -= step;
                }
                if (fondos[13].x <= shaftX) {
                    fondos[13].x = shaftX;
                    fondosPhase = PHASE_5_DEEP_BASE_DESCENT;

                    // Transition to deep base vertical descent (Fondos 13 to 15)
                    fondos[13].x = shaftX; fondos[13].y = 0f; fondos[13].direction = DIR_VERTICAL_DOWN;
                    fondos[14].x = shaftX; fondos[14].y = SEGMENT_H; fondos[14].direction = DIR_VERTICAL_DOWN; fondos[14].active = true;
                    fondos[15].x = shaftX; fondos[15].y = SEGMENT_H * 2f; fondos[15].direction = DIR_VERTICAL_DOWN; fondos[15].active = true;

                    enemies.add(new WallTurret(shaftX + 16f, 80f, WallTurret.Side.LEFT, -scrollSpeed));
                    enemies.add(new WallTurret(shaftX + 224f, 150f, WallTurret.Side.RIGHT, -scrollSpeed));
                }
                break;

            case PHASE_5_DEEP_BASE_DESCENT: // Vertical descent to deep subterranean base (Fondos 13 to 15)
                for (int i = 13; i <= 15; i++) {
                    fondos[i].y -= step;
                }
                if (fondos[15].y <= 0f) {
                    fondos[15].y = 0f;
                    fondosPhase = PHASE_6_FINAL_APPROACH_HORIZ;

                    // Transition to final approach corridor toward fortress (Fondos 15 to 20)
                    fondos[15].x = shaftX; fondos[15].y = 0f; fondos[15].direction = DIR_HORIZONTAL_LEFT;
                    fondos[16].x = shaftX + SEGMENT_W; fondos[16].y = 0f; fondos[16].direction = DIR_HORIZONTAL_LEFT; fondos[16].active = true;
                    fondos[17].x = shaftX + SEGMENT_W * 2f; fondos[17].y = 0f; fondos[17].direction = DIR_HORIZONTAL_LEFT; fondos[17].active = true;
                    fondos[18].x = shaftX + SEGMENT_W * 3f; fondos[18].y = 0f; fondos[18].direction = DIR_HORIZONTAL_LEFT; fondos[18].active = true;
                    fondos[19].x = shaftX + SEGMENT_W * 4f; fondos[19].y = 0f; fondos[19].direction = DIR_HORIZONTAL_LEFT; fondos[19].active = true;
                    fondos[20].x = shaftX + SEGMENT_W * 5f; fondos[20].y = 0f; fondos[20].direction = DIR_HORIZONTAL_LEFT; fondos[20].active = true;

                    enemies.add(new MissileLauncher(virtualWidth + 100f, 178f, false));
                }
                break;

            case PHASE_6_FINAL_APPROACH_HORIZ: // Final corridor toward the boss fortress
                for (int i = 15; i <= 20; i++) {
                    if (!fondos[i].locked) {
                        fondos[i].x -= step;
                    }
                }
                // Anchor at boss fortress when Fondo 21 (fondo21MGF.png) aligns with right wall
                float anchorX = virtualWidth - SEGMENT_W;
                if (fondos[20].x <= anchorX) {
                    fondos[20].x = anchorX;
                    fondos[20].locked = true;
                    bossEncounter = true;
                    fondosPhase = PHASE_7_BOSS_ARENA_LOCK;
                    spawnBoss(audio);
                }
                break;

            case PHASE_7_BOSS_ARENA_LOCK: // Boss arena lock
                break;
        }
    }

    private void updateLightning(float dt, AudioManager audio) {
        // Atmospheric lightning active in open sky phases (Phases 0 and 2)
        if (fondosPhase == PHASE_0_OUTER_RUINS_HORIZ || fondosPhase == PHASE_2_UPPER_CORRIDOR_HORIZ) {
            lightningCooldown -= dt;
            if (lightningCooldown <= 0f) {
                lightningCooldown = 3.5f + (float) (Math.random() * 4.0);
                triggerLightning();
                audio.playSound(AudioManager.SFX_EXPLOSION);
            }
        }
        if (lightningFlashTimer > 0f) {
            lightningFlashTimer -= dt;
        }
    }

    private void triggerLightning() {
        lightningFlashTimer = 0.24f;
        lightningPoints.clear();
        float currentX = 60f + (float) (Math.random() * Math.max(100f, virtualWidth - 120f));
        float currentY = 0f;
        lightningPoints.add(new PointF(currentX, currentY));

        while (currentY < 140f) {
            currentY += 14f + (float) (Math.random() * 12f);
            currentX += (float) (Math.random() * 24f - 12f);
            lightningPoints.add(new PointF(currentX, currentY));
        }
    }

    private void updateSpawns(float dt) {
        if (bossEncounter) return;

        // 1. Flying enemy wave (3 undulating drones in formation)
        flyerWaveTimer -= dt;
        if (flyerWaveTimer <= 0f) {
            flyerWaveTimer = 6.5f + (float) (Math.random() * 3.5);
            float baseSpawnY = 40f + (float) (Math.random() * 120f);
            for (int i = 0; i < 3; i++) {
                DroneFlyer v = new DroneFlyer(virtualWidth + 20f + (i * 32f), baseSpawnY, true);
                enemies.add(v);
            }
        }

        // 2. Power-up containers (ItemBox releasing Wide, Laser, Speed, or Life)
        itemBoxSpawnTimer -= dt;
        if (itemBoxSpawnTimer <= 0f) {
            itemBoxSpawnTimer = 11.0f + (float) (Math.random() * 7.0);
            float spawnY = 40f + (float) (Math.random() * (virtualHeight - 90f));
            PowerUp.Type drop;
            double r = Math.random();
            if (r < 0.35) drop = PowerUp.Type.WIDE;
            else if (r < 0.65) drop = PowerUp.Type.LASER;
            else if (r < 0.85) drop = PowerUp.Type.SPEED;
            else drop = PowerUp.Type.LIFE;

            ItemBox box = new ItemBox(virtualWidth + 16f, spawnY, drop);
            powerUps.add(box);
        }

        // 3. Ground combatants (JumperMech / WalkerRobot)
        groundEnemyTimer -= dt;
        if (groundEnemyTimer <= 0f) {
            groundEnemyTimer = 7.0f + (float) (Math.random() * 5.0);
            if (Math.random() < 0.5) {
                enemies.add(new JumperMech(virtualWidth + 20f, 170f));
            } else {
                enemies.add(new WalkerRobot(virtualWidth + 20f, 168f));
            }
        }
    }

    private void spawnBoss(AudioManager audio) {
        if (boss == null) {
            boss = new FinalMachineBoss(virtualWidth - 62f, 75f, stageConfig.bossName, stageConfig.bossHp);
            enemies.add(boss);

            // Armored support turrets defending the boss fortress
            enemies.add(new WallTurret(virtualWidth - 62f, 26f, WallTurret.Side.LEFT));
            enemies.add(new WallTurret(virtualWidth - 62f, 172f, WallTurret.Side.LEFT));

            // Boss presentation fanfare and dramatic intro shake
            audio.playSound(AudioManager.SFX_PRESENTACION);
            triggerScreenShake(5.0f, 0.45f);
        }
    }

    private void handleCollisions(AudioManager audio) {
        RectF playerRect = player.getBounds();
        List<PowerUp> newDrops = new ArrayList<>();

        // 1. Player bullets vs Enemies, Boss Plasma Spheres, and Power Containers
        for (Bullet b : bullets) {
            if (!b.alive || !b.isPlayerBullet()) continue;
            RectF bulletRect = b.getBounds();

            // A. Boss plasma shield deflection
            if (boss != null && boss.alive && !boss.isDefeated) {
                if (boss.checkOrbDeflection(b, audio)) {
                    triggerScreenShake(1.5f, 0.1f);
                    continue;
                }
            }

            // B. Player bullet vs Enemies
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
                                (e instanceof FinalMachineBoss) ? Explosion.Type.BOSS : Explosion.Type.NORMAL));
                        triggerScreenShake(3.5f, 0.25f);
                    }
                    if (!b.penetrating) break;
                }
            }

            // C. Player bullet vs Power-up Containers
            for (PowerUp p : powerUps) {
                if (!p.alive || !p.isContainer()) continue;
                if (RectF.intersects(bulletRect, p.getBounds())) {
                    if (!b.penetrating) {
                        b.alive = false;
                    }
                    if (p.takeHit()) {
                        p.alive = false;
                        newDrops.add(new PowerUp(p.x, p.y, p.dropType));
                        score += 200;
                        audio.playSound(AudioManager.SFX_EXPLOSION);
                        explosions.add(new Explosion(p.x, p.y, Explosion.Type.NORMAL));
                    }
                    if (!b.penetrating) break;
                }
            }
        }

        // 2. Enemy bullets vs Player
        for (Bullet b : bullets) {
            if (!b.alive || b.isPlayerBullet()) continue;
            if (RectF.intersects(playerRect, b.getBounds())) {
                b.alive = false;
                player.takeDamage(1, audio);
                triggerScreenShake(2.5f, 0.15f);
            }
        }

        // 3. Enemies vs Player
        for (Enemy e : enemies) {
            if (!e.alive) continue;
            if (RectF.intersects(playerRect, e.getBounds())) {
                player.takeDamage(1, audio);
                triggerScreenShake(3.0f, 0.2f);
            }
        }

        // 4. Power-up Capsules vs Player
        for (PowerUp p : powerUps) {
            if (!p.alive) continue;
            if (RectF.intersects(playerRect, p.getBounds())) {
                if (p.isContainer()) {
                    if (p.takeHit()) {
                        p.alive = false;
                        newDrops.add(new PowerUp(p.x, p.y, p.dropType));
                        score += 200;
                        audio.playSound(AudioManager.SFX_EXPLOSION);
                        explosions.add(new Explosion(p.x, p.y, Explosion.Type.NORMAL));
                    }
                } else {
                    p.applyTo(player, audio);
                    score += 500;
                }
            }
        }

        if (!newDrops.isEmpty()) {
            powerUps.addAll(newDrops);
        }
    }

    private void cleanupEntities() {
        Iterator<Enemy> eIt = enemies.iterator();
        while (eIt.hasNext()) {
            Enemy e = eIt.next();
            if (!e.alive && !(e instanceof FinalMachineBoss && ((FinalMachineBoss) e).defeatTimer < 3.4f)) {
                eIt.remove();
            } else if (e.x < -120f || e.x > virtualWidth + 200f || e.y < -120f || e.y > virtualHeight + 120f) {
                if (!(e instanceof FinalMachineBoss)) {
                    eIt.remove();
                }
            }
        }

        Iterator<Bullet> bIt = bullets.iterator();
        while (bIt.hasNext()) {
            Bullet b = bIt.next();
            if (!b.alive || b.x < -40f || b.x > virtualWidth + 60f || b.y < -40f || b.y > virtualHeight + 40f) {
                bIt.remove();
            }
        }

        Iterator<PowerUp> pIt = powerUps.iterator();
        while (pIt.hasNext()) {
            PowerUp p = pIt.next();
            if (!p.alive || p.x < -50f || p.x > virtualWidth + 100f) {
                pIt.remove();
            }
        }

        Iterator<Explosion> expIt = explosions.iterator();
        while (expIt.hasNext()) {
            if (!expIt.next().alive) {
                expIt.remove();
            }
        }
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        canvas.save();
        canvas.translate(shakeOffsetX, shakeOffsetY);

        // 1. Render multidirectional background segments
        renderBackgrounds(canvas, loader, paint);

        // 2. Atmospheric lightning effects
        renderLightning(canvas);

        // 3. Render entities
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
        // Black space/night background
        canvas.drawColor(0xFF000000);

        // Render each active background segment currently visible on screen
        for (int i = 0; i < TOTAL_SCREENS; i++) {
            FondoPiece f = fondos[i];
            if (!f.active) continue;

            if (f.x + SEGMENT_W < -10f || f.x > virtualWidth + 10f ||
                f.y + SEGMENT_H < -10f || f.y > virtualHeight + 10f) {
                continue;
            }

            Bitmap bmp = loader.getBitmap(f.getFileName());
            if (bmp != null) {
                canvas.drawBitmap(bmp, f.x, f.y - 8f, paint);
            }
        }

        // Seamless widescreen margin padding during vertical shaft descent
        if (isVerticalPhase()) {
            renderWidescreenMargins(canvas, loader, paint);
        }
    }

    private void renderWidescreenMargins(Canvas canvas, AssetLoader loader, Paint paint) {
        float shaftX = getShaftX();
        if (shaftX <= 0f) return;

        Bitmap plating = loader.getBitmap("maquinas.png");
        if (plating == null) return;

        // Sub-rect of solid machinery wall plating from maquinas.png
        Rect srcPlating = new Rect(48, 108, 176, 236);
        float tileW = 128f;
        float tileH = 128f;

        // Pad left margin: [0 .. shaftX]
        float x = 0f;
        while (x < shaftX) {
            float blockW = Math.min(tileW, shaftX - x);
            Rect src = new Rect(srcPlating.left, srcPlating.top,
                    (int) (srcPlating.left + (srcPlating.width() * (blockW / tileW))), srcPlating.bottom);
            for (float y = 0f; y < virtualHeight; y += tileH) {
                float blockH = Math.min(tileH, virtualHeight - y);
                Rect srcY = new Rect(src.left, src.top, src.right,
                        (int) (src.top + (srcPlating.height() * (blockH / tileH))));
                canvas.drawBitmap(plating, srcY, new RectF(x, y, x + blockW, y + blockH), paint);
            }
            x += tileW;
        }

        // Pad right margin: [shaftX + SEGMENT_W .. virtualWidth]
        float rightStart = shaftX + SEGMENT_W;
        x = rightStart;
        while (x < virtualWidth) {
            float blockW = Math.min(tileW, virtualWidth - x);
            Rect src = new Rect(srcPlating.left, srcPlating.top,
                    (int) (srcPlating.left + (srcPlating.width() * (blockW / tileW))), srcPlating.bottom);
            for (float y = 0f; y < virtualHeight; y += tileH) {
                float blockH = Math.min(tileH, virtualHeight - y);
                Rect srcY = new Rect(src.left, src.top, src.right,
                        (int) (src.top + (srcPlating.height() * (blockH / tileH))));
                canvas.drawBitmap(plating, srcY, new RectF(x, y, x + blockW, y + blockH), paint);
            }
            x += tileW;
        }
    }

    private void renderLightning(Canvas canvas) {
        if (lightningFlashTimer > 0f) {
            // Ambient sky illumination (double-pulse lightning flash)
            if (lightningFlashTimer > 0.16f || (lightningFlashTimer > 0.05f && lightningFlashTimer < 0.11f)) {
                canvas.drawColor(0x55E0F8FF);
            }
            // Draw forked lightning bolt
            if (lightningPoints.size() >= 2) {
                for (int p = 0; p < lightningPoints.size() - 1; p++) {
                    PointF p1 = lightningPoints.get(p);
                    PointF p2 = lightningPoints.get(p + 1);
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, lightningPaint);
                }
            }
        }
    }
}
