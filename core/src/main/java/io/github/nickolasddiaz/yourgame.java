package io.github.nickolasddiaz;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.systems.ChunkSystem;
import io.github.nickolasddiaz.systems.RenderSystem;

import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;


public class yourgame extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    public ScreenViewport viewport;
    public Engine engine;
    public SettingsComponent settings;

    public void create() {
        engine = new Engine();
        batch = new SpriteBatch();

        // Create player entity with properly initialized components
        Entity player = new Entity();

        // Initialize transform
        TransformComponent transform = new TransformComponent();
        transform.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        player.add(transform);

        // Initialize sprite
        SpriteComponent spriteComponent = new SpriteComponent();
        spriteComponent.tankSprite = new Sprite(new Texture("tank.png"));
        spriteComponent.tankSprite.setSize(TILE_SIZE * TILE_SIZE *4, TILE_SIZE * TILE_SIZE * 4);
        player.add(spriteComponent);

        // Add other components
        settings = new SettingsComponent();
        player.add(new PlayerComponent());
        player.add(new ChunkComponent());
        player.add(new CameraComponent());
        player.add(new StatsComponent());
        player.add(new CollisionComponent());
        player.add(new JoystickComponent());
        player.add(settings);

        // Add entity to engine
        engine.addEntity(player);

        engine.addSystem(new ChunkSystem());
        engine.addSystem(new RenderSystem(batch));

        font = new BitmapFont();
        viewport = new ScreenViewport();
        viewport.setWorldSize(2, 1);
        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());
        this.setScreen(new MainMenuScreen(this));

    }

    public void render() {
        super.render();
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
    }

}
