package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.audio.AudioClip;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AudioSystemTest {
    @Test
    void loadsWavWithoutOpeningDeviceAndRejectsForeignOrClosedClips() throws IOException {
        AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
        var wave = new ByteArrayOutputStream();
        try (var pcm = new AudioInputStream(new ByteArrayInputStream(new byte[1600]), format, 800)) {
            javax.sound.sampled.AudioSystem.write(pcm, AudioFileFormat.Type.WAVE, wave);
        }

        var input = new ByteArrayInputStream(wave.toByteArray()) {
            @Override public void close() {
                fail("Loading a clip must leave the caller's stream open");
            }
        };
        APIFactory factory = InternalFactoryStub.APIBuilder().OpenGL().JavaSound().create();
        assertTrue(factory.supportsAudio());
        try (var audio = factory.createAudioSystem(); var other = factory.createAudioSystem()) {
            AudioClip clip = audio.load(input);
            assertEquals(0.1, clip.durationSeconds(), 0.000001);
            assertThrows(IllegalArgumentException.class, () -> other.createSource(clip));
            clip.close();
            clip.close();
            assertThrows(IllegalArgumentException.class, () -> audio.createSource(clip));
        }
    }

    @Test
    void builderRequiresGraphicsAndLeavesAudioOptional() {
        assertThrows(IllegalStateException.class, () -> InternalFactoryStub.APIBuilder().JavaSound().create());
        assertThrows(IllegalStateException.class, () -> InternalFactoryStub.APIBuilder().OpenGL().Vulkan());
        assertThrows(IllegalStateException.class, () -> InternalFactoryStub.APIBuilder().JavaSound().JavaSound());
        assertThrows(UnsupportedOperationException.class, () -> InternalFactoryStub.APIBuilder().Vulkan().create());

        APIFactory factory = InternalFactoryStub.APIBuilder().OpenGL().create();
        assertFalse(factory.supportsAudio());
        assertThrows(UnsupportedOperationException.class, factory::createAudioSystem);
    }
}
