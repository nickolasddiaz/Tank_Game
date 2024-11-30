package io.github.nickolasddiaz;

import com.badlogic.gdx.math.Vector2;

import java.util.*;

import static io.github.nickolasddiaz.MapGenerator.*;

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
                if(biome == BiomeType.DESSERT.ordinal()) multiplier = 3;
                else if(biome == BiomeType.WILD_WEST.ordinal()) multiplier = 2;
                else if(biome == BiomeType.TUNDRA.ordinal()) multiplier = 3;
                else if(biome == BiomeType.PLAINS.ordinal()) multiplier = 5;
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
        CROSS(true, true, true, true, 1),            // ╬
        HORIZONTAL(false, true, true, false, 10),     // ═
        VERTICAL(true, false, false, true, 10),       // ║
        RIGHT_DOWN(false, false, true, true, 1),     // ╔
        LEFT_DOWN(false, true, false, true, 1),      // ╗
        RIGHT_UP(true, false, true, false, 1),       // ╚
        LEFT_UP(true, true, false, false, 1),        // ╝
        LEFT_UP_DOWN(true, true, false, true, 1),    // ╣
        RIGHT_UP_DOWN(true, false, true, true, 1),   // ╠
        LEFT_RIGHT_DOWN(false, true, true, true, 1), // ╦
        LEFT_RIGHT_UP(true, true, true, false, 1);   // ╩

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

    PriorityQueue<Pair<Integer, Vector2>> Queue = new PriorityQueue<>(Comparator.comparingInt(Pair::getKey));
    private static final int ROAD = (int) BiomeType.OCEAN.ordinal();
    private final Random random;
    Cell[][] grid = new Cell[TERRAIN_SIZE][TERRAIN_SIZE];

    public TerrainGenerator(int seed) {
        random = new Random(seed);
    }

    public int[][] generate(int[][] biomeMap, boolean[] top, boolean[] left, boolean[] right, boolean[] down) {
        grid = new Cell[TERRAIN_SIZE][TERRAIN_SIZE];
        // Initialize grid with cells that have all possibilities
        for (int i = 0; i < TERRAIN_SIZE; i++) { // TERRAIN_SIZE = MAP_SIZE/ROAD_SIZE
            for (int j = 0; j < TERRAIN_SIZE; j++) {
                grid[i][j] = new Cell(null, null, null, null);
            }
        }
        // Create initial random points
        for(int i = 0; i < TERRAIN_SIZE; i++){
            if(top[i]){
                biomeUp(i, TERRAIN_SIZE-1, biomeMap);
                grid[i][TERRAIN_SIZE-1].down = true;
                grid[i][TERRAIN_SIZE-1].selectedOption = Options.VERTICAL;
                grid[i][TERRAIN_SIZE-1].collapsed = true;
                insertRoad(i, TERRAIN_SIZE-1, grid[i][TERRAIN_SIZE-1].isUp(), grid[i][TERRAIN_SIZE-1].isLeft(), grid[i][TERRAIN_SIZE-1].isRight(), grid[i][TERRAIN_SIZE-1].isDown(), biomeMap);
                getNewCells(i, TERRAIN_SIZE-1);
            }
            if(left[i]){
                biomeLeft(0, i, biomeMap); //this line is to make sure the border between the edges are filled
                grid[0][i].right = true;
                grid[0][i].selectedOption = Options.HORIZONTAL;
                grid[0][i].collapsed = true;
                insertRoad(0, i, grid[0][i].isUp(), grid[0][i].isLeft(), grid[0][i].isRight(), grid[0][i].isDown(), biomeMap);
                getNewCells(0, i);
            }
            if(right[i]){
                biomeRight(TERRAIN_SIZE-1, i, biomeMap); //this line is to make sure the border between the edges are filled
                grid[TERRAIN_SIZE-1][i].left = true;
                grid[TERRAIN_SIZE-1][i].selectedOption = Options.HORIZONTAL;
                grid[TERRAIN_SIZE-1][i].collapsed = true;
                insertRoad(TERRAIN_SIZE-1, i, grid[TERRAIN_SIZE-1][i].isUp(), grid[TERRAIN_SIZE-1][i].isLeft(), grid[TERRAIN_SIZE-1][i].isRight(), grid[TERRAIN_SIZE-1][i].isDown(), biomeMap);
                getNewCells(TERRAIN_SIZE-1, i);
            }
            if(down[i]){
                biomeDown(i, 0, biomeMap);
                grid[i][0].up = true;
                grid[i][0].selectedOption = Options.VERTICAL;
                grid[i][0].collapsed = true;
                insertRoad(i, 0, grid[i][0].isUp(), grid[i][0].isLeft(), grid[i][0].isRight(), grid[i][0].isDown(), biomeMap);
                getNewCells(i, 0);
            }
        }

        // Process the queue until empty
        while (!Queue.isEmpty()) {
            Pair<Integer, Vector2> pair = Queue.poll();
            int x = (int) pair.getValue().x;
            int y = (int) pair.getValue().y;

            if (!grid[x][y].collapsed) {
                grid[x][y].collapse(biomeMap[x*ROAD_SIZE+ROAD_SIZE/2][y*ROAD_SIZE+ROAD_SIZE/2]);
                insertRoad(x, y, grid[x][y].isUp(), grid[x][y].isLeft(), grid[x][y].isRight(), grid[x][y].isDown(), biomeMap);
                getNewCells(x, y);
            }
        }

        return biomeMap;
    }

    private void insertRoad(int x, int y, Boolean up, Boolean left, Boolean right, Boolean down, int[][] biomeMap){
        /*  01100110    01100110
            01101110 -> 01111110
            01101110 -> 01111110
            01100110    01100110 1 represents the road if (checkIfOutOfBounds(x,y)) fixes the problem*/

        if(up != null && up){
            biomeUp(x, y, biomeMap);
            if(checkIfOutOfBounds(x, y+1)){
                biomeDown(x, y+1, biomeMap);
            }
        }
        if(left != null && left){
            biomeLeft(x, y, biomeMap);
            if(checkIfOutOfBounds(x-1, y)){
                biomeRight(x-1, y, biomeMap);
            }
        }
        if(right != null && right){
            biomeRight(x, y, biomeMap);
            if(checkIfOutOfBounds(x+1, y)){
                biomeLeft(x+1, y, biomeMap);
            }
        }
        if(down != null && down){
            biomeDown(x, y, biomeMap);
            if(checkIfOutOfBounds(x, y-1)){
                biomeUp(x, y-1, biomeMap);
            }
        }

    }
    private void biomeUp(int x, int y, int[][] biomeMap){
        x *= ROAD_SIZE; y *= ROAD_SIZE;
        for(int i = ROAD_SIZE-1; i > ROAD_SIZE/2-2 ; i--){
            biomeMap[x+ROAD_SIZE/2][y+i] =  ROAD;
            biomeMap[x+ROAD_SIZE/2-1][y+i] =  ROAD;
        }
    }
    private void biomeLeft(int x, int y, int[][] biomeMap){
        x *= ROAD_SIZE; y *= ROAD_SIZE;
        for(int i = 0; i < ROAD_SIZE/2+1; i++){
            biomeMap[x+i][y+ROAD_SIZE/2] =  ROAD;
            biomeMap[x+i][y+ROAD_SIZE/2-1] =  ROAD;
        }
    }
    private void biomeRight(int x, int y, int[][] biomeMap){
        x *= ROAD_SIZE; y *= ROAD_SIZE;
        for(int i = ROAD_SIZE-1; i > ROAD_SIZE/2-2 ; i--){
            biomeMap[x+i][y+ROAD_SIZE/2] =  ROAD;
            biomeMap[x+i][y+ROAD_SIZE/2-1] =  ROAD;
        }
    }
    private void biomeDown(int x, int y, int[][] biomeMap){
        x *= ROAD_SIZE; y *= ROAD_SIZE;
        for(int i = 0; i < ROAD_SIZE/2+1; i++){
            biomeMap[x+ROAD_SIZE/2][y+i] = ROAD;
            biomeMap[x+ROAD_SIZE/2-1][y+i] = ROAD;
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
            if ((x == 0 && y == 0) || (x == 0 && y == TERRAIN_SIZE - 1) ||
                (x == TERRAIN_SIZE - 1 && y == 0) || (x == TERRAIN_SIZE - 1 && y == TERRAIN_SIZE - 1)) {
                // For corners, ensure at least two connections are possible
                grid[x][y].removeOptions(
                    (x == 0 && y == TERRAIN_SIZE - 1) || (x == TERRAIN_SIZE - 1 && y == TERRAIN_SIZE - 1), // up
                    (x == 0 && y == 0) || (x == 0 && y == TERRAIN_SIZE - 1), // left
                    (x == TERRAIN_SIZE - 1 && y == 0) || (x == TERRAIN_SIZE - 1 && y == TERRAIN_SIZE - 1), // right
                    (x == 0 && y == 0) || (x == TERRAIN_SIZE - 1 && y == 0) // down
                );
                Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
                return;
            }

            // Handle edge cases (non-corner edges)
            if (x <= 1 || x >= TERRAIN_SIZE - 2) {
                grid[x][y].removeOptions(
                    y >= TERRAIN_SIZE / 2,
                    (x >= TERRAIN_SIZE / 2) ? null : false,
                    (x >= TERRAIN_SIZE / 2) ? false : null,
                    y <= TERRAIN_SIZE / 2
                );
                Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
                return;
            }

            if (y <= 1 || y >= TERRAIN_SIZE - 2) {
                grid[x][y].removeOptions(
                    (y >= TERRAIN_SIZE / 2) ? false : null,
                    x <= TERRAIN_SIZE / 2,
                    x >= TERRAIN_SIZE / 2,
                    (y >= TERRAIN_SIZE / 2) ? null : false
                );
                Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
                return;
            }

            // For interior cells
            grid[x][y].removeOptions(up, left, right, down);
            Queue.add(Pair.of(grid[x][y].getOptionCount(), new Vector2(x, y)));
        }
    }

    private boolean checkIfOutOfBounds(int x, int y) {
        return x >= 0 && x < TERRAIN_SIZE && y >= 0 && y < TERRAIN_SIZE;
    }
}
