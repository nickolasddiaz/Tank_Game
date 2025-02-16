package io.github.nickolasddiaz.utils;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Vector2;

import java.util.*;

public class MapGenerator {
    public static final int ROAD_SIZE = 5 * 2; //how wide/long will the road be in tiles the road will only be two tiles wide. It needs to be even
    public static final int MAP_SIZE = 8 * ROAD_SIZE; //how many tiles will there be in a chunk 80
    public static final int TILE_SIZE = 8;
    public static final float FREQUENCY = 0.01f; //how much noise will be generated
    public static final double ROAD_DENSITY = 0.1; //how many roads will there be
    public static final double DECORATION_DENSITY = 0.002; //how many decorations will there be
    public static final float TILE_PER_METER = 32f;
    public static final float itemSize = TILE_SIZE*TILE_SIZE / TILE_PER_METER ; // size of one tile in meters
    public static final int chunkSize = (int) (MAP_SIZE * itemSize); // unit of one chunk length
    public static final int ALL_CHUNK_SIZE = 3 * MAP_SIZE; // unit of three chunk length

    // units are used in determining positioning in the game world
    // MAP_SIZE how many rows of tiles in a chunk, 80 tiles
    // itemSize how many meters of units in a tile, 2 meters or 64 units
    // chunkSize how many meters of units in a chunk, 5120 units or 160 meters
    // ALL_CHUNK_SIZE how many tiles in a row of three chunks or the entire load length, 240 tiles = 3 * MAP_SIZE

    private final HashMap<Integer, TileType> biomes;
    private final HashMap<TileType, TextureRegion> tileTextures;
    private final FastNoiseLite noise;
    private final TerrainGenerator roads;
    private final int seed;

    private boolean[][] notWalkableGrid;


    public MapGenerator(int seed) {
        AssetManager assetManager = new AssetManager();

        // Queue the atlas for loading
        assetManager.load("tank_game.atlas", TextureAtlas.class);
        assetManager.finishLoading(); // Blocks until loading is complete
        TextureAtlas atlas = assetManager.get("tank_game.atlas", TextureAtlas.class);

        biomes = new HashMap<>();
        tileTextures = new HashMap<>();
        noise  = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        noise.SetSeed(seed);
        noise.SetFrequency(FREQUENCY);
        noise.SetCellularReturnType(FastNoiseLite.CellularReturnType.CellValue);
        noise.SetCellularDistanceFunction(FastNoiseLite.CellularDistanceFunction.Hybrid);
        noise.SetDomainWarpType(FastNoiseLite.DomainWarpType.OpenSimplex2);
        noise.SetDomainWarpAmp(50);
        roads = new TerrainGenerator(seed);
        initializeBiomes();
        initializeTileTextures(atlas);
        this.seed = seed;
    }

    public TiledMap generateMap(int xOffset, int yOffset) {
        int[][] biomeMap = generateNoise(xOffset, yOffset);
        Integer[][] TerrainMap = roads.generate(biomeMap, generateRoads(xOffset, yOffset),
            generateRoads(xOffset, yOffset), generateRoads(xOffset +MAP_SIZE, yOffset),
            generateRoads(xOffset, yOffset -MAP_SIZE));

        return convertToTiledMap(biomeMap, TerrainMap, xOffset * itemSize, yOffset * itemSize);
    }
    private boolean[] generateRoads(int xOffset, int yOffset) {
        boolean[] road = new boolean[TILE_SIZE];
        Random random = new Random(seed + xOffset * 31L + yOffset * 37L);
        for (int i = 0; i < road.length; i++) {
            road[i] = random.nextDouble() < ROAD_DENSITY;
        }
        return road;
    }

