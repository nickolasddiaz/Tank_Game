package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.MissileComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class MissileFactory {

    private final Engine engine;
    private final Skin skin;
    private final ChunkComponent chunk;

    public MissileFactory(Engine engine, Skin skin, ChunkComponent chunk) {
        this.engine = engine;
        this.skin = skin;
        this.chunk = chunk;
    }

    public void spawnMissile(Vector2 position, float rotation, float speed, int damage, float size, Color color, boolean team) {
        Entity missile = new Entity();

        short categoryBits = (team ? P_MISSILE : E_MISSILE);

        // Create transform component with missile properties
        TransformComponent transform = new TransformComponent(
            chunk.world,
            skin.getSprite("missile"),
            (int) (itemSize*size),
            (int) (itemSize*size),
            color,
            true, // isPolygon for rotation
            categoryBits,
            position,
            rotation,
            damage // its health is the damage
        );
        missile.add(transform);

        MissileComponent missileComponent = new MissileComponent(speed, chunk,team);
        missile.add(missileComponent);

        engine.addEntity(missile);
    }
}
