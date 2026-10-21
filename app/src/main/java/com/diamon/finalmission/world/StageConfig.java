package com.diamon.finalmission.world;

/**
 * Configuración y metadatos de cada una de las 8 fases (Nivel 1 a Nivel 8) de Final Mission.
 */
public class StageConfig {

    public final int stageNumber;
    public final String title;
    public final String subtitle;
    public final String bossName;
    public final float bossHp;
    public final float enemyHpMultiplier;
    public final float fireRateMultiplier;
    public final float scrollSpeed;

    public StageConfig(int stageNumber, String title, String subtitle, String bossName,
                       float bossHp, float enemyHpMultiplier, float fireRateMultiplier, float scrollSpeed) {
        this.stageNumber = stageNumber;
        this.title = title;
        this.subtitle = subtitle;
        this.bossName = bossName;
        this.bossHp = bossHp;
        this.enemyHpMultiplier = enemyHpMultiplier;
        this.fireRateMultiplier = fireRateMultiplier;
        this.scrollSpeed = scrollSpeed;
    }

    public static StageConfig getForStage(int stageNumber) {
        switch (stageNumber) {
            case 1:
                return new StageConfig(1, "STAGE 1", "DESTROYED METROPOLIS",
                        "MAQUINA FINAL", 60f, 1.0f, 1.0f, 30f);
            case 2:
                return new StageConfig(2, "STAGE 2", "AERIAL BATTLE FLEET",
                        "HEAVY BATTLECRUISER", 75f, 1.15f, 1.15f, 32f);
            case 3:
                return new StageConfig(3, "STAGE 3", "SUBTERRANEAN LAVA MINES",
                        "MAGMA DRILL CORE", 90f, 1.30f, 1.25f, 32f);
            case 4:
                return new StageConfig(4, "STAGE 4", "ORBITAL SPACE ELEVATOR",
                        "ORBITAL GUARDIAN MECH", 105f, 1.45f, 1.35f, 34f);
            case 5:
                return new StageConfig(5, "STAGE 5", "ASTEROID DEFENSE NETWORK",
                        "SATELLITE FORTRESS", 120f, 1.60f, 1.45f, 34f);
            case 6:
                return new StageConfig(6, "STAGE 6", "BIOMECHANICAL ALIEN HIVE",
                        "BIO-HIVE EMPRESS", 135f, 1.75f, 1.55f, 36f);
            case 7:
                return new StageConfig(7, "STAGE 7", "POWER GENERATOR MATRIX",
                        "REACTOR DEFENSE MATRIX", 150f, 1.90f, 1.65f, 36f);
            case 8:
            default:
                return new StageConfig(8, "STAGE 8", "FINAL MISSION - MOTHER CORE",
                        "EMPEROR OVERMIND CORE", 175f, 2.05f, 1.80f, 38f);
        }
    }
}
