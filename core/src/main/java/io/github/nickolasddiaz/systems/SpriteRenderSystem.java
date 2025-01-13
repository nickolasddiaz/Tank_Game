package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.SortedIteratingSystem;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.CameraComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;

public class SpriteRenderSystem extends SortedIteratingSystem {
    private final SpriteBatch batch;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<CameraComponent> cameraMapper;

    public SpriteRenderSystem(SpriteBatch batch) {
        super(Family.all(TransformComponent.class).get(),
            (e1, e2) -> 0);

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
        if (transform.collided) { //bounce back SPEED is itemSize
            transform.tempPosition = new Vector2(transform.tempPosition.x - TILE_SIZE * (float) Math.cos(Math.toRadians(transform.tempRotation) * deltaTime) ,
                    transform.tempPosition.y - TILE_SIZE * (float) Math.sin(Math.toRadians(transform.tempRotation)) * deltaTime);

            if(transform.bouncePosition.dst(transform.tempPosition) >= TILE_SIZE){
                transform.collided = false;
                transform.position = transform.bouncePosition.cpy();
                transform.rotation = transform.tempRotation -180f;
            }
            modifySprite(transform.tempPosition, transform.tempRotation, transform.color, transform.sprite);
        }

        else if (transform.sprite != null) {
            transform.position.add((transform.slowDown)? transform.movement.scl(.3f) :transform.movement);
            transform.movement = new Vector2(0, 0);
            modifySprite(transform.position, transform.rotation, transform.color, transform.sprite);
        }
    }


    private void modifySprite(Vector2 position, float Rotation, Color color, Sprite sprite){
        sprite.setPosition(position.x, position.y);
        sprite.setRotation(Rotation);
        sprite.setOriginCenter();
        if(color != null)
            sprite.setColor(color);
        sprite.draw(batch);
    }
}
