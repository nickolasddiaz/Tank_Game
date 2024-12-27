package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import io.github.nickolasddiaz.components.*;



public class JoystickInputSystem extends EntitySystem {
    private final ComponentMapper<JoystickComponent> joystickMapper;
    private final ComponentMapper<SettingsComponent> settingsMapper;

    private final Image joyStickImage;
    private final Image joyStickBaseImage;
    private final ShapeRenderer shapeRendererCircle;
    private final ShapeRenderer shapeRendererTouchLocation;

    private Vector2 stickPosition;

    private JoystickComponent joystickComponent;
    private SettingsComponent settingsComponent;

    private final Stage stage = new Stage();

    public JoystickInputSystem() {
        joystickMapper = ComponentMapper.getFor(JoystickComponent.class);
        settingsMapper = ComponentMapper.getFor(SettingsComponent.class);

        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ui_tank_game.atlas"));

        joyStickImage = new Image(atlas.findRegion("handle"));
        joyStickBaseImage = new Image(atlas.findRegion("handle_background"));


        // Initialize shape renderers for joystick
        shapeRendererTouchLocation = new ShapeRenderer();
        shapeRendererCircle = new ShapeRenderer();
    }


    @Override
    public void addedToEngine(Engine engine) {
        Entity player = engine.getEntitiesFor(Family.all(
            JoystickComponent.class,
            SettingsComponent.class
        ).get()).first();

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
        joystickComponent.joyStickTouchCircle = new Circle(joyStickBasePosition.x, joyStickBasePosition.y, joystickBaseRadius * 3f);

        // Initial stick position
        stickPosition = new Vector2(joyStickBasePosition);
        joystickComponent.stickPositionMovement = new Vector2(stickPosition);

        stage.addActor(joyStickImage);
        stage.addActor(joyStickBaseImage);

        joyStickImage.setSize  (joystickComponent.joyStickBaseCircle.radius, joystickComponent.joyStickBaseCircle.radius);
        joyStickBaseImage.setSize(joystickComponent.joyStickBaseCircle.radius * 3f,joystickComponent.joyStickBaseCircle.radius * 3f);

    }

    @Override
    public void update(float deltaTime) {

        if (Gdx.input.isTouched() && joystickComponent.joyStickTouchCircle.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();
            Vector2 touchPosition = new Vector2(touchX, touchY);

            // Calculate the new stick position within the joystick base circle
            Vector2 direction = touchPosition.sub(joystickComponent.joyStickBaseCircle.x, joystickComponent.joyStickBaseCircle.y);
            if (direction.len() > joystickComponent.joyStickBaseCircle.radius) {
                direction.setLength(joystickComponent.joyStickBaseCircle.radius);
            }
            stickPosition.set(joystickComponent.joyStickBaseCircle.x + direction.x, joystickComponent.joyStickBaseCircle.y + direction.y);
            joystickComponent.stickPositionMovement.set(stickPosition);
        }else {
            stickPosition.set(joystickComponent.joyStickBaseCircle.x, joystickComponent.joyStickBaseCircle.y);
            joystickComponent.stickPositionMovement.set(stickPosition);
        }

        joyStickImage.setPosition(joystickComponent.stickPositionMovement.x - joystickComponent.joyStickBaseCircle.radius/2, joystickComponent.stickPositionMovement.y - joystickComponent.joyStickBaseCircle.radius/2);
        joyStickBaseImage.setPosition(joystickComponent.joyStickBaseCircle.x - joystickComponent.joyStickBaseCircle.radius * 1.5f, joystickComponent.joyStickBaseCircle.y - joystickComponent.joyStickBaseCircle.radius * 1.5f);


        stage.act(deltaTime);
        stage.draw();

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
