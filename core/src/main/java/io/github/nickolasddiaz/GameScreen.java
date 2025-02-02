package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import io.github.nickolasddiaz.systems.*;
import io.github.nickolasddiaz.utils.EntityStats;

public class GameScreen implements Screen {
    private final yourgame game;
    private OptionsScreen optionsScreen;
    private PowerUpScreen powerUpScreen;
    float seconds = 0;
    Stage stage = new Stage();


    public GameScreen(final yourgame game) {
        this.game = game;
        game.engine.removeEntity(game.car);

        game.engine.addSystem(new PlayerSystem(game.settings, game.chunk));
        game.engine.addSystem(new StatsRenderSystem(game.skin));
        if(game.settings.IS_MOBILE) {
            game.engine.addSystem(new JoystickInputSystem(game.skin));
        }

        game.engine.addSystem(new BulletSystem(game.engine, game.chunk));
        game.engine.addSystem(new MissileSystem(game.engine, game.chunk));

        Button pauseButton = new Button(game.skin);
        pauseButton.setStyle(game.skin.get("pause", Button.ButtonStyle.class));
        pauseButton.setSize(Gdx.graphics.getWidth() / 10f, Gdx.graphics.getHeight() / 7f);
        pauseButton.setPosition(Gdx.graphics.getWidth() - pauseButton.getWidth()*1.2f, Gdx.graphics.getHeight() - pauseButton.getHeight()*1.2f);
        stage.addActor(pauseButton);
        Gdx.input.setInputProcessor(stage);
        pauseButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                Gdx.app.log("Pause", "Toggled");
                game.settings.paused = !game.settings.paused;
            }
        });
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            togglePause();
        }

        // Render paused state UI
        if (game.statsComponent.upgrade) {
            if (powerUpScreen == null) {
                powerUpScreen = new PowerUpScreen(game,game.chunk.random);
            }
            powerUpScreen.render(delta);
            return;
        }

        // Render paused state UI
        if (game.settings.paused) {
            if (optionsScreen == null) {
                optionsScreen = new OptionsScreen(game, false); // false means don't dispose GameScreen
            }
            optionsScreen.render(delta);
            return;
        }

        // Regular gameplay rendering
        if (optionsScreen != null) {
            optionsScreen = null;
            Gdx.input.setInputProcessor(stage);
        }
        if (powerUpScreen != null) {
            powerUpScreen = null;
        }

        seconds += delta;
        if(!game.settings.paused)
            game.updateGame(delta);
        stage.act(delta);
        stage.draw();

        if (game.engine.getSystem(PlayerSystem.class).getEntities().size() == 0) {
            game.statsComponent.setHealthLevel(0);
            game.setScreen(new DeathScreen(game, game.statsComponent.getScore()));
        }

        //float spawn = 40f / (game.statsComponent.getStars()/5f) ;
        float spawn = 4;
        while (seconds > spawn) {
            seconds -= spawn;
            game.enemyFactory.createTank(false, new EntityStats(game.chunk.random, false, game.bulletFactory, game.missileFactory, game.landMineFactory, game.enemyFactory, game.chunk, game.statsComponent.getStars()));
        }
    }

    private void togglePause() {
        game.settings.paused = !game.settings.paused;
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
