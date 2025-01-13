package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.systems.MapGenerator.*;

public class EnemyFactory {
    private final Engine engine;
    private final TextureAtlas atlas; // Assuming you're using a texture atlas
    CameraComponent cameraComponent;
    ChunkComponent chunkComponent;
    StatsComponent statsComponent;
    TransformComponent playerComponent;
    SettingsComponent settings;


    public EnemyFactory(Engine engine, TextureAtlas atlas, CameraComponent cameraComponent, ChunkComponent chunkComponent, StatsComponent statsComponent, TransformComponent playerComponent, SettingsComponent settings) {
        this.engine = engine;
        this.atlas = atlas;
        this.cameraComponent = cameraComponent;
        this.chunkComponent = chunkComponent;
        this.statsComponent = statsComponent;
        this.playerComponent = playerComponent;
        this.settings = settings;
    }

    public void createTank(int enemyType, Vector2 spawnPosition){ {
        Entity tank = engine.createEntity();

        tank.add(statsComponent);
        tank.add(cameraComponent);
        tank.add(chunkComponent);
        TransformComponent transformComponent = new TransformComponent();
        Vector2 tempPosition = spawnPosition.cpy();
        float length,radiusAngle;
        int i = 0;
        while (i < 10) {
            radiusAngle = (float) Math.toRadians(chunkComponent.random.nextInt(360));
            length = chunkComponent.random.nextFloat() * chunkSize;
            Vector2 position = new Vector2((float) Math.cos(radiusAngle) * length, (float) Math.sin(radiusAngle) * length);
            if (chunkComponent.getObjectIsInsideBoolean(new Vector2(position.x, position.y), chunkComponent.horizontalFilter)) {
                tempPosition = position;
                break;
            }
            i++;
        }
        //transformComponent.color = carColors[carTypeIndex];
        //enemy sprite is 30x50 now is 76x128
        transformComponent.updateSprite(new Sprite(atlas.findRegion("tank")),itemSize *2, (int) (itemSize *1.2f) , tempPosition, null, 0f);
        tank.add(transformComponent);

        EnemyComponent enemyComponent = new EnemyComponent(100f,10f,100f,10f, (float) statsComponent.getStars() /15);
        tank.add(enemyComponent);

        tank.add(settings);
        tank.add(new CollisionComponent(transformComponent.sprite.getX(), transformComponent.sprite.getY(), transformComponent.sprite.getWidth(), transformComponent.sprite.getHeight(), transformComponent.sprite.getBoundingRectangle(), chunkComponent.movingObject));
        engine.addSystem(new CollisionSystem());


        engine.addSystem(new EnemySystem(engine, playerComponent, cameraComponent, settings));

        engine.addEntity(tank);
    }

    }
}

