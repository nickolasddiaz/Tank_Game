package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.systems.MapGenerator.*;

public class CarFactory {
    private final Engine engine;
    private final TextureAtlas atlas;
    CameraComponent cameraComponent;
    ChunkComponent chunkComponent;
    Color[] carColors = new Color[]{Color.BLUE, Color.GREEN, Color.PURPLE, Color.YELLOW, Color.CHARTREUSE, Color.PINK, Color.WHITE, Color.GRAY, Color.RED, Color.ORANGE};

    public CarFactory(Engine engine, TextureAtlas atlas, CameraComponent cameraComponent, ChunkComponent chunkComponent) {
        this.engine = engine;
        this.atlas = atlas;
        this.cameraComponent = cameraComponent;
        this.chunkComponent = chunkComponent;
    }
    public Entity createTank(TransformComponent transform){
        Entity tank = engine.createEntity();
        CarComponent carComponent = null;

        Rectangle rect = chunkComponent.getObjectIsInsideRect(new Rectangle(0,0,chunkSize,chunkSize), chunkComponent.horizontalFilter);
        if(rect == null) rect = chunkComponent.getObjectIsInsideRect(new Rectangle(-chunkSize,-chunkSize,2*chunkSize,2*chunkSize), chunkComponent.horizontalFilter);


        // Create car component
        boolean isRight = chunkComponent.random.nextBoolean();
        float spawnY = rect.y + ((isRight) ? 0 : rect.height - chunkComponent.carWidth);
        float spawnX = rect.x + chunkComponent.random.nextFloat() * rect.width; // random x between rect.x and rect.x + rect.width

        transform.position = new Vector2(spawnX,spawnY);
        transform.rotation = 0f;

        carComponent = new CarComponent(isRight, (isRight ? rect.x + rect.width - (float)MAP_SIZE / 2 : rect.x + (float)MAP_SIZE / 2));
        carComponent.horizontal = true;

        tank.add(transform);
        tank.add(carComponent);
        tank.add(chunkComponent);
        tank.add(cameraComponent);

        // Add to engine
        engine.addEntity(tank);

        return tank;
    }

    public void createCar(Vector2 position, boolean direction, float changeDirection, boolean horizontal, int carTypeIndex) {
        Entity car = engine.createEntity();

        // Create transform component
        //car sprite is 26x60 now is 76x176
        TransformComponent transformComponent = new TransformComponent(new Sprite(atlas.findRegion("car")), (int) (itemSize * 1.34f), (int) (itemSize *.67f), carColors[carTypeIndex], true, "CAR", chunkComponent.world,position, 0f);
        transformComponent.updateBounds();
        car.add(transformComponent);

        // Create car component
        CarComponent carComponent = new CarComponent(direction, changeDirection);
        carComponent.horizontal = horizontal;
        car.add(carComponent);
        car.add(chunkComponent);

        // Add sprite component for rendering
        car.add(cameraComponent);

        // Add to engine
        engine.addEntity(car);

    }
}
