package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.nickolasddiaz.components.*;

public class StatsRenderSystem extends EntitySystem {
    private Stage stage;
    StatsComponent statsComponent;
    SettingsComponent settingsComponent;
    float regenerationTime = 0f;
    private final Skin skin;

    public StatsRenderSystem(Skin skin) {
        this.skin = skin;
    }

    @Override
    public void addedToEngine(Engine engine) {
        Entity player = engine.getEntitiesFor(Family.all(
            StatsComponent.class,
            SettingsComponent.class
        ).get()).first();
        ComponentMapper<StatsComponent> statsMapper = ComponentMapper.getFor(StatsComponent.class);
        ComponentMapper<SettingsComponent> settingsMapper = ComponentMapper.getFor(SettingsComponent.class);

        statsComponent = statsMapper.get(player);
        settingsComponent = settingsMapper.get(player);

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        float iconSize = Gdx.graphics.getWidth() / 30f;

        statsComponent.starImages = new Image[10];
        for (int i = 0; i < 10; i++) {
            statsComponent.starImages[i] = new Image(skin.getDrawable(i % 2 == 0 ? "left_star" : "right_star"));
            statsComponent.starImages[i].setSize(iconSize, iconSize);
            stage.addActor(statsComponent.starImages[i]);
        }

        statsComponent.heartImages = new Image[12];
        for (int i = 0; i < 12; i++) {
            String regionName;
            switch (i % 4) {
                case 0: regionName = "top_left_heart"; break;
                case 1: regionName = "bottom_left_heart"; break;
                case 2: regionName = "bottom_right_heart"; break;
                default: regionName = "top_right_heart"; break;
            }
            statsComponent.heartImages[i] = new Image(skin.getDrawable(regionName));
            statsComponent.heartImages[i].setSize(iconSize, iconSize);
            stage.addActor(statsComponent.heartImages[i]);
        }

        float padding = Gdx.graphics.getWidth() / 40f;
        float yOffset = Gdx.graphics.getHeight() - iconSize - 2 * padding;

        // Create and position score label
        statsComponent.scoreLabel = new Label("Score:\n0P", skin);
        statsComponent.scoreLabel.setSize(Gdx.graphics.getWidth() / 10f, Gdx.graphics.getHeight() / 8f);
        statsComponent.scoreLabel.setPosition(padding, yOffset);
        stage.addActor(statsComponent.scoreLabel);

        // Calculate starting x position after score label
        float currentX = statsComponent.scoreLabel.getX() + statsComponent.scoreLabel.getWidth() + padding*2;

        // Position heart images in a row
        for (int i = 0; i < 3; i++) {
            float x = currentX + i * iconSize * 1.5f;
            float pad = iconSize / 2;
            float heartPad = i * pad ;
            statsComponent.heartImages[i*4].setPosition(x - pad + heartPad, yOffset + pad + iconSize /2);
            statsComponent.heartImages[i*4+1].setPosition(x - pad + heartPad, yOffset - pad + iconSize /2);
            statsComponent.heartImages[i*4+2].setPosition(x + pad + heartPad, yOffset - pad + iconSize /2);
            statsComponent.heartImages[i*4+3].setPosition(x + pad + heartPad, yOffset + pad + iconSize /2);
        }

        // Add health label after hearts
        currentX += 3 * iconSize * 1.5f + padding*2;
        statsComponent.healthLabel = new Label("0H", skin);
        statsComponent.healthLabel.setSize(Gdx.graphics.getWidth() / 10f, Gdx.graphics.getHeight() / 8f);
        statsComponent.healthLabel.setPosition(currentX, yOffset);
        stage.addActor(statsComponent.healthLabel);

        // Position star images after health label
        currentX += statsComponent.healthLabel.getWidth() - padding;
        for (int i = 0; i < 10; i++) {
            statsComponent.starImages[i].setPosition(currentX + ((float) i / 2) * (iconSize * 2f), yOffset + iconSize /1.2f);
        }

        // Add stars label after stars
        currentX += 5 * iconSize * 2f + padding;
        statsComponent.starsLabel = new Label("0S", skin);
        statsComponent.starsLabel.setSize(Gdx.graphics.getWidth() / 10f, Gdx.graphics.getHeight() / 8f);
        statsComponent.starsLabel.setPosition(currentX, yOffset);
        stage.addActor(statsComponent.starsLabel);

        statsComponent.addHealthLevel(3);
        //statsComponent.addStarLevel(21);
    }

    @Override
    public void update(float deltaTime) {
        regenerationTime += deltaTime;
        if (regenerationTime > statsComponent.regenerationRate) {
            regenerationTime = 0f;
            statsComponent.addHealthLevel(statsComponent.regeneration);
        }


        stage.act(deltaTime);
        stage.draw();
    }
}
