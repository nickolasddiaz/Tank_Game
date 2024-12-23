package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import java.util.Random;

public class StatsComponent implements Component {
    private int score = 0;
    private int stars = 0;
    private int health = 0;
    private boolean isDead = false;

    public final Color[] starColors = new Color[10];
    public final Color[] heartColors = new Color[12];

    public Image[] starImages;
    public Image[] heartImages;

    public Label scoreLabel, healthLabel, starsLabel;


    public int getScore() { return score; }
    public int getStars() { return stars; }
    public int getHealth() { return health; }
    public boolean isDead() { return isDead; }

    public void addStarLevel(int wantedLevel) {
        stars += wantedLevel;
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
            if (starColors[i] != null) {
                starImages[i].setColor(starColors[i]);
            } else {
                starImages[i].setColor(Color.CLEAR);
            }
        }
        starsLabel.setText(stars + "S");
    }

    public void addHealthLevel(int healthLevel) {
        health += healthLevel;
        int level = health % 12;
        int flatLevel = (health - level) / 12;
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

        if (health <= 0) {
            isDead = true;
        }
        healthLabel.setText(health + "H");
    }

    private Color setColor(int level) {
        switch (level) {
            case -1:
                return null;
            case 1:
                return Color.YELLOW;
            case 2:
                return Color.ORANGE;
            case 3:
                return Color.RED;
            case 4:
                return Color.PINK;
            case 5:
                return Color.BLUE;
            case 6:
                return Color.GREEN;
            case 7:
                return Color.PURPLE;
            case 8:
                return Color.CYAN;
            case 9:
                return Color.GRAY;
            default: {
                Random random = new Random(level);
                return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1.0f);
            }
        }
    }

    public void setScore(int score) {
        this.score = score;
    }
}
