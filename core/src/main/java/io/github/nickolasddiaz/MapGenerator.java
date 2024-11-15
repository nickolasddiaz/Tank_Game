package io.github.nickolasddiaz;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Vector2;

import java.awt.Point;
import java.util.*;

import static java.lang.Math.max;

enum BiomeType {
    NONE, PLAINS, DESSERT, OCEAN, WILDWEST, TUNDRA
}

public class MapGenerator {
    public static final int MAP_SIZE = 200;
    public static final int TILE_SIZE = 8;
    public static final double FREQUENCY = 0.02;

    private final HashMap<Integer, BiomeType> biomes;
    private final HashMap<BiomeType, TextureRegion> biomeTextures;
    private final OpenSimplexNoise openSimplexNoise;
    private final Long SEED;



    public MapGenerator(long seed) {

        biomes = new HashMap<Integer, BiomeType>();
        biomeTextures = new HashMap<BiomeType, TextureRegion>();
        openSimplexNoise = new OpenSimplexNoise(seed);
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
                double noiseValue = openSimplexNoise.eval((x+xoffset) * FREQUENCY, (y+yoffset) * FREQUENCY, (x+xoffset + y+yoffset) * FREQUENCY);
                tileMap[x][y] = assignBiomeType(noiseValue);
            }
        }
        return fillinBiomeArray(tileMap, xoffset, yoffset);
    }

    private int[][] fillinBiomeArray(int[][] biomeArray,int xoffset, int yoffset) {
        // Define the possible replacement biomes for NONE
        BiomeType[] replacementBiomes = {BiomeType.DESSERT, BiomeType.WILDWEST, BiomeType.TUNDRA};
        Random random;

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (biomeArray[x][y] == BiomeType.NONE.ordinal()) {
                    // Choose a random replacement biome for this region
                    random = new Random(getBiomeSize(x + xoffset,y+ yoffset));
                    BiomeType randomBiome = replacementBiomes[random.nextInt(replacementBiomes.length)];
                    fillRegionIteratively(biomeArray, x, y, BiomeType.NONE.ordinal(), randomBiome.ordinal());
                }
            }
        }
        return biomeArray;
    }
    private int getBiomeSize(int startX, int startY) {
        int count = 0;
        Set<Vector2> visited = new HashSet<Vector2>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        Queue<int[]> queue = new LinkedList<int[]>();
        queue.add(new int[]{startX, startY});
        visited.add(new Vector2(startX, startY));

        while (!queue.isEmpty() && count < 10000) {
            int[] cell = queue.poll();
            int x = cell[0];
            int y = cell[1];

            if (assignBiomeType(openSimplexNoise.eval(x * FREQUENCY, y * FREQUENCY, (x + y) * FREQUENCY)) == BiomeType.NONE.ordinal()) {
                count++;

                for (int[] dir : directions) {
                    int newX = x + dir[0];
                    int newY = y + dir[1];
                    if (!visited.contains(new Vector2(newX,newY)) && assignBiomeType(openSimplexNoise.eval(newX * FREQUENCY, newY * FREQUENCY, (newX + newY) * FREQUENCY)) == BiomeType.NONE.ordinal()) {
                        queue.add(new int[]{newX, newY});
                        visited.add(new Vector2(newX, newY));
                    }
                }
            }
        }
        System.out.println(count + " " + startX + " " + startY + " " + count);
        return count;
    }

    private void fillRegionIteratively(int[][] biomeArray, int startX, int startY, int targetBiome, int replacementBiome) {
        // Define directions for 4-way connectivity (up, down, left, right)
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        // Use a queue to manage the cells to fill
        Queue<int[]> queue = new LinkedList<int[]>();
        queue.add(new int[]{startX, startY});
        biomeArray[startX][startY] = replacementBiome;

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0];
            int y = cell[1];

            // Check each of the four neighboring cells
            for (int[] direction : directions) {
                int newX = x + direction[0];
                int newY = y + direction[1];

                // Check boundaries and target biome condition
                if (newX >= 0 && newX < MAP_SIZE && newY >= 0 && newY < MAP_SIZE &&
                    biomeArray[newX][newY] == targetBiome) {
                    // Fill the cell and add it to the queue
                    biomeArray[newX][newY] = replacementBiome;
                    queue.add(new int[]{newX, newY});
                }
            }
        }
    }

    private int assignBiomeType(double noiseValue) {
        if (noiseValue < -0.6) {
            return BiomeType.OCEAN.ordinal();
        } else if (noiseValue < -0.10) {
            return BiomeType.NONE.ordinal();
        } else if (noiseValue < 0.5) {
            return BiomeType.PLAINS.ordinal();
        } else {
            return BiomeType.OCEAN.ordinal();
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
