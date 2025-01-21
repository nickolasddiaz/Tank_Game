package io.github.nickolasddiaz;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.systems.*;

import static io.github.nickolasddiaz.utils.MapGenerator.TILE_SIZE;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;


public class yourgame extends Game {

    public SpriteBatch batch;
    public ScreenViewport viewport;
    public Engine engine;
    public SettingsComponent settings;
    public CarFactory carFactory;
    public CameraComponent camera;
    public TransformComponent transform;
    public Entity car;
    public EnemyFactory enemyFactory;
    public StatsComponent statsComponent;
    public ChunkComponent chunk;
    public TextureAtlas atlas;
    public Entity player;
    public BulletFactory bulletFactory;
    public FitViewport stageViewport;



    public void create() {
        engine = new Engine();
        batch = new SpriteBatch();
        stageViewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Create player entity with properly initialized components
        player = new Entity();
        this.atlas = new TextureAtlas(Gdx.files.internal("ui_tank_game.atlas"));
        //tank sprite is 30x50 now is 76x128

        // Add other components
        PlayerComponent playerComponent = new PlayerComponent();
        settings = new SettingsComponent();
        player.add(playerComponent);
        chunk = new ChunkComponent();
        player.add(chunk);
        //tank size is 30 width and 50 height
        transform =new TransformComponent(new Sprite(atlas.findRegion("tank")),itemSize *2, (int) (itemSize *1.2f),null, true, "PLAYER", chunk.world, new Vector2(0f,0f), 0f,1000);
        player.add(transform);
        camera = new CameraComponent();
        player.add(camera);
        statsComponent = new StatsComponent();
        player.add(statsComponent);
        player.add(new JoystickComponent());
        player.add(settings);

        // Add entity to engine
        engine.addEntity(player);
        carFactory = new CarFactory(engine, atlas, camera, chunk);
        engine.addSystem(new ChunkSystem(carFactory, transform));
        car = carFactory.createTank(transform);

        viewport = new ScreenViewport();
        viewport.setWorldSize(Gdx.graphics.getWidth() * TILE_SIZE, Gdx.graphics.getHeight() * TILE_SIZE);
        this.setScreen(new MainMenuScreen(this));

        engine.addSystem(new CarSystem(engine, chunk));
        engine.addSystem(new SpriteRenderSystem(batch,camera,settings, chunk.shapeRenderer, engine));
        bulletFactory = new BulletFactory(chunk.world, engine, atlas);
        enemyFactory = new EnemyFactory(engine, atlas, camera, chunk, statsComponent, transform, settings,playerComponent, bulletFactory,chunk);
        transform.turretComponent(
            new Sprite(atlas.findRegion("turret")),
            new Vector2(itemSize*1.1f, itemSize * 0.5f),  // Position at center of tank
            itemSize*1.48f,                                // turret width
            itemSize                                 // turret height
        ); //52 width 20 height modified 77 width and 20 height for better axis rotation


    }

    public void render() {
        super.render();
    }

    public void dispose() {
        batch.dispose();
    }

}
