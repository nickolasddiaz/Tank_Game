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


import java.util.Random;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.TILE_PER_METER;
import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class EntityStats{
    public float health = 4;
    public int reduceDamage = 0;
    public int regeneration = 1;
    public float regenerationRate = 15f;
    public float allySpawnerRate = 20f;
    public boolean canSpawnAlly = false;
    public int amountOfBullets = 1;
    public int explosiveRadiusAndDamage = 1;
    public float bulletSize = 1.0f;
    public float criticalDamageMultiplier = 1.2f;
    public float criticalChance = 0.1f;
    public int backShotsAmount = 0;
    public boolean CanDestroy = false;
    public boolean CanShootMissile = false;
    public float missileRate = 4f;
    public boolean CanShootMine = false;
    public float mineRate = 5f;
    public float speed = itemSize * 10f;
    public float fireRate = 2f;
    public float timeSinceLastShot = 2f;
    public float bulletSpeed = 12f * itemSize;
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

    public int onRoad = 0;
    public int onBush = 0;
    public float isOnFire = 0;
    public float isFrozen = 0;

    private final Random random;

    public EntityStats(Random random, boolean team, BulletFactory bulletFactory,
                       MissileFactory missileFactory, LandMineFactory landMineFactory,
                       EnemyFactory enemyFactory, ChunkComponent chunk, int level, boolean canSpawn) {
        this(random, team, bulletFactory, missileFactory, landMineFactory, enemyFactory, chunk);
        this.canSpawnAlly = canSpawn; // ensure that allys can recursively spawn allys

        // Base stat increases per level
        float healthIncreasePerLevel = 2.0f;
        int bulletDamageIncreasePerLevel = 1;
        float speedIncreasePerLevel = itemSize * 0.5f;
        float fireRateReducePerLevel = 0.1f; // Lower is better for fire rate
        float bulletSpeedIncreasePerLevel = itemSize * 0.5f;
        float bulletSizeIncreasePerLevel = 0.1f;
        float criticalChanceIncreasePerLevel = 0.05f;
        float criticalDamageMultiplierIncreasePerLevel = 0.2f;
        int amountOfBulletsIncreaseEveryNLevels = 5; // Add a bullet every 5 levels
        int backShotsAmountIncreaseEveryNLevels = 10; // Add a back shot every 10 levels

        // Apply deterministic stat increases based on level
        health += healthIncreasePerLevel * level;
        bulletDamage += bulletDamageIncreasePerLevel * level;
        speed += speedIncreasePerLevel * level;
        fireRate = Math.max(0.1f, fireRate - (fireRateReducePerLevel * level)); // Prevent negative fire rate
        bulletSpeed += bulletSpeedIncreasePerLevel * level;
        bulletSize += bulletSizeIncreasePerLevel * level;
        criticalChance = Math.min(1.0f, criticalChance + (criticalChanceIncreasePerLevel * level)); // Cap at 100%
        criticalDamageMultiplier += criticalDamageMultiplierIncreasePerLevel * level;

        // Add bullets and back shots at specific level thresholds
        amountOfBullets += level / amountOfBulletsIncreaseEveryNLevels;
        backShotsAmount += level / backShotsAmountIncreaseEveryNLevels;

        // Update dependent stats
        spinSpeed = speed / 2f * TILE_PER_METER;

        // Assign exactly one special ability based on level and a random roll
        // The higher the level, the more likely to get a special ability
        if (level >= 3) { // Minimum level threshold for special abilities
            float specialAbilityChance = Math.min(0.9f, 0.3f + (level * 0.05f)); // Cap at 90% chance

            if (random.nextFloat() < specialAbilityChance) {
                // Choose one special ability based on random value
                int specialAbilityType = random.nextInt(4);

                switch (specialAbilityType) {
                    case 0:
                        CanShootMine = true;
                        mineRate = Math.max(1.0f, 5.0f - (level * 0.2f)); // Improve mine rate with level
                        break;
                    case 1:
                        CanShootMissile = true;
                        missileRate = Math.max(1.0f, 4.0f - (level * 0.15f)); // Improve missile rate with level
                        break;
                    case 2:
                        CanDestroy = true;
                        explosiveRadiusAndDamage += level / 2; // Increase explosive power with level
                        break;
                    case 3:
                        canSpawnAlly = true;
                        allySpawnerRate = Math.max(3.0f, 10.0f - (level * 0.3f)); // Improve ally spawn rate with level
                        break;
                }
            }
        }
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
        if (regeneration > 0 && team) {
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
        velocity.scl(1f + (onRoad >=1 ? 0.3f : 0) + (onBush >= 1 ? -0.3f : 0));

        // Handle missile shooting
        if (CanShootMissile) {
            missileSpawnTime += delta;
            if (missileSpawnTime > missileRate) {
                missileSpawnTime = 0f;
                missileFactory.spawnMissile(position.cpy(), rotation,
                    bulletSpeed, calculateDamage() * ((team)? 4:1) * explosiveRadiusAndDamage,
                    bulletSize, null, team);
            }
        }

        // Handle mine placement
        if (CanShootMine) {
            landMineSpawnTime += delta;
            if (landMineSpawnTime > mineRate) {
                landMineSpawnTime = 0f;
                landMineFactory.createLandMine(position.cpy(), calculateDamage() * explosiveRadiusAndDamage * ((team)? 4:1), team);
            }
        }

        // Handle bullet shooting
        timeSinceLastShot += delta;
        if (canShoot && timeSinceLastShot > fireRate) {
            spawnBullets(position.cpy(), rotation);
            timeSinceLastShot = 0f;
        }

        // Handle ally spawning
        if (canSpawnAlly) {
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
        return new EntityStats(random, team, bulletFactory, missileFactory, landMineFactory, enemyFactory, chunk, level, false);
    }
}
