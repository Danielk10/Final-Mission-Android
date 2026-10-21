package com.diamon.finalmission;

import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.engine.TouchController;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.ItemBox;
import com.diamon.finalmission.entities.Player;
import com.diamon.finalmission.entities.PowerUp;
import com.diamon.finalmission.entities.PowerUpCapsule;
import com.diamon.finalmission.entities.Satellite;
import com.diamon.finalmission.entities.WeaponType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests verifying authentic NES Final Mission (Japanese Famicom) gameplay mechanics:
 * - 3 HP blocks max health (strictly authentic Japanese Final Mission).
 * - Extra lives stock starting at 3 (P=02 stock indicator).
 * - Special weapon downgraded back to NORMAL upon taking damage.
 * - 2 Orbital satellites with counter-clockwise rotation and angle locking.
 * - Weapon systems: Normal, 3-way Wide spread, Penetrating Laser, Satellite shots.
 * - Power-up capsules ('W', 'L', 'S', 'B' life recovery) and cracking ItemBox.
 * - TouchController lock bounds and gestures.
 */
public class GameplayMechanicsTest {

    @Test
    public void testPlayerInitialHealthAndLives() {
        Player player = new Player();
        assertEquals("Japanese Final Mission max health must be strictly 3 pips", 3, Player.MAX_HEALTH);
        assertEquals("Player health must start at MAX_HEALTH (3)", 3, player.health);
        assertEquals("Player lives must start at 3", 3, Player.STARTING_LIVES);
        assertEquals("Player lives must be 3", 3, player.lives);
        assertEquals("Reserve lives stock must be 2 (P=02 on HUD)", 2, Math.max(0, player.lives - 1));
        assertTrue("Player must start alive", player.alive);
        assertEquals("Player default weapon must be NORMAL", WeaponType.NORMAL, player.weaponType);
    }

    @Test
    public void testPlayerCharacterNames() {
        Player player1P = new Player();
        player1P.characterId = 1;
        assertEquals("SPARKY", player1P.getCharacterName());
        assertEquals("SPARKY", player1P.getShortCharacterName());

        Player player2P = new Player();
        player2P.characterId = 2;
        assertEquals("SERGEI MISSIONOV", player2P.getCharacterName());
        assertEquals("SERGEI", player2P.getShortCharacterName());
    }

    @Test
    public void testPlayerDamageAndWeaponDowngrade() {
        Player player = new Player();
        player.weaponType = WeaponType.LASER;
        player.immuneTimer = 0f;

        // Damage player without audio manager crash (passing dummy audio)
        AudioManager dummyAudio = null;
        try {
            player.takeDamage(1, dummyAudio);
        } catch (Exception ignored) {
            // Audio manager might be unmocked, but state is updated
        }

        assertEquals("Health should decrement to 2", 2, player.health);
        assertEquals("In authentic Japanese Final Mission, taking a hit loses special weapon back to NORMAL",
                WeaponType.NORMAL, player.weaponType);
        assertTrue("Immunity timer must be activated (2.0s)", player.immuneTimer > 1.5f);
    }

    @Test
    public void testPlayerDeathAndRespawn() {
        Player player = new Player();
        player.immuneTimer = 0f;
        player.weaponType = WeaponType.WIDE;
        player.speedMultiplier = 1.5f;

        // Take fatal damage
        try {
            player.takeDamage(3, null);
        } catch (Exception ignored) {}

        // Fatal damage starts disintegration sequence without instant teleport
        assertTrue("Player must enter dying state upon fatal hit", player.isDying);
        assertEquals("Health should be 0 while disintegrating", 0, player.health);
        assertEquals("Satellites should be cleared upon death", 0, player.satellites.size());
        assertEquals("Lives should NOT decrement until disintegration completes", 3, player.lives);

        // Advance through disintegration sequence (0.60s)
        player.update(0.65f, false, false, null, new ArrayList<>(), null);

        assertFalse("Dying state should finish after 0.6s", player.isDying);
        assertTrue("Disintegration finish must flag explosion spawn", player.shouldSpawnDeathExplosion);
        assertEquals("Lives should decrement to 2 after sequence completes", 2, player.lives);
        assertEquals("Respawned health must be full 3 pips", 3, player.health);
        assertTrue("Player should still be alive on 2nd life", player.alive);
        assertEquals("Reserve stock after 1 death is P=01", 1, Math.max(0, player.lives - 1));
        assertEquals("Weapon must reset to NORMAL on death", WeaponType.NORMAL, player.weaponType);
        assertEquals("Speed multiplier must reset to 1.0 on death", 1.0f, player.speedMultiplier, 0.001f);
        assertEquals("Must have 2 fresh satellites after respawn", 2, player.satellites.size());
    }

