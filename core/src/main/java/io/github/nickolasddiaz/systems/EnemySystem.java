package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;

import static io.github.nickolasddiaz.systems.MapGenerator.chunkSize;
import static io.github.nickolasddiaz.systems.MapGenerator.TILE_SIZE;


public class EnemySystem extends IteratingSystem {
    private final ComponentMapper<EnemyComponent> EnemyMapper;
    private final ComponentMapper<ChunkComponent> chunkMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final TransformComponent player;
    private final Engine engine;
    private final Rectangle lazyPath = new Rectangle();
    private GraphPath<WorldNode> path;

    public EnemySystem(Engine engine, TransformComponent player) {
        super(Family.all(EnemyComponent.class, ChunkComponent.class, TransformComponent.class).get());
        EnemyMapper = ComponentMapper.getFor(EnemyComponent.class);
        chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.player = player;
        this.engine = engine;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyComponent car = EnemyMapper.get(entity);
        ChunkComponent chunk = chunkMapper.get(entity);
        TransformComponent enemy = transformMapper.get(entity);

        Vector2 chunkPosition = new Vector2((int) Math.floor(enemy.position.x / chunkSize), (int) Math.floor(enemy.position.y / chunkSize));
        if(!chunk.mapChunks.containsKey(chunkPosition) || car.health <= 0) {
            engine.removeEntity(entity);
            return;
        }

        // Move enemy towards player
        if(path != null && path.getCount() > 1) {
            WorldNode nextNode = path.get(path.getCount() - 2);
            Vector2 nextPosition = new Vector2(nextNode.position.x, nextNode.position.y);
            Vector2 direction = nextPosition.sub(enemy.position).nor();
            enemy.position.add(direction.scl(car.speed * deltaTime));
        }

        if(lazyPath.contains(player.position)) {
            return;
        }
        lazyPath.set(player.position.x - TILE_SIZE, player.position.y - TILE_SIZE, TILE_SIZE, TILE_SIZE);

        // Get nearest nodes to start and end positions
        WorldNode startNode = chunk.pathfindingGraph.getNodeAt(enemy.position.x, enemy.position.y);
        WorldNode endNode = chunk.pathfindingGraph.getNodeAt(player.position.x, player.position.y);

        // Use LibGDX pathfinding
        IndexedAStarPathFinder<WorldNode> pathFinder = new IndexedAStarPathFinder<>(chunk.pathfindingGraph);
        path = new DefaultGraphPath<>();
        Gdx.app.log("test", path.getCount() + "");
        pathFinder.searchNodePath(startNode, endNode, new ManhattanDistance(), path);

    }

    public static class ManhattanDistance implements Heuristic<WorldNode> {
        @Override
        public float estimate(WorldNode node, WorldNode endNode) {
            return Math.abs(node.position.x - endNode.position.x) + Math.abs(node.position.y - endNode.position.y);
        }
    }
}
