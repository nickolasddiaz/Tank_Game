package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class BulletComponent implements Component {
    public float bullet_speed;

    public BulletComponent(float SPEED) {
        this.bullet_speed = SPEED;
    }

}
