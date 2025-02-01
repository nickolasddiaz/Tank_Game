package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.StatsComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;


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
                Fixture fixtureA = contact.getFixtureA();
                Fixture fixtureB = contact.getFixtureB();

                // Early exit if either fixture is null
                if (fixtureA == null || fixtureB == null) return;

                Body bodyA = fixtureA.getBody();
                Body bodyB = fixtureB.getBody();

                // Early exit if either body is null
                if (bodyA == null || bodyB == null) return;

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
        boolean isABullet = isBullet(categoryA);
        boolean isBBullet = isBullet(categoryB);

        if (isABullet || isBBullet) {
            Object userDataA = bodyA.getUserData();
            Object userDataB = bodyB.getUserData();

            // Early exit if user data is missing
            if (userDataA == null || userDataB == null) return;

            TransformComponent bulletTransform = isABullet ?
                (TransformComponent) userDataA :
                (TransformComponent) userDataB;

            TransformComponent otherTransform = isABullet ?
                (TransformComponent) userDataB :
                (TransformComponent) userDataA;

            short otherCategory = isABullet ? categoryB : categoryA;

            // Don't process road collisions
            if (otherCategory != HORIZONTAL_ROAD &&
                otherCategory != VERTICAL_ROAD) {
                handleBulletHit(bulletTransform, otherTransform, otherCategory);
            }
        }
    }

    private boolean isBullet(short category) {
        return (category & (P_BULLET | E_BULLET)) != 0;
    }

    private void handleBulletHit(TransformComponent bullet, TransformComponent other, short category) {
        // Ensure the bullet still exists
        if (bullet.body == null || bullet.body.getFixtureList().size == 0) return;

        boolean isPlayerBullet = (bullet.body.getFixtureList().first().getFilterData().categoryBits == P_BULLET);

        switch (category) {
            case OCEAN:
            case STRUCTURE:
                break;

            case CAR:
                other.health = 0;
                if (!isPlayerBullet) {
                    statsComponent.addScore(1);
                }
                break;

            case ALLY:
            case PLAYER:
                if (!isPlayerBullet && other.health > statsComponent.reduceDamage) {
                    other.health -= bullet.health - statsComponent.reduceDamage;
                }
                break;

            case ENEMY:
                if (isPlayerBullet) {
                    other.health -= bullet.health;
                    if (other.health <= 0) {
                        statsComponent.addScore(1);
                    }
                }
                break;
        }
        // Mark bullet for destruction
        bullet.health = 0;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        BulletComponent bullet = bulletMapper.get(entity);

        // Check if bullet still exists
        if (transform.body == null) {
            engine.removeEntity(entity);
            return;
        }

        // Check if bullet is out of bounds
        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.getPosition()))) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Apply velocity to bullet
        float velocityX = (float) (bullet.bullet_speed * Math.cos(Math.toRadians(transform.rotation)));
        float velocityY = (float) (bullet.bullet_speed * Math.sin(Math.toRadians(transform.rotation)));
        transform.velocity.set(velocityX, velocityY);
    }
}

