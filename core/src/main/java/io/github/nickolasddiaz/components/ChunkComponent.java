package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
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
// itemSize how many meters of units in a tile, 2 meters or 64 units
// chunkSize how many meters of units in a chunk, 5120 units or 160 meters
// ALL_CHUNK_SIZE how many tiles in a row of three chunks or the entire load length, 240 tiles = 3 * MAP_SIZE

public class ChunkComponent implements Component {
    public ShapeRenderer shapeRenderer = new ShapeRenderer();
    public HashMap<Vector2, TiledMap> mapChunks = new HashMap<>();
    public HashMap<Vector2, boolean[][]> walkChunks = new HashMap<>();
    public Vector2 currentChunk = new Vector2(0, 0);
    public Random random = new Random(System.currentTimeMillis());
    public float carWidth = 64/TILE_PER_METER;
    public WorldGraph pathfindingGraph;
    public CollisionCategory category;
    private StatsComponent statsComponent;
    private SettingsComponent settingsComponent;

    private final Sound car_sound = Gdx.audio.newSound(Gdx.files.internal("sounds/destroy_car.mp3"));
    private final Sound tank_sound = Gdx.audio.newSound(Gdx.files.internal("sounds/destroy_tank.mp3"));
    private final Sound house_sound = Gdx.audio.newSound(Gdx.files.internal("sounds/house_destroy.mp3"));
    private final Sound deflect_sound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank_deflect.mp3"));

    public World world;
    // Store bodies for each chunk to manage cleanup
    private final HashMap<Vector2, ArrayList<Body>> chunkBodies = new HashMap<>();

