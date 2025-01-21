package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.systems.*;

public class GameScreen implements Screen {
    private final yourgame game;
    private OptionsScreen optionsScreen;
    float seconds = 0;
    Stage stage = new Stage();


    public GameScreen(final yourgame game) {
        this.game = game;
        game.engine.removeEntity(game.car);

        game.engine.addSystem(new PlayerSystem(game.settings, game.chunk,game.bulletFactory,game.statsComponent));
        game.engine.addSystem(new StatsRenderSystem());
        if(game.settings.IS_MOBILE) {
            game.engine.addSystem(new JoystickInputSystem());
        }

        game.engine.addSystem(new BulletSystem(game.engine, game.chunk));
        Skin skin = new Skin(Gdx.files.internal("ui_tank_game.json"));

        Button pauseButton = new Button(skin);
        pauseButton.setStyle(skin.get("pause", Button.ButtonStyle.class));
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
        seconds += delta;
        game.engine.update(delta);
        stage.act(delta);
        stage.draw();

        if (game.settings.paused) {
            if (optionsScreen == null) {
                optionsScreen = new OptionsScreen(game, false);  // false means don't dispose GameScreen
            }
            optionsScreen.render(delta);

            return;
        }else{
            if (optionsScreen != null) {
                optionsScreen = null;
                Gdx.input.setInputProcessor(stage);
            }
        }
        if(game.statsComponent.getHealth() <= 0){
            game.setScreen(new DeathScreen(game, game.statsComponent.getScore()));
        }


        float spawn = 2f / (game.statsComponent.getScore() / 3f+ 1f);
        while(seconds > spawn){
           seconds -= spawn;
           game.enemyFactory.createTank(0, game.transform.position);
        }

    }

    private void togglePause() {
        game.settings.paused = !game.settings.paused;
        if (!game.settings.paused && optionsScreen != null) {

            optionsScreen.dispose();
            optionsScreen = null;
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
