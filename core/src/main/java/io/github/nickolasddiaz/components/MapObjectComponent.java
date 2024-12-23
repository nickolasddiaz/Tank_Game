package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Rectangle;

public class MapObjectComponent implements Component {
    public String type; // "ocean", "road", "obstacle", "house"
    public Rectangle bounds = new Rectangle();
}
