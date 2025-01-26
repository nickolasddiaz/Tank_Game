package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import com.dongbat.jbump.World;
import io.github.nickolasddiaz.utils.CollisionObject;
import io.github.nickolasddiaz.utils.WorldGraph;

import java.util.*;

import static io.github.nickolasddiaz.utils.MapGenerator.*;
// units are used in determining positioning in the game world
// MAP_SIZE how many rows of tiles in a chunk, 80 tiles
// itemSize how many rows of units in a tile, 64 units
// chunkSize how many rows of units in a chunk, 5120 units = 80 * 64 or MAP_SIZE * itemSize
// ALL_CHUNK_SIZE how many tiles in a row of three chunks or the entire load length, 240 tiles = 3 * MAP_SIZE

public class ChunkComponent implements Component {
    public ShapeRenderer shapeRenderer = new ShapeRenderer();
    public HashMap<Vector2, TiledMap> mapChunks = new HashMap<>();
    public HashMap<Vector2, boolean[][]> walkChunks = new HashMap<>();
    public Vector2 currentChunk = new Vector2(0, 0);
    public Random random = new Random(System.currentTimeMillis());
    public float carWidth = 64;
    public WorldGraph pathfindingGraph;
    private final Engine engine;


    public World<CollisionObject> world;


    private final HashMap<Vector2, ArrayList<CollisionObject>> chunkItems = new HashMap<>();

    public ChunkComponent(Engine engine) {
        world = new World<>();
        world.setTileMode(false);
        this.engine = engine;
    }

    public boolean getObjectIsInsideBoolean(Vector2 playerPosition, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        world.queryPoint(playerPosition.x, playerPosition.y, filter, items);
        return items.isEmpty();
    }

    public Rectangle getObjectIsInside(Vector2 playerPosition, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        world.queryPoint(playerPosition.x, playerPosition.y, filter, items);
        return items.isEmpty() ? null : ((RectangleMapObject) items.get(0).userData).getRectangle();
    }


    public Rectangle getObjectIsInsideRect(Rectangle playerRect, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        world.queryRect(playerRect.x, playerRect.y, playerRect.width, playerRect.height,filter, items);
        return items.isEmpty() ? null : ((CollisionObject) items.get(0).userData).getBounds();
    }

    public ArrayList<Item> getObjectsIsInsideRect(CollisionFilter filter,Rectangle playerRect) {
        ArrayList<Item> items = new ArrayList<>();
        world.queryRect(playerRect.x, playerRect.y, playerRect.width, playerRect.height, filter, items);
        return items.isEmpty() ? null : items;
    }

    public boolean isObjectInRay(Vector2 start, Vector2 end, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        world.querySegment(start.x, start.y, end.x, end.y, filter, items);
        return items.isEmpty();
    }

    public Rectangle getObjectIsInsideRectMapChunk(Rectangle playerRect, String objectName) { // old slow version
        for(Map.Entry<Vector2, TiledMap> entry : mapChunks.entrySet()) {
            TiledMap chunkMap = entry.getValue();
            MapObjects objects = chunkMap.getLayers().get("OBJECTS").getObjects();
            for (MapObject obj : objects) {
                if (obj.getName() != null) {
                    if (obj instanceof RectangleMapObject) {
                        RectangleMapObject rectObj = (RectangleMapObject) obj;
                        Rectangle rect = rectObj.getRectangle();
                        if (objectName.equals(rectObj.getName()) && playerRect.overlaps(rect)) {
                            return rect;
                        }
                    }
                }
            }
        }
        return null;
    }

