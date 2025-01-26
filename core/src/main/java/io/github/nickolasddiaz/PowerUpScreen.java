package io.github.nickolasddiaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static io.github.nickolasddiaz.utils.MapGenerator.itemSize;

// four upgrade types jade gold silver bronze
// three upgrade options

public class PowerUpScreen extends InputAdapter implements Screen {
    private final yourgame game;
    private final Stage stage;
    private int rollNumber;
    private Table option1, option2, option3;
    private final ImageTextButton reRollButton;
    private final Random random;

    // Enum for all possible upgrades with rarity
    public enum UpgradeType {
        // Player Attributes
        REGENERATION(Rarity.SILVER, "Regeneration", "Increases health regeneration"),
        SPEED(Rarity.BRONZE, "Speed Boost", "Increases player movement speed"),
        HEALTH_INCREASE(Rarity.BRONZE, "Health Boost", "multiplies and adds your current health"),
        ALLY_SPAWNER(Rarity.GOLD, "Ally Spawner", "Spawns friendly allies more rappidly"),
        EXTRA_SHOT(Rarity.GOLD, "Extra Shot", "Adds an additional bullet per shot"),
        BACK_SHOTS(Rarity.SILVER, "Back Shots", "Shoots additional bullets behind"),
        BETTER_ARMOR(Rarity.GOLD, "Improved Armor", "Reduces damage taken upon being hit"),
        LUCK(Rarity.SILVER, "Lucky Charm", "Increases chance of better upgrades"),
        ALL_AROUND(Rarity.GOLD, "All Around", "Increases health regeneration, speed, health and damage"),

        // Weapon Modifications
        BULLET_SPEED(Rarity.BRONZE, "Bullet Speed", "Increases projectile velocity"),
        BULLET_DAMAGE(Rarity.SILVER, "Bullet Damage", "Increases bullet damage"),
        BULLET_SIZE(Rarity.BRONZE, "Bullet Size", "Makes bullets much bigger"),
        FIRE_RATE(Rarity.SILVER, "Fire Rate", "Increases the rate at which bullets are fired"),
        CRITICAL_DAMAGE(Rarity.SILVER, "Critical Damage", "Increases critical hit multiplier"),
        CRITICAL_CHANCE(Rarity.SILVER, "Critical Chance", "Increases critical hit probability"),
        MISSILE(Rarity.JADE, "Missile Rate", "Gain the ability to shoot missiles"),
        MINE(Rarity.JADE, "Mine Rate", "Gain the ability to drop mines"),

        // Special Effects
        FREEZE_SHOT(Rarity.SILVER, "Freeze Shot", "Temporarily freezes enemies"),
        BURN_DAMAGE(Rarity.BRONZE, "Burn Damage", "Adds burning damage over time"),
        EXPLOSIVE_INCREASE(Rarity.GOLD, "Explosive Power", "Increases explosion damage and radius"),
        POINT_MULTIPLIER(Rarity.GOLD, "Point Boost", "Increases points earned"),
        DECREASE_WANTED_LEVEL(Rarity.BRONZE, "Reduce Star Level", "Reduces enemy aggression"),
        RE_ROLL_NUMBER(Rarity.SILVER, "Re-roll", "Allows for additional upgrade re-rolls"),
        DESTROY_HOUSES(Rarity.JADE, "Destroy Houses", "Gain the ability to destroy houses");

        public enum Rarity {
            BRONZE(50),
            SILVER(30),
            GOLD(15),
            JADE(5);

            private final int probability;

            Rarity(int probability) {
                this.probability = probability;
            }

            public int getProbability() {
                return probability;
            }
        }

        private final Rarity rarity;
        private final String name;
        private final String description;

        UpgradeType(Rarity rarity, String name, String description) {
            this.rarity = rarity;
            this.name = name;
            this.description = description;
        }

