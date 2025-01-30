package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.components.ChunkComponent.*;
import static io.github.nickolasddiaz.utils.MapGenerator.*;

public class EnemyFactory {
    private final Engine engine;
    private final Skin skin;
    CameraComponent cameraComponent;
    ChunkComponent chunkComponent;
    StatsComponent statsComponent;
    SettingsComponent settings;
    PlayerComponent playerComponent;


    public EnemyFactory(Engine engine, Skin skin, CameraComponent cameraComponent, ChunkComponent chunkComponent, StatsComponent statsComponent, TransformComponent playerTransformComponent, SettingsComponent settings, PlayerComponent playerComponent, BulletFactory bulletFactory,ChunkComponent chunk) {
        this.engine = engine;
        this.skin = skin;
        this.cameraComponent = cameraComponent;
        this.chunkComponent = chunkComponent;
        this.statsComponent = statsComponent;
        this.settings = settings;
        this.playerComponent = playerComponent;
        engine.addSystem(new EnemySystem(engine, playerTransformComponent, cameraComponent, settings, bulletFactory,chunk));
    }

    public void createTank(Vector2 spawnPosition, boolean isAlly){ {
        playerComponent.enemyCount++;
        Entity tank = engine.createEntity();

        tank.add(statsComponent);
        tank.add(cameraComponent);
        tank.add(chunkComponent);
        //enemy sprite is 30x50 now is 76x128
        Vector2 tempPosition = spawnPosition.cpy();
        float length,radiusAngle;
        int i = 0;
        while (i < 10) {
            radiusAngle = (float) Math.toRadians(chunkComponent.random.nextInt(360));
            length = chunkComponent.random.nextFloat() * chunkSize;
            Vector2 position = new Vector2((float) Math.cos(radiusAngle) * length, (float) Math.sin(radiusAngle) * length);
            if (chunkComponent.isPointInside(position, HORIZONTAL_ROAD)) {
                tempPosition = position;
                break;
            }
            i++;
        }
        //transformComponent.color = carColors[carTypeIndex];
        TransformComponent transformComponent = new TransformComponent(chunkComponent.world, skin.getSprite("tank"),itemSize *2, (int) (itemSize *1.2f), (isAlly)? Color.GREEN : null, true, (isAlly)? ALLY : ENEMY, tempPosition, 0f,2);

        tank.add(transformComponent);
        //(float) statsComponent.getStars() /15
        EnemyComponent enemyComponent = new EnemyComponent(0f,10f,10f,1f, 20f * itemSize, 5, isAlly);
        tank.add(enemyComponent);
        transformComponent.turretComponent(
            skin.getSprite("turret"),
            new Vector2(itemSize*1.1f, itemSize * 0.5f),  // Position at center of tank
            itemSize*1.48f,                                // turret width
            itemSize                                 // turret height
        ); //52 width 20 height modified 77 width and 20 height for better axis rotation


        tank.add(settings);

        engine.addEntity(tank);
    }

    }
}

