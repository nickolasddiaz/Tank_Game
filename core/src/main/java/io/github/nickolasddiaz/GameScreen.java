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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static io.github.nickolasddiaz.MainMenuScreen.IS_MOBILE;
import static io.github.nickolasddiaz.MapGenerator.TILE_SIZE;

public class GameScreen implements Screen {
    private final yourgame game;
    private final ChunkManager chunkManager;

    Texture tankTexture;
    Sprite tankSprite;
    Rectangle tankRectangle;
    float tankWidth;
    float tankHeight;


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
    public static final float SPEED = 8000f;
    private static final float JOYSTICK_SCREEN_MARGIN_PERCENT = 0.1f;
    private static final float JOYSTICK_BASE_DIAMETER_PERCENT = 0.15f;
    private static final float JOYSTICK_TOUCH_DIAMETER_MULTIPLIER = 2f;

    public GameScreen(final yourgame game) {
        this.game = game;
        chunkManager = new ChunkManager(game);

        // Initialize tank
        tankTexture = new Texture("tank.png");
        tankSprite = new Sprite(tankTexture);
        tankSprite.setSize(TILE_SIZE * TILE_SIZE * 4, TILE_SIZE * TILE_SIZE * 4);

        // Initialize screen viewport for UI elements
        screenCamera = new OrthographicCamera();
        screenViewport = new ScreenViewport(screenCamera);
        screenViewport.setWorldSize(1920, 1080);
        screenCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        tankRectangle = new Rectangle();

        // Initialize joystick
        initializeJoystick();
    }
    private void initializeJoystick() {
        if (!IS_MOBILE) return;

        // Calculate joystick dimensions based on screen size
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Calculate joystick base size (diameter)
        float joystickBaseDiameter = screenWidth * JOYSTICK_BASE_DIAMETER_PERCENT;
        float joystickBaseRadius = joystickBaseDiameter / 2;

        // Calculate joystick position with margin
        float marginX = screenWidth * JOYSTICK_SCREEN_MARGIN_PERCENT;
        float marginY = screenHeight * JOYSTICK_SCREEN_MARGIN_PERCENT;

        // Position joystick at bottom-left with margin
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
        chunkManager.updateCamera(tankSprite.getX() + tankSprite.getWidth() / 2, tankSprite.getY() + tankSprite.getHeight() / 2);

        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(chunkManager.getCamera().combined);

        chunkManager.renderChunks();

        game.batch.begin();

        tankSprite.draw(game.batch);

        game.batch.end();

        if(IS_MOBILE) {
            screenViewport.apply();
            game.batch.setProjectionMatrix(screenCamera.combined);
            game.batch.begin();
            renderJoystick();
            game.batch.end();
            shapeRendererTouchLocation.begin(ShapeRenderer.ShapeType.Line); // shapeRenderer cannot be within the game.batch.begin() and game.batch.end() block
            shapeRendererTouchLocation.setColor(Color.BLUE);
            shapeRendererTouchLocation.circle(joyStickTouchCircle.x, joyStickTouchCircle.y, joyStickTouchCircle.radius);
            shapeRendererTouchLocation.end();

            shapeRendererCircle.begin(ShapeRenderer.ShapeType.Line);
            shapeRendererCircle.setColor(Color.ORANGE);
            shapeRendererCircle.circle(joyStickBaseCircle.x, joyStickBaseCircle.y, joyStickBaseCircle.radius);
            shapeRendererCircle.end();
            chunkManager.debugRenderChunkBoundaries(game);
        }
    }
    private void renderJoystick() {
        if (!IS_MOBILE) return;

        // Render joystick only if not touched or touch is within joystick area
        if(!Gdx.input.isTouched() || !joyStickTouchCircle.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
            stickPositionMovement.set(stickPosition);
        }

        // Draw joystick sprites/shapes for debugging
        game.batch.draw(joyStickBaseTexture, stickPosition.x - joyStickBaseCircle.radius, stickPosition.y - joyStickBaseCircle.radius, joyStickBaseCircle.radius*2, joyStickBaseCircle.radius * 2);
        game.batch.draw(joyStickTexture, stickPositionMovement.x - joyStickBaseCircle.radius/2, stickPositionMovement.y - joyStickBaseCircle.radius/2, joyStickBaseCircle.radius, joyStickBaseCircle.radius);
    }

    private void input() {
        Vector2 direction = new Vector2(0, 0);
        float speedMultiplier = 1f;

        if (IS_MOBILE && Gdx.input.isTouched()) {
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
                    speedMultiplier = 1f; // Maximum speed when at circle's edge
                }

                direction = stickPositionMovement.cpy().sub(center).nor();
            }
        } else {
            // Keyboard input (unchanged)
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction.x += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction.x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direction.y += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direction.y -= 1f;
        }

        // Movement calculation (unchanged)
        float delta = Gdx.graphics.getDeltaTime();
        if (direction.len() > 0) {
            direction.nor();
            float adjustedSpeed = IS_MOBILE ? SPEED * speedMultiplier : SPEED;
            direction.scl(adjustedSpeed * delta);
            tankSprite.translate(direction.x, direction.y);
        }
    }

    private void logic() {
        tankRectangle.set(tankSprite.getX(), tankSprite.getY(), tankWidth, tankHeight);
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
        screenViewport.update(width, height, true);
        screenCamera.setToOrtho(false, screenViewport.getWorldWidth(), screenViewport.getWorldHeight());
        if (IS_MOBILE) {
            initializeJoystick();
        }
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        tankTexture.dispose();
        joyStickTexture.dispose();
        chunkManager.dispose();
        shapeRendererCircle.dispose();
        shapeRendererTouchLocation.dispose();
    }
}
