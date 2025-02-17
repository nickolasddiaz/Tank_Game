package io.github.nickolasddiaz.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import io.github.nickolasddiaz.components.ChunkComponent;
import io.github.nickolasddiaz.systems.BulletFactory;
import io.github.nickolasddiaz.systems.EnemyFactory;
import io.github.nickolasddiaz.systems.LandMineFactory;
import io.github.nickolasddiaz.systems.MissileFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.TILE_PER_METER;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class EntityStats{
    public float health = 4;
    public int reduceDamage = 0;
    public int regeneration = 1;
    public float regenerationRate = 10f;
    public float allySpawnerRate = 0f;
    public int amountOfBullets = 1;
    public float freezeDuration = 2;
    public float burnDuration = 2;
    public int explosiveRadiusAndDamage = 1;
    public float bulletSize = 1.0f;
    public float criticalDamageMultiplier = 1.2f;
    public float criticalChance = 0.1f;
    public int backShotsAmount = 0;
    public boolean CanDestroy = false;
    public boolean CanShootMissile = false;
    public float missileRate = 3f;
    public boolean CanShootMine = false;
    public float mineRate = 5f;
    public float speed = itemSize * 5f;
    public float fireRate = 2f;
    public float timeSinceLastShot = 2f;
    public float bulletSpeed = 20f * itemSize;
    public int bulletDamage = 1;

    public float spinSpeed = speed / 2f * TILE_PER_METER;
    public boolean team; // true for ally, false for enemy
    private final BulletFactory bulletFactory;
    private final MissileFactory missileFactory;
    private final LandMineFactory landMineFactory;
    private final EnemyFactory enemyFactory;
    private final ChunkComponent chunk;

    private float spawnTime = 0f;
    private float landMineSpawnTime = 0f;
    private float missileSpawnTime = 0f;

    public boolean onRoad = false;
    public boolean onBush = false;
    public float isOnFire = 0;
    public float isFrozen = 0;
    public Vector2 velocity = new Vector2();

    private final Random random;

    public EntityStats(Random random, boolean team, BulletFactory bulletFactory,
                       MissileFactory missileFactory, LandMineFactory landMineFactory,
                       EnemyFactory enemyFactory, ChunkComponent chunk, int level) {
        this(random, team, bulletFactory, missileFactory, landMineFactory, enemyFactory, chunk);

        // Define all possible stats that can be improved
        Object[][] stats = {
            {health, 2f},                          // Base increase per level
            {bulletDamage, 1},
            {speed, itemSize * 0.5f},
            {regeneration, 1},
            {regenerationRate, -0.5f},             // Decrease time between regenerations
            {fireRate, -0.02f},                    // Decrease time between shots
            {bulletSpeed, itemSize * 0.5f},
            {bulletSize, 0.1f},
            {criticalChance, 0.05f},
            {criticalDamageMultiplier, .5f},
            {amountOfBullets, 1},
            {backShotsAmount, 1}
        };

        // For each level, improve 5 random stats
        for (int i = 0; i < level; i++) {
            // Create a copy of the stats array indices to randomly select from
            List<Integer> availableStats = new ArrayList<>();
            for (int j = 0; j < stats.length; j++) {
                availableStats.add(j);
            }

            // Select and improve 5 random stats
            for (int j = 0; j < stats.length; j++) {
                int selectedIndex = random.nextInt(availableStats.size());
                int statIndex = availableStats.get(selectedIndex);
                availableStats.remove(selectedIndex);

                Object[] stat = stats[statIndex];
                if (stat[0] instanceof Float) {
                    float currentValue = (Float) stat[0];
                    float increase = (Float) stat[1];
                    stat[0] = currentValue + increase;
                } else if (stat[0] instanceof Integer) {
                    int currentValue = (Integer) stat[0];
                    int increase = (Integer) stat[1];
                    stat[0] = currentValue + increase;
                }
            }

            // Apply the improved stats back to the entity
            health = (Float) stats[0][0];
            bulletDamage = (Integer) stats[1][0];
            speed = (Float) stats[2][0];
            regeneration = (Integer) stats[3][0];
            regenerationRate = (Float) stats[4][0];
            fireRate = (Float) stats[5][0];
            bulletSpeed = (Float) stats[6][0];
            bulletSize = (Float) stats[7][0];
            criticalChance = (Float) stats[8][0];
            criticalDamageMultiplier = (Float) stats[9][0];
            amountOfBullets = (Integer) stats[10][0];
            backShotsAmount = (Integer) stats[11][0];

            // Update dependent stats
            spinSpeed = speed/2f * TILE_PER_METER;
        }

        // Add special abilities based on level thresholds
        if(random.nextFloat() > 15f/level) CanShootMine = true;
        if(random.nextFloat() > 15f/level) CanShootMissile = true;
        if(random.nextFloat() > 15f/level) CanDestroy = true;
        //allySpawnerRate is left out as it will become too crazy
    }

    public EntityStats(Random random, boolean team, BulletFactory bulletFactory,
                       MissileFactory missileFactory, LandMineFactory landMineFactory,
                       EnemyFactory enemyFactory, ChunkComponent chunk) {
        this.random = random;
        this.team = team;
        this.bulletFactory = bulletFactory;
        this.missileFactory = missileFactory;
        this.landMineFactory = landMineFactory;
        this.enemyFactory = enemyFactory;
        this.chunk = chunk;
    }

    public Vector2 emulate(float delta, Vector2 position, float rotation, Vector2 velocity, boolean canShoot) {
        // Handle regeneration
        if (regeneration > 0) {
            health += regeneration/regenerationRate * delta;
        }

        // Handle burn effect
        if (isOnFire > 0) {
            isOnFire -= delta;
            health -= isOnFire * delta;
        }

        // Handle freeze effect
        if (isFrozen > 0) {
            isFrozen -= delta;
            velocity.scl(.05f);
        }

        // Handle speed boost
        velocity.scl(1f + (onRoad ? 0.5f : 0) + (onBush ? -0.5f : 0));

        // Handle missile shooting
        if (CanShootMissile) {
            missileSpawnTime += delta;
            if (missileSpawnTime > missileRate) {
                missileSpawnTime = 0f;
                missileFactory.spawnMissile(position.cpy(), rotation,
                    bulletSpeed, calculateDamage() * 4,
                    bulletSize, null, team);
            }
        }

        // Handle mine placement
        if (CanShootMine) {
            landMineSpawnTime += delta;
            if (landMineSpawnTime > mineRate) {
                landMineSpawnTime = 0f;
                landMineFactory.createLandMine(position.cpy(), calculateDamage() * 2, team);
            }
        }

        // Handle bullet shooting
        timeSinceLastShot += delta;
        if (canShoot && timeSinceLastShot > fireRate) {
            spawnBullets(position.cpy(), rotation);
            timeSinceLastShot = 0f;
        }

        // Handle ally spawning
        if (allySpawnerRate > 0) {
            handleAllySpawning(delta, position);
        }
        return velocity;
    }

    private void handleAllySpawning(float delta, Vector2 position) {
        spawnTime += delta;
        if (spawnTime > allySpawnerRate) {
            spawnTime = 0f;
            float spawnLength = 2 * itemSize;

            Rectangle spawnArea = new Rectangle(
                position.x - spawnLength / 2f,
                position.y - spawnLength / 2f,
                spawnLength,
                spawnLength
            );

            Body[] bodies = chunk.getBodiesInRect(spawnArea, (short) (PLAYER | STRUCTURE | OCEAN | ENEMY | ALLY));

            Vector2[] spawnPositions = new Vector2[]{
                new Vector2(spawnArea.x, spawnArea.y),
                new Vector2(spawnArea.x + spawnLength, spawnArea.y),
                new Vector2(spawnArea.x, spawnArea.y + spawnLength),
                new Vector2(spawnArea.x + spawnLength, spawnArea.y + spawnLength)
            };

            for (Vector2 pos : spawnPositions) {
                if (!isPositionOccupied(pos, bodies)) {
                    enemyFactory.createTank(pos, team, this);
                    break;
                }
            }
        }
    }

    private boolean isPositionOccupied(Vector2 position, Body[] bodies) {
        for (Body body : bodies) {
            if (body.getPosition().dst(position) < itemSize) {
                return true;
            }
        }
        return false;
    }

    public int calculateDamage() {
        float chance = random.nextFloat();
        float multiplier = (chance < criticalChance) ? criticalDamageMultiplier : 1f;
        return (int) (bulletDamage * multiplier);
    }

    private float[] calculateShotAngles(float rotation) {
        float[] angles = new float[amountOfBullets + backShotsAmount];
        int index = 0;
        float step = Math.min(60f / Math.max(amountOfBullets - 1, 1), 5f);
        float rotate = rotation % 360;

        for (int i = 0; i < amountOfBullets; i++) {
            angles[index++] = rotate + step * (i - (amountOfBullets - 1) / 2f);
        }
        rotate = (rotation + 180) % 360;
        step = Math.min(60f / Math.max(backShotsAmount - 1, 1), 5f);

        for (int i = 0; i < backShotsAmount; i++) {
            angles[index++] = rotate + step * (i - (backShotsAmount - 1) / 2f);
        }

        return angles;
    }

    private void spawnBullets(Vector2 position, float rotation) {
        Color bulletColor = team ? Color.YELLOW : Color.RED;
        for (float shotAngle : calculateShotAngles(rotation)) {
            bulletFactory.createBullet(position.cpy(), shotAngle, bulletSpeed, calculateDamage() + 1, bulletSize, bulletColor, team);
        }
    }

    public EntityStats clone(int level) {
        return new EntityStats(random, team, bulletFactory, missileFactory, landMineFactory, enemyFactory, chunk, level);
    }
}
