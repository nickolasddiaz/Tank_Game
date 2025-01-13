package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.World;

public class CollisionComponent extends EntitySystem implements Component {
    public Rectangle bounds;
    public Polygon polygonBounds;
    public Item<Rectangle> item;
    private float width;
    private float height;

    public CollisionComponent(float x, float y, float width, float height, Rectangle bounds, World world) {
        this.width = width;
        this.height = height;
        this.bounds = bounds;

        // Create polygon with correct vertex ordering (clockwise)
        float[] vertices = new float[] {
            0, 0,  // bottom left
            width, 0,   // bottom right
            width, height,    // top right
            0, height    // top left
        };

        polygonBounds = new Polygon(vertices);
        // Set origin to center for proper rotation
        polygonBounds.setOrigin(width/2, height/2);
        // Set initial position
        polygonBounds.setPosition(x, y);

        // Create AABB that encompasses the sprite at any rotation
        float maxDimension = (float) Math.sqrt(width * width + height * height);
        this.bounds = new Rectangle(
            0,
            0,
            maxDimension,
            maxDimension
        );
        bounds.width -= maxDimension/2 - (height-width);
        bounds.height -= maxDimension/2;

        item = world.add(new Item<>(this.bounds), x, y, maxDimension, maxDimension);
    }

    public void updateBounds(Vector2 position, float rotation) {
        // Update polygon position and rotation
        polygonBounds.setPosition(position.x, position.y);
        polygonBounds.setRotation(rotation);

        // Update AABB to maintain encapsulation of rotated sprite
        float maxDimension = (float) Math.sqrt(width * width + height * height);
        bounds.setPosition(
            position.x,
            position.y - maxDimension/4
        );

    }

    public void dispose(World world) {
        if (item != null) {
            world.remove(item);
        }
    }
}
