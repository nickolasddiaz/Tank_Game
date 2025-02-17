package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.CollisionCategory.PLAYER;


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
            if(transform.body.getFixtureList().get(0).getFilterData().categoryBits != PLAYER) {
                transform.dispose();
                engine.removeEntity(entity);
            }
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
        // Calculate the center point of the tank
        Vector2 tankCenter = transform.getPosition();

        // Convert rotation angles to radians
        float tankRotationRad = (float) Math.toRadians(transform.rotation);
        float turretRotationRad = (float) Math.toRadians(transform.turretRotation);

        // Calculate the offset position in the tank's local space
        Vector2 turretPosition = new Vector2();

        // Rotate the offset around the tank's center
        turretPosition.x = tankCenter.x +
            (transform.turretOffSetPosition.x * (float) Math.cos(tankRotationRad) -
                transform.turretOffSetPosition.y * (float) Math.sin(tankRotationRad));
        turretPosition.y = tankCenter.y +
            (transform.turretOffSetPosition.x * (float) Math.sin(tankRotationRad) +
                transform.turretOffSetPosition.y * (float) Math.cos(tankRotationRad));

        // Set the turret sprite properties
        transform.turretSprite.setOriginCenter();
        transform.turretSprite.setPosition(
            turretPosition.x - transform.turretSprite.getWidth() / 2f,
            turretPosition.y - transform.turretSprite.getHeight() / 2f
        );

        // The turret rotation should be absolute (not relative to tank rotation)
        transform.turretSprite.setRotation(transform.turretRotation);

        if (transform.color != null) {
            transform.turretSprite.setColor(transform.color);
        }

        transform.turretSprite.draw(batch);
    }

}
