package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

public class JoystickInputSystem extends EntitySystem {
    private final ComponentMapper<JoystickComponent> joystickMapper;
    private final ComponentMapper<CameraComponent> cameraMapper;
    private final ComponentMapper<SettingsComponent> settingsMapper;

    private final Sprite joyStickTexture;
    private final Sprite joyStickBaseTexture;
    private ShapeRenderer shapeRendererCircle;
    private ShapeRenderer shapeRendererTouchLocation;

    private Vector2 stickPosition;

    private JoystickComponent joystickComponent;
    private CameraComponent cameraComponent;
    private SettingsComponent settingsComponent;

    public JoystickInputSystem() {
        joystickMapper = ComponentMapper.getFor(JoystickComponent.class);
        cameraMapper = ComponentMapper.getFor(CameraComponent.class);
        settingsMapper = ComponentMapper.getFor(SettingsComponent.class);

        joyStickTexture = new Sprite(new Texture("handle.png"));
        joyStickBaseTexture = new Sprite(new Texture("handle_background.png"));

        // Initialize shape renderers for joystick
        shapeRendererTouchLocation = new ShapeRenderer();
        shapeRendererCircle = new ShapeRenderer();
    }


    @Override
    public void addedToEngine(Engine engine) {
        Entity player = engine.getEntitiesFor(Family.all(
            CameraComponent.class,
            JoystickComponent.class
        ).get()).first();

        cameraComponent = cameraMapper.get(player);
        joystickComponent = joystickMapper.get(player);
        settingsComponent = settingsMapper.get(player);


        float joystickBaseRadius = Gdx.graphics.getWidth() * .075f;
        float marginX = Gdx.graphics.getWidth() * 0.1f;
        float marginY = Gdx.graphics.getHeight() * 0.1f;

        Vector2 joyStickBasePosition = new Vector2(
            marginX + joystickBaseRadius,
            marginY + joystickBaseRadius
        );

        // Create circles for joystick interaction
        joystickComponent.joyStickBaseCircle = new Circle(joyStickBasePosition.x, joyStickBasePosition.y, joystickBaseRadius);

        // Create a larger touch area
        joystickComponent.joyStickTouchCircle = new Circle(joyStickBasePosition.x, joyStickBasePosition.y, joystickBaseRadius * 2.5f);

        // Initial stick position
        stickPosition = new Vector2(joyStickBasePosition);
        joystickComponent.stickPositionMovement = new Vector2(stickPosition);

        joyStickTexture.setSize(joystickComponent.joyStickBaseCircle.radius, joystickComponent.joyStickBaseCircle.radius);
        joyStickBaseTexture.setSize(joystickComponent.joyStickBaseCircle.radius * 2.5f,joystickComponent.joyStickBaseCircle.radius * 2.5f);

    }

    @Override
    public void update(float deltaTime) {

        if (!Gdx.input.isTouched() || joystickComponent.joyStickTouchCircle.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
            joystickComponent.stickPositionMovement.set(stickPosition);
        }

        cameraComponent.batch.setProjectionMatrix(cameraComponent.camera.projection);
        cameraComponent.batch.begin();

        joyStickTexture.setPosition(joystickComponent.stickPositionMovement.x - joystickComponent.joyStickBaseCircle.radius, joystickComponent.stickPositionMovement.y - joystickComponent.joyStickBaseCircle.radius);
        joyStickBaseTexture.setPosition(joystickComponent.joyStickBaseCircle.x - joystickComponent.joyStickBaseCircle.radius, joystickComponent.joyStickBaseCircle.y - joystickComponent.joyStickBaseCircle.radius);

        joyStickTexture.draw(cameraComponent.batch);
        joyStickBaseTexture.draw(cameraComponent.batch);

        cameraComponent.batch.end();
        cameraComponent.batch.setProjectionMatrix(cameraComponent.camera.combined);

        if(settingsComponent.DEBUG) {
            shapeRendererTouchLocation.begin(ShapeRenderer.ShapeType.Line); // shapeRenderer cannot be within the game.batch.begin() and game.batch.end() block
            shapeRendererTouchLocation.setColor(Color.BLUE);
            shapeRendererTouchLocation.circle(joystickComponent.joyStickTouchCircle.x, joystickComponent.joyStickTouchCircle.y, joystickComponent.joyStickTouchCircle.radius);
            shapeRendererTouchLocation.end();

            shapeRendererCircle.begin(ShapeRenderer.ShapeType.Line);
            shapeRendererCircle.setColor(Color.ORANGE);
            shapeRendererCircle.circle(joystickComponent.joyStickBaseCircle.x, joystickComponent.joyStickBaseCircle.y, joystickComponent.joyStickBaseCircle.radius);
            shapeRendererCircle.end();
        }
    }
}
