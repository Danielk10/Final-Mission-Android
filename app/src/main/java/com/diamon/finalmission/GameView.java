package com.diamon.finalmission;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Motor Gráfico 2D nativo en SurfaceView a 60 FPS para Final Mission Android.
 * Renderiza en resolución virtual retro NES (256 x 224) con preservación exacta
 * de relación de aspecto mediante letterboxing/pillarboxing automático.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final int VIRTUAL_WIDTH = 256;
    public static final int VIRTUAL_HEIGHT = 224;

    public static final int STATE_TITLE = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_GAMEOVER = 2;
    public static final int STATE_VICTORY = 3;

    private int gameState = STATE_TITLE;

    private SurfaceHolder holder;
    private Thread gameThread;
    private volatile boolean isRunning = false;

    // Bitmap de rasterización virtual retro (256x224)
    private Bitmap virtualBitmap;
    private Canvas virtualCanvas;
    private Rect srcRect;
    private Rect dstRect;
    private final Paint scalePaint = new Paint();
    private final Paint textPaint = new Paint();

    // Entidades del juego
    private Player player;
    private Satellite satellite1;
    private Satellite satellite2;
    private TouchController touchController;
    private AudioManager audioManager;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private final List<PowerUp> powerups = new ArrayList<>();

    // Fondos de escenario
    private final List<Bitmap> stageBackgrounds = new ArrayList<>();
    private float stageScrollX = 0f;
    private final float baseScrollSpeed = 0.6f;
    private int currentStageIndex = 0;

    // Diccionario de sprites cargados en memoria
    private final Map<String, Bitmap> spriteMap = new HashMap<>();
    private final List<Bitmap> playerRightSprites = new ArrayList<>();
    private final List<Bitmap> playerLeftSprites = new ArrayList<>();

    private int score = 0;
    private float enemySpawnTimer = 0f;
    private final Random random = new Random();
    private boolean bossSpawned = false;

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

        virtualBitmap = Bitmap.createBitmap(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, Bitmap.Config.ARGB_8888);
        virtualCanvas = new Canvas(virtualBitmap);

        srcRect = new Rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        dstRect = new Rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        scalePaint.setFilterBitmap(false); // Estilo retro pixel-perfect nítido

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f);
        textPaint.setFakeBoldText(true);
        textPaint.setAntiAlias(false);

        audioManager = new AudioManager(getContext());
        touchController = new TouchController();
        player = new Player();
        satellite1 = new Satellite(0);
        satellite2 = new Satellite(1);

        loadGameAssets();
        audioManager.playMusic("menu.wav", true);
    }

    private void loadGameAssets() {
        AssetManager assets = getContext().getAssets();

        // 1. Cargar sprites del jugador Arnold
        for (int i = 1; i <= 6; i++) {
            Bitmap b = loadBitmap(assets, "sprites/jugador1D" + i + ".png");
            if (b != null) playerRightSprites.add(b);
        }
        for (int i = 1; i <= 3; i++) {
            Bitmap b = loadBitmap(assets, "sprites/jugador1I" + i + ".png");
            if (b != null) playerLeftSprites.add(b);
        }

        // 2. Cargar fondos de escenario
        for (int i = 1; i <= 20; i++) {
            Bitmap b = loadBitmap(assets, "sprites/fondo" + i + ".png");
            if (b != null) stageBackgrounds.add(b);
        }
        Bitmap bossArena = loadBitmap(assets, "sprites/fondo21MGF.png");
        if (bossArena != null) stageBackgrounds.add(bossArena);

        // 3. Cargar sprites clave al mapa
        String[] keys = {
            "sateliteHD1.png", "sateliteHD2.png", "sateliteHI1.png", "sateliteHI2.png",
            "sateliteVA1.png", "sateliteVA2.png", "sateliteVB1.png", "sateliteVB2.png",
            "sateliteDD1.png", "sateliteDD2.png", "sateliteDI1.png", "sateliteDI2.png",
            "balaSatelite.png", "balaHD.png", "balaHI.png", "balaWD1.png", "balaWI1.png",
            "balaLD3.png", "balaLI3.png", "balaE1.png", "misilD1.png", "misilH1.png",
            "antiAreoD1.png", "robotI.png", "maquinaFinal.png",
            "voladorD1.png", "voladorI1.png", "voladorI2.png", "voladorI3.png",
            "saltador1.png", "saltador2.png", "saltador3.png", "saltador4.png",
            "lanzaMisilI1.png", "lanzaMisilI2.png", "lanzaMisilI3.png",
            "explosion1.png", "explosion2.png", "explosion3.png", "explosion4.png",
            "explosionB1.png", "explosionB2.png", "explosionB3.png", "explosionB4.png", "explosionB5.png",
            "poderW.png", "poderL.png", "poderS.png", "poderB.png",
            "vida1.png", "vida2.png", "menu1.png", "logo.png"
        };

        for (String k : keys) {
            Bitmap bmp = loadBitmap(assets, "sprites/" + k);
            if (bmp != null) {
                spriteMap.put(k, bmp);
            }
        }
    }

    private Bitmap loadBitmap(AssetManager assets, String path) {
        try {
            InputStream is = assets.open(path);
            Bitmap b = BitmapFactory.decodeStream(is);
            is.close();
            return b;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        gameThread = new Thread(this, "FinalMissionLoop");
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Cálculo de letterboxing/pillarboxing para preservar relación 256:224 (8:7 / 4:3 aprox)
        float targetAspect = (float) VIRTUAL_WIDTH / (float) VIRTUAL_HEIGHT;
        float screenAspect = (float) width / (float) height;

        int finalW, finalH;
        int left, top;

        if (screenAspect > targetAspect) {
            // Pantalla panorámica: Pilarbox (franjas negras a los lados)
            finalH = height;
            finalW = (int) (height * targetAspect);
            left = (width - finalW) / 2;
            top = 0;
        } else {
            // Letterbox (franjas negras arriba y abajo)
            finalW = width;
            finalH = (int) (width / targetAspect);
            left = 0;
            top = (height - finalH) / 2;
        }

        dstRect.set(left, top, left + finalW, top + finalH);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException ignored) {}
        if (audioManager != null) {
            audioManager.release();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final long nsPerFrame = 1_000_000_000L / 60L; // 60 FPS fijos

        while (isRunning) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            lastTime = now;

            // Clamping del delta time para estabilidad en caso de pausa
            if (dt > 0.05f) dt = 0.05f;

            update(dt);
            render();

            long elapsed = System.nanoTime() - now;
            long sleepNs = nsPerFrame - elapsed;
            if (sleepNs > 1_000_000L) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update(float dt) {
        if (gameState == STATE_TITLE) {
            return;
        }

        if (gameState == STATE_PLAYING) {
            touchController.update(dt);

            // 1. Scroll de escenario
            stageScrollX += baseScrollSpeed;
            if (stageScrollX >= 256f) {
                stageScrollX -= 256f;
                if (currentStageIndex < stageBackgrounds.size() - 2) {
                    currentStageIndex++;
                } else if (!bossSpawned) {
                    bossSpawned = true;
                    enemies.add(new Enemy(280f, 80f, Enemy.TYPE_BOSS));
                }
            }

            // 2. Actualizar Jugador y Satélites
            player.update(dt, touchController, bullets, audioManager, playerRightSprites, playerLeftSprites);
            satellite1.update(dt, player, touchController, bullets);
            satellite2.update(dt, player, touchController, bullets);

            // 3. Generar oleadas continuas de enemigos NES
            if (!bossSpawned) {
                enemySpawnTimer += dt;
                if (enemySpawnTimer >= 2.2f) {
                    enemySpawnTimer = 0f;
                    int r = random.nextInt(4);
                    float spawnY = 30f + random.nextFloat() * 140f;
                    enemies.add(new Enemy(270f, spawnY, r));
                }
            }

            // 4. Actualizar Balas
            Iterator<Bullet> bIt = bullets.iterator();
            while (bIt.hasNext()) {
                Bullet b = bIt.next();
                b.update(dt);
                if (!b.isActive()) {
                    bIt.remove();
                    continue;
                }

                // Colisión balas del jugador con enemigos
                if (b.isPlayerBullet()) {
                    RectF bBox = b.getBoundingBox();
                    for (Enemy e : enemies) {
                        if (e.isActive() && RectF.intersects(bBox, e.getBoundingBox())) {
                            e.takeDamage(b.getDamage());
                            b.setActive(false);
                            explosions.add(new Explosion(b.getX(), b.getY(), false));
                            audioManager.playSound("explosion.wav");
                            score += 100;

                            // Posible caída de Power-Up
                            if (!e.isActive() && random.nextFloat() < 0.28f) {
                                char[] pTypes = {'W', 'L', 'S', 'B'};
                                powerups.add(new PowerUp(e.getX(), e.getY(), pTypes[random.nextInt(pTypes.length)]));
                            }
                            break;
                        }
                    }
                } else {
                    // Bala enemiga impacta al jugador
                    RectF bBox = b.getBoundingBox();
                    RectF pBox = new RectF(player.getX(), player.getY(), player.getX() + player.getWidth(), player.getY() + player.getHeight());
                    if (RectF.intersects(bBox, pBox)) {
                        b.setActive(false);
                        player.takeDamage(1, audioManager);
                        explosions.add(new Explosion(player.getX() + player.getWidth() / 2f, player.getY() + player.getHeight() / 2f, false));
                        if (player.isDead()) {
                            gameState = STATE_GAMEOVER;
                            audioManager.playSound("finDeJuego.wav");
                        }
                    }
                }
            }

            // 5. Actualizar Enemigos
            Iterator<Enemy> eIt = enemies.iterator();
            while (eIt.hasNext()) {
                Enemy e = eIt.next();
                e.update(dt, player, bullets, audioManager, baseScrollSpeed);
                if (!e.isActive()) {
                    if (e.getType() == Enemy.TYPE_BOSS) {
                        gameState = STATE_VICTORY;
                        score += 10000;
                    }
                    explosions.add(new Explosion(e.getX() + 12f, e.getY() + 12f, e.getType() == Enemy.TYPE_BOSS));
                    eIt.remove();
                    continue;
                }

                // Colisión directa de cuerpo a cuerpo enemigo con jugador
                RectF eBox = e.getBoundingBox();
                RectF pBox = new RectF(player.getX(), player.getY(), player.getX() + player.getWidth(), player.getY() + player.getHeight());
                if (RectF.intersects(eBox, pBox)) {
                    player.takeDamage(1, audioManager);
                    if (player.isDead()) {
                        gameState = STATE_GAMEOVER;
                        audioManager.playSound("finDeJuego.wav");
                    }
                }
            }

            // 6. Actualizar Power-Ups
            Iterator<PowerUp> pIt = powerups.iterator();
            while (pIt.hasNext()) {
                PowerUp p = pIt.next();
                p.update(dt, baseScrollSpeed);
                if (!p.isActive()) {
                    pIt.remove();
                    continue;
                }

                RectF pBox = new RectF(player.getX(), player.getY(), player.getX() + player.getWidth(), player.getY() + player.getHeight());
                if (RectF.intersects(p.getBoundingBox(), pBox)) {
                    player.applyPowerUp(p.getType(), audioManager);
                    p.setActive(false);
                    pIt.remove();
                }
            }

            // 7. Actualizar Explosiones
            Iterator<Explosion> exIt = explosions.iterator();
            while (exIt.hasNext()) {
                Explosion ex = exIt.next();
                ex.update(dt);
                if (!ex.isActive()) {
                    exIt.remove();
                }
            }
        }
    }

    private void render() {
        // 1. Dibujar en el canvas virtual retro NES (256 x 224)
        virtualCanvas.drawColor(Color.BLACK);

        if (gameState == STATE_TITLE) {
            renderTitleScreen(virtualCanvas);
        } else {
            // Fondo Parallax de la etapa
            renderBackground(virtualCanvas);

            // Entidades del juego
            for (PowerUp p : powerups) p.draw(virtualCanvas, spriteMap);
            for (Enemy e : enemies) e.draw(virtualCanvas, spriteMap);
            player.draw(virtualCanvas, playerRightSprites, playerLeftSprites);
            satellite1.draw(virtualCanvas, spriteMap);
            satellite2.draw(virtualCanvas, spriteMap);
            for (Bullet b : bullets) b.draw(virtualCanvas, spriteMap);
            for (Explosion ex : explosions) ex.draw(virtualCanvas, spriteMap);

            // Interfaz de Usuario / HUD NES
            renderHUD(virtualCanvas);

            if (gameState == STATE_GAMEOVER) {
                renderGameOver(virtualCanvas);
            } else if (gameState == STATE_VICTORY) {
                renderVictory(virtualCanvas);
            }
        }

        // 2. Volcar el canvas virtual al SurfaceView escalando con aspect ratio
        Canvas screenCanvas = null;
        try {
            screenCanvas = holder.lockHardwareCanvas();
            if (screenCanvas == null) {
                screenCanvas = holder.lockCanvas();
            }
            if (screenCanvas != null) {
                screenCanvas.drawColor(Color.BLACK); // Franjas negras de letterbox
                screenCanvas.drawBitmap(virtualBitmap, srcRect, dstRect, scalePaint);
            }
        } catch (Exception ignored) {
        } finally {
            if (screenCanvas != null) {
                try {
                    holder.unlockCanvasAndPost(screenCanvas);
                } catch (Exception ignored) {}
            }
        }
    }

    private void renderBackground(Canvas canvas) {
        if (stageBackgrounds.isEmpty()) return;

        int bgIdx1 = Math.min(currentStageIndex, stageBackgrounds.size() - 1);
        int bgIdx2 = Math.min(currentStageIndex + 1, stageBackgrounds.size() - 1);

        Bitmap b1 = stageBackgrounds.get(bgIdx1);
        Bitmap b2 = stageBackgrounds.get(bgIdx2);

        if (b1 != null) {
            canvas.drawBitmap(b1, -stageScrollX, 0f, scalePaint);
        }
        if (b2 != null) {
            canvas.drawBitmap(b2, 256f - stageScrollX, 0f, scalePaint);
        }
    }

    private void renderHUD(Canvas canvas) {
        // Indicador de Vida NES (Segmentos vida1 / vida2)
        Bitmap lifeFull = spriteMap.get("vida1.png");
        Bitmap lifeEmpty = spriteMap.get("vida2.png");

        for (int i = 0; i < 6; i++) {
            Bitmap b = (i < player.getHealth()) ? lifeFull : lifeEmpty;
            if (b != null) {
                canvas.drawBitmap(b, 10f + i * 8f, 6f, scalePaint);
            }
        }

        // Tipo de Arma activa
        String weaponStr = "NORM";
        if (player.getPowerType() == Player.POWER_WIDE) weaponStr = "WIDE";
        else if (player.getPowerType() == Player.POWER_LASER) weaponStr = "LASR";

        // Satélites Bloqueados o Libres
        String satLock = touchController.isSatelliteLocked() ? "SAT:LOCK" : "SAT:ORBIT";

        textPaint.setColor(Color.WHITE);
        canvas.drawText("P1: " + score, 70f, 13f, textPaint);
        canvas.drawText("W: " + weaponStr, 150f, 13f, textPaint);
        textPaint.setColor(touchController.isSatelliteLocked() ? Color.YELLOW : Color.CYAN);
        canvas.drawText(satLock, 195f, 13f, textPaint);
    }

    private void renderTitleScreen(Canvas canvas) {
        Bitmap logo = spriteMap.get("logo.png");
        if (logo != null) {
            Rect dst = new Rect(28, 25, 228, 115);
            canvas.drawBitmap(logo, null, dst, scalePaint);
        } else {
            textPaint.setColor(Color.RED);
            textPaint.setTextSize(18f);
            canvas.drawText("FINAL MISSION", 50f, 70f, textPaint);
            textPaint.setTextSize(10f);
        }

        textPaint.setColor(Color.WHITE);
        canvas.drawText("NATSUME 1990 - ANDROID 2026", 52f, 140f, textPaint);
        textPaint.setColor(Color.YELLOW);
        canvas.drawText("TOCA PARA COMENZAR LA MISION", 42f, 175f, textPaint);
    }

    private void renderGameOver(Canvas canvas) {
        Bitmap fin = spriteMap.get("finJuego.png");
        if (fin != null) {
            canvas.drawBitmap(fin, 32f, 80f, scalePaint);
        } else {
            textPaint.setColor(Color.RED);
            textPaint.setTextSize(16f);
            canvas.drawText("GAME OVER", 80f, 100f, textPaint);
            textPaint.setTextSize(10f);
        }
        textPaint.setColor(Color.WHITE);
        canvas.drawText("TOCA PARA REINTENTAR", 65f, 145f, textPaint);
    }

    private void renderVictory(Canvas canvas) {
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(14f);
        canvas.drawText("¡MISION CUMPLIDA!", 60f, 95f, textPaint);
        textPaint.setTextSize(10f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("PUNTOS: " + score, 85f, 120f, textPaint);
        canvas.drawText("TOCA PARA CONTINUAR", 68f, 150f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        // Convertir coordenadas físicas a virtuales NES (256x224)
        float px = event.getX(actionIndex);
        float py = event.getY(actionIndex);

        float vx = (px - dstRect.left) * ((float) VIRTUAL_WIDTH / Math.max(1f, dstRect.width()));
        float vy = (py - dstRect.top) * ((float) VIRTUAL_HEIGHT / Math.max(1f, dstRect.height()));

        if (gameState == STATE_TITLE) {
            if (action == MotionEvent.ACTION_DOWN) {
                gameState = STATE_PLAYING;
                audioManager.playSound("comienzo1.wav");
                audioManager.playMusic("musica.wav", true);
            }
            return true;
        }

        if (gameState == STATE_GAMEOVER || gameState == STATE_VICTORY) {
            if (action == MotionEvent.ACTION_DOWN) {
                // Reiniciar partida
                player = new Player();
                satellite1 = new Satellite(0);
                satellite2 = new Satellite(1);
                bullets.clear();
                enemies.clear();
                explosions.clear();
                powerups.clear();
                score = 0;
                stageScrollX = 0f;
                currentStageIndex = 0;
                bossSpawned = false;
                gameState = STATE_PLAYING;
                audioManager.playMusic("musica.wav", true);
            }
            return true;
        }

        int pointerId = event.getPointerId(actionIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                touchController.onTouchDown(pointerId, vx, vy, player.getX(), player.getY());
                return true;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    float curVx = (event.getX(i) - dstRect.left) * ((float) VIRTUAL_WIDTH / Math.max(1f, dstRect.width()));
                    float curVy = (event.getY(i) - dstRect.top) * ((float) VIRTUAL_HEIGHT / Math.max(1f, dstRect.height()));
                    touchController.onTouchMove(pId, curVx, curVy, player);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                touchController.onTouchUp(pointerId, vx, vy, player);
                return true;

            case MotionEvent.ACTION_CANCEL:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    touchController.onTouchUp(event.getPointerId(i), vx, vy, player);
                }
                return true;
        }

        return super.onTouchEvent(event);
    }
}
