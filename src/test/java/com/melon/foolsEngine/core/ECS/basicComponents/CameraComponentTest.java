package com.melon.foolsEngine.core.ECS.basicComponents;

import static org.junit.jupiter.api.Assertions.*;

import com.melon.foolsEngine.util.ProjectionType;
import org.junit.jupiter.api.Test;

class CameraComponentTest {

    @Test
    void perspectiveConstructorDefaults() {
        CameraComponent c = new CameraComponent(60, 0.01f);
        assertEquals(ProjectionType.PERSPECTIVE, c.projectionType);
        assertEquals(60, c.FOVy);
        assertEquals(0.01f, c.near);
        assertTrue(c.active);
        assertTrue(c.isMainCam);
    }

    @Test
    void perspectiveConstructorWithActive() {
        CameraComponent c = new CameraComponent(45, 0.1f, false);
        assertEquals(ProjectionType.PERSPECTIVE, c.projectionType);
        assertEquals(45, c.FOVy);
        assertEquals(0.1f, c.near);
        assertFalse(c.active);
    }

    @Test
    void orthographicConstructorDefaults() {
        CameraComponent c = new CameraComponent(0.1f, 100f, ProjectionType.ORTHOGRAPHIC, 10f);
        assertEquals(ProjectionType.ORTHOGRAPHIC, c.projectionType);
        assertEquals(0.1f, c.near);
        assertEquals(100f, c.far);
        assertEquals(10f, c.orthoSize);
        assertTrue(c.active);
    }

    @Test
    void orthographicConstructorWithActive() {
        CameraComponent c = new CameraComponent(0.5f, 50f, ProjectionType.ORTHOGRAPHIC, 5f, false);
        assertEquals(ProjectionType.ORTHOGRAPHIC, c.projectionType);
        assertEquals(0.5f, c.near);
        assertEquals(50f, c.far);
        assertEquals(5f, c.orthoSize);
        assertFalse(c.active);
    }

    @Test
    void perspectiveCameraHasZeroOrthoSize() {
        CameraComponent c = new CameraComponent(60, 0.01f);
        assertEquals(0f, c.orthoSize);
    }
}
