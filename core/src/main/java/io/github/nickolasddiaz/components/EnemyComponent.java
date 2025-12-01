package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.utils.EntityStats;
import io.github.nickolasddiaz.utils.GraphNode;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class EnemyComponent implements Component {
    public EntityStats stats;

    public float enemyType;
    public float minDistance = 4f*itemSize;
    public GraphPath<GraphNode> path;
    public GraphPath<GraphNode> previousPath;
    public final Rectangle lazyPath = new Rectangle();
    public Vector2 nextPathWorld = new Vector2();
    public int pathIndex = 0;

    public float pathfindingCooldown = 0.5f; // Seconds between pathfinding attempts
    public float timeSinceLastPathfinding = 0f;


    public EnemyComponent(float enemyType, EntityStats stats) {
        this.enemyType = enemyType;
        this.path = new DefaultGraphPath<>();
        this.previousPath = null; // Will be set when first valid path is found
        this.stats = stats;
    }
}
