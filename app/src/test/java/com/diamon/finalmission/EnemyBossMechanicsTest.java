package com.diamon.finalmission;

import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Explosion;
import com.diamon.finalmission.entities.Player;
import com.diamon.finalmission.entities.enemies.AntiAirTurret;
import com.diamon.finalmission.entities.enemies.DroneFlyer;
import com.diamon.finalmission.entities.enemies.FinalMachineBoss;
import com.diamon.finalmission.entities.enemies.JumperMech;
import com.diamon.finalmission.entities.enemies.MissileLauncher;
import com.diamon.finalmission.entities.enemies.WalkerRobot;
import com.diamon.finalmission.entities.enemies.WallTurret;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Rigorous unit tests verifying reverse-engineered Enemy and Boss mechanics
 * for authentic NES Final Mission (Natsume 1990) fidelity.
 */
public class EnemyBossMechanicsTest {

    @Test
    public void testAntiAirTurretSpecsAndRecoil() {
        AntiAirTurret turretGround = new AntiAirTurret(100f, 180f, AntiAirTurret.Mount.GROUND);
        assertEquals(3f, turretGround.hp, 0.001f);
        assertEquals(300, turretGround.scoreValue);
        assertEquals(15f, turretGround.width, 0.001f);
        assertEquals(16f, turretGround.height, 0.001f);
        assertTrue(turretGround.alive);

        AntiAirTurret turretCeiling = new AntiAirTurret(100f, 20f, AntiAirTurret.Mount.CEILING);
        assertEquals(AntiAirTurret.Mount.CEILING, turretCeiling.mount);

        Player player = new Player(100f, 60f); // Player directly above ground turret
        List<Bullet> enemyBullets = new ArrayList<>();

        // Update with player overhead
        turretGround.update(0.016f, player, enemyBullets, 0f);
        assertEquals(0f, turretGround.recoilTimer, 0.001f);

        // Force shoot trigger to verify recoil animation and aimed bullet creation
        for (int i = 0; i < 200 && enemyBullets.isEmpty(); i++) {
            turretGround.update(0.02f, player, enemyBullets, 0f);
        }

        assertFalse("Ground turret must fire bullet at player overhead", enemyBullets.isEmpty());
        assertTrue("Recoil timer must be triggered upon firing", turretGround.recoilTimer > 0f);

        Bullet b = enemyBullets.get(0);
        assertEquals(Bullet.Type.ENEMY_NORMAL, b.type);
        assertTrue("Bullet aimed at player overhead should have upward velocity", b.vy < 0f);
    }

    @Test
    public void testAntiAirTurretVerticalMotionAndFactory() {
        float shaftDescentSpeed = 45f;
        AntiAirTurret turret = AntiAirTurret.create(120f, -50f, AntiAirTurret.Mount.CEILING, shaftDescentSpeed);
        assertEquals(AntiAirTurret.Mount.CEILING, turret.mount);
        assertEquals(shaftDescentSpeed, turret.vy, 0.001f);

        Player player = new Player(120f, 100f);
        List<Bullet> enemyBullets = new ArrayList<>();

        float initialY = turret.y;
        turret.update(0.1f, player, enemyBullets, 0f);
        assertEquals("AntiAirTurret y must advance with vy * dt during vertical descent",
                initialY + (shaftDescentSpeed * 0.1f), turret.y, 0.001f);
    }

    @Test
    public void testWallTurretSidesAndPlasmaLasers() {
        // Turret on LEFT wall (Side.LEFT) should face and fire RIGHT across the screen
        WallTurret leftWallTurret = new WallTurret(20f, 80f, WallTurret.Side.LEFT);
        assertFalse("Turret mounted on LEFT wall must not face left (it faces right into screen)", leftWallTurret.facingLeft);
        assertEquals(WallTurret.TURRET_HP, leftWallTurret.hp, 0.001f);
        assertEquals(800, leftWallTurret.scoreValue);

        // Turret on RIGHT wall (Side.RIGHT) should face and fire LEFT across the screen
        WallTurret rightWallTurret = new WallTurret(240f, 80f, WallTurret.Side.RIGHT);
        assertTrue("Turret mounted on RIGHT wall must face left into screen", rightWallTurret.facingLeft);

        Player player = new Player(130f, 80f);
        List<Bullet> bulletsFromLeft = new ArrayList<>();
        List<Bullet> bulletsFromRight = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            leftWallTurret.update(0.02f, player, bulletsFromLeft, 0f);
            rightWallTurret.update(0.02f, player, bulletsFromRight, 0f);
        }

        assertFalse("Left wall turret must fire", bulletsFromLeft.isEmpty());
        assertFalse("Right wall turret must fire", bulletsFromRight.isEmpty());

