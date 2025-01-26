package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.World;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.MissileComponent;
import io.github.nickolasddiaz.components.TransformComponent;
import io.github.nickolasddiaz.utils.CollisionObject;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class MissileFactory {

    private final World<CollisionObject> world;
    private final Engine engine;
    private final TextureAtlas atlas;
    private final ChunkComponent chunk;

    public MissileFactory(World<CollisionObject> world, Engine engine, TextureAtlas atlas, ChunkComponent chunk) {
        this.world = world;
        this.engine = engine;
        this.atlas = atlas;
        this.chunk = chunk;
    }

    public void spawnMissile(Vector2 position, float rotation, float speed, int damage, float size, Color color) {
        Entity missile = new Entity();

        // Create transform component with missile properties
        TransformComponent transform = new TransformComponent(
            atlas.createSprite("missile"),
            (int) (itemSize*size),
            (int) (itemSize*size),
            color,
            true, // isPolygon for rotation
            "MISSILE",
            world,
            position,
            rotation,
            damage // its health is the damage
        );
        missile.add(transform);

        MissileComponent missileComponent = new MissileComponent(speed, chunk);
        missile.add(missileComponent);

        engine.addEntity(missile);
    }
}
