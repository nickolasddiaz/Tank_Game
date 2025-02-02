package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.TransformComponent;
import io.github.nickolasddiaz.utils.EntityStats;

import static io.github.nickolasddiaz.utils.CollisionCategory.E_BULLET;
import static io.github.nickolasddiaz.utils.CollisionCategory.P_BULLET;
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

    public void createBullet(Vector2 position, float rotation, float speed, EntityStats damage, float size, Color color, boolean team) {
        Entity entity = new Entity();

        short categoryBits = team ? P_BULLET : E_BULLET;


        TransformComponent transform = new TransformComponent(
            world,
            skin.getSprite("bullet"),
            (int) (itemSize * size),
            (int) (itemSize * size),
            color,
            true,      // isDynamic
            categoryBits,
            position,
            rotation,
            damage.calculateDamage()
        );

        // Set up bullet-specific physics properties
        transform.body.setBullet(true);
        transform.body.setGravityScale(0);

        // Add components to entity
        entity.add(transform);
        entity.add(new BulletComponent(speed));

        engine.addEntity(entity);
    }
}
