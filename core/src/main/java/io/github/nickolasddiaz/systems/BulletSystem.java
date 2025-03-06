package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.TransformComponent;


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

        // Check if bullet still exists
        if (transform.body == null) {
            transform.health = 0;
            return;
        }

        // Check if bullet is out of bounds
        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.getPosition()))) {
            transform.health = 0;
            return;
        }

        // Apply velocity to bullet
        float velocityX = (float) (bullet.bullet_speed * Math.cos(Math.toRadians(transform.rotation)));
        float velocityY = (float) (bullet.bullet_speed * Math.sin(Math.toRadians(transform.rotation)));
        transform.velocity.set(velocityX, velocityY);
    }
}

