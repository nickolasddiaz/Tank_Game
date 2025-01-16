package io.github.nickolasddiaz.utils;

import com.badlogic.gdx.math.Vector2;

import java.util.*;

import static io.github.nickolasddiaz.utils.MapGenerator.*;

public class TerrainGenerator {
    class Cell {
        boolean collapsed;
        Boolean up;
        Boolean left;
        Boolean right;
        Boolean down;
        Options selectedOption; // Stores the collapsed option

        public Cell(Boolean top, Boolean left, Boolean right, Boolean bottom) {
            this.collapsed = false;
            this.up = top;
            this.left = left;
            this.right = right;
            this.down = bottom;
            this.selectedOption = null; // Initially not collapsed
        }

        // Collapse the cell to a single option
        public void collapse(int biome) {
            int multiplier;
            if (!collapsed) {
                List<Options> validOptions = getValidOptions();
                if(biome == TileType.DESSERT.ordinal()) multiplier = 3;
                else if(biome == TileType.WILD_WEST.ordinal()) multiplier = 2;
                else if(biome == TileType.TUNDRA.ordinal()) multiplier = 3;
                else if(biome == TileType.PLAINS.ordinal()) multiplier = 5;
                else if(biome == TileType.OCEAN.ordinal()) {
                    this.collapsed = true; this.up = false; this.left = false; this.right = false; this.down = false;
                    return;
                }
                else {
                    multiplier = 1;
                }

                if (validOptions.isEmpty()) {
                    return;
                    //throw new IllegalStateException("No valid options to collapse.");
                }

                // Weighted random selection
                int totalWeight = validOptions.stream().mapToInt(option -> {
                    if (option == Options.HORIZONTAL || option == Options.VERTICAL) {
                        return option.chance * multiplier;
                    }
                    return option.chance;
                }).sum();

                int randomWeight = random.nextInt(totalWeight);
                for (Options option : validOptions) {
                    int adjustedChance = option.chance;
                    if (option == Options.HORIZONTAL || option == Options.VERTICAL) {
                        adjustedChance *= multiplier;
                    }

                    randomWeight -= adjustedChance;
                    if (randomWeight < 0) {
                        this.selectedOption = option;
                        break;
                    }
                }

                // Update the cell to the collapsed state
                this.collapsed = true;
                this.up = selectedOption.up;
                this.left = selectedOption.left;
                this.right = selectedOption.right;
                this.down = selectedOption.down;
            }
        }

        // Get valid options based on current constraints
        private List<Options> getValidOptions() {
            List<Options> validOptions = new ArrayList<>();
            for (Options option : Options.values()) {
                if ((up == null || option.up.equals(up)) &&
                    (left == null || option.left.equals(left)) &&
                    (right == null || option.right.equals(right)) &&
                    (down == null || option.down.equals(down))) {
                    validOptions.add(option);
                }
            }
            return validOptions;
        }

        // Remove options by updating constraints
        public void removeOptions(Boolean up, Boolean left, Boolean right, Boolean down) {
            if (collapsed) {
                return;
            }

            // Update constraints based on provided values
            if (up != null) this.up = up;
            if (left != null) this.left = left;
            if (right != null) this.right = right;
            if (down != null) this.down = down;
        }

        public int getOptionCount() {
            return getValidOptions().size();
        }

        public Boolean isUp() { return up; }
        public Boolean isLeft() { return left; }
        public Boolean isRight() { return right; }
        public Boolean isDown() { return down; }
    }

    enum Options {
        CROSS(true, true, true, true, 1),               // ╬
        HORIZONTAL(false, true, true, false, 10),       // ═
        VERTICAL(true, false, false, true, 10),         // ║
        RIGHT_DOWN(false, false, true, true, 1),        // ╔
        LEFT_DOWN(false, true, false, true, 1),         // ╗
        RIGHT_UP(true, false, true, false, 1),          // ╚
        LEFT_UP(true, true, false, false, 1),           // ╝
        LEFT_UP_DOWN(true, true, false, true, 1),       // ╣
        RIGHT_UP_DOWN(true, false, true, true, 1),      // ╠
        LEFT_RIGHT_DOWN(false, true, true, true, 1),    // ╦
        LEFT_RIGHT_UP(true, true, true, false, 1);      // ╩

