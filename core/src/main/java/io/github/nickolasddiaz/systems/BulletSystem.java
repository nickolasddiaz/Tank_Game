package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.Item;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.utils.CollisionObject;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.ArrayList;

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

        if(transform.isDead ||!chunk.mapChunks.containsKey(chunkPosition)) {
            transform.isDead = true;
            removeBullet(entity, transform);
            engine.removeEntity(entity);
            return;
        }
        bullet.time_alive += deltaTime;

        // Move bullet
        transform.position.add( (float) (bullet.bullet_speed * deltaTime * Math.cos(Math.toRadians(transform.rotation))),
                                (float) (bullet.bullet_speed * deltaTime * Math.sin(Math.toRadians(transform.rotation))));
        // Check for collisions
        ArrayList<Item> collisions = chunk.getObjectsIsInsideRect(
            chunk.bulletHitFilter,
            transform.item.userData.getBounds(),
            chunk.world
        );

        if (collisions != null && bullet.time_alive > 0.3f) {
            for (Item item : collisions) {
                CollisionObject colObj = (CollisionObject) item.userData;
                String objType = colObj.getObjectType();

                // Check collision with valid targets
                if (objType.equals("ENEMY") || objType.equals("PLAYER") ||
                    objType.equals("STRUCTURE") || objType.equals("CAR")) {

                    handleBulletCollision(entity, transform, colObj);
                    return;
                }
            }
        }
    }


    private void handleBulletCollision(Entity bullet, TransformComponent transform, CollisionObject hitObject) {
        // Handle bullet hit effects here (damage, particles, etc)

        // Mark bullet for removal
        transform.isDead = true;
        removeBullet(bullet, transform);
    }

    private void removeBullet(Entity bullet, TransformComponent transform) {
        // Clean up resources
        transform.dispose(); // This removes the item from the collision world
        engine.removeEntity(bullet);
    }
}
