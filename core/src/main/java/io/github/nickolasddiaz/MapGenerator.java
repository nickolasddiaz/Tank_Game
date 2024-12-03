package io.github.nickolasddiaz;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import java.util.*;

enum TileType {
    //biomes; biome >= TileType.OCEAN.ordinal() && biome <= TileType.TUNDRA.ordinal()
    DESSERT, PLAINS, WILD_WEST, TUNDRA,
    //decorations; biome >= TileType.PLAIN_TREE.ordinal() && biome <= TileType.TUNDRA_ROCK.ordinal()
    PLAIN_TREE, PLAIN_SHRUB, PLAINS_STONE,
    DESSERT_SHRUB, PALM_TREE, DESSERT_STONE,
    TUMBLEWEED, CACTUS, DEAD_TREE,
    ICICLE, TUNDRA_TREE, TUNDRA_ROCK,
    //structures; biome >= TileType.PLAINS_BUILDING1.ordinal() && biome <= TileType.TUNDRA_BUILDING2_12.ordinal()
    PLAINS_BUILDING1, PLAINS_BUILDING2, PLAINS_BUILDING3, PLAINS_BUILDING4, PLAINS_BUILDING5, PLAINS_BUILDING6, PLAINS_BUILDING7, PLAINS_BUILDING8, PLAINS_BUILDING9, PLAINS_BUILDING10, PLAINS_BUILDING11, PLAINS_BUILDING12,
    PLAINS_BUILDING1_1, PLAINS_BUILDING1_2, PLAINS_BUILDING1_3, PLAINS_BUILDING1_4, PLAINS_BUILDING1_5, PLAINS_BUILDING1_6, PLAINS_BUILDING1_7, PLAINS_BUILDING1_8, PLAINS_BUILDING1_9, PLAINS_BUILDING1_10, PLAINS_BUILDING1_11, PLAINS_BUILDING1_12,
    PLAINS_BUILDING2_1, PLAINS_BUILDING2_2, PLAINS_BUILDING2_3, PLAINS_BUILDING2_4, PLAINS_BUILDING2_5, PLAINS_BUILDING2_6, PLAINS_BUILDING2_7, PLAINS_BUILDING2_8, PLAINS_BUILDING2_9, PLAINS_BUILDING2_10, PLAINS_BUILDING2_11, PLAINS_BUILDING2_12,
    DESSERT_BUILDING1, DESSERT_BUILDING2, DESSERT_BUILDING3, DESSERT_BUILDING4, DESSERT_BUILDING5, DESSERT_BUILDING6, DESSERT_BUILDING7, DESSERT_BUILDING8, DESSERT_BUILDING9, DESSERT_BUILDING10, DESSERT_BUILDING11, DESSERT_BUILDING12,
    DESSERT_BUILDING1_1, DESSERT_BUILDING1_2, DESSERT_BUILDING1_3, DESSERT_BUILDING1_4, DESSERT_BUILDING1_5, DESSERT_BUILDING1_6, DESSERT_BUILDING1_7, DESSERT_BUILDING1_8, DESSERT_BUILDING1_9, DESSERT_BUILDING1_10, DESSERT_BUILDING1_11, DESSERT_BUILDING1_12,
    DESSERT_BUILDING2_1, DESSERT_BUILDING2_2, DESSERT_BUILDING2_3, DESSERT_BUILDING2_4, DESSERT_BUILDING2_5, DESSERT_BUILDING2_6, DESSERT_BUILDING2_7, DESSERT_BUILDING2_8, DESSERT_BUILDING2_9, DESSERT_BUILDING2_10, DESSERT_BUILDING2_11, DESSERT_BUILDING2_12,
    WILD_WEST_BUILDING1, WILD_WEST_BUILDING2, WILD_WEST_BUILDING3, WILD_WEST_BUILDING4, WILD_WEST_BUILDING5, WILD_WEST_BUILDING6, WILD_WEST_BUILDING7, WILD_WEST_BUILDING8, WILD_WEST_BUILDING9, WILD_WEST_BUILDING10, WILD_WEST_BUILDING11, WILD_WEST_BUILDING12,
    WILD_WEST_BUILDING1_1, WILD_WEST_BUILDING1_2, WILD_WEST_BUILDING1_3, WILD_WEST_BUILDING1_4, WILD_WEST_BUILDING1_5, WILD_WEST_BUILDING1_6, WILD_WEST_BUILDING1_7, WILD_WEST_BUILDING1_8, WILD_WEST_BUILDING1_9, WILD_WEST_BUILDING1_10, WILD_WEST_BUILDING1_11, WILD_WEST_BUILDING1_12,
    WILD_WEST_BUILDING2_1, WILD_WEST_BUILDING2_2, WILD_WEST_BUILDING2_3, WILD_WEST_BUILDING2_4, WILD_WEST_BUILDING2_5, WILD_WEST_BUILDING2_6, WILD_WEST_BUILDING2_7, WILD_WEST_BUILDING2_8, WILD_WEST_BUILDING2_9, WILD_WEST_BUILDING2_10, WILD_WEST_BUILDING2_11, WILD_WEST_BUILDING2_12,
    TUNDRA_BUILDING1, TUNDRA_BUILDING2, TUNDRA_BUILDING3, TUNDRA_BUILDING4, TUNDRA_BUILDING5, TUNDRA_BUILDING6, TUNDRA_BUILDING7, TUNDRA_BUILDING8, TUNDRA_BUILDING9, TUNDRA_BUILDING10, TUNDRA_BUILDING11, TUNDRA_BUILDING12,
    TUNDRA_BUILDING1_1, TUNDRA_BUILDING1_2, TUNDRA_BUILDING1_3, TUNDRA_BUILDING1_4, TUNDRA_BUILDING1_5, TUNDRA_BUILDING1_6, TUNDRA_BUILDING1_7, TUNDRA_BUILDING1_8, TUNDRA_BUILDING1_9, TUNDRA_BUILDING1_10, TUNDRA_BUILDING1_11, TUNDRA_BUILDING1_12,
    TUNDRA_BUILDING2_1, TUNDRA_BUILDING2_2, TUNDRA_BUILDING2_3, TUNDRA_BUILDING2_4, TUNDRA_BUILDING2_5, TUNDRA_BUILDING2_6, TUNDRA_BUILDING2_7, TUNDRA_BUILDING2_8, TUNDRA_BUILDING2_9, TUNDRA_BUILDING2_10, TUNDRA_BUILDING2_11, TUNDRA_BUILDING2_12,
    //roads; biome >= TileType.OCEAN.ordinal()
    OCEAN,
    ROAD_LEFT,ROAD_RIGHT,ROAD_TOP,ROAD_BOTTOM,ROAD_TOP_LEFT,ROAD_TOP_RIGHT,ROAD_BOTTOM_LEFT,ROAD_BOTTOM_RIGHT,
}//169 tiles