        public final Boolean up;
        public final Boolean left;
        public final Boolean right;
        public final Boolean down;
        public final int chance;

        // Constructor
        Options(Boolean up, Boolean left, Boolean right, Boolean down, int chance) {
            this.up = up;
            this.left = left;
            this.right = right;
            this.down = down;
            this.chance = chance;
        }
    }

    Queue<Vector2> Structure_Queue = new LinkedList<>();
    PriorityQueue<Pair<Integer, Vector2>> Queue = new PriorityQueue<>(Comparator.comparingInt(Pair::getKey));
    public static final int OCEAN = TileType.OCEAN.ordinal();
    public static final int ROAD = TileType.ROAD.ordinal();
    public static final int ROAD_CROSSWALK_WIDTH = TileType.ROAD_CROSSWALK_WIDTH.ordinal();
    public static final int ROAD_CROSSWALK_HEIGHT = TileType.ROAD_CROSSWALK_HEIGHT.ordinal();

    private final Random random;
    Cell[][] grid = new Cell[TILE_SIZE][TILE_SIZE];
    private boolean[] isRoadTop = new boolean[TILE_SIZE];
    private boolean[] isRoadBottom = new boolean[TILE_SIZE];


    public TerrainGenerator(int seed) {
        random = new Random(seed);
    }

