package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.BulletComponent;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.components.SettingsComponent;
import io.github.nickolasddiaz.components.TransformComponent;
import io.github.nickolasddiaz.utils.EntityStats;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.CollisionCategory.CAR;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class BulletFactory {
    private final World world;
    private final Engine engine;
    private final Skin skin;
    private final ChunkComponent chunk;
    private final SettingsComponent settings;
    private final Sound bulletSound;

    public BulletFactory(World world, Engine engine, Skin skin, ChunkComponent chunk, SettingsComponent settings) {
        this.world = world;
        this.engine = engine;
        this.skin = skin;
        this.chunk = chunk;
        this.settings = settings;
        bulletSound = Gdx.audio.newSound(Gdx.files.internal("sounds/shoot_bullet.mp3"));
    }

    public void createBullet(Vector2 position, float rotation, float speed, int damage, float size, Color color, boolean team) {
        Entity entity = new Entity();

        short categoryBits = team ? P_BULLET : E_BULLET;

        // Play bullet sound
        bulletSound.play(settings.sfxVolume);


        TransformComponent transform = new TransformComponent(
            world,
            skin.getSprite(Type(chunk.random, P_BULLET)),
            (int) (itemSize * size),
            (int) (itemSize * size),
            color,
            true,      // isDynamic
            categoryBits,
            position,
            rotation,
            damage
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
