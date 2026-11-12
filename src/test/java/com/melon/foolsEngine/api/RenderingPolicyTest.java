package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.rendering.resource.*;
import com.melon.foolsEngine.api.rendering.shader.ShaderPass;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderingPolicyTest {
    @Test void capacityFailureDoesNotMutateLights() {
        LightEnvironment env = new LightEnvironment(1);
        Light light = Light.point(new Vector3f(1), new Vector3f(), 1);
        env.add(light);
        assertThrows(IllegalStateException.class, () -> env.add(Light.point(new Vector3f(1), new Vector3f(), 1)));
        assertEquals(1, env.size());
        env.remove(light);
        env.remove(light);
        assertEquals(0, env.size());
    }
    @Test void renderingPoliciesAreIndependent() {
        ShaderPass pass = ShaderPass.core().transparent().sort(ShaderPass.SortMode.BATCHED)
                .depthWrite(true).depthTest(false).build();
        assertEquals(ShaderPass.SortMode.BATCHED, pass.sortMode());
        assertTrue(pass.depthWrite());
        assertFalse(pass.depthTest());
        assertEquals(ShaderPass.BlendMode.ALPHA_BLEND, pass.blendMode());
        assertEquals(ShaderPass.SortMode.BATCHED, ShaderPass.core().additive().build().sortMode());
    }
}
