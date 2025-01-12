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

    public CollisionComponent(float width, float height, World world) {
        polygonBounds = new Polygon(new float[]{0,0,width,0,width,height,0,height});
        bounds = polygonBounds.getBoundingRectangle();
        Item<Rectangle> item = world.add(new Item<Rectangle>(bounds), bounds.x, bounds.y, bounds.width, bounds.height);

    }

    public void updateBounds(Vector2 position, float rotation) {
        this.bounds.setPosition(position);
        this.polygonBounds.setPosition(position.x, position.y);
        this.polygonBounds.setRotation(rotation);
    }

    public void dispose(World world) {
        world.remove(this.item);
    }


}