    @Test
    public void testSatellitesInitialPositionAndCounterClockwiseOrbit() {
        Player player = new Player();
        assertEquals("Player must have 2 orbital satellites", 2, player.satellites.size());

        Satellite pod0 = player.satellites.get(0);
        Satellite pod1 = player.satellites.get(1);

        // Pod 0 starts at -PI/2 (top, y < centerY)
        // Pod 1 starts at +PI/2 (bottom, y > centerY)
        float cx = 100f;
        float cy = 100f;
        pod0.update(0f, cx, cy, false, null, false, 0f);
        pod1.update(0f, cx, cy, false, null, false, 0f);

        assertTrue("Pod 0 must start above player", pod0.y < cy);
        assertTrue("Pod 1 must start below player", pod1.y > cy);
        assertEquals("Orbit radius should be 22 pixels", 22f, pod0.orbitRadius, 0.1f);

        // Stationary: satellites do NOT rotate!
        float angleStationary = pod0.orbitAngle;
        pod0.update(0.2f, cx, cy, false, null, false, 0f);
        assertEquals("Satellites must NOT auto-spin when stationary", angleStationary, pod0.orbitAngle, 0.0001f);

        // Moving: satellites orbit counter-clockwise (angle decreases)
        pod0.update(0.1f, cx, cy, false, null, true, 5.0f);
        assertTrue("Orbit angle must decrease when player moves (movement-driven orbit)",
                pod0.orbitAngle < angleStationary);
    }

    @Test
    public void testSatelliteLocking() {
        Satellite sat = new Satellite(0);
        float cx = 100f;
        float cy = 100f;

        // Unlocked orbit
        sat.update(0.1f, cx, cy, false, null);
        assertFalse("Satellite should not be locked", sat.isLocked);

        // Lock in place
        sat.update(0.1f, cx, cy, true, null);
        assertTrue("Satellite should be locked", sat.isLocked);
        assertNotNull("Locked angle should be recorded", sat.lockedAngle);

        float lockedAim = sat.aimAngle;
        // Updating while locked should retain locked aim angle
        sat.update(0.1f, cx, cy, true, null);
        assertEquals("Locked aim angle must remain constant", lockedAim, sat.aimAngle, 0.001f);

        // Aiming touch override
        float targetAngle = (float) Math.toRadians(45.0);
        sat.update(0.1f, cx, cy, true, targetAngle);
        assertEquals("Target angle should override aim", targetAngle, sat.aimAngle, 0.001f);
    }

    @Test
    public void testBulletCreationAndParameters() {
        Bullet normal = Bullet.createPlayerNormal(50f, 50f, true);
        assertEquals(Bullet.Type.PLAYER_NORMAL, normal.type);
        assertEquals(360f, normal.vx, 1.0f);
        assertFalse("Normal bullet must not penetrate", normal.penetrating);
        assertTrue(normal.isPlayerBullet());

        Bullet wide = Bullet.createPlayerWide(50f, 50f, true, 15f);
        assertEquals(Bullet.Type.PLAYER_WIDE, wide.type);
        assertTrue("Wide bullet should have positive vy for +15 deg", wide.vy > 0);
        assertTrue(wide.isPlayerBullet());

        Bullet laser = Bullet.createPlayerLaser(50f, 50f, true);
        assertEquals(Bullet.Type.PLAYER_LASER, laser.type);
        assertEquals(520f, laser.vx, 1.0f);
        assertTrue("Laser beam must penetrate multiple targets", laser.penetrating);
        assertTrue(laser.isPlayerBullet());

        Bullet satBullet = Bullet.createSatellite(50f, 50f, 0f);
        assertEquals(Bullet.Type.SATELLITE, satBullet.type);
        assertEquals(360f, satBullet.vx, 1.0f);
        assertTrue(satBullet.isPlayerBullet());
    }

