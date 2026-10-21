package com.diamon.finalmission;

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
 * Gestor de Audio nativo para Final Mission Android.
 * - SoundPool: Efectos de sonido (SFX) con mínima latencia y múltiples canales simultáneos.
 * - MediaPlayer: Música de fondo (BGM) en bucle.
 */
public class AudioManager {
    private static final String TAG = "AudioManager";

    private final Context context;
    private SoundPool soundPool;
    private MediaPlayer bgmPlayer;

    private final Map<String, Integer> soundMap = new HashMap<>();
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;

    public AudioManager(Context context) {
        this.context = context.getApplicationContext();
        initSoundPool();
    }

    private void initSoundPool() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();

        // Precargar efectos de sonido de Final Mission
        loadSound("disparo.wav");
        loadSound("disparoL.wav");
        loadSound("explosion.wav");
        loadSound("poder.wav");
        loadSound("muriendo.wav");
        loadSound("pausa.wav");
        loadSound("finDeJuego.wav");
        loadSound("comienzo1.wav");
    }

    private void loadSound(String filename) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/" + filename);
            int soundId = soundPool.load(afd, 1);
            soundMap.put(filename, soundId);
        } catch (IOException e) {
            Log.w(TAG, "No se pudo cargar SFX: " + filename, e);
        }
    }

    public void playSound(String filename) {
        if (!soundEnabled || soundPool == null) return;
        Integer id = soundMap.get(filename);
        if (id != null) {
            soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    public void playMusic(String filename, boolean loop) {
        if (!musicEnabled) return;
        stopMusic();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/" + filename);
            bgmPlayer = new MediaPlayer();
            bgmPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            bgmPlayer.setLooping(loop);
            bgmPlayer.setVolume(0.8f, 0.8f);
            bgmPlayer.prepare();
            bgmPlayer.start();
        } catch (Exception e) {
            Log.w(TAG, "No se pudo reproducir música: " + filename, e);
        }
    }

    public void pauseMusic() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (musicEnabled && bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    public void stopMusic() {
        if (bgmPlayer != null) {
            try {
                if (bgmPlayer.isPlaying()) {
                    bgmPlayer.stop();
                }
                bgmPlayer.release();
            } catch (Exception ignored) {}
            bgmPlayer = null;
        }
    }

    public void release() {
        stopMusic();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        soundMap.clear();
    }
}
