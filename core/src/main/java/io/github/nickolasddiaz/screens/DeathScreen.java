package io.github.nickolasddiaz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.nickolasddiaz.yourgame;

import java.util.ArrayList;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;

public class DeathScreen implements Screen {
    private final yourgame game;
    private final Stage stage;
    private final int finalScore;
    private final TextField nameField;
    private final ImageTextButton submitButton;
    private boolean scoreSubmitted = false;

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
        float titleSize = Gdx.graphics.getWidth() / 400f;
        float textSize = Gdx.graphics.getWidth() / 1000f;

        // Create UI elements
        Label gameOverLabel = new Label("GAME OVER", game.skin);
        Label scoreLabel = new Label("Score: " + finalScore, game.skin);

        // Name input elements
        Preferences pref = Gdx.app.getPreferences(PLAYER_NAME);
        String name = pref.getString(PLAYER_NAME, "Enter Your Name:");

        nameField = new TextField(name, game.skin);
        submitButton = new ImageTextButton("Save Score", game.skin);

        // Navigation buttons
        ImageTextButton mainMenuButton = new ImageTextButton("Main Menu", game.skin);
        ImageTextButton retryButton = new ImageTextButton("Try Again", game.skin);
        ImageTextButton leaderboardButton = new ImageTextButton("View Leaderboard", game.skin);

        // Set font scales
        mainMenuButton.getLabel().setFontScale(textSize);
        retryButton.getLabel().setFontScale(textSize);
        submitButton.getLabel().setFontScale(textSize);
        leaderboardButton.getLabel().setFontScale(textSize);

        // Style and position the labels
        gameOverLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));
        gameOverLabel.setAlignment(1);
        gameOverLabel.setFontScale(titleSize);
        gameOverLabel.setSize(buttonWidth, buttonHeight);
        gameOverLabel.setPosition(buttonInitialX, buttonInitialY + buttonSpacing * 2.5f);

        scoreLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));
        scoreLabel.setAlignment(1);
        scoreLabel.setFontScale(titleSize);
        scoreLabel.setSize(buttonWidth, buttonHeight);
        scoreLabel.setPosition(buttonInitialX, buttonInitialY + buttonSpacing * 1.5f);


        nameField.setSize(buttonWidth, buttonHeight / 2);
        nameField.setPosition(buttonInitialX, buttonInitialY + buttonSpacing);

        submitButton.setSize(buttonWidth, buttonHeight);
        submitButton.setPosition(buttonInitialX, buttonInitialY);

        // Style and position the navigation buttons
        mainMenuButton.setSize(buttonWidth, buttonHeight);
        retryButton.setSize(buttonWidth, buttonHeight);
        leaderboardButton.setSize(buttonWidth, buttonHeight);

        mainMenuButton.setPosition(buttonInitialX, buttonInitialY - buttonSpacing);
        retryButton.setPosition(buttonInitialX, buttonInitialY - buttonSpacing * 2f);
        leaderboardButton.setPosition(buttonInitialX, buttonInitialY - buttonSpacing * 3);

        // Add listeners
        submitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                if (!nameField.getText().isEmpty() && !scoreSubmitted) {
                    saveScore(nameField.getText(), finalScore);
                    scoreSubmitted = true;
                    submitButton.setDisabled(true);
                    nameField.setDisabled(true);
                    cleanupGame();
                    game.setScreen(new MainMenuScreen(game));
                    dispose();
                }
            }
        });

        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                cleanupGame();
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        retryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                cleanupGame();
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });

        leaderboardButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                cleanupGame();
                game.setScreen(new LeaderboardScreen(game));
                dispose();
            }
        });

        // Add actors to stage
        stage.addActor(gameOverLabel);
        stage.addActor(scoreLabel);
        stage.addActor(nameField);
        stage.addActor(submitButton);
        stage.addActor(mainMenuButton);
        stage.addActor(retryButton);
        stage.addActor(leaderboardButton);
    }

    // Instead of using Json class for serialization
    private void saveScore(String playerName, int score) {
        Preferences pref = Gdx.app.getPreferences(SCORE_NAME);

        // Get existing scores
        ArrayList<PlayerScore> scores = getScoresFromPreferences();

        // Add new score
        scores.add(new PlayerScore(playerName, score));

        // Save player name
        Preferences name = Gdx.app.getPreferences(PLAYER_NAME);
        name.putString(PLAYER_NAME, playerName);
        name.flush();

        // Sort scores (highest first)
        scores.sort((p1, p2) -> Integer.compare(p2.score, p1.score));

        // Save as individual key-value pairs instead of a single JSON string
        pref.putInteger("scoreCount", scores.size());
        for (int i = 0; i < scores.size(); i++) {
            pref.putString("name" + i, scores.get(i).name);
            pref.putInteger("score" + i, scores.get(i).score);
        }
        pref.flush();
    }

    public static ArrayList<PlayerScore> getScoresFromPreferences() {
        Preferences pref = Gdx.app.getPreferences(SCORE_NAME);
        ArrayList<PlayerScore> scores = new ArrayList<>();

        int count = pref.getInteger("scoreCount", 0);
        for (int i = 0; i < count; i++) {
            String name = pref.getString("name" + i, "");
            int score = pref.getInteger("score" + i, 0);
            scores.add(new PlayerScore(name, score));
        }

        return scores;
    }

    private void cleanupGame() {
        // Reset the entity system
        game.engine.removeAllEntities();
        game.create(); // This will reinitialize the core game components
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        game.updateChunk(delta);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        game.stageViewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
        Gdx.input.setInputProcessor(stage);
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

    //Class to store player score data
    public static class PlayerScore {
        public String name;
        public int score;
        public PlayerScore() {} // Required empty constructor for Json deserialization

        public PlayerScore(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }
}
