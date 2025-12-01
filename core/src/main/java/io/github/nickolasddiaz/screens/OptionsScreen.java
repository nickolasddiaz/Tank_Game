package io.github.nickolasddiaz.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.nickolasddiaz.systems.JoystickInputSystem;
import io.github.nickolasddiaz.yourgame;

import static io.github.nickolasddiaz.utils.CollisionCategory.GAME_SETTINGS;

public class OptionsScreen implements Screen {
    private final yourgame game;
    private final Stage stage;
    private Boolean lastClickedVolumeSFX = true;
    private final Label musicVolumeLabel;
    private final Label sxfVolumeLabel;
    private final Slider musicVolumeSlider;
    private final Slider sxfVolumeSlider;
    private final boolean dispose;
    private final Preferences pref = Gdx.app.getPreferences(GAME_SETTINGS);


    public OptionsScreen(final yourgame game, boolean dispose) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        this.dispose = dispose;

        float buttonHeight = Gdx.graphics.getHeight() / 10f;
        float buttonWidth = Gdx.graphics.getWidth() / 2f;
        float buttonInitialX = Gdx.graphics.getWidth() / 2f - buttonWidth / 2f;
        float buttonInitialY = Gdx.graphics.getHeight() / 2f;
        float buttonSpacing = buttonHeight / 2;
        float textSize = Gdx.graphics.getWidth() / 1200f;

        // Volume Controls
        musicVolumeLabel = new ImageTextButton("Music Volume " + (int) (game.settings.musicVolume * 100), game.skin).getLabel();
        musicVolumeLabel.setFontScale(textSize);
        musicVolumeSlider = new Slider(0, 100, 1, false, game.skin);
        musicVolumeSlider.setValue((int) (game.settings.musicVolume * 100));
        sxfVolumeLabel = new ImageTextButton("SFX Volume " + (int) (game.settings.sfxVolume * 100), game.skin).getLabel();
        sxfVolumeLabel.setFontScale(textSize);
        sxfVolumeSlider = new Slider(0, 100, 1, false, game.skin);
        sxfVolumeSlider.setValue((int) (game.settings.sfxVolume * 100));

        Table mobileContainer = new Table();
        Table debugContainer = new Table();
        Table autoFireContainer = new Table();

        CheckBox mobileCheckBox = new CheckBox("", game.skin); // Remove text from checkbox
        CheckBox debugCheckBox = new CheckBox("", game.skin);
        CheckBox autoFireCheckBox = new CheckBox("", game.skin);

        Label mobileLabel = new Label("Mobile Controls", game.skin);
        mobileLabel.setFontScale(textSize);
        Label debugLabel = new Label("Debug Mode", game.skin);
        debugLabel.setFontScale(textSize);
        Label autoFireLabel = new Label("Auto Fire", game.skin);
        autoFireLabel.setFontScale(textSize);

        mobileCheckBox.setChecked(game.settings.IS_MOBILE);
        debugCheckBox.setChecked(game.settings.DEBUG);
        autoFireCheckBox.setChecked(game.settings.AUTO_FIRE);

        mobileContainer.add(mobileCheckBox).center().padBottom(5);
        mobileContainer.row();
        mobileContainer.add(mobileLabel).center();

        debugContainer.add(debugCheckBox).center().padBottom(5);
        debugContainer.row();
        debugContainer.add(debugLabel).center();

        autoFireContainer.add(autoFireCheckBox).center().padBottom(5);
        autoFireContainer.row();
        autoFireContainer.add(autoFireLabel).center();


        float containerWidth = buttonWidth / 3;
        float containerHeight = buttonHeight * 1.5f;

        mobileContainer.setSize(containerWidth, containerHeight);
        debugContainer.setSize(containerWidth, containerHeight);
        autoFireContainer.setSize(containerWidth, containerHeight);

        // Position the containers
        float containerY = buttonInitialY - buttonHeight/2;

        mobileContainer.setPosition(buttonInitialX, containerY);
        debugContainer.setPosition(buttonInitialX + containerWidth - buttonSpacing/4f, containerY);
        autoFireContainer.setPosition(buttonInitialX + containerWidth*2f - buttonSpacing/2f, containerY);

        // Add containers to stage instead of individual checkboxes
        stage.addActor(mobileContainer);
        stage.addActor(debugContainer);
        stage.addActor(autoFireContainer);

        Button continueButton = new Button(game.skin);
        Button mainMenuButton = new Button(game.skin);
        continueButton.setStyle(game.skin.get("play", Button.ButtonStyle.class));

        // Set button sizes and positions
        musicVolumeLabel.setSize(buttonWidth, buttonHeight);
        musicVolumeSlider.setSize(buttonWidth, buttonHeight);
        sxfVolumeLabel.setSize(buttonWidth, buttonHeight);
        sxfVolumeSlider.setSize(buttonWidth, buttonHeight);

