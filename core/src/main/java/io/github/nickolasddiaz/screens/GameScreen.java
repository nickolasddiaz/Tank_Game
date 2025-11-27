package io.github.nickolasddiaz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import io.github.nickolasddiaz.systems.*;
import io.github.nickolasddiaz.utils.EntityStats;
import io.github.nickolasddiaz.yourgame;

import static io.github.nickolasddiaz.utils.CollisionCategory.getEnemySpawnRate;

public class GameScreen implements Screen {
    private final yourgame game;
    private OptionsScreen optionsScreen;
    private PowerUpScreen powerUpScreen;
    float seconds = 0;
    Stage stage = new Stage();
    boolean isZooming = true;
    float zoomInt = 0f;

    public GameScreen(final yourgame game) {
        this.game = game;

        Button pauseButton = new Button(game.skin);
        pauseButton.setStyle(game.skin.get("pause", Button.ButtonStyle.class));
        pauseButton.setSize(Gdx.graphics.getWidth() / 10f, Gdx.graphics.getHeight() / 7f);
        pauseButton.setPosition(Gdx.graphics.getWidth() - pauseButton.getWidth()*1.2f, Gdx.graphics.getHeight() - pauseButton.getHeight()*1.2f);
        stage.addActor(pauseButton);
        pauseButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                game.settings.paused = !game.settings.paused;
                game.ui_sound();
            }
        });
        Gdx.input.setInputProcessor(stage);
    }
    private void setUp(){
        game.engine.removeEntity(game.car);
        game.settings.is_Playing = true;

        game.engine.addSystem(new PlayerSystem(game.settings, game.chunk));
        if(game.settings.IS_MOBILE) {
            game.engine.addSystem(new JoystickInputSystem(game.skin));
        }
        game.engine.addSystem(new BulletSystem(game.engine, game.chunk));
        game.engine.addSystem(new MissileSystem(game.engine, game.chunk));
    }

    private void zoomIn(float delta){
        if(game.camera.zoomLevel < 17){
            isZooming = false;
            game.camera.updateViewport(16);
            this.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            setUp();
        }else{
            zoomInt += delta * 16;
            if(zoomInt >= 1) {
                zoomInt--;
                game.camera.updateViewport(game.camera.zoomLevel - 1);
                this.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }

        }
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            togglePause();
        }
        if(isZooming)
            zoomIn(delta);

        // Render upgrade screen
        if (game.statsComponent.upgrade) {
            if (powerUpScreen == null) {
                resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                powerUpScreen = new PowerUpScreen(game,game.chunk.random);
            }
            powerUpScreen.render(delta);
            return;
        }

        // Render paused state UI
        if (game.settings.paused) {
            if (optionsScreen == null) {
                resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                optionsScreen = new OptionsScreen(game, false); // false means don't dispose GameScreen
            }
            optionsScreen.render(delta);
            return;
        }

        // Regular gameplay rendering
        if (optionsScreen != null) {
            optionsScreen = null;
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.input.setInputProcessor(stage);
        }
        if (powerUpScreen != null) {
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            powerUpScreen = null;
        }

        seconds += delta;
        stage.act(delta);
        if(!game.settings.paused)
            game.updateGame(delta);

        if (!isZooming)
            stage.draw();

        if (game.statsComponent.getHealth() <= 0) {
            game.statsComponent.setHealthLevel(0);
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            game.setScreen(new DeathScreen(game, game.statsComponent.getScore()));
        }

        float spawn = getEnemySpawnRate(game.statsComponent.getStars());
        while (seconds > spawn) {
            seconds -= spawn;
            game.enemyFactory.createTank(false, new EntityStats(game.chunk.random, false, game.bulletFactory, game.missileFactory, game.landMineFactory, game.enemyFactory, game.chunk, game.statsComponent.getStars()*3, true));
        }
    }

    private void togglePause() {
        game.settings.paused = !game.settings.paused;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if(game.settings.paused){
            pause();
        }else{
            resume();
        }
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
        if (optionsScreen != null) {
            optionsScreen.resize(width, height);
        }
        game.stageViewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {
        pause();
    }

    @Override
    public void pause() {
        game.settings.paused = true;
    }

    @Override
    public void resume() {
        game.settings.paused = false;
        if (optionsScreen != null) {
            optionsScreen.dispose();
            optionsScreen = null;
        }
    }

    @Override
    public void dispose() {
        if (optionsScreen != null) {
            optionsScreen.dispose();
        }
    }
}