public class MapGenerator {
    public static final int ROAD_SIZE = 5 * 2; //how wide/long will the road be in tiles the road will only be two tiles wide. It needs to be even
    public static final int MAP_SIZE = 16 * ROAD_SIZE; //how many tiles will there be in a chunk
    public static final int TILE_SIZE = 8;
    public static final float FREQUENCY = 0.01f; //how much noise will be generated
    public static final double ROAD_DENSITY = 0.1;
    public static final double DECORATION_DENSITY = 0.01;//how many roads will there be
    public static final int TERRAIN_SIZE = MAP_SIZE / ROAD_SIZE;

    private final HashMap<Integer, TileType> biomes;
    private final HashMap<TileType, TextureRegion> tileTextures;
    private final FastNoiseLite noise;
    private final TerrainGenerator roads;
    private final int seed;


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

        return convertToTiledMap(biomeMap, TerrainMap);
    }
    private boolean[] generateRoads(int xOffset, int yOffset) {
        boolean[] road = new boolean[TERRAIN_SIZE];
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
        if (noiseValue < -0.9) {
            return TileType.OCEAN.ordinal();
        } else if (noiseValue < -0.75) {
            return TileType.WILD_WEST.ordinal();
        } else if(noiseValue < -0.35) {
            return TileType.TUNDRA.ordinal();
        }else if (noiseValue < 0.30) {
            return TileType.PLAINS.ordinal();
        } else if (noiseValue < 0.80) {
            return TileType.DESSERT.ordinal();
        } else {
            return TileType.PLAINS.ordinal();
        }
    }

    //functions below to convert int[][] biomeMap and terrainMap to a TiledMap
    private TiledMap convertToTiledMap(int[][] biomeMap, Integer[][] terrainMap) {
        TiledMap map = new TiledMap();

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

        // Populate layers and objects
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                // Biome layer
                biomeLayer.setCell(x, y, precomputedBiomeCells.getOrDefault(biomeMap[x][y], precomputedBiomeCells.get(TileType.OCEAN.ordinal())));

                // Terrain layer
                if (terrainMap[x][y] != null) {
                    int terrainNumber = terrainMap[x][y];
                    terrainLayer.setCell(x, y, precomputedTerrainCells.getOrDefault(terrainNumber, precomputedTerrainCells.get(TileType.OCEAN.ordinal())));

                    // Object layer logic
                    addObjectToLayer(objectLayer, x, y, terrainNumber, biomeMap[x][y]);
                }

            }
        }

        map.getLayers().add(biomeLayer);
        map.getLayers().add(terrainLayer);

        MapLayer objectLayerContainer = new MapLayer();
        for (MapObject object : objectLayer) {
            objectLayerContainer.getObjects().add(object);
        }
        map.getLayers().add(objectLayerContainer);

        return map;
    }

    private void addObjectToLayer(MapObjects objectLayer, int x, int y, int terrainNumber, int biomeNumber) {
        if (terrainNumber >= TileType.PLAIN_TREE.ordinal() && terrainNumber <= TileType.TUNDRA_ROCK.ordinal()) {
            MapObject decorationObject = new RectangleMapObject(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            decorationObject.setName("DECORATION");
            objectLayer.add(decorationObject);
        }
        if (terrainNumber >= TileType.PLAINS_BUILDING1.ordinal() && terrainNumber <= TileType.TUNDRA_BUILDING2_12.ordinal()) {
            MapObject structureObject = new RectangleMapObject(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            structureObject.setName("STRUCTURE");
            objectLayer.add(structureObject);
        }
        if (terrainNumber >= TileType.ROAD_LEFT.ordinal()) {
            MapObject roadObject = new RectangleMapObject(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            roadObject.setName("ROAD");
            objectLayer.add(roadObject);
        }
        if (biomeNumber == TileType.OCEAN.ordinal()) {
            MapObject oceanObject = new RectangleMapObject(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            oceanObject.setName("OCEAN");
            objectLayer.add(oceanObject);
        }
    }


    private void initializeBiomes() {
        TileType[] types = TileType.values();
        for (int i = 0; i < types.length; i++) {
            biomes.put(i, types[i]);
        }
    }

    private void initializeTileTextures(TextureAtlas atlas) {
        for (TileType type : TileType.values()) {
            // Use the biome type's name as the region name (or adjust as necessary)
            TextureRegion region = atlas.findRegion(type.name().toUpperCase());
            tileTextures.put(type, region);
        }
    }
}
