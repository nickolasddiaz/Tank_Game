package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class PlayerSystem extends IteratingSystem{
    private final ComponentMapper<PlayerComponent> playerMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<JoystickComponent> joystickMapper;
    private final ChunkComponent chunk;
    private final SettingsComponent settings;
    private TransformComponent lockedTarget;
    private float timeToReadjust = 0f;


    public PlayerSystem(SettingsComponent settings, ChunkComponent chunk) {
        super(Family.all(PlayerComponent.class, TransformComponent.class, JoystickComponent.class).get());
        playerMapper = ComponentMapper.getFor(PlayerComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        joystickMapper = ComponentMapper.getFor(JoystickComponent.class);
        this.settings = settings;
        this.chunk = chunk;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);
        JoystickComponent joystick = joystickMapper.get(entity);

        // Update health in stats
        player.stats.health = transform.health;
        // Handle movement and rotation
        handleMovement(player, transform, joystick, deltaTime);

        player.stats.emulate(deltaTime, transform.getPosition(), transform.turretRotation, transform.velocity,
            (settings.AUTO_FIRE || Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isTouched()));

        transform.health = player.stats.health;

        transform.velocity = player.stats.velocity;
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
        Vector2 movement = new Vector2(direction).scl(player.stats.speed);
        transform.rotation += direction.x * player.stats.spinSpeed * deltaTime;
        transform.rotation = (transform.rotation + 360) % 360;


        float angleRad = (float) Math.toRadians(transform.rotation);
        transform.velocity.x = MathUtils.cos(angleRad) * movement.y;
        transform.velocity.y = MathUtils.sin(angleRad) * movement.y;

    }

    private void handleDirectionalMovement(PlayerComponent player, TransformComponent transform,
                                           Vector2 direction, float speedMultiplier, float deltaTime) {
        Vector2 movement = new Vector2(direction).scl(player.stats.speed * speedMultiplier);
        float angleRad = (float) Math.toRadians(transform.rotation);
        transform.velocity.x = MathUtils.cos(angleRad) * movement.len();
        transform.velocity.y = MathUtils.sin(angleRad) * movement.len();

        float targetAngle = direction.angleDeg();
        float angleDifference = ((targetAngle - transform.rotation + 540) % 360) - 180;

        if (Math.abs(angleDifference) > 5) {
            float turnSpeed = Math.min(player.stats.spinSpeed * deltaTime,
                Math.abs(angleDifference) * 0.5f);
            transform.rotation += Math.signum(angleDifference) * turnSpeed;
        } else {
            transform.rotation = targetAngle;
        }
        transform.rotation = (transform.rotation + 360) % 360;
    }
}
