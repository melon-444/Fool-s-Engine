package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.ui.UiDrawList;
import java.util.List;

/** UI quads sharing an optional anchor on the entity's world transform. */
public final class UiComponent extends Component {
    private List<UiDrawList.Quad> quads;
    public boolean visible = true;
    public boolean followTransform;
    public float offsetX;
    public float offsetY;
    public float offsetZ;

    public UiComponent(UiDrawList.Quad quad) {
        setQuad(quad);
    }

    public UiDrawList.Quad getQuad() { return quads.getFirst(); }

    public void setQuad(UiDrawList.Quad quad) {
        quads = List.of(quad);
    }

    public List<UiDrawList.Quad> getQuads() {
        return quads;
    }

    public void setQuads(List<UiDrawList.Quad> quads) {
        if (quads.isEmpty()) {
            throw new IllegalArgumentException("UI component requires at least one quad");
        }
        this.quads = List.copyOf(quads);
    }
}
