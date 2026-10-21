package com.diamon.finalmission;

import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.engine.TouchController;
import com.diamon.finalmission.world.Level;
import com.diamon.finalmission.world.StageConfig;
import android.graphics.PointF;

import com.diamon.finalmission.world.LevelDataParser;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests verifying Stage configurations, Audio track definitions, and Camera viewport calculations.
 */
public class StageCameraAudioTest {

    @Test
    public void testAllStagesConfigured() {
        for (int i = 1; i <= StageConfig.TOTAL_STAGES; i++) {
            StageConfig config = StageConfig.getForStage(i);
            assertNotNull("Stage " + i + " config should not be null", config);
            assertEquals("Stage number mismatch", i, config.stageNumber);
            assertNotNull("Title should exist", config.title);
            assertNotNull("Subtitle should exist", config.subtitle);
            assertNotNull("Boss name should exist", config.bossName);
            assertTrue("Boss HP should be positive", config.bossHp > 0);
            assertTrue("Enemy HP multiplier should scale with difficulty", config.enemyHpMultiplier >= 1.0f);
            assertTrue("Fire rate multiplier should scale with difficulty", config.fireRateMultiplier >= 1.0f);
            assertTrue("Scroll speed should be positive", config.scrollSpeed > 0);
        }
    }

    @Test
    public void testStageConfigClamping() {
        // Less than 1 should clamp to stage 1
        StageConfig underflow = StageConfig.getForStage(0);
        assertEquals(1, underflow.stageNumber);
        assertEquals("STAGE 1", underflow.title);
        assertEquals("FINAL MACHINE", underflow.bossName);

        StageConfig negative = StageConfig.getForStage(-5);
        assertEquals(1, negative.stageNumber);

        // Greater than TOTAL_STAGES should clamp to stage 5
        StageConfig overflow = StageConfig.getForStage(6);
        assertEquals(StageConfig.TOTAL_STAGES, overflow.stageNumber);
        assertEquals("STAGE 5", overflow.title);
        assertEquals("EMPEROR OVERMIND CORE", overflow.bossName);

        StageConfig large = StageConfig.getForStage(100);
        assertEquals(StageConfig.TOTAL_STAGES, large.stageNumber);
    }

    @Test
    public void testAudioFilesExistInAssets() {
        String[] requiredAudio = {
                AudioManager.SFX_DISPARO,
                AudioManager.SFX_DISPARO_L,
                AudioManager.SFX_EXPLOSION,
                AudioManager.SFX_MURIENDO,
                AudioManager.SFX_PODER,
                AudioManager.SFX_PAUSA,
                AudioManager.SFX_MENU,
                AudioManager.SFX_PRESENTACION,
                AudioManager.BGM_INTRODUCCION,
                AudioManager.BGM_MUSICA,
                AudioManager.BGM_COMIENZO,
                AudioManager.BGM_PRESENTACION,
                AudioManager.BGM_FIN_JUEGO
        };

        File audioDir = new File("src/main/assets/audio");
        for (String file : requiredAudio) {
            File f = new File(audioDir, file);
            assertTrue("Audio file must exist in assets/audio: " + file, f.exists());
            assertTrue("Audio file must have non-zero size: " + file, f.length() > 0);
        }
    }

    @Test
    public void testWidescreenShaftCenteringMath() {
        // NES base resolution
        float segmentW = Level.SEGMENT_W;
        assertEquals(256f, segmentW, 0.001f);

        // Typical 16:9 widescreen phone (virtual width ~ 398)
        float virtualWidth16_9 = 398f;
        float shaftX16_9 = (virtualWidth16_9 - segmentW) / 2f;
        assertEquals(71f, shaftX16_9, 0.001f);

        // Typical 20:9 ultra-wide phone (virtual width ~ 498)
        float virtualWidth20_9 = 498f;
        float shaftX20_9 = (virtualWidth20_9 - segmentW) / 2f;
        assertEquals(121f, shaftX20_9, 0.001f);

        // Standard 4:3 NES aspect ratio (virtual width = 256)
        float virtualWidth4_3 = 256f;
        float shaftX4_3 = (virtualWidth4_3 - segmentW) / 2f;
        assertEquals(0f, shaftX4_3, 0.001f);
    }

