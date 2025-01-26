package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;

public class LandMineComponent implements Component {
    public int damage;

    public LandMineComponent(int damage) {
        this.damage = damage;
    }
}
