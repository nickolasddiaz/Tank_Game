package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.Item;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.CollisionObject;

import java.util.ArrayList;

import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;

public class PlayerMovementSystem extends IteratingSystem {
    private final ComponentMapper<PlayerComponent> playerMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<JoystickComponent> joystickMapper;
    private final ChunkComponent chunk;
    private final SettingsComponent settings;
    private CollisionObject lockedTarget;

    public PlayerMovementSystem(SettingsComponent settings, ChunkComponent chunk) {
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

        Vector2 direction = new Vector2(0, 0);
        float speedMultiplier = 1f;

        if(!settings.AUTO_FIRE)
            // Reverse Y to account for the coordinate system
            transform.turretRotation = (float) Math.toDegrees(Math.atan2(Gdx.graphics.getHeight() / 2f - Gdx.input.getY(), Gdx.input.getX() - Gdx.graphics.getWidth() / 2f));
        else{
            if (lockedTarget == null && player.enemyCount > 0) {
                ArrayList<Item> enemies = chunk.getObjectsIsInsideRect(chunk.enemyFilter, new Rectangle(transform.position.x - chunkSize / 2f, transform.position.y - chunkSize / 2f, chunkSize, chunkSize), chunk.world);

                if (enemies != null && !enemies.isEmpty()) {
                    float minDistance = Float.MAX_VALUE;
                    for (Item enemy : enemies) {
                        CollisionObject enemyObject = (CollisionObject) enemy.userData;
                        float distance = transform.position.dst(enemyObject.getPosition());
                        if (distance < minDistance) {
                            minDistance = distance;
                            lockedTarget = enemyObject;
                        }
                    }
                }
            }
            else if(lockedTarget != null){
                Vector2 targetPosition = lockedTarget.getPosition();
                transform.turretRotation = (float) Math.toDegrees(Math.atan2(targetPosition.y - transform.position.y, targetPosition.x - transform.position.x));
            }

        }
        if (settings.IS_MOBILE && Gdx.input.isTouched()) {
            Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());

            if (joystick.joyStickTouchCircle.contains(touchPos)) {
                Vector2 center = new Vector2(joystick.joyStickBaseCircle.x, joystick.joyStickBaseCircle.y);

                if (joystick.joyStickBaseCircle.contains(touchPos)) {
                    joystick.stickPositionMovement = touchPos.cpy();
                    speedMultiplier = touchPos.dst(center) / joystick.joyStickBaseCircle.radius;
                } else {
                    Vector2 offset = touchPos.cpy().sub(center);
                    offset.nor().scl(joystick.joyStickBaseCircle.radius);
                    joystick.stickPositionMovement = center.cpy().add(offset);
                }

                direction = joystick.stickPositionMovement.cpy().sub(center).nor();
                if (direction.len2() > 0) {
                    Vector2 movement = new Vector2(direction).scl(deltaTime * player.SPEED * speedMultiplier);
                    float targetAngle = direction.angleDeg();
                    float angleDifference = ((targetAngle - transform.rotation + 540) % 360) - 180;

                    // Adjust rotation with gradual speed
                    if (Math.abs(angleDifference) > 5) {
                        float turnSpeed = Math.min(player.spinSpeed * deltaTime, Math.abs(angleDifference) * 0.5f);
                        transform.rotation += Math.signum(angleDifference) * turnSpeed;
                    } else {
                        transform.rotation = targetAngle;
                    }
                    transform.rotation = (transform.rotation + 360) % 360;

                    // Apply movement based on rotation
                    float angleRad = (float) Math.toRadians(transform.rotation);
                    transform.movement.x = MathUtils.cos(angleRad) * movement.len();
                    transform.movement.y = MathUtils.sin(angleRad) * movement.len();
                }
            }
        } else {
            // Desktop controls
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction.x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction.x += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direction.y += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direction.y -= 1f;

            if (direction.len2() > 0 && direction.y != 0) {
                direction.nor();
                Vector2 movement = new Vector2(direction).scl(deltaTime * player.SPEED);
                transform.rotation += direction.x * player.spinSpeed * deltaTime;
                transform.rotation = (transform.rotation + 360) % 360;


                float angleRad = (float) Math.toRadians(transform.rotation);
                transform.movement.x = MathUtils.cos(angleRad) * movement.y;
                transform.movement.y = MathUtils.sin(angleRad) * movement.y;
            }
        }
    }
}