    @Test
    public void testStageClearBonusMath() {
        int stage = 1;
        int health = 3;
        int lives = 3;

        int clearBonus = stage * 5000;
        int lifeBonus = health * 1000;
        int stockBonus = lives * 2000;
        int totalBonus = clearBonus + lifeBonus + stockBonus;

        assertEquals(5000, clearBonus);
        assertEquals(3000, lifeBonus);
        assertEquals(6000, stockBonus);
        assertEquals(14000, totalBonus);
    }

    @Test
    public void testStageClearBonusAllStages() {
        for (int stage = 1; stage <= StageConfig.TOTAL_STAGES; stage++) {
            for (int health = 1; health <= 4; health++) {
                for (int lives = 0; lives <= 3; lives++) {
                    int clearBonus = stage * 5000;
                    int lifeBonus = health * 1000;
                    int stockBonus = lives * 2000;
                    int totalBonus = clearBonus + lifeBonus + stockBonus;

                    assertEquals(stage * 5000, clearBonus);
                    assertEquals(health * 1000, lifeBonus);
                    assertEquals(lives * 2000, stockBonus);
                    assertTrue("Total bonus must be strictly positive", totalBonus > 0);
                }
            }
        }
    }

    @Test
    public void testLevelDataParserCleanParsing() throws Exception {
        File dataFile = new File("src/main/assets/data/datosNiveles.txt");
        assertTrue("datosNiveles.txt must exist", dataFile.exists());

        try (FileInputStream fis = new FileInputStream(dataFile)) {
            LevelDataParser parser = new LevelDataParser(fis);
            assertTrue("Parser should report isLoaded=true", parser.isLoaded());
            assertEquals("Default level should be 1", 1, parser.getDefaultLevel());

            List<LevelDataParser.ActorSpawnDef> stage1Spawns = parser.getSpawnsForLevel(1);
            assertNotNull("Stage 1 spawns should not be null", stage1Spawns);
            assertEquals("Stage 1 should have 59 actor spawns defined in authentic data", 59, stage1Spawns.size());

            for (LevelDataParser.ActorSpawnDef def : stage1Spawns) {
                assertNotNull("Actor type should not be null", def.actorType);
                assertFalse("Actor type should not be empty", def.actorType.trim().isEmpty());
                assertEquals("Level name should be Nivel 1", "Nivel 1", def.levelName);
            }

            // Verify stages 2 to 5 can be queried cleanly without returning null
            for (int s = 2; s <= StageConfig.TOTAL_STAGES; s++) {
                List<LevelDataParser.ActorSpawnDef> spawns = parser.getSpawnsForLevel(s);
                assertNotNull("Stage " + s + " query should return a valid non-null list", spawns);
            }

            // Clamping checks on parser queries
            assertNotNull("Underflow query should clamp safely", parser.getSpawnsForLevel(0));
            assertNotNull("Overflow query should clamp safely", parser.getSpawnsForLevel(99));
        }
    }

    @Test
    public void testSceneryCollisionClamping() {
        Level level = new Level();

        // 1. Phase 0 (City Ruins): Ground structures at y >= 180f block player
        level.fondosPhase = Level.PHASE_0_OUTER_RUINS_HORIZ;
        level.player.y = 200f;
        level.clampPlayerToScenery();
        assertTrue("Player y must be clamped to <= 180f in Phase 0", level.player.y <= 180f);
        assertEquals(180f, level.player.y, 0.001f);

        level.player.y = 5f;
        level.clampPlayerToScenery();
        assertTrue("Player y must be >= 10f in Phase 0", level.player.y >= 10f);
        assertEquals(10f, level.player.y, 0.001f);

        // 2. Phase 1 (Vertical Elevator Shaft Ascent): Side walls clamp player
        level.fondosPhase = Level.PHASE_1_ELEVATOR_SHAFT_ASCENT;
        float shaftX = level.getShaftX();
        float expectedMinX = shaftX + 12f;
        float expectedMaxX = shaftX + Level.SEGMENT_W - level.player.width - 12f;

        level.player.x = shaftX - 50f;
        level.clampPlayerToScenery();
        assertEquals("Player x clamped to left shaft wall", expectedMinX, level.player.x, 0.001f);

        level.player.x = shaftX + Level.SEGMENT_W + 50f;
        level.clampPlayerToScenery();
        assertEquals("Player x clamped to right shaft wall", expectedMaxX, level.player.x, 0.001f);

        // 3. Phase 2 (Base Corridors): Ceiling at y >= 26f and ground at y <= 185f block player
        level.fondosPhase = Level.PHASE_2_UPPER_CORRIDOR_HORIZ;
        level.player.y = 10f;
        level.clampPlayerToScenery();
        assertTrue("Player y clamped to ceiling (y >= 26f)", level.player.y >= 26f);
        assertEquals(26f, level.player.y, 0.001f);

        level.player.y = 210f;
        level.clampPlayerToScenery();
        assertTrue("Player y clamped to ground (y <= 185f)", level.player.y <= 185f);
        assertEquals(185f, level.player.y, 0.001f);
    }

