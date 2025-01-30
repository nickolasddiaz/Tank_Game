package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.utils.GraphNode;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class EnemyComponent implements Component {
    public float health;
    public float speed;
    public float enemyType;
    public float minDistance = 6f*itemSize;
    public float spinSpeed;
    public GraphPath<GraphNode> path;
    public GraphPath<GraphNode> previousPath;
    public final Rectangle lazyPath = new Rectangle();
    public final Vector2 nextPath = new Vector2();
    public Vector2 nextPathWorld = new Vector2();
    public final Rectangle nextPathRect = new Rectangle();
    public int pathIndex = 0;

    public float fireRate;
    public float timeSinceLastShot = 0f;
    public float bulletSpeed;
    public int bulletDamage;

    public float pathfindingCooldown = 0.5f; // Seconds between pathfinding attempts
    public float timeSinceLastPathfinding = 0f;

    public boolean isAlly = false;

    public EnemyComponent(float enemyType, float health, float speed, float fireRate, float bulletSpeed, int bulletDamage, boolean isAlly) {
        this.enemyType = enemyType;
        this.health = health;
        this.speed = speed * itemSize;
        this.spinSpeed = this.speed/2f;
        this.path = new DefaultGraphPath<>();
        this.previousPath = null; // Will be set when first valid path is found
        this.fireRate = fireRate;
        this.bulletSpeed = bulletSpeed;
        this.bulletDamage = bulletDamage;
        this.isAlly = isAlly;
    }
}
