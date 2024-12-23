package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

public class JoystickComponent implements Component {
    public Vector2 stickPositionMovement = new Vector2();
    public Circle joyStickBaseCircle = new Circle();
    public Circle joyStickTouchCircle = new Circle();
}

