package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.SortedIteratingSystem;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.CameraComponent;
import io.github.nickolasddiaz.components.SettingsComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;

public class SpriteRenderSystem extends SortedIteratingSystem {
    private final SpriteBatch batch;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final CameraComponent camera;
    private final SettingsComponent settings;
    private final ShapeRenderer shapeRenderer;

    public SpriteRenderSystem(SpriteBatch batch, CameraComponent camera, SettingsComponent settings, ShapeRenderer shapeRenderer) {
        super(Family.all(TransformComponent.class).get(),
            (e1, e2) -> 0);

        this.batch = batch;
        this.camera = camera;
        this.settings = settings;
        this.shapeRenderer = shapeRenderer;
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
    }

    @Override
    public void update(float deltaTime) {
        batch.setProjectionMatrix(camera.camera.combined);
        batch.begin();
        super.update(deltaTime);
        batch.end();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);
        if(settings.DEBUG) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.polygon(transform.item.userData.getPolygon().getTransformedVertices());
            shapeRenderer.rect(transform.item.userData.getBounds().x, transform.item.userData.getBounds().y, transform.item.userData.getBounds().width, transform.item.userData.getBounds().height);
            shapeRenderer.end();
        }
        if (transform.collided) { //bounce back SPEED is itemSize
            transform.tempPosition = new Vector2(transform.tempPosition.x - TILE_SIZE * (float) Math.cos(Math.toRadians(transform.tempRotation) * deltaTime) ,
                    transform.tempPosition.y - TILE_SIZE * (float) Math.sin(Math.toRadians(transform.tempRotation)) * deltaTime);

            if(transform.bouncePosition.dst(transform.tempPosition) >= TILE_SIZE){
                transform.collided = false;
                transform.position = transform.bouncePosition.cpy();
                transform.rotation = transform.tempRotation -180f;
            }
            drawSprite(transform.tempPosition, transform.tempRotation, transform.color, transform.sprite);
        }

        else {
            transform.position.add((transform.slowDown)? transform.movement.scl(.3f) :transform.movement);
            transform.movement = new Vector2(0, 0);
            drawSprite(transform.position, transform.rotation, transform.color, transform.sprite);
        }
        transform.updateBounds();
    }


    private void drawSprite(Vector2 position, float Rotation, Color color, Sprite sprite){
        sprite.setPosition(position.x, position.y);
        sprite.setRotation(Rotation);
        sprite.setOriginCenter();
        if(color != null)
            sprite.setColor(color);
        sprite.draw(batch);
    }
}
