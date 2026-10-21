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
 * High-performance audio manager for Final Mission Android.
 * - SoundPool: Immediate, low-latency playback of concurrent sound effects (shots, lasers, hits, explosions).
 * - MediaPlayer: Background music (BGM) and jingles with volume normalization, clean looping, and thread safety.
 */
public class AudioManager {
    private static final String TAG = "AudioManager";

    private final Context context;
    private SoundPool soundPool;
    private final Map<String, Integer> soundMap = new HashMap<>();

    private MediaPlayer bgmPlayer;
    private MediaPlayer jinglePlayer;
    private String currentBgmTrack = null;

    private boolean isMuted = false;
    private float sfxVolume = 1.0f;
    private float bgmVolume = 0.85f;

    // Authentic SFX Identifiers
    public static final String SFX_DISPARO = "disparo.wav";
    public static final String SFX_DISPARO_L = "disparoL.wav";
    public static final String SFX_EXPLOSION = "explosion.wav";
    public static final String SFX_MURIENDO = "muriendo.wav";
    public static final String SFX_PODER = "poder.wav";
    public static final String SFX_PAUSA = "pausa.wav";
    public static final String SFX_MENU = "menu.wav";
    public static final String SFX_PRESENTACION = "precentacion1.wav";

    // Authentic BGM Identifiers
    public static final String BGM_INTRODUCCION = "introduccion.wav";
    public static final String BGM_MUSICA = "musica.wav";
    public static final String BGM_COMIENZO = "comienzo1.wav";
    public static final String BGM_PRESENTACION = "precentacion1.wav";
    public static final String BGM_FIN_JUEGO = "finDeJuego.wav";
    public static final String BGM_MENU = "menu.wav";

    public AudioManager(Context context) {
        this.context = context.getApplicationContext();
        initSoundPool();
        preloadSounds();
    }

    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(12)
                .setAudioAttributes(audioAttributes)
                .build();
    }

    private void preloadSounds() {
        String[] sfxFiles = {
                SFX_DISPARO,
                SFX_DISPARO_L,
                SFX_EXPLOSION,
                SFX_MURIENDO,
                SFX_PODER,
                SFX_PAUSA,
                SFX_MENU,
                SFX_PRESENTACION
        };

        for (String file : sfxFiles) {
            try {
                AssetFileDescriptor afd = context.getAssets().openFd("audio/" + file);
                int soundId = soundPool.load(afd, 1);
                soundMap.put(file, soundId);
                afd.close();
            } catch (IOException e) {
                Log.w(TAG, "Failed to preload sound effect: " + file, e);
            }
        }
    }

    /**
     * Plays a preloaded sound effect with current SFX volume.
     *
     * @param sfxName File name of the sound effect.
     */
    public synchronized void playSound(String sfxName) {
        if (isMuted || soundPool == null) return;
        Integer id = soundMap.get(sfxName);
        if (id != null && id > 0) {
            soundPool.play(id, sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    /**
     * Alias for playSound.
     */
    public void playSfx(String sfxName) {
        playSound(sfxName);
    }

    /**
     * Plays background music (BGM) with clean looping and volume normalization.
     * If the requested track is already playing, avoids jarring re-starts.
     *
     * @param bgmName File name of the BGM track.
     * @param loop    Whether playback should loop indefinitely.
     */
    public synchronized void playMusic(String bgmName, boolean loop) {
        if (isMuted) return;

        if (bgmPlayer != null && bgmName.equals(currentBgmTrack)) {
            try {
                if (bgmPlayer.isPlaying()) {
                    return;
                } else {
                    bgmPlayer.start();
                    return;
                }
            } catch (Exception ignored) {
                // If in invalid state, recreate below
            }
        }

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
            currentBgmTrack = bgmName;
        } catch (Exception e) {
            Log.e(TAG, "Error playing music track: " + bgmName, e);
            if (bgmPlayer != null) {
                try {
                    bgmPlayer.release();
                } catch (Exception ignored) {}
                bgmPlayer = null;
            }
            currentBgmTrack = null;
        }
    }

    /**
     * Plays a non-looping jingle or fanfare (such as stage intro or game over).
     *
     * @param jingleName   File name of the jingle.
     * @param onCompletion Callback invoked once the jingle finishes playing.
     */
    public synchronized void playJingle(String jingleName, final Runnable onCompletion) {
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
            Log.e(TAG, "Error playing jingle: " + jingleName, e);
            if (onCompletion != null) {
                onCompletion.run();
            }
        }
    }

    /**
     * Stops active background music playback.
     */
    public synchronized void stopMusic() {
        if (bgmPlayer != null) {
            try {
                if (bgmPlayer.isPlaying()) {
                    bgmPlayer.stop();
                }
                bgmPlayer.release();
            } catch (Exception ignored) {
            } finally {
                bgmPlayer = null;
                currentBgmTrack = null;
            }
        }
    }

    /**
     * Stops active jingle playback.
     */
    public synchronized void stopJingle() {
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

    /**
     * Pauses active background music playback.
     */
    public synchronized void pauseMusic() {
        if (bgmPlayer != null) {
            try {
                if (bgmPlayer.isPlaying()) {
                    bgmPlayer.pause();
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Resumes background music playback if paused.
     */
    public synchronized void resumeMusic() {
        if (bgmPlayer != null && !isMuted) {
            try {
                if (!bgmPlayer.isPlaying()) {
                    bgmPlayer.start();
                }
            } catch (Exception ignored) {}
        }
    }

    public synchronized void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public synchronized float getSfxVolume() {
        return sfxVolume;
    }

    public synchronized void setBgmVolume(float volume) {
        this.bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (bgmPlayer != null) {
            try {
                bgmPlayer.setVolume(this.bgmVolume, this.bgmVolume);
            } catch (Exception ignored) {}
        }
        if (jinglePlayer != null) {
            try {
                jinglePlayer.setVolume(this.bgmVolume, this.bgmVolume);
            } catch (Exception ignored) {}
        }
    }

    public synchronized float getBgmVolume() {
        return bgmVolume;
    }

    public synchronized void setMuted(boolean muted) {
        this.isMuted = muted;
        if (muted) {
            pauseMusic();
            stopJingle();
        } else {
            resumeMusic();
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    /**
     * Releases all audio resources and hardware decoders upon game exit.
     */
    public synchronized void release() {
        stopMusic();
        stopJingle();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        soundMap.clear();
    }
}
