package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.GraphNode;

import static io.github.nickolasddiaz.utils.MapGenerator.*;


public class EnemySystem extends IteratingSystem {
    private final ComponentMapper<EnemyComponent> EnemyMapper;
    private final ComponentMapper<ChunkComponent> chunkMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final TransformComponent player;
    private final SettingsComponent settings;
    private final Engine engine;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BulletFactory bulletFactory;



    public EnemySystem(Engine engine, TransformComponent player, CameraComponent camera, SettingsComponent settings, BulletFactory bulletFactory) {
        super(Family.all(EnemyComponent.class, ChunkComponent.class, TransformComponent.class).get());
        EnemyMapper = ComponentMapper.getFor(EnemyComponent.class);
        chunkMapper = ComponentMapper.getFor(ChunkComponent.class);
        transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.player = player;
        this.engine = engine;
        shapeRenderer.setProjectionMatrix(camera.camera.combined);
        shapeRenderer.setColor(Color.GREEN);
        this.settings = settings;
        this.bulletFactory = bulletFactory;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyComponent enemyComponent = EnemyMapper.get(entity);
        ChunkComponent chunk = chunkMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);

        //make sure the enemy is in a valid chunk and still alive
        Vector2 chunkPosition = new Vector2(
            (int) Math.floor(transform.position.x / chunkSize),
            (int) Math.floor(transform.position.y / chunkSize)
        );

        Vector2 targetPosition = player.position;
        transform.turretRotation = (float) Math.toDegrees(Math.atan2(targetPosition.y - transform.position.y, targetPosition.x - transform.position.x));

        if (!chunk.mapChunks.containsKey(chunkPosition) || enemyComponent.health <= 0) {
            Gdx.app.log("EnemySystem", "Removing enemy entity");
            engine.removeEntity(entity);
            return;
        }

        enemyComponent.timeSinceLastShot += deltaTime;
        if(enemyComponent.timeSinceLastShot > enemyComponent.fireRate){
            enemyComponent.timeSinceLastShot = 0f;
            if(player.position.dst(transform.position) < chunkSize)
                bulletFactory.createBullet(transform.position.cpy(), transform.turretRotation + (chunk.random.nextFloat()-.5f)*10f, enemyComponent.bulletSpeed, enemyComponent.bulletDamage, Color.RED);
        }

        // Check if enemy is within minimum distance of player
        float distanceToPlayer = transform.position.dst(player.position);
        if (distanceToPlayer <= enemyComponent.minDistance) {
            transform.movement = Vector2.Zero;
            return;
        }

        // Only recalculate path if player has moved outside the lazy path rectangle or if we don't have a valid path yet
        if (!enemyComponent.lazyPath.contains(player.position) || enemyComponent.path.getCount() == 0) {
            Vector2 startPos = chunk.worldToGridCoordinates(transform.position);
            Vector2 endPos = chunk.worldToGridCoordinates(player.position);

            GraphNode startNode = chunk.pathfindingGraph.getNodeAt(startPos);
            GraphNode endNode = chunk.pathfindingGraph.getNodeAt(endPos);

            // Handle null nodes by using previous path or creating direct path to player
            if (startNode == null || endNode == null) {
                if (enemyComponent.previousPath != null && enemyComponent.previousPath.getCount() > 0) {
                    Gdx.app.log("EnemySystem", "Using previous path as fallback");
                    enemyComponent.path = enemyComponent.previousPath;
                } else {
                    // Create a simple direct path to the player if no previous path exists
                    DefaultGraphPath<GraphNode> directPath = new DefaultGraphPath<>();
                    GraphNode simpleStartNode = new GraphNode(startPos);
                    GraphNode simpleEndNode = new GraphNode(endPos);
                    directPath.add(simpleStartNode);
                    directPath.add(simpleEndNode);
                    enemyComponent.path = directPath;
                    Gdx.app.log("EnemySystem", "Created direct path to player as fallback");
                }
            } else {
                // Store the current path as previous path before calculating new one
                if (enemyComponent.path.getCount() > 0) {
                    enemyComponent.previousPath = new DefaultGraphPath<>();
                    for (GraphNode node : enemyComponent.path) {
                        enemyComponent.previousPath.add(node);
                    }
                }

                IndexedAStarPathFinder<GraphNode> pathFinder = new IndexedAStarPathFinder<>(chunk.pathfindingGraph);
                boolean pathFound = pathFinder.searchNodePath(startNode, endNode, new ManhattanDistance(), enemyComponent.path);

                if (pathFound) {
                    enemyComponent.lazyPath.set(
                        player.position.x - TILE_SIZE,
                        player.position.y - TILE_SIZE,
                        TILE_SIZE * 2,
                        TILE_SIZE * 2
                    );
                    Gdx.app.log("EnemySystem", "Path found with " + enemyComponent.path.getCount() + " nodes");
                } else {
                    Gdx.app.log("EnemySystem", "No path found, using previous path");
                    if (enemyComponent.previousPath != null) {
                        enemyComponent.path = enemyComponent.previousPath;
                    }
                }
            }
        }

