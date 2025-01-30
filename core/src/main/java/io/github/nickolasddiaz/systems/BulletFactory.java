package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.TransformComponent;
import io.github.nickolasddiaz.components.ChunkComponent;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class BulletFactory {
    private final World world;
    private final Engine engine;
    private final Skin skin;

    public BulletFactory(World world, Engine engine, Skin skin) {
        this.world = world;
        this.engine = engine;
        this.skin = skin;
    }

    public void createBullet(Vector2 position, float rotation, float speed, int damage, float size, Color color, boolean team) {
        Entity entity = new Entity();

        // Create transform component with bullet
        short categoryBits = (short) (ChunkComponent.BULLET +
            (team ? ChunkComponent.FROM_THE_PLAYER : 0));

        TransformComponent transform = new TransformComponent(
            world,
            skin.getSprite("bullet"),
            (int) (itemSize * size),
            (int) (itemSize * size),
            color,
            true,  // isDynamic
            categoryBits,
            position,
            rotation,
            damage
        );

        // Set up bullet-specific physics properties
        transform.body.setBullet(true);  // Enable continuous collision detection
        transform.body.setGravityScale(0);  // Bullets don't use gravity

        // Set up collision filtering
        Filter filter = new Filter();
        filter.categoryBits = categoryBits;
        filter.maskBits = (short) (ChunkComponent.STRUCTURE | ChunkComponent.OCEAN |
            ChunkComponent.CAR | ChunkComponent.ENEMY |
            ChunkComponent.PLAYER | ChunkComponent.ALLY);
        transform.body.getFixtureList().first().setFilterData(filter);

        // Add components to entity
        entity.add(transform);
        entity.add(new BulletComponent(speed));

        engine.addEntity(entity);
    }
}
