package com.melon.foolsEngine.api.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Owns a collection of audio resources independently of the graphics backend. */
public interface AudioSystem extends AutoCloseable {
    /** Decodes a short sound into memory. The caller owns the input stream. */
    AudioClip load(InputStream input) throws IOException;

    default AudioClip load(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return load(input);
        }
    }

    /** Creates an independent playback instance for a clip from this system. */
    AudioSource createSource(AudioClip clip);

    /** Stops playback and releases all sources and clips. Safe to call twice. */
    @Override
    void close();
}
