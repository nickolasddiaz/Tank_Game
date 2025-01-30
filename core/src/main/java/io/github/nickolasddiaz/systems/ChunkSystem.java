package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.MapGenerator;

import java.util.HashMap;
import java.util.Map;

import static io.github.nickolasddiaz.utils.MapGenerator.*;
import static io.github.nickolasddiaz.utils.MapGenerator.MAP_SIZE;

public class ChunkSystem extends EntitySystem {
    private final ComponentMapper<ChunkComponent> chunkMapper;
    private final ComponentMapper<CameraComponent> cameraMapper;
    private final ComponentMapper<SettingsComponent> settingsMapper;

    private final OrthogonalTiledMapRenderer chunkRenderer;
    private final MapGenerator mapGenerator;
    private final Matrix4 tempMatrix;
    private final int CHUNK_LOAD_RADIUS = 1;

    // Cache components to avoid repeated lookups
    private ChunkComponent chunk;
    private CameraComponent cameraComponent;
    private final TransformComponent tankComponent;
    private SettingsComponent settingsComponent;

    CarFactory carFactory;

    public ChunkSystem(CarFactory carFactory, TransformComponent transformComponent) {
        this.carFactory = carFactory;

        // Initialize mappers
        chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
        cameraMapper = ComponentMapper.getFor(CameraComponent.class);
        tankComponent = transformComponent;
        settingsMapper = ComponentMapper.getFor(SettingsComponent.class);

        // Initialize rendering tools
        chunkRenderer = new OrthogonalTiledMapRenderer(null, TILE_SIZE);
        mapGenerator = new MapGenerator((int)System.currentTimeMillis());
        tempMatrix = new Matrix4();
    }

    @Override
    public void addedToEngine(Engine engine) {
        Entity player = engine.getEntitiesFor(Family.all(
            ChunkComponent.class,
            CameraComponent.class,
            SettingsComponent.class
        ).get()).first();

        chunk = chunkMapper.get(player);
        cameraComponent = cameraMapper.get(player);
        settingsComponent = settingsMapper.get(player);

        loadInitialChunks();
    }

    @Override
    public void update(float deltaTime) {
        updateCamera(tankComponent.position.x, tankComponent.position.y);
        renderChunks();

        if (settingsComponent.DEBUG) {
            debugRenderChunkBoundaries();
        }
    }

