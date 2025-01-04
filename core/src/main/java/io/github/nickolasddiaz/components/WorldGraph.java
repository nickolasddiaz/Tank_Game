package io.github.nickolasddiaz.components;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import com.dongbat.jbump.World;

import java.util.*;

import static com.badlogic.gdx.math.MathUtils.random;
import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE; // Buffer distance from obstacles
import static io.github.nickolasddiaz.systems.MapGenerator.itemSize; // Space between nodes

public class WorldGraph implements IndexedGraph<WorldNode> {
    private final float diagonalSpacing = itemSize * 1.414f; // √2 * nodeSpacing
    private final HashMap<Vector2, WorldNode> nodes = new HashMap<>();
    private final HashMap<WorldNode, Integer> nodeIndices = new HashMap<>();
    private int nextIndex = 0;

    private final Map<Vector2, Set<Item>> spatialCache = new HashMap<>();
    private static final int GRID_CELL_SIZE = 8;

    private final Vector2[] neighborOffsets = {
        new Vector2(-itemSize, 0), new Vector2(itemSize, 0),
        new Vector2(0, -itemSize), new Vector2(0, itemSize),
        new Vector2(-itemSize, -itemSize), new Vector2(itemSize, itemSize),
        new Vector2(-itemSize, itemSize), new Vector2(itemSize, -itemSize)
    };

    public WorldGraph() {}

    public void updateChunk(Vector2 chunkPosition, ArrayList<Item> structureItems, ArrayList<Item> oceanItems,
                            World<RectangleMapObject> tileWorld, World<PolygonMapObject> oceanWorld) {
        float chunkSize = 80 * 64;
        Rectangle chunkBounds = new Rectangle(
            chunkPosition.x * chunkSize,
            chunkPosition.y * chunkSize,
            chunkSize,
            chunkSize
        );

        clearChunkNodes(chunkBounds);
        updateSpatialCache(structureItems, oceanItems);

        // Optimize node placement by using larger spacing in open areas
        float spacing = itemSize * 2; // Increased spacing
        for (float x = chunkBounds.x; x < chunkBounds.x + chunkBounds.width; x += spacing) {
            for (float y = chunkBounds.y; y < chunkBounds.y + chunkBounds.height; y += spacing) {
                Vector2 position = new Vector2(x, y);

                // Only create nodes where needed (near obstacles or in strategic locations)
                if (isStrategicLocation(position, structureItems) && isPositionValid(position, structureItems, oceanItems)) {
                    WorldNode node = new WorldNode(position);
                    nodes.put(position, node);
                    nodeIndices.put(node, nextIndex++);
                }
            }
        }

        connectNodesOptimized(chunkBounds, tileWorld, oceanWorld);
    }

