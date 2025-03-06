package io.github.nickolasddiaz.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.GraphNode;

import static io.github.nickolasddiaz.utils.MapGenerator.*;

public class EnemySystem extends IteratingSystem {
    private final ComponentMapper<EnemyComponent> enemyMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ChunkComponent chunk;
    private final TransformComponent player;
    private final SettingsComponent settings;
    private final Engine engine;

    public EnemySystem(Engine engine, TransformComponent player, ChunkComponent chunk, SettingsComponent settings) {
        super(Family.all(EnemyComponent.class, TransformComponent.class).get());
        this.enemyMapper = ComponentMapper.getFor(EnemyComponent.class);
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.player = player;
        this.engine = engine;
        this.chunk = chunk;
        this.settings = settings;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyComponent enemyComponent = enemyMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);

        transform.velocity.setZero();

        // Check if enemy is in valid chunk and alive
        Vector2 chunkPosition = chunk.getChunkPosition(transform.getPosition());
        if (!chunk.mapChunks.containsKey(chunkPosition) || transform.health <= 0) {
            transform.health = 0;
            return;
        }

        if(settings.DEBUG)
            debug(transform.getPosition(), enemyComponent.nextPathWorld);

        // Handle turret rotation
        updateTurretRotation(transform, enemyComponent);

        // Check distance to player
        float distanceToPlayer = transform.getPosition().dst(player.getPosition());
        if (distanceToPlayer <= enemyComponent.minDistance) {
            end(enemyComponent, transform, deltaTime);
            return;
        }

        // Update pathfinding
        updatePathfinding(transform, enemyComponent, deltaTime);

        getNextPath(chunk, transform.getPosition(), enemyComponent);

        // Move enemy
        if (enemyComponent.path.getCount() > 1 || enemyComponent.pathIndex < enemyComponent.path.getCount() - 1) {
            moveEnemy(transform, enemyComponent, deltaTime);
        }

