package io.github.nickolasddiaz;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;

import java.util.*;

enum BiomeType {
    NONE, PLAINS, DESSERT, OCEAN, WILD_WEST, TUNDRA, ROAD;

}

public class MapGenerator {

    public static final int ROAD_SIZE = 5 * 2; //how wide/long will the road be in tiles the road will only be two tiles wide. It needs to be even
    public static final int MAP_SIZE = 4 * ROAD_SIZE; //how many tiles will there be in a chunk
    public static final int TILE_SIZE = 8;
    public static final float FREQUENCY = 0.01f; //how much noise will be generated
    private static final double INITIAL_DENSITY = 0.1; //how many roads will there be
    public static final int TERRAIN_SIZE = MAP_SIZE / ROAD_SIZE;



    private final HashMap<Integer, BiomeType> biomes;
    private final HashMap<BiomeType, TextureRegion> biomeTextures;
    private final FastNoiseLite noise;
    private final TerrainGenerator roads;
    private final int seed;
    private final TextureAtlas atlas;



    public MapGenerator(int seed) {
        AssetManager assetManager = new AssetManager();

        // Queue the atlas for loading
        assetManager.load("tiles.txt", TextureAtlas.class);
        assetManager.finishLoading(); // Blocks until loading is complete
        atlas = assetManager.get("tiles.txt", TextureAtlas.class);

        biomes = new HashMap<>();
        biomeTextures = new HashMap<>();
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
        initializeBiomeTextures(atlas);
        this.seed = seed;
    }

    public TiledMap generateMap(int xOffset, int yOffset) {
        int[][] noiseMap = generateNoise(xOffset, yOffset);


        // Generate map with road connections
        return convertToTiledMap(
            roads.generate(noiseMap, generateRoads(xOffset, yOffset), generateRoads(xOffset, yOffset), generateRoads(xOffset +MAP_SIZE, yOffset), generateRoads(xOffset, yOffset -MAP_SIZE))
        );
    }
    private boolean[] generateRoads(int xOffset, int yOffset) {
        boolean[] road = new boolean[TERRAIN_SIZE];
        Random random = new Random(seed + xOffset * 31 + yOffset * 37);
        for (int i = 0; i < road.length; i++) {
            road[i] = random.nextDouble() < INITIAL_DENSITY;
        }

        return road;
    }

    private int[][] generateNoise(int xOffset, int yOffset) {
        int[][] tileMap = new int[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                double noiseValue = noise.GetNoise((x+xOffset), (y+yOffset));
                tileMap[x][y] = (int) assignBiomeType(noiseValue);
            }
        }
        return tileMap;
    }

    private int assignBiomeType(double noiseValue) {
        if (noiseValue < -0.9) {
            return BiomeType.OCEAN.ordinal();
        } else if (noiseValue < -0.75) {
            return BiomeType.WILD_WEST.ordinal();
        } else if(noiseValue < -0.35) {
            return BiomeType.TUNDRA.ordinal();
        }else if (noiseValue < 0.30) {
            return BiomeType.PLAINS.ordinal();
        } else if (noiseValue < 0.80) {
            return BiomeType.DESSERT.ordinal();
        } else {
            return BiomeType.PLAINS.ordinal();
        }
    }

    //functions below to convert int[][] biomesArray to a TiledMap
    private TiledMap convertToTiledMap(int[][] biomesArray) {
        TiledMap map = new TiledMap();
        TiledMapTileLayer layer = new TiledMapTileLayer(MAP_SIZE, MAP_SIZE, TILE_SIZE, TILE_SIZE);

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                Cell cell = new Cell();

                // Safely retrieve the biome type
                BiomeType biomeType = biomes.getOrDefault(biomesArray[x][y], BiomeType.ROAD);
                TextureRegion textureRegion = biomeTextures.get(biomeType);

                // Fallback to ROAD texture if textureRegion is null
                if (textureRegion == null) {
                    textureRegion = biomeTextures.get(BiomeType.ROAD);
                }

                cell.setTile(new StaticTiledMapTile(textureRegion));
                layer.setCell(x, y, cell);
            }
        }
        map.getLayers().add(layer);
        return map;
    }

    private void initializeBiomes() {
        BiomeType[] types = BiomeType.values();
        for (int i = 0; i < types.length; i++) {
            biomes.put(i, types[i]);
        }
    }

    private void initializeBiomeTextures(TextureAtlas atlas) {
        for (BiomeType type : BiomeType.values()) {
            // Use the biome type's name as the region name (or adjust as necessary)
            TextureRegion region = atlas.findRegion(type.name().toLowerCase());

            if (region == null) {
                continue;
            }

            biomeTextures.put(type, region);
        }
    }
}