    public Integer[][] generate(int[][] biomeMap, boolean[] top, boolean[] left, boolean[] right, boolean[] down) {
        isRoadTop = top;
        isRoadBottom = down;
        grid = new Cell[TILE_SIZE][TILE_SIZE];// Initialize grid with cells that have all possibilities
        Integer[][] TerrainMap = new Integer[MAP_SIZE][MAP_SIZE];
        for (int i = 0; i < TILE_SIZE; i++) { // TILE_SIZE = MAP_SIZE/ROAD_SIZE
            for (int j = 0; j < TILE_SIZE; j++) {
                grid[i][j] = new Cell(null, null, null, null);
            }
        }
        for(int i = 0; i < TILE_SIZE; i++){
            if(top[i]){
                roadUp(i, TILE_SIZE-1, TerrainMap, biomeMap);
                grid[i][TILE_SIZE-1].down = true;
                grid[i][TILE_SIZE-1].selectedOption = Options.VERTICAL;
                grid[i][TILE_SIZE-1].collapsed = true;
                insertRoad(i, TILE_SIZE-1, grid[i][TILE_SIZE-1].isUp(), grid[i][TILE_SIZE-1].isLeft(), grid[i][TILE_SIZE-1].isRight(), grid[i][TILE_SIZE-1].isDown(), TerrainMap, biomeMap);
                getNewCells(i, TILE_SIZE-1);
            }
            if(left[i]){
                roadLeft(0, i, TerrainMap, biomeMap); //make sure the border between the edges are filled
                grid[0][i].right = true;
                grid[0][i].selectedOption = Options.HORIZONTAL;
                grid[0][i].collapsed = true;
                insertRoad(0, i, grid[0][i].isUp(), grid[0][i].isLeft(), grid[0][i].isRight(), grid[0][i].isDown(), TerrainMap, biomeMap);
                getNewCells(0, i);
            }
            if(right[i]){
                roadRight(TILE_SIZE-1, i, TerrainMap, biomeMap); //make sure the border between the edges are filled
                grid[TILE_SIZE-1][i].left = true;
                grid[TILE_SIZE-1][i].selectedOption = Options.HORIZONTAL;
                grid[TILE_SIZE-1][i].collapsed = true;
                insertRoad(TILE_SIZE-1, i, grid[TILE_SIZE-1][i].isUp(), grid[TILE_SIZE-1][i].isLeft(), grid[TILE_SIZE-1][i].isRight(), grid[TILE_SIZE-1][i].isDown(), TerrainMap, biomeMap);
                getNewCells(TILE_SIZE-1, i);
            }
            if(down[i]){
                roadDown(i, 0, TerrainMap, biomeMap);
                grid[i][0].up = true;
                grid[i][0].selectedOption = Options.VERTICAL;
                grid[i][0].collapsed = true;
                insertRoad(i, 0, grid[i][0].isUp(), grid[i][0].isLeft(), grid[i][0].isRight(), grid[i][0].isDown(), TerrainMap, biomeMap);
                getNewCells(i, 0);
            }
        }

        while (!Queue.isEmpty()) {// collapsing cells
            Pair<Integer, Vector2> pair = Queue.poll();
            int x = (int) pair.getValue().x;
            int y = (int) pair.getValue().y;

            if (!grid[x][y].collapsed) {
                if (biomeMap[x * ROAD_SIZE + ROAD_SIZE/2][y * ROAD_SIZE + ROAD_SIZE/2] == OCEAN ) {
                    grid[x][y].collapse(OCEAN);
                } else {
                    grid[x][y].collapse(biomeMap[x * ROAD_SIZE + ROAD_SIZE / 2][y * ROAD_SIZE + ROAD_SIZE / 2]);
                    placeStructure(x * ROAD_SIZE, y * ROAD_SIZE, biomeMap, TerrainMap);
                    insertRoad(x, y, grid[x][y].isUp(), grid[x][y].isLeft(), grid[x][y].isRight(), grid[x][y].isDown(), TerrainMap, biomeMap);
                    getNewCells(x, y);
                    Structure_Queue.add(new Vector2(x, y));
                }
            }
        }
        while (!Structure_Queue.isEmpty()) {// placing structures
            Vector2 pair = Structure_Queue.poll();
            placeStructure((int) pair.x*ROAD_SIZE, (int) pair.y*ROAD_SIZE, biomeMap, TerrainMap);
        }

        for (int x = 0; x < MAP_SIZE; x++) {// placing decorations
            for (int y = 0; y < MAP_SIZE; y++) {
                if(TerrainMap[x][y] != null) continue;
                int biome = biomeMap[x][y];
                double densityLimit = random.nextDouble(); // random number between 0 and 1 meant for the density of decorations
                int addition = random.nextInt(3); // random number between 0 and 2 meant for the three options of each decoration for each biome
                switch (TileType.valueOf(TileType.values()[biome].name())) {
                    case DESSERT: if (densityLimit < DECORATION_DENSITY * 1) TerrainMap[x][y] = TileType.DESSERT_SHRUB.ordinal() + addition; break;
                    case WILD_WEST: if (densityLimit < DECORATION_DENSITY * 2) TerrainMap[x][y] = TileType.TUMBLEWEED.ordinal() + addition; break;
                    case TUNDRA: if (densityLimit < DECORATION_DENSITY * 2) TerrainMap[x][y] = TileType.ICICLE.ordinal() + addition; break;
                    case PLAINS: if (densityLimit < DECORATION_DENSITY * 4) TerrainMap[x][y] = TileType.PLAIN_TREE.ordinal() + addition; break;
                }
            }
        }

        return TerrainMap;
    }