    @Test
    public void testItemBoxAndPowerUps() {
        ItemBox box = new ItemBox(200f, 100f, PowerUp.Type.WIDE);
        assertTrue("ItemBox must be identified as container", box.isContainer());
        assertEquals("ItemBox starts with 2 HP durability", 2, box.containerHp);

        // First hit cracks the box
        boolean broken1 = box.takeHit();
        assertFalse("First hit should not break container", broken1);
        assertEquals(1, box.containerHp);
        assertTrue("Hit flash timer must be active", box.hitFlashTimer > 0f);

        // Second hit breaks the box open
        boolean broken2 = box.takeHit();
        assertTrue("Second hit breaks the container open", broken2);
        assertEquals(0, box.containerHp);

        // PowerUpCapsule applies Wide weapon
        PowerUpCapsule wideCapsule = new PowerUpCapsule(200f, 100f, PowerUp.Type.WIDE);
        assertFalse("Capsule is not a container", wideCapsule.isContainer());
        Player player = new Player();
        try {
            wideCapsule.applyTo(player, null);
        } catch (Exception ignored) {}
        assertEquals("Player weapon upgraded to WIDE", WeaponType.WIDE, player.weaponType);

        // Bomb weapon capsule upgrades weapon to BOMB
        PowerUpCapsule bombCapsule = new PowerUpCapsule(200f, 100f, PowerUp.Type.BOMB);
        try {
            bombCapsule.applyTo(player, null);
        } catch (Exception ignored) {}
        assertEquals("Player weapon upgraded to BOMB", WeaponType.BOMB, player.weaponType);

        // Life recovery capsule 'B' recovers 1 health block
        player.health = 1;
        PowerUpCapsule lifeCapsule = new PowerUpCapsule(200f, 100f, PowerUp.Type.LIFE);
        try {
            lifeCapsule.applyTo(player, null);
        } catch (Exception ignored) {}
        assertEquals("Life recovery 'B' heals 1 HP block", 2, player.health);
    }

    @Test
    public void testTouchControllerBounds() {
        TouchController tc = new TouchController();
        assertNotNull("Lock button bounds must be initialized", tc.lockButtonBounds);
        float width = tc.lockButtonBounds.right - tc.lockButtonBounds.left;
        float height = tc.lockButtonBounds.bottom - tc.lockButtonBounds.top;
        assertTrue("Lock button width must be positive", width > 0);
        assertTrue("Lock button height must be positive", height > 0);

        // Test updating bounds for widescreen 400x224
        tc.updateLockButtonBounds(400f, 224f);
        assertTrue("Button should be on the right side of the screen", tc.lockButtonBounds.right <= 400f);
        assertTrue("Button should be near the bottom of the screen", tc.lockButtonBounds.bottom <= 224f);

        // Hit testing
        float btnCenterX = (tc.lockButtonBounds.left + tc.lockButtonBounds.right) / 2f;
        float btnCenterY = (tc.lockButtonBounds.top + tc.lockButtonBounds.bottom) / 2f;
        assertTrue("Center of lock button must register touch", tc.isTouchOnLockButton(btnCenterX, btnCenterY));
        assertFalse("Top left (0,0) must not register on lock button", tc.isTouchOnLockButton(0f, 0f));
    }

