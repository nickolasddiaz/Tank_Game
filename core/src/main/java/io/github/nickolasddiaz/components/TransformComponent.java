package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class TransformComponent implements Component {
    public Vector2 position = new Vector2();
    public float rotation = 0f;
    public Sprite sprite;
    public Color color = null;
    public Vector2 bouncePosition = new Vector2();
    public Vector2 tempPosition = new Vector2();
    public boolean collided = false;
    public float tempRotation = 0f;
    public boolean slowDown = false;
    public Vector2 movement = new Vector2();

    public void updateSprite(Sprite sprite, int width, int height, Vector2 position, Color color, float rotation) {
        this.sprite = sprite;
        this.sprite.setSize(width, height);
        this.position = position;
        this.color = color;
        this.rotation = rotation;
    }

}