        // Move the enemy along its unique path
        if (enemyComponent.nextPathWorld != null && transform.position.dst(enemyComponent.nextPathWorld) <= 3 * itemSize) {
            getNextPath(chunk, transform.position, enemyComponent);
        }

        if (enemyComponent.path.getCount() > 1 || enemyComponent.pathIndex < enemyComponent.path.getCount() - 1) {
            moveEnemy(transform, enemyComponent, deltaTime);
            if(settings.DEBUG) {
                renderNextPathRect(transform.position, enemyComponent.nextPathWorld, enemyComponent);
            }
        }
    }

    private void renderNextPathRect(Vector2 enemy, Vector2 path, EnemyComponent car) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.rect(car.nextPathRect.x, car.nextPathRect.y, car.nextPathRect.width, car.nextPathRect.height);
        shapeRenderer.line(enemy, path);
        shapeRenderer.end();
    }

    private void getNextPath(ChunkComponent chunk, Vector2 enemyPosition, EnemyComponent enemyComponent) {
        do {
            if (enemyComponent.pathIndex < enemyComponent.path.getCount() - 1) {
                enemyComponent.nextPath.set(enemyComponent.path.get(++enemyComponent.pathIndex).position);
            } else {
                break;
            }
        } while (chunk.isObjectInRay(chunk.GridToWorldCoordinates(enemyComponent.nextPath), enemyPosition, chunk.obstaclesFilter));
        enemyComponent.nextPathWorld = chunk.GridToWorldCoordinates(enemyComponent.nextPath);
        enemyComponent.nextPathRect.set(
            enemyComponent.nextPathWorld.x,
            enemyComponent.nextPathWorld.y,
            itemSize,
            itemSize
        );
    }

    private void moveEnemy(TransformComponent transform, EnemyComponent enemyComponent, float deltaTime) {
        Vector2 direction = new Vector2(enemyComponent.nextPathWorld).sub(transform.position).nor();

        // Gradual rotation
        float targetAngle = direction.angleDeg();
        float angleDifference = ((targetAngle - transform.rotation + 540) % 360) - 180;

        if (Math.abs(angleDifference) > 5) {
            float turnSpeed = Math.min(enemyComponent.spinSpeed * deltaTime, Math.abs(angleDifference) * 0.5f);
            transform.rotation += Math.signum(angleDifference) * turnSpeed;
        } else {
            transform.rotation = targetAngle;
        }
        transform.rotation = (transform.rotation + 360) % 360;

        // Movement
        float angleRad = (float) Math.toRadians(transform.rotation);
        transform.movement = new Vector2((float) Math.cos(angleRad), (float) Math.sin(angleRad)).scl(enemyComponent.speed * deltaTime);
    }


    public static class ManhattanDistance implements Heuristic<GraphNode> {
        @Override
        public float estimate(GraphNode node, GraphNode endNode) {
            return Math.abs(node.position.x - endNode.position.x) + Math.abs(node.position.y - endNode.position.y);
        }
    }
}
