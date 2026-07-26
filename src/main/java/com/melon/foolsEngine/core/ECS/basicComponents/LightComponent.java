// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.util.LightType;
import org.joml.Vector3f;

public class LightComponent extends Component{
    public Vector3f color;
    public Vector3f direction;
    public Vector3f position;
    public float innerTheta;
    public float outerTheta;
    public final LightType lightType;
    public boolean castsShadow = false;
    public float shadowNear = 0.1f;
    public float intensity = 1.0f;

    public LightComponent(Vector3f color, Vector3f direction, Vector3f position, float innerTheta, float outerTheta) {
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.innerTheta = innerTheta;
        this.outerTheta = outerTheta;
        this.lightType = LightType.SPOT;
    }

    public LightComponent(Vector3f color, Vector3f direction, Vector3f position) {
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.lightType = LightType.POINT;
    }

    public LightComponent(Vector3f color, Vector3f direction) {
        this.color = color;
        this.direction = direction;
        this.lightType = LightType.PARALLEL;
    }
}
