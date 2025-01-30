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
        if (!chunk.mapChunks.containsKey(chunk.getChunkPosition(transform.position))) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }
        if(body == null) return;

        // Update position from Box2D body
        transform.position.set(body.getPosition());
        transform.rotation = ((car.horizontal) ?
            (car.direction) ? 0 : 180 :
            (car.direction) ? 90 : 270);
        body.setTransform(body.getPosition(), (float) Math.toRadians(transform.rotation));

        if (car.horizontal) {
            moveCarBox2D(body, car, deltaTime);
            if (car.direction) {
                if (car.changeDirection > transform.position.x) {
                    body.setLinearVelocity(car.speed * deltaTime, 0);
                    return;
                }
            } else {
                if (car.changeDirection < transform.position.x) {
                    body.setLinearVelocity(-car.speed * deltaTime, 0);
                    return;
                }
            }
        } else {
            moveCarBox2D(body, car, deltaTime);
            if (car.direction) {
                if (car.changeDirection > transform.position.y) {
                    body.setLinearVelocity(0, car.speed * deltaTime);
                    return;
                }
            } else {
                if (car.changeDirection < transform.position.y) {
                    body.setLinearVelocity(0, -car.speed * deltaTime);
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
                if (fixture.getFilterData().categoryBits == ChunkComponent.HORIZONTAL_ROAD) {
                    Body roadBody = fixture.getBody();
                    Vector2 pos = roadBody.getPosition();
                    horizontalRoad[0] = new Rectangle(
                        pos.x - querySize/2, pos.y - querySize/2,
                        querySize, querySize
                    );
                }
                return true;
            },
            transform.position.x - querySize/2,
            transform.position.y - querySize/2,
            transform.position.x + querySize/2,
            transform.position.y + querySize/2
        );

        chunk.world.QueryAABB(
            fixture -> {
                if (fixture.getFilterData().categoryBits == ChunkComponent.VERTICAL_ROAD) {
                    Body roadBody = fixture.getBody();
                    Vector2 pos = roadBody.getPosition();
                    verticalRoad[0] = new Rectangle(
                        pos.x - querySize/2, pos.y - querySize/2,
                        querySize, querySize
                    );
                }
                return true;
            },
            transform.position.x - querySize/2,
            transform.position.y - querySize/2,
            transform.position.x + querySize/2,
            transform.position.y + querySize/2
        );


        handleRoadLogic(car, transform, body, horizontalRoad[0], verticalRoad[0]);
    }

    private void moveCarBox2D(Body body, CarComponent car, float deltaTime) {
        Vector2 currentVel = body.getLinearVelocity();
        if (currentVel.len() > car.speed) {
            currentVel.nor().scl(car.speed);
            body.setLinearVelocity(currentVel);
        }
    }

    private void handleRoadLogic(CarComponent car, TransformComponent transform, Body body,
                                 Rectangle horizontalRoad, Rectangle verticalRoad) {
        if (horizontalRoad == null && verticalRoad == null) {
            transform.dispose();
            if(transform.body != null && transform.body.getUserData() != null)
                engine.removeEntity((Entity) transform.body.getUserData());
            return;
        }

        // Handle road changes and U-turns
        if (horizontalRoad != null && !car.horizontal) {
            handleHorizontalRoadTransition(car, body, horizontalRoad);
        } else if (verticalRoad != null && car.horizontal) {
            handleVerticalRoadTransition(car, body, verticalRoad);
        } else {
            handleUTurn(car, transform, body, horizontalRoad, verticalRoad);
        }
    }

    private void handleHorizontalRoadTransition(CarComponent car, Body body, Rectangle road) {
        boolean right = road.x + road.width > body.getPosition().x + MAP_SIZE * 3;
        boolean left = road.x < body.getPosition().x - MAP_SIZE * 3;
        boolean direction = (right && left) ? chunk.random.nextBoolean() : right;

        car.horizontal = true;
        car.direction = direction;
        car.changeDirection = direction ?
            road.x + road.width - MAP_SIZE :
            road.x + MAP_SIZE;

        body.setLinearVelocity(direction ? car.speed : -car.speed, 0);
    }

    private void handleVerticalRoadTransition(CarComponent car, Body body, Rectangle road) {
        boolean up = road.y + road.height > body.getPosition().y + MAP_SIZE * 3;
        boolean down = road.y < body.getPosition().y - MAP_SIZE * 3;
        boolean direction = (up && down) ? chunk.random.nextBoolean() : up;

        car.horizontal = false;
        car.direction = direction;
        car.changeDirection = direction ?
            road.y + road.height - MAP_SIZE :
            road.y + MAP_SIZE;

        body.setLinearVelocity(0, direction ? car.speed : -car.speed);
    }

    private void handleUTurn(CarComponent car, TransformComponent transform, Body body,
                             Rectangle horizontalRoad, Rectangle verticalRoad) {
        if (horizontalRoad != null) {
            car.direction = !car.direction;
            car.changeDirection = car.direction ?
                horizontalRoad.x + horizontalRoad.width - MAP_SIZE :
                horizontalRoad.x + MAP_SIZE;

            float yPos = horizontalRoad.y + (car.direction ? 0 : horizontalRoad.height - chunk.carWidth);
            body.setTransform(body.getPosition().x, yPos, body.getAngle());
            body.setLinearVelocity(car.direction ? car.speed : -car.speed, 0);
        } else if (verticalRoad != null) {
            car.direction = !car.direction;
            car.changeDirection = car.direction ?
                verticalRoad.y + verticalRoad.height - MAP_SIZE :
                verticalRoad.y + MAP_SIZE;

            float xPos = verticalRoad.x + (car.direction ? verticalRoad.width - chunk.carWidth : 0);
            body.setTransform(xPos, body.getPosition().y, body.getAngle());
            body.setLinearVelocity(0, car.direction ? car.speed : -car.speed);
        }
    }
}
