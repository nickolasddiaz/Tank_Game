package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.tommyettinger.textra.FWSkin;

public class DeathScreen implements Screen {
    private final yourgame game;
    private final Stage stage;
    private final int finalScore;

    public DeathScreen(final yourgame game, int score) {
        this.game = game;
        this.finalScore = score;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Calculate dimensions similar to MainMenuScreen
        float buttonHeight = Gdx.graphics.getHeight() / 10f;
        float buttonWidth = Gdx.graphics.getWidth() / 2f;
        float buttonSpacing = Gdx.graphics.getHeight() / 15f + buttonHeight;
        float buttonInitialY = Gdx.graphics.getHeight() / 2f - buttonHeight;
        float buttonInitialX = Gdx.graphics.getWidth() / 2f - buttonWidth / 2f;

        // Load the skin

        // Create UI elements
        Label gameOverLabel = new Label("GAME OVER", game.skin);
        Label scoreLabel = new Label("Score: " + finalScore, game.skin);
        ImageTextButton mainMenuButton = new ImageTextButton("Main Menu", game.skin);
        ImageTextButton retryButton = new ImageTextButton("Try Again", game.skin);

        // Style and position the labels
        gameOverLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));
        gameOverLabel.setAlignment(1);
        gameOverLabel.setFontScale(2);
        gameOverLabel.setSize(buttonWidth, buttonHeight);
        gameOverLabel.setPosition(buttonInitialX, buttonInitialY + buttonSpacing * 2);

        scoreLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));
        scoreLabel.setAlignment(1);
        scoreLabel.setFontScale(1.5f);
        scoreLabel.setSize(buttonWidth, buttonHeight);
        scoreLabel.setPosition(buttonInitialX, buttonInitialY + buttonSpacing);

        // Style and position the buttons
        mainMenuButton.setSize(buttonWidth, buttonHeight);
        retryButton.setSize(buttonWidth, buttonHeight);
        mainMenuButton.setPosition(buttonInitialX, buttonInitialY);
        retryButton.setPosition(buttonInitialX, buttonInitialY - buttonSpacing);

        // Add listeners
        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                cleanupGame();
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        retryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                cleanupGame();
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });

        // Add actors to stage
        stage.addActor(gameOverLabel);
        stage.addActor(scoreLabel);
        stage.addActor(mainMenuButton);
        stage.addActor(retryButton);
    }

    private void cleanupGame() {
        // Reset the entity system
        game.engine.removeAllEntities();

        // Reset game components
        //game.chunk.reset(); // You'll need to add a reset method to ChunkComponent
        //game.statsComponent.reset(); // You'll need to add a reset method to StatsComponent

        // Recreate necessary entities
        game.create(); // This will reinitialize the core game components
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        game.engine.update(delta);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

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
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
