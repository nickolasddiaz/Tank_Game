package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Rectangle;

public class CollisionComponent implements Component {
    public Rectangle bounds = new Rectangle();
    public String collisionLayer; // e.g., "OBJECTS"
}

