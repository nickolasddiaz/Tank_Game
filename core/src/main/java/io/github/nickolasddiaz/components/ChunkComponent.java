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


public class ChunkComponent implements Component {
    public HashMap<Vector2, TiledMap> mapChunks = new HashMap<>();
    public Vector2 currentChunk = new Vector2(0, 0);
    public Random random = new Random(System.currentTimeMillis());
    public float carWidth = 64;
    public WorldGraph pathfindingGraph = new WorldGraph();


    public final World<RectangleMapObject> tileWorld; // HORIZONTAL, VERTICAL, STRUCTURE, DECORATION
    public final World<PolygonMapObject> oceanWorld; //OCEAN

    private final HashMap<Vector2, ArrayList<Item>> chunkItems = new HashMap<>();

    public ChunkComponent() {
        tileWorld = new World<>();
        oceanWorld = new World<>();
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

    public ArrayList<Item> getObjectIsInside(CollisionFilter filter,Vector2 playerPosition) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryPoint(playerPosition.x, playerPosition.y, filter, items);
        return items.isEmpty() ? null : items;
    }

    public Rectangle getObjectIsInsideRect(Rectangle playerRect, CollisionFilter filter) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryRect(playerRect.x, playerRect.y, playerRect.width, playerRect.height,filter, items);
        return items.isEmpty() ? null : ((RectangleMapObject) items.get(0).userData).getRectangle();
    }

    public ArrayList<Item> getObjectIsInsideRect(CollisionFilter filter,Rectangle playerRect) {
        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryRect(playerRect.x, playerRect.y, playerRect.width, playerRect.height, filter, items);
        return items.isEmpty() ? null : items;
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

    public void cacheObjectsNodes(){
        pathfindingGraph = new WorldGraph();
        for(Map.Entry<Vector2, TiledMap> entry : mapChunks.entrySet()) {
            Vector2 chunkPosition = entry.getKey();
            TiledMap chunkMap = entry.getValue();
            MapObjects objects = chunkMap.getLayers().get("OBJECTS").getObjects();
            ArrayList<Item> structureItems = new ArrayList<>();
            ArrayList<Item> oceanItems = new ArrayList<>();
            objects.forEach(obj -> {
                if (obj.getName() != null) {
                    if (obj instanceof RectangleMapObject) {
                        RectangleMapObject rectObj = (RectangleMapObject) obj;
                        if ("STRUCTURE".equals(rectObj.getName())) {
                            structureItems.add(new Item<>(rectObj));
                        }
                    } else if (obj instanceof PolygonMapObject) {
                        PolygonMapObject polyObj = (PolygonMapObject) obj;
                        oceanItems.add(new Item<>(polyObj));
                    }
                }
            });
            pathfindingGraph.updateChunk(chunkPosition, structureItems, oceanItems, tileWorld, oceanWorld);
        }

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


}
