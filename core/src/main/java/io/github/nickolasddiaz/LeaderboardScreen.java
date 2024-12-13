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

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        Table table = new Table();

        Table topTable = new Table();
        topTable.setFillParent(true);
        topTable.top();
        Label titleLabel = new Label("Leaderboard", skin);
        titleLabel.setAlignment(Align.center);
        TextButton backButton = new TextButton("Back to Main Menu", skin);

        topTable.add(backButton).expandX().pad(10); // Add padding for aesthetics
        stage.addActor(topTable);

        table.add(titleLabel).padBottom(20);
        String[] players = {"Player 1", "Player 2", "Player 3", "Player 4", "Player 5","Player 6", "Player 7", "Player 8", "Player 9", "Player 10, Player 11"};
        int[] scores = {1000, 850, 750, 600, 500, 450, 400, 350, 300, 250, 200};
        for(int x = 0; x < 10; x++)
        for (int i = 0; i < players.length; i+=2) {
            Label rankLabel = new Label((i + 1) + ".", skin);
            Label playerLabel = new Label(players[i], skin);
            Label scoreLabel = new Label(String.valueOf(scores[i]), skin);
            Label rankLabel2 = new Label((i + 1) + ".", skin);
            Label playerLabel2 = new Label(players[i+1], skin);
            Label scoreLabel2 = new Label(String.valueOf(scores[i+1]), skin);
            table.add(rankLabel).padRight(20);
            table.add(playerLabel).expandX().left();
            table.add(scoreLabel).padLeft(20);
            table.add(rankLabel2).padRight(20);
            table.add(playerLabel2).expandX().left();
            table.add(scoreLabel2).padLeft(20);
            table.row().padTop(0);
        }
        table.row().padTop(20);

        ScrollPane scrollPane = new ScrollPane(table, skin);
        scrollPane.setFillParent(true);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setFillParent(true);
        scrollPane.getStyle().background = null;
        topTable.add(backButton).expandX().pad(10); // Add padding for aesthetics
        stage.addActor(scrollPane);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        game.chunkManager.updateCamera(0, 0);
        game.batch.begin();
        game.chunkManager.renderChunks();
        if(game.DEBUG)
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
        stage.dispose();
    }
}
