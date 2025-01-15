package io.github.nickolasddiaz.components;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class CollisionObject {
    private Rectangle rectangleObject;
    private Polygon polygonObject;
    private final boolean isPolygon;
    private final String objectType; // HORIZONTAL, VERTICAL, STRUCTURE, DECORATION, OCEAN, CAR, ENEMY, PLAYER, BULLET

    // Constructor for non-rotatable rectangle objects
    public CollisionObject(Rectangle rectangleObject, String objectType) {
        this.rectangleObject = rectangleObject;
        this.isPolygon = false;
        this.objectType = objectType;
    }

    // Constructor for rotatable polygon objects
    public CollisionObject(Polygon polygonObject, String objectType) {
        this.polygonObject = polygonObject;
        this.isPolygon = true;
        this.objectType = objectType;
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
    public Polygon getNonEncasedPolygon(){
        return polygonObject;
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
}
