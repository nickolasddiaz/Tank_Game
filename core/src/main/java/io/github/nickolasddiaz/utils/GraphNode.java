package io.github.nickolasddiaz.utils;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class GraphNode implements Connection<GraphNode> {
    public final Vector2 position;
    private final Array<Connection<GraphNode>> connections;

    public GraphNode(Vector2 position) {
        this.position = position;
        this.connections = new Array<>();
    }


    public void addConnection(Connection<GraphNode> connection) {
        connections.add(connection);
    }

    public Array<Connection<GraphNode>> getConnections() {
        return connections;
    }

    @Override
    public float getCost() {
        return position.dst(getToNode().getPosition());
    }

    private Vector2 getPosition() {
        return position;
    }

    @Override
    public GraphNode getFromNode() {
        return this;
    }

    @Override
    public GraphNode getToNode() {
        if (connections.size > 0) {
            return connections.get(0).getToNode();
        }
        return null;
    }
}
