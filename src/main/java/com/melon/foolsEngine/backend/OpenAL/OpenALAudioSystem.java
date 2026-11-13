package com.melon.foolsEngine.backend.OpenAL;

import com.melon.foolsEngine.api.audio.AudioClip;
import com.melon.foolsEngine.api.audio.AudioSource;
import com.melon.foolsEngine.api.audio.AudioSystem;
import com.melon.foolsEngine.util.PcmSound;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.EXTThreadLocalContext.*;

/** Short-sound playback using an independent OpenAL Soft device and context. */
public final class OpenALAudioSystem implements AudioSystem {
    private final long device;
    private final long context;
    private final ALCapabilities capabilities;
    private final Set<Clip> clips = new HashSet<>();
    private boolean closed;

    public OpenALAudioSystem() {
        long openedDevice = alcOpenDevice((ByteBuffer) null);
        if (openedDevice == 0) {
            throw new IllegalStateException("Cannot open the default OpenAL device");
        }
        long createdContext = 0;
        try {
            var deviceCapabilities = ALC.createCapabilities(openedDevice);
            if (!deviceCapabilities.ALC_EXT_thread_local_context) {
                throw new UnsupportedOperationException("OpenAL requires ALC_EXT_thread_local_context");
            }
            createdContext = alcCreateContext(openedDevice, (IntBuffer) null);
            if (createdContext == 0) {
                throw new IllegalStateException("Cannot create an OpenAL context");
            }
            capabilities = withContext(createdContext, () -> AL.createCapabilities(deviceCapabilities));
        } catch (RuntimeException | Error e) {
            if (createdContext != 0) {
                alcDestroyContext(createdContext);
            }
            alcCloseDevice(openedDevice);
            throw e;
        }
        device = openedDevice;
        context = createdContext;
    }

    @Override
    public synchronized AudioClip load(InputStream input) throws IOException {
        requireOpen();
        PcmSound pcm = PcmSound.decode(input, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
        int format = pcm.format().getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        ByteBuffer samples = MemoryUtil.memAlloc(pcm.data().length);
        try {
            samples.put(pcm.data()).flip();
            return inContext(() -> {
                int buffer = alGenBuffers();
                checkError("create buffer");
                try {
                    alBufferData(buffer, format, samples, Math.round(pcm.format().getSampleRate()));
                    checkError("upload samples");
                } catch (RuntimeException | Error e) {
                    alDeleteBuffers(buffer);
                    throw e;
                }
                Clip clip = new Clip(buffer, pcm.durationSeconds());
                clips.add(clip);
                return clip;
            });
        } finally {
            MemoryUtil.memFree(samples);
        }
    }

    @Override
    public synchronized AudioSource createSource(AudioClip clip) {
        requireOpen();
        if (!(clip instanceof Clip buffer) || !clips.contains(buffer)) {
            throw new IllegalArgumentException("Expected an open clip from this audio system");
        }
        return inContext(() -> {
            int id = alGenSources();
            checkError("create source");
            try {
                alSourcei(id, AL_BUFFER, buffer.id);
                checkError("attach buffer to source");
            } catch (RuntimeException | Error e) {
                alDeleteSources(id);
                throw e;
            }
            Source source = new Source(buffer, id);
            buffer.sources.add(source);
            return source;
        });
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        // Destroying the context also releases all sources and buffers it owns.
        alcDestroyContext(context);
        alcCloseDevice(device);
        for (Clip clip : clips) {
            clip.sources.clear();
        }
        clips.clear();
        closed = true;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Audio system is closed");
        }
    }

    private <T> T inContext(Supplier<T> action) {
        return withContext(context, () -> {
            AL.setCurrentThread(capabilities);
            return action.get();
        });
    }

    private static <T> T withContext(long context, Supplier<T> action) {
        long previous = alcGetThreadContext();
        ALCapabilities previousCapabilities = previous == 0 ? null : AL.getCapabilities();
        if (!alcSetThreadContext(context)) {
            throw new IllegalStateException("Cannot activate the OpenAL context");
        }
        try {
            return action.get();
        } finally {
            boolean restored = alcSetThreadContext(previous);
            AL.setCurrentThread(restored ? previousCapabilities : null);
            if (!restored) {
                alcSetThreadContext(0);
                throw new IllegalStateException("Cannot restore the previous OpenAL context");
            }
        }
    }

    private static void checkError(String action) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new IllegalStateException("OpenAL failed to " + action + " (error " + error + ")");
        }
    }

    private final class Clip implements AudioClip {
        private final int id;
        private final double duration;
        private final Set<Source> sources = new HashSet<>();

        private Clip(int id, double duration) {
            this.id = id;
            this.duration = duration;
        }

        @Override
        public double durationSeconds() {
            return duration;
        }

        @Override
        public void close() {
            synchronized (OpenALAudioSystem.this) {
                if (!clips.contains(this)) {
                    return;
                }
                inContext(() -> {
                    for (Source source : sources) {
                        alDeleteSources(source.id);
                    }
                    sources.clear();
                    alDeleteBuffers(id);
                    clips.remove(this);
                    return null;
                });
            }
        }
    }

    private final class Source implements AudioSource {
        private final Clip clip;
        private final int id;
        private boolean looping;

        private Source(Clip clip, int id) {
            this.clip = clip;
            this.id = id;
        }

        @Override
        public void play() {
            command(() -> {
                alSourceRewind(id);
                start();
            });
        }

        @Override public void pause() { command(() -> alSourcePause(id)); }
        @Override public void stop() { command(() -> alSourceRewind(id)); }

        @Override
        public void resume() {
            command(() -> {
                if (alGetSourcei(id, AL_SOURCE_STATE) == AL_PAUSED) {
                    start();
                }
            });
        }

        private void start() {
            alSourcei(id, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
            alSourcePlay(id);
        }

        @Override
        public boolean isPlaying() {
            synchronized (OpenALAudioSystem.this) {
                return clip.sources.contains(this)
                        && inContext(() -> alGetSourcei(id, AL_SOURCE_STATE) == AL_PLAYING);
            }
        }

        @Override
        public void setLooping(boolean looping) {
            synchronized (OpenALAudioSystem.this) {
                requireSource();
                this.looping = looping;
            }
        }

        @Override
        public void setVolume(float volume) {
            if (!Float.isFinite(volume) || volume < 0 || volume > 1) {
                throw new IllegalArgumentException("Volume must be between 0 and 1");
            }
            command(() -> alSourcef(id, AL_GAIN, volume));
        }

        @Override
        public void close() {
            synchronized (OpenALAudioSystem.this) {
                if (clip.sources.contains(this)) {
                    inContext(() -> {
                        alDeleteSources(id);
                        checkError("delete source");
                        clip.sources.remove(this);
                        return null;
                    });
                }
            }
        }

        private void command(Runnable action) {
            synchronized (OpenALAudioSystem.this) {
                requireSource();
                inContext(() -> {
                    action.run();
                    checkError("control source");
                    return null;
                });
            }
        }

        private void requireSource() {
            if (!clip.sources.contains(this)) {
                throw new IllegalStateException("Audio source is closed");
            }
        }
    }
}
