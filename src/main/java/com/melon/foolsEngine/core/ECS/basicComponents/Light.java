package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.util.LightType;
import org.joml.Vector3f;

//TODO Complete spot light data structure
public class Light extends Component{
    public Vector3f color;
    public Vector3f direction;
    public Vector3f position;
    public float innerTheta;
    public float outerTheta;
    public final LightType lightType;
    public boolean castsShadow = false;

    public Light(Vector3f color, Vector3f direction, Vector3f position, float innerTheta, float outerTheta) {
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.innerTheta = innerTheta;
        this.outerTheta = outerTheta;
        this.lightType = LightType.SPOT;
    }

    public Light(Vector3f color, Vector3f direction, Vector3f position) {
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.lightType = LightType.POINT;
    }

    public Light(Vector3f color, Vector3f direction) {
        this.color = color;
        this.direction = direction;
        this.lightType = LightType.PARALLEL;
    }
}