        continueButton.setSize(buttonWidth / 4, buttonHeight * 1.5f);
        mainMenuButton.setSize(buttonWidth / 4, buttonHeight * 1.5f);

        musicVolumeLabel.setPosition(buttonInitialX, buttonInitialY + buttonHeight*2 * 3);
        musicVolumeSlider.setPosition(buttonInitialX, buttonInitialY + buttonHeight*2 * 2);
        sxfVolumeLabel.setPosition(buttonInitialX, buttonInitialY + buttonHeight *2 + 10);
        sxfVolumeSlider.setPosition(buttonInitialX, buttonInitialY + buttonHeight + 20);


        mainMenuButton.setPosition((float) Gdx.graphics.getWidth() / 2 - 6*buttonSpacing, buttonInitialY - buttonHeight - buttonSpacing * 3);
        continueButton.setPosition((float) Gdx.graphics.getWidth() / 2 + 2*buttonSpacing, buttonInitialY - buttonHeight - buttonSpacing * 3);

        // Add buttons to stage
        //stage.addActor(musicVolumeLabel);
        //stage.addActor(musicVolumeSlider);
        stage.addActor(sxfVolumeLabel);
        stage.addActor(sxfVolumeSlider);
        stage.addActor(continueButton);
        stage.addActor(mainMenuButton);

        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                if(dispose) {
                    game.setScreen(new MainMenuScreen(game));
                }else {
                    cleanupGame();
                    game.settings.paused = false;
                    game.statsComponent.setHealthLevel(0);
                    game.setScreen(new MainMenuScreen(game));
                    dispose();
                }
            }
        });
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                if (dispose) {
                    game.setScreen(new GameScreen(game));
                }
                game.settings.paused = false; // Unpause the game
                dispose();
            }
        });
        musicVolumeSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                setMusicVolume((int) musicVolumeSlider.getValue());
                pref.putFloat("musicVolume", musicVolumeSlider.getValue() / 100f);
                pref.flush();
                lastClickedVolumeSFX = true;
            }
        });
        sxfVolumeSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                setSFXVolume((int) sxfVolumeSlider.getValue());
                pref.putFloat("sfxVolume", sxfVolumeSlider.getValue() / 100f);
                pref.flush();
                lastClickedVolumeSFX = false;
            }
        });
        debugCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                game.settings.DEBUG = debugCheckBox.isChecked();
                pref.putBoolean("DEBUG", debugCheckBox.isChecked());
                pref.flush();
            }
        });
        mobileCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                game.settings.IS_MOBILE = mobileCheckBox.isChecked();
                pref.putBoolean("IS_MOBILE", mobileCheckBox.isChecked());
                pref.flush();
                if(game.settings.IS_MOBILE) {
                    game.engine.addSystem(new JoystickInputSystem(game.skin));
                }
                else{
                    game.engine.removeSystem(game.engine.getSystem(JoystickInputSystem.class));
                }
            }
        });
        autoFireCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.ui_sound();
                game.settings.AUTO_FIRE = autoFireCheckBox.isChecked();
                pref.putBoolean("AUTO_FIRE", game.settings.AUTO_FIRE);
                pref.flush();
            }
        });

    }

    private void setSFXVolume(int value) {
        if (value < 0 || value > 100) {
            return;
        }
        game.settings.sfxVolume = value / 100f;
        sxfVolumeLabel.setText("SFX Volume: " + value);
        sxfVolumeSlider.setValue(value);
    }

    private void setMusicVolume(int value) {
        if (value < 0 || value > 100) {
            return;
        }
        game.settings.musicVolume = value / 100f;
        musicVolumeLabel.setText("Music Volume: " + value);
        musicVolumeSlider.setValue(value);
    }

    @Override
    public void render(float delta) {
        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            if (lastClickedVolumeSFX) {
                setMusicVolume((int) (game.settings.musicVolume * 100 + 1));
            } else {
                setSFXVolume((int) (game.settings.sfxVolume * 100 + 1));
            }
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            if (lastClickedVolumeSFX) {
                setMusicVolume((int) (game.settings.musicVolume * 100 - 1));
            } else {
                setSFXVolume((int) (game.settings.sfxVolume * 100 - 1));
            }
        }
        if(dispose)
            game.updateGame(delta);
        else
            game.updateChunk(delta);

        try {
            stage.act(delta);
            stage.draw();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanupGame() {
        // Reset the entity system
        game.engine.removeAllEntities();
        game.create(); // This will reinitialize the core game components
    }

    @Override
    public void resize(int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }
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

    @Override
    public void show() {
    }
}
