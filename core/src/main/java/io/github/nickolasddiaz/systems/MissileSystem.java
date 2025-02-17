package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class MissileSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<MissileComponent> missileMapper;
    private final ChunkComponent chunk;
    private final Engine engine;

    public MissileSystem(Engine engine, ChunkComponent chunk) {
        super(Family.all(TransformComponent.class, MissileComponent.class).get());
        this.engine = engine;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.missileMapper = ComponentMapper.getFor(MissileComponent.class);
        this.chunk = chunk;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        MissileComponent missile = missileMapper.get(entity);

        // Check if in a valid chunk
        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.getPosition())) || transform.body == null) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Update tracking timer
        missile.trackingTimer += deltaTime;
        if (missile.trackingTimer >= MissileComponent.TRACKING_INTERVAL) {
            findTarget(missile, transform);
            missile.trackingTimer = 0f;
        }

        // Missile movement with optional tracking
        if (missile.targetPosition != null && missile.targetPosition.body != null) {
            // Calculate angle to target
            TransformComponent target = missile.targetPosition;
            Vector2 targetPos = target.getPosition();
            Vector2 directionToTarget = new Vector2(
                targetPos.x - transform.getPosition().x,
                targetPos.y - transform.getPosition().y
            ).nor();
            float targetAngle = directionToTarget.angleDeg();

            // Smoothly rotate towards target
            float rotationSpeed = 180f; // Degrees per second
            float angleDifference = targetAngle - transform.rotation;

            // Normalize angle difference
            if (angleDifference > 180f) angleDifference -= 360f;
            if (angleDifference < -180f) angleDifference += 360f;

            // Apply rotation
            if (Math.abs(angleDifference) < rotationSpeed * deltaTime) {
                transform.rotation = targetAngle;
            } else {
                transform.rotation = transform.rotation + Math.signum(angleDifference) * rotationSpeed * deltaTime;
            }
        }

        // Apply missile velocity
        Vector2 direction = new Vector2(1, 0).rotateRad(transform.body.getAngle());
        transform.velocity.set(direction.scl(missile.missile_speed));
    }

    private void findTarget(MissileComponent missile, TransformComponent transform) {
        float searchAngle = 30f;
        float searchLength = chunkSize / 2f;
        Vector2 currentPosition = transform.getPosition();
        float currentRotation = transform.rotation;

        // Create AABB for broad phase
        Rectangle aabb = new Rectangle();
        aabb.set(currentPosition.x - searchLength, currentPosition.y - searchLength, 2 * searchLength, 2 * searchLength);

        // Query the world for potential targets
        chunk.world.QueryAABB(fixture -> {
            if (((fixture.getFilterData().categoryBits & missile.searchBits) != 0)) {
                Body body = fixture.getBody();
                TransformComponent potentialTarget = (TransformComponent)body.getUserData();

                if (potentialTarget != null) {
                    Vector2 directionToTarget = new Vector2(
                        potentialTarget.getPosition().x - currentPosition.x,
                        potentialTarget.getPosition().y - currentPosition.y
                    );
                    float angleToTarget = directionToTarget.angleDeg();
                    float distance = directionToTarget.len();

                    // Check if target is within search cone
                    float angleDifference = Math.abs(angleToTarget - currentRotation);
                    angleDifference = Math.min(angleDifference, 360 - angleDifference);

                    if (angleDifference <= searchAngle && distance <= searchLength) {
                        if (missile.targetPosition == null ||
                            distance < missile.targetPosition.getPosition().dst(currentPosition)) {
                            missile.targetPosition = potentialTarget;
                        }
                    }
                }
            }
            return true;
        }, aabb.x, aabb.y, aabb.x + aabb.width, aabb.y + aabb.height);
    }
}
