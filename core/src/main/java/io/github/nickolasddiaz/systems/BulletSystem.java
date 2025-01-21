package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.utils.CollisionObject;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.ArrayList;
import java.util.Objects;

import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class BulletSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<BulletComponent> bulletMapper;
    private final ChunkComponent chunk;
    private final Engine engine;

    public BulletSystem(Engine engine, ChunkComponent chunk) {
        super(Family.all(TransformComponent.class, BulletComponent.class).get());
        this.engine = engine;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.bulletMapper = ComponentMapper.getFor(BulletComponent.class);
        this.chunk = chunk;
    }


    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        BulletComponent bullet = bulletMapper.get(entity);

        Vector2 chunkPosition = new Vector2((int) Math.floor(transform.position.x / chunkSize), (int) Math.floor(transform.position.y / chunkSize));
        if (!chunk.mapChunks.containsKey(chunkPosition)) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Move bullet
        transform.position.add( (float) (bullet.bullet_speed * deltaTime * Math.cos(Math.toRadians(transform.rotation))),
                                (float) (bullet.bullet_speed * deltaTime * Math.sin(Math.toRadians(transform.rotation))));

        chunk.world.move(transform.item, transform.position.x, transform.position.y, CollisionFilter);
    }

    private final CollisionFilter CollisionFilter = new CollisionFilter() {
        @Override
        public Response filter(Item item, Item other) {
            if(other == null) {
                return null;
            }
            CollisionObject otherObject = (CollisionObject) other.userData;
            switch (otherObject.getObjectType()) {
                case "OCEAN":
                case "STRUCTURE":
                    if (Intersector.overlapConvexPolygons(otherObject.getPolygon(), ((CollisionObject) item.userData).getPolygon())) {
                        ((CollisionObject) item.userData).health = 0;
                    }
                    return Response.touch;
                case "CAR":
                    otherObject.health = 0;
                    ((CollisionObject) item.userData).health = 0;
                    return Response.cross;
                case "PLAYER":
                    if(!Objects.equals(((CollisionObject) item.userData).getObjectType(), "P_BULLET")){
                        otherObject.health -= ((CollisionObject) item.userData).health;
                        ((CollisionObject) item.userData).health = 0;
                    }
                    return Response.cross;
                case "ENEMY":
                    if(!Objects.equals(((CollisionObject) item.userData).getObjectType(), "E_BULLET")){
                        otherObject.health -= ((CollisionObject) item.userData).health;
                        ((CollisionObject) item.userData).health = 0;
                        }
                    return Response.cross;
            }
            return null;
        }
    };
}
