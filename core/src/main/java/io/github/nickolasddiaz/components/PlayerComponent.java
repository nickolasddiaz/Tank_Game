package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.systems.BulletFactory;

import java.util.Random;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

public class PlayerComponent implements Component {
    // regeneration, speed, ally spawner, bullet speed, bullet damage, fire rate,
    // decrease wanted level, extra shot, freeze shot, burn damage, burn duration, freeze duration,
    // point multiplier, luck, health increase, re-roll number, explosive increase,
    // bullet size, critical damage multiplier, critical chance, better armor, back_shots

    //regeneration regenerationRate pointMultiplier luck reRollNumber and reduceDamage moved to StatsComponent
    public float allySpawnerRate = 20f;
    public int amountOfBullets = 2;
    public float freezeAmount = 1f;
    public int burnAmount = 1;
    public int freezeDuration = 2;
    public int burnDuration = 2;
    public int explosiveRadiusAndDamage = 1;
    public float bulletSize = 1.5f;
    public float criticalDamageMultiplier = 120f;
    public float criticalChance = 0.1f;
    public int backShotsAmount = 0;
    public boolean CanDestroy = false;
    public boolean CanShootMissile = false;
    public boolean CanShootMine = false;

    public float SPEED = itemSize * 50f;
    public float spinSpeed = SPEED/16;
    public int enemyCount = 0;
    public float fireRate = 0.5f;
    public float timeSinceLastShot = .5f;
    public float bulletSpeed = 20f * itemSize;
    public int bulletDamage = 5;
    private final Random random;
    public PlayerComponent(Random random){
        this.random = random;
    }

    private int calculateDamage(){
            float chance = criticalChance % 100;
            float multiplier = (random.nextFloat() < chance) ? criticalDamageMultiplier : 1f;
            return (int) (bulletDamage * multiplier * (criticalChance - chance)/ 100);
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

    public void spawnBullets(Vector2 position, float rotation, BulletFactory bulletFactory, Color color){
        for (float shotAngle : calculateShotAngles(rotation)) {
            bulletFactory.createBullet(position.cpy(), shotAngle, bulletSpeed, calculateDamage(), bulletSize, color, "P_BULLET");
        }
    }
}
