package io.github.nickolasddiaz.utils;

public final class CollisionCategory {
    public static final short HORIZONTAL_ROAD = 1;
    public static final short VERTICAL_ROAD =   2;
    public static final short STRUCTURE =       4;
    public static final short DECORATION =      8;
    public static final short OCEAN =           16;
    public static final short CAR =             32;
    public static final short ENEMY =           64;
    public static final short PLAYER =          128;
    public static final short ALLY =            256;
    public static final short P_BULLET =        512;
    public static final short P_MISSILE =       1024;
    public static final short P_MINE =          2048;
    public static final short E_BULLET =        4096;
    public static final short E_MISSILE =       8192;
    public static final short E_MINE =          16384;
    //16 options possible

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
                            return STRUCTURE_FILTER | VEHICLE_FILTER;
            case P_BULLET: case P_MISSILE: case P_MINE: case E_BULLET: case E_MISSILE: case E_MINE:
                            return STRUCTURE;
            case OCEAN:     return VEHICLE_FILTER;
            case STRUCTURE: return VEHICLE_FILTER | PROJECTILE_FILTER;
            default:        return 0x0; // CAR, HORIZONTAL_ROAD, VERTICAL_ROAD, and DECORATION will not get collision boundaries no matter what
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
}
