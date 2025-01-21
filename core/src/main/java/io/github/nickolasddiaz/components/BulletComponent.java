package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class BulletComponent implements Component {
    public float bullet_speed = itemSize * 10f;
    public int damage = 1;

    public BulletComponent(float SPEED, int damage) {
        this.bullet_speed = SPEED;
        this.damage = damage;
    }

}
