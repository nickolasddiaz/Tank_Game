package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;

public class EnemyComponent implements Component {
    public float health = 100;
    public float damage = 10;
    public float speed = 100;
    public float enemyType = 0;
    public float lowestDistance = 100;

    public EnemyComponent(float enemyType, float health, float damage, float speed, float multiplier) {
        this.enemyType = enemyType;
        this.health = health * multiplier;
        this.damage = damage * multiplier;
        this.speed = speed * multiplier;
    }
}