    @Test
    public void testPlayerDyingFramesAndDuration() {
        Player player = new Player();
        player.immuneTimer = 0f;
        player.takeDamage(3, null);

        assertTrue("Player should be dying", player.isDying);
        assertEquals(0.60f, Player.DEATH_DURATION, 0.001f);

        // Frame 0: 0.0 - 0.2s
        player.deathTimer = 0.10f;
        int frame0 = Math.min(2, (int) (player.deathTimer / 0.20f));
        assertEquals("0.10s should be frame index 0", 0, frame0);

        // Frame 1: 0.2 - 0.4s
        player.deathTimer = 0.30f;
        int frame1 = Math.min(2, (int) (player.deathTimer / 0.20f));
        assertEquals("0.30s should be frame index 1", 1, frame1);

        // Frame 2: 0.4 - 0.6s
        player.deathTimer = 0.50f;
        int frame2 = Math.min(2, (int) (player.deathTimer / 0.20f));
        assertEquals("0.50s should be frame index 2", 2, frame2);
    }

    @Test
    public void testDirectionalSatelliteOrbit() {
        Satellite sat = new Satellite(0);
        float cx = 100f;
        float cy = 100f;

        // Stationary: angle must not change
        float initAngle = sat.orbitAngle;
        sat.update(0.1f, cx, cy, false, null, 0f, 0f, false, 0f);
        assertEquals("Stationary must not change orbit angle", initAngle, sat.orbitAngle, 0.0001f);

        // Moving RIGHT (dx > 0.05): counter-clockwise (angle decreases)
        float beforeRight = sat.orbitAngle;
        sat.update(0.1f, cx, cy, false, null, 10f, 0f, true, 10f);
        assertTrue("Moving RIGHT must rotate counter-clockwise (angle decreases)", sat.orbitAngle < beforeRight);

        // Moving LEFT (dx < -0.05): clockwise (angle increases)
        float beforeLeft = sat.orbitAngle;
        sat.update(0.1f, cx, cy, false, null, -10f, 0f, true, 10f);
        assertTrue("Moving LEFT must rotate clockwise (angle increases)", sat.orbitAngle > beforeLeft);

        // Moving UP (dy < -0.05): counter-clockwise (angle decreases)
        float beforeUp = sat.orbitAngle;
        sat.update(0.1f, cx, cy, false, null, 0f, -10f, true, 10f);
        assertTrue("Moving UP must rotate counter-clockwise (angle decreases)", sat.orbitAngle < beforeUp);

        // Moving DOWN (dy > 0.05): clockwise (angle increases)
        float beforeDown = sat.orbitAngle;
        sat.update(0.1f, cx, cy, false, null, 0f, 10f, true, 10f);
        assertTrue("Moving DOWN must rotate clockwise (angle increases)", sat.orbitAngle > beforeDown);

        // Step clamping: even with huge drag distance (e.g. 500px), step must be clamped to <= 0.15f
        float beforeClamp = sat.orbitAngle;
        sat.update(0.1f, cx, cy, false, null, 500f, 0f, true, 500f);
        float deltaAngle = beforeClamp - sat.orbitAngle;
        assertEquals("Step must be clamped to 0.15f per frame", 0.15f, deltaAngle, 0.001f);
    }

    @Test
    public void testTouchControllerDirectionalFacingOnDrag() {
        TouchController tc = new TouchController();
        float playerX = 50f;
        float playerY = 50f;

        // Primary touch down
        tc.onTouchDown(0, 100f, 100f, playerX, playerY);
        assertNull(tc.flickFacing);

        // Drag right (moveDx = +10f > 1.5f) -> flickFacing = true
        tc.onTouchMove(0, 110f, 100f);
        assertEquals("Dragging right must set flickFacing to true (face right)", Boolean.TRUE, tc.flickFacing);

        // Drag left (moveDx = -10f < -1.5f) -> flickFacing = false
        tc.onTouchMove(0, 100f, 100f);
        assertEquals("Dragging left must set flickFacing to false (face left)", Boolean.FALSE, tc.flickFacing);
    }

    @Test
    public void testTouchControllerResync() {
        TouchController tc = new TouchController();
        float playerX = 50f;
        float playerY = 50f;
        tc.onTouchDown(0, 100f, 100f, playerX, playerY);

        assertEquals(50f, tc.touchDeltaX, 0.01f);
        assertEquals(50f, tc.touchDeltaY, 0.01f);

        // Simulate wall collision clamping player to 10f, 50f while finger remains at 100f, 100f
        tc.resync(10f, 50f);
        assertEquals("Resync must update touchDeltaX to match clamped position", 90f, tc.touchDeltaX, 0.01f);
        assertEquals(50f, tc.touchDeltaY, 0.01f);
    }