        public Rarity getRarity() {
            return rarity;
        }
        public Rarity getUpgradedRarity() {
            return Rarity.values()[rarity.ordinal() + 1];
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public PowerUpScreen(final yourgame game, Random random) {
        this.game = game;
        this.random = random;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        rollNumber = game.statsComponent.reRollNumber;

        // Initial upgrade selection
        selectUpgrades();

        // Re-roll button setup
        reRollButton = new ImageTextButton("Re-roll: " + rollNumber, game.skin, "re-roll");
        reRollButton.setPosition(10, 0); // Bottom left position
        reRollButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (rollNumber > 0) {
                    rollNumber--;
                    selectUpgrades();
                    reRollButton.setText("Re-roll: " + rollNumber);
                }
            }
        });

        stage.addActor(reRollButton);
    }

    private void selectUpgrades() {
        // Get upgrades based on luck
        List<UpgradeType> selectedUpgrades = getRandomUpgrades(game.statsComponent.luck);
        // Setup upgrade buttons
        setupUpgradeButton(option1, selectedUpgrades.get(0), 0);
        setupUpgradeButton(option2, selectedUpgrades.get(1), 1);
        setupUpgradeButton(option3, selectedUpgrades.get(2), 2);
    }

    private List<UpgradeType> getRandomUpgrades(int luck) {
        List<UpgradeType> selectedUpgrades = new ArrayList<>();

        while (selectedUpgrades.size() < 3) {
            // Adjust probabilities based on luck
            UpgradeType upgrade = getWeightedRandomUpgrade(luck);

            // Ensure no duplicates
            if (!selectedUpgrades.contains(upgrade)) {
                selectedUpgrades.add(upgrade);
            }
        }

        return selectedUpgrades;
    }

    private UpgradeType getWeightedRandomUpgrade(int luck) {
        // Luck modifies the probability distribution
        List<UpgradeType> upgrades = Arrays.stream(UpgradeType.values())
            .sorted((a, b) -> {
                // Adjust probabilities based on luck
                int luckMultiplier = Math.max(1, luck);
                return Integer.compare(
                    b.getRarity().getProbability() * luckMultiplier,
                    a.getRarity().getProbability() * luckMultiplier
                );
            })
            .collect(Collectors.toList());

        int totalWeight = upgrades.stream()
            .mapToInt(u -> u.getRarity().getProbability() * Math.max(1, luck))
            .sum();

        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (UpgradeType upgrade : upgrades) {
            cumulativeWeight += upgrade.getRarity().getProbability() * Math.max(1, luck);
            if (randomValue < cumulativeWeight) {
                return upgrade;
            }
        }

        // Fallback
        return upgrades.get(0);
    }

    private void setupUpgradeButton(Table button, UpgradeType upgrade, int index) {
        if (button != null) {
            stage.getActors().removeValue(button, true);
        }
        boolean threeXMulti = false;
        if(upgrade.rarity != UpgradeType.Rarity.JADE)
            threeXMulti = random.nextFloat() < 0.1f * game.statsComponent.luck/100;

        button = new Table();
        button.setWidth(190); // Reduced width
        button.setHeight(320); // Set a fixed height
        button.pad(10); // Add padding

        // Background and hover effect
        button.setBackground(game.skin.getDrawable("poweruppage"));

        Label label = new Label(upgrade.getName() + (threeXMulti ? " (x3)" : ""), game.skin, "title");
        label.setFontScale(0.5f);
        label.setWrap(true);
        button.add(label).expandX().fillX().center().padBottom(10);

        button.row();
        Label descLabel = new Label(upgrade.getDescription(), game.skin);
        descLabel.setFontScale(0.8f);
        descLabel.setWrap(true);
        button.add(descLabel).expandX().fillX().center().padBottom(10);

        button.row();
        Image image = new Image(game.skin.getDrawable(upgrade.getName()));
        image.setColor(getRarityStyle((threeXMulti ? upgrade.getUpgradedRarity() : upgrade.getRarity())));
        button.add(image).size(120).center(); // Fixed image size

        // Calculate button positioning
        float screenWidth = Gdx.graphics.getWidth();
        float buttonWidth = 200;
        float spacing = (screenWidth - (3 * buttonWidth)) / 4;

        button.setPosition(
            spacing + (index * (buttonWidth + spacing)) + 10,
            Gdx.graphics.getHeight() / 2f - 150 // Adjusted to center vertically
        );

        // Hover effect
        Table finalButton = button;
        boolean finalThreeXMulti = threeXMulti;
        button.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                changed(upgrade, finalThreeXMulti);
                return true;
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                finalButton.setBackground(game.skin.getDrawable("poweruppagegrey"));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                finalButton.setBackground(game.skin.getDrawable("poweruppage"));
            }
        });

        stage.addActor(button);

        // Store references for potential later manipulation
        switch (index) {
            case 0: option1 = button; break;
            case 1: option2 = button; break;
            case 2: option3 = button; break;
        }
    }
    private void changed(UpgradeType upgrade, boolean finalThreeXMulti) {
        game.settings.paused = false;
        applyUpgrade(upgrade, (finalThreeXMulti ? 3 : 1));
        game.statsComponent.upgrade = false;
        dispose();
    }

    private Color getRarityStyle(UpgradeType.Rarity rarity) {
        switch (rarity) {
            case JADE: return new Color(0x57ffc1FF);
            case GOLD: return new Color(0xFFD700FF);
            case SILVER: return new Color(0xf2f2f2FF);
            default: return new Color(0xebccadFF); // BRONZE
        }
    }
    private void applyUpgrade(UpgradeType upgrade, int multiplier) {
        switch (upgrade) {
            case REGENERATION: game.statsComponent.regeneration += multiplier;break;
            case SPEED: game.playerComponent.SPEED += itemSize * 10f * multiplier;break;
            case HEALTH_INCREASE: game.statsComponent.addHealthLevel(15 * multiplier);break;
            case ALLY_SPAWNER: game.playerComponent.allySpawnerRate /= 1.4f * multiplier;break;
            case EXTRA_SHOT: game.playerComponent.amountOfBullets += multiplier;break;
            case BACK_SHOTS: game.playerComponent.backShotsAmount += multiplier;break;
            case BETTER_ARMOR: game.statsComponent.reduceDamage += multiplier;break;
            case LUCK: game.statsComponent.luck += multiplier;break;
            case BULLET_SPEED: game.playerComponent.bulletSpeed += 2f * itemSize * multiplier;break;
            case BULLET_DAMAGE: game.playerComponent.bulletDamage += 2 * multiplier;break;
            case BULLET_SIZE: game.playerComponent.bulletSize += 0.4f * multiplier;break;
            case FIRE_RATE: game.playerComponent.fireRate *= 0.8f * multiplier;break;
            case CRITICAL_DAMAGE: game.playerComponent.criticalDamageMultiplier += .5f * multiplier;break;
            case CRITICAL_CHANCE: game.playerComponent.criticalChance += 0.25f * multiplier;break;
            case FREEZE_SHOT: game.playerComponent.freezeAmount += 0.5f * multiplier;break;
            case BURN_DAMAGE: game.playerComponent.burnAmount += 2 *multiplier;break;
            case EXPLOSIVE_INCREASE: game.playerComponent.explosiveRadiusAndDamage += multiplier;break;
            case POINT_MULTIPLIER: game.statsComponent.pointMultiplier += 0.4f * multiplier;break;
            case DECREASE_WANTED_LEVEL: game.statsComponent.addStarLevel(-multiplier*3);break;
            case RE_ROLL_NUMBER: game.statsComponent.reRollNumber += multiplier;break;
            case DESTROY_HOUSES: game.playerComponent.CanDestroy = true;break;
            case MISSILE: game.playerComponent.CanShootMissile = true; game.playerComponent.missileRate *= .8f * multiplier;  break;
            case MINE: game.playerComponent.CanShootMine = true; game.playerComponent.mineRate *= .7f * multiplier; break;
            case ALL_AROUND:
                game.statsComponent.regeneration += multiplier;
                game.playerComponent.SPEED += itemSize * 10f * multiplier;
                game.statsComponent.addHealthLevel(15 * multiplier);
                game.playerComponent.bulletSpeed += 2f * itemSize * multiplier;
                game.playerComponent.bulletDamage += 2 * multiplier;
                break;
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
