package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.rendering.ui.UiDrawList;
import com.melon.foolsEngine.api.rendering.ui.UiInteraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiInteractionTest {
    @Test
    void onlyTheTopPresentedTargetReceivesAClick() {
        var list = new UiDrawList();
        var ui = new UiInteraction();
        var white = UiDrawList.Color.WHITE;
        list.hitRegion("model", new UiDrawList.Quad(0, 0, 30, 30, white, 0));
        list.blockPointer(new UiDrawList.Quad(0, 0, 20, 20, white, 1));
        list.hitRegion("button", new UiDrawList.Quad(5, 5, 10, 10, white, 2));
        list.beginPresentation();
        for (var quad : list.orderedCommands()) {
            list.presentHitRegion(quad, quad.x(), quad.y(), 40, 40);
        }
        ui.update(list, 2, 2, false, true);
        assertNull(ui.hit());
        ui.update(list, 6, 6, true, true);
        assertTrue(ui.isHovered("button"));
        assertTrue(ui.isPressed("button"));
        ui.update(list, 25, 25, false, true);
        assertTrue(ui.isHovered("model"));
        assertFalse(ui.isClicked("model"));
        assertFalse(ui.isClicked("button"));
        ui.update(list, 6, 6, true, true);
        ui.update(list, 6, 6, false, true);
        assertTrue(ui.isClicked("button"));
        ui.update(list, 6, 6, true, true);
        ui.update(list, 6, 6, true, false);
        ui.update(list, 6, 6, false, true);
        assertFalse(ui.isClicked("button"));
    }
}
