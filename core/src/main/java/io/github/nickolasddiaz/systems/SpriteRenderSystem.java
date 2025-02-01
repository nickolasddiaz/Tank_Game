package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import io.github.nickolasddiaz.components.*;


public class SpriteRenderSystem extends SortedIteratingSystem {
    private final SpriteBatch batch;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final CameraComponent camera;
    private final SettingsComponent settings;
    private final Engine engine;
    private final Box2DDebugRenderer debugRenderer;

    public SpriteRenderSystem(SpriteBatch batch, CameraComponent camera,
                              SettingsComponent settings, Engine engine) {
        super(Family.all(TransformComponent.class).get(), (e1, e2) -> 0);

        this.batch = batch;
        this.camera = camera;
        this.settings = settings;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.engine = engine;
        this.debugRenderer = new Box2DDebugRenderer();
    }

    @Override
    public void update(float deltaTime) {
        batch.setProjectionMatrix(camera.camera.combined);
        batch.begin();
        super.update(deltaTime);
        batch.end();

        if (settings.DEBUG) {
            ChunkComponent chunkComponent = engine.getEntitiesFor(Family.all(ChunkComponent.class).get())
                .first().getComponent(ChunkComponent.class);
            debugRenderer.render(chunkComponent.world, camera.camera.combined);
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = transformMapper.get(entity);

        if (transform.health <= 0 || transform.body == null) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Apply movement forces
        transform.applyMovement();

        drawSprite(transform.getPosition(), transform.rotation, transform.color, transform.sprite);

        if (transform.hasTurret) {
            drawTurret(transform);
        }
    }

    private void drawSprite(Vector2 position, float rotation, Color color, Sprite sprite) {
        sprite.setOriginCenter();
        sprite.setPosition(position.x - sprite.getWidth() / 2f, position.y - sprite.getHeight() / 2f);
        sprite.setRotation(rotation);

        if (color != null) sprite.setColor(color);

        sprite.draw(batch);
    }

    private void drawTurret(TransformComponent transform) {
        Vector2 turretPosition = new Vector2();
        float baseRotationRad = (float) Math.toRadians(transform.rotation);

        turretPosition.x = transform.getPosition().x +
            (transform.turretOffSetPosition.x * (float) Math.cos(baseRotationRad) -
                transform.turretOffSetPosition.y * (float) Math.sin(baseRotationRad));
        turretPosition.y = transform.getPosition().y +
            (transform.turretOffSetPosition.x * (float) Math.sin(baseRotationRad) +
                transform.turretOffSetPosition.y * (float) Math.cos(baseRotationRad));

        transform.turretSprite.setPosition(turretPosition.x, turretPosition.y);
        transform.turretSprite.setRotation(transform.turretRotation);
        transform.turretSprite.setOriginCenter();

        if (transform.color != null) transform.turretSprite.setColor(transform.color);

        transform.turretSprite.draw(batch);
    }

}