    // Cache objects for a specific chunk
    public void cacheObjects(Vector2 chunkPosition, TiledMap chunkMap) {
        ArrayList<CollisionObject> items = new ArrayList<>();
        MapObjects objects = chunkMap.getLayers().get("OBJECTS").getObjects();

        objects.forEach(obj -> {
            if (obj.getName() != null) {
                CollisionObject collisionObject;
                Rectangle bounds;

                if (obj instanceof RectangleMapObject) {
                    RectangleMapObject rectObj = (RectangleMapObject) obj;
                    collisionObject = new CollisionObject(rectObj.getRectangle(), rectObj.getName(),10);
                    bounds = rectObj.getRectangle();
                } else if (obj instanceof PolygonMapObject) {
                    PolygonMapObject polyObj = (PolygonMapObject) obj;
                    collisionObject = new CollisionObject(polyObj.getPolygon(), polyObj.getName(),10);
                    bounds = polyObj.getPolygon().getBoundingRectangle();
                } else {
                    return;
                }

                Item<CollisionObject> item = new Item<>(collisionObject);
                items.add(item.userData);
                world.add(item, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        chunkItems.put(chunkPosition, items);
    }

    public void cacheObjectsNodes() {
        boolean[][] walkableGrid = new boolean[ALL_CHUNK_SIZE][ALL_CHUNK_SIZE];
        // Initialize all cells as walkable
        for (boolean[] row : walkableGrid) {
            Arrays.fill(row, true);
        }
        for (Map.Entry<Vector2, boolean[][]> entry : walkChunks.entrySet()) {
            boolean[][] nonWalkGrid = entry.getValue();
            Vector2 chunkPosition = entry.getKey().cpy();
            chunkPosition.x = (chunkPosition.x - currentChunk.x) * MAP_SIZE + MAP_SIZE;
            chunkPosition.y = (chunkPosition.y - currentChunk.y) * MAP_SIZE + MAP_SIZE;

            for (int x = 0; x < nonWalkGrid.length; x++) {
                for (int y = 0; y < nonWalkGrid[x].length; y++) {
                    if (nonWalkGrid[x][y]) {
                        walkableGrid[(int) chunkPosition.x + x][(int) chunkPosition.y + y] = false;
                    }
                }
            }
        }

        // Create the pathfinding graph
        pathfindingGraph = new WorldGraph(walkableGrid, currentChunk);
    }

    public void destroyStructure(Vector2 position) {
        Vector2 grid = worldToGridCoordinates(position);
        Vector2 mapPosition = new Vector2(grid.x % MAP_SIZE, grid.y % MAP_SIZE);
        // Get the chunk map based on the chunk position
        TiledMap chunkMap = mapChunks.get(getChunkPosition(position));
        // Retrieve the desired layer 2 where the structure resides
        TiledMapTileLayer layer = (TiledMapTileLayer) chunkMap.getLayers().get(1);
        // Remove the tile at the calculated map position
        if (layer != null) {
            for (int i = (int) mapPosition.x; i < mapPosition.x + 4; i++) {
                for (int j = (int) mapPosition.y; j < mapPosition.y + 3; j++) {
                    layer.setCell(i, j, null);
                }
            }
        }
    }


    public Vector2 getChunkPosition(Vector2 position) {
        return new Vector2((int) Math.floor(position.x / chunkSize), (int) Math.floor(position.y / chunkSize));
    }

    // assist with coordinate conversion
    public Vector2 worldToGridCoordinates(Vector2 world) {// moving units of the three chunks into an array of ALL_CHUNK_SIZE or 240 tiles
        float gridX = (world.x - (currentChunk.x * chunkSize)) / itemSize + MAP_SIZE;
        float gridY = (world.y - (currentChunk.y * chunkSize)) / itemSize + MAP_SIZE;

        return new Vector2((int) gridX, (int) gridY);
    }

    // assist with coordinate conversion
    public Vector2 GridToWorldCoordinates(Vector2 grid) { // moving an array of ALL_CHUNK_SIZE or 240 tiles into units of the three chunks
        return new Vector2(
            currentChunk.x * chunkSize  + (grid.x * itemSize) - chunkSize,
            currentChunk.y * chunkSize  + (grid.y * itemSize) - chunkSize
        );
    }

    public void clearWorlds() {
        world = new World<>();
        chunkItems.clear();
    }
    public void addWorlds(){
        engine.getEntitiesFor(Family.all(TransformComponent.class).get()).forEach(entity -> {
            Item<CollisionObject> transform = entity.getComponent(TransformComponent.class).item;

            if (transform.userData != null && transform.userData.health > 0) {
                Rectangle bounds = transform.userData.getBounds();
                world.add(transform, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
    }


    public float getAngleFromPoint(Polygon polygon, Rectangle rect){
        Vector2 polygonCenter = new Vector2(polygon.getX() + polygon.getOriginX(), polygon.getY() + polygon.getOriginY());
        Vector2 point = new Vector2(rect.x + rect.width / 2, rect.y + rect.height / 2);
        return getAngleFromPoint(polygonCenter, point);
    }

    public float getAngleFromPoint(Vector2 polygonCenter, Vector2 point) {
        Vector2 toPoint = new Vector2(point).sub(polygonCenter); // Create vector from center to point
        return MathUtils.atan2(toPoint.y, toPoint.x) * MathUtils.radiansToDegrees;  // Calculate angle in degrees
    }

    public Polygon rectangletoPolygon(Rectangle rect){
        return  new Polygon(new float[]{
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        });
    }

    public CollisionFilter createFilter(String... objectTypes) {
        return (item, other) -> {
            for (String type : objectTypes) {
                if (type.equals(((CollisionObject) item.userData).getObjectType())) {
                    return Response.cross;
                }
            }
            return null;
        };
    }

    public CollisionFilter obstaclesFilter = createFilter("STRUCTURE", "DECORATION");
    public CollisionFilter verticalFilter = createFilter("VERTICAL");
    public CollisionFilter horizontalFilter = createFilter("HORIZONTAL");
    public CollisionFilter enemyFilter = createFilter("ENEMY");
    public CollisionFilter allySpawnFilter = createFilter("PLAYER","STRUCTURE","OCEAN", "ENEMY", "ALLY");
}
