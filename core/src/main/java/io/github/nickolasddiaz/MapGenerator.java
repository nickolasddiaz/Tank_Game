package io.github.nickolasddiaz;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import java.util.*;

enum BiomeType {
    NONE, PLAINS, DESSERT, OCEAN, WILDWEST, TUNDRA
}

public class MapGenerator {
    public static final int MAP_SIZE = 50;
    public static final int TILE_SIZE = 8;
    public static final float FREQUENCY = 0.02f;

    private final HashMap<Integer, BiomeType> biomes;
    private final HashMap<BiomeType, TextureRegion> biomeTextures;
    private final FastNoiseLite noise ;
    private final int SEED;

    public MapGenerator(int seed) {

        biomes = new HashMap<Integer, BiomeType>();
        biomeTextures = new HashMap<BiomeType, TextureRegion>();
        noise  = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        noise.SetSeed(seed);
        noise.SetFrequency(FREQUENCY);
        noise.SetCellularReturnType(FastNoiseLite.CellularReturnType.CellValue);
        noise.SetCellularDistanceFunction(FastNoiseLite.CellularDistanceFunction.Hybrid);
        noise.SetDomainWarpType(FastNoiseLite.DomainWarpType.OpenSimplex2);
        noise.SetDomainWarpAmp(50);
        this.SEED = seed;
        initializeBiomes();
        initializeBiomeTextures();
    }

    public TiledMap generateMap(int xoffset, int yoffset) {
        int[][] biomeArray = generateNoise(xoffset, yoffset);
        return convertToTiledMap(biomeArray);
    }

    private int[][] generateNoise(int xoffset, int yoffset) {
        int[][] tileMap = new int[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                double noiseValue = noise.GetNoise((x+xoffset), (y+yoffset));
                tileMap[x][y] = assignBiomeType(noiseValue);
            }
        }
        return tileMap;
    }

    private int assignBiomeType(double noiseValue) {
        if (noiseValue < -0.9) {
            return BiomeType.OCEAN.ordinal();
        } else if (noiseValue < -0.75) {
            return BiomeType.WILDWEST.ordinal();
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
                BiomeType biomeType = biomes.get(biomesArray[x][y]);
                TextureRegion textureRegion = biomeTextures.get(biomeType);
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
            case WILDWEST: return "wildwest.png";
            case TUNDRA: return "tundra.png";
            default: return "ocean.png";
        }
    }
}
