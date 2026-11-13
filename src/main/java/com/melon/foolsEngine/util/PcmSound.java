package com.melon.foolsEngine.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Shared decoder for short mono or stereo sounds in signed 16-bit PCM. */
public record PcmSound(AudioFormat format, byte[] data) {
    private static final int PCM_SAMPLE_BITS = 16;

    /** Converts the source format to the signed 16-bit PCM used by the playback backends. */
    public static PcmSound decode(InputStream input, boolean bigEndian) throws IOException {
        Objects.requireNonNull(input, "input");
        InputStream borrowed = new FilterInputStream(input) {
            @Override public void close() {}
        };
        try (var buffered = new BufferedInputStream(borrowed);
             AudioInputStream encoded = javax.sound.sampled.AudioSystem.getAudioInputStream(buffered)) {
            AudioFormat source = encoded.getFormat();
            int channels = source.getChannels();
            float rate = source.getSampleRate();
            if ((channels != 1 && channels != 2) || !Float.isFinite(rate) || rate < 1) {
                throw new IOException("Expected a mono or stereo sound with a valid sample rate");
            }
            AudioFormat format = new AudioFormat(rate, PCM_SAMPLE_BITS, channels, true, bigEndian);
            try (AudioInputStream pcm = javax.sound.sampled.AudioSystem.getAudioInputStream(format, encoded)) {
                byte[] data = pcm.readAllBytes();
                if (data.length == 0) {
                    throw new IOException("Sound has no audio frames");
                }
                return new PcmSound(format, data);
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
            throw new IOException("Unsupported audio format", e);
        }
    }

    public double durationSeconds() {
        return data.length / format.getFrameSize() / (double) format.getFrameRate();
    }
}
