package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;
import static io.github.nickolasddiaz.systems.MapGenerator.chunkSize;

public class EnemyFactorySystem {
    private final Engine engine;
    private final TextureAtlas atlas; // Assuming you're using a texture atlas
    CameraComponent cameraComponent;
    ChunkComponent chunkComponent;
    StatsComponent statsComponent;
    TransformComponent playerComponent;


    public EnemyFactorySystem(Engine engine, TextureAtlas atlas, CameraComponent cameraComponent, ChunkComponent chunkComponent, StatsComponent statsComponent, TransformComponent playerComponent) {
        this.engine = engine;
        this.atlas = atlas;
        this.cameraComponent = cameraComponent;
        this.chunkComponent = chunkComponent;
        this.statsComponent = statsComponent;
        this.playerComponent = playerComponent;
    }

    public void createTank(int enemyType, Vector2 spawnPosition){ {
        Entity tank = engine.createEntity();

        tank.add(statsComponent);
        tank.add(cameraComponent);
        tank.add(chunkComponent);
        TransformComponent transformComponent = new TransformComponent();
        transformComponent.sprite = new Sprite(atlas.findRegion("tank"));
        //transformComponent.color = carColors[carTypeIndex];
        transformComponent.position.set(spawnPosition);
        transformComponent.sprite.setSize(TILE_SIZE * TILE_SIZE *4, TILE_SIZE * TILE_SIZE *4);
        tank.add(transformComponent);

        float length,radiusAngle;
        int i = 0;
        while (i < 10) {
            radiusAngle = (float) Math.toRadians(chunkComponent.random.nextInt(360));
            length = chunkComponent.random.nextFloat() * chunkSize;
            Vector2 position = new Vector2((float) Math.cos(radiusAngle) * length, (float) Math.sin(radiusAngle) * length);
            if (chunkComponent.getObjectIsInsideBoolean(new Vector2(position.x, position.y), chunkComponent.horizontalFilter)) {
                transformComponent.position.set(position);
                break;
            }
            i++;
        }

        EnemyComponent enemyComponent = new EnemyComponent(100f,10f,100f,100f, (float) statsComponent.getStars() /15);
        tank.add(enemyComponent);


        engine.addSystem(new EnemySystem(engine, playerComponent));

        engine.addEntity(tank);
    }

    }
}

