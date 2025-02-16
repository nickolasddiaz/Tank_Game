package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;

public class MainMenuScreen implements Screen {

    final yourgame game;
    private final Stage stage;


    public MainMenuScreen(final yourgame game) {
        float buttonHeight = Gdx.graphics.getHeight() / 10f;
        float buttonWidth = Gdx.graphics.getWidth() / 2f;
        float buttonSpacing = Gdx.graphics.getHeight() / 15f + buttonHeight;
        float buttonInitialY = Gdx.graphics.getHeight() / 2.2f - 2.5f * buttonHeight;
        float buttonInitialX = Gdx.graphics.getWidth() / 2f - buttonWidth / 2f;

        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Label titleLabel = new Label("Tank Game", game.skin);
        ImageTextButton startButton = new ImageTextButton("Start", game.skin);
        ImageTextButton optionsButton = new ImageTextButton("Options", game.skin);
        ImageTextButton leaderboardButton = new ImageTextButton("Leaderboard", game.skin);
        ImageTextButton aboutButton = new ImageTextButton("About", game.skin);
        titleLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));

        titleLabel.setAlignment(1);
        titleLabel.setFontScale(2);
        titleLabel.setSize(buttonWidth, buttonHeight);
        startButton.setSize(buttonWidth, buttonHeight);
        optionsButton.setSize(buttonWidth, buttonHeight);
        leaderboardButton.setSize(buttonWidth, buttonHeight);
        aboutButton.setSize(buttonWidth, buttonHeight);

        titleLabel.setPosition(buttonInitialX, buttonInitialY + buttonSpacing * 3);
        startButton.setPosition(buttonInitialX, buttonInitialY + buttonSpacing * 2);
        optionsButton.setPosition(buttonInitialX, buttonInitialY + buttonSpacing);
        leaderboardButton.setPosition(buttonInitialX, buttonInitialY);
        aboutButton.setPosition(buttonInitialX, buttonInitialY - buttonSpacing);

        stage.addActor(titleLabel);
        stage.addActor(startButton);
        stage.addActor(optionsButton);
        stage.addActor(leaderboardButton);
        stage.addActor(aboutButton);

        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });
        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new OptionsScreen(game, true));
                dispose();
            }
        });

        leaderboardButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LeaderboardScreen(game));
                dispose();
            }
        });

        aboutButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new AboutScreen(game));
                dispose();
            }
        });
    }
    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.updateGame(delta);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
        game.stageViewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }
    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

}

