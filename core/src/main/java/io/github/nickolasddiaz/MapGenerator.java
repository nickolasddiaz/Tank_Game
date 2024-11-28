package io.github.nickolasddiaz;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;

import java.util.*;

enum BiomeType {
    NONE(.0f), PLAINS(.9f), DESSERT(.3f), OCEAN(.0f), WILD_WEST(.5f), TUNDRA(.2f), ROAD(1f);

    private final float chance;
    BiomeType(float chance) {
        this.chance = chance;
    }

    public float getChance() {
        return chance;
    }
}

public class MapGenerator {

    public static final int ROAD_SIZE = 4 * 2; //how wide/long will the road be in tiles the road will only be two tiles wide. It needs to be even
    public static final int MAP_SIZE = 16 * ROAD_SIZE; //how many tiles will there be in a chunk
    public static final int TILE_SIZE = 8;
    public static final float FREQUENCY = 0.02f; //how much noise will be generated
    private static final double INITIAL_DENSITY = 0.07; //how many roads will there be
    public static final int TERRAIN_SIZE = MAP_SIZE / ROAD_SIZE;



    private final HashMap<Integer, BiomeType> biomes;
    private final HashMap<BiomeType, TextureRegion> biomeTextures;
    private final FastNoiseLite noise;
    private final TerrainGenerator roads;
    private final int seed;



    public MapGenerator(int seed) {
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
        initializeBiomeTextures();
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
    private void initializeBiomeTextures() {         // Converts textures to TextureRegions
        for (BiomeType type : BiomeType.values()) {
            Texture texture = new Texture(getBiomeTexturePath(type));
            biomeTextures.put(type, new TextureRegion(texture));
        }
    }

    private String getBiomeTexturePath(BiomeType type) {
        switch (type) {
            case PLAINS: return "plains.png";
            case DESSERT: return "desert.png";
            case OCEAN: return "ocean.png";
            case WILD_WEST: return "wild_west.png";
            case TUNDRA: return "tundra.png";
            case ROAD: return "road.png";
            default: return "road.png";     }
    }
}
