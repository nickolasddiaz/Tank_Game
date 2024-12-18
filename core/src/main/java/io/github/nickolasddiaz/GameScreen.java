package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static io.github.nickolasddiaz.MapGenerator.TILE_SIZE;

public class GameScreen implements Screen {
    private final yourgame game;

    private final Texture tankTexture;
    private final Sprite tankSprite;
    private final Rectangle tankRectangle;
    private float tankWidth;
    private float tankHeight;

    public final float SPEED = 8000f;
    private float tankSpin = 0;
    private final float SPEED_OF_SPIN = SPEED/16;


    private Texture joyStickTexture;
    private Texture joyStickBaseTexture;
    private Circle joyStickBaseCircle;
    private Circle joyStickTouchCircle;
    private ShapeRenderer shapeRendererCircle;
    private ShapeRenderer shapeRendererTouchLocation;

    private Vector2 stickPosition;
    private Vector2 stickPositionMovement;

    private final ScreenViewport screenViewport;

    OrthographicCamera screenCamera;
    private static final float JOYSTICK_SCREEN_MARGIN_PERCENT = 0.1f;
    private static final float JOYSTICK_BASE_DIAMETER_PERCENT = 0.15f;
    private static final float JOYSTICK_TOUCH_DIAMETER_MULTIPLIER = 2.5f;

    private final Stage stage;
    private int score = 0;
    private final Label scoreLabel;
    private final StatsRenderer statsRenderer;
    private boolean paused = false;



