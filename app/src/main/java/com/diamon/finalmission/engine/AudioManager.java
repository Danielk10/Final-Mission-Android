package com.diamon.finalmission.engine;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de audio para Final Mission.
 * Emplea SoundPool para efectos de sonido inmediatos de baja latencia
 * y MediaPlayer para música de fondo (BGM) y cortinillas.
 */
public class AudioManager {
    private static final String TAG = "AudioManager";

    private final Context context;
    private SoundPool soundPool;
    private final Map<String, Integer> soundMap = new HashMap<>();

    private MediaPlayer bgmPlayer;
    private MediaPlayer jinglePlayer;
    private boolean isMuted = false;
    private float sfxVolume = 1.0f;
    private float bgmVolume = 0.85f;

    // Nombres de archivos SFX comunes
    public static final String SFX_DISPARO = "disparo.wav";
    public static final String SFX_DISPARO_L = "disparoL.wav";
    public static final String SFX_EXPLOSION = "explosion.wav";
    public static final String SFX_MURIENDO = "muriendo.wav";
    public static final String SFX_PODER = "poder.wav";
    public static final String SFX_PAUSA = "pausa.wav";
    public static final String SFX_PRESENTACION = "precentacion1.wav";

    // Nombres de archivos BGM
    public static final String BGM_MUSICA = "musica.wav";
    public static final String BGM_MENU = "menu.wav";
    public static final String BGM_INTRODUCCION = "introduccion.wav";
    public static final String BGM_COMIENZO = "comienzo1.wav";
    public static final String BGM_FIN_JUEGO = "finDeJuego.wav";

    public AudioManager(Context context) {
        this.context = context.getApplicationContext();
        initSoundPool();
        loadSounds();
    }

    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build();
    }

    private void loadSounds() {
        String[] sfxFiles = {
                SFX_DISPARO,
                SFX_DISPARO_L,
                SFX_EXPLOSION,
                SFX_MURIENDO,
                SFX_PODER,
                SFX_PAUSA,
                SFX_PRESENTACION
        };

        for (String file : sfxFiles) {
            try {
                AssetFileDescriptor afd = context.getAssets().openFd("audio/" + file);
                int soundId = soundPool.load(afd, 1);
                soundMap.put(file, soundId);
                afd.close();
            } catch (IOException e) {
                Log.w(TAG, "No se pudo cargar efecto: " + file, e);
            }
        }
    }

    public void playSound(String sfxName) {
        if (isMuted || soundPool == null) return;
        Integer id = soundMap.get(sfxName);
        if (id != null && id > 0) {
            soundPool.play(id, sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    public void playMusic(String bgmName, boolean loop) {
        if (isMuted) return;
        stopMusic();

        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/" + bgmName);
            bgmPlayer = new MediaPlayer();
            bgmPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            bgmPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            bgmPlayer.setLooping(loop);
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
            bgmPlayer.prepare();
            bgmPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo música: " + bgmName, e);
            if (bgmPlayer != null) {
                bgmPlayer.release();
                bgmPlayer = null;
            }
        }
    }

    public void playJingle(String jingleName, final Runnable onCompletion) {
        if (isMuted) {
            if (onCompletion != null) onCompletion.run();
            return;
        }

        stopJingle();

        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/" + jingleName);
            jinglePlayer = new MediaPlayer();
            jinglePlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            jinglePlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            jinglePlayer.setLooping(false);
            jinglePlayer.setVolume(bgmVolume, bgmVolume);
            jinglePlayer.setOnCompletionListener(mp -> {
                if (onCompletion != null) {
                    onCompletion.run();
                }
            });
            jinglePlayer.prepare();
            jinglePlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error reproduciendo jingle: " + jingleName, e);
            if (onCompletion != null) {
                onCompletion.run();
            }
        }
    }

    public void stopMusic() {
        if (bgmPlayer != null) {
            try {
                if (bgmPlayer.isPlaying()) {
                    bgmPlayer.stop();
                }
                bgmPlayer.release();
            } catch (Exception ignored) {
            } finally {
                bgmPlayer = null;
            }
        }
    }

    public void stopJingle() {
        if (jinglePlayer != null) {
            try {
                if (jinglePlayer.isPlaying()) {
                    jinglePlayer.stop();
                }
                jinglePlayer.release();
            } catch (Exception ignored) {
            } finally {
                jinglePlayer = null;
            }
        }
    }

    public void pauseMusic() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (bgmPlayer != null && !isMuted && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    public void release() {
        stopMusic();
        stopJingle();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        soundMap.clear();
    }
}
