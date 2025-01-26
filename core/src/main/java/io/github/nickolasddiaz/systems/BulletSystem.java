package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Response;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.StatsComponent;
import io.github.nickolasddiaz.utils.CollisionObject;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.Objects;

import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class BulletSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<BulletComponent> bulletMapper;
    private final ChunkComponent chunk;
    private StatsComponent statsComponent;
    private final Engine engine;

    public BulletSystem(Engine engine, ChunkComponent chunk, StatsComponent statsComponent) {
        super(Family.all(TransformComponent.class, BulletComponent.class).get());
        this.engine = engine;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.bulletMapper = ComponentMapper.getFor(BulletComponent.class);
        this.chunk = chunk;
        this.statsComponent = statsComponent;
    }


    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        BulletComponent bullet = bulletMapper.get(entity);

        transform.tempPosition.set(transform.position);

        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.position))) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Move bullet
        transform.position.add( (float) (bullet.bullet_speed * deltaTime * Math.cos(Math.toRadians(transform.rotation))),
                                (float) (bullet.bullet_speed * deltaTime * Math.sin(Math.toRadians(transform.rotation))));

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
            case "CAR":
                otherObject.health = 0;
                ((CollisionObject) item.userData).health = 0;
                if(Objects.equals(((CollisionObject) item.userData).getObjectType(), "P_BULLET"))
                    addScore();
                return Response.touch;
            case "ALLY":
            case "PLAYER":
                if (!Objects.equals(((CollisionObject) item.userData).getObjectType(), "P_BULLET")) {
                    if(otherObject.health > statsComponent.reduceDamage)
                        otherObject.health -= ((CollisionObject) item.userData).health - statsComponent.reduceDamage;
                    ((CollisionObject) item.userData).health = 0;
                    return Response.touch;
                }
                break;
            case "ENEMY":
                if (!Objects.equals(((CollisionObject) item.userData).getObjectType(), "E_BULLET")) {
                    otherObject.health -= ((CollisionObject) item.userData).health;
                    ((CollisionObject) item.userData).health = 0;
                    if(otherObject.health <= 0){
                        addScore();
                    }
                    return Response.touch;
                }
                break;

        }
        return Response.cross;
    };
    private void addScore(){
        statsComponent.addScore(1);
    }

}
