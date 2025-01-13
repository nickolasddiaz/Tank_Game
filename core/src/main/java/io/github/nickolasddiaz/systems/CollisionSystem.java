package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.*;
import com.dongbat.jbump.Item;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.CollisionComponent;
import io.github.nickolasddiaz.components.SettingsComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.ArrayList;
import java.util.Objects;

import static io.github.nickolasddiaz.systems.MapGenerator.itemSize;

public class CollisionSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<CollisionComponent> collisionMapper;
    private final ComponentMapper<ChunkComponent> chunkMapper;
    private final ComponentMapper<SettingsComponent> settingsMapper;

    public CollisionSystem() {
        super(Family.all(TransformComponent.class, ChunkComponent.class, CollisionComponent.class,SettingsComponent.class).get());
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.collisionMapper = ComponentMapper.getFor(CollisionComponent.class);
        this.chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
        this.settingsMapper = ComponentMapper.getFor(SettingsComponent.class);

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
        transform.slowDown = false;

        if(settingsMapper.get(entity).DEBUG) {
            debugCollision(collide.bounds,collide.polygonBounds, chunk.shapeRenderer);
        }

        if(ocean != null){
           Polygon object = ((PolygonMapObject) ocean.get(0).userData).getPolygon();
           if (Intersector.overlapConvexPolygons(object, collide.polygonBounds)) {
                Gdx.app.log("CollisionSystem", "Ocean Collision");
                float collisionAngle = chunk.getAngleFromPoint(object, collide.bounds);
                setCollided(transform, collisionAngle);
                return;

            }
        }
        if (obstacles != null) {
            RectangleMapObject temp = (RectangleMapObject) obstacles.get(0).userData;
            if(!Intersector.overlapConvexPolygons(collide.polygonBounds, chunk.rectangletoPolygon(temp.getRectangle()))){
                return;
            }

            if(Objects.equals(temp.getName(), "DECORATION")){
                transform.slowDown = true;
            }else {
                float collisionAngle = chunk.getAngleFromPoint(collide.polygonBounds, temp.getRectangle());
                setCollided(transform, collisionAngle);
            }


        }
    }

    private void setCollided(TransformComponent transform, float rotation){
        transform.tempPosition = transform.position.cpy();
        transform.tempRotation = rotation;
        transform.bouncePosition = transform.position.add(new Vector2(itemSize*1.5f, 0).setAngleDeg(rotation -180f));
        transform.collided = true;
    }

    public void debugCollision(Rectangle bounds, Polygon polygonBounds, ShapeRenderer shapeRenderer) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.polygon(polygonBounds.getTransformedVertices());
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapeRenderer.end();
    }

}

