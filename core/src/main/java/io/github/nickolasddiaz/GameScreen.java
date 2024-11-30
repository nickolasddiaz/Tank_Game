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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.HashMap;
import java.util.Map;

import static io.github.nickolasddiaz.MapGenerator.MAP_SIZE;//100
import static io.github.nickolasddiaz.MapGenerator.TILE_SIZE;//8

import static java.lang.Math.signum;

public class GameScreen implements Screen {
    yourgame game;
    private final OrthographicCamera camera;
    MapGenerator mapGenerator;
    final private HashMap<Vector2, TiledMap> mapChunks = new HashMap<Vector2, TiledMap>();

    private final float chunkSize; //100 * 8 * 8 = 6,400 // chunkSize = MAP_SIZE * TILE_SIZE * TILE_SIZE;

    Texture tankTexture;
    Sprite tankSprite;
    Vector2 touchPos;
    Rectangle tankRectangle;
    float tankWidth;
    float tankHeight;

    private final Vector2 currentChunk = new Vector2(0, 0);
    private static final int CHUNK_LOAD_RADIUS = 1;
    private final OrthogonalTiledMapRenderer chunkRenderer;
    private final Matrix4 tempMatrix = new Matrix4();
    private final Rectangle cameraBounds;
    private final ShapeRenderer shapeRenderer;

    public static final float SPEED = 8000f;


    public GameScreen(final yourgame game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.viewport.getWorldWidth() * TILE_SIZE * TILE_SIZE *6, game.viewport.getWorldHeight() * TILE_SIZE * TILE_SIZE *6);
        cameraBounds = new Rectangle();

        mapGenerator = new MapGenerator((int)System.currentTimeMillis());
        chunkSize = MAP_SIZE * TILE_SIZE * TILE_SIZE;

        // Initialize with surrounding chunks
        loadInitialChunks();

        chunkRenderer = new OrthogonalTiledMapRenderer(null, TILE_SIZE);
        tankTexture = new Texture("tank.png");


        tankSprite = new Sprite(tankTexture);
        tankSprite.setSize(TILE_SIZE * TILE_SIZE *4, TILE_SIZE * TILE_SIZE *4); // Set appropriate size for the tank
        touchPos = new Vector2();
        tankRectangle = new Rectangle();
        shapeRenderer = new ShapeRenderer();

    }

    private void updateCamera() {
        float cameraX = tankSprite.getX() + tankSprite.getWidth() / 2;
        float cameraY = tankSprite.getY() + tankSprite.getHeight() / 2;

        int chunkX = (int) Math.floor(cameraX / chunkSize);
        int chunkY = (int) Math.floor(cameraY / chunkSize);
        Vector2 newChunk = new Vector2(chunkX, chunkY);

        if (!newChunk.equals(currentChunk)) {
            updateLoadedChunks(newChunk);
            currentChunk.set(newChunk);
        }

        camera.position.set(cameraX, cameraY, 0);
        camera.update();

        cameraBounds.set(
            camera.position.x - camera.viewportWidth / 2,
            camera.position.y - camera.viewportHeight / 2,
            camera.viewportWidth,
            camera.viewportHeight
        );
    }

    private void updateLoadedChunks(Vector2 centerChunk) {
        HashMap<Vector2, TiledMap> newChunks = new HashMap<Vector2, TiledMap>();
        for (int x = (int)centerChunk.x - CHUNK_LOAD_RADIUS; x <= centerChunk.x + CHUNK_LOAD_RADIUS; x++) {
            for (int y = (int)centerChunk.y - CHUNK_LOAD_RADIUS; y <= centerChunk.y + CHUNK_LOAD_RADIUS; y++) {
                Vector2 chunkPos = new Vector2(x, y);
                if (!mapChunks.containsKey(chunkPos)) {
                    TiledMap newChunk = mapGenerator.generateMap(x * MAP_SIZE, y * MAP_SIZE);
                    newChunks.put(chunkPos, newChunk);
                } else {
                    newChunks.put(chunkPos, mapChunks.get(chunkPos));
                }
            }
        }

        for (Vector2 key : mapChunks.keySet()) {
            if (!newChunks.containsKey(key)) {
                mapChunks.get(key).dispose();
            }
        }

        mapChunks.clear();
        mapChunks.putAll(newChunks);
    }

    private void loadInitialChunks(){
        for (int x = -CHUNK_LOAD_RADIUS; x <= CHUNK_LOAD_RADIUS; x++) {
            for (int y = -CHUNK_LOAD_RADIUS; y <= CHUNK_LOAD_RADIUS; y++) {
                Vector2 chunkPos = new Vector2(x, y);
                mapChunks.put(chunkPos, mapGenerator.generateMap(x * MAP_SIZE, y * MAP_SIZE));
            }
        }
    }

    @Override
    public void show() {
    }


    @Override
    public void render(float delta) {
        input();
        logic();
        updateCamera();
        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        renderChunks();

        game.batch.begin();
        tankSprite.draw(game.batch);
        game.batch.end();

        // Debug: Draw chunk boundaries
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        for (Vector2 chunkPos : mapChunks.keySet()) {
            float x = chunkPos.x * chunkSize;
            float y = chunkPos.y * chunkSize;
            shapeRenderer.rect(x, y, chunkSize, chunkSize);
        }
        shapeRenderer.end();

    }

    private void renderChunks() {
        // Update chunkRenderer with the current camera matrix.
        chunkRenderer.setView(camera);

        for (Map.Entry<Vector2, TiledMap> entry : mapChunks.entrySet()) {
            Vector2 chunkPos = entry.getKey();
            TiledMap chunk = entry.getValue();

            float offsetX = chunkPos.x * chunkSize;
            float offsetY = chunkPos.y * chunkSize;

            // Check if the chunk is within the camera's view (culling)
            if (isChunkVisible(offsetX, offsetY)) {
                chunkRenderer.setMap(chunk);
                tempMatrix.set(camera.combined);
                tempMatrix.translate(offsetX, offsetY, 0);

                // Set the matrix for the chunk and render it
                chunkRenderer.setView(tempMatrix, 0, 0, (int) chunkSize, (int) chunkSize);

                // Render the chunk
                chunkRenderer.render();
            }
        }
    }

    private boolean isChunkVisible(float offsetX, float offsetY) {
        return cameraBounds.overlaps(new Rectangle(offsetX, offsetY, chunkSize, chunkSize));
    }




    private void input() {
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            tankSprite.translateX(SPEED * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            tankSprite.translateX(-SPEED * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            tankSprite.translateY(SPEED * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            tankSprite.translateY(-SPEED * delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            game.viewport.unproject(touchPos);
            tankSprite.translateX(signum(touchPos.x) * SPEED * delta);
            tankSprite.translateY(signum(touchPos.y) * SPEED * delta);
        }
    }

    private void logic() {
        float delta = Gdx.graphics.getDeltaTime();
        tankRectangle.set(tankSprite.getX(), tankSprite.getY(), tankWidth, tankHeight);

    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
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
        shapeRenderer.dispose();
        for (TiledMap map : mapChunks.values()) {
            map.dispose();
        }
    }
}
