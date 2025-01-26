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
import io.github.nickolasddiaz.components.StatsComponent;
import io.github.nickolasddiaz.components.TransformComponent;

import static io.github.nickolasddiaz.utils.MapGenerator.TILE_SIZE;

public class SpriteRenderSystem extends SortedIteratingSystem {
    private final SpriteBatch batch;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final CameraComponent camera;
    private final SettingsComponent settings;
    private final ShapeRenderer shapeRenderer;
    private final StatsComponent stats;
    private final Engine engine;

    public SpriteRenderSystem(SpriteBatch batch, CameraComponent camera, SettingsComponent settings, ShapeRenderer shapeRenderer, StatsComponent stats, Engine engine) {
        super(Family.all(TransformComponent.class).get(),
            (e1, e2) -> 0);

        this.batch = batch;
        this.camera = camera;
        this.settings = settings;
        this.shapeRenderer = shapeRenderer;
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.engine = engine;
        this.stats = stats;
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
        if(transform.item.userData.health <= 0){
            transform.dispose();
            engine.removeEntity(entity);
            //stats.addScore(1);
            return;
        }

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
            if(transform.speedBoost == 1f)
                transform.position.add(transform.movement);
            else{
                transform.position.add(transform.movement.scl(transform.speedBoost));
                transform.speedBoost = 1f;
            }
            transform.movement = new Vector2(0, 0);
            drawSprite(transform.position, transform.rotation, transform.color, transform.sprite);
        }
        if(transform.hasTurret)
            drawTurret(transform);

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
    private void drawTurret(TransformComponent transform){
        // Calculate the turret's position based on the base entity's position and rotation
        Vector2 turretPosition = new Vector2();
        float baseRotationRad = (float) Math.toRadians(transform.rotation);

        turretPosition.x = transform.position.x +
            (transform.turretOffSetPosition.x * (float) Math.cos(baseRotationRad) -
                transform.turretOffSetPosition.y * (float) Math.sin(baseRotationRad));
        turretPosition.y = transform.position.y +
            (transform.turretOffSetPosition.x * (float) Math.sin(baseRotationRad) +
                transform.turretOffSetPosition.y * (float) Math.cos(baseRotationRad));

        // Draw the turret sprite
        transform.turretSprite.setPosition(turretPosition.x, turretPosition.y);
        transform.turretSprite.setRotation(transform.turretRotation);
        transform.turretSprite.setOriginCenter();

        // If the base has a color, apply it to the turret as well
        if (transform.color != null) {
            transform.turretSprite.setColor(transform.color);
        }

        transform.turretSprite.draw(batch);
    }
}
