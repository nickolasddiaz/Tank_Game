package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class PlayerSystem extends IteratingSystem {
    private final ComponentMapper<PlayerComponent> playerMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<JoystickComponent> joystickMapper;
    private final ChunkComponent chunk;
    private final SettingsComponent settings;
    private TransformComponent lockedTarget;
    private final StatsComponent statsComponent;
    private final BulletFactory bulletFactory;
    private float timeToReadjust = 0f;
    private float spawnTime = 0f;
    private final EnemyFactory enemyFactory;
    private final MissileFactory missileFactory;
    private final LandMineFactory landMineFactory;
    private float landMineSpawnTime = 0f;
    private float missileSpawnTime = 0f;

    public PlayerSystem(SettingsComponent settings, ChunkComponent chunk, BulletFactory bulletFactory,
                        StatsComponent statsComponent, EnemyFactory enemyFactory,
                        MissileFactory missileFactory, LandMineFactory landMineFactory) {
        super(Family.all(PlayerComponent.class, TransformComponent.class, JoystickComponent.class).get());
        playerMapper = ComponentMapper.getFor(PlayerComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        joystickMapper = ComponentMapper.getFor(JoystickComponent.class);
        this.settings = settings;
        this.chunk = chunk;
        this.bulletFactory = bulletFactory;
        this.statsComponent = statsComponent;
        this.enemyFactory = enemyFactory;
        this.missileFactory = missileFactory;
        this.landMineFactory = landMineFactory;

        // Set up collision handling
        ContactListener contact =  new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Gdx.app.log("player collision", "test");
                handleCollision(contact);
            }

            @Override
            public void endContact(Contact contact) {}

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        };
    }

    private void handleCollision(Contact contact) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();
        TransformComponent transformA = (TransformComponent) bodyA.getUserData();
        TransformComponent transformB = (TransformComponent) bodyB.getUserData();
        Gdx.app.log("player collision", "test");

        if (transformA == null || transformB == null) return;

        // Get collision categories
        short categoryB = contact.getFixtureB().getFilterData().categoryBits;

        // Handle player collisions
        handlePlayerCollision(transformA, transformB, categoryB);
    }

    private void handlePlayerCollision(TransformComponent player, TransformComponent other, short category) {
        Gdx.app.log("player collision", String.valueOf(category));
        switch (category) {
            case STRUCTURE:
                chunk.destroyStructure(other.getPosition());
                break;
            case CAR:
                other.health = 0;
                player.speedBoost *= 0.4f;
                statsComponent.addScore(1);
                break;
            case HORIZONTAL_ROAD:
            case VERTICAL_ROAD:
                Gdx.app.log("boost","test");
                player.speedBoost += 0.4f;
                break;
            case DECORATION:
                player.speedBoost *= 0.4f;
                break;
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);
        JoystickComponent joystick = joystickMapper.get(entity);

        // Update health in stats
        if (statsComponent.getHealth() != transform.health) {
            statsComponent.setHealthLevel(transform.health);
        }

        // Handle missile shooting
        if (player.CanShootMissile) {
            missileSpawnTime += deltaTime;
            if (missileSpawnTime > player.missileRate) {
                missileSpawnTime = 0f;
                missileFactory.spawnMissile(transform.getPosition().cpy(), transform.turretRotation,
                    player.bulletSpeed, player.calculateDamage() * 4,
                    player.bulletSize, null, true);
            }
        }

        // Handle mine placement
        if (player.CanShootMine) {
            landMineSpawnTime += deltaTime;
            if (landMineSpawnTime > player.mineRate) {
                landMineSpawnTime = 0f;
                landMineFactory.createLandMine(transform.getPosition().cpy(), player.calculateDamage() * 2, true);
            }
        }

        // Handle shooting
        player.timeSinceLastShot += deltaTime;
        timeToReadjust += deltaTime;
        if ((settings.AUTO_FIRE || Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isTouched())
            && player.timeSinceLastShot > player.fireRate) {
            player.spawnBullets(transform.getPosition().cpy(), transform.turretRotation, bulletFactory, Color.YELLOW);
            player.timeSinceLastShot = 0f;
        }

        // Handle ally spawning
        handleAllySpawning(player, transform, deltaTime);

        // Handle movement and rotation
        handleMovement(player, transform, joystick, deltaTime);
    }

    private void handleAllySpawning(PlayerComponent player, TransformComponent transform, float deltaTime) {
        spawnTime += deltaTime;
        if (spawnTime > player.allySpawnerRate) {
            spawnTime = 0f;
            int spawnLength = 2 * itemSize;

            Rectangle spawnArea = new Rectangle(
                transform.getPosition().x - spawnLength / 2f,
                transform.getPosition().y - spawnLength / 2f,
                spawnLength,
                spawnLength
            );

            // Get bodies in spawn area
            Body[] bodies = chunk.getBodiesInRect(spawnArea,
                (short)(PLAYER | STRUCTURE | OCEAN | ENEMY | ALLY));

            Vector2[] spawnPositions = new Vector2[]{
                new Vector2(spawnArea.x, spawnArea.y),
                new Vector2(spawnArea.x + spawnLength, spawnArea.y),
                new Vector2(spawnArea.x, spawnArea.y + spawnLength),
                new Vector2(spawnArea.x + spawnLength, spawnArea.y + spawnLength)
            };

            // Try to spawn at each position
            for (Vector2 pos : spawnPositions) {
                if (!isPositionOccupied(pos, bodies)) {
                    enemyFactory.createTank(pos, true);
                    break;
                }
            }
        }
    }

    private boolean isPositionOccupied(Vector2 position, Body[] bodies) {
        for (Body body : bodies) {
            if (body.getPosition().dst(position) < itemSize) {
                return true;
            }
        }
        return false;
    }

    private void handleMovement(PlayerComponent player, TransformComponent transform,
                                JoystickComponent joystick, float deltaTime) {
        if (!settings.AUTO_FIRE) {
            transform.turretRotation = (float) Math.toDegrees(Math.atan2(
                Gdx.graphics.getHeight() / 2f - Gdx.input.getY(),
                Gdx.input.getX() - Gdx.graphics.getWidth() / 2f));
        } else {
            handleAutoAim(transform);
        }

        Vector2 direction = new Vector2(0, 0);
        float speedMultiplier = 1f;
        if (settings.IS_MOBILE && Gdx.input.isTouched()) {
            handleMobileMovement(player, transform, joystick, speedMultiplier, deltaTime);
        } else {
            handleDesktopMovement(player, transform, direction, deltaTime);
        }
    }

    private void handleAutoAim(TransformComponent transform) {
        if ((lockedTarget == null || timeToReadjust > 1f)) {
            timeToReadjust = 0f;
            Rectangle searchArea = new Rectangle(
                transform.getPosition().x - chunkSize / 2f,
                transform.getPosition().y - chunkSize / 2f,
                chunkSize,
                chunkSize
            );

            Body[] enemies = chunk.getBodiesInRect(searchArea, ENEMY);

            if (enemies.length > 0) {
                float minDistance = Float.MAX_VALUE;
                for (Body enemyBody : enemies) {
                    TransformComponent enemyTransform = (TransformComponent) enemyBody.getUserData();
                    float distance = transform.getPosition().dst(enemyTransform.getPosition());
                    if (distance < minDistance) {
                        minDistance = distance;
                        lockedTarget = enemyTransform;
                    }
                }
            }
        } else {
            transform.turretRotation = (float) Math.toDegrees(Math.atan2(
                lockedTarget.getPosition().y - transform.getPosition().y,
                lockedTarget.getPosition().x - transform.getPosition().x));
        }
    }

    private void handleMobileMovement(PlayerComponent player, TransformComponent transform,
                                      JoystickComponent joystick, float speedMultiplier, float deltaTime) {

        Vector2 touchPos = new Vector2(Gdx.input.getX(),
            Gdx.graphics.getHeight() - Gdx.input.getY());

        if (joystick.joyStickTouchCircle.contains(touchPos)) {
            Vector2 center = new Vector2(joystick.joyStickBaseCircle.x,
                joystick.joyStickBaseCircle.y);

            if (joystick.joyStickBaseCircle.contains(touchPos)) {
                joystick.stickPositionMovement = touchPos.cpy();
                speedMultiplier = touchPos.dst(center) / joystick.joyStickBaseCircle.radius;
            } else {
                Vector2 offset = touchPos.cpy().sub(center);
                offset.nor().scl(joystick.joyStickBaseCircle.radius);
                joystick.stickPositionMovement = center.cpy().add(offset);
            }

            Vector2 direction = joystick.stickPositionMovement.cpy().sub(center).nor();
            handleDirectionalMovement(player, transform, direction, speedMultiplier, deltaTime);

        }

    }

    private void handleDesktopMovement(PlayerComponent player, TransformComponent transform,
                                       Vector2 direction, float deltaTime) {
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction.x -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction.x += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direction.y += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direction.y -= 1f;

        direction.nor();
        Vector2 movement = new Vector2(direction).scl(player.SPEED);
        transform.rotation += direction.x * player.spinSpeed * deltaTime;
        transform.rotation = (transform.rotation + 360) % 360;


        float angleRad = (float) Math.toRadians(transform.rotation);
        transform.velocity.x = MathUtils.cos(angleRad) * movement.y;
        transform.velocity.y = MathUtils.sin(angleRad) * movement.y;

    }

    private void handleDirectionalMovement(PlayerComponent player, TransformComponent transform,
                                           Vector2 direction, float speedMultiplier, float deltaTime) {
        Vector2 movement = new Vector2(direction).scl(player.SPEED * speedMultiplier);
        float angleRad = (float) Math.toRadians(transform.rotation);
        transform.velocity.x = MathUtils.cos(angleRad) * movement.len();
        transform.velocity.y = MathUtils.sin(angleRad) * movement.len();

        float targetAngle = direction.angleDeg();
        float angleDifference = ((targetAngle - transform.rotation + 540) % 360) - 180;

        if (Math.abs(angleDifference) > 5) {
            float turnSpeed = Math.min(player.spinSpeed * deltaTime,
                Math.abs(angleDifference) * 0.5f);
            transform.rotation += Math.signum(angleDifference) * turnSpeed;
        } else {
            transform.rotation = targetAngle;
        }
        transform.rotation = (transform.rotation + 360) % 360;
    }
}