    public GameScreen(final yourgame game) {
        this.game = game;

        tankTexture = new Texture("tank.png");
        tankSprite = new Sprite(tankTexture);
        tankSprite.setSize(TILE_SIZE * TILE_SIZE * 4, TILE_SIZE * TILE_SIZE * 4);

        screenCamera = new OrthographicCamera();
        screenViewport = new ScreenViewport(screenCamera);
        screenViewport.setWorldSize(1920, 1080);
        screenCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        tankRectangle = new Rectangle();

        initializeJoystick();
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        Skin skin = new Skin(Gdx.files.internal("ui_tank_game.json"));
        Button pauseButton = new Button(skin);
        pauseButton.setStyle(skin.get("pause", Button.ButtonStyle.class));
        pauseButton.setSize(Gdx.graphics.getWidth() / 10f, Gdx.graphics.getHeight() / 7f);
        float iconsHeight = Gdx.graphics.getHeight() - pauseButton.getHeight()*1.2f;

        pauseButton.setPosition(Gdx.graphics.getWidth() - pauseButton.getWidth()*1.2f, iconsHeight);
        stage.addActor(pauseButton);
        pauseButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                pause();
            }
        });

        scoreLabel = new Label("Score: " + score, skin);
        scoreLabel.setSize(Gdx.graphics.getWidth() / 10f, Gdx.graphics.getHeight() / 8f);
        scoreLabel.setPosition((float) Gdx.graphics.getWidth() /2 - scoreLabel.getWidth()/2, iconsHeight);
        stage.addActor(scoreLabel);

        statsRenderer = new StatsRenderer(Gdx.graphics.getWidth() / 25f, iconsHeight);

    }
    private void initializeJoystick() {
        if (!game.IS_MOBILE) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float joystickBaseDiameter = screenWidth * JOYSTICK_BASE_DIAMETER_PERCENT;
        float joystickBaseRadius = joystickBaseDiameter / 2;

        float marginX = screenWidth * JOYSTICK_SCREEN_MARGIN_PERCENT;
        float marginY = screenHeight * JOYSTICK_SCREEN_MARGIN_PERCENT;

        Vector2 joyStickBasePosition = new Vector2(
            marginX + joystickBaseRadius,
            marginY + joystickBaseRadius
        );

        joyStickTexture = new Texture("handle.png");
        joyStickBaseTexture = new Texture("handle_background.png");


        // Initialize shape renderers for joystick
        shapeRendererTouchLocation = new ShapeRenderer();
        shapeRendererCircle = new ShapeRenderer();

        // Create circles for joystick interaction
        joyStickBaseCircle = new Circle(joyStickBasePosition.x, joyStickBasePosition.y, joystickBaseRadius);

        // Create a larger touch area
        joyStickTouchCircle = new Circle(joyStickBasePosition.x, joyStickBasePosition.y, joystickBaseRadius * JOYSTICK_TOUCH_DIAMETER_MULTIPLIER);

        // Initial stick position
        stickPosition = new Vector2(joyStickBasePosition);
        stickPositionMovement = new Vector2(stickPosition);
    }


    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        input();
        logic();
        game.chunkManager.updateCamera(tankSprite.getX() + tankSprite.getWidth() / 2, tankSprite.getY() + tankSprite.getHeight() / 2);

        if(paused) return;
        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.viewport.apply();
        game.batch.setProjectionMatrix(game.chunkManager.getCamera().combined);
        game.chunkManager.renderChunks();
        game.batch.begin();
        tankSprite.draw(game.batch);
        statsRenderer.render(game.batch,screenCamera);
        game.batch.end();


        if(game.IS_MOBILE) {
            screenViewport.apply();
            game.batch.setProjectionMatrix(screenCamera.combined);
            game.batch.begin();
            renderJoystick();
            game.batch.end();

        }
        if(game.DEBUG) {
            game.chunkManager.debugRenderChunkBoundaries(game);
            if(game.IS_MOBILE) {
                shapeRendererTouchLocation.begin(ShapeRenderer.ShapeType.Line); // shapeRenderer cannot be within the game.batch.begin() and game.batch.end() block
                shapeRendererTouchLocation.setColor(Color.BLUE);
                shapeRendererTouchLocation.circle(joyStickTouchCircle.x, joyStickTouchCircle.y, joyStickTouchCircle.radius);
                shapeRendererTouchLocation.end();

                shapeRendererCircle.begin(ShapeRenderer.ShapeType.Line);
                shapeRendererCircle.setColor(Color.ORANGE);
                shapeRendererCircle.circle(joyStickBaseCircle.x, joyStickBaseCircle.y, joyStickBaseCircle.radius);
                shapeRendererCircle.end();
            }

        }
        stage.act(delta);
        stage.draw();

    }
    private void renderJoystick() {
        if (!game.IS_MOBILE) return;

        // Render joystick only if not touched or touch is within joystick area
        if(!Gdx.input.isTouched() || !joyStickTouchCircle.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
            stickPositionMovement.set(stickPosition);
        }

        // Draw joystick sprites/shapes for debugging
        game.batch.draw(joyStickBaseTexture, stickPosition.x - joyStickBaseCircle.radius, stickPosition.y - joyStickBaseCircle.radius, joyStickBaseCircle.radius*2, joyStickBaseCircle.radius * 2);
        game.batch.draw(joyStickTexture, stickPositionMovement.x - joyStickBaseCircle.radius/2, stickPositionMovement.y - joyStickBaseCircle.radius/2, joyStickBaseCircle.radius, joyStickBaseCircle.radius);
    }

    private void input() {
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) paused = !paused;
        if(paused) return;

        Vector2 direction = new Vector2(0, 0);
        float delta = Gdx.graphics.getDeltaTime();
        float speedMultiplier = 1f;

        if (game.IS_MOBILE && Gdx.input.isTouched()) {
            Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());

            if (joyStickTouchCircle.contains(touchPos)) {
                Vector2 center = new Vector2(joyStickBaseCircle.x, joyStickBaseCircle.y);

                if (joyStickBaseCircle.contains(touchPos)) {
                    // Touch is within the base circle
                    stickPositionMovement = touchPos.cpy();
                    speedMultiplier = touchPos.dst(center) / joyStickBaseCircle.radius;
                } else {
                    // Touch is outside base circle but within touch area
                    Vector2 offset = touchPos.cpy().sub(center);
                    offset.nor().scl(joyStickBaseCircle.radius);
                    stickPositionMovement = center.cpy().add(offset);
                }
                direction = stickPositionMovement.cpy().sub(center).nor();
                if (direction.len2() > 0) {
                    direction.nor();  // Normalize to maintain consistent speed at any angle
                    Vector2 movement = new Vector2(direction).scl(delta * SPEED * speedMultiplier);
                    float targetAngle = direction.angleDeg();
                    float angleDifference = ((targetAngle - tankSpin + 540) % 360) - 180;
                    // Gradually adjust rotation speed
                    if (Math.abs(angleDifference) > 5) {
                        float turnSpeed = Math.min(SPEED_OF_SPIN * delta, Math.abs(angleDifference) * 0.8f) * speedMultiplier;
                        tankSpin += Math.signum(angleDifference) * turnSpeed;
                    } else {
                        tankSpin = targetAngle;
                    }
                    tankSpin = (tankSpin + 360) % 360; // Ensure the angle remains within [0, 360)
                    tankSprite.setRotation(tankSpin + 90);

                    // Apply movement based on angle
                    float angleRad = (float) Math.toRadians(tankSpin);
                    tankSprite.setOrigin(tankSprite.getWidth() / 2, tankSprite.getHeight() / 2);
                    tankSprite.translate(MathUtils.cos(angleRad) * movement.len(), MathUtils.sin(angleRad) * movement.len());
                }
            }
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction.x += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction.x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direction.y += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direction.y -= 1f;

            if (direction.len2() > 0 && direction.y != 0) {
                direction.nor(); // Normalize to maintain consistent speed at any angle
                Vector2 movement = new Vector2(direction).scl(delta * SPEED);
                tankSpin += direction.x * SPEED_OF_SPIN * delta;
                tankSpin %= 360;
                tankSprite.setRotation(tankSpin+90);
                float angleRad = (float) Math.toRadians(tankSpin);
                tankSprite.setOrigin(tankSprite.getWidth() / 2, tankSprite.getHeight() / 2);
                tankSprite.translate(MathUtils.cos(angleRad) * movement.y, MathUtils.sin(angleRad) * movement.y);
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.M)){
            statsRenderer.addStarLevel(1);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.N)){
            statsRenderer.addHealthLevel(1);
        }
    }

    private void logic() {
        tankRectangle.set(tankSprite.getX(), tankSprite.getY(), tankWidth, tankHeight);
    }

    private void setScore(int score) {
        this.score = score;
        scoreLabel.setText("Score: " + score);
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
        screenViewport.update(width, height, true);
        screenCamera.setToOrtho(false, screenViewport.getWorldWidth(), screenViewport.getWorldHeight());
        if (game.IS_MOBILE) {
            initializeJoystick();
        }
    }

    @Override
    public void hide() {
        pause();
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public void dispose() {
        tankTexture.dispose();
        joyStickTexture.dispose();
        game.chunkManager.dispose();
        shapeRendererCircle.dispose();
        shapeRendererTouchLocation.dispose();
        stage.dispose();
        }
}
