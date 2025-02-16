package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static io.github.nickolasddiaz.utils.MapGenerator.TILE_PER_METER;
import static io.github.nickolasddiaz.utils.MapGenerator.TILE_SIZE;

public class CameraComponent implements Component {
    public OrthographicCamera camera;
    public Rectangle cameraBounds;
    public SpriteBatch batch;
    public ScreenViewport viewport;

    public CameraComponent() {
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);

        camera.setToOrtho(false, Gdx.graphics.getWidth() * TILE_SIZE, Gdx.graphics.getHeight() * TILE_SIZE);
        viewport.update((int) (Gdx.graphics.getWidth() * TILE_SIZE  / TILE_PER_METER), (int) (Gdx.graphics.getHeight() * TILE_SIZE  / TILE_PER_METER), true);
        cameraBounds = new Rectangle();
        batch = new SpriteBatch();

    }
}
