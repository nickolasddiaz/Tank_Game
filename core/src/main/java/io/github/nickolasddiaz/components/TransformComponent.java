package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.World;

public class TransformComponent implements Component {
    public Vector2 position;
    public float rotation = 0f;
    public Sprite sprite;
    public Color color;
    public Vector2 bouncePosition = new Vector2();
    public Vector2 tempPosition = new Vector2();
    public boolean collided = false;
    public float tempRotation = 0f;
    public boolean slowDown = false;
    public Vector2 movement = new Vector2();
    public Item<CollisionObject> item;
    public World<CollisionObject> world;


    public TransformComponent(Sprite sprite, int width, int height, Color color, boolean isPolygon, String objectType, World<CollisionObject> world, Vector2 position, float rotation) {
        this.position = position;
        this.rotation = rotation;

        this.world = world;
        this.sprite = sprite;
        this.sprite.setSize(width, height);
        this.color = color;

        if(isPolygon) {
            Polygon polygonBounds = new Polygon(new float[] { // Create polygon with correct vertex ordering (clockwise)
                0, 0,  // bottom left
                width, 0,   // bottom right
                width, height,    // top right
                0, height    // top left
            });

            // Create AABB that encompasses the sprite at any rotation
            float maxDimension = (float) Math.sqrt(width * width + height * height);
            polygonBounds.setOrigin(width/2f, height/2f); // Set origin to center for proper rotation
            item = world.add(new Item<>(new CollisionObject(polygonBounds, objectType)), 0, 0,
                maxDimension/2 + (height-width),
                maxDimension/2f);
        } else {
            item = world.add(new Item<>(new CollisionObject(sprite.getBoundingRectangle(), objectType)), sprite.getX(), sprite.getY(), sprite.getWidth(), sprite.getHeight());
        }
    }

    public void updateBounds() {
        item.userData.updatePosition(position);
        item.userData.updateRotation(rotation);
        world.move(item, position.x, position.y, CollisionFilter.defaultFilter);
    }

    public void dispose(){
        world.remove(item);
    }

}
