package com.melon.foolsEngine.backend.JavaSound;

import com.melon.foolsEngine.api.audio.AudioClip;
import com.melon.foolsEngine.api.audio.AudioSource;
import com.melon.foolsEngine.api.audio.AudioSystem;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Java Sound playback for short sounds such as mono or stereo PCM WAV files. */
public final class JavaSoundAudioSystem implements AudioSystem {
    public static AudioSystem create() {
        return new JavaSoundAudioSystem();
    }

    /** @deprecated Select Java Sound through the API builder. */
    @Deprecated
    public JavaSoundAudioSystem() {}

    private final Set<DecodedClip> clips = new HashSet<>();
    private boolean closed;

    @Override
    public synchronized AudioClip load(InputStream input) throws IOException {
        requireOpen();
        Objects.requireNonNull(input, "input");
        InputStream borrowed = new FilterInputStream(input) {
            @Override
            public void close() {}
        };
        try (var buffered = new BufferedInputStream(borrowed);
             AudioInputStream encoded = javax.sound.sampled.AudioSystem.getAudioInputStream(buffered)) {
            AudioFormat source = encoded.getFormat();
            int channels = source.getChannels();
            if ((channels != 1 && channels != 2) || source.getSampleRate() <= 0) {
                throw new IOException("Expected a mono or stereo sound with a valid sample rate");
            }
            AudioFormat format = new AudioFormat(source.getSampleRate(), 16, channels, true, false);
            try (AudioInputStream pcm = javax.sound.sampled.AudioSystem.getAudioInputStream(format, encoded)) {
                byte[] data = pcm.readAllBytes();
                if (data.length == 0) {
                    throw new IOException("Sound has no audio frames");
                }
                DecodedClip clip = new DecodedClip(format, data);
                clips.add(clip);
                return clip;
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
            throw new IOException("Unsupported audio format", e);
        }
    }

    @Override
    public synchronized AudioSource createSource(AudioClip clip) {
        requireOpen();
        if (!(clip instanceof DecodedClip decoded) || !clips.contains(decoded)) {
            throw new IllegalArgumentException("Expected an open clip from this audio system");
        }
        Clip line = null;
        try {
            line = javax.sound.sampled.AudioSystem.getClip();
            line.open(decoded.format, decoded.data, 0, decoded.data.length);
            Source source = new Source(decoded, line);
            decoded.sources.add(source);
            return source;
        } catch (LineUnavailableException | IllegalArgumentException e) {
            if (line != null) {
                line.close();
            }
            throw new IllegalStateException("Cannot open an audio playback line", e);
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        for (DecodedClip clip : List.copyOf(clips)) {
            clip.close();
        }
        closed = true;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Audio system is closed");
        }
    }

    private final class DecodedClip implements AudioClip {
        private final AudioFormat format;
        private final double duration;
        private final Set<Source> sources = new HashSet<>();
        private byte[] data;

        private DecodedClip(AudioFormat format, byte[] data) {
            this.format = format;
            this.data = data;
            this.duration = data.length / format.getFrameSize() / (double) format.getFrameRate();
        }

        @Override
        public double durationSeconds() {
            return duration;
        }

        @Override
        public void close() {
            synchronized (JavaSoundAudioSystem.this) {
                for (Source source : List.copyOf(sources)) {
                    source.close();
                }
                clips.remove(this);
                data = null;
            }
        }
    }

    private final class Source implements AudioSource {
        private final DecodedClip owner;
        private final Clip line;
        private boolean looping;
        private boolean paused;

        private Source(DecodedClip owner, Clip line) {
            this.owner = owner;
            this.line = line;
        }

        @Override
        public void play() {
            synchronized (JavaSoundAudioSystem.this) {
                stop();
                startAtCursor();
            }
        }

        @Override
        public void pause() {
            synchronized (JavaSoundAudioSystem.this) {
                requireLine();
                if (line.isRunning()) {
                    line.stop();
                    paused = true;
                }
            }
        }

        @Override
        public void resume() {
            synchronized (JavaSoundAudioSystem.this) {
                requireLine();
                if (paused) {
                    startAtCursor();
                }
            }
        }

        private void startAtCursor() {
            paused = false;
            if (looping) {
                line.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                line.start();
            }
        }

        @Override
        public void stop() {
            synchronized (JavaSoundAudioSystem.this) {
                requireLine();
                line.stop();
                line.setFramePosition(0);
                paused = false;
            }
        }

        @Override
        public boolean isPlaying() {
            synchronized (JavaSoundAudioSystem.this) {
                return line.isRunning();
            }
        }

        @Override
        public void setLooping(boolean looping) {
            synchronized (JavaSoundAudioSystem.this) {
                requireLine();
                this.looping = looping;
            }
        }

        @Override
        public void setVolume(float volume) {
            if (!Float.isFinite(volume) || volume < 0 || volume > 1) {
                throw new IllegalArgumentException("Volume must be between 0 and 1");
            }
            synchronized (JavaSoundAudioSystem.this) {
                requireLine();
                if (!line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    throw new UnsupportedOperationException("Playback line does not support volume control");
                }
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float decibels = volume == 0 ? gain.getMinimum() : (float) (20 * Math.log10(volume));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
            }
        }

        @Override
        public void close() {
            synchronized (JavaSoundAudioSystem.this) {
                if (owner.sources.remove(this)) {
                    line.close();
                }
            }
        }

        private void requireLine() {
            if (!line.isOpen()) {
                throw new IllegalStateException("Audio source is closed");
            }
        }
    }
}
