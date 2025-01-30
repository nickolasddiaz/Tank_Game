package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.StatsComponent;
import io.github.nickolasddiaz.components.TransformComponent;

public class BulletSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<BulletComponent> bulletMapper;
    private final ChunkComponent chunk;
    private final StatsComponent statsComponent;
    private final Engine engine;

    public BulletSystem(Engine engine, ChunkComponent chunk, StatsComponent statsComponent) {
        super(Family.all(TransformComponent.class, BulletComponent.class).get());
        this.engine = engine;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.bulletMapper = ComponentMapper.getFor(BulletComponent.class);
        this.chunk = chunk;
        this.statsComponent = statsComponent;

        // Set up collision handling
        chunk.world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                handleBulletCollision(contact);
            }

            @Override
            public void endContact(Contact contact) {}

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void handleBulletCollision(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();

        // Get collision categories
        short categoryA = fixtureA.getFilterData().categoryBits;
        short categoryB = fixtureB.getFilterData().categoryBits;

        // Check if either fixture is a bullet
        if (categoryA == ChunkComponent.BULLET || categoryB == ChunkComponent.BULLET) {
            TransformComponent bulletTransform = (categoryA == ChunkComponent.BULLET) ?
                (TransformComponent) bodyA.getUserData() :
                (TransformComponent) bodyB.getUserData();

            TransformComponent otherTransform = (categoryA == ChunkComponent.BULLET) ?
                (TransformComponent) bodyB.getUserData() :
                (TransformComponent) bodyA.getUserData();

            short otherCategory = (categoryA == ChunkComponent.BULLET) ? categoryB : categoryA;

            if (otherCategory != ChunkComponent.HORIZONTAL_ROAD && otherCategory != ChunkComponent.VERTICAL_ROAD) {
                handleBulletHit(bulletTransform, otherTransform, otherCategory);
            }
        }
    }

    private void handleBulletHit(TransformComponent bullet, TransformComponent other, short category) {
        boolean isPlayerBullet = (category & 1) != 0; // Check if bullet is from player

        switch (category) {
            case ChunkComponent.OCEAN:
            case ChunkComponent.STRUCTURE:
                bullet.health = 0;
                break;

            case ChunkComponent.CAR:
                other.health = 0;
                bullet.health = 0;
                if (!isPlayerBullet) {
                    statsComponent.addScore(1);
                }
                break;

            case ChunkComponent.ALLY:
            case ChunkComponent.PLAYER:
                if (isPlayerBullet) {
                    if (other.health > statsComponent.reduceDamage) {
                        other.health -= bullet.health - statsComponent.reduceDamage;
                    }
                    bullet.health = 0;
                }
                break;

            case ChunkComponent.ENEMY:
                if (!isPlayerBullet) {
                    other.health -= bullet.health;
                    bullet.health = 0;
                    if (other.health <= 0) {
                        statsComponent.addScore(1);
                    }
                }
                break;
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        BulletComponent bullet = bulletMapper.get(entity);

        // Store previous position
        transform.tempPosition.set(transform.position);

        // Check if bullet is out of bounds
        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.position))) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Apply velocity to bullet
        float velocityX = (float) (bullet.bullet_speed * Math.cos(Math.toRadians(transform.rotation)));
        float velocityY = (float) (bullet.bullet_speed * Math.sin(Math.toRadians(transform.rotation)));
        transform.body.setLinearVelocity(velocityX, velocityY);

        // Update position from physics body
        transform.updateTransform();

        // Check if bullet should be destroyed
        if (transform.health <= 0) {
            transform.dispose();
            engine.removeEntity(entity);
        }
    }
}
