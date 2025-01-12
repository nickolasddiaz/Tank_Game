package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import com.dongbat.jbump.World;
import java.util.*;

import static io.github.nickolasddiaz.systems.MapGenerator.*;
// units are used in determining positioning in the game world
// MAP_SIZE how many rows of tiles in a chunk, 80 tiles
// itemSize how many rows of units in a tile, 64 units
// chunkSize how many rows of units in a chunk, 5120 units = 80 * 64 or MAP_SIZE * itemSize
// ALL_CHUNK_SIZE how many tiles in a row of three chunks or the entire load length, 240 tiles = 3 * MAP_SIZE

public class ChunkComponent implements Component {
    public HashMap<Vector2, TiledMap> mapChunks = new HashMap<>();
    public HashMap<Vector2, boolean[][]> walkChunks = new HashMap<>();
    public Vector2 currentChunk = new Vector2(0, 0);
    public Random random = new Random(System.currentTimeMillis());
    public float carWidth = 64;
    public WorldGraph pathfindingGraph;


    public final World<RectangleMapObject> tileWorld; // HORIZONTAL, VERTICAL, STRUCTURE, DECORATION
    public final World<PolygonMapObject> oceanWorld; //OCEAN
    public final World<PolygonMapObject> movingObject; //This is for player, enemy, cars and bullets


    private final HashMap<Vector2, ArrayList<Item>> chunkItems = new HashMap<>();

    public ChunkComponent() {
        tileWorld = new World<>();
        oceanWorld = new World<>();
        movingObject = new World<>();
    }

    public boolean getObjectIsInsideBoolean(Vector2 playerPosition, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryPoint(playerPosition.x, playerPosition.y, filter, items);
        return items.isEmpty();
    }

    public Rectangle getObjectIsInside(Vector2 playerPosition, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryPoint(playerPosition.x, playerPosition.y, filter, items);
        return items.isEmpty() ? null : ((RectangleMapObject) items.get(0).userData).getRectangle();
    }


    public ArrayList<Item> getObjectsIsInside(CollisionFilter filter,Vector2 playerPosition) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryPoint(playerPosition.x, playerPosition.y, filter, items);
        return items.isEmpty() ? null : items;
    }

    public Rectangle getObjectIsInsideRect(Rectangle playerRect, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryRect(playerRect.x, playerRect.y, playerRect.width, playerRect.height,filter, items);
        return items.isEmpty() ? null : ((RectangleMapObject) items.get(0).userData).getRectangle();
    }

    public ArrayList<Item> getObjectsIsInsideRect(CollisionFilter filter,Rectangle playerRect, World world) {
        ArrayList<Item> items = new ArrayList<>();
        world.queryRect(playerRect.x, playerRect.y, playerRect.width, playerRect.height, filter, items);
        return items.isEmpty() ? null : items;
    }

    public boolean isObjectInRay(Vector2 start, Vector2 end, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.querySegment(start.x, start.y, end.x, end.y, filter, items);
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
        ArrayList<Item> items = new ArrayList<>();
        MapObjects objects = chunkMap.getLayers().get("OBJECTS").getObjects();

        objects.forEach(obj -> {
            if (obj.getName() != null) {
                if (obj instanceof RectangleMapObject) {
                    RectangleMapObject rectObj = (RectangleMapObject) obj;
                    Rectangle rect = rectObj.getRectangle();
                    Item<RectangleMapObject> item = new Item<>(rectObj);
                    tileWorld.add(item, rect.x, rect.y, rect.width, rect.height);
                    items.add(item);
                } else if (obj instanceof PolygonMapObject) {
                    PolygonMapObject polyObj = (PolygonMapObject) obj;
                    Item<PolygonMapObject> item = new Item<>(polyObj);
                    Rectangle rect = polyObj.getPolygon().getBoundingRectangle();
                    oceanWorld.add(item, rect.x, rect.y, rect.width, rect.height);
                    items.add(item);
                }
            }

        });
        chunkItems.put(chunkPosition, items);
    }

    private Vector2 gamePlaneToNormalPlane(Vector2 num, Vector2 currentChunk){
        return new Vector2((int)((Math.abs(num.x) - currentChunk.x) / MAP_SIZE + MAP_SIZE), (int)((Math.abs(num.y)- currentChunk.y) / MAP_SIZE + MAP_SIZE));
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
        for (ArrayList<Item> items : chunkItems.values()) {
            for (Item item : items) {
                if (item.userData instanceof RectangleMapObject) {
                    tileWorld.remove(item);
                } else if (item.userData instanceof PolygonMapObject) {
                    oceanWorld.remove(item);
                }
            }
        }
        chunkItems.clear();
    }


    public CollisionFilter horizontalFilter = (item, item1) -> {
        if (item.userData instanceof RectangleMapObject) {
            RectangleMapObject rectObj = (RectangleMapObject) item.userData;
            if ("HORIZONTAL".equals(rectObj.getName())) {
                return Response.cross;
            }
        }
        return null;
    };
    public CollisionFilter verticalFilter = (item, item1) -> {
        if (item.userData instanceof RectangleMapObject) {
            RectangleMapObject rectObj = (RectangleMapObject) item.userData;
            if ("VERTICAL".equals(rectObj.getName())) {
                return Response.cross;
            }
        }
        return null;
    };
    public CollisionFilter obstaclesFilter = (item, item1) -> {
        if (item.userData instanceof RectangleMapObject) {
            RectangleMapObject rectObj = (RectangleMapObject) item.userData;
            if ("STRUCTURE".equals(rectObj.getName()) || "DECORATION".equals(rectObj.getName())) {
                return Response.cross;
            }
        }
        return null;
    };
    public CollisionFilter oceanFilter = (item, item1) -> {
        if (item.userData instanceof PolygonMapObject) {
            PolygonMapObject rectObj = (PolygonMapObject) item.userData;
            if ("STRUCTURE".equals(rectObj.getName()) || "DECORATION".equals(rectObj.getName())) {
                return Response.cross;
            }
        }
        return null;
    };


}