    public ChunkComponent() {
        this.world = new World(new Vector2(0, 0), true);
        category = new CollisionCategory();
        world.setContactListener(GameContactListener);
        shapeRenderer.setAutoShapeType(true);
    }
    public void stats(StatsComponent statsComponent, SettingsComponent settingsComponent){
        this.statsComponent = statsComponent;
        this.settingsComponent = settingsComponent;
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

    public Vector2 getPointEnemySpawn(Vector2 playerPosition) {
        float worldPercent = 2f;
        float spawnDistance = worldPercent * chunkSize;

        int randomIndex = random.nextInt(4);// 0 top 1 right 2 bottom 3 left

        // Generate four random spawn locations (one on each side of a square)
        Vector2 bottomPoint = new Vector2(
            playerPosition.x + (randomIndex == 0 ? -chunkSize : randomIndex == 1 ? chunkSize : randomIndex == 2 ? -chunkSize : -spawnDistance),
            playerPosition.y + (randomIndex == 0 ? chunkSize : randomIndex == 1 ? -chunkSize : randomIndex == 2 ? -spawnDistance : -chunkSize));

        Vector2 topPoint = new Vector2(
            playerPosition.x + (randomIndex == 0 ? chunkSize : randomIndex == 1 ? spawnDistance : randomIndex == 2 ? chunkSize : -chunkSize),
            playerPosition.y + (randomIndex == 0 ? spawnDistance : randomIndex == 1 ? chunkSize : randomIndex == 2 ? -chunkSize : chunkSize));

        // Array to store a valid spawn point
        List<Vector2> result = new ArrayList<>();

        world.QueryAABB(fixture -> {
            if ((fixture.getFilterData().categoryBits & ROAD) != 0) {
                return false;
            }
            if(fixture.getBody().getPosition() != null)
                result.add(new Vector2(fixture.getBody().getPosition()));
            return true;
        }, bottomPoint.x, bottomPoint.y, topPoint.x, topPoint.y);

        // If no valid spawn found, return player's position as fallback
        if(result.isEmpty()) {
            return null;
        }else{
            return result.get(random.nextInt(result.size()));
        }
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
        Vector2 mapPosition = new Vector2(grid.x % MAP_SIZE-itemSize, grid.y % MAP_SIZE-itemSize/2);
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
        MapObjects objects = chunkMap.getLayers().get("OBJECTS").getObjects();
        objects.forEach(obj -> {
            if (obj instanceof RectangleMapObject) {
                RectangleMapObject rectObj = (RectangleMapObject) obj;
                Rectangle rect = rectObj.getRectangle();
                if (rect.contains(position)) {
                    objects.remove(obj);
                }
            }
        });
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
            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            short categoryA = fixtureA.getFilterData().categoryBits;
            short categoryB = fixtureB.getFilterData().categoryBits;

            // Ensure that the lower category bit is always `fixtureA` for consistent processing
            if (categoryA > categoryB) {
                Fixture temp = fixtureA;
                fixtureA = fixtureB;
                fixtureB = temp;
                short tempCategory = categoryA;
                categoryA = categoryB;
                categoryB = tempCategory;
            }

            switch (categoryA) {
                case CAR:
                    if ((categoryB & (P_BULLET | P_MISSILE | P_MINE | E_BULLET | E_MISSILE | E_MINE)) != 0) {
                        if(settingsComponent.is_Playing)
                            car_sound.play(settingsComponent.sfxVolume);
                        ((TransformComponent) fixtureA.getBody().getUserData()).health = 0;
                        ((TransformComponent) fixtureB.getBody().getUserData()).health = 0;
                        if((categoryB & (P_BULLET | P_MISSILE | P_MINE)) != 0){
                            statsComponent.addScore(2);
                        }
                    } else if ((categoryB & (ENEMY | PLAYER | ALLY)) != 0) {
                        car_sound.play(settingsComponent.sfxVolume);
                        ((TransformComponent) fixtureA.getBody().getUserData()).health = 0;
                        if((categoryB & ENEMY) == 0){
                            statsComponent.addScore(1);
                        }
                    }
                    break;
                case ENEMY:
                    if ((categoryB & (P_BULLET | P_MISSILE | P_MINE)) != 0) {
                        handleDamage((TransformComponent) fixtureB.getBody().getUserData(), ((TransformComponent) fixtureA.getBody().getUserData()));
                        if(((TransformComponent) fixtureB.getBody().getUserData()).health <= 0){
                            statsComponent.addScore(1);
                        }
                    }
                    break;

                case PLAYER:
                case ALLY:
                    if ((categoryB & (E_BULLET | E_MISSILE | E_MINE)) != 0) {
                        handleDamage((TransformComponent) fixtureB.getBody().getUserData(), (TransformComponent) fixtureA.getBody().getUserData());
                    }
                    break;

                case HORIZONTAL_ROAD:
                case VERTICAL_ROAD:
                case DECORATION:
                    if ((categoryB & (PLAYER | ALLY | ENEMY)) != 0) {
                        handleSpeedBoost(categoryA != DECORATION, (TransformComponent) fixtureB.getBody().getUserData(),true);
                    }
                    break;

                case STRUCTURE:
                    if ((categoryB & (PLAYER | ALLY | ENEMY)) != 0) {
                        structure((TransformComponent) fixtureB.getBody().getUserData(), fixtureA.getBody());
                    } else if ((categoryB & (P_BULLET | P_MISSILE | P_MINE | E_BULLET | E_MISSILE | E_MINE)) != 0) {
                        ((TransformComponent) fixtureB.getBody().getUserData()).health = 0;
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void endContact(Contact contact) {
            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            short categoryA = fixtureA.getFilterData().categoryBits;
            short categoryB = fixtureB.getFilterData().categoryBits;

            // Ensure that the lower category bit is always `fixtureA` for consistent processing
            if (categoryA > categoryB) {
                fixtureB = fixtureA;
                short tempCategory = categoryA;
                categoryA = categoryB;
                categoryB = tempCategory;
            }

            switch (categoryA) {
                case HORIZONTAL_ROAD:
                case VERTICAL_ROAD:
                case DECORATION:
                    if ((categoryB & (PLAYER | ALLY | ENEMY)) != 0) {
                        handleSpeedBoost(categoryA != DECORATION, (TransformComponent) fixtureB.getBody().getUserData(),false);
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {}

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {}
    };


    public void structure(TransformComponent transformB, Body bodyA){
        if(transformB.stats.CanDestroy) {
                house_sound.play(settingsComponent.sfxVolume);
                destroyStructure(bodyA.getPosition());
                bodyA.setUserData(true);
          }
    }

    private void handleDamage(TransformComponent transformA, TransformComponent transformB) {
        int armor = transformB.stats.reduceDamage;
        if(armor > transformB.health){
            transformA.health = 0;
            deflect_sound.play(settingsComponent.sfxVolume);
            return;
        }
        transformB.health -= transformA.health - armor;
        if(transformB.health <= 0){
            tank_sound.play(settingsComponent.sfxVolume);
        }else{
            deflect_sound.play(settingsComponent.sfxVolume);
        }
        transformA.health = 0;
    }

    private void handleSpeedBoost(boolean roadOrBush, TransformComponent transform, boolean apply) {
            if(roadOrBush)
                transform.stats.onRoad +=  apply? 1:-1 ;
            else
                transform.stats.onBush += apply? 1:-1;
    }
}
