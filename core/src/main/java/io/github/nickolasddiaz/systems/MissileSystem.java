package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Response;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.CollisionObject;

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
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        MissileComponent missile = missileMapper.get(entity);

        // Track position temporarily
        transform.tempPosition.set(transform.position);

        // Check if in a valid chunk
        Vector2 chunkPosition = new Vector2(
            (int) Math.floor(transform.position.x / chunkSize),
            (int) Math.floor(transform.position.y / chunkSize)
        );
        if (!chunk.mapChunks.containsKey(chunkPosition)) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Update tracking timer
        missile.trackingTimer += deltaTime;
        if (missile.trackingTimer >= MissileComponent.TRACKING_INTERVAL) {
            missile.findTarget(transform.position, transform.rotation);
            missile.trackingTimer = 0f;
        }

        // Missile movement with optional tracking
        if (missile.targetPosition != null) {
            // Calculate angle to target
            Vector2 directionToTarget = missile.targetPosition.getPosition().cpy().sub(transform.position).nor();
            float targetAngle = directionToTarget.angleDeg();

            // Smoothly rotate towards target
            float rotationSpeed = 180f; // Degrees per second
            float angleDifference = targetAngle - transform.rotation;

            // Normalize angle difference
            if (angleDifference > 180f) angleDifference -= 360f;
            if (angleDifference < -180f) angleDifference += 360f;

            // Rotate
            if (Math.abs(angleDifference) < rotationSpeed * deltaTime) {
                transform.rotation = targetAngle;
            } else {
                transform.rotation += Math.signum(angleDifference) * rotationSpeed * deltaTime;
            }
        }

        // Move missile
        transform.position.add(
            (float) (missile.missile_speed * deltaTime * Math.cos(Math.toRadians(transform.rotation))),
            (float) (missile.missile_speed * deltaTime * Math.sin(Math.toRadians(transform.rotation)))
        );

        // Update world collision
        if (chunk.world.getRect(transform.item) == null) {
            Rectangle rect = transform.item.userData.getBounds();
            chunk.world.add(transform.item, transform.position.x, transform.position.y, rect.width, rect.height);
        }
        Response.Result result = chunk.world.move(transform.item, transform.position.x, transform.position.y, CollisionFilter);
        transform.position.set(result.goalX, result.goalY);
        if(transform.item.userData.health <= 0) {
            transform.position.set(transform.tempPosition);
        }
    }

    private final CollisionFilter CollisionFilter = (item, other) -> {
        if(other == null) {
            return null;
        }
        CollisionObject otherObject = (CollisionObject) other.userData;
        switch (otherObject.getObjectType()) {
            case "OCEAN":
            case "STRUCTURE":
                if (Intersector.overlapConvexPolygons(otherObject.getPolygon(), ((CollisionObject) item.userData).getPolygon())) {
                    ((CollisionObject) item.userData).health = 0;
                    return Response.touch;
                } break;
            case "ENEMY":
                otherObject.health -= ((CollisionObject) item.userData).health;
                ((CollisionObject) item.userData).health = 0;
                if(otherObject.health <= 0){
                    addScore();
                }
                return Response.touch;
        }
        return Response.cross;
    };

    private void addScore(){
        statsComponent.addScore(1);
    }
}
