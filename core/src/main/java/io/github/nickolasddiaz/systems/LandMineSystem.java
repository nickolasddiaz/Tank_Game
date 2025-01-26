package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.dongbat.jbump.Response;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.CollisionObject;


public class LandMineSystem extends IteratingSystem {
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<LandMineComponent> landMineMapper;
    private final ChunkComponent chunk;
    private final StatsComponent statsComponent;
    private final Engine engine;

    public LandMineSystem(Engine engine, ChunkComponent chunk, StatsComponent statsComponent) {
        super(Family.all(TransformComponent.class, LandMineComponent.class).get());
        this.engine = engine;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.landMineMapper = ComponentMapper.getFor(LandMineComponent.class);
        this.chunk = chunk;
        this.statsComponent = statsComponent;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        LandMineComponent landMine = landMineMapper.get(entity);

        if (chunk.world.getRect(transform.item) == null) {
            Rectangle rect = transform.item.userData.getBounds();
            chunk.world.add(transform.item, transform.position.x, transform.position.y, rect.width, rect.height);
        }

        // Check for collision without changing position
        chunk.world.move(transform.item, transform.position.x, transform.position.y, CollisionFilter);
    }

    private final com.dongbat.jbump.CollisionFilter CollisionFilter = (item, other) -> {
        if(other == null) {
            return null;
        }
        CollisionObject otherObject = (CollisionObject) other.userData;
        switch (otherObject.getObjectType()) {
            case "CAR":
            case "ENEMY":
                otherObject.health -= ((CollisionObject) item.userData).health;
                ((CollisionObject) item.userData).health = 0;

                if (otherObject.health <= 0 && otherObject.getObjectType().equals("ENEMY")) {
                    addScore();
                }
                break;

        }
        return Response.cross;
    };
    private void addScore(){
        statsComponent.addScore(1);
    }
}
