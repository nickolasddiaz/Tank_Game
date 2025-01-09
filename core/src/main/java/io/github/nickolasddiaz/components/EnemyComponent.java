package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class EnemyComponent implements Component {
    public float health = 100;
    public float damage = 10;
    public float speed = 100;
    public float enemyType = 0;
    public float lowestDistance = 100;
    public float spinSpeed = speed/16;
    public GraphPath<GraphNode> path;
    public final Rectangle lazyPath = new Rectangle();
    public final Vector2 nextPath = new Vector2();
    public Vector2 nextPathWorld = new Vector2();
    public final Rectangle nextPathRect = new Rectangle();
    public int pathIndex = 0;

    public EnemyComponent(float enemyType, float health, float damage, float speed, float multiplier) {
        this.enemyType = enemyType;
        this.health = health * multiplier;
        this.damage = damage * multiplier;
        this.speed = speed * multiplier *5;
        this.spinSpeed = this.speed/2;
        this.path = new DefaultGraphPath<>();
    }
}
