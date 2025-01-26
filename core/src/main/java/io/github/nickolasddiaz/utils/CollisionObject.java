package io.github.nickolasddiaz.utils;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class CollisionObject {
    private Rectangle rectangleObject;
    private Polygon polygonObject;
    private final boolean isPolygon;
    private final String objectType; // HORIZONTAL, VERTICAL, STRUCTURE, DECORATION, OCEAN, CAR, ENEMY, PLAYER, ALLY, P_BULLET, E_BULLET
    public int health = 10;

    // Constructor for non-rotatable rectangle objects
    public CollisionObject(Rectangle rectangleObject, String objectType, int health) {
        this.rectangleObject = rectangleObject;
        this.isPolygon = false;
        this.objectType = objectType;
        this.health = health;
    }

    // Constructor for rotatable polygon objects
    public CollisionObject(Polygon polygonObject, String objectType, int health) {
        this.polygonObject = polygonObject;
        this.isPolygon = true;
        this.objectType = objectType;
        this.health = health;
    }

    public boolean isisPolygon() {
        return isPolygon;
    }

    public String getObjectType() {
        return objectType;
    }

    public Rectangle getBounds() {
        if (isPolygon) {
            return polygonObject.getBoundingRectangle();
        }
        return rectangleObject;
    }

    public Polygon getPolygon() {
        if (isPolygon) {
            return polygonObject;
        }
        // Convert rectangle to polygon if needed
        Rectangle rect = rectangleObject;
        return new Polygon(new float[]{
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        });
    }

    public void updatePosition(Vector2 position) {
        if (isPolygon) {
            polygonObject.setPosition(position.x, position.y);
        } else {
            rectangleObject.setPosition(position.x, position.y);
        }
    }

    public void updateRotation(float rotation) {
        if (isPolygon) {
            polygonObject.setRotation(rotation);
        }
    }

    public Vector2 getPosition() {
        if (isPolygon) {
            return new Vector2(polygonObject.getX(), polygonObject.getY());
        }
        return new Vector2(rectangleObject.x, rectangleObject.y);
    }
}
