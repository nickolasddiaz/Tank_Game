package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TypingButton;

public class CreditsScreen extends InputAdapter implements Screen {
    private final yourgame game;
    private final Stage stage;

    public CreditsScreen(final yourgame game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        float titleSize = Gdx.graphics.getWidth() / 400f;

        // Create a table for layout
        Table table = new Table();
        table.setFillParent(true);

        // Add title
        Label titleLabel = new Label("Credits", game.skin);
        titleLabel.setStyle(game.skin.get("title", Label.LabelStyle.class));
        titleLabel.setAlignment(Align.center);
        titleLabel.setFontScale(titleSize);


        // Back Button
        ImageTextButton aboutButton = new ImageTextButton("Back to About Menu", game.skin);
        aboutButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                game.setScreen(new AboutScreen(game));
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

        // Create clickable links for sources
        TypingButton[] sourceButtons = new TypingButton[] {
            createLinkButton("3dpyramid - Joysticks", "https://3dpyramid.itch.io/simple-joystick-free"),
            createLinkButton("marcusvh - 2d Cars", "https://marcusvh.itch.io/2d-cars"),
            createLinkButton("jimhatama - Tanks", "https://jimhatama.itch.io/ww2-pixel-top-view-tanks"),
            createLinkButton("applestreetg - Missiles", "https://applestreetg.itch.io/2d-stylized-missile-pack"),
        };

        // Add components to the table
        table.add(titleLabel).padBottom(20).row();

        // Add source buttons
        for (TypingButton button : sourceButtons) {
            table.add(button).padBottom(10).row();
        }

        aboutButton.getLabel().setFontScale(Gdx.graphics.getWidth() / 1000f);
        table.add(aboutButton).padTop(20).size(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 10f).row();
        backButton.getLabel().setFontScale(Gdx.graphics.getWidth() / 1000f);
        table.add(backButton).padTop(20).size(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 10f);

        // Add table to stage
        stage.addActor(table);
    }

    // Helper method to create clickable link buttons
    private TypingButton createLinkButton(String text, final String url) {
        TypingButton linkButton = new TypingButton(text, game.skin);
        linkButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                Gdx.net.openURI(url);
            }
        });
        linkButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                linkButton.setText("[/]" + text);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                linkButton.setText(text);
            }
        });

        return linkButton;
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
