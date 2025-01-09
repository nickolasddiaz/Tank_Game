package io.github.nickolasddiaz.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import static io.github.nickolasddiaz.systems.MapGenerator.ALL_CHUNK_SIZE; // unit of three chunk length 240
import static io.github.nickolasddiaz.systems.MapGenerator.MAP_SIZE; // unit of one tile length

public class WorldGraph implements IndexedGraph<GraphNode> {
    private final GraphNode[][] nodes;
    private final Vector2 chunkPosition;

    public WorldGraph(boolean[][] walkableGrid, Vector2 chunkPosition) {
        this.nodes = new GraphNode[ALL_CHUNK_SIZE][ALL_CHUNK_SIZE];
        this.chunkPosition = chunkPosition;

        // Create nodes for walkable areas
        for (int x = 0; x < ALL_CHUNK_SIZE; x++) {
            for (int y = 0; y < ALL_CHUNK_SIZE; y++) {
                if (walkableGrid[x][y]) {
                    nodes[x][y] = new GraphNode(new Vector2(x, y));
                }
            }
        }

        // Create connections between nodes
        for (int x = 0; x < ALL_CHUNK_SIZE; x++) {
            for (int y = 0; y < ALL_CHUNK_SIZE; y++) {
                if (nodes[x][y] != null) {
                    createNodeConnections(x, y);
                }
            }
        }
    }

    private void createNodeConnections(int x, int y) {
        // Define possible movement directions (8-way movement)
        int[][] directions = {
            {0, 1}, {1, 0}, {0, -1}, {-1, 0},  // Cardinal
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // Diagonal
        };

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];

            if (isValidPosition(newX, newY) && nodes[newX][newY] != null) {
                GraphNode fromNode = nodes[x][y];
                GraphNode toNode = nodes[newX][newY];
                fromNode.addConnection(new DefaultConnection<>(fromNode, toNode));
            }
        }
    }
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < ALL_CHUNK_SIZE && y >= 0 && y < ALL_CHUNK_SIZE;
    }

    public GraphNode getNodeAt(Vector2 pos) {
        int x = (int) pos.x;
        int y = (int) pos.y;

        if (!isValidPosition(x, y)) {
            Gdx.app.log("WorldGraph", "Invalid position: " + pos);
            return null;
        }

        return nodes[x][y];
    }

    @Override
    public Array<Connection<GraphNode>> getConnections(GraphNode fromNode) {
        return fromNode.getConnections();
    }

    @Override
    public int getIndex(GraphNode node) {
        // Ensure the index stays within bounds by using modulo
        int x = (int) node.position.x % ALL_CHUNK_SIZE;
        int y = (int) node.position.y % ALL_CHUNK_SIZE;
        return x * ALL_CHUNK_SIZE + y;
    }

    @Override
    public int getNodeCount() {
        return ALL_CHUNK_SIZE * ALL_CHUNK_SIZE;  // Total number of possible nodes
    }

    public GraphNode getNodeAt(float x, float y) {
        return nodes[(int) x][(int) y];
    }

}