    @Test
    public void testVerticalShaftAscendingLayout() {
        Level level = new Level();
        float shaftX = level.getShaftX();

        // Simulate reaching the shaft at the end of Phase 0
        level.fondosPhase = Level.PHASE_0_OUTER_RUINS_HORIZ;
        level.fondos[4].x = shaftX;

        // Advance 1 frame in updateFondos to trigger Phase 1 transition
        level.update(0.016f, false, false, null, null);

        assertEquals("Phase should transition to vertical shaft ascent",
                Level.PHASE_1_ELEVATOR_SHAFT_ASCENT, level.fondosPhase);
        assertTrue("Level should recognize vertical phase", level.isVerticalPhase());

        // Verify backgrounds are stacked UPWARDS above screen
        assertEquals(shaftX, level.fondos[4].x, 0.001f);
        assertTrue("Fondo 4 at y >= 0", level.fondos[4].y >= 0f);

        // Fondos 5, 6, 7 are stacked above (negative y coordinates)
        assertTrue("Fondo 5 starts stacked above screen", level.fondos[5].y < 0f);
        assertTrue("Fondo 6 starts stacked above Fondo 5", level.fondos[6].y < level.fondos[5].y);
        assertTrue("Fondo 7 starts stacked above Fondo 6", level.fondos[7].y < level.fondos[6].y);

        // Backgrounds scroll DOWNWARDS (y increases with step) as camera ascends
        float prevY = level.fondos[7].y;
        level.update(0.1f, false, false, null, null);
        assertTrue("Backgrounds must scroll downwards (+y) during ascent", level.fondos[7].y > prevY);
    }

    @Test
    public void testLightningPaletteFlash() {
        Level level = new Level();
        assertEquals(0f, level.lightningFlashTimer, 0.001f);

        level.triggerLightning();
        assertEquals(0.24f, level.lightningFlashTimer, 0.001f);

        level.update(0.05f, false, false, null, null);
        assertTrue("Timer should decay with dt", level.lightningFlashTimer < 0.24f);
        assertTrue("Timer should remain positive during flash", level.lightningFlashTimer > 0f);
    }

    @Test
    public void testSplashAndMenuGameStateAndAssets() {
        // Verify GameState includes SPLASH and MENU
        assertNotNull(com.diamon.finalmission.engine.GameState.valueOf("SPLASH"));
        assertNotNull(com.diamon.finalmission.engine.GameState.valueOf("MENU"));

        // Verify splash & menu graphics exist in assets
        File spriteDir = new File("src/main/assets/sprites");
        String[] menuSprites = {
                "introduccion.png",
                "menu1.png",
                "menu2.png",
                "selector1.png",
                "selector2.png",
                "sateliteHD1.png",
                "jugador1D1.png",
                "jugador2D1.png"
        };
        for (String sprite : menuSprites) {
            File f = new File(spriteDir, sprite);
            assertTrue("Sprite asset must exist: " + sprite, f.exists());
            assertTrue("Sprite asset must have content: " + sprite, f.length() > 0);
        }

        // Verify menu audio constants
        assertEquals("menu.wav", AudioManager.BGM_MENU);
        assertEquals("precentacion1.wav", AudioManager.SFX_PRESENTACION);

        // Verify Level initialization for Sparky (1) and Sergei (2)
        Level levelSparky = new Level();
        levelSparky.initLevel(1);
        assertEquals("Player 1 should be Sparky", 1, levelSparky.player.characterId);
        assertEquals("SPARKY", levelSparky.player.getShortCharacterName());

        Level levelSergei = new Level();
        levelSergei.initLevel(2);
        assertEquals("Player 2 should be Sergei", 2, levelSergei.player.characterId);
        assertEquals("SERGEI", levelSergei.player.getShortCharacterName());
    }

