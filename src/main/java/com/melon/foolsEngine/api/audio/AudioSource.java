package com.melon.foolsEngine.api.audio;

/** A playback instance with its own cursor and settings. */
public interface AudioSource extends AutoCloseable {
    /** Starts from the beginning, including when already playing. */
    void play();

    void pause();

    /** Continues a paused source. Has no effect on a stopped source. */
    void resume();

    /** Stops and resets the playback cursor. */
    void stop();

    boolean isPlaying();

    /** Sets looping for the next play or resume. */
    void setLooping(boolean looping);

    /** Sets linear gain from 0 (minimum) to 1 (unchanged), subject to device limits. */
    void setVolume(float volume);

    /** Stops playback and releases this source. Safe to call twice. */
    @Override
    void close();
}
