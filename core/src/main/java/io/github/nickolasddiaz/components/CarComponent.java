package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

import static io.github.nickolasddiaz.systems.MapGenerator.MAP_SIZE;

public class CarComponent implements Component, Pool.Poolable {
    public float speed = MAP_SIZE; // 80
    public boolean direction = false; // up/right or down/left
    public boolean horizontal = false; // horizontal/vertical
    public float changeDirection = 0;

    public CarComponent(boolean direction, float changeDirection) {
        this.direction = direction;
        this.changeDirection = changeDirection;
    }

    @Override
    public void reset() {
        speed = 50f;
        direction = false;
        horizontal = false;
        changeDirection = 0;
    }
}
