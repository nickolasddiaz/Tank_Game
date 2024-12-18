package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Random;

public class StatsRenderer {
    private int stars,health = 0;
    private final TextureRegion leftHealthRegion,rightHealthRegion, top_rightHealthRegion, top_leftHealthRegion, bottom_rightHealthRegion, bottom_leftHealthRegion;
    private final float iconSize, iconsHeight;

    private final float[] starPositions, heartPositions;
    private final Color[] starColors, heartColors;



    public StatsRenderer(float iconSize, float iconsHeight) {
        this.iconSize = iconSize;
        this.iconsHeight = iconsHeight;
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ui_tank_game.atlas"));
        leftHealthRegion = atlas.findRegion("left_star");
        rightHealthRegion = atlas.findRegion("right_star");
        top_rightHealthRegion = atlas.findRegion("top_right_heart");
        top_leftHealthRegion = atlas.findRegion("top_left_heart");
        bottom_rightHealthRegion = atlas.findRegion("bottom_right_heart");
        bottom_leftHealthRegion = atlas.findRegion("bottom_left_heart");
        starPositions = new float[10];
        starColors = new Color[10];
        heartPositions = new float[12];
        heartColors = new Color[12];

        float xOffset = Gdx.graphics.getWidth() / 40f;

        for (int i = 0; i < 10; i+=2) {
            starPositions[i] = xOffset + ((float) i / 2) * (iconSize * 2);
            starPositions[i + 1] = starPositions[i] + iconSize;
        }
        for(int i = 0; i < 12; i+=4){
            heartPositions[i] = xOffset + ((float) i / 2) * (iconSize * 2);
            heartPositions[i + 1] = xOffset + ((float) i / 2) * (iconSize * 2);
            heartPositions[i + 2] = heartPositions[i] + iconSize;
            heartPositions[i + 3] = heartPositions[i] + iconSize;
        }
    }

    public void render(SpriteBatch batch, OrthographicCamera camera) {
        batch.setProjectionMatrix(camera.combined);
        for (int i = 0; i < 10; i+=2) {
            if(starColors[i] != null) {
                batch.setColor(starColors[i]);
                batch.draw(leftHealthRegion, starPositions[i], iconsHeight, iconSize, iconSize);
            }
            if (starColors[i+1] != null) {
                batch.setColor(starColors[i+1]);
                batch.draw(rightHealthRegion, starPositions[i+1], iconsHeight, iconSize, iconSize);
            }
        }
        for(int i = 0; i < 12; i+=4){
            if(heartColors[i] != null) {
                batch.setColor(heartColors[i]);
                batch.draw(top_leftHealthRegion, heartPositions[i], iconsHeight-iconSize, iconSize, iconSize);
            }
            if(heartColors[i+1] != null) {
                batch.setColor(heartColors[i + 1]);
                batch.draw(bottom_leftHealthRegion, heartPositions[i + 1], iconsHeight-iconSize*2, iconSize, iconSize);
            }
            if(heartColors[i+2] != null) {
                batch.setColor(heartColors[i + 2]);
                batch.draw(bottom_rightHealthRegion, heartPositions[i + 2], iconsHeight-iconSize*2, iconSize, iconSize);
            }
            if(heartColors[i+3] != null) {
                batch.setColor(heartColors[i + 3]);
                batch.draw(top_rightHealthRegion, heartPositions[i + 3], iconsHeight-iconSize, iconSize, iconSize);
            }
        }
        batch.setColor(Color.WHITE);
    }

    public void addStarLevel(int wantedLevel) {
        stars += wantedLevel;
        int level = stars % 10;
        int flatLevel = (stars - level) / 10;
        Color color = setColor(flatLevel);
        Color secondColor = setColor(flatLevel-1);

        for(int i = 0; i < level; i++){
            starColors[i] = color;
        }
        for(int i = level; i < 10; i++){
            starColors[i] = secondColor;
    }
    }
    public void addHealthLevel(int healthLevel) {
        health += healthLevel;
        int level = health % 12;
        int flatLevel = (health - level) / 12;
        Color color = setColor(flatLevel);
        Color secondColor = setColor(flatLevel-1);

        for(int i = 0; i < level; i++){
            heartColors[i] = color;
        }
        for(int i = level; i < 12; i++){
            heartColors[i] = secondColor;
        }
    }
    private Color setColor(int level) {
        switch (level) {
            case -1:return null;
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
