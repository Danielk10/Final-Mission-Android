package com.diamon.finalmission.engine;

import android.graphics.PointF;
import android.view.MotionEvent;

/**
 * Gestor táctil multitáctil para Final Mission.
 * Adaptado directamente de AI-Rebellion (touch.rs) con fidelidad estricta al clásico de NES:
 * - Dedo primario (movimiento): Arrastre relativo 1:1 (deltaX, deltaY) para que el dedo NUNCA tape al jugador.
 *   Handoff suave si el jugador alterna dedos.
 * - Dedo secundario (libre):
 *   * Arrastre 360°: apunta y bloquea los satélites en cualquier ángulo.
 *   * Flick horizontal (< 0.40s): cambia la dirección del jugador (izquierda / derecha).
 *   * Toque rápido (< 0.40s, sin arrastre): conmuta el modo bloqueo/órbita de los satélites.
 *   * Sin bombas (NES auténtico).
 * - Disparo continuo automático mientras se toca la pantalla.
 */
public class TouchController {

    public int primaryId = -1;
    public int secondaryId = -1;

    public float deltaXTactil = 0f;
    public float deltaYTactil = 0f;
    public boolean touchInitialized = false;

    public float primaryX = 0f;
    public float primaryY = 0f;
    public float secondaryX = 0f;
    public float secondaryY = 0f;

    public float secondaryOriginX = 0f;
    public float secondaryOriginY = 0f;
    public float secondaryStartX = 0f;
    public float secondaryStartY = 0f;
    public float secondaryTouchTime = 0f;
    public boolean secondaryHasDragged = false;

    public boolean isTouching = false;
    public boolean isFiring = false;

    public boolean satelliteLock = false;
    public Float satelliteTargetAngle = null;

    // Indicador de flick: Boolean.TRUE = Derecha, Boolean.FALSE = Izquierda, null = sin flick
    public Boolean flickFacing = null;

    public TouchController() {
        reset();
    }

    public void reset() {
        primaryId = -1;
        secondaryId = -1;
        deltaXTactil = 0f;
        deltaYTactil = 0f;
        touchInitialized = false;
        primaryX = 0f;
        primaryY = 0f;
        secondaryX = 0f;
        secondaryY = 0f;
        secondaryOriginX = 0f;
        secondaryOriginY = 0f;
        secondaryStartX = 0f;
        secondaryStartY = 0f;
        secondaryTouchTime = 0f;
        secondaryHasDragged = false;
        isTouching = false;
        isFiring = false;
        satelliteLock = false;
        satelliteTargetAngle = null;
        flickFacing = null;
    }

    public void update(float dt) {
        if (secondaryId != -1) {
            secondaryTouchTime += dt;
        }
    }

    public void onTouchDown(int pointerId, float x, float y, float playerX, float playerY) {
        if (primaryId == -1) {
            // Primer dedo: control de movimiento y disparo continuo
            primaryId = pointerId;
            primaryX = x;
            primaryY = y;

            deltaXTactil = x - playerX;
            deltaYTactil = y - playerY;
            touchInitialized = true;

            isTouching = true;
            isFiring = true;
        } else if (secondaryId == -1 && pointerId != primaryId) {
            // Segundo dedo (dedo libre)
            secondaryId = pointerId;
            secondaryX = x;
            secondaryY = y;
            secondaryOriginX = x;
            secondaryOriginY = y;
            secondaryStartX = x;
            secondaryStartY = y;
            secondaryTouchTime = 0f;
            secondaryHasDragged = false;
        }
    }

    public PointF onTouchMove(int pointerId, float x, float y) {
        if (primaryId == pointerId) {
            primaryX = x;
            primaryY = y;
            isTouching = true;
            isFiring = true;

            if (touchInitialized) {
                float newX = x - deltaXTactil;
                float newY = y - deltaYTactil;
                return new PointF(newX, newY);
            }
        } else if (secondaryId == pointerId) {
            secondaryX = x;
            secondaryY = y;

            float dx = x - secondaryStartX;
            float dy = y - secondaryStartY;
            float distSq = dx * dx + dy * dy;

            // Control y apuntado de satélites en 360 grados
            if (distSq > 100f) { // umbral de 10 píxeles
                secondaryHasDragged = true;
                satelliteLock = true;

                // Clamping flotante con radio de 60 px
                float dist = (float) Math.sqrt(distSq);
                float maxRadius = 60f;
                if (dist > maxRadius) {
                    secondaryStartX = x - (dx / dist) * maxRadius;
                    secondaryStartY = y - (dy / dist) * maxRadius;
                    dx = x - secondaryStartX;
                    dy = y - secondaryStartY;
                }

                satelliteTargetAngle = (float) Math.atan2(dy, dx);
            }
        }
        return null;
    }

    public void onTouchUp(int pointerId, float x, float y, float playerX, float playerY) {
        if (primaryId == pointerId) {
            if (secondaryId != -1) {
                // Smooth handoff: el segundo dedo asume el movimiento
                primaryId = secondaryId;
                primaryX = secondaryX;
                primaryY = secondaryY;
                deltaXTactil = primaryX - playerX;
                deltaYTactil = primaryY - playerY;
                touchInitialized = true;

                secondaryId = -1;
                secondaryHasDragged = false;
                secondaryTouchTime = 0f;
            } else {
                primaryId = -1;
                touchInitialized = false;
                isTouching = false;
                isFiring = false;
            }
        } else if (secondaryId == pointerId) {
            float totalDx = x - secondaryOriginX;
            float totalDy = y - secondaryOriginY;
            float totalDistSq = totalDx * totalDx + totalDy * totalDy;
            float touchTime = secondaryTouchTime;

            if (touchTime < 0.40f) {
                if (Math.abs(totalDx) >= 25f && Math.abs(totalDx) >= Math.abs(totalDy) * 1.1f) {
                    // Latigazo horizontal rápido (Flick) -> Voltear personaje
                    flickFacing = totalDx > 0f;
                } else if (totalDistSq < 400f) {
                    // Toque rápido sin arrastre (< 20 px) -> conmuta bloqueo de satélites
                    satelliteLock = !satelliteLock;
                }
            }

            secondaryId = -1;
            secondaryHasDragged = false;
            secondaryTouchTime = 0f;
        }
    }

    public void onTouchCancel(float playerX, float playerY) {
        reset();
    }
}
