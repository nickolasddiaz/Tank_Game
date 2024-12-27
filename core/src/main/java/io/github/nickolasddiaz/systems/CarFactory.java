package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.systems.MapGenerator.MAP_SIZE;
import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;

public class CarFactory {
    private final Engine engine;
    private final TextureAtlas atlas; // Assuming you're using a texture atlas
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

        transform.sprite = new Sprite(atlas.findRegion("tank"));
        transform.sprite.setSize(TILE_SIZE * TILE_SIZE *4 , TILE_SIZE * TILE_SIZE * 4);

        CarComponent carComponent = null;


        // Create car component
        for (MapObject object : chunkComponent.mapChunks.get(new Vector2(0,0)).getLayers().get("OBJECTS").getObjects()) {
            if (object instanceof RectangleMapObject) {
                if ("HORIZONTAL".equals(object.getName())) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    boolean isRight = chunkComponent.random.nextBoolean();
                    float spawnY = rect.y + ((isRight) ? 0 : rect.height - chunkComponent.carWidth);
                    float spawnX = rect.x + chunkComponent.random.nextFloat() * rect.width; // random x between rect.x and rect.x + rect.width

                    transform.position.set(spawnX,spawnY);
                    carComponent = new CarComponent(isRight, (isRight ? rect.x + rect.width - (float)MAP_SIZE / 2 : rect.x + (float)MAP_SIZE / 2));
                    carComponent.horizontal = true;
                    break;
                }
            }
        }
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
        TransformComponent transformComponent = new TransformComponent();
        transformComponent.sprite = new Sprite(atlas.findRegion("car"));
        transformComponent.color = carColors[carTypeIndex];
        transformComponent.position.set(position);
        transformComponent.rotation = 0f;
        transformComponent.sprite.setSize(TILE_SIZE * TILE_SIZE , TILE_SIZE * TILE_SIZE *2);
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
