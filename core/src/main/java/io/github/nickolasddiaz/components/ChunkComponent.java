package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.utils.CollisionCategory;
import io.github.nickolasddiaz.utils.WorldGraph;
import java.util.*;


import static io.github.nickolasddiaz.utils.CollisionCategory.*;
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
    public CollisionCategory category;

    public World world;
    // Store bodies for each chunk to manage cleanup
    private final HashMap<Vector2, ArrayList<Body>> chunkBodies = new HashMap<>();

    public ChunkComponent() {
        this.world = new World(new Vector2(0, 0), true);
        category = new CollisionCategory();
        world.setContactListener(GameContactListener);
    }
    // Helper method to create a body for a rectangle object
    public Body createRectangleBody(World world, Rectangle rect, short category) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set((rect.x + rect.width/2), (rect.y + rect.height/2));

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(rect.width/2, rect.height/2);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.filter.categoryBits = category;
        fixtureDef.filter.maskBits = categoryToFilterBits(category);
        fixtureDef.isSensor = (category == DECORATION || category == HORIZONTAL_ROAD || category == VERTICAL_ROAD);

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);

        shape.dispose();
        return body;
    }

    // Helper method to create a body for a polygon object
    public Body createChainShape(World world, Polygon polygon, short category) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(polygon.getX(), polygon.getY());
        bodyDef.angle = (float)Math.toRadians(polygon.getRotation());

        ChainShape shape = new ChainShape();
        float[] vertices = polygon.getVertices();

        shape.createLoop(vertices);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.filter.categoryBits = category;
        fixtureDef.filter.maskBits = categoryToFilterBits(category);

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);

        shape.dispose();
        return body;
    }

    // Query methods for collision detection
    public boolean isPointInside(Vector2 point, short categoryBits) {
        final boolean[] result = {false};
        world.QueryAABB(fixture -> {
            if ((fixture.getFilterData().categoryBits & categoryBits) != 0) {
                result[0] = true;
                return false;
            }
            return true;
        }, point.x - 0.1f, point.y - 0.1f, point.x + 0.1f, point.y + 0.1f);
        return result[0];
    }

    public Body[] getBodiesInRect(Rectangle rect, short categoryBits) {
        ArrayList<Body> result = new ArrayList<>();
        world.QueryAABB(fixture -> {
            if ((fixture.getFilterData().categoryBits & categoryBits) != 0) {
                result.add(fixture.getBody());
            }
            return true;
        }, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
        return result.toArray(new Body[0]);
    }


    // Cache objects for a specific chunk
    public void cacheObjects(Vector2 chunkPosition, TiledMap chunkMap) {
        ArrayList<Body> bodies = new ArrayList<>();
        MapObjects objects = chunkMap.getLayers().get("OBJECTS").getObjects();


        objects.forEach(obj -> {
            if (obj.getName() != null) {
                Body body;
                if (obj instanceof RectangleMapObject) {
                    RectangleMapObject rectObj = (RectangleMapObject) obj;
                    body = createRectangleBody(world, rectObj.getRectangle(), category.getFilterBit(rectObj.getName()));
                    bodies.add(body);
                } else if (obj instanceof PolygonMapObject) {
                    PolygonMapObject polyObj = (PolygonMapObject) obj;
                     body = createChainShape(world, polyObj.getPolygon(), category.getFilterBit(polyObj.getName()));
                    bodies.add(body);
                }
            }
        });
        chunkBodies.put(chunkPosition, bodies);
    }

    // Clear Box2D bodies for unloaded chunks
    public void clearChunkBodies(Vector2 chunkPosition) {
        ArrayList<Body> bodies = chunkBodies.get(chunkPosition);
        if (bodies != null) {
            for (Body body : bodies) {
                world.destroyBody(body);
            }
            bodies.clear();
            chunkBodies.remove(chunkPosition);
        }
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

    ContactListener GameContactListener = new ContactListener() {
        @Override
        public void beginContact(Contact contact) {
            short categoryABits = contact.getFixtureA().getFilterData().categoryBits;
            short categoryBBits = contact.getFixtureB().getFilterData().categoryBits;

            // Handle player projectiles (P_BULLET, P_MISSILE, P_MINE)
            if ((categoryABits & (P_BULLET | P_MISSILE | P_MINE)) != 0) {
                if ((categoryBBits & (ENEMY)) != 0) {
                    handleDamage(categoryBBits, 1, (TransformComponent) contact.getFixtureB().getBody().getUserData(), contact.getFixtureB().getBody());
                }
            }
            // Handle enemy projectiles (E_BULLET, E_MISSILE, E_MINE)
            else if ((categoryABits & (E_BULLET | E_MISSILE | E_MINE)) != 0) {
                if ((categoryBBits & (PLAYER | ALLY)) != 0) {
                    handleDamage(categoryBBits, 1, (TransformComponent) contact.getFixtureB().getBody().getUserData(), contact.getFixtureB().getBody());
                }
            }
            // Handle roads and decorations
            else if ((categoryABits & (HORIZONTAL_ROAD | VERTICAL_ROAD | DECORATION)) != 0) {
                handleSpeedBoost(categoryBBits, categoryABits != DECORATION, (TransformComponent) contact.getFixtureB().getBody().getUserData());
            }
            // Handle structure collisions
            else if (categoryABits == STRUCTURE) {
                structure(categoryBBits, (TransformComponent) contact.getFixtureB().getBody().getUserData(), contact.getFixtureB().getBody());
            }
            // Handle player collisions with structures
            else if (categoryABits == PLAYER) {
                structure(categoryBBits, (TransformComponent) contact.getFixtureA().getBody().getUserData(), contact.getFixtureA().getBody());
            }
        }

        @Override
        public void endContact(Contact contact) {}

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {}

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {}
    };

    public void structure(short categoryBits, TransformComponent transform, Body bodyB){
        switch (categoryBits) {
            case PLAYER: case ALLY: case ENEMY:
                if(transform.stats.CanDestroy) {
                    destroyStructure(bodyB.getPosition());
                    world.destroyBody(bodyB);
                }
        }
    }

    private void handleDamage(short categoryBits, int damage, TransformComponent transform, Body bodyB) {
        switch (categoryBits) {
            case PLAYER: case ALLY: case ENEMY:
                transform.health -= damage;
        }
        world.destroyBody(bodyB);
    }

    private void handleSpeedBoost(short categoryBits, boolean roadOrBush, TransformComponent transform) {
        switch (categoryBits) {
            case PLAYER: case ALLY: case ENEMY:
                if(roadOrBush) {
                    transform.stats.onRoad = true;
                } else {
                    transform.stats.onBush = true;
                }
        }
    }
}
