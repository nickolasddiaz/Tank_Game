package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.*;

public class ChunkComponent implements Component {
    public HashMap<Vector2, TiledMap> mapChunks = new HashMap<>();
    public Vector2 currentChunk = new Vector2(0, 0);
    public Random random = new Random(System.currentTimeMillis());
    public float carWidth = 64;

    // Caches for different types of objects
    public HashMap<Vector2, RectangleMapObject> horizontalRoadObject = new HashMap<>(); //"HORIZONTAL" object name
    public HashMap<Vector2, RectangleMapObject> verticalRoadObject = new HashMap<>(); //"VERTICAL"
    public HashMap<Vector2, PolygonMapObject> oceanObject = new HashMap<>(); //OCEAN
    public HashMap<Vector2, RectangleMapObject> houseObject = new HashMap<>(); // "STRUCTURE"
    public HashMap<Vector2, RectangleMapObject> obstacleObject = new HashMap<>(); // "DECORATION"


    public Rectangle getObjectIsInside(Vector2 playerPosition, Vector2 chunkPosition, HashMap<Vector2, RectangleMapObject> objects) {
        if(objects.isEmpty()) return null;
        for (Map.Entry<Vector2, RectangleMapObject> entry : objects.entrySet()) {
            if (entry.getKey().equals(chunkPosition) && entry.getValue().getRectangle().contains(playerPosition)) {
                return entry.getValue().getRectangle();
            }
        }
        return null;
    }

    // Cache objects for a specific chunk
    public void cacheObjects(Vector2 chunkPosition, TiledMap chunkMap) {
        mapChunks.put(chunkPosition, chunkMap);
        MapObjects objects = new MapObjects();

        chunkMap.getLayers().get("OBJECTS").getObjects().forEach(obj -> {
            if (obj.getName() != null) {
                if (obj instanceof RectangleMapObject) {
                    RectangleMapObject rectObj = (RectangleMapObject) obj;
                    switch (obj.getName()) {
                        case "HORIZONTAL":
                            horizontalRoadObject.put(chunkPosition, rectObj);
                            break;
                        case "VERTICAL":
                            verticalRoadObject.put(chunkPosition, rectObj);
                            break;
                        case "STRUCTURE":
                            houseObject.put(chunkPosition, rectObj);
                            break;
                        case "DECORATION":
                            obstacleObject.put(chunkPosition, rectObj);
                            break;
                    }
                } else if (obj instanceof PolygonMapObject) {
                    PolygonMapObject polyObj = (PolygonMapObject) obj;
                    if ("OCEAN".equals(obj.getName())) {
                        oceanObject.put(chunkPosition, polyObj);
                    }
                }
            }
        });
    }

    public void removeCache(Vector2 chunkPosition) {
        horizontalRoadObject.remove(chunkPosition);
        verticalRoadObject.remove(chunkPosition);
        oceanObject.remove(chunkPosition);
        houseObject.remove(chunkPosition);
        obstacleObject.remove(chunkPosition);
        mapChunks.remove(chunkPosition);
    }
    public void clearCache() {
        horizontalRoadObject.clear();
        verticalRoadObject.clear();
        oceanObject.clear();
        houseObject.clear();
        obstacleObject.clear();
        mapChunks.clear();
    }


}