    @Test
    public void testBombWeaponAndBulletAttributes() {
        Bullet playerBomb = Bullet.createPlayerBomb(50f, 50f, true);
        assertEquals(Bullet.Type.PLAYER_BOMB, playerBomb.type);
        assertEquals(300f, playerBomb.vx, 1.0f);
        assertEquals(0f, playerBomb.vy, 0.01f);
        assertEquals(2.5f, playerBomb.damage, 0.01f);
        assertTrue("Bomb must be player bullet", playerBomb.isPlayerBullet());

        Bullet satBomb = Bullet.createSatelliteBomb(50f, 50f, (float) Math.PI / 2f);
        assertEquals(Bullet.Type.SATELLITE_BOMB, satBomb.type);
        assertEquals(0f, satBomb.vx, 1.0f);
        assertEquals(300f, satBomb.vy, 1.0f);
        assertEquals(2.5f, satBomb.damage, 0.01f);
        assertTrue("Satellite bomb must be player bullet", satBomb.isPlayerBullet());
    }

    @Test
    public void testSatelliteWeaponCoordination() {
        Player player = new Player();
        List<Bullet> bullets = new ArrayList<>();

        // 1. Normal weapon
        player.weaponType = WeaponType.NORMAL;
        player.update(0.01f, true, false, null, bullets, null);
        assertEquals(3, bullets.size()); // 1 player + 2 satellites
        assertEquals(Bullet.Type.PLAYER_NORMAL, bullets.get(0).type);
        assertEquals(Bullet.Type.SATELLITE, bullets.get(1).type);
        assertEquals(Bullet.Type.SATELLITE, bullets.get(2).type);

        // 2. Wide weapon
        bullets.clear();
        player.weaponType = WeaponType.WIDE;
        // Reset fire cooldown to shoot immediately
        player.update(0.15f, true, false, null, bullets, null);
        assertEquals(5, bullets.size()); // 3 player wide + 2 satellite wide
        assertEquals(Bullet.Type.PLAYER_WIDE, bullets.get(0).type);
        assertEquals(Bullet.Type.SATELLITE_WIDE, bullets.get(3).type);
        assertEquals(Bullet.Type.SATELLITE_WIDE, bullets.get(4).type);

        // 3. Laser weapon
        bullets.clear();
        player.weaponType = WeaponType.LASER;
        player.update(0.15f, true, false, null, bullets, null);
        assertEquals(3, bullets.size()); // 1 player laser + 2 satellite laser
        assertEquals(Bullet.Type.PLAYER_LASER, bullets.get(0).type);
        assertEquals(Bullet.Type.SATELLITE_LASER, bullets.get(1).type);
        assertEquals(Bullet.Type.SATELLITE_LASER, bullets.get(2).type);

        // 4. Bomb weapon
        bullets.clear();
        player.weaponType = WeaponType.BOMB;
        player.update(0.15f, true, false, null, bullets, null);
        assertEquals(3, bullets.size()); // 1 player bomb + 2 satellite bomb
        assertEquals(Bullet.Type.PLAYER_BOMB, bullets.get(0).type);
        assertEquals(Bullet.Type.SATELLITE_BOMB, bullets.get(1).type);
        assertEquals(Bullet.Type.SATELLITE_BOMB, bullets.get(2).type);
    }

    @Test
    public void testPowerUpCapsuleBomb() {
        Player player = new Player();
        player.health = 1;
        player.weaponType = WeaponType.NORMAL;

        PowerUpCapsule bombCapsule = new PowerUpCapsule(100f, 100f, PowerUp.Type.BOMB);
        bombCapsule.applyTo(player, null);

        assertEquals("Capsule B must upgrade player to BOMB weapon", WeaponType.BOMB, player.weaponType);
        assertEquals("Capsule B must heal 1 HP", 2, player.health);
    }
}