    private void updateCamera(float cameraX, float cameraY) {
        int chunkX = (int) Math.floor(cameraX / chunkSize);
        int chunkY = (int) Math.floor(cameraY / chunkSize);
        Vector2 newChunk = new Vector2(chunkX, chunkY);

        if (!newChunk.equals(chunk.currentChunk)) {
            chunk.currentChunk.set(newChunk);
            updateLoadedChunks(newChunk);
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
        HashMap<Vector2, boolean[][]> tempWalkChunks = new HashMap<>();


        // Load new chunks
        for (int x = (int)centerChunk.x - CHUNK_LOAD_RADIUS; x <= centerChunk.x + CHUNK_LOAD_RADIUS; x++) {
            for (int y = (int)centerChunk.y - CHUNK_LOAD_RADIUS; y <= centerChunk.y + CHUNK_LOAD_RADIUS; y++) {
                Vector2 chunkPos = new Vector2(x, y);
                if (!chunk.mapChunks.containsKey(chunkPos)) {
                    TiledMap temp = mapGenerator.generateMap(x * MAP_SIZE, y * MAP_SIZE);
                    tempWalkChunks.put(chunkPos.cpy(), mapGenerator.getNotWalkableGrid()); // new Vector2(chunkPos) to avoid reference issues
                    spawnCars(temp.getLayers().get("OBJECTS").getObjects());
                    newChunks.put(chunkPos, temp);
                    chunk.cacheObjects(chunkPos, temp);  // Cache objects for new chunk
                } else {
                    newChunks.put(chunkPos, chunk.mapChunks.get(chunkPos));
                    tempWalkChunks.put(chunkPos.cpy(), chunk.walkChunks.get(chunkPos));
                    // re-cache existing chunk's objects
                    chunk.cacheObjects(chunkPos, chunk.mapChunks.get(chunkPos));
                }
            }
        }

        for (Vector2 chunkPos : chunk.mapChunks.keySet()) {
            if (!newChunks.containsKey(chunkPos)) {
                chunk.clearChunkBodies(chunkPos);
            }
        }

        chunk.walkChunks.clear();
        chunk.walkChunks.putAll(tempWalkChunks);

        chunk.mapChunks.clear();
        chunk.mapChunks.putAll(newChunks);
        chunk.cacheObjectsNodes();
    }

    private void loadInitialChunks() {
        for (int x = -CHUNK_LOAD_RADIUS; x <= CHUNK_LOAD_RADIUS; x++) {
            for (int y = -CHUNK_LOAD_RADIUS; y <= CHUNK_LOAD_RADIUS; y++) {
                Vector2 chunkPos = new Vector2(x, y);
                TiledMap temp = mapGenerator.generateMap(x * MAP_SIZE, y * MAP_SIZE);
                spawnCars(temp.getLayers().get("OBJECTS").getObjects());
                chunk.cacheObjects(chunkPos, temp);
                chunk.walkChunks.put(chunkPos.cpy(), mapGenerator.getNotWalkableGrid());
                chunk.mapChunks.put(chunkPos, temp);
            }
        }
        chunk.cacheObjectsNodes();
    }
    private void spawnCars(MapObjects objects) {
        for (MapObject object : objects) {
            if (object instanceof RectangleMapObject) {
                //if(chunk.random.nextFloat() < .6) continue; // 1/3 chance
                if ("HORIZONTAL".equals(object.getName())) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    boolean isRight = chunk.random.nextBoolean();
                    float spawnY = rect.y + ((isRight) ? 0 : rect.height - chunk.carWidth);
                    float spawnX = rect.x + chunk.random.nextFloat() * rect.width; // random x between rect.x and rect.x + rect.width
                    carFactory.createCar(new Vector2(spawnX, spawnY), isRight,
                        (isRight ? rect.x + rect.width - (float)MAP_SIZE / 2 : rect.x + (float)MAP_SIZE / 2),
                        true, chunk.random.nextInt(10));
                } else if ("VERTICAL".equals(object.getName())) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    boolean isUp = chunk.random.nextBoolean();
                    float spawnX = rect.x + ((isUp) ? rect.width - chunk.carWidth : 0);
                    float spawnY = rect.y + chunk.random.nextFloat() * rect.height; // random y between rect.y and rect.y + rect.height
                    carFactory.createCar(new Vector2(spawnX, spawnY), isUp,
                        (isUp ? rect.y + rect.height - (float)MAP_SIZE / 2 : rect.y + (float)MAP_SIZE / 2),
                        false, chunk.random.nextInt(10));
                }
            }
        }
    }

    private void renderChunks() {
        chunkRenderer.setView(cameraComponent.camera);
        float cameraHeight = cameraComponent.camera.viewportHeight / 2;
        float cameraWidth = cameraComponent.camera.viewportWidth / 2;

        for (Map.Entry<Vector2, TiledMap> entry : chunk.mapChunks.entrySet()) {
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

    public void debugRenderChunkBoundaries() { // around 250 to 500 tiledMapObjects total
        chunk.shapeRenderer.setProjectionMatrix(cameraComponent.camera.combined);
        chunk.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        chunk.shapeRenderer.setColor(Color.RED);
        for (Map.Entry<Vector2, TiledMap> entry : chunk.mapChunks.entrySet()) {
            chunk.shapeRenderer.rect(entry.getKey().x * chunkSize, entry.getKey().y * chunkSize, chunkSize, chunkSize);

            entry.getValue().getLayers().get("OBJECTS").getObjects().forEach(obj -> {
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    chunk.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
                } else if (obj instanceof PolygonMapObject) {
                    Polygon poly = ((PolygonMapObject) obj).getPolygon();
                    chunk.shapeRenderer.polygon(poly.getTransformedVertices());
                }
            });
        }
        chunk.shapeRenderer.end();
    }

    private boolean isChunkVisible(float offsetX, float offsetY) {
        return cameraComponent.cameraBounds.overlaps(new Rectangle(offsetX, offsetY, chunkSize, chunkSize));
    }
}
