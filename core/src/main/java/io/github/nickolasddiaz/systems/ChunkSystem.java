package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.nickolasddiaz.systems.MapGenerator.*;
import static io.github.nickolasddiaz.systems.MapGenerator.MAP_SIZE;

public class ChunkSystem extends EntitySystem {
    private final ComponentMapper<ChunkComponent> chunkMapper;
    private final ComponentMapper<CameraComponent> cameraMapper;
    private final ComponentMapper<SpriteComponent> spriteMapper;
    private final ComponentMapper<SettingsComponent> settingsMapper;

    private final ShapeRenderer shapeRenderer;
    private final OrthogonalTiledMapRenderer chunkRenderer;
    private final MapGenerator mapGenerator;
    private final Matrix4 tempMatrix;
    private final int CHUNK_LOAD_RADIUS = 1;

    // Cache components to avoid repeated lookups
    private ChunkComponent chunkComponent;
    private CameraComponent cameraComponent;
    private SpriteComponent spriteComponent;
    private SettingsComponent settingsComponent;

    public ChunkSystem() {

        // Initialize mappers
        chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
        cameraMapper = ComponentMapper.getFor(CameraComponent.class);
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
        settingsMapper = ComponentMapper.getFor(SettingsComponent.class);

        // Initialize rendering tools
        shapeRenderer = new ShapeRenderer();
        chunkRenderer = new OrthogonalTiledMapRenderer(null, TILE_SIZE);
        mapGenerator = new MapGenerator((int)System.currentTimeMillis());
        tempMatrix = new Matrix4();
    }

    @Override
    public void addedToEngine(Engine engine) {
        Entity player = engine.getEntitiesFor(Family.all(
            PlayerComponent.class,
            ChunkComponent.class,
            CameraComponent.class,
            SpriteComponent.class,
            SettingsComponent.class
        ).get()).first();

        chunkComponent = chunkMapper.get(player);
        cameraComponent = cameraMapper.get(player);
        spriteComponent = spriteMapper.get(player);
        settingsComponent = settingsMapper.get(player);


        loadInitialChunks();
    }

    @Override
    public void update(float deltaTime) {
        updateCamera(spriteComponent.tankSprite.getX(), spriteComponent.tankSprite.getY());
        renderChunks();

        if (settingsComponent.DEBUG) {
            debugRenderChunkBoundaries();
        }
    }

    private void updateCamera(float cameraX, float cameraY) {
        int chunkX = (int) Math.floor(cameraX / chunkSize);
        int chunkY = (int) Math.floor(cameraY / chunkSize);
        Vector2 newChunk = new Vector2(chunkX, chunkY);

        if (!newChunk.equals(chunkComponent.currentChunk)) {
            updateLoadedChunks(newChunk);
            chunkComponent.currentChunk.set(newChunk);
        }

        cameraComponent.camera.position.set(cameraX, cameraY, 0);
        cameraComponent.camera.update();
        updateCameraBounds();
    }

    private void updateCameraBounds() {
        cameraComponent.cameraBounds.set(
            cameraComponent.camera.position.x - cameraComponent.camera.viewportWidth / 2,
            cameraComponent.camera.position.y - cameraComponent.camera.viewportHeight / 2,
            cameraComponent.camera.viewportWidth,
            cameraComponent.camera.viewportHeight
        );
    }

    private void updateLoadedChunks(Vector2 centerChunk) {
        HashMap<Vector2, TiledMap> newChunks = new HashMap<>();

        // Load new chunks
        for (int x = (int)centerChunk.x - CHUNK_LOAD_RADIUS; x <= centerChunk.x + CHUNK_LOAD_RADIUS; x++) {
            for (int y = (int)centerChunk.y - CHUNK_LOAD_RADIUS; y <= centerChunk.y + CHUNK_LOAD_RADIUS; y++) {
                Vector2 chunkPos = new Vector2(x, y);
                if (!chunkComponent.mapChunks.containsKey(chunkPos)) {
                    newChunks.put(chunkPos, mapGenerator.generateMap(x * MAP_SIZE, y * MAP_SIZE));
                } else {
                    newChunks.put(chunkPos, chunkComponent.mapChunks.get(chunkPos));
                }
            }
        }

        // Dispose old chunks
        for (TiledMap map : chunkComponent.mapChunks.values()) {
            if (!newChunks.containsValue(map)) {
                map.dispose();
            }
        }

        chunkComponent.mapChunks.clear();
        chunkComponent.mapChunks.putAll(newChunks);
    }

    private void loadInitialChunks() {
        for (int x = -CHUNK_LOAD_RADIUS; x <= CHUNK_LOAD_RADIUS; x++) {
            for (int y = -CHUNK_LOAD_RADIUS; y <= CHUNK_LOAD_RADIUS; y++) {
                Vector2 chunkPos = new Vector2(x, y);
                chunkComponent.mapChunks.put(chunkPos, mapGenerator.generateMap(x * MAP_SIZE, y * MAP_SIZE));
            }
        }
    }

    private void renderChunks() {
        chunkRenderer.setView(cameraComponent.camera);
        float cameraHeight = cameraComponent.camera.viewportHeight / 2;
        float cameraWidth = cameraComponent.camera.viewportWidth / 2;

        for (Map.Entry<Vector2, TiledMap> entry : chunkComponent.mapChunks.entrySet()) {
            Vector2 chunkPos = entry.getKey();
            float offsetX = chunkPos.x * chunkSize;
            float offsetY = chunkPos.y * chunkSize;

            if (!isChunkVisible(offsetX, offsetY)) continue;

            renderChunk(entry.getValue(), offsetX, offsetY, cameraWidth, cameraHeight);
        }
    }
    private void renderChunk(TiledMap chunk, float offsetX, float offsetY, float cameraWidth, float cameraHeight) {
        chunkRenderer.setMap(chunk);
        tempMatrix.set(cameraComponent.camera.combined);
        tempMatrix.translate(offsetX, offsetY, 0);

        float left = Math.max(0, cameraComponent.camera.position.x - cameraWidth - offsetX);
        float bottom = Math.max(0, cameraComponent.camera.position.y - cameraHeight - offsetY);
        float right = Math.min(chunkSize, cameraComponent.camera.position.x + cameraWidth - offsetX);
        float top = Math.min(chunkSize, cameraComponent.camera.position.y + cameraHeight - offsetY);

        if (right > left && top > bottom) {
            chunkRenderer.setView(tempMatrix, left, bottom, right, top);
            chunkRenderer.render();
        }
    }

    public void debugRenderChunkBoundaries() {
        AtomicInteger objects = new AtomicInteger();
        shapeRenderer.setProjectionMatrix(cameraComponent.camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        for (Map.Entry<Vector2, TiledMap> entry : chunkComponent.mapChunks.entrySet()) {
            shapeRenderer.rect(entry.getKey().x * chunkSize, entry.getKey().y * chunkSize, chunkSize, chunkSize);

            entry.getValue().getLayers().get("OBJECTS").getObjects().forEach(obj -> {
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
                    objects.getAndIncrement();
                } else if (obj instanceof PolygonMapObject) {
                    Polygon poly = ((PolygonMapObject) obj).getPolygon();
                    shapeRenderer.polygon(poly.getTransformedVertices());
                    objects.getAndIncrement();
                }
            });
        }
        shapeRenderer.end();
        Gdx.app.log("ChunkManager", "Objects: " + objects.get());

    }

    private boolean isChunkVisible(float offsetX, float offsetY) {
        return cameraComponent.cameraBounds.overlaps(new Rectangle(offsetX, offsetY, chunkSize, chunkSize));
    }
}
