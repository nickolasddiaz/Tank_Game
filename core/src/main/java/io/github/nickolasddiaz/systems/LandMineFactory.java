package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.LandMineComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import java.util.Random;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.CollisionCategory.P_BULLET;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class LandMineFactory {

    private final World world;
    private final Engine engine;
    private final Skin skin;
    private final Random random;

    public LandMineFactory(World world, Engine engine, Skin skin, Random random) {
        this.world = world;
        this.engine = engine;
        this.skin = skin;
        this.random = random;
    }

    public void createLandMine(Vector2 position, int damage, boolean team) {
        Entity landMine = new Entity();

        short categoryBits = (team ? P_MINE : E_MINE);

        TransformComponent transform = new TransformComponent(
            world,
            skin.getSprite(Type(random, P_MINE)), // Random mine sprite from 1 to 3
            itemSize,
            itemSize,
            teamColor(team),
            true,
            categoryBits,
            position,
            0,
            damage
        );
        transform.timeToLive = 50f; //set mine time to live
        landMine.add(transform);

        LandMineComponent landMineComponent = new LandMineComponent(damage);
        landMine.add(landMineComponent);

        engine.addEntity(landMine);
    }
}
