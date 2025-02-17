package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.CollisionCategory.CAR;
import static io.github.nickolasddiaz.utils.CollisionCategory.HORIZONTAL_ROAD;
import static io.github.nickolasddiaz.utils.MapGenerator.*;

public class CarFactory {
    private final Engine engine;
    private final Skin skin;
    CameraComponent cameraComponent;
    ChunkComponent chunkComponent;
    Color[] carColors = new Color[]{Color.BLUE, Color.GREEN, Color.PURPLE, Color.YELLOW, Color.CHARTREUSE, Color.PINK, Color.WHITE, Color.GRAY, Color.RED, Color.ORANGE, Color.CYAN, Color.MAGENTA, Color.BROWN, Color.TEAL, Color.OLIVE, Color.SKY, Color.LIME, Color.MAROON, Color.NAVY, Color.TEAL, Color.TEAL, Color.VIOLET, Color.FOREST, Color.SALMON, Color.TAN,  Color.MAROON, Color.NAVY, Color.TEAL, Color.TEAL, Color.VIOLET, Color.FOREST, Color.SALMON, Color.TAN};

    public CarFactory(Engine engine, Skin skin, CameraComponent cameraComponent, ChunkComponent chunkComponent) {
        this.engine = engine;
        this.skin = skin;
        this.cameraComponent = cameraComponent;
        this.chunkComponent = chunkComponent;
    }
    public Entity createTank(TransformComponent transform){
        Entity tank = engine.createEntity();
        CarComponent carComponent;

        Body[] rect = chunkComponent.getBodiesInRect(new Rectangle(0,0,chunkSize,chunkSize), HORIZONTAL_ROAD); // search within the center chunk
        if(rect == null || rect.length == 0) rect = chunkComponent.getBodiesInRect(new Rectangle(-chunkSize,-chunkSize,2*chunkSize,2*chunkSize), HORIZONTAL_ROAD); // search within the 3x3 chunks

        Body road = rect[chunkComponent.random.nextInt(Math.abs(rect.length))];
        Rectangle roadRect = new Rectangle(road.getPosition().x, road.getPosition().y, road.getFixtureList().get(0).getShape().getRadius() * 2, road.getFixtureList().get(0).getShape().getRadius() * 2);

        // Create car component
        boolean isRight = chunkComponent.random.nextBoolean();
        float spawnY = roadRect.y + ((isRight) ? 0 : roadRect.height - chunkComponent.carWidth);
        float spawnX = roadRect.x + chunkComponent.random.nextFloat() * roadRect.width; // random x between rect.x and rect.x + rect.width

        transform.body.setTransform(spawnX,spawnY,0f);
        transform.rotation = 0f;

        carComponent = new CarComponent(isRight, (isRight ? roadRect.x + roadRect.width - itemSize / 2f : roadRect.x + itemSize / 2f));
        carComponent.horizontal = true;

        tank.add(transform);
        tank.add(carComponent);
        tank.add(chunkComponent);
        tank.add(cameraComponent);

        // Add to engine
        engine.addEntity(tank);

        return tank;
    }

    public void createCar(Vector2 position, boolean direction, float changeDirection,
                          boolean horizontal) {
        Entity car = engine.createEntity();

        // Create transform component with Box2D body
        TransformComponent transformComponent = new TransformComponent(
            chunkComponent.world,
            skin.getSprite("car"),
            (int) (itemSize * 1.80f),
            (int) (itemSize * .78f),
            carColors[chunkComponent.random.nextInt(carColors.length)],
            true,  // isDynamic
            CAR,
            position,
            horizontal ? (direction ? 0 : 180) : (direction ? 90 : 270),
            1  // health
        );

        // Create car component
        CarComponent carComponent = new CarComponent(direction, changeDirection);
        carComponent.horizontal = horizontal;

        // Set initial velocity based on direction
        if (horizontal) {
            transformComponent.body.setLinearVelocity(
                direction ? carComponent.speed : -carComponent.speed,
                0
            );
        } else {
            transformComponent.body.setLinearVelocity(
                0,
                direction ? carComponent.speed : -carComponent.speed
            );
        }

        // Add components to entity
        car.add(transformComponent);
        car.add(carComponent);
        car.add(chunkComponent);
        car.add(cameraComponent);

        // Add to engine
        engine.addEntity(car);
    }
}
