package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import io.github.nickolasddiaz.systems.JoystickInputSystem;
import io.github.nickolasddiaz.systems.PlayerMovementSystem;
import io.github.nickolasddiaz.systems.StatsRenderSystem;

public class GameScreen implements Screen {
    private final yourgame game;
    private OptionsScreen optionsScreen;

    public GameScreen(final yourgame game) {
        this.game = game;
        game.engine.removeEntity(game.car);
        game.engine.addSystem(new PlayerMovementSystem());
        game.engine.addSystem(new StatsRenderSystem(game.batch));
        if(game.settings.IS_MOBILE) {
            game.engine.addSystem(new JoystickInputSystem());
        }
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (game.settings.paused) {
            if (optionsScreen == null) {
                optionsScreen = new OptionsScreen(game, false);  // false means don't dispose GameScreen
            }
            optionsScreen.render(delta);
            return;
        }

        game.engine.update(delta);
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
