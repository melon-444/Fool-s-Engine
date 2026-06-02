package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.util.LightType;
import org.joml.Vector3f;

//TODO Complete spot light data structure
public class Light extends Component{
    public Vector3f color;
    public Vector3f direction;
    public Vector3f position;
    public float thetaOfCutOff;
    public final LightType lightType;
    public boolean castsShadow = false;

    public Light(Vector3f color, Vector3f direction, Vector3f position, float thetaOfCutOff) {
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.thetaOfCutOff = thetaOfCutOff;
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
