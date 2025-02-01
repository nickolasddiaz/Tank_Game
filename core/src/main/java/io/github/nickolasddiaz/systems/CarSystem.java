package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.CollisionCategory.HORIZONTAL_ROAD;
import static io.github.nickolasddiaz.utils.CollisionCategory.VERTICAL_ROAD;
import static io.github.nickolasddiaz.utils.MapGenerator.*;

public class CarSystem extends IteratingSystem {
    private final ComponentMapper<CarComponent> carMapper;
    private final ChunkComponent chunk;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final Engine engine;

    public CarSystem(Engine engine, ChunkComponent chunk) {
        super(Family.all(CarComponent.class, TransformComponent.class).get());
        this.engine = engine;
        this.chunk = chunk;
        carMapper = ComponentMapper.getFor(CarComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CarComponent car = carMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);
        Body body = transform.body;

        // Check if in a valid chunk
        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.getPosition()))) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }
        if(body == null) return;
        transform.velocity.setZero();

        // Update position from Box2D body
        transform.rotation = ((car.horizontal) ?
            (car.direction) ? 0 : 180 :
            (car.direction) ? 90 : 270);

        if (car.horizontal) {
            if (car.direction) {
                if (car.changeDirection > transform.getPosition().x) {
                    transform.velocity.set(car.speed, 0);
                    return;
                }
            } else {
                if (car.changeDirection < transform.getPosition().x) {
                    transform.velocity.set(-car.speed, 0);
                    return;
                }
            }
        } else {
            if (car.direction) {
                if (car.changeDirection > transform.getPosition().y) {
                    transform.velocity.set(0, car.speed);
                    return;
                }
            } else {
                if (car.changeDirection < transform.getPosition().y) {
                    transform.velocity.set(0, -car.speed);
                    return;
                }
            }
        }

        // Query for roads using Box2D
        final Rectangle[] horizontalRoad = {null};
        final Rectangle[] verticalRoad = {null};

        float querySize = MAP_SIZE;
        chunk.world.QueryAABB(
            fixture -> {
                if (fixture.getFilterData().categoryBits == HORIZONTAL_ROAD) {
                    Body roadBody = fixture.getBody();
                    Vector2 pos = roadBody.getPosition();
                    horizontalRoad[0] = new Rectangle(
                        pos.x - querySize/2, pos.y - querySize/2,
                        querySize, querySize
                    );
                }
                return true;
            },
            transform.getPosition().x - querySize/2,
            transform.getPosition().y - querySize/2,
            transform.getPosition().x + querySize/2,
            transform.getPosition().y + querySize/2
        );

        chunk.world.QueryAABB(
            fixture -> {
                if (fixture.getFilterData().categoryBits == VERTICAL_ROAD) {
                    Body roadBody = fixture.getBody();
                    Vector2 pos = roadBody.getPosition();
                    verticalRoad[0] = new Rectangle(
                        pos.x - querySize/2, pos.y - querySize/2,
                        querySize, querySize
                    );
                }
                return true;
            },
            transform.getPosition().x - querySize/2,
            transform.getPosition().y - querySize/2,
            transform.getPosition().x + querySize/2,
            transform.getPosition().y + querySize/2
        );


        handleRoadLogic(car, transform, horizontalRoad[0], verticalRoad[0]);
    }

    private void handleRoadLogic(CarComponent car, TransformComponent transform,
                                 Rectangle horizontalRoad, Rectangle verticalRoad) {
        if (horizontalRoad == null && verticalRoad == null) {
            transform.dispose();
            if(transform.body != null && transform.body.getUserData() != null)
                engine.removeEntity((Entity) transform.body.getUserData());
            return;
        }

        // Handle road changes and U-turns
        if (horizontalRoad != null && !car.horizontal) {
            handleHorizontalRoadTransition(car, transform, horizontalRoad);
        } else if (verticalRoad != null && car.horizontal) {
            handleVerticalRoadTransition(car, transform, verticalRoad);
        } else {
            handleUTurn(car, transform, horizontalRoad, verticalRoad);
        }
    }

    private void handleHorizontalRoadTransition(CarComponent car, TransformComponent transform, Rectangle road) {
        boolean right = road.x + road.width > transform.body.getPosition().x + MAP_SIZE * 3;
        boolean left = road.x < transform.body.getPosition().x - MAP_SIZE * 3;
        boolean direction = (right && left) ? chunk.random.nextBoolean() : right;

        car.horizontal = true;
        car.direction = direction;
        car.changeDirection = direction ?
            road.x + road.width - MAP_SIZE :
            road.x + MAP_SIZE;

        transform.velocity = new Vector2(direction ? car.speed : -car.speed ,0f);
    }

    private void handleVerticalRoadTransition(CarComponent car, TransformComponent transform, Rectangle road) {
        boolean up = road.y + road.height > transform.body.getPosition().y + MAP_SIZE * 3;
        boolean down = road.y < transform.body.getPosition().y - MAP_SIZE * 3;
        boolean direction = (up && down) ? chunk.random.nextBoolean() : up;

        car.horizontal = false;
        car.direction = direction;
        car.changeDirection = direction ?
            road.y + road.height - MAP_SIZE :
            road.y + MAP_SIZE;

        transform.velocity = new Vector2(0f, direction ? car.speed : -car.speed);
    }

    private void handleUTurn(CarComponent car, TransformComponent transform, Rectangle horizontalRoad, Rectangle verticalRoad) {
        if (horizontalRoad != null) {
            car.direction = !car.direction;
            car.changeDirection = car.direction ?
                horizontalRoad.x + horizontalRoad.width - MAP_SIZE :
                horizontalRoad.x + MAP_SIZE;

            float yPos = horizontalRoad.y + (car.direction ? 0 : horizontalRoad.height - chunk.carWidth);
            transform.body.setTransform(transform.getPosition().x, yPos, transform.body.getAngle());
            transform.velocity = new Vector2(car.direction ? car.speed : -car.speed, 0f);
        } else if (verticalRoad != null) {
            car.direction = !car.direction;
            car.changeDirection = car.direction ?
                verticalRoad.y + verticalRoad.height - MAP_SIZE :
                verticalRoad.y + MAP_SIZE;

            float xPos = verticalRoad.x + (car.direction ? verticalRoad.width - chunk.carWidth : 0);
            transform.body.setTransform(xPos, transform.getPosition().y, transform.body.getAngle());
            transform.velocity = new Vector2(0f, car.direction ? car.speed : -car.speed);

        }
    }
}
