package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.nickolasddiaz.systems.JoystickInputSystem;

public class OptionsScreen implements Screen {
    private final yourgame game;
    private final Stage stage;
    private Boolean lastClickedVolumeSFX = true;
    private final Label musicVolumeLabel;
    private final Label sxfVolumeLabel;
    private final Slider musicVolumeSlider;
    private final Slider sxfVolumeSlider;

    public OptionsScreen(final yourgame game, boolean dispose) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        float buttonHeight = Gdx.graphics.getHeight() / 10f;
        float buttonWidth = Gdx.graphics.getWidth() / 2f;
        float buttonInitialX = Gdx.graphics.getWidth() / 2f - buttonWidth / 2f;
        float buttonInitialY = Gdx.graphics.getHeight() / 2f;
        float buttonSpacing = buttonHeight / 2;

        Skin skin = new Skin(Gdx.files.internal("ui_tank_game.json"));

        // Volume Controls
        musicVolumeLabel = new ImageTextButton("Music Volume " + game.settings.musicVolume, skin).getLabel();
        musicVolumeSlider = new Slider(0, 100, 1, false, skin);
        setMusicVolume(game.settings.musicVolume);
        sxfVolumeLabel = new ImageTextButton("SFX Volume " + game.settings.sfxVolume, skin).getLabel();
        sxfVolumeSlider = new Slider(0, 100, 1, false, skin);
        setSFXVolume(game.settings.sfxVolume);

        CheckBox mobileCheckBox = new CheckBox("Mobile Controls", skin);
        mobileCheckBox.setChecked(game.settings.IS_MOBILE);
        CheckBox debugCheckBox = new CheckBox("Debug Mode", skin);
        debugCheckBox.setChecked(game.settings.DEBUG);

        Button continueButton = new Button(skin);
        Button mainMenuButton = new Button(skin);
        continueButton.setStyle(skin.get("play", Button.ButtonStyle.class));

        // Set button sizes and positions
        musicVolumeLabel.setSize(buttonWidth, buttonHeight);
        musicVolumeSlider.setSize(buttonWidth, buttonHeight);
        sxfVolumeLabel.setSize(buttonWidth, buttonHeight);
        sxfVolumeSlider.setSize(buttonWidth, buttonHeight);
        mobileCheckBox.setSize(buttonWidth / 2, buttonHeight);
        debugCheckBox.setSize(buttonWidth / 2, buttonHeight);
        continueButton.setSize(buttonWidth / 4, buttonHeight * 1.5f);
        mainMenuButton.setSize(buttonWidth / 4, buttonHeight * 1.5f);

        musicVolumeLabel.setPosition(buttonInitialX, buttonInitialY + buttonHeight * 3);
        musicVolumeSlider.setPosition(buttonInitialX, buttonInitialY + buttonHeight * 2);
        sxfVolumeLabel.setPosition(buttonInitialX, buttonInitialY + buttonHeight);
        sxfVolumeSlider.setPosition(buttonInitialX, buttonInitialY);
        mobileCheckBox.setPosition(buttonInitialX, buttonInitialY - buttonHeight - buttonSpacing);
        debugCheckBox.setPosition(buttonInitialX + buttonWidth / 2 + buttonSpacing, buttonInitialY - buttonHeight - buttonSpacing);
        continueButton.setPosition((float) Gdx.graphics.getWidth() / 2 - buttonSpacing * 4.5f, buttonInitialY - buttonHeight * 2 - buttonSpacing * 3);
        mainMenuButton.setPosition((float) Gdx.graphics.getWidth() / 2 + buttonSpacing, buttonInitialY - buttonHeight * 2 - buttonSpacing * 3);

        // Add buttons to stage
        stage.addActor(musicVolumeLabel);
        stage.addActor(musicVolumeSlider);
        stage.addActor(sxfVolumeLabel);
        stage.addActor(sxfVolumeSlider);
        stage.addActor(debugCheckBox);
        stage.addActor(mobileCheckBox);
        stage.addActor(continueButton);
        stage.addActor(mainMenuButton);

        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
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
                setMusicVolume((int) musicVolumeSlider.getValue());
                lastClickedVolumeSFX = true;
            }
        });
        sxfVolumeSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setSFXVolume((int) sxfVolumeSlider.getValue());
                lastClickedVolumeSFX = false;
            }
        });
        debugCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.settings.DEBUG = debugCheckBox.isChecked();
            }
        });
        mobileCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.settings.IS_MOBILE = mobileCheckBox.isChecked();
                if(game.settings.IS_MOBILE) {
                    game.engine.addSystem(new JoystickInputSystem());
                }
                else{
                    game.engine.removeSystem(game.engine.getSystem(JoystickInputSystem.class));
                }
            }
        });
    }

    private void setSFXVolume(int value) {
        if (value < 0 || value > 100) {
            return;
        }
        game.settings.sfxVolume = value;
        sxfVolumeLabel.setText("SFX Volume: " + game.settings.sfxVolume);
        sxfVolumeSlider.setValue(game.settings.sfxVolume);
    }

    private void setMusicVolume(int value) {
        if (value < 0 || value > 100) {
            return;
        }
        game.settings.musicVolume = value;
        musicVolumeLabel.setText("Music Volume: " + game.settings.musicVolume);
        musicVolumeSlider.setValue(game.settings.musicVolume);
    }

    @Override
    public void render(float delta) {
        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            if (lastClickedVolumeSFX) {
                setMusicVolume(game.settings.musicVolume + 1);
            } else {
                setSFXVolume(game.settings.sfxVolume + 1);
            }
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            if (lastClickedVolumeSFX) {
                setMusicVolume(game.settings.musicVolume - 1);
            } else {
                setSFXVolume(game.settings.sfxVolume - 1);
            }
        }
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

    @Override
    public void show() {
    }
}
