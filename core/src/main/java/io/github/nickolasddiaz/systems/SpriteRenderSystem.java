package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.SortedIteratingSystem;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.nickolasddiaz.components.CameraComponent;
import io.github.nickolasddiaz.components.TransformComponent;

public class SpriteRenderSystem extends SortedIteratingSystem {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<CameraComponent> cameraMapper;

    public SpriteRenderSystem(SpriteBatch batch) {
        super(Family.all(TransformComponent.class).get(),
            (e1, e2) -> 0); // Simple comparator since we don't need sorting yet

        this.batch = batch;
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        cameraMapper = ComponentMapper.getFor(CameraComponent.class);
    }

    @Override
    public void update(float deltaTime) {
        Entity cameraEntity = getEngine().getEntitiesFor(Family.all(CameraComponent.class).get()).first();
        CameraComponent cameraComponent = cameraMapper.get(cameraEntity);

        batch.setProjectionMatrix(cameraComponent.camera.combined);
        batch.begin();
        super.update(deltaTime);
        batch.end();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        if (transform.sprite != null) {
            transform.sprite.setPosition(transform.position.x, transform.position.y);
            transform.sprite.setRotation(transform.rotation -90);
            transform.sprite.setOriginCenter();
            if(transform.color != null)
                transform.sprite.setColor(transform.color);
            transform.sprite.draw(batch);
        }

    }
}
