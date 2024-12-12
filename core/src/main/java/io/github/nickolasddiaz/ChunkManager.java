package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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

import static io.github.nickolasddiaz.MapGenerator.MAP_SIZE;
import static io.github.nickolasddiaz.MapGenerator.TILE_SIZE;

public class ChunkManager {
    private final OrthographicCamera camera;
    private final MapGenerator mapGenerator;
    private final HashMap<Vector2, TiledMap> mapChunks = new HashMap<>();

    private final float chunkSize; // MAP_SIZE * TILE_SIZE * TILE_SIZE = 80 * 8 * 8 = 5120
    private static final int CHUNK_LOAD_RADIUS = 1;

    private final OrthogonalTiledMapRenderer chunkRenderer;
    private final Matrix4 tempMatrix = new Matrix4();
    private final Rectangle cameraBounds;
    private final ShapeRenderer shapeRenderer;

    private final Vector2 currentChunk = new Vector2(0, 0);

    public ChunkManager(yourgame game) {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() * TILE_SIZE, Gdx.graphics.getHeight() * TILE_SIZE);
        cameraBounds = new Rectangle();

        mapGenerator = new MapGenerator((int)System.currentTimeMillis());
        chunkSize = MAP_SIZE * TILE_SIZE * TILE_SIZE;

        // Initialize with surrounding chunks
        loadInitialChunks();

        chunkRenderer = new OrthogonalTiledMapRenderer(null, TILE_SIZE);
        shapeRenderer = new ShapeRenderer();
    }

    public void updateCamera(float cameraX, float cameraY) {
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
        HashMap<Vector2, TiledMap> newChunks = new HashMap<>();
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

    private void loadInitialChunks() {
        for (int x = -CHUNK_LOAD_RADIUS; x <= CHUNK_LOAD_RADIUS; x++) {
            for (int y = -CHUNK_LOAD_RADIUS; y <= CHUNK_LOAD_RADIUS; y++) {
                Vector2 chunkPos = new Vector2(x, y);
                mapChunks.put(chunkPos, mapGenerator.generateMap(x * MAP_SIZE, y * MAP_SIZE));
            }
        }
    }

    public void renderChunks() {
        // Update chunkRenderer with the current camera matrix.
        chunkRenderer.setView(camera);
        float cameraHeight = camera.viewportHeight / 2;
        float cameraWidth = camera.viewportWidth / 2;

        for (Map.Entry<Vector2, TiledMap> entry : mapChunks.entrySet()) {
            Vector2 chunkPos = entry.getKey();
            TiledMap chunk = entry.getValue();

            float offsetX = chunkPos.x * chunkSize;
            float offsetY = chunkPos.y * chunkSize;

            if (!isChunkVisible(offsetX, offsetY)) continue; // Culling

            chunkRenderer.setMap(chunk);
            tempMatrix.set(camera.combined);
            tempMatrix.translate(offsetX, offsetY, 0);

            float left = Math.max(0, camera.position.x - cameraWidth - offsetX);
            float bottom = Math.max(0, camera.position.y - cameraHeight - offsetY);
            float right = Math.min(chunkSize, camera.position.x + cameraWidth - offsetX);
            float top = Math.min(chunkSize, camera.position.y + cameraHeight - offsetY);

            if (right > left && top > bottom) {
                chunkRenderer.setView(tempMatrix, left, bottom, right, top);
                chunkRenderer.render();
            }
        }
    }

    public void debugRenderChunkBoundaries(yourgame game) {
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

    private boolean isChunkVisible(float offsetX, float offsetY) {
        return cameraBounds.overlaps(new Rectangle(offsetX, offsetY, chunkSize, chunkSize));
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void dispose() {
        shapeRenderer.dispose();
        for (TiledMap map : mapChunks.values()) {
            map.dispose();
        }
    }
}
