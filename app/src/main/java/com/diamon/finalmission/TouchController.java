package com.diamon.finalmission;

import android.view.MotionEvent;

/**
 * Gestor multitáctil para Final Mission adaptado de la arquitectura de IA Rebellion.
 * 
 * Principios:
 * 1. Primer dedo (primaryId - Movimiento Relativo 1:1):
 *    - deltaX = touchX - playerX; deltaY = touchY - playerY
 *    - Permite mover al soldado por toda la pantalla sin que el dedo tape el sprite del personaje.
 *    - Traspaso suave si se cambia de dedo.
 * 2. Segundo dedo (secondaryId - Dedo Libre / Satélites y Giro):
 *    - Mantener y arrastrar en 360°: Bloquea y orienta los 2 satélites orbitales hacia ese ángulo (emulando el botón A de la NES).
 *    - Deslizamiento rápido horizontal (< 0.40s): Invierte el sentido del jugador (mirar a derecha o izquierda).
 *    - Toque rápido (< 0.25s): Alterna el bloqueo/desbloqueo de órbita libre de los satélites.
 * 3. Disparo automático continuo durante el contacto táctil.
 * 4. 100% fiel a la ROM NES: Sin bombas ni mecánicas inexistentes en el cartucho original.
 */
public class TouchController {

    private int primaryId = -1;
    private int secondaryId = -1;

    // Arrastre relativo 1:1 (Algoritmo de Jugador.java de Danielk10)
    private float deltaXTactil = 0f;
    private float deltaYTactil = 0f;

    // Posición del dedo primario en coordenadas virtuales
    private float primaryX = 0f;
    private float primaryY = 0f;

    // Estado del dedo secundario
    private float secondaryStartX = 0f;
    private float secondaryStartY = 0f;
    private float secondaryCurrentX = 0f;
    private float secondaryCurrentY = 0f;
    private long secondaryDownTime = 0;
    private boolean secondaryHasDragged = false;

    // Estado del juego
    private boolean isTouching = false;
    private boolean isFiring = false;
    private boolean satelliteLocked = false;
    private float satelliteTargetAngle = 0f;
    private Boolean flickFacing = null; // true = derecha, false = izquierda

    public void update(float dt) {
        flickFacing = null;
    }

    public void onTouchDown(int pointerId, float x, float y, float playerX, float playerY) {
        isTouching = true;
        isFiring = true;

        if (primaryId == -1) {
            // Asignar como dedo de movimiento principal
            primaryId = pointerId;
            primaryX = x;
            primaryY = y;
            deltaXTactil = x - playerX;
            deltaYTactil = y - playerY;
        } else if (secondaryId == -1 && pointerId != primaryId) {
            // Asignar como dedo secundario (control de satélites y flick)
            secondaryId = pointerId;
            secondaryStartX = x;
            secondaryStartY = y;
            secondaryCurrentX = x;
            secondaryCurrentY = y;
            secondaryDownTime = System.currentTimeMillis();
            secondaryHasDragged = false;
        }
    }

    public void onTouchMove(int pointerId, float x, float y, Player player) {
        if (pointerId == primaryId) {
            primaryX = x;
            primaryY = y;
            // Movimiento relativo 1:1
            float targetX = x - deltaXTactil;
            float targetY = y - deltaYTactil;
            player.setPositionTarget(targetX, targetY);
        } else if (pointerId == secondaryId) {
            secondaryCurrentX = x;
            secondaryCurrentY = y;

            float dx = x - secondaryStartX;
            float dy = y - secondaryStartY;
            float distSq = dx * dx + dy * dy;

            if (distSq > 16f) { // Umbral de arrastre
                secondaryHasDragged = true;
                // Apuntado en 360 grados de los satélites
                satelliteTargetAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
                if (satelliteTargetAngle < 0) {
                    satelliteTargetAngle += 360f;
                }
                satelliteLocked = true;
            }
        }
    }

    public void onTouchUp(int pointerId, float x, float y, Player player) {
        if (pointerId == primaryId) {
            primaryId = -1;
            // Si el dedo secundario sigue en pantalla, promoverlo a dedo principal
            if (secondaryId != -1) {
                primaryId = secondaryId;
                secondaryId = -1;
                primaryX = secondaryCurrentX;
                primaryY = secondaryCurrentY;
                deltaXTactil = primaryX - player.getX();
                deltaYTactil = primaryY - player.getY();
            } else {
                isTouching = false;
                isFiring = false;
            }
        } else if (pointerId == secondaryId) {
            long duration = System.currentTimeMillis() - secondaryDownTime;
            float dx = x - secondaryStartX;
            float dy = y - secondaryStartY;

            if (!secondaryHasDragged && duration < 250) {
                // Toque rápido: Alternar bloqueo de satélites
                satelliteLocked = !satelliteLocked;
            } else if (duration < 400 && Math.abs(dx) > 20f && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                // Flick horizontal rápido: Girar soldado
                if (dx > 0) {
                    flickFacing = true;
                    player.setFacingRight(true);
                } else {
                    flickFacing = false;
                    player.setFacingRight(false);
                }
            }

            secondaryId = -1;
            secondaryHasDragged = false;
        }
    }

    public boolean isFiring() {
        return isFiring;
    }

    public boolean isSatelliteLocked() {
        return satelliteLocked;
    }

    public float getSatelliteTargetAngle() {
        return satelliteTargetAngle;
    }

    public void setSatelliteLocked(boolean locked) {
        this.satelliteLocked = locked;
    }

    public Boolean getFlickFacing() {
        return flickFacing;
    }
}
