package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class PlayerComponent implements Component {
    public float SPEED = itemSize * 50f;
    public float spinSpeed = SPEED/16;
    public int enemyCount = 0;
}
