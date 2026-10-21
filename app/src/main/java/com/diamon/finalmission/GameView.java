package com.diamon.finalmission;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.engine.GameState;
import com.diamon.finalmission.engine.TouchController;
import com.diamon.finalmission.ui.HUD;
import com.diamon.finalmission.world.Level;

import java.util.Random;

/**
 * Vista principal de alto rendimiento de Final Mission para Android.
 * - Modo Horizontal Landscape completo (Widescreen sin barras negras, adaptado al aspect ratio del móvil).
 * - Cinemática de prólogo NES auténtica (fondoIntroduccion3.png, introduccion.png, introduccion.wav,
 *   skyline en scroll, texto de invasión alienígena 2029 A.D., soldados despegando con jetpack y satélites).
 * - Menú principal con selección 1P Arnold / 2P Sigourney, Help / Controls (ayuda.png), Start Mission.
 * - Tarjeta de presentación de fase (STAGE 1..8 con comienzo1.wav).
 * - Pantalla de Continuar (continuar.png) con cuenta regresiva de 9 segundos y Game Over (finJuego.png, finDeJuego.wav).
 * - Pantalla de fin de fase con recuento de bonificación (finNivel.png).
 * - Giro ágil de orientación (flick horizontal) y satélites 360° táctiles sin tapar la nave.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final int VIRTUAL_HEIGHT = 224;
    public int virtualWidth = 398; // Ancho virtual adaptativo widescreen

    private SurfaceHolder holder;
    private Thread renderThread;
    private volatile boolean isRunning = false;

    // Lienzo virtual retro widescreen y búfer de volcado
    private Bitmap virtualBitmap;
    private Canvas virtualCanvas;
    private Rect srcRect;
    private RectF dstRect;
    private Paint renderPaint;

    // Subsistemas del motor
    private AssetLoader assetLoader;
    private AudioManager audioManager;
    private TouchController touchController;
    private Level level;
    private HUD hud;
    private GameState gameState = GameState.MENU;

    // Selección de personaje y opciones de menú
    private int selectedCharacter = 1; // 1 = Arnold, 2 = Sigourney
    private int menuOptionIndex = 0;   // 0 = 1P Arnold, 1 = 2P Sigourney, 2 = Help, 3 = Prologue
    private float menuTimer = 0f;
    private float menuIdleTimer = 0f;
    private float stateTransitionTimer = 0f;

    // Temporizadores de pantallas
    private float prologueTimer = 0f;
    private float stageIntroTimer = 0f;
    private float continueCountdown = 9.99f;
    private int continueOption = 0; // 0 = CONTINUE, 1 = END
    private float gameOverTimer = 0f;

    // Tipografías retro y pinceles
    private Paint retroTextPaint;
    private Paint retroShadowPaint;
    private Paint titleTextPaint;
    private Paint countdownPaint;
    private Paint cardBgPaint;
    private Paint cardBorderPaint;

    // Campo de estrellas de fondo para pantallas panorámicas
    private static final int STAR_COUNT = 45;
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private final int[] starColor = new int[STAR_COUNT];
    private final Paint starPaint = new Paint();

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);

        virtualBitmap = Bitmap.createBitmap(virtualWidth, VIRTUAL_HEIGHT, Bitmap.Config.ARGB_8888);
        virtualCanvas = new Canvas(virtualBitmap);
        srcRect = new Rect(0, 0, virtualWidth, VIRTUAL_HEIGHT);
        dstRect = new RectF(0, 0, virtualWidth, VIRTUAL_HEIGHT);

        renderPaint = new Paint();
        renderPaint.setFilterBitmap(false);

        assetLoader = new AssetLoader(getContext());
        audioManager = new AudioManager(getContext());
        touchController = new TouchController();
        level = new Level(getContext());
        hud = new HUD();

        retroTextPaint = new Paint();
        retroTextPaint.setColor(Color.WHITE);
        retroTextPaint.setTextSize(8f);
        retroTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        retroTextPaint.setAntiAlias(false);

        retroShadowPaint = new Paint();
        retroShadowPaint.setColor(Color.BLACK);
        retroShadowPaint.setTextSize(8f);
        retroShadowPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        retroShadowPaint.setAntiAlias(false);

        titleTextPaint = new Paint();
        titleTextPaint.setColor(Color.rgb(252, 216, 168)); // Oro retro NES
        titleTextPaint.setTextSize(12f);
        titleTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        titleTextPaint.setAntiAlias(false);

        countdownPaint = new Paint();
        countdownPaint.setColor(Color.WHITE);
        countdownPaint.setTextSize(24f);
        countdownPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        countdownPaint.setAntiAlias(false);

        cardBgPaint = new Paint();
        cardBgPaint.setColor(Color.rgb(16, 20, 36));
        cardBgPaint.setStyle(Paint.Style.FILL);

        cardBorderPaint = new Paint();
        cardBorderPaint.setColor(Color.rgb(96, 160, 255));
        cardBorderPaint.setStyle(Paint.Style.STROKE);
        cardBorderPaint.setStrokeWidth(1.5f);

        // Generar campo de estrellas retro
        Random rng = new Random(42);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = rng.nextFloat() * 600f;
            starY[i] = rng.nextFloat() * VIRTUAL_HEIGHT;
            int shade = 140 + rng.nextInt(115);
            starColor[i] = Color.rgb(shade, shade, shade + 15);
        }

        setFocusable(true);
        setKeepScreenOn(true);

        setGameState(GameState.MENU);
    }

    public void setGameState(GameState newState) {
        this.gameState = newState;
        stateTransitionTimer = 0f;

        switch (newState) {
            case PROLOGUE:
                prologueTimer = 0f;
                audioManager.stopMusic();
                audioManager.playMusic(AudioManager.BGM_INTRODUCCION, true);
                break;
            case MENU:
                menuIdleTimer = 0f;
                audioManager.playMusic(AudioManager.BGM_MENU, true);
                break;
            case HELP:
                break;
            case STAGE_INTRO:
                stageIntroTimer = 0f;
                audioManager.stopMusic();
                audioManager.playJingle(AudioManager.BGM_COMIENZO, () -> {
                    if (gameState == GameState.STAGE_INTRO) {
                        setGameState(GameState.PLAYING);
                    }
                });
                break;
            case PLAYING:
                audioManager.playMusic(AudioManager.BGM_MUSICA, true);
                break;
            case STAGE_CLEAR:
                audioManager.stopMusic();
                audioManager.playSound(AudioManager.SFX_PODER);
                break;
            case CONTINUE:
                continueCountdown = 9.99f;
                continueOption = 0;
                audioManager.stopMusic();
                break;
            case GAME_OVER:
                gameOverTimer = 0f;
                audioManager.stopMusic();
                audioManager.playJingle(AudioManager.BGM_FIN_JUEGO, null);
                break;
            case VICTORY:
                audioManager.stopMusic();
                audioManager.playMusic(AudioManager.BGM_MENU, true);
                break;
            case PAUSED:
                audioManager.pauseMusic();
                audioManager.playSound(AudioManager.SFX_PAUSA);
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (width <= 0 || height <= 0) return;

        // Modo horizontal widescreen completo sin barras negras (100% de la pantalla)
        float aspect = (float) width / (float) height;
        virtualWidth = Math.max(256, Math.round(VIRTUAL_HEIGHT * aspect));

        // Recrear búfer virtual retro adaptado al nuevo ancho panorámico
        if (virtualBitmap != null && !virtualBitmap.isRecycled()) {
            virtualBitmap.recycle();
        }
        virtualBitmap = Bitmap.createBitmap(virtualWidth, VIRTUAL_HEIGHT, Bitmap.Config.ARGB_8888);
        virtualCanvas = new Canvas(virtualBitmap);

        srcRect.set(0, 0, virtualWidth, VIRTUAL_HEIGHT);
        dstRect.set(0f, 0f, (float) width, (float) height);

        level.setVirtualDimensions(virtualWidth, VIRTUAL_HEIGHT);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    public void resume() {
        if (!isRunning) {
            isRunning = true;
            renderThread = new Thread(this, "FinalMissionRenderThread");
            renderThread.start();
            audioManager.resumeMusic();
        }
    }

    public void pause() {
        isRunning = false;
        audioManager.pauseMusic();
        if (renderThread != null) {
            try {
                renderThread.join(600);
            } catch (InterruptedException ignored) {}
            renderThread = null;
        }
    }

    public void release() {
        pause();
        if (audioManager != null) {
            audioManager.release();
        }
        if (assetLoader != null) {
            assetLoader.clear();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final long targetFrameNs = 16_666_666L; // 60 FPS exactos

        while (isRunning) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000.0f;
            lastTime = now;

            if (dt > 0.1f) dt = 0.016f;

            updateGame(dt);
            renderGame();

            Canvas canvas = null;
            try {
                canvas = holder.lockHardwareCanvas();
                if (canvas == null) {
                    canvas = holder.lockCanvas();
                }
                if (canvas != null) {
                    canvas.drawBitmap(virtualBitmap, srcRect, dstRect, renderPaint);
                }
            } catch (Exception ignored) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            long elapsedNs = System.nanoTime() - now;
            long sleepNs = targetFrameNs - elapsedNs;
            if (sleepNs > 1_000_000L) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void updateGame(float dt) {
        touchController.update(dt);
        stateTransitionTimer += dt;
        menuTimer += dt;

        switch (gameState) {
            case PROLOGUE:
                prologueTimer += dt;
                if (prologueTimer >= 19.5f) {
                    setGameState(GameState.MENU);
                }
                break;

            case MENU:
                menuIdleTimer += dt;
                // Si pasa 12 segundos sin tocar la pantalla, se activa el prólogo cinemático automáticamente
                if (menuIdleTimer >= 12.0f) {
                    setGameState(GameState.PROLOGUE);
                }
                break;

            case HELP:
                break;

            case STAGE_INTRO:
                stageIntroTimer += dt;
                if (stageIntroTimer >= 3.87f) {
                    setGameState(GameState.PLAYING);
                }
                break;

            case PLAYING:
                // Giro ágil del jugador (flick horizontal o cambio táctil)
                if (touchController.flickFacing != null) {
                    level.player.setFacing(touchController.flickFacing);
                    touchController.flickFacing = null;
                }

                level.update(dt, touchController.isTouching, touchController.satelliteLock,
                        touchController.satelliteTargetAngle, audioManager);

                // Fin de vidas -> Pantalla de Continuar auténtica NES
                if (!level.player.alive && level.player.lives <= 0) {
                    setGameState(GameState.CONTINUE);
                }

                // Fin de nivel si el jefe es destruido
                if (level.boss != null && level.boss.isDefeated && level.boss.defeatTimer >= 2.8f) {
                    level.calculateStageClearBonus();
                    setGameState(GameState.STAGE_CLEAR);
                }
                break;

            case STAGE_CLEAR:
                level.updateTally(dt, audioManager);
                if (level.tallyFinished && stateTransitionTimer >= 4.5f) {
                    advanceToNextStage();
                }
                break;

            case CONTINUE:
                continueCountdown -= dt;
                if (continueCountdown <= 0f) {
                    continueCountdown = 0f;
                    setGameState(GameState.GAME_OVER);
                }
                break;

            case GAME_OVER:
                gameOverTimer += dt;
                break;

            case VICTORY:
            case PAUSED:
                break;
        }
    }

    private void advanceToNextStage() {
        if (level.currentStage < 8) {
            level.initStage(level.currentStage + 1);
            setGameState(GameState.STAGE_INTRO);
        } else {
            setGameState(GameState.VICTORY);
        }
    }

    private void renderGame() {
        switch (gameState) {
            case PROLOGUE:
                renderPrologueScreen();
                break;
            case MENU:
                renderMenuScreen();
                break;
            case HELP:
                renderHelpScreen();
                break;
            case STAGE_INTRO:
                renderStageIntroCard();
                break;
            case PLAYING:
                level.render(virtualCanvas, assetLoader, renderPaint);
                hud.render(virtualCanvas, assetLoader, level.player, level.score, level.highScore,
                        level.boss, level.stageTime, level.currentStage);
                break;
            case STAGE_CLEAR:
                level.render(virtualCanvas, assetLoader, renderPaint);
                renderStageClearScreen();
                break;
            case PAUSED:
                level.render(virtualCanvas, assetLoader, renderPaint);
                hud.render(virtualCanvas, assetLoader, level.player, level.score, level.highScore,
                        level.boss, level.stageTime, level.currentStage);
                virtualCanvas.drawColor(0x88000000);
                drawCenteredText(virtualCanvas, "PAUSED", 112f, Color.YELLOW, 12f);
                break;
            case CONTINUE:
                renderContinueScreen();
                break;
            case GAME_OVER:
                renderGameOverScreen();
                break;
            case VICTORY:
                renderVictoryScreen();
                break;
        }
    }

    /**
     * 1. STORY PROLOGUE / CINEMÁTICA NES AUTÉNTICA:
     * - Utiliza fondoIntroduccion3.png (panorama 512x240), introduccion.png e introduccion.wav.
     * - Desplaza la silueta de la ciudad destruida.
     * - Relata la invasión alienígena cybernética de 2029 A.D.
     * - Transición al despegue de Arnold y Sigourney con jetpack y satélites orbitales.
     */
    private void renderPrologueScreen() {
        virtualCanvas.drawColor(Color.BLACK);

        float maxPan = Math.max(0f, 512f - virtualWidth);
        float panT = Math.min(1.0f, prologueTimer / 8.0f);
        float scrollX = panT * maxPan;

        // Renderizado del panorama de la ciudad destruida (fondoIntroduccion3.png)
        Bitmap skyline = assetLoader.getBitmap("fondoIntroduccion3.png");
        if (skyline != null) {
            Rect srcPan = new Rect((int) scrollX, 0, (int) Math.min(512f, scrollX + virtualWidth), 240);
            RectF dstPan = new RectF(0, -8f, virtualWidth, 232f);
            virtualCanvas.drawBitmap(skyline, srcPan, dstPan, renderPaint);
        }

        drawTwinklingStars(virtualCanvas);

        // Acto 1 (0.0s - 7.5s): El Ataque Alienígena
        if (prologueTimer < 7.5f) {
            drawShadowBox(virtualCanvas, 16f, 168f, virtualWidth - 32f, 48f);
            drawCenteredText(virtualCanvas, "A.D. 2029 -- EARTH UNDER MERCILESS SIEGE.", 180f, Color.rgb(255, 210, 140), 7.5f);
            drawCenteredText(virtualCanvas, "A CYBERNETIC ALIEN INVASION STRUCK SUDDENLY.", 192f, Color.WHITE, 7.5f);
            drawCenteredText(virtualCanvas, "HUMAN METROPOLISES BURNED TO SMOKING RUINS.", 204f, Color.WHITE, 7.5f);
        }
        // Acto 2 (7.5s - 13.5s): Movilización de Arnold & Sigourney y Despegue
        else if (prologueTimer < 13.5f) {
            drawShadowBox(virtualCanvas, 16f, 12f, virtualWidth - 32f, 36f);
            drawCenteredText(virtualCanvas, "SPECIAL CYBERNETIC ATTACK TEAM (S.C.A.T.)", 22f, Color.rgb(255, 210, 140), 7.5f);
            drawCenteredText(virtualCanvas, "TWO ELITE CYBORG WARRIORS SORTIE FOR COUNTER-STRIKE:", 32f, Color.WHITE, 7.5f);
            drawCenteredText(virtualCanvas, "ARNOLD & SIGOURNEY", 42f, Color.rgb(130, 220, 255), 8.5f);

            float flightPhase = Math.max(0f, (prologueTimer - 8.5f) / 3.5f);
            float heroY = 135f - (flightPhase * 95f);
            float hero1X = (virtualWidth * 0.38f) + (flightPhase * 50f);
            float hero2X = (virtualWidth * 0.58f) + (flightPhase * 50f);

            int aFrame = (int) ((menuTimer * 12) % 6) + 1;
            Bitmap arnoldBmp = assetLoader.getBitmap("jugador1D" + aFrame + ".png");
            if (arnoldBmp != null) {
                virtualCanvas.drawBitmap(arnoldBmp, hero1X, heroY, renderPaint);
            }

            int sFrame = (int) ((menuTimer * 12) % 4) + 1;
            Bitmap sigourneyBmp = assetLoader.getBitmap("jugador2D" + sFrame + ".png");
            if (sigourneyBmp != null) {
                virtualCanvas.drawBitmap(sigourneyBmp, hero2X, heroY + 4f, renderPaint);
            }

            int flameIdx = (int) ((menuTimer * 18) % 3) + 1;
            Bitmap flameBmp = assetLoader.getBitmap("humoMisil" + flameIdx + ".png");
            if (flameBmp != null) {
                virtualCanvas.drawBitmap(flameBmp, hero1X + 4f, heroY + 24f, renderPaint);
                virtualCanvas.drawBitmap(flameBmp, hero2X + 4f, heroY + 28f, renderPaint);
            }

            float satAngle = menuTimer * 7.0f;
            float sat1X = hero1X + 11f + (float) Math.cos(satAngle) * 22f;
            float sat1Y = heroY + 14f + (float) Math.sin(satAngle) * 16f;
            float sat2X = hero2X + 11f + (float) Math.cos(satAngle + Math.PI) * 22f;
            float sat2Y = heroY + 14f + (float) Math.sin(satAngle + Math.PI) * 16f;

            Bitmap satBmp = assetLoader.getBitmap("sateliteHD1.png");
            if (satBmp != null) {
                virtualCanvas.drawBitmap(satBmp, sat1X - satBmp.getWidth() / 2f, sat1Y - satBmp.getHeight() / 2f, renderPaint);
                virtualCanvas.drawBitmap(satBmp, sat2X - satBmp.getWidth() / 2f, sat2Y - satBmp.getHeight() / 2f, renderPaint);
            }
        }
        // Acto 3 (13.5s - 19.5s): Emblema Final Mission y Comienzo de la Misión
        else {
            Bitmap introLogo = assetLoader.getBitmap("introduccion.png");
            if (introLogo != null) {
                float logoX = (virtualWidth - 256f) / 2f;
                virtualCanvas.drawBitmap(introLogo, logoX, -8f, renderPaint);
            }

            drawShadowBox(virtualCanvas, (virtualWidth - 220f) / 2f, 140f, 220f, 65f);
            drawCenteredText(virtualCanvas, "OPERATION: PENETRATE ALIEN DEFENSE LINE", 154f, Color.rgb(255, 215, 120), 8f);
            drawCenteredText(virtualCanvas, "AND DESTROY THE CYBERNETIC MOTHERSHIP!", 166f, Color.WHITE, 7.5f);
            drawCenteredText(virtualCanvas, "THE FATE OF HUMANITY RESTS IN YOUR HANDS.", 178f, Color.rgb(180, 220, 255), 7f);

            if (menuTimer % 0.6f < 0.35f) {
                drawCenteredText(virtualCanvas, "TOUCH TO START MISSION", 196f, Color.rgb(255, 240, 160), 8.5f);
            }
        }

        drawShadowText(virtualCanvas, "TAP TO SKIP", virtualWidth - 62f, 216f, Color.LTGRAY, 6.5f);
    }

    /**
     * 2. TITLE & MAIN MENU:
     * - Utiliza menu1.png y menu2.png alternando brillo del logotipo.
     * - Opciones: 1P Arnold, 2P Sigourney, Help / Controls (ayuda.png), Story Prologue.
     * - Cursor animado selector1.png / selector2.png.
     * - Vista previa de sprites de Arnold y Sigourney.
     */
    private void renderMenuScreen() {
        virtualCanvas.drawColor(Color.BLACK);
        drawTwinklingStars(virtualCanvas);

        int menuBmpIdx = ((int) (menuTimer * 5.0f) % 2) + 1;
        Bitmap menuBmp = assetLoader.getBitmap("menu" + menuBmpIdx + ".png");

        float menuOffsetLeft = (virtualWidth - 256f) / 2f;
        if (menuBmp != null) {
            virtualCanvas.drawBitmap(menuBmp, menuOffsetLeft, -8f, renderPaint);
        }

        // Decoración retro en márgenes widescreen
        if (menuOffsetLeft > 10f) {
            drawShadowText(virtualCanvas, "S.C.A.T. 2029", 10f, 20f, Color.rgb(96, 160, 255), 7f);
            drawShadowText(virtualCanvas, "CYBER ASSAULT", 10f, 32f, Color.LTGRAY, 6.5f);

            drawShadowText(virtualCanvas, "NATSUME 1990", virtualWidth - 75f, 20f, Color.rgb(255, 200, 120), 7f);
            drawShadowText(virtualCanvas, "8 STAGES WIDE", virtualWidth - 75f, 32f, Color.LTGRAY, 6.5f);
        }

        if (menuTimer % 0.6f < 0.38f) {
            drawShadowText(virtualCanvas, "TOUCH TO START MISSION", menuOffsetLeft + 68f, 144f, Color.rgb(255, 240, 160), 7.5f);
        }

        // Identificadores y distintivos de Arnold (1P) y Sigourney (2P)
        drawShadowText(virtualCanvas, "(ARNOLD)", menuOffsetLeft + 154f, 168f,
                (selectedCharacter == 1) ? Color.rgb(255, 220, 120) : Color.LTGRAY, 7f);
        drawShadowText(virtualCanvas, "(SIGOURNEY)", menuOffsetLeft + 162f, 184f,
                (selectedCharacter == 2) ? Color.rgb(130, 220, 255) : Color.LTGRAY, 7f);

        drawShadowText(virtualCanvas, "HELP / CONTROLS", menuOffsetLeft + 90f, 196f,
                (menuOptionIndex == 2) ? Color.rgb(255, 240, 160) : Color.WHITE, 7.5f);
        drawShadowText(virtualCanvas, "STORY PROLOGUE", menuOffsetLeft + 90f, 206f,
                (menuOptionIndex == 3) ? Color.rgb(255, 240, 160) : Color.rgb(180, 210, 240), 7f);

        // Previsualización animada de los héroes
        int hero1Frame = (int) ((menuTimer * 6f) % 6) + 1;
        Bitmap aMini = assetLoader.getBitmap("jugador1D" + hero1Frame + ".png");
        if (aMini != null) {
            virtualCanvas.drawBitmap(aMini, menuOffsetLeft + 208f, 154f, renderPaint);
        }

        int hero2Frame = (int) ((menuTimer * 6f) % 4) + 1;
        Bitmap sMini = assetLoader.getBitmap("jugador2D" + hero2Frame + ".png");
        if (sMini != null) {
            virtualCanvas.drawBitmap(sMini, menuOffsetLeft + 218f, 170f, renderPaint);
        }

        // Cursor animado NES
        int selFrame = (menuTimer % 0.3f < 0.15f) ? 1 : 2;
        Bitmap selector = assetLoader.getBitmap("selector" + selFrame + ".png");
        if (selector != null) {
            float cursorBob = (float) Math.sin(menuTimer * 10f) * 1.5f;
            float cursorX = menuOffsetLeft + 72f + cursorBob;
            float cursorY = 160f;
            if (menuOptionIndex == 1) cursorY = 176f;
            else if (menuOptionIndex == 2) cursorY = 188f;
            else if (menuOptionIndex == 3) cursorY = 198f;

            virtualCanvas.drawBitmap(selector, cursorX, cursorY, renderPaint);
        }
    }

    /**
     * Pantalla de Ayuda / Controles (ayuda.png).
     */
    private void renderHelpScreen() {
        virtualCanvas.drawColor(Color.rgb(12, 16, 24));

        Bitmap ayudaBmp = assetLoader.getBitmap("ayuda.png");
        if (ayudaBmp != null) {
            float scale = Math.min((float) virtualWidth / 640f, 224f / 480f);
            float drawW = 640f * scale;
            float drawH = 480f * scale;
            float drawX = (virtualWidth - drawW) / 2f;
            float drawY = (VIRTUAL_HEIGHT - drawH) / 2f;
            virtualCanvas.drawBitmap(ayudaBmp, new Rect(0, 0, 640, 480),
                    new RectF(drawX, drawY, drawX + drawW, drawY + drawH), renderPaint);
        }

        if (menuTimer % 0.6f < 0.35f) {
            drawCenteredText(virtualCanvas, "TOUCH ANYWHERE TO RETURN TO MENU", 216f, Color.YELLOW, 7.5f);
        }
    }

    /**
     * 3. STAGE INTRO CARD:
     * - Muestra "STAGE X" banner.
     * - Reproduce comienzo1.wav.
     */
    private void renderStageIntroCard() {
        virtualCanvas.drawColor(Color.BLACK);
        drawTwinklingStars(virtualCanvas);

        float cardW = Math.min(260f, virtualWidth - 40f);
        float cardH = 130f;
        float cardX = (virtualWidth - cardW) / 2f;
        float cardY = (VIRTUAL_HEIGHT - cardH) / 2f;

        virtualCanvas.drawRect(cardX, cardY, cardX + cardW, cardY + cardH, cardBgPaint);
        virtualCanvas.drawRect(cardX, cardY, cardX + cardW, cardY + cardH, cardBorderPaint);
        virtualCanvas.drawRect(cardX + 2f, cardY + 2f, cardX + cardW - 2f, cardY + cardH - 2f, cardBorderPaint);

        drawCenteredText(virtualCanvas, "========================", cardY + 20f, Color.rgb(96, 160, 255), 8f);
        drawCenteredText(virtualCanvas, "STAGE  " + level.currentStage, cardY + 36f, Color.rgb(255, 220, 130), 14f);
        drawCenteredText(virtualCanvas, "========================", cardY + 48f, Color.rgb(96, 160, 255), 8f);

        drawCenteredText(virtualCanvas, level.stageConfig.subtitle, cardY + 66f, Color.WHITE, 8f);

        String soldierStr = "SOLDIER: " + level.player.getCharacterName();
        drawCenteredText(virtualCanvas, soldierStr, cardY + 82f,
                (level.player.characterId == 2) ? Color.rgb(130, 220, 255) : Color.rgb(255, 200, 120), 8.5f);

        String sprite = (level.player.characterId == 2) ? "jugador2D1.png" : "jugador1D1.png";
        Bitmap heroPreview = assetLoader.getBitmap(sprite);
        if (heroPreview != null) {
            virtualCanvas.drawBitmap(heroPreview, cardX + 16f, cardY + 68f, renderPaint);
        }

        if (menuTimer % 0.4f < 0.22f) {
            drawCenteredText(virtualCanvas, "READY!", cardY + 110f, Color.rgb(255, 240, 160), 9f);
        }
    }

    /**
     * Pantalla de Fin de Fase (finNivel.png y recuento de puntos).
     */
    private void renderStageClearScreen() {
        virtualCanvas.drawColor(0xAA000000);

        Bitmap banner = assetLoader.getBitmap("finNivel.png");
        if (banner != null) {
            float bx = (virtualWidth - banner.getWidth()) / 2f;
            virtualCanvas.drawBitmap(banner, bx, 25f, renderPaint);
        }

        float cx = virtualWidth / 2f;
        drawCenteredText(virtualCanvas, "STAGE " + level.currentStage + " CLEARED!", 95f, Color.rgb(255, 220, 120), 9f);
        drawCenteredText(virtualCanvas, level.stageConfig.subtitle, 110f, Color.WHITE, 8f);

        int displayedClear = (int) (level.stageClearBonus * level.tallyProgress);
        int displayedLife = (int) (level.stageLifeBonus * level.tallyProgress);
        int displayedStock = (int) (level.stageStockBonus * level.tallyProgress);

        float leftColX = cx - 70f;
        drawShadowText(virtualCanvas, String.format("CLEAR BONUS:  +%05d", displayedClear), leftColX, 130f, Color.WHITE, 7.5f);
        drawShadowText(virtualCanvas, String.format("LIFE BONUS:   +%05d", displayedLife), leftColX, 144f, Color.WHITE, 7.5f);
        drawShadowText(virtualCanvas, String.format("STOCK BONUS:  +%05d", displayedStock), leftColX, 158f, Color.WHITE, 7.5f);

        drawCenteredText(virtualCanvas, String.format("TOTAL SCORE:  %06d", level.score), 178f, Color.rgb(180, 220, 255), 8f);

        if (level.tallyFinished && (menuTimer % 0.6f < 0.35f)) {
            String promptText = (level.currentStage < 8) ? "TOUCH TO PROCEED TO NEXT STAGE" : "TOUCH TO COMPLETE MISSION";
            drawCenteredText(virtualCanvas, promptText, 202f, Color.YELLOW, 8f);
        }
    }

    /**
     * 4. CONTINUE SCREEN (continuar.png):
     * - Cuenta regresiva de 9 segundos.
     * - Opciones: CONTINUE o END.
     */
    private void renderContinueScreen() {
        virtualCanvas.drawColor(Color.BLACK);
        drawTwinklingStars(virtualCanvas);

        Bitmap contBmp = assetLoader.getBitmap("continuar.png");
        float menuOffsetLeft = (virtualWidth - 256f) / 2f;

        if (contBmp != null) {
            virtualCanvas.drawBitmap(contBmp, menuOffsetLeft, -8f, renderPaint);
        }

        drawShadowText(virtualCanvas, String.format("%06d", level.highScore), menuOffsetLeft + 168f, 40f, Color.WHITE, 8f);
        drawShadowText(virtualCanvas, String.format("%06d", level.score), menuOffsetLeft + 168f, 72f, Color.WHITE, 8f);

        int countSec = (int) Math.ceil(continueCountdown);
        int timerColor = (continueCountdown <= 3.0f && (menuTimer % 0.2f < 0.1f)) ? Color.RED : Color.YELLOW;

        countdownPaint.setColor(timerColor);
        String countStr = String.valueOf(countSec);
        float countW = countdownPaint.measureText(countStr);
        virtualCanvas.drawText(countStr, (virtualWidth - countW) / 2f, 120f, countdownPaint);

        drawCenteredText(virtualCanvas, "TIME REMAINING", 96f, Color.rgb(180, 210, 240), 7.5f);

        int selFrame = (menuTimer % 0.3f < 0.15f) ? 1 : 2;
        Bitmap selector = assetLoader.getBitmap("selector" + selFrame + ".png");
        if (selector != null) {
            float cursorBob = (float) Math.sin(menuTimer * 10f) * 1.5f;
            float cursorX = menuOffsetLeft + 78f + cursorBob;
            float cursorY = (continueOption == 0) ? 144f : 160f;
            virtualCanvas.drawBitmap(selector, cursorX, cursorY, renderPaint);
        }
    }

    /**
     * Pantalla de Fin de Juego (finJuego.png, finDeJuego.wav).
     */
    private void renderGameOverScreen() {
        virtualCanvas.drawColor(Color.BLACK);
        drawTwinklingStars(virtualCanvas);

        Bitmap overBmp = assetLoader.getBitmap("finJuego.png");
        float menuOffsetLeft = (virtualWidth - 256f) / 2f;
        if (overBmp != null) {
            virtualCanvas.drawBitmap(overBmp, menuOffsetLeft, -8f, renderPaint);
        }

        drawCenteredText(virtualCanvas, "GAME OVER", 100f, Color.WHITE, 12f);
        drawCenteredText(virtualCanvas, String.format("STAGE REACHED: %d / 8", level.currentStage), 125f, Color.rgb(180, 210, 240), 8f);
        drawCenteredText(virtualCanvas, String.format("FINAL SCORE:   %06d", level.score), 145f, Color.rgb(255, 215, 120), 8.5f);

        if (gameOverTimer > 1.2f && (menuTimer % 0.6f < 0.35f)) {
            drawCenteredText(virtualCanvas, "TOUCH TO RETRY", 180f, Color.rgb(255, 240, 160), 8f);
        }
    }

    /**
     * Pantalla de Victoria de Fin de Misión (finNivel.png).
     */
    private void renderVictoryScreen() {
        virtualCanvas.drawColor(Color.BLACK);
        drawTwinklingStars(virtualCanvas);

        Bitmap vicBmp = assetLoader.getBitmap("finNivel.png");
        float menuOffsetLeft = (virtualWidth - 256f) / 2f;
        if (vicBmp != null) {
            virtualCanvas.drawBitmap(vicBmp, menuOffsetLeft, -8f, renderPaint);
        }

        drawCenteredText(virtualCanvas, "ALL 8 MISSIONS ACCOMPLISHED!", 85f, Color.rgb(255, 220, 120), 10f);
        drawCenteredText(virtualCanvas, "THE EARTH IS SAVED!", 105f, Color.WHITE, 8.5f);
        drawCenteredText(virtualCanvas, String.format("FINAL HIGH SCORE: %06d", level.score), 130f, Color.rgb(180, 220, 255), 8.5f);
        drawCenteredText(virtualCanvas, "CONGRATULATIONS ARNOLD & SIGOURNEY", 155f, Color.rgb(255, 200, 130), 8f);

        if (menuTimer % 0.6f < 0.35f) {
            drawCenteredText(virtualCanvas, "TOUCH TO RETURN TO MENU", 190f, Color.YELLOW, 8f);
        }
    }

    private void drawTwinklingStars(Canvas canvas) {
        for (int i = 0; i < STAR_COUNT; i++) {
            float blink = (float) Math.sin((menuTimer * 3f) + i);
            if (blink > -0.3f) {
                starPaint.setColor(starColor[i]);
                canvas.drawPoint(starX[i] % virtualWidth, starY[i], starPaint);
            }
        }
    }

    private void drawShadowBox(Canvas canvas, float x, float y, float w, float h) {
        Paint boxPaint = new Paint();
        boxPaint.setColor(0xCC000000);
        canvas.drawRect(x, y, x + w, y + h, boxPaint);

        Paint bBorder = new Paint();
        bBorder.setColor(Color.rgb(64, 96, 160));
        bBorder.setStyle(Paint.Style.STROKE);
        bBorder.setStrokeWidth(1f);
        canvas.drawRect(x, y, x + w, y + h, bBorder);
    }

    private void drawShadowText(Canvas canvas, String text, float x, float y, int color, float size) {
        retroShadowPaint.setTextSize(size);
        retroShadowPaint.setColor(Color.BLACK);
        canvas.drawText(text, x + 1f, y + 1f, retroShadowPaint);

        retroTextPaint.setTextSize(size);
        retroTextPaint.setColor(color);
        canvas.drawText(text, x, y, retroTextPaint);
    }

    private void drawCenteredText(Canvas canvas, String text, float y, int color, float size) {
        retroTextPaint.setTextSize(size);
        float textW = retroTextPaint.measureText(text);
        float x = (virtualWidth - textW) / 2f;
        drawShadowText(canvas, text, x, y, color, size);
    }

    public boolean onBackPressed() {
        switch (gameState) {
            case PROLOGUE:
            case HELP:
            case STAGE_INTRO:
                setGameState(GameState.MENU);
                return true;
            case PLAYING:
                setGameState(GameState.PAUSED);
                return true;
            case PAUSED:
                setGameState(GameState.MENU);
                return true;
            case CONTINUE:
                setGameState(GameState.GAME_OVER);
                return true;
            case GAME_OVER:
            case VICTORY:
                setGameState(GameState.MENU);
                return true;
            case MENU:
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        float touchVirtualX = (event.getX(actionIndex) / dstRect.width()) * (float) virtualWidth;
        float touchVirtualY = (event.getY(actionIndex) / dstRect.height()) * (float) VIRTUAL_HEIGHT;

        if (gameState != GameState.PLAYING) {
            if (action == MotionEvent.ACTION_DOWN) {
                switch (gameState) {
                    case PROLOGUE:
                        setGameState(GameState.MENU);
                        return true;

                    case MENU:
                        menuIdleTimer = 0f;
                        if (touchVirtualY >= 150f && touchVirtualY < 166f) {
                            selectedCharacter = 1;
                            menuOptionIndex = 0;
                            level.initLevel(selectedCharacter);
                            setGameState(GameState.STAGE_INTRO);
                            return true;
                        } else if (touchVirtualY >= 166f && touchVirtualY < 182f) {
                            selectedCharacter = 2;
                            menuOptionIndex = 1;
                            level.initLevel(selectedCharacter);
                            setGameState(GameState.STAGE_INTRO);
                            return true;
                        } else if (touchVirtualY >= 182f && touchVirtualY < 198f) {
                            menuOptionIndex = 2;
                            setGameState(GameState.HELP);
                            return true;
                        } else if (touchVirtualY >= 198f && touchVirtualY < 216f) {
                            menuOptionIndex = 3;
                            setGameState(GameState.PROLOGUE);
                            return true;
                        } else {
                            level.initLevel(selectedCharacter);
                            setGameState(GameState.STAGE_INTRO);
                            return true;
                        }

                    case HELP:
                        setGameState(GameState.MENU);
                        return true;

                    case STAGE_INTRO:
                        if (stageIntroTimer > 0.4f) {
                            setGameState(GameState.PLAYING);
                        }
                        return true;

                    case STAGE_CLEAR:
                        if (level.tallyFinished || stateTransitionTimer > 1.2f) {
                            advanceToNextStage();
                        }
                        return true;

                    case CONTINUE:
                        if (touchVirtualY >= 136f && touchVirtualY < 152f) {
                            continueOption = 0;
                            level.restartAfterContinue();
                            setGameState(GameState.STAGE_INTRO);
                            return true;
                        } else if (touchVirtualY >= 152f && touchVirtualY < 172f) {
                            continueOption = 1;
                            setGameState(GameState.GAME_OVER);
                            return true;
                        } else {
                            continueOption = (continueOption == 0) ? 1 : 0;
                            return true;
                        }

                    case GAME_OVER:
                        if (gameOverTimer > 1.2f) {
                            setGameState(GameState.MENU);
                        }
                        return true;

                    case VICTORY:
                        if (stateTransitionTimer > 1.0f) {
                            setGameState(GameState.MENU);
                        }
                        return true;

                    case PAUSED:
                        setGameState(GameState.PLAYING);
                        return true;
                }
            }
            return true;
        }

        // Gestión táctil durante el juego activo
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int pointerId = event.getPointerId(actionIndex);
                touchController.onTouchDown(pointerId, touchVirtualX, touchVirtualY,
                        level.player.x, level.player.y);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int count = event.getPointerCount();
                for (int i = 0; i < count; i++) {
                    int pointerId = event.getPointerId(i);
                    float vx = (event.getX(i) / dstRect.width()) * (float) virtualWidth;
                    float vy = (event.getY(i) / dstRect.height()) * (float) VIRTUAL_HEIGHT;

                    PointF newPos = touchController.onTouchMove(pointerId, vx, vy);
                    if (newPos != null) {
                        level.player.x = newPos.x;
                        level.player.y = newPos.y;
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerId = event.getPointerId(actionIndex);
                touchController.onTouchUp(pointerId, touchVirtualX, touchVirtualY,
                        level.player.x, level.player.y);
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                touchController.onTouchCancel(level.player.x, level.player.y);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}
