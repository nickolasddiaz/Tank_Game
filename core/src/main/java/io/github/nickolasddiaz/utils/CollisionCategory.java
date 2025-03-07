package io.github.nickolasddiaz.utils;

import com.badlogic.gdx.graphics.Color;

import java.util.Random;

public final class CollisionCategory {
    public static final String GAME_SETTINGS = "game_settings";
    public static final String SCORE_NAME = "game_leaderboard";
    public static final String SCORES_KEY = "player_scores";
    public static final String PLAYER_NAME = "player_name";

    public static final short HORIZONTAL_ROAD = 0x1;
    public static final short VERTICAL_ROAD =   0x1 << 1;
    public static final short STRUCTURE =       0x1 << 2;
    public static final short DECORATION =      0x1 << 3;
    public static final short OCEAN =           0x1 << 4;
    public static final short CAR =             0x1 << 5;
    public static final short ENEMY =           0x1 << 6;
    public static final short PLAYER =          0x1 << 7;
    public static final short ALLY =            0x1 << 8;
    public static final short P_BULLET =        0x1 << 9;
    public static final short P_MISSILE =       0x1 << 10;
    public static final short P_MINE =          0x1 << 11;
    public static final short E_BULLET =        0x1 << 12;
    public static final short E_MISSILE =       0x1 << 13;
    public static final short E_MINE =          0x1 << 14;
    //16 options possible

    public static final short ROAD = HORIZONTAL_ROAD | VERTICAL_ROAD;           //3
    public static final short STRUCTURE_FILTER = STRUCTURE | DECORATION | OCEAN;//28
    public static final short PLAYER_FILTER = P_BULLET | P_MISSILE | P_MINE;    //3584
    public static final short ENEMY_FILTER = E_BULLET | E_MISSILE | E_MINE;     //28627
    public static final short PROJECTILE_FILTER = PLAYER_FILTER | ENEMY_FILTER; //32256
    public static final short VEHICLE_FILTER = PLAYER | ENEMY | ALLY;           //448

    public CollisionCategory() {
        // Prevent instantiation
    }

    public static short categoryToFilterBits(short category) {
        switch (category) { //both collisions have to be true in both ways example code bool collide = (filterA.maskBits & filterB.categoryBits) != 0 && (filterA.categoryBits & filterB.maskBits) != 0;
            //every filter here added will make collision boundaries
            case PLAYER: case ALLY: case ENEMY:
                            return STRUCTURE_FILTER | VEHICLE_FILTER | ROAD | CAR | PROJECTILE_FILTER;
            case P_BULLET: case P_MISSILE: case P_MINE: case E_BULLET: case E_MISSILE: case E_MINE:
                            return STRUCTURE | VEHICLE_FILTER | CAR;
            case OCEAN: case HORIZONTAL_ROAD: case VERTICAL_ROAD: case DECORATION:
                            return VEHICLE_FILTER;
            case STRUCTURE: case CAR:
                return VEHICLE_FILTER | PROJECTILE_FILTER;
            default:        return 0x0;
        }
    }

    public short getFilterBit(String name){
        switch (name){
            case "HORIZONTAL":  return HORIZONTAL_ROAD;
            case "VERTICAL":    return VERTICAL_ROAD;
            case "STRUCTURE":   return STRUCTURE;
            case "DECORATION":  return DECORATION;
            case "OCEAN":       return OCEAN;
            case "CAR":         return CAR;
            case "ENEMY":       return ENEMY;
            case "PLAYER":      return PLAYER;
            case "ALLY":        return ALLY;
            case "P_BULLET":    return P_BULLET;
            case "P_MISSILE":   return P_MISSILE;
            case "P_MINE":      return P_MINE;
            case "E_BULLET":    return E_BULLET;
            case "E_MISSILE":   return E_MISSILE;
            case "E_MINE":      return E_MINE;
            default:            return 0x0;
        }
    }
    public static Color teamColor(boolean team){
        return team ? Color.FOREST : Color.FIREBRICK;
    }
    public static String Type(Random random, short category){
        switch (category){
            case ENEMY: case PLAYER: case ALLY:
                return "" + random.nextInt(14);
            case P_BULLET: case E_BULLET:
                //return "bullet" + random.nextInt(10);
                return "bullet";
            case P_MISSILE: case E_MISSILE:
                return "missile" + random.nextInt(6);
            case P_MINE: case E_MINE:
                return "mine" + random.nextInt(3);
            case CAR:
                return "car" + random.nextInt(20);
            default: return "";
        }
    }
    public static float getEnemySpawnRate(float stars){ //Power Law Decay for spawn rate in seconds, https://desmos.com/calculator/zgbmhyxujn
        return Math.max(0.2f, 6f / (float) Math.pow(stars + 1, 0.3));

    }
}
