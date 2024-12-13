package io.github.nickolasddiaz;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;


public class yourgame extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    public ScreenViewport viewport;
    public ChunkManager chunkManager;
    public int musicVolume = 50;
    public int sfxVolume = 50;
    public boolean IS_MOBILE;
    public boolean DEBUG = false;


    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        viewport = new ScreenViewport();
        viewport.setWorldSize(2, 1);
        chunkManager = new ChunkManager(this);
        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());
        this.setScreen(new MainMenuScreen(this));
        IS_MOBILE = Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS;
    }

    public void render() {
        super.render();
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
    }

}
