package com.melon.foolsEngine.api.audio;

/** A reusable, fully decoded sound owned by an {@link AudioSystem}. */
public interface AudioClip extends AutoCloseable {
    double durationSeconds();

    /** Releases this sound and closes any sources still using it. Safe to call twice. */
    @Override
    void close();
}
