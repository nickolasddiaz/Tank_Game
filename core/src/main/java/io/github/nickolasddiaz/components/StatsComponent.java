package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;


import java.util.Random;

import static io.github.nickolasddiaz.utils.CollisionCategory.getEnemySpawnRate;

public class StatsComponent implements Component {
    private int score = 0;
    private int stars = 0;
    public final TransformComponent player;
    public boolean upgrade = false;
    public int localHealth;


    public StatsComponent(TransformComponent player) {
        this.player = player;
        localHealth = (int) player.health;
    }

    public final Color[] starColors = new Color[10];
    public final Color[] heartColors = new Color[12];

    public Image[] starImages;
    public Image[] heartImages;

    public Label scoreLabel, healthLabel, starsLabel;

    public float pointMultiplier = 1f;
    public int luck = 10;
    public int reRollNumber = 1;
    private int addStar = 0;

    public int getScore() { return score; }
    public void addScore(int score) {
        addStar++;
        if(addStar >= 18 * (1/getEnemySpawnRate(getStars()))){
            addStar = 0;
            addStarLevel(1);
        }
        this.score += (int) (score* pointMultiplier);
        if(scoreLabel != null)
            scoreLabel.setText("Score:\n" + this.score);
    }

    public int getStars() { return stars; }
    public int getHealth() { return (int) player.health; }

    public void addStarLevel(int wantedLevel) {
        int passes = stars/3;
        stars += wantedLevel;
        if(stars/3 > passes){
            upgrade = true;
        }

        if (stars < 0) {
            stars = 0;
        }
        int level = stars % 10;
        int flatLevel = (stars - level) / 10;
        Color color = setColor(flatLevel);
        Color secondColor = setColor(flatLevel - 1);

        for (int i = 0; i < level; i++) {
            starColors[i] = color;
        }
        for (int i = level; i < 10; i++) {
            starColors[i] = secondColor;
        }

        for (int i = 0; i < 10; i++) {
            if (starColors[i] != null && starImages != null) {
                starImages[i].setColor(starColors[i]);
            } else if(starImages != null){
                starImages[i].setColor(Color.CLEAR);
            }
        }
        if(starsLabel != null)
            starsLabel.setText(stars + "S");
    }
    public void addHealthLevel(int healthLevel){
        if(player.health + healthLevel <= 0){
            setHealthLevel(0);return;
        }
        player.health += healthLevel;
        setHealthLevel(getHealth());
    }

    public void setHealthLevel(int healthLevel) {
        if (healthLevel < 0) {
            healthLevel = 0;
        }
        localHealth = healthLevel;
        int level = Math.max(0, Math.min(getHealth() % 12, 11));
        int flatLevel = (getHealth() - level) / 12;
        Color color = setColor(flatLevel);
        Color secondColor = setColor(flatLevel - 1);

        for (int i = 0; i < level; i++) {
            heartColors[i] = color;
        }
        for (int i = level; i < 12; i++) {
            heartColors[i] = secondColor;
        }

        for (int i = 0; i < 12; i++) {
            if (heartColors[i] != null) {
                heartImages[i].setColor(heartColors[i]);
            } else {
                heartImages[i].setColor(Color.CLEAR);
            }
        }

        healthLabel.setText(getHealth() + "H");
    }

    private Color setColor(int level) {
        switch (level) {
            case -1:return Color.CLEAR;
            case 1: return Color.YELLOW;
            case 2: return Color.ORANGE;
            case 3: return Color.RED;
            case 4: return Color.PINK;
            case 5: return Color.BLUE;
            case 6: return Color.GREEN;
            case 7: return Color.PURPLE;
            case 8: return Color.CYAN;
            case 9: return Color.GRAY;
            default: {
                Random random = new Random(level);
                return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1.0f);
            }
        }
    }
}
