package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.utils.MapGenerator.TILE_SIZE;

public class SpriteRenderSystem extends SortedIteratingSystem {
    private final SpriteBatch batch;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final CameraComponent camera;
    private final SettingsComponent settings;
    private final ShapeRenderer shapeRenderer;
    private final StatsComponent stats;
    private final Engine engine;
    private final Box2DDebugRenderer debugRenderer;

    public SpriteRenderSystem(SpriteBatch batch, CameraComponent camera,
                              SettingsComponent settings, ShapeRenderer shapeRenderer,
                              StatsComponent stats, Engine engine) {
        super(Family.all(TransformComponent.class).get(), (e1, e2) -> 0);

        this.batch = batch;
        this.camera = camera;
        this.settings = settings;
        this.shapeRenderer = shapeRenderer;
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.engine = engine;
        this.stats = stats;
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

        if (transform.health <= 0) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Update position and rotation from physics body
        transform.updateTransform();

        // Apply movement forces
        transform.applyMovement();

        if (transform.collided) {
            transform.tempPosition = new Vector2(
                transform.tempPosition.x - TILE_SIZE * (float) Math.cos(Math.toRadians(transform.tempRotation) * deltaTime),
                transform.tempPosition.y - TILE_SIZE * (float) Math.sin(Math.toRadians(transform.tempRotation)) * deltaTime
            );

            if (transform.bouncePosition.dst(transform.tempPosition) >= TILE_SIZE) {
                transform.collided = false;
                transform.position = transform.bouncePosition.cpy();
                transform.rotation = transform.tempRotation - 180f;
                transform.body.setTransform(transform.position, (float)Math.toRadians(transform.rotation));
            }
            drawSprite(transform.tempPosition, transform.tempRotation, transform.color, transform.sprite);
        } else {
            drawSprite(transform.position, transform.rotation, transform.color, transform.sprite);
        }

        if (transform.hasTurret) {
            drawTurret(transform);
        }
    }

    private void drawSprite(Vector2 position, float rotation, Color color, Sprite sprite) {
        sprite.setPosition(position.x, position.y);
        sprite.setRotation(rotation);
        sprite.setOriginCenter();
        if (color != null) {
            sprite.setColor(color);
        }
        sprite.draw(batch);
    }

    private void drawTurret(TransformComponent transform) {
        Vector2 turretPosition = new Vector2();
        float baseRotationRad = (float) Math.toRadians(transform.rotation);

        turretPosition.x = transform.position.x +
            (transform.turretOffSetPosition.x * (float) Math.cos(baseRotationRad) -
                transform.turretOffSetPosition.y * (float) Math.sin(baseRotationRad));
        turretPosition.y = transform.position.y +
            (transform.turretOffSetPosition.x * (float) Math.sin(baseRotationRad) +
                transform.turretOffSetPosition.y * (float) Math.cos(baseRotationRad));

        transform.turretSprite.setPosition(turretPosition.x, turretPosition.y);
        transform.turretSprite.setRotation(transform.turretRotation);
        transform.turretSprite.setOriginCenter();

        if (transform.color != null) {
            transform.turretSprite.setColor(transform.color);
        }

        transform.turretSprite.draw(batch);
    }

    public void dispose() {
        debugRenderer.dispose();
    }
}
