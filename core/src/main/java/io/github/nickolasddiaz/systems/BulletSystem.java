package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import com.dongbat.jbump.World;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.utils.CollisionObject;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.ArrayList;

import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class BulletSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<ChunkComponent> chunkMapper;
    private final Engine engine;
    private static final float BULLET_SPEED = 500f;
    private static final int BULLET_SIZE = 8;

    public BulletSystem(Engine engine) {
        super(Family.all(TransformComponent.class, ChunkComponent.class).get());
        this.engine = engine;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
    }

    public Entity createBullet(Vector2 position, float rotation, World<CollisionObject> world, Sprite bulletSprite) {
        Entity bullet = new Entity();

        // Create transform component with bullet properties
        TransformComponent transform = new TransformComponent(
            bulletSprite,
            BULLET_SIZE,
            BULLET_SIZE,
            Color.YELLOW,
            true, // isPolygon for rotation
            "BULLET",
            world,
            position,
            rotation
        );

        // Set bullet specific properties
        transform.movement.set(BULLET_SPEED, 0).setAngleDeg(rotation); // Set initial velocity

        bullet.add(transform);
        engine.addEntity(bullet);

        return bullet;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        ChunkComponent chunk = chunkMapper.get(entity);

        Vector2 chunkPosition = new Vector2((int) Math.floor(transform.position.x / chunkSize), (int) Math.floor(transform.position.y / chunkSize));

        if(!chunk.mapChunks.containsKey(chunkPosition)) {
            engine.removeEntity(entity);
            return;
        }

        // Update lifetime and check for expiration
        if (transform.isDead) {
            removeBullet(entity, transform);
            return;
        }

        // Check for collisions
        ArrayList<Item> collisions = chunk.getObjectsIsInsideRect(
            createBulletCollisionFilter(),
            transform.item.userData.getBounds(),
            chunk.world
        );

        if (collisions != null) {
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

    private CollisionFilter createBulletCollisionFilter() {
        return (item, other) -> {
            CollisionObject obj = (CollisionObject) item.userData;
            String type = obj.getObjectType();
            return (type.equals("ENEMY") || type.equals("PLAYER") ||
                type.equals("STRUCTURE") || type.equals("CAR")) ?
                Response.cross : null;
        };
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
