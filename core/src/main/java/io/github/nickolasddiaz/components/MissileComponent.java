package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.Item;
import io.github.nickolasddiaz.utils.CollisionObject;

import java.util.ArrayList;

import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class MissileComponent implements Component {
    public float missile_speed;
    public float trackingTimer = 0f;
    public static final float TRACKING_INTERVAL = 0.2f;
    public CollisionObject targetPosition = null;
    public final ChunkComponent chunk;

    public MissileComponent(float speed, ChunkComponent chunk) {
        this.missile_speed = speed;
        this.chunk = chunk;
    }

    public void findTarget(Vector2 currentPosition, float currentRotation) {
        float searchAngle = 30f;
        float searchLength = chunkSize / 2f;

        ArrayList<Item> items = new ArrayList<>();
        chunk.world.queryRect(
            currentPosition.x - searchLength,
            currentPosition.y - searchLength,
            chunkSize,
            chunkSize,
            chunk.enemyFilter,
            items
        );

        float closestDistance = Float.MAX_VALUE;
        for (Item item : items) {
            if (item.userData == null) continue;

            CollisionObject collisionObject = (CollisionObject) item.userData;
            Vector2 targetPos = collisionObject.getPosition();

            // Calculate angle and distance to target
            Vector2 directionToTarget = targetPos.cpy().sub(currentPosition);
            float angleToTarget = directionToTarget.angleDeg();
            float distance = directionToTarget.len();

            // Check if target is within search cone and distance
            float angleDifference = Math.abs(angleToTarget - currentRotation);
            angleDifference = Math.min(angleDifference, 360 - angleDifference);

            if (angleDifference <= searchAngle && distance <= searchLength) {
                if (distance < closestDistance) {
                    closestDistance = distance;
                    targetPosition = collisionObject;
                }
            }
        }
    }
}
