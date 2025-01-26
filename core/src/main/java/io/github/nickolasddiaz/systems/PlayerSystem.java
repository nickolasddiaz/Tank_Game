package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.CollisionObject;
import java.util.ArrayList;

import static io.github.nickolasddiaz.utils.MapGenerator.chunkSize;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class PlayerSystem extends IteratingSystem {
    private final ComponentMapper<PlayerComponent> playerMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ComponentMapper<JoystickComponent> joystickMapper;
    private final ChunkComponent chunk;
    private final SettingsComponent settings;
    private CollisionObject lockedTarget;
    private final StatsComponent statsComponent;
    private final BulletFactory bulletFactory;
    private float timeToReadjust = 0f;
    private float speedBoost = 1f;
    private float collisionAngle = 0f;
    private float spawnTime = 0f;
    private final EnemyFactory enemyFactory;
    private final MissileFactory missileFactory;
    private final LandMineFactory landMineFactory;
    private float landMineSpawnTime = 0f;
    private float MissileSpawnTime = 0f;

    public PlayerSystem(SettingsComponent settings, ChunkComponent chunk, BulletFactory bulletFactory,StatsComponent statsComponent, EnemyFactory enemyFactory, MissileFactory missileFactory, LandMineFactory landMineFactory) {
        super(Family.all(PlayerComponent.class, TransformComponent.class, JoystickComponent.class).get());
        playerMapper = ComponentMapper.getFor(PlayerComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        joystickMapper = ComponentMapper.getFor(JoystickComponent.class);
        this.settings = settings;
        this.chunk = chunk;
        this.bulletFactory = bulletFactory;
        this.statsComponent = statsComponent;
        this.enemyFactory = enemyFactory;
        this.missileFactory = missileFactory;
        this.landMineFactory = landMineFactory;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);
        JoystickComponent joystick = joystickMapper.get(entity);
        if(statsComponent.getHealth() != transform.item.userData.health){
            statsComponent.setHealthLevel(transform.item.userData.health);
        }

        //fire missiles
        if(player.CanShootMissile){
            MissileSpawnTime += deltaTime;
            if(MissileSpawnTime > player.missileRate){
                MissileSpawnTime = 0f;
                missileFactory.spawnMissile(transform.position.cpy(), transform.turretRotation, player.bulletSpeed, player.calculateDamage() * 4, player.bulletSize, null);
            }
        }

        //fire landmines
        if(player.CanShootMine){
            landMineSpawnTime += deltaTime;
            if(landMineSpawnTime > player.mineRate){
                landMineSpawnTime = 0f;
                landMineFactory.createLandMine(transform.position.cpy(), player.calculateDamage() * 2);
            }
        }


        // Fire bullets
        player.timeSinceLastShot += deltaTime;
        timeToReadjust += deltaTime;
        if((settings.AUTO_FIRE || Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isTouched()) && player.timeSinceLastShot > player.fireRate){
            player.spawnBullets(transform.position,transform.turretRotation, bulletFactory, Color.YELLOW);
            player.timeSinceLastShot = 0f;
        }


        // Spawn allies
        spawnTime += deltaTime;
        if (spawnTime > player.allySpawnerRate) {
            spawnTime = 0f;
            int spawnLength = 2 * itemSize;

            // Define the spawn area rectangle
            Rectangle spawnArea = new Rectangle(
                transform.position.x - spawnLength / 2f,
                transform.position.y - spawnLength / 2f,
                spawnLength,
                spawnLength
            );

            // Get items within the spawn area
            ArrayList<Item> items = chunk.getObjectsIsInsideRect(chunk.allySpawnFilter, spawnArea);

            // Define possible spawn positions
            Vector2[] spawnPositions = new Vector2[]{
                new Vector2(spawnArea.x, spawnArea.y),
                new Vector2(spawnArea.x + spawnLength, spawnArea.y),
                new Vector2(spawnArea.x, spawnArea.y + spawnLength),
                new Vector2(spawnArea.x + spawnLength, spawnArea.y + spawnLength)
            };

            // Randomize spawn order
            int[] cornerOrder = chunk.random.ints(0, 4).distinct().limit(4).toArray();

            // Attempt to spawn at a randomized corner
            for (int index : cornerOrder) {
                Vector2 spawnPosition = spawnPositions[index];
                boolean positionOccupied = false;

                if (items != null) {
                    for (Item item : items) {
                        CollisionObject object = (CollisionObject) item.userData;
                        if (object.getBounds().contains(spawnPosition)) {
                            positionOccupied = true;
                            break;
                        }
                    }
                }

                if (!positionOccupied) {
                    enemyFactory.createTank(spawnPosition, true);
                    break;
                }
            }
        }



        Vector2 direction = new Vector2(0, 0);
        float speedMultiplier = 1f;

        if(!settings.AUTO_FIRE)
            // Reverse Y to account for the coordinate system
            transform.turretRotation = (float) Math.toDegrees(Math.atan2(Gdx.graphics.getHeight() / 2f - Gdx.input.getY(), Gdx.input.getX() - Gdx.graphics.getWidth() / 2f));
        else{
            if ((lockedTarget == null || timeToReadjust > 1f)&& player.enemyCount > 0) {
                timeToReadjust = 0f;
                ArrayList<Item> enemies = chunk.getObjectsIsInsideRect(chunk.enemyFilter, new Rectangle(transform.position.x - chunkSize / 2f, transform.position.y - chunkSize / 2f, chunkSize, chunkSize));

                if (enemies != null && !enemies.isEmpty()) {
                    float minDistance = Float.MAX_VALUE;
                    for (Item enemy : enemies) {
                        CollisionObject enemyObject = (CollisionObject) enemy.userData;
                        float distance = transform.position.dst(enemyObject.getPosition());
                        if (distance < minDistance) {
                            minDistance = distance;
                            lockedTarget = enemyObject;
                        }
                    }
                }
            }
            else if(lockedTarget != null){
                Vector2 targetPosition = lockedTarget.getPosition();
                transform.turretRotation = (float) Math.toDegrees(Math.atan2(targetPosition.y - transform.position.y, targetPosition.x - transform.position.x));
            }

        }

        if(transform.collided){ return; }

        if (settings.IS_MOBILE && Gdx.input.isTouched()) {
            Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());

            if (joystick.joyStickTouchCircle.contains(touchPos)) {
                Vector2 center = new Vector2(joystick.joyStickBaseCircle.x, joystick.joyStickBaseCircle.y);

                if (joystick.joyStickBaseCircle.contains(touchPos)) {
                    joystick.stickPositionMovement = touchPos.cpy();
                    speedMultiplier = touchPos.dst(center) / joystick.joyStickBaseCircle.radius;
                } else {
                    Vector2 offset = touchPos.cpy().sub(center);
                    offset.nor().scl(joystick.joyStickBaseCircle.radius);
                    joystick.stickPositionMovement = center.cpy().add(offset);
                }

                direction = joystick.stickPositionMovement.cpy().sub(center).nor();
                if (direction.len2() > 0) {
                    Vector2 movement = new Vector2(direction).scl(deltaTime * player.SPEED * speedMultiplier);
                    float targetAngle = direction.angleDeg();
                    float angleDifference = ((targetAngle - transform.rotation + 540) % 360) - 180;

                    // Adjust rotation with gradual speed
                    if (Math.abs(angleDifference) > 5) {
                        float turnSpeed = Math.min(player.spinSpeed * deltaTime, Math.abs(angleDifference) * 0.5f);
                        transform.rotation += Math.signum(angleDifference) * turnSpeed;
                    } else {
                        transform.rotation = targetAngle;
                    }
                    transform.rotation = (transform.rotation + 360) % 360;

                    // Apply movement based on rotation
                    float angleRad = (float) Math.toRadians(transform.rotation);
                    transform.movement.x = MathUtils.cos(angleRad) * movement.len();
                    transform.movement.y = MathUtils.sin(angleRad) * movement.len();
                }
            }
        } else {
            // Desktop controls
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction.x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction.x += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direction.y += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direction.y -= 1f;

            if (direction.len2() > 0 && direction.y != 0) {
                direction.nor();
                Vector2 movement = new Vector2(direction).scl(deltaTime * player.SPEED);
                transform.rotation += direction.x * player.spinSpeed * deltaTime;
                transform.rotation = (transform.rotation + 360) % 360;


                float angleRad = (float) Math.toRadians(transform.rotation);
                transform.movement.x = MathUtils.cos(angleRad) * movement.y;
                transform.movement.y = MathUtils.sin(angleRad) * movement.y;
            }
        }
        if (chunk.world.getRect(transform.item) == null) {
            Gdx.app.log("Invalid item or missing rect: ", "error");
            Rectangle rect = transform.item.userData.getBounds();
            chunk.world.add(transform.item, transform.position.x, transform.position.y, rect.width, rect.height);
        }
        Response.Result result = chunk.world.move(transform.item, transform.position.x, transform.position.y, CollisionFilter);
        transform.position.set(result.goalX, result.goalY);

        transform.speedBoost = speedBoost;
        speedBoost = 1f;
        if(collisionAngle != 0) {
            transform.setCollided(collisionAngle);
            collisionAngle = 0;
        }
    }
    private final CollisionFilter CollisionFilter = new CollisionFilter() {
        @Override
        public Response filter(Item item, Item other) {
            if(other == null) {
                return null;
            }
            CollisionObject otherObject = (CollisionObject) other.userData;
            switch (otherObject.getObjectType()) {
                case "STRUCTURE":
                    chunk.destroyStructure(otherObject.getPosition());
                    //chunk.world.remove(other);
                case "OCEAN":
                    if (Intersector.overlapConvexPolygons(otherObject.getPolygon(), ((CollisionObject) item.userData).getPolygon())) {
                        return Response.slide;
                    }
                    return Response.cross;
                case "CAR":
                    otherObject.health = 0;
                    speedBoost *= .4f;
                    addScore();
                    return Response.cross;
                case "ENEMY":
                    return Response.bounce;
                case "HORIZONTAL":
                case "VERTICAL":
                    speedBoost += .4f;
                    return Response.cross;
                case "DECORATION":
                    speedBoost *= .4f;
                    return Response.cross;

            }
            return Response.cross;
        }
    };

    private void addScore(){
        statsComponent.addScore(1);
    }
}
