package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.FWSkin;
import com.github.tommyettinger.textra.TypingButton;

public class AboutScreen implements Screen {
    private final yourgame game;
    private final Stage stage;

    public AboutScreen(final yourgame game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Skin skin = new FWSkin(Gdx.files.internal("ui_tank_game.json"));

        // Create a table for layout
        Table table = new Table();
        table.setFillParent(true);

        // Add title
        Label titleLabel = new Label("About", skin);
        titleLabel.setStyle(skin.get("title", Label.LabelStyle.class));
        titleLabel.setAlignment(Align.center);
        titleLabel.setFontScale(2);

        // Game description
        Label descriptionLabel = new Label("GTA Tank\nCreated by: Nickolas Diaz", skin);
        descriptionLabel.setAlignment(Align.center);
        descriptionLabel.setStyle(skin.get("title", Label.LabelStyle.class));
        descriptionLabel.setFontScale(0.7f);

        // Add clickable links using TextButtons
        TypingButton websiteButton = new TypingButton("Personal Website", skin);
        websiteButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI("https://nickolasddiaz.github.io/");
            }
        });
        // Add a listener for hover and exit
        websiteButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                // Underline the text when hovered
                websiteButton.setText("[/]Personal Website");
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                // Reset to the original text when not hovered
                websiteButton.setText("Personal Website");
            }
        });

        TypingButton githubButton = new TypingButton("Game GitHub", skin);
        githubButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI("https://github.com/nickolasddiaz/Tank_Game");
            }
        });
        githubButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                githubButton.setText("[/]Game Github");
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                githubButton.setText("Game GitHub");
            }
        });

        TypingButton playGameButton = new TypingButton("Game Website", skin);
        playGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI("https://locationofthegame");
            }
        });
        playGameButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                playGameButton.setText("[/]Game Website");
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                playGameButton.setText("Game Website");
            }
        });

        // Back Button
        ImageTextButton backButton = new ImageTextButton("Back to Main Menu", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // Add components to the table
        table.add(titleLabel).padBottom(20).row();
        table.add(descriptionLabel).padBottom(10).row();
        table.add(websiteButton).padBottom(10).row();
        table.add(githubButton).padBottom(10).row();
        table.add(playGameButton).padBottom(20).row();
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
        game.engine.update(delta);

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