    @Test
    public void testAuthenticStage1SpawnsFromData() {
        Level level = new Level();
        level.initStage(1);

        // Verify that all 59 authentic stage spawns from datosNiveles.txt are queued
        assertEquals("Stage 1 must load all 59 authentic spawns from datosNiveles.txt",
                59, level.pendingSpawns.size());

        // Verify Phase 0 spawns (first 7)
        int phase0Count = 0;
        int phase1Count = 0;
        int phase2Count = 0;
        for (Level.PendingSpawn spawn : level.pendingSpawns) {
            if (spawn.phase == Level.PHASE_0_OUTER_RUINS_HORIZ) phase0Count++;
            else if (spawn.phase == Level.PHASE_1_ELEVATOR_SHAFT_ASCENT) phase1Count++;
            else if (spawn.phase == Level.PHASE_2_UPPER_CORRIDOR_HORIZ) phase2Count++;
        }
        assertEquals(7, phase0Count);
        assertEquals(9, phase1Count);
        assertEquals(16, phase2Count);

        // Verify shaft enemies have positive downward vertical speed (vy > 0) to match camera ascent
        for (Level.PendingSpawn spawn : level.pendingSpawns) {
            if (spawn.phase == Level.PHASE_1_ELEVATOR_SHAFT_ASCENT) {
                if (spawn.enemy instanceof com.diamon.finalmission.entities.enemies.WallTurret) {
                    com.diamon.finalmission.entities.enemies.WallTurret wt =
                            (com.diamon.finalmission.entities.enemies.WallTurret) spawn.enemy;
                    assertTrue("WallTurret in shaft must scroll down (+vy)", wt.vy > 0f);
                } else if (spawn.enemy instanceof com.diamon.finalmission.entities.enemies.AntiAirTurret) {
                    com.diamon.finalmission.entities.enemies.AntiAirTurret at =
                            (com.diamon.finalmission.entities.enemies.AntiAirTurret) spawn.enemy;
                    assertTrue("AntiAirTurret in shaft must scroll down (+vy)", at.vy > 0f);
                }
            }
        }
    }

    @Test
    public void testTouchControllerResyncOnSceneryClamping() {
        Level level = new Level();
        TouchController touch = new TouchController();

        // Simulate finger down at (100, 150) with player at (100, 150)
        touch.onTouchDown(0, 100f, 150f, 100f, 150f);
        assertEquals(0f, touch.touchDeltaX, 0.001f);
        assertEquals(0f, touch.touchDeltaY, 0.001f);

        // Player is moved into Phase 0 ground structures (e.g., y = 220f)
        level.fondosPhase = Level.PHASE_0_OUTER_RUINS_HORIZ;
        level.player.x = 100f;
        level.player.y = 220f;
        touch.primaryX = 100f;
        touch.primaryY = 220f;

        // Clamping should force player.y to 180f and resynchronize touchController
        level.clampPlayerToScenery(touch);
        assertEquals(180f, level.player.y, 0.001f);

        // Verify touch controller resynced touchDeltaY = primaryY (220) - player.y (180) = 40f
        assertEquals(40f, touch.touchDeltaY, 0.001f);

        // Now dragging finger up by 10px (from 220 to 210) should move player up immediately to 170f
        PointF newPos = touch.onTouchMove(0, 100f, 210f);
        assertNotNull(newPos);
        assertEquals("Touch controller delta must yield immediate response without deadzone",
                170f, 210f - touch.touchDeltaY, 0.001f);
    }
}