        end(enemyComponent, transform, deltaTime);
    }
    private void end(EnemyComponent enemyComponent, TransformComponent transform, float deltaTime) {
        enemyComponent.stats.health = transform.health;
        transform.velocity = enemyComponent.stats.emulate(deltaTime, transform.getPosition(), transform.turretRotation, transform.velocity,
            (Math.abs(player.getPosition().dst(transform.getPosition())) < chunkSize/3f));
        transform.health = enemyComponent.stats.health;
    }

    private void getNextPath(ChunkComponent chunk, Vector2 Position, EnemyComponent enemyComponent) {
        if(Position.dst(enemyComponent.nextPathWorld) < itemSize*itemSize*itemSize && enemyComponent.pathIndex < enemyComponent.path.getCount() - 1) {
            enemyComponent.nextPathWorld.set(chunk.GridToWorldCoordinates((enemyComponent.path.get(++enemyComponent.pathIndex).position)));
        }
    }

    private void updatePathfinding(TransformComponent transform, EnemyComponent enemyComponent, float deltaTime) {
        // Only recalculate path if player has moved outside the lazy path rectangle or if we don't have a valid path yet
        enemyComponent.timeSinceLastPathfinding += deltaTime;
        if (enemyComponent.timeSinceLastPathfinding < enemyComponent.pathfindingCooldown) {
            return;
        }
        enemyComponent.timeSinceLastPathfinding = 0f;

        // Only recalculate path if player has moved outside the lazy path rectangle or if we don't have a valid path yet
        if (!enemyComponent.lazyPath.contains(player.getPosition()) || enemyComponent.path.getCount() == 0) {
            Vector2 startPos = chunk.worldToGridCoordinates(transform.getPosition());
            Vector2 endPos = chunk.worldToGridCoordinates(player.getPosition());

            GraphNode startNode = chunk.pathfindingGraph.getNodeAt(startPos);
            GraphNode endNode = chunk.pathfindingGraph.getNodeAt(endPos);

            // Handle null nodes by using previous path or creating direct path to player
            if (startNode == null || endNode == null) {
                if (enemyComponent.previousPath != null && enemyComponent.previousPath.getCount() > 0) {
                    enemyComponent.path = enemyComponent.previousPath;
                } else {
                    // Create a simple direct path to the player if no previous path exists
                    DefaultGraphPath<GraphNode> directPath = new DefaultGraphPath<>();
                    GraphNode simpleStartNode = new GraphNode(startPos);
                    GraphNode simpleEndNode = new GraphNode(endPos);
                    directPath.add(simpleStartNode);
                    directPath.add(simpleEndNode);
                    enemyComponent.path = directPath;
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
                enemyComponent.path.clear();
                boolean pathFound = pathFinder.searchNodePath(startNode, endNode, new ManhattanDistance(), enemyComponent.path);

                if (pathFound) {
                    enemyComponent.lazyPath.set(
                        player.getPosition().x - TILE_SIZE,
                        player.getPosition().y - TILE_SIZE,
                        TILE_SIZE * 2,
                        TILE_SIZE * 2
                    );
                    enemyComponent.pathIndex = 0;
                    enemyComponent.nextPathWorld.set(chunk.GridToWorldCoordinates((enemyComponent.path.get(enemyComponent.pathIndex).position)));
                } else {
                    if (enemyComponent.previousPath != null) {
                        enemyComponent.path = enemyComponent.previousPath;
                    }
                }
            }
        }


    }

    private void updateTurretRotation(TransformComponent transform, EnemyComponent enemyComponent) {
        if (enemyComponent.stats.team) {
            transform.turretRotation = player.turretRotation;
        } else {
            Vector2 targetPosition = player.getPosition();
            transform.turretRotation = (float) Math.toDegrees(
                Math.atan2(
                    targetPosition.y - transform.getPosition().y,
                    targetPosition.x - transform.getPosition().x
                )
            );
        }
    }

    private void moveEnemy(TransformComponent transform, EnemyComponent enemyComponent, float deltaTime) {
        // Get current position and target position
        Vector2 currentPos = transform.getPosition();
        Vector2 targetPos = new Vector2(enemyComponent.nextPathWorld);

        // Only change direction if we're not too close to the target
        float distanceToTarget = currentPos.dst(targetPos);
        if (distanceToTarget > 0.5f) { // Adjust this threshold as needed
            // To Calculate direction vector to the target
            Vector2 direction = new Vector2(targetPos).sub(currentPos).nor();

            // Calculate target angle in degrees
            float targetAngle = direction.angleDeg();

            // Current rotation in degrees
            float currentRotation = transform.rotation;

            // Calculate the shortest angle difference (between -180 and 180)
            float angleDifference = ((targetAngle - currentRotation + 180) % 360) - 180;

            // Apply rotation with smoothing and dampening
            float turnSpeed = enemyComponent.stats.spinSpeed;

            // Gradually reduce turning speed as we get closer to the target angle
            float turnFactor = Math.min(1.0f, Math.abs(angleDifference) / 45.0f);
            float actualTurnSpeed = turnSpeed * turnFactor * deltaTime;

            // Apply rotation with a minimum threshold to avoid micro-adjustments
            if (Math.abs(angleDifference) > 1.0f) {
                transform.rotation += Math.signum(angleDifference) * actualTurnSpeed;
            } else {
                transform.rotation = targetAngle; // Snap to exact angle when very close
            }

            // Normalize rotation to 0-360 range
            transform.rotation = (transform.rotation + 360) % 360;
        }

        // Apply forward movement in the direction the entity is facing
        float angleInRadians = (float) Math.toRadians(transform.rotation);
        transform.velocity = new Vector2(
            (float) Math.cos(angleInRadians) * enemyComponent.stats.speed,
            (float) Math.sin(angleInRadians) * enemyComponent.stats.speed
        );
        if(Math.abs(player.getPosition().dst(transform.getPosition())) > chunkSize/2f)
            transform.velocity.scl(4);
    }

    private static class ManhattanDistance implements Heuristic<GraphNode> {
        @Override
        public float estimate(GraphNode node, GraphNode endNode) {
            return Math.abs(node.position.x - endNode.position.x) +
                Math.abs(node.position.y - endNode.position.y);
        }
    }

    private void debug(Vector2 start, Vector2 end){
        chunk.shapeRenderer.begin();
        chunk.shapeRenderer.setColor(Color.RED);
        chunk.shapeRenderer.line(start, end);
        chunk.shapeRenderer.end();
    }

}
