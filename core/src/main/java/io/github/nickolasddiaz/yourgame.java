package io.github.nickolasddiaz;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.systems.*;

import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;


public class yourgame extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    public ScreenViewport viewport;
    public Engine engine;
    public SettingsComponent settings;
    public CarFactory carFactory;
    public CameraComponent camera;
    public TransformComponent transform;
    public Entity car;
    public EnemyFactorySystem enemyFactorySystem;
    public StatsComponent statsComponent;
    public ChunkComponent chunk;
    public Entity player;


    public void create() {
        engine = new Engine();
        batch = new SpriteBatch();

        // Create player entity with properly initialized components
        player = new Entity();
        transform =new TransformComponent();

        player.add(transform);

        // Add other components
        settings = new SettingsComponent();
        player.add(new PlayerComponent());
        chunk = new ChunkComponent();
        player.add(chunk);
        camera = new CameraComponent();
        player.add(camera);
        statsComponent = new StatsComponent();
        player.add(statsComponent);
        player.add(new JoystickComponent());
        player.add(settings);

        // Add entity to engine
        engine.addEntity(player);

        carFactory = new CarFactory(engine, new TextureAtlas(Gdx.files.internal("ui_tank_game.atlas")), camera, chunk);
        engine.addSystem(new ChunkSystem(carFactory, transform));
        car = carFactory.createTank(transform);

        viewport = new ScreenViewport();
        viewport.setWorldSize(Gdx.graphics.getWidth() * TILE_SIZE, Gdx.graphics.getHeight() * TILE_SIZE);
        this.setScreen(new MainMenuScreen(this));

        engine.addSystem(new CarSystem(engine));
        engine.addSystem(new SpriteRenderSystem(batch));
        enemyFactorySystem = new EnemyFactorySystem(engine, new TextureAtlas(Gdx.files.internal("ui_tank_game.atlas")), camera, chunk, statsComponent, transform, settings);

    }

    public void render() {
        super.render();
    }

    public void dispose() {
        batch.dispose();
    }

}
