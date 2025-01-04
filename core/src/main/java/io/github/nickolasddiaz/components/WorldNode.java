package io.github.nickolasddiaz.components;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class WorldNode {
    public final Vector2 position;
    private final Array<Connection<WorldNode>> connections;

    public WorldNode(Vector2 position) {
        this.position = position;
        this.connections = new Array<>();
    }

    public void addConnection(Connection<WorldNode> connection) {
        connections.add(connection);
    }

    public Array<Connection<WorldNode>> getConnections() {
        return connections;
    }
}
