package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.*;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.CollisionObject;
import io.github.nickolasddiaz.components.SettingsComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.ArrayList;
import java.util.Objects;

import static io.github.nickolasddiaz.systems.MapGenerator.itemSize;

public class CollisionSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<ChunkComponent> chunkMapper;

    public CollisionSystem() {
        super(Family.all(TransformComponent.class, ChunkComponent.class,SettingsComponent.class).get());
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        ChunkComponent chunk = chunkMapper.get(entity);

        if(transform.collided){
            return;
        }

        // Update collision bounds to match current position
        ArrayList<Item> obstacles = chunk.getObjectsIsInsideRect(CollisionFilter.defaultFilter, transform.item.userData.getBounds(), chunk.world);

        transform.slowDown = false;

        if (obstacles != null) {
            CollisionObject object = ((CollisionObject) obstacles.get(0).userData);
            if(!Intersector.overlapConvexPolygons(transform.item.userData.getPolygon(), object.getPolygon())){
                return;
            }
            if(Objects.equals(object.getObjectType(), "OCEAN")){
                if (Intersector.overlapConvexPolygons(object.getNonEncasedPolygon(), transform.item.userData.getPolygon())) {
                    float collisionAngle = chunk.getAngleFromPoint(object.getNonEncasedPolygon(), transform.item.userData.getBounds());
                    setCollided(transform, collisionAngle);
                    return;
                }
            }

            if(Objects.equals(object.getObjectType(), "DECORATION")){
                transform.slowDown = true;
            }else if(Objects.equals(object.getObjectType(), "ENEMY") || Objects.equals(object.getObjectType(), "PLAYER")|| Objects.equals(object.getObjectType(), "STRUCTURE")){
                float collisionAngle = chunk.getAngleFromPoint(transform.item.userData.getPolygon(), object.getBounds());
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

}

