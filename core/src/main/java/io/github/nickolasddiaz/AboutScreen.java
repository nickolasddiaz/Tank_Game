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
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingButton;

public class AboutScreen implements Screen {
    private final yourgame game;
    private final Stage stage;

    public AboutScreen(final yourgame game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        float titleSize = Gdx.graphics.getWidth() / 400f;

        // Create a table for layout
        Table table = new Table();
        table.setFillParent(true);

        // Add title
        Label titleLabel = new Label("About", game.skin);
        titleLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));
        titleLabel.setAlignment(Align.center);
        titleLabel.setFontScale(titleSize);
        titleLabel.setFontScale(titleSize);

        // Game description
        Label descriptionLabel = new Label("Tank Game\nCreated by: Nickolas Diaz", game.skin);
        descriptionLabel.setAlignment(Align.center);
        descriptionLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));

        // Add clickable links using TextButtons
        TypingButton websiteButton = new TypingButton("Personal Website", game.skin);
        websiteButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
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

        TypingButton githubButton = new TypingButton("Game GitHub", game.skin);

        githubButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
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

        // Credits Button
        ImageTextButton creditsButton = new ImageTextButton("Artwork Used", game.skin);
        creditsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                game.setScreen(new CreditsScreen(game));
                dispose();
            }
        });

        // Back Button
        ImageTextButton backButton = new ImageTextButton("Back to Main Menu", game.skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // Add components to the table
        table.add(titleLabel).padBottom(20).row();
        table.add(descriptionLabel).padBottom(10).row();
        table.add(websiteButton).padBottom(10).row();
        table.add(githubButton).padBottom(10).row();
        creditsButton.getLabel().setFontScale(Gdx.graphics.getWidth() / 1000f);
        table.add(creditsButton).padTop(20).size(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 10f).row();
        backButton.getLabel().setFontScale(Gdx.graphics.getWidth() / 1000f);
        table.add(backButton).padTop(20).size(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 10f);




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