    private boolean isStrategicLocation(Vector2 position, ArrayList<Item> structureItems) {
        // Create nodes near obstacles and at strategic points
        boolean nearObstacle = false;
        float checkRadius = itemSize * 3;

        for (Item item : getItemsInRange(position, checkRadius)) {
            if (item.userData instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) item.userData).getRectangle();
                float dist = distanceToRectangle(position, rect);
                if (dist < checkRadius) {
                    nearObstacle = true;
                    break;
                }
            }
        }

        // Also create some nodes in open areas for better pathfinding
        return nearObstacle || random.nextFloat() < 0.2f; // 20% chance in open areas
    }

    private float distanceToRectangle(Vector2 point, Rectangle rect) {
        float dx = Math.max(rect.x - point.x, 0);
        float dy = Math.max(rect.y - point.y, 0);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void updateSpatialCache(ArrayList<Item> structureItems, ArrayList<Item> oceanItems) {
        spatialCache.clear();

        // Group items by grid cells for faster spatial queries
        for (Item item : structureItems) {
            Rectangle rect = ((RectangleMapObject) item.userData).getRectangle();
            addToSpatialCache(item, rect);
        }

        for (Item item : oceanItems) {
            Rectangle rect = ((PolygonMapObject) item.userData).getPolygon().getBoundingRectangle();
            addToSpatialCache(item, rect);
        }
    }

    private void addToSpatialCache(Item item, Rectangle rect) {
        int startX = (int)(rect.x / GRID_CELL_SIZE);
        int startY = (int)(rect.y / GRID_CELL_SIZE);
        int endX = (int)((rect.x + rect.width) / GRID_CELL_SIZE);
        int endY = (int)((rect.y + rect.height) / GRID_CELL_SIZE);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                Vector2 cell = new Vector2(x, y);
                spatialCache.computeIfAbsent(cell, k -> new HashSet<>()).add(item);
            }
        }
    }

    private Collection<Item> getItemsInRange(Vector2 position, float radius) {
        int startX = (int)((position.x - radius) / GRID_CELL_SIZE);
        int startY = (int)((position.y - radius) / GRID_CELL_SIZE);
        int endX = (int)((position.x + radius) / GRID_CELL_SIZE);
        int endY = (int)((position.y + radius) / GRID_CELL_SIZE);

        Set<Item> items = new HashSet<>();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                Set<Item> cellItems = spatialCache.get(new Vector2(x, y));
                if (cellItems != null) {
                    items.addAll(cellItems);
                }
            }
        }
        return items;
    }
    private void connectNodesOptimized(Rectangle chunkBounds, World<RectangleMapObject> tileWorld,
                                       World<PolygonMapObject> oceanWorld) {
        float extendedBuffer = itemSize * 2;
        Rectangle extendedBounds = new Rectangle(
            chunkBounds.x - extendedBuffer,
            chunkBounds.y - extendedBuffer,
            chunkBounds.width + extendedBuffer * 2,
            chunkBounds.height + extendedBuffer * 2
        );

        // Use spatial partitioning for faster neighbor finding
        Map<Vector2, WorldNode> localNodes = new HashMap<>();
        nodes.forEach((pos, node) -> {
            if (extendedBounds.contains(pos.x, pos.y)) {
                localNodes.put(pos, node);
            }
        });

        // Connect nodes using pre-calculated offsets
        localNodes.forEach((pos, node) -> {
            for (Vector2 offset : neighborOffsets) {
                Vector2 neighborPos = new Vector2(pos).add(offset);
                WorldNode neighbor = localNodes.get(neighborPos);

                if (neighbor != null && isPathClear(node.position, neighbor.position, tileWorld, oceanWorld)) {
                    node.addConnection(new WorldConnection(node, neighbor));
                }
            }
        });
    }

    private boolean isPositionValid(Vector2 position, ArrayList<Item> structureItems, ArrayList<Item> oceanItems) {
        // Check structures
        for (Item item : structureItems) {
            Rectangle rect = ((com.badlogic.gdx.maps.objects.RectangleMapObject) item.userData).getRectangle();
            if (isPositionNearRectangle(position, rect)) {
                return false;
            }
        }

        // Check ocean
        for (Item item : oceanItems) {
            Rectangle rect = ((com.badlogic.gdx.maps.objects.PolygonMapObject) item.userData)
                .getPolygon().getBoundingRectangle();
            if (isPositionNearRectangle(position, rect)) {
                return false;
            }
        }

        return true;
    }

    private boolean isPositionNearRectangle(Vector2 position, Rectangle rect) {
        // Check if position is inside or within buffer distance of rectangle
        return position.x >= rect.x - TILE_SIZE &&
            position.x <= rect.x + rect.width + TILE_SIZE &&
            position.y >= rect.y - TILE_SIZE &&
            position.y <= rect.y + rect.height + TILE_SIZE;
    }

    private void clearChunkNodes(Rectangle chunkBounds) {
        nodes.entrySet().removeIf(entry -> {
            Vector2 pos = entry.getValue().position;
            return chunkBounds.contains(pos.x, pos.y);
        });
    }

    private void connectNodes(Rectangle chunkBounds, World<RectangleMapObject> tileWorld, World<PolygonMapObject> oceanWorld) {
        // Get all nodes in and around the chunk
        Array<WorldNode> chunkNodes = new Array<>();
        float extendedBuffer = itemSize * 2; // Look slightly outside chunk for connections

        Rectangle extendedBounds = new Rectangle(
            chunkBounds.x - extendedBuffer,
            chunkBounds.y - extendedBuffer,
            chunkBounds.width + extendedBuffer * 2,
            chunkBounds.height + extendedBuffer * 2
        );

        nodes.values().forEach(node -> {
            if (extendedBounds.contains(node.position.x, node.position.y)) {
                chunkNodes.add(node);
            }
        });

        // Connect nodes to neighbors
        for (int i = 0; i < chunkNodes.size; i++) {
            WorldNode node = chunkNodes.get(i);
            for (int j = i + 1; j < chunkNodes.size; j++) {
                WorldNode other = chunkNodes.get(j);
                float dist = node.position.dst(other.position);
                if (dist <= diagonalSpacing + 0.1f) { // Small epsilon for float comparison
                    // Check if connection crosses any obstacles
                    if (isPathClear(node.position, other.position, tileWorld, oceanWorld)) {
                        node.addConnection(new WorldConnection(node, other));
                        other.addConnection(new WorldConnection(other, node)); // Add reverse connection
                    }
                }
            }
        }
    }

    private final CollisionFilter structureFilter = (item, item1) -> {
        if (item.userData instanceof RectangleMapObject) {
            RectangleMapObject rectObj = (RectangleMapObject) item.userData;
            if ("STRUCTURE".equals(rectObj.getName())) {
                return Response.cross;
            }
        }
        return null;
    };
    private final CollisionFilter oceanFilter = (item, item1) -> {
        if (item.userData instanceof PolygonMapObject) {
            PolygonMapObject rectObj = (PolygonMapObject) item.userData;
            if ("OCEAN".equals(rectObj.getName())) {
                return Response.cross;
            }
        }
        return null;
    };

    private boolean isPathClear(Vector2 start, Vector2 end, World<RectangleMapObject> tileWorld, World<PolygonMapObject> oceanWorld) {

        ArrayList<Item> items = new ArrayList<>();
        tileWorld.queryRay(start.x, start.y, end.x, end.y, structureFilter, items);
        if (!items.isEmpty()) {
            return false;
        }
        ArrayList<Item> oceanItems = new ArrayList<>();
        oceanWorld.queryRay(start.x, start.y, end.x, end.y, oceanFilter, oceanItems);
        return oceanItems.isEmpty();
    }

    public WorldNode getNodeAt(float x, float y) {
        Vector2 closest = null;
        float minDist = Float.MAX_VALUE;

        for (Vector2 pos : nodes.keySet()) {
            float dist = Vector2.dst(x, y, pos.x, pos.y);
            if (dist < minDist) {
                minDist = dist;
                closest = pos;
            }
        }

        return closest != null ? nodes.get(closest) : null;
    }

    @Override
    public Array<Connection<WorldNode>> getConnections(WorldNode fromNode) {
        return fromNode.getConnections();
    }

    @Override
    public int getIndex(WorldNode node) {
        return nodeIndices.getOrDefault(node, -1);
    }

    @Override
    public int getNodeCount() {
        return nodes.size();
    }
}
