package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.systems.MapGenerator.chunkSize; // unit of one chunk length
import static io.github.nickolasddiaz.systems.MapGenerator.MAP_SIZE; // unit of one tile length

public class CarSystem extends IteratingSystem {
    private final ComponentMapper<CarComponent> CarMapper;
    private final ComponentMapper<ChunkComponent> chunkMapper;
    private final ComponentMapper<TransformComponent> transformMapper;

    private final Engine engine;
    ShapeRenderer shapeRenderer = new ShapeRenderer();


    public CarSystem(Engine engine) {
        super(Family.all(CarComponent.class, ChunkComponent.class, TransformComponent.class).get());
        this.engine = engine;
        CarMapper = ComponentMapper.getFor(CarComponent.class);
        chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CarComponent car = CarMapper.get(entity);
        ChunkComponent chunk = chunkMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);

        Vector2 chunkPosition = new Vector2((int) Math.floor(transform.position.x / chunkSize), (int) Math.floor(transform.position.y / chunkSize));

        if(!chunk.mapChunks.containsKey(chunkPosition)) {
            engine.removeEntity(entity);
            return;
        }

        transform.position = new Vector2(transform.position.x, transform.position.y);
        transform.rotation =  ((car.horizontal)? (car.direction)? 0 : 180: (car.direction)? 90 : 270); // right: 0 left: 180 up: 90 down: 270

//        shapeRenderer.begin(ShapeRenderer.ShapeType.Line); // pink is right, green is left, blue is up, purple is down
//        shapeRenderer.setColor((car.horizontal)? (car.direction)? Color.PINK:Color.GREEN: (car.direction)? Color.BLUE:Color.PURPLE);
//        if (car.horizontal) {
//            shapeRenderer.line(car.position.x, car.position.y, car.changeDirection, car.position.y);
//        } else {
//            shapeRenderer.line(car.position.x, car.position.y, car.position.x, car.changeDirection);
//        }
//        shapeRenderer.end();


        if(car.horizontal){
            if(car.direction) {
                if(car.changeDirection > transform.position.x) { // going right
                    transform.position.x += car.speed * deltaTime;return;}
            }else{
                if(car.changeDirection < transform.position.x) { // going left
                    transform.position.x -= car.speed * deltaTime;return;}}
        }else{
            if(car.direction) {
                if(car.changeDirection > transform.position.y) { // going up
                    transform.position.y += car.speed * deltaTime;return;}
            }else{
                if(car.changeDirection < transform.position.y) { // going down
                    transform.position.y -= car.speed * deltaTime;return;}
            }
        }

        Rectangle horizontalObject = chunk.getObjectIsInside(transform.position, chunkPosition, chunk.horizontalRoadObject);
        Rectangle verticalObject = chunk.getObjectIsInside(transform.position, chunkPosition, chunk.verticalRoadObject);


        if(horizontalObject == null && verticalObject == null) { //disposing of the car as it is out of bounds
            engine.removeEntity(entity);
            Gdx.app.log("CarSystem", "Car out of bounds");
            return;
        }

        if(horizontalObject != null && !car.horizontal) { // if the car is vertical and there is a horizontal object
            boolean right = horizontalObject.contains(transform.position.x + MAP_SIZE *3, transform.position.y); //check if going right is possible
            boolean left =  horizontalObject.contains(transform.position.x - MAP_SIZE *3, transform.position.y); //check if going left is possible
            boolean direction = (right && left)? chunk.random.nextBoolean() : right; //true is right, false is left it is never neither
            if(!right && !left) {Gdx.app.log("CarSystem", "Car error"); return;}
            car.horizontal = true;
            car.direction = direction;
            car.changeDirection = direction? horizontalObject.x + horizontalObject.width - MAP_SIZE: horizontalObject.x + MAP_SIZE;
        }
        else if(verticalObject != null && car.horizontal) { // if the car is horizontal and there is a vertical object
            boolean up = verticalObject.contains(transform.position.x, transform.position.y + MAP_SIZE *3); //check if going up is possible
            boolean down = verticalObject.contains(transform.position.x, transform.position.y - MAP_SIZE *3); //check if going down is possible
            boolean direction = (up && down)? chunk.random.nextBoolean() : up; //true is up, false is down it is never neither
            if(!up && !down) {Gdx.app.log("CarSystem", "Car error"); return;}
            car.horizontal = false;
            car.direction = direction;
            car.changeDirection = direction? verticalObject.y + verticalObject.height - MAP_SIZE: verticalObject.y +  MAP_SIZE;
        }else{
            if(horizontalObject != null) {
                if(car.direction && transform.position.x % chunkSize < 3* MAP_SIZE){ // car is going right and near the right border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(3* MAP_SIZE,0), chunkPosition.add(1,0), chunk.verticalRoadObject);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.x + roadGoesPastChunk.width - MAP_SIZE;
                        transform.position.x += 3*MAP_SIZE; //make the car be in the correct chunk
                        return;
                    }
                } else if (!car.direction && transform.position.x % chunkSize > chunkSize - 3* MAP_SIZE) { // car is going left and near the left border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(-3* MAP_SIZE,0), chunkPosition.add(-1,0), chunk.verticalRoadObject);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.x + MAP_SIZE;
                        transform.position.x -= 3*MAP_SIZE; //make the car be in the correct chunk
                        return;
                    }
                }
                car.direction = !car.direction; // making a U-turn
                car.changeDirection = (car.direction) ? horizontalObject.x  + horizontalObject.width - MAP_SIZE : horizontalObject.x + MAP_SIZE;
            }else{
                if(car.direction && transform.position.y % chunkSize < 3* MAP_SIZE){ // car is going up and near the top border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(0,3* MAP_SIZE), chunkPosition.add(0,1), chunk.horizontalRoadObject);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.y + roadGoesPastChunk.width - MAP_SIZE;
                        transform.position.y += 3*MAP_SIZE; //make the car be in the correct chunk
                        return;
                    }
                } else if (!car.direction && transform.position.y % chunkSize > chunkSize - 3* MAP_SIZE) { // car is going down and near the bottom border
                    Rectangle roadGoesPastChunk = chunk.getObjectIsInside(transform.position.add(0,-3* MAP_SIZE), chunkPosition.add(0,-1), chunk.horizontalRoadObject);
                    if(roadGoesPastChunk != null) {
                        car.changeDirection = roadGoesPastChunk.y + MAP_SIZE;
                        transform.position.y -= 3*MAP_SIZE; //make the car be in the correct chunk
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
    }
}