    private void insertRoad(int x, int y, Boolean up, Boolean left, Boolean right, Boolean down, Integer[][] TerrainMap, int[][] BiomeMap){
        /*  01100110    01100110
            01101110 -> 01111110
            01101110 -> 01111110
            01100110    01100110 1 represents the road on if (checkIfOutOfBounds(x,y)){} fixes the problem*/

        if(up != null && up){ roadUp(x, y, TerrainMap, BiomeMap);
            if(checkIfOutOfBounds(x, y+1)) roadDown(x, y+1, TerrainMap, BiomeMap);
        }
        if(left != null && left){ roadLeft(x, y, TerrainMap, BiomeMap);
            if(checkIfOutOfBounds(x-1, y)) roadRight(x-1, y, TerrainMap, BiomeMap);
        }
        if(right != null && right){ roadRight(x, y, TerrainMap, BiomeMap);
            if(checkIfOutOfBounds(x+1, y)) roadLeft(x+1, y, TerrainMap, BiomeMap);
        }
        if(down != null && down){ roadDown(x, y, TerrainMap, BiomeMap);
            if(checkIfOutOfBounds(x, y-1))roadUp(x, y-1, TerrainMap, BiomeMap);
        }

    }
    private void roadUp(int x, int y, Integer[][] TerrainMap, int[][] BiomeMap){
        y *= ROAD_SIZE;
        if(y == MAP_SIZE-ROAD_SIZE && !isRoadTop[x]) return;
        x *= ROAD_SIZE;
        if(TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != null && TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != TileType.ROAD_RIGHT.ordinal()) {
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2 + 1] = ROAD_CROSSWALK_HEIGHT;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2 + 1] = ROAD_CROSSWALK_HEIGHT;
            if(TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2] != null){
                TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2] = ROAD_CROSSWALK_WIDTH;
                TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2 - 1] = ROAD_CROSSWALK_WIDTH;
            }
            if(TerrainMap[x + ROAD_SIZE / 2 + 1][y + ROAD_SIZE / 2] != null){
                TerrainMap[x + ROAD_SIZE / 2 + 1][y + ROAD_SIZE / 2] = ROAD_CROSSWALK_WIDTH;
                TerrainMap[x + ROAD_SIZE / 2 + 1][y + ROAD_SIZE / 2 - 1] = ROAD_CROSSWALK_WIDTH;
            }
        }
        for(int i = ROAD_SIZE-1; i > ROAD_SIZE/2-2 ; i--){
            if(BiomeMap[x+ROAD_SIZE/2][y+i] < OCEAN && BiomeMap[x+ROAD_SIZE/2-1][y+i] < OCEAN && TerrainMap[x+ROAD_SIZE/2][y+i] == null) {
                TerrainMap[x + ROAD_SIZE / 2][y + i] = TileType.ROAD_RIGHT.ordinal();
                TerrainMap[x + ROAD_SIZE / 2 - 1][y + i] = TileType.ROAD_LEFT.ordinal();
            }
        }
    }
    private void roadLeft(int x, int y, Integer[][] TerrainMap, int[][] BiomeMap){
        x *= ROAD_SIZE; y *= ROAD_SIZE;
        if(TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != null && TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != TileType.ROAD_TOP.ordinal()) {
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2] = ROAD_CROSSWALK_WIDTH;
            TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2 - 1] = ROAD_CROSSWALK_WIDTH;
            if(TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2-2] != null){
                TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2-2] = ROAD_CROSSWALK_HEIGHT;
                TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2-2] = ROAD_CROSSWALK_HEIGHT;
            }
            if(TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2+1] != null){
                TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2+1] = ROAD_CROSSWALK_HEIGHT;
                TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2+1] = ROAD_CROSSWALK_HEIGHT;
            }
        }
        for(int i = 0; i < ROAD_SIZE/2+1; i++){
            if(BiomeMap[x+i][y+ROAD_SIZE/2] < OCEAN && BiomeMap[x+i][y+ROAD_SIZE/2-1] < OCEAN && TerrainMap[x+i][y+ROAD_SIZE/2] == null) {
                TerrainMap[x + i][y + ROAD_SIZE / 2] = TileType.ROAD_TOP.ordinal();
                TerrainMap[x + i][y + ROAD_SIZE / 2 - 1] = TileType.ROAD_BOTTOM.ordinal();
            }
        }
    }
    private void roadRight(int x, int y, Integer[][] TerrainMap, int[][] BiomeMap){
        x *= ROAD_SIZE; y *= ROAD_SIZE;

        if(TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != null && TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != TileType.ROAD_TOP.ordinal()) {
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2+1][y + ROAD_SIZE / 2] = ROAD_CROSSWALK_WIDTH;
            TerrainMap[x + ROAD_SIZE / 2+1][y + ROAD_SIZE / 2 - 1] = ROAD_CROSSWALK_WIDTH;
            if(TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2-2] != null){
                TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2-2] = ROAD_CROSSWALK_HEIGHT;
                TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2-2] = ROAD_CROSSWALK_HEIGHT;
            }
            if(TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2+1] != null){
                TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2+1] = ROAD_CROSSWALK_HEIGHT;
                TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2+1] = ROAD_CROSSWALK_HEIGHT;
            }
        }
        for(int i = ROAD_SIZE-1; i > ROAD_SIZE/2-2 ; i--){
            if(BiomeMap[x+i][y+ROAD_SIZE/2] != OCEAN && BiomeMap[x+i][y+ROAD_SIZE/2-1] != OCEAN && TerrainMap[x+i][y+ROAD_SIZE/2] == null) {
                TerrainMap[x + i][y + ROAD_SIZE / 2] = TileType.ROAD_TOP.ordinal();
                TerrainMap[x + i][y + ROAD_SIZE / 2 - 1] = TileType.ROAD_BOTTOM.ordinal();
            }
        }


    }
    private void roadDown(int x, int y, Integer[][] TerrainMap, int[][] BiomeMap){
        if(y == 0 && !isRoadBottom[x]) return;
        x *= ROAD_SIZE; y *= ROAD_SIZE;
        if(TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != null && TerrainMap[x+ROAD_SIZE/2][y+ROAD_SIZE/2] != TileType.ROAD_RIGHT.ordinal()) {
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2 - 1] = ROAD;
            TerrainMap[x + ROAD_SIZE / 2][y + ROAD_SIZE / 2 - 2] = ROAD_CROSSWALK_HEIGHT;
            TerrainMap[x + ROAD_SIZE / 2 - 1][y + ROAD_SIZE / 2 - 2] = ROAD_CROSSWALK_HEIGHT;
            if(TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2] != null){
                TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2] = ROAD_CROSSWALK_WIDTH;
                TerrainMap[x + ROAD_SIZE / 2 - 2][y + ROAD_SIZE / 2 - 1] = ROAD_CROSSWALK_WIDTH;
            }
            if(TerrainMap[x + ROAD_SIZE / 2 + 1][y + ROAD_SIZE / 2] != null){
                TerrainMap[x + ROAD_SIZE / 2 + 1][y + ROAD_SIZE / 2] = ROAD_CROSSWALK_WIDTH;
                TerrainMap[x + ROAD_SIZE / 2 + 1][y + ROAD_SIZE / 2 - 1] = ROAD_CROSSWALK_WIDTH;
            }
        }
        for(int i = 0; i < ROAD_SIZE/2+1; i++){
            if(BiomeMap[x+ROAD_SIZE/2][y+i] < OCEAN && BiomeMap[x+ROAD_SIZE/2-1][y+i] < OCEAN && TerrainMap[x+ROAD_SIZE/2][y+i] == null) {
                TerrainMap[x + ROAD_SIZE / 2][y + i] = TileType.ROAD_RIGHT.ordinal();
                TerrainMap[x + ROAD_SIZE / 2 - 1][y + i] = TileType.ROAD_LEFT.ordinal();
            }
        }
    }

    private void getNewCells(int x, int y) {
        checkAndAddCell(x, y + 1, grid[x][y].isUp(), null, null, null, true);     // Up
        checkAndAddCell(x, y - 1, grid[x][y].isDown(), true, null, null, null);   // Down
        checkAndAddCell(x - 1, y, grid[x][y].isLeft(), null, null, true,null);   // Left
        checkAndAddCell(x + 1, y, grid[x][y].isRight(), null, true, null,null);  // Right
    }

    private void checkAndAddCell(int x, int y, Boolean sourceConnection, Boolean up, Boolean left, Boolean right, Boolean down) {
        if (checkIfOutOfBounds(x, y) && !grid[x][y].collapsed && sourceConnection != null && sourceConnection) {
            // Handle corner cases specifically
            if ((x == 0 && y == 0) || (x == 0 && y == TILE_SIZE - 1) ||
                (x == TILE_SIZE - 1 && y == 0) || (x == TILE_SIZE - 1 && y == TILE_SIZE - 1)) {
                // For corners, ensure at least two connections are possible
                grid[x][y].removeOptions(
                    (x == 0 && y == TILE_SIZE - 1) || (x == TILE_SIZE - 1 && y == TILE_SIZE - 1), // up
                    x == 0, // left
                    x == TILE_SIZE - 1, // right
                    (x == 0 && y == 0) || (x == TILE_SIZE - 1 && y == 0) // down
                );
                Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
                return;
            }

            // Handle edge cases (non-corner edges)
            if (x <= 1 || x >= TILE_SIZE - 2) {
                grid[x][y].removeOptions(
                    y >= TILE_SIZE / 2,
                    (x >= TILE_SIZE / 2) ? null : false,
                    (x >= TILE_SIZE / 2) ? false : null,
                    y <= TILE_SIZE / 2
                );
                Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
                return;
            }

            if (y <= 1 || y >= TILE_SIZE - 2) {
                grid[x][y].removeOptions(
                    (y >= TILE_SIZE / 2) ? false : null,
                    x <= TILE_SIZE / 2,
                    x >= TILE_SIZE / 2,
                    (y >= TILE_SIZE / 2) ? null : false
                );
                Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
                return;
            }

            // For interior cells
            grid[x][y].removeOptions(up, left, right, down);
            Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
        }
    }

    private void placeStructure(int x, int y, int[][] biomeMap, Integer[][] TerrainMap){
        if(random.nextInt(3) == 0) return; // one third chance of placing a structure
        if(y > MAP_SIZE -5) return;
        int start_x = x - ROAD_SIZE/4;
        if(start_x < 0) start_x = 0;
        int end_x = start_x + 5;
        int start_y = y + ROAD_SIZE/2 +1;
        int end_y = start_y + 3;

        if(TerrainMap[x+1][start_y-1] == null)  return; // if the structure is on a road
        int biome = biomeMap[x][y];
        if(biome >= OCEAN) return; // if the biome is a decoration or structure or terrain
        for(int i = start_x; i < end_x; i++){
            for(int j = start_y; j < end_y; j++){
                if(biomeMap[i][j] != biome) return; // if the structure is not in the same biome
            }
        }
        int structure;
        switch (TileType.valueOf(TileType.values()[biome].name())){
            case DESSERT: structure = TileType.DESSERT_BUILDING1.ordinal() + random.nextInt(3) * 12; break;
            case WILD_WEST: structure = TileType.WILD_WEST_BUILDING1.ordinal() + random.nextInt(3) * 12; break;
            case TUNDRA: structure = TileType.TUNDRA_BUILDING1.ordinal() + random.nextInt(3) * 12; break;
            default: structure = TileType.PLAINS_BUILDING1.ordinal() + random.nextInt(3) * 12; break;
        }

        for (int j = end_y - 1; j >= start_y; j--) {
            for (int i = start_x; i < end_x-1; i++) {
                TerrainMap[i][j] = structure;
                structure++;
            }
        }
        }

    private boolean checkIfOutOfBounds(int x, int y) {
        return x >= 0 && x < TILE_SIZE && y >= 0 && y < TILE_SIZE;
    }

    public static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public static <K, V> Pair<K, V> of(K key, V value) {
            return new Pair<>(key, value);
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}


