package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.EntityStats;

import static io.github.nickolasddiaz.components.ChunkComponent.*;
import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.*;

public class EnemyFactory {
    private final Engine engine;
    private final Skin skin;
    CameraComponent cameraComponent;
    ChunkComponent chunkComponent;
    StatsComponent statsComponent;
    SettingsComponent settings;
    PlayerComponent playerComponent;


    public EnemyFactory(Engine engine, Skin skin, CameraComponent cameraComponent, ChunkComponent chunkComponent, StatsComponent statsComponent, TransformComponent playerTransformComponent, SettingsComponent settings, PlayerComponent playerComponent ,ChunkComponent chunk) {
        this.engine = engine;
        this.skin = skin;
        this.cameraComponent = cameraComponent;
        this.chunkComponent = chunkComponent;
        this.statsComponent = statsComponent;
        this.settings = settings;
        this.playerComponent = playerComponent;
        engine.addSystem(new EnemySystem(engine, playerTransformComponent,chunk));
    }
    private Vector2 getPosition(){
        Vector2 position = new Vector2();
        int i = 0;
        while (i < 10) {
            position = new Vector2(chunkComponent.random.nextInt(chunkSize) - MAP_SIZE / 2f, chunkComponent.random.nextInt(chunkSize) - MAP_SIZE / 2f);
            position.add(chunkComponent.currentChunk.scl(chunkSize));
            if (chunkComponent.isPointInside(position, HORIZONTAL_ROAD)) {
                break;
            }
            i++;
        }
        return position;
    }
    public void createTank(boolean ally, EntityStats stats){
        createTank(getPosition(), ally, stats);
    }

    public void createTank(Vector2 spawnPosition, boolean isAlly, EntityStats stats){ {
        Entity tank = engine.createEntity();

        tank.add(statsComponent);
        tank.add(cameraComponent);
        tank.add(chunkComponent);
        //enemy sprite is 30x50 now is 76x128

        //transformComponent.color = carColors[carTypeIndex];
        TransformComponent transformComponent = new TransformComponent(chunkComponent.world, skin.getSprite("tank"),itemSize *2, (int) (itemSize *1.2f), (isAlly)? Color.GREEN : null,
             true, (isAlly)? ALLY : ENEMY, spawnPosition, 0f,2);

        tank.add(transformComponent);
        //(float) statsComponent.getStars() /15
        EnemyComponent enemyComponent = new EnemyComponent(0f, stats.clone());
        transformComponent.addEntityStats(enemyComponent.stats);
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

