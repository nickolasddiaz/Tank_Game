package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class LeaderboardScreen implements Screen {
    private final yourgame game;
    private final Stage stage;

    public LeaderboardScreen(final yourgame game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin(Gdx.files.internal("ui_tank_game.json"));

        // Main root table
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // Top section for title and button
        Table topTable = new Table();
        rootTable.add(topTable).top().expandX().pad(10).row();

        Label titleLabel = new Label("Leaderboard", skin, "title");
        topTable.add(titleLabel).expandX().padBottom(10);

        // Back button
        ImageTextButton backButton = new ImageTextButton("Back to Main Menu", skin);
        topTable.row();
        topTable.add(backButton).center().padTop(10);

        // Leaderboard Table inside a ScrollPane
        Table leaderboardTable = new Table();
        ScrollPane scrollPane = new ScrollPane(leaderboardTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsVisible(true);
        scrollPane.getStyle().background = null;

        rootTable.add(scrollPane).expand().fill().pad(20).row();

        // Player data
        String[] players = {"Player 1", "Player 2", "Player 3", "Player 4", "Player 5",
            "Player 6", "Player 7", "Player 8", "Player 9", "Player 10", "Player 11"};
        int[] scores = {1000, 850, 750, 600, 500, 450, 400, 350, 300, 250, 200};

        // Add player leaderboard entries
        for (int i = 0; i < players.length; i++) {
            Label rankLabel = new Label((i + 1) + ".", skin);
            Label playerLabel = new Label(players[i], skin);
            Label scoreLabel = new Label(String.valueOf(scores[i]), skin);

            leaderboardTable.add(rankLabel).padRight(10).width(50).align(Align.left);
            leaderboardTable.add(playerLabel).expandX().align(Align.left).padRight(20);
            leaderboardTable.add(scoreLabel).align(Align.right).width(100).row();
        }

        // Back button functionality
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
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
        game.chunkManager.updateCamera(0, 0);

        game.batch.begin();
        game.chunkManager.renderChunks();
        if (game.DEBUG)
            game.chunkManager.debugRenderChunkBoundaries(game);
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
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
