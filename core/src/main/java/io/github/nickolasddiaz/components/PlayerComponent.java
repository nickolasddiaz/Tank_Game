package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import io.github.nickolasddiaz.utils.EntityStats;

public class PlayerComponent implements Component {
    public EntityStats stats;

    public PlayerComponent(EntityStats stats) {
        this.stats = stats;
    }


}
