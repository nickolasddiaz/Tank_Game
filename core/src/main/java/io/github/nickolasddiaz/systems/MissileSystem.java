package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class MissileSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<MissileComponent> missileMapper;
    private final ChunkComponent chunk;
    private final StatsComponent statsComponent;
    private final Engine engine;

    public MissileSystem(Engine engine, ChunkComponent chunk, StatsComponent statsComponent) {
        super(Family.all(TransformComponent.class, MissileComponent.class).get());
        this.engine = engine;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.missileMapper = ComponentMapper.getFor(MissileComponent.class);
        this.chunk = chunk;
        this.statsComponent = statsComponent;

        // Set up collision handling
        chunk.world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                handleCollision(contact.getFixtureA(), contact.getFixtureB());
            }

            @Override
            public void endContact(Contact contact) {}

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void handleCollision(Fixture fixtureA, Fixture fixtureB) {
        // Get the user data from both fixtures
        Object userDataA = fixtureA.getBody().getUserData();
        Object userDataB = fixtureB.getBody().getUserData();

        // Handle missile collisions
        TransformComponent missile = null;
        TransformComponent target = null;

        if (userDataA instanceof TransformComponent && userDataB instanceof TransformComponent) {
            TransformComponent transformA = (TransformComponent) userDataA;
            TransformComponent transformB = (TransformComponent) userDataB;

            // Check if one is a missile and the other is a target
            if (isMissile(transformA.body) && isTarget(transformB.body)) {
                missile = transformA;
                target = transformB;
            } else if (isMissile(transformB.body) && isTarget(transformA.body)) {
                missile = transformB;
                target = transformA;
            }

            if (missile != null && target != null) {
                // Handle damage
                target.health -= 1; // Adjust damage as needed
                if (target.health <= 0) {
                    statsComponent.addScore(1);
                }
                // Mark missile for destruction
                missile.health = 0;
            }
        }
    }

    private boolean isMissile(Body body) {
        return (body.getFixtureList().get(0).getFilterData().categoryBits & ChunkComponent.MISSILE) != 0;
    }

    private boolean isTarget(Body body) {
        return (body.getFixtureList().get(0).getFilterData().categoryBits &
            (ChunkComponent.ENEMY | ChunkComponent.STRUCTURE | ChunkComponent.OCEAN)) != 0;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        MissileComponent missile = missileMapper.get(entity);

        // Check if in a valid chunk
        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.position))) {
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
        if (missile.targetPosition != null) {
            // Calculate angle to target
            TransformComponent target = missile.targetPosition;
            Vector2 targetPos = target.position;
            Vector2 directionToTarget = new Vector2(
                targetPos.x - transform.position.x,
                targetPos.y - transform.position.y
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
                transform.body.setTransform(transform.body.getPosition(), (float)Math.toRadians(targetAngle));
            } else {
                float newAngle = transform.rotation + Math.signum(angleDifference) * rotationSpeed * deltaTime;
                transform.body.setTransform(transform.body.getPosition(), (float)Math.toRadians(newAngle));
            }
        }

        // Apply missile velocity
        Vector2 direction = new Vector2(1, 0).rotateRad(transform.body.getAngle());
        transform.body.setLinearVelocity(direction.scl(missile.missile_speed));

        // Update transform from physics body
        transform.updateTransform();

        // Check for destruction
        if (transform.health <= 0) {
            transform.dispose();
            engine.removeEntity(entity);
        }
    }

    private void findTarget(MissileComponent missile, TransformComponent transform) {
        float searchAngle = 30f;
        float searchLength = chunkSize / 2f;
        Vector2 currentPosition = transform.position;
        float currentRotation = transform.rotation;

        // Create AABB for broad phase
        Rectangle aabb = new Rectangle();
        aabb.set(currentPosition.x - searchLength, currentPosition.y - searchLength, 2 * searchLength, 2 * searchLength);

        // Query the world for potential targets
        chunk.world.QueryAABB(fixture -> {
            if ((fixture.getFilterData().categoryBits & ChunkComponent.ENEMY) != 0) {
                Body body = fixture.getBody();
                TransformComponent potentialTarget = (TransformComponent)body.getUserData();

                if (potentialTarget != null) {
                    Vector2 directionToTarget = new Vector2(
                        potentialTarget.position.x - currentPosition.x,
                        potentialTarget.position.y - currentPosition.y
                    );
                    float angleToTarget = directionToTarget.angleDeg();
                    float distance = directionToTarget.len();

                    // Check if target is within search cone
                    float angleDifference = Math.abs(angleToTarget - currentRotation);
                    angleDifference = Math.min(angleDifference, 360 - angleDifference);

                    if (angleDifference <= searchAngle && distance <= searchLength) {
                        if (missile.targetPosition == null ||
                            distance < missile.targetPosition.position.dst(currentPosition)) {
                            missile.targetPosition = potentialTarget;
                        }
                    }
                }
            }
            return true;
        }, aabb.x, aabb.y, aabb.x + aabb.width, aabb.y + aabb.height);
    }
}
