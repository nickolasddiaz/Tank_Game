package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

public class PlayerMovementSystem extends IteratingSystem {
    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<JoystickComponent> joystickMapper;
    private ComponentMapper<SettingsComponent> settingsMapper;

    public PlayerMovementSystem() {
        super(Family.all(PlayerComponent.class, TransformComponent.class, JoystickComponent.class, SettingsComponent.class).get());
        playerMapper = ComponentMapper.getFor(PlayerComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        joystickMapper = ComponentMapper.getFor(JoystickComponent.class);
        settingsMapper = ComponentMapper.getFor(SettingsComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);
        JoystickComponent joystick = joystickMapper.get(entity);
        SettingsComponent settings = settingsMapper.get(entity);

        Vector2 direction = new Vector2(0, 0);
        float speedMultiplier = 1f;


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
                    transform.position.x += MathUtils.cos(angleRad) * movement.len();
                    transform.position.y += MathUtils.sin(angleRad) * movement.len();
                }
            }
        } else {
            // Desktop controls
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction.x += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction.x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direction.y += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direction.y -= 1f;

            if (direction.len2() > 0 && direction.y != 0) {
                direction.nor();
                Vector2 movement = new Vector2(direction).scl(deltaTime * player.SPEED);
                transform.rotation += direction.x * player.spinSpeed * deltaTime;
                transform.rotation = (transform.rotation + 360) % 360;


                float angleRad = (float) Math.toRadians(transform.rotation);
                transform.position.x += MathUtils.cos(angleRad) * movement.y;
                transform.position.y += MathUtils.sin(angleRad) * movement.y;
            }
        }
    }
}
