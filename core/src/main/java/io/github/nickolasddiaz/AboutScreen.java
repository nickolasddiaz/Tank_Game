package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class AboutScreen implements Screen {
    private final yourgame game;
    private final Stage stage;

    public AboutScreen(final yourgame game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Create a table for layout
        Table table = new Table();
        table.setFillParent(true);

        // Add title
        Label titleLabel = new Label("About", skin);
        titleLabel.setAlignment(Align.center);
        titleLabel.setColor(Color.GOLD);
        titleLabel.setFontScale(2);


        // Game description
        Label descriptionLabel = new Label(
            "GTA Tank\n\n" +
                "Created by: Nickolas Diaz\n\n" +
                "Personal Website: https://nickolasddiaz.github.io/\n" +
                "Github of game: https://github.com/nickolasddiaz/Tank_Game\n" +
                "Play at: https://locationofthegame\n\n"+
                "Thank you for playing my game!",
            skin
        );
        descriptionLabel.setAlignment(Align.center);
        descriptionLabel.setColor(Color.GOLD);

        // Back Button
        TextButton backButton = new TextButton("Back to Main Menu", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // Add components to the table
        table.add(titleLabel).padBottom(20).row();
        table.add(descriptionLabel).padBottom(20).row();
        table.add(backButton);

        // Add table to stage
        stage.addActor(table);
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
