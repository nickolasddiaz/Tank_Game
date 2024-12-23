package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;

public class CameraComponent implements Component {
    public OrthographicCamera camera;
    public Rectangle cameraBounds;
    public SpriteBatch batch;
    public ScreenViewport viewport;

    public CameraComponent() {
        viewport = new ScreenViewport();
        viewport.setWorldSize(2, 1);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() * TILE_SIZE, Gdx.graphics.getHeight() * TILE_SIZE);
        cameraBounds = new Rectangle();
        batch = new SpriteBatch();

    }
}