        Bullet bLeft = bulletsFromLeft.get(0);
        assertEquals(Bullet.Type.ENEMY_WALL_LASER, bLeft.type);
        assertTrue("Bullet from left wall turret must travel right (vx > 0)", bLeft.vx > 0f);

        Bullet bRight = bulletsFromRight.get(0);
        assertEquals(Bullet.Type.ENEMY_WALL_LASER, bRight.type);
        assertTrue("Bullet from right wall turret must travel left (vx < 0)", bRight.vx < 0f);
    }

    @Test
    public void testWallTurretVerticalShaftDescentVelocity() {
        float shaftDescentSpeed = -30f;
        WallTurret shaftTurret = new WallTurret(50f, 100f, WallTurret.Side.LEFT, shaftDescentSpeed);
        Player player = new Player(100f, 100f);
        List<Bullet> enemyBullets = new ArrayList<>();

        float initialY = shaftTurret.y;
        shaftTurret.update(0.1f, player, enemyBullets, 0f);
        assertEquals(initialY + (shaftDescentSpeed * 0.1f), shaftTurret.y, 0.001f);
    }

    @Test
    public void testDroneFlyerWaveOscillationAndAlignedDrops() {
        DroneFlyer flyer = new DroneFlyer(200f, 60f, true, 0f);
        assertEquals(2f, flyer.hp, 0.001f);
        assertEquals(200, flyer.scoreValue);

        float startY = flyer.y;
        Player player = new Player(150f, 140f);
        List<Bullet> enemyBullets = new ArrayList<>();

        // Advance flight until flyer approaches player's horizontal corridor
        for (int i = 0; i < 80; i++) {
            flyer.update(0.016f, player, enemyBullets, 0f);
            // Y must oscillate in sinusoidal wave
            assertNotEquals("Y coordinate must oscillate along sine wave path", startY, flyer.y);
        }

        assertFalse("Drone must drop angled shot when aligned with player", enemyBullets.isEmpty());
        Bullet drop = enemyBullets.get(0);
        assertEquals(Bullet.Type.ENEMY_NORMAL, drop.type);
        assertTrue("Dropped shot must have downward velocity toward player", drop.vy > 0f);
    }

    @Test
    public void testJumperMechGroundPatrolAndApexFire() {
        JumperMech jumper = new JumperMech(150f, 170f);
        assertEquals(4f, jumper.hp, 0.001f);
        assertEquals(400, jumper.scoreValue);

        Player player = new Player(80f, 120f);
        List<Bullet> enemyBullets = new ArrayList<>();

        float initialX = jumper.x;
        // Ground patrol phase
        jumper.update(0.1f, player, enemyBullets, 0f);
        assertNotEquals("Jumper mech must patrol along ground", initialX, jumper.x);

        // Advance through jump preparation and leap
        boolean jumpedInAir = false;
        for (int i = 0; i < 150; i++) {
            jumper.update(0.02f, player, enemyBullets, 0f);
            if (jumper.y < 170f - 10f) {
                jumpedInAir = true;
            }
        }

        assertTrue("Jumper mech must leap into air", jumpedInAir);
        assertFalse("Jumper mech must fire jump projectile near apex", enemyBullets.isEmpty());
        Bullet b = enemyBullets.get(0);
        assertEquals(Bullet.Type.ENEMY_JUMPER, b.type);
        assertTrue("Jumper bullet must descend downward toward player", b.vy > 0f);
    }

    @Test
    public void testMissileLauncherHatchAnimationAndSmokeTrail() {
        MissileLauncher launcher = new MissileLauncher(150f, 180f, false);
        assertEquals(5f, launcher.hp, 0.001f);
        assertEquals(600, launcher.scoreValue);

        Player player = new Player(80f, 100f);
        List<Bullet> bullets = new ArrayList<>();

        for (int i = 0; i < 150 && bullets.isEmpty(); i++) {
            launcher.update(0.02f, player, bullets, 0f);
        }

        assertFalse("Missile launcher must launch missile", bullets.isEmpty());
        Bullet missile = bullets.get(0);
        assertEquals(Bullet.Type.ENEMY_MISSILE, missile.type);
        assertEquals(2, missile.missileHp);

        // Verify missile flight physics and animated smoke trail generation
        boolean emittedSmoke = false;
        for (int i = 0; i < 40; i++) {
            missile.update(0.02f);
            if (missile.hasPendingSmoke) {
                emittedSmoke = true;
                missile.hasPendingSmoke = false;
                assertTrue("Smoke position must be valid", missile.pendingSmokeX != 0f || missile.pendingSmokeY != 0f);
            }
        }
        assertTrue("Missile must emit trailing smoke puffs", emittedSmoke);
    }

    @Test
    public void testWalkerRobotPatrolAndBurstFire() {
        WalkerRobot walker = new WalkerRobot(180f, 168f);
        assertEquals(6f, walker.hp, 0.001f);
        assertEquals(500, walker.scoreValue);

        Player player = new Player(100f, 168f);
        List<Bullet> bullets = new ArrayList<>();

        // Advance through patrol into burst firing state
        for (int i = 0; i < 200 && bullets.size() < 3; i++) {
            walker.update(0.02f, player, bullets, 0f);
        }

        assertTrue("Walker robot must fire rapid 3-shot burst", bullets.size() >= 3);
        for (Bullet b : bullets) {
            assertEquals(Bullet.Type.ENEMY_NORMAL, b.type);
            assertTrue("Facing left robot burst must travel left", b.vx < 0f);
        }
    }

    @Test
    public void testFinalMachineBossVulnerabilityWindowAndOrbDeflection() {
        FinalMachineBoss boss = new FinalMachineBoss(200f, 75f, "FINAL MACHINE", 80f, 1.0f);
        assertEquals("FINAL MACHINE", boss.bossName);
        assertEquals(80f, boss.maxHp, 0.001f);
        assertEquals(80f, boss.hp, 0.001f);
        assertEquals(3, boss.orbs.size());
        assertFalse(boss.isDefeated);
        assertNotNull(boss.turretTop);
        assertNotNull(boss.turretBottom);

        Player player = new Player(60f, 80f);
        List<Bullet> enemyBullets = new ArrayList<>();

        // Initially cannon is closed/shielded
        assertFalse("Boss cannon should start closed/shielded", boss.isVulnerable);

        // Advance boss until vulnerability window opens
        for (int i = 0; i < 150 && !boss.isVulnerable; i++) {
            boss.update(0.02f, player, enemyBullets, 0f);
        }

        assertTrue("Boss cannon must open and become vulnerable", boss.isVulnerable);
        assertEquals(1.0f, boss.cannonOpenProgress, 0.001f);
        assertFalse("Enemy bullets should be launched from open cannon", enemyBullets.isEmpty());

        // When vulnerable, checkOrbDeflection returns false (bullets can hit core)
        Bullet playerBullet = Bullet.createPlayerNormal(boss.x + 10f, boss.y + 10f, true);
        assertFalse("Player bullets should NOT be deflected during vulnerability window",
                boss.checkOrbDeflection(playerBullet, null));
    }

    @Test
    public void testFinalMachineBossPhasesAndDefeatExplosionChain() {
        FinalMachineBoss boss = new FinalMachineBoss(200f, 75f, "FINAL MACHINE", 80f, 1.2f);
        assertEquals(1, boss.getPhase());

        // Phase 2 transition at HP <= 50%
        boss.takeDamage(35f);
        assertEquals(2, boss.getPhase());

        // Phase 3 transition at HP <= 25%
        boss.takeDamage(30f);
        assertEquals(3, boss.getPhase());

        // Defeat transition
        boolean killed = boss.takeDamage(20f);
        assertTrue(killed);
        assertTrue(boss.isDefeated);
        assertTrue("Orbs must clear upon defeat", boss.orbs.isEmpty());

        // Verify chain explosion emission
        int explosionsSpawned = 0;
        for (int i = 0; i < 100; i++) {
            boss.update(0.02f, new Player(0f, 0f), new ArrayList<>(), 0f);
            Explosion exp = boss.checkSpawnChainExplosion(null);
            if (exp != null) {
                explosionsSpawned++;
                assertEquals(Explosion.Type.BOSS, exp.type);
            }
        }

        assertTrue("Defeated boss must produce multiple chain explosions", explosionsSpawned >= 3);
    }

    @Test
    public void testExplosionTypesAndFrameLifecycles() {
        Explosion normal = new Explosion(50f, 50f, Explosion.Type.NORMAL);
        assertEquals(Explosion.Type.NORMAL, normal.type);
        assertTrue(normal.alive);

        for (int i = 0; i < 10; i++) {
            normal.update(0.04f);
        }
        assertFalse("Normal explosion should expire after 4 frames", normal.alive);

        Explosion smoke = new Explosion(50f, 50f, Explosion.Type.MISSILE_SMOKE);
        assertEquals(Explosion.Type.MISSILE_SMOKE, smoke.type);
        for (int i = 0; i < 10; i++) {
            smoke.update(0.04f);
        }
        assertFalse("Smoke particle should expire after 3 frames", smoke.alive);

        Explosion bossExp = new Explosion(50f, 50f, Explosion.Type.BOSS);
        assertEquals(Explosion.Type.BOSS, bossExp.type);
        for (int i = 0; i < 15; i++) {
            bossExp.update(0.04f);
        }
        assertFalse("Boss explosion should expire after 5 frames", bossExp.alive);
    }
}
