package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import io.github.nickolasddiaz.components.*;


import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize; // unit of one chunk length
import static io.github.nickolasddiaz.utils.MapGenerator.MAP_SIZE; // unit of one tile length

public class CarSystem extends IteratingSystem {
    private final ComponentMapper<CarComponent> CarMapper;
    private final ChunkComponent chunk;
    private final ComponentMapper<TransformComponent> transformMapper;

    private final Engine engine;


    public CarSystem(Engine engine, ChunkComponent chunk) {
        super(Family.all(CarComponent.class, TransformComponent.class).get());
        this.engine = engine;
        this.chunk = chunk;
        CarMapper = ComponentMapper.getFor(CarComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CarComponent car = CarMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);

        Vector2 chunkPosition = new Vector2((int) Math.floor(transform.position.x / chunkSize), (int) Math.floor(transform.position.y / chunkSize));

        if(!chunk.mapChunks.containsKey(chunkPosition)) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        transform.position = new Vector2(transform.position.x, transform.position.y);
        transform.rotation =  ((car.horizontal)? (car.direction)? 0 : 180: (car.direction)? 90 : 270); // right: 0 left: 180 up: 90 down: 270

        if(car.horizontal){
            moveCar(transform);
            if(car.direction) {
                if(car.changeDirection > transform.position.x) { // going right
                    transform.position.x += car.speed * deltaTime;return;}
            }else{
                if(car.changeDirection < transform.position.x) { // going left
                    transform.position.x -= car.speed * deltaTime;return;}}
        }else{
            moveCar(transform);
            if(car.direction) {
                if(car.changeDirection > transform.position.y) { // going up
                    transform.position.y += car.speed * deltaTime;return;}
            }else{
                if(car.changeDirection < transform.position.y) { // going down
                    transform.position.y -= car.speed * deltaTime;return;}
            }
        }

        Rectangle horizontalObject = chunk.getObjectIsInsideRectMapChunk(new Rectangle(transform.position.x, transform.position.y, MAP_SIZE, MAP_SIZE), "HORIZONTAL"); //give me 0 out of bound errors
        Rectangle verticalObject = chunk.getObjectIsInsideRectMapChunk(new Rectangle(transform.position.x, transform.position.y, MAP_SIZE, MAP_SIZE), "VERTICAL");

        if(horizontalObject == null && verticalObject == null) { //disposing of the car as it is out of bounds
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        if(horizontalObject != null && !car.horizontal) { // if the car is vertical and there is a horizontal object
            boolean right = horizontalObject.contains(transform.position.x + MAP_SIZE *3, transform.position.y); //check if going right is possible
            boolean left =  horizontalObject.contains(transform.position.x - MAP_SIZE *3, transform.position.y); //check if going left is possible
            boolean direction = (right && left)? chunk.random.nextBoolean() : right; //true is right, false is left it is never neither
            car.horizontal = true;
            car.direction = direction;
            car.changeDirection = direction? horizontalObject.x + horizontalObject.width - MAP_SIZE: horizontalObject.x + MAP_SIZE;
        }
        else if(verticalObject != null && car.horizontal) { // if the car is horizontal and there is a vertical object
            boolean up = verticalObject.contains(transform.position.x, transform.position.y + MAP_SIZE *3); //check if going up is possible
            boolean down = verticalObject.contains(transform.position.x, transform.position.y - MAP_SIZE *3); //check if going down is possible
            boolean direction = (up && down)? chunk.random.nextBoolean() : up; //true is up, false is down it is never neither
            car.horizontal = false;
            car.direction = direction;
            car.changeDirection = direction? verticalObject.y + verticalObject.height - MAP_SIZE: verticalObject.y +  MAP_SIZE;
        }else{
            if(horizontalObject != null) {
                if(car.direction && transform.position.x % chunkSize < 3* MAP_SIZE){ // car is going right and near the right border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(3* MAP_SIZE,0), chunk.horizontalFilter);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.x + roadGoesPastChunk.width - MAP_SIZE;
                        transform.position.x += 3*MAP_SIZE; //make the car be in the correct chunk
                        moveCar(transform);
                        return;
                    }
                } else if (!car.direction && transform.position.x % chunkSize > chunkSize - 3* MAP_SIZE) { // car is going left and near the left border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(-3* MAP_SIZE,0), chunk.verticalFilter);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.x + MAP_SIZE;
                        transform.position.x -= 3*MAP_SIZE; //make the car be in the correct chunk
                        moveCar(transform);
                        return;
                    }
                }
                car.direction = !car.direction; // making a U-turn
                car.changeDirection = (car.direction) ? horizontalObject.x  + horizontalObject.width - MAP_SIZE : horizontalObject.x + MAP_SIZE;
            }else{
                if(car.direction && transform.position.y % chunkSize < 3* MAP_SIZE){ // car is going up and near the top border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(0,3* MAP_SIZE), chunk.horizontalFilter);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.y + roadGoesPastChunk.width - MAP_SIZE;
                        transform.position.y += 3*MAP_SIZE; //make the car be in the correct chunk
                        moveCar(transform);
                        return;
                    }
                } else if (!car.direction && transform.position.y % chunkSize > chunkSize - 3* MAP_SIZE) { // car is going down and near the bottom border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(0,-3* MAP_SIZE), chunk.verticalFilter);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.y + MAP_SIZE;
                        transform.position.y -= 3*MAP_SIZE; //make the car be in the correct chunk
                        moveCar(transform);
                        return;
                    }
                }
                car.direction = !car.direction; // making a U-turn
                car.changeDirection = (car.direction) ? verticalObject.y + verticalObject.height - MAP_SIZE : verticalObject.y +  MAP_SIZE ;
            }
        }

        if(horizontalObject != null) { // moving right/up should be on the right side of the road
            transform.position.y = horizontalObject.y + ((car.direction) ? 0 : horizontalObject.height - chunk.carWidth); //right or left
        } else{
            transform.position.x = verticalObject.x + ((car.direction) ? verticalObject.width - chunk.carWidth : 0); //align the car with the side of the road
        }
        moveCar(transform);
    }

    private void moveCar(TransformComponent transform){
        chunk.world.move(transform.item, transform.position.x, transform.position.y, CollisionFilter.defaultFilter);
    }
}


