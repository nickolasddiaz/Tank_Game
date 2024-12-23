package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.nickolasddiaz.components.CameraComponent;
import io.github.nickolasddiaz.components.SpriteComponent;
import io.github.nickolasddiaz.components.TransformComponent;

public class RenderSystem extends SortedIteratingSystem {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<TransformComponent> transformMapper;
    private ComponentMapper<CameraComponent> cameraMapper;

    public RenderSystem(SpriteBatch batch) {
        super(Family.all(TransformComponent.class, SpriteComponent.class).get(),
            (e1, e2) -> 0); // Simple comparator since we don't need sorting yet

        this.batch = batch;
        spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        cameraMapper = ComponentMapper.getFor(CameraComponent.class);
    }

    @Override
    public void update(float deltaTime) {
        batch.begin();
        super.update(deltaTime);
        batch.end();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        SpriteComponent sprite = spriteMapper.get(entity);
        CameraComponent cameraComponent = cameraMapper.get(entity);
        batch.setProjectionMatrix(cameraComponent.camera.combined);

        if (sprite.tankSprite != null) {
            // Update sprite position and rotation based on transform
            sprite.tankSprite.setPosition(transform.position.x, transform.position.y);
            sprite.tankSprite.setRotation(transform.rotation +90);
            sprite.tankSprite.setScale(transform.scale.x, transform.scale.y);
            sprite.tankSprite.setOrigin(sprite.tankSprite.getWidth() / 2, sprite.tankSprite.getHeight() / 2);

            // Draw the sprite
            sprite.tankSprite.draw(batch);
        }
    }
}
