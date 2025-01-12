package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.*;
import com.dongbat.jbump.Item;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.CollisionComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.ArrayList;

import static io.github.nickolasddiaz.systems.MapGenerator.itemSize;

public class CollisionSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<CollisionComponent> collisionMapper;
    private final ComponentMapper<ChunkComponent> chunkMapper;

    public CollisionSystem() {
        super(Family.all(TransformComponent.class, ChunkComponent.class, CollisionComponent.class).get());
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.collisionMapper = ComponentMapper.getFor(CollisionComponent.class);
        this.chunkMapper = ComponentMapper.getFor(ChunkComponent.class);

    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        CollisionComponent collide = collisionMapper.get(entity);
        ChunkComponent chunk = chunkMapper.get(entity);

        if(transform.collided){
            return;
        }

        // Update collision bounds to match current position
        ArrayList<Item> obstacles = chunk.getObjectsIsInsideRect(chunk.obstaclesFilter, collide.bounds, chunk.tileWorld); // RectangleMapObject
        ArrayList<Item> ocean = chunk.getObjectsIsInsideRect(chunk.oceanFilter, collide.bounds, chunk.oceanWorld); // PolygonMapObject

        collide.updateBounds(transform.position, transform.rotation);

        if(ocean != null){
            for(Item item : ocean){
                PolygonMapObject object = (PolygonMapObject) item.userData;
                if (checkCollision(object, collide)) {
                    setCollided(transform);
                    return;
                }
            }
        }
        if (obstacles != null) {
            Vector2 collisionPoint = getCollisionResponse(((RectangleMapObject) obstacles.get(0).userData).getRectangle(), collide.polygonBounds);
            if(collisionPoint == null) return;

            float angle = collisionPoint.angleDeg(transform.position);
            collisionPoint.add(new Vector2(itemSize*4, 0).setAngleDeg(angle));
            setCollided(transform, collisionPoint, angle);
        }
    }
    private void setCollided(TransformComponent transform){
        transform.tempPosition = transform.position.cpy();
        transform.tempRotation = transform.rotation;
        transform.bouncePosition = transform.position.add(new Vector2(itemSize*2, 0).setAngleDeg(transform.rotation - 180f));
        transform.collided = true;
    }

    private void setCollided(TransformComponent transform, Vector2 position, float rotation){
        transform.tempPosition = transform.position.cpy();
        transform.tempRotation = rotation;
        transform.bouncePosition = position;
        transform.collided = true;
    }

    private boolean checkCollision(PolygonMapObject object, CollisionComponent collide) {
        Polygon objectPolygon = object.getPolygon();
        // If rectangles overlap, do detailed polygon intersection test
        return Intersector.overlapConvexPolygons(collide.polygonBounds, objectPolygon);
    }

    public static Vector2 getCollisionResponse(Rectangle rect, Polygon poly) {
        // Convert rectangle to polygon for intersection test
        Polygon rectPoly = new Polygon(new float[] {
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        });

        // Check if shapes overlap
        if (!Intersector.overlapConvexPolygons(rectPoly, poly)) {
            return null;
        }

        // Get polygon center
        float[] vertices = poly.getTransformedVertices();
        Vector2 polyCenter = new Vector2();
        for (int i = 0; i < vertices.length; i += 2) {
            polyCenter.x += vertices[i];
            polyCenter.y += vertices[i + 1];
        }
        polyCenter.scl(1f / (vertices.length / 2f));

        // Get rectangle center
        Vector2 rectCenter = new Vector2(
            rect.x + rect.width / 2,
            rect.y + rect.height / 2
        );

        // Determine which side was hit by comparing centers
        Vector2 bounceDir = new Vector2();
        float dx = Math.abs(polyCenter.x - rectCenter.x);
        float dy = Math.abs(polyCenter.y - rectCenter.y);

        if (dx > dy) {
            // Horizontal collision
            if (polyCenter.x < rectCenter.x) {
                bounceDir.set(-1, 0); // Left collision
            } else {
                bounceDir.set(1, 0);  // Right collision
            }
        } else {
            // Vertical collision
            if (polyCenter.y < rectCenter.y) {
                bounceDir.set(0, -1); // Bottom collision
            } else {
                bounceDir.set(0, 1);  // Top collision
            }
        }

        // Scale bounce direction by desired distance
        return bounceDir.scl(itemSize*2);
    }

}

