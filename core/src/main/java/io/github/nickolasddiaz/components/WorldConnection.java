package io.github.nickolasddiaz.components;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Vector2;

public class WorldConnection implements Connection<WorldNode> {
    private final WorldNode fromNode;
    private final WorldNode toNode;
    private final float cost;

    public WorldConnection(WorldNode fromNode, WorldNode toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.cost = Vector2.dst(
            fromNode.position.x, fromNode.position.y,
            toNode.position.x, toNode.position.y
        );
    }

    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public WorldNode getFromNode() {
        return fromNode;
    }

    @Override
    public WorldNode getToNode() {
        return toNode;
    }
}
