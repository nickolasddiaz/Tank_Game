// TransformComponent.java
package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.utils.EntityStats;

import static io.github.nickolasddiaz.utils.CollisionCategory.categoryToFilterBits;

public class TransformComponent implements Component {
    public Vector2 velocity = new Vector2();
    public float rotation;
    public Sprite sprite;
    public Color color;
    public float speedBoost = 1f;

    public Body body;
    public float health;

    public boolean hasTurret = false;
    public Sprite turretSprite;
    public float turretRotation = 0f;
    public Vector2 turretOffSetPosition;
    public int turretLength;
    public EntityStats stats;

    public void addEntityStats(EntityStats stats){ //only player ally and enemy has stats
        this.stats = stats;
    }

    public TransformComponent(World world, Sprite sprite, int width, int height, Color color,
                              boolean isDynamic, short categoryBits, Vector2 position, float rotation, int health) {
        this.rotation = rotation;
        this.sprite = sprite;
        this.sprite.setSize(width, height);
        this.color = color;
        this.health = health;

        // Create Box2D body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = isDynamic ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody;
        bodyDef.position.set(position);
        bodyDef.angle = (float)Math.toRadians(rotation);

        // Create shape
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);


        // Create fixture
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = isDynamic ? 1.0f : 0.0f;
        fixtureDef.friction = 0.2f;
        fixtureDef.restitution = 0.2f;
        fixtureDef.filter.categoryBits = categoryBits;
        fixtureDef.filter.maskBits = categoryToFilterBits(categoryBits);

        // Create body and add fixture
        body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        body.setUserData(this);

        shape.dispose();
    }

    public void turretComponent(Sprite turretSprite, Vector2 turretOffSetPosition, float width, int height) {
        this.turretSprite = turretSprite;
        this.turretSprite.setSize(width, height);
        this.turretOffSetPosition = turretOffSetPosition;
        turretOffSetPosition.add(-width/1.5f, -height/1.5f);
        this.turretLength = height;
        this.hasTurret = true;
    }

    public void applyMovement() {
        if(body == null) return;
        body.setTransform(getPosition(), (float) Math.toRadians(rotation));
        body.setLinearVelocity(velocity.scl(speedBoost));
    }
    public Vector2 getPosition(){
        return body.getPosition();
    }

    public void dispose() {
        if (body != null && body.getWorld() != null) {
            body.getWorld().destroyBody(body);
            body = null;
        }
    }
}
