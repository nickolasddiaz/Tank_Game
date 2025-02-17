package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;

public class MissileComponent implements Component {
    public float missile_speed;
    public float trackingTimer = 0f;
    public static final float TRACKING_INTERVAL = 0.2f;
    public TransformComponent targetPosition = null;
    public final ChunkComponent chunk;
    public short searchBits;

    public MissileComponent(float speed, ChunkComponent chunk, boolean team) {
        this.missile_speed = speed;
        this.chunk = chunk;
        this.searchBits = team ? ENEMY : PLAYER|ALLY;
    }
}
