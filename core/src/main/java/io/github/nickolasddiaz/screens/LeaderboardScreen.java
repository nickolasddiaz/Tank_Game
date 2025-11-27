package io.github.nickolasddiaz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import io.github.nickolasddiaz.yourgame;

import java.util.ArrayList;

public class LeaderboardScreen implements Screen {
    private final yourgame game;
    private final Stage stage;

    public LeaderboardScreen(final yourgame game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Main root table
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // Top section for title and button
        Table topTable = new Table();
        rootTable.add(topTable).top().expandX().pad(10).row();

        Label titleLabel = new Label("Leaderboard", game.skin, "title");
        topTable.add(titleLabel).expandX().padBottom(10);

        // Back button
        ImageTextButton backButton = new ImageTextButton("Back to Main Menu", game.skin);
        topTable.row();
        topTable.add(backButton).center().padTop(10);

        // Leaderboard Table inside a ScrollPane
        Table leaderboardTable = new Table();
        ScrollPane scrollPane = new ScrollPane(leaderboardTable, game.skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsVisible(true);
        scrollPane.getStyle().background = null;

        rootTable.add(scrollPane).expand().fill().pad(20).row();

        // Add header row
        Label rankHeaderLabel = new Label("Rank", game.skin);
        Label playerHeaderLabel = new Label("Player", game.skin);
        Label scoreHeaderLabel = new Label("Score", game.skin);

        leaderboardTable.add(rankHeaderLabel).padRight(10).width(50).align(Align.left);
        leaderboardTable.add(playerHeaderLabel).expandX().align(Align.left).padRight(20);
        leaderboardTable.add(scoreHeaderLabel).align(Align.right).width(100).row();

        // Add separator row
        //leaderboardTable.add(new Image(game.skin.getDrawable("default-horizontal"))).colspan(3).fillX().padBottom(5).padTop(5).row();

        // Load player scores from preferences
        ArrayList<DeathScreen.PlayerScore> playerScores = DeathScreen.getScoresFromPreferences();

        // Add player leaderboard entries
        if (playerScores.isEmpty()) {
            // If no scores are saved yet, show a message
            Label noScoresLabel = new Label("No scores yet. Play the game to set records!", game.skin);
            leaderboardTable.add(noScoresLabel).colspan(3).padTop(20).row();
        } else {
            // Add all player scores
            for (int i = 0; i < playerScores.size(); i++) {
                DeathScreen.PlayerScore playerScore = playerScores.get(i);

                Label rankLabel = new Label((i + 1) + ".", game.skin);
                Label playerLabel = new Label(playerScore.name, game.skin);
                Label scoreLabel = new Label(String.valueOf(playerScore.score), game.skin);

                leaderboardTable.add(rankLabel).padRight(10).width(50).align(Align.left);
                leaderboardTable.add(playerLabel).expandX().align(Align.left).padRight(20);
                leaderboardTable.add(scoreLabel).align(Align.right).width(100).row();
            }
        }

        // Back button functionality
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.updateGame(delta);

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
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
