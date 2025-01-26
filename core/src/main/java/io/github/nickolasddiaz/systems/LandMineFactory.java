package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.World;
import io.github.nickolasddiaz.components.LandMineComponent;
import io.github.nickolasddiaz.components.TransformComponent;
import io.github.nickolasddiaz.utils.CollisionObject;

import java.util.Random;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class LandMineFactory {

    private final World<CollisionObject> world;
    private final Engine engine;
    private final TextureAtlas atlas;
    private final Random random;

    public LandMineFactory(World<CollisionObject> world, Engine engine, TextureAtlas atlas, Random random) {
        this.world = world;
        this.engine = engine;
        this.atlas = atlas;
        this.random = random;
    }

    public void createLandMine(Vector2 position, int damage) {
        Entity landMine = new Entity();

        TransformComponent transform = new TransformComponent(
            atlas.createSprite("mine" + (random.nextInt(3) + 1)),
            itemSize,
            itemSize,
            null,
            true,
            "MINE",
            world,
            position,
            0,
            damage
        );
        landMine.add(transform);

        LandMineComponent landMineComponent = new LandMineComponent(damage);
        landMine.add(landMineComponent);

        engine.addEntity(landMine);
    }
}