    private int[][] generateNoise(int xOffset, int yOffset) {
        int[][] tileMap = new int[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                double noiseValue = noise.GetNoise((x+xOffset), (y+yOffset));
                tileMap[x][y] = assignTileType(noiseValue);
            }
        }
        return tileMap;
    }

    private int assignTileType(double noiseValue) {
        if (noiseValue < -0.70) {//switch statement not possible because of the java version
            return TileType.OCEAN.ordinal();    // 15% for Ocean
        } else if (noiseValue < -0.30) {
            return TileType.WILD_WEST.ordinal();// 20% for Wild West
        } else if (noiseValue < 0.10) {
            return TileType.TUNDRA.ordinal();   // 20% for Tundra
        } else if (noiseValue < 0.60) {
            return TileType.PLAINS.ordinal();   // 25% for Plains
        } else {
            return TileType.DESSERT.ordinal();  // 20% for Desert
        }
    }

    //functions below to convert int[][] biomeMap and terrainMap to a TiledMap
    public boolean[][] getNotWalkableGrid() {
        return notWalkableGrid;
    }

    private TiledMap convertToTiledMap(int[][] biomeMap, Integer[][] terrainMap, float xOffset, float yOffset) {
        TiledMap map = new TiledMap();
        notWalkableGrid = new boolean[MAP_SIZE][MAP_SIZE];

        TiledMapTileLayer biomeLayer = new TiledMapTileLayer(MAP_SIZE, MAP_SIZE, TILE_SIZE, TILE_SIZE);
        TiledMapTileLayer terrainLayer = new TiledMapTileLayer(MAP_SIZE, MAP_SIZE, TILE_SIZE, TILE_SIZE);

        // Object layer for additional objects
        MapObjects objectLayer = new MapObjects();

        // Precompute biome and terrain textures
        Map<Integer, Cell> precomputedBiomeCells = new HashMap<>();
        Map<Integer, Cell> precomputedTerrainCells = new HashMap<>();

        for (int biomeType : biomes.keySet()) {
            TileType type = biomes.get(biomeType);
            TextureRegion texture = tileTextures.getOrDefault(type, tileTextures.get(TileType.OCEAN));
            Cell cell = new Cell();
            cell.setTile(new StaticTiledMapTile(texture));
            precomputedBiomeCells.put(biomeType, cell);
        }

        for (int terrainType : biomes.keySet()) {
            TileType type = biomes.get(terrainType);
            TextureRegion texture = tileTextures.getOrDefault(type, tileTextures.get(TileType.OCEAN));
            Cell cell = new Cell();
            cell.setTile(new StaticTiledMapTile(texture));
            precomputedTerrainCells.put(terrainType, cell);
        }
        Stack<Vector2> points = new Stack<>();
        boolean[][] visited = new boolean[MAP_SIZE][MAP_SIZE];

        for (int x = 0; x < MAP_SIZE; x++) {

            for (int y = 0; y < MAP_SIZE; y++) {
                biomeLayer.setCell(x, y, precomputedBiomeCells.getOrDefault(biomeMap[x][y], precomputedBiomeCells.get(TileType.OCEAN.ordinal())));
                if (terrainMap[x][y] != null) {
                    int terrainNumber = terrainMap[x][y];
                    terrainLayer.setCell(x, y, precomputedTerrainCells.getOrDefault(terrainNumber, precomputedTerrainCells.get(TileType.OCEAN.ordinal())));

                    if (terrainNumber >= TileType.PLAIN_TREE.ordinal() && terrainNumber <= TileType.TUNDRA_ROCK.ordinal()) {
                        MapObject decorationObject = new RectangleMapObject(x * itemSize + xOffset, y * itemSize + yOffset, itemSize, itemSize);
                        decorationObject.setName("DECORATION");
                        objectLayer.add(decorationObject);
                        notWalkableGrid[x][y] = true;
                    }
                    if (terrainNumber == TileType.PLAINS_BUILDING9.ordinal() || terrainNumber == TileType.PLAINS_BUILDING1_9.ordinal() || terrainNumber == TileType.PLAINS_BUILDING2_9.ordinal() || terrainNumber == TileType.DESSERT_BUILDING9.ordinal() || terrainNumber == TileType.DESSERT_BUILDING1_9.ordinal() || terrainNumber == TileType.DESSERT_BUILDING2_9.ordinal() || terrainNumber == TileType.TUNDRA_BUILDING9.ordinal() || terrainNumber == TileType.TUNDRA_BUILDING1_9.ordinal() || terrainNumber == TileType.TUNDRA_BUILDING2_9.ordinal() || terrainNumber == TileType.WILD_WEST_BUILDING9.ordinal() || terrainNumber == TileType.WILD_WEST_BUILDING1_9.ordinal() || terrainNumber == TileType.WILD_WEST_BUILDING2_9.ordinal()){
                        MapObject structureObject = new RectangleMapObject(x * itemSize + xOffset, y * itemSize + yOffset, 4 * itemSize, 3 * itemSize);
                        structureObject.setName("STRUCTURE");
                        objectLayer.add(structureObject);
                        for (int i = x; i < x + 4; i++) {
                            for (int j = y; j < y + 3; j++) {
                                notWalkableGrid[i][j] = true;
                            }
                        }
                    }
                }

                if (terrainMap[x][y] != null && terrainMap[x][y] >= TileType.ROAD_LEFT.ordinal()) {
                    boolean isEndOfMap = (y == MAP_SIZE - 1);
                    boolean isRoadStart = (y < MAP_SIZE - 1) &&
                        (terrainMap[x][y + 1] == null || terrainMap[x][y + 1] < TileType.ROAD_LEFT.ordinal()) &&
                        (y >= 4 && terrainMap[x][y-4] != null && terrainMap[x][y - 4] == TileType.ROAD_LEFT.ordinal());

                    if (!isEndOfMap && isRoadStart || (y == MAP_SIZE - 1 && x > 0 && (terrainMap[x-1][y] == null || terrainMap[x-1][y] < TileType.ROAD_LEFT.ordinal()))) {
                        int i = y;
                        while (i >= 0 && terrainMap[x][i] != null && terrainMap[x][i] >= TileType.ROAD_LEFT.ordinal()) {
                            i--;
                        }
                        MapObject roadObject = new RectangleMapObject(x * itemSize + xOffset, (i + 1) * itemSize +yOffset, 2 * itemSize, (y - i) * itemSize);
                        roadObject.setName("VERTICAL");
                        objectLayer.add(roadObject);
                    }
                    isEndOfMap = (x == MAP_SIZE - 1);
                    isRoadStart = (x < MAP_SIZE - 1) &&
                        (terrainMap[x+1][y] == null || terrainMap[x+1][y] < TileType.ROAD_LEFT.ordinal()) &&
                        (x >= 4 && terrainMap[x-4][y] != null && terrainMap[x-4][y] == TileType.ROAD_BOTTOM.ordinal());

                    if (!isEndOfMap && isRoadStart || (x == MAP_SIZE - 1 && y > 0 && (terrainMap[x][y-1] == null || terrainMap[x][y-1] < TileType.ROAD_LEFT.ordinal()))) {
                        int i = x;
                        while (i >= 0 && terrainMap[i][y] != null && terrainMap[i][y] >= TileType.ROAD_LEFT.ordinal()) {
                            i--;
                        }
                        MapObject roadObject = new RectangleMapObject((i + 1) * itemSize + xOffset, y * itemSize + yOffset, (x - i) * itemSize, 2 * itemSize);
                        roadObject.setName("HORIZONTAL");
                        objectLayer.add(roadObject);
                    }
                }
                if(biomeMap[x][y] == TileType.OCEAN.ordinal()){
                    notWalkableGrid[x][y] = true;
                }

                if (biomeMap[x][y] == TileType.OCEAN.ordinal() && !visited[x][y]) {
                    boolean isBoundary = false;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < MAP_SIZE && ny >= 0 && ny < MAP_SIZE) {
                                if (biomeMap[nx][ny] != TileType.OCEAN.ordinal()) {
                                    isBoundary = true;
                                    break;
                                }
                            }
                        }
                        if (isBoundary) break;
                    }

                    if (isBoundary) {
                        points.push(new Vector2(x, y));
                        visited[x][y] = true;
                        float[] temp = addOceanVectors(points, biomeMap, visited, xOffset, yOffset);
                        if(temp.length > 4) {
                            PolygonMapObject oceanObject = new PolygonMapObject(temp);
                            oceanObject.setName("OCEAN");
                            objectLayer.add(oceanObject);
                        }
                    }
                }

            }
        }

        map.getLayers().add(biomeLayer);
        map.getLayers().add(terrainLayer);

        MapLayer objectLayerContainer = new MapLayer();
        for (MapObject object : objectLayer) {
            objectLayerContainer.getObjects().add(object);
        }
        objectLayerContainer.setName("OBJECTS");
        map.getLayers().add(objectLayerContainer);

        return map;
    }

    private float[] addOceanVectors(Stack<Vector2> points, int[][] biomeMap, boolean[][] visited, float xOffset, float yOffset) {
        ArrayList<Float> vertices = new ArrayList<>();
        int[][] directions = {
            {0, 1},   // top
            {1, 1},   // top-right
            {1, 0},   // right
            {1, -1},  // bottom-right
            {0, -1},  // bottom
            {-1, -1}, // bottom-left
            {-1, 0},  // left
            {-1, 1}   // top-left
        };

        while (!points.empty()) {
            Vector2 current = points.pop();
            int x = (int) current.x;
            int y = (int) current.y;

            vertices.add((x * itemSize) + xOffset);
            vertices.add((y * itemSize) + yOffset);

            boolean foundNext = false;
            for (int[] dir : directions) {
                int newX = x + dir[0];
                int newY = y + dir[1];

                if (newX >= 0 && newX < MAP_SIZE && newY >= 0 && newY < MAP_SIZE) {
                    if (biomeMap[newX][newY] == TileType.OCEAN.ordinal() && !visited[newX][newY]) {
                        boolean isBoundary = false;
                        for (int[] checkDir : directions) {
                            int checkX = newX + checkDir[0];
                            int checkY = newY + checkDir[1];

                                if (!(checkX >= 0 && checkX < MAP_SIZE && checkY >= 0 && checkY < MAP_SIZE) || biomeMap[checkX][checkY] != TileType.OCEAN.ordinal()) {
                                    isBoundary = true;
                                    break;

                            }
                        }

                        if (isBoundary) {
                            points.push(new Vector2(newX, newY));
                            visited[newX][newY] = true;
                            foundNext = true;
                            break;
                        }
                    }
                }
            }

            if (!foundNext && !points.empty()) {
                vertices.add(Float.NaN);
                vertices.add(Float.NaN);
            }
        }
        float[] result = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            result[i] = vertices.get(i);
        }

        return result;
    }


    private void initializeBiomes() {
        TileType[] types = TileType.values();
        for (int i = 0; i < types.length; i++) {
            biomes.put(i, types[i]);
        }
    }

    private void initializeTileTextures(TextureAtlas atlas) {
        for (TileType type : TileType.values()) {
            TextureRegion region = atlas.findRegion(type.name().toUpperCase());
            tileTextures.put(type, region);
        }
    }
}
