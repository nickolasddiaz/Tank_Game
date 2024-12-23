package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

public class ChunkComponent implements Component {
    public HashMap<Vector2, TiledMap> mapChunks = new HashMap<>();
    public Vector2 currentChunk = new Vector2(0, 0);
}
