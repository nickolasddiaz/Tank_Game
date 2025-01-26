package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.World;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.TransformComponent;
import io.github.nickolasddiaz.utils.CollisionObject;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class BulletFactory {

    private final World<CollisionObject> world;
    private final Engine engine;
    private final TextureAtlas atlas;

    public BulletFactory(World<CollisionObject> world, Engine engine, TextureAtlas atlas) {
        this.world = world;
        this.engine = engine;
        this.atlas = atlas;
    }

    public void createBullet(Vector2 position, float rotation, float speed, int damage, float size, Color color, String team) {
        Entity bullet = new Entity();

        // Create transform component with bullet properties
        TransformComponent transform = new TransformComponent(
            atlas.createSprite("bullet"),
            (int) (itemSize*size),
            (int) (itemSize*size),
            color,
            true, // isPolygon for rotation
            team,
            world,
            position,
            rotation,
            10
        );
        bullet.add(transform);
        BulletComponent bulletComponent = new BulletComponent(speed, damage);
        bullet.add(bulletComponent);

        engine.addEntity(bullet);
    }
}
