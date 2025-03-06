package io.github.nickolasddiaz;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.tommyettinger.textra.FWSkin;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.systems.*;
import io.github.nickolasddiaz.utils.EntityStats;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
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
    public StatsComponent statsComponent;
    public ChunkComponent chunk;
    public Entity player;
    public BulletFactory bulletFactory;
    public MissileFactory missileFactory;
    public LandMineFactory landMineFactory;
    public EnemyFactory enemyFactory;
    public ScreenViewport stageViewport;
    public Skin skin;
    public PlayerComponent playerComponent;
    private ChunkSystem chunkSystem;

    // Sounds
    Sound ui_sound;

    public void create() {
        ui_sound = Gdx.audio.newSound(Gdx.files.internal("sounds/click_ui.mp3"));

        engine = new Engine();
        batch = new SpriteBatch();
        stageViewport = new ScreenViewport();

        // Create player entity with properly initialized components
        player = new Entity();
        skin = new FWSkin(Gdx.files.internal("ui_tank_game.json"));

        // Add other components
        if(settings == null)
            settings = new SettingsComponent();
        chunk = new ChunkComponent();
        player.add(chunk);
        //turret sprite is 50x20 while the tank sprite is 50x26 where itemSize is 25
        String tankType = Type(chunk.random, PLAYER);
        transform =new TransformComponent(chunk.world, skin.getSprite("hull" + tankType),itemSize * 2, itemSize,teamColor(true), true, PLAYER, new Vector2(0f,0f), 0f,10);
        player.add(transform);
        camera = new CameraComponent();
        player.add(camera);
        statsComponent = new StatsComponent(transform);
        chunk.stats(statsComponent, settings);
        player.add(new JoystickComponent());
        player.add(settings);
        player.add(statsComponent);

        // Add entity to engine
        engine.addEntity(player);
        carFactory = new CarFactory(engine, skin, camera, chunk);
        chunkSystem = new ChunkSystem(carFactory, transform);
        engine.addSystem(chunkSystem);
        car = carFactory.createTank(transform);

        viewport = new ScreenViewport();
        viewport.setWorldSize(Gdx.graphics.getWidth() * TILE_SIZE, Gdx.graphics.getHeight() * TILE_SIZE);
        this.setScreen(new MainMenuScreen(this));

        engine.addSystem(new CarSystem(chunk));
        engine.addSystem(new SpriteRenderSystem(batch,camera,settings, engine));
        bulletFactory = new BulletFactory(chunk.world, engine, skin, chunk, settings);
        enemyFactory = new EnemyFactory(engine, skin, camera, chunk, statsComponent, transform, settings,playerComponent ,chunk);
        transform.turretComponent(skin.getSprite("turret"+tankType));
        missileFactory = new MissileFactory(engine, skin, chunk, settings);
        landMineFactory = new LandMineFactory(chunk.world,engine,skin,chunk.random);


        playerComponent = new PlayerComponent(new EntityStats(chunk.random, true, bulletFactory, missileFactory, landMineFactory, enemyFactory, chunk));
        player.add(playerComponent);
        transform.addEntityStats(playerComponent.stats);
        engine.addSystem(new StatsRenderSystem(skin));

        Gdx.input.setCatchKey(Input.Keys.SPACE, true);
        //Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    public void render() {
        super.render();
    }

    public void dispose() {
        batch.dispose();
    }

    public void updateGame(float delta) {
        engine.update(delta);
        chunk.world.step(delta, 6, 2);
        Array<Body> bodies = new Array<>();
        Array<Body> bodiesToDestroy = new Array<>();
        chunk.world.getBodies(bodies);
        for (Body body : bodies) {
            if (body.getUserData() instanceof Boolean) {
                bodiesToDestroy.add(body);
            } else if (body.getUserData() instanceof TransformComponent && ((TransformComponent) body.getUserData()).timeToLive != null) {
                ((TransformComponent) body.getUserData()).timeToLive -= delta;
                if (((TransformComponent) body.getUserData()).timeToLive <= 0) {
                    bodiesToDestroy.add(body);
                }
            }
        }
        for (Body body : bodiesToDestroy) {
            chunk.world.destroyBody(body);
        }
    }
    public void updateChunk(float delta){
        chunkSystem.update(delta);
    }
    public void ui_sound(){
        ui_sound.play(settings.sfxVolume);
    }
}
