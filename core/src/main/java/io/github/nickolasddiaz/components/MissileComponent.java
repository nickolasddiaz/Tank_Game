package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;

public class MissileComponent implements Component {
    public float missile_speed;
    public float trackingTimer = 0f;
    public static final float TRACKING_INTERVAL = 0.2f;
    public TransformComponent targetPosition = null;
    public final ChunkComponent chunk;

    public MissileComponent(float speed, ChunkComponent chunk) {
        this.missile_speed = speed;
        this.chunk = chunk;
    }
}
