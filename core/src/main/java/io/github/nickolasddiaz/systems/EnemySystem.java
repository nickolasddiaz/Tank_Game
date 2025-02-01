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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.nickolasddiaz.components.*;
import io.github.nickolasddiaz.utils.GraphNode;

import static io.github.nickolasddiaz.utils.CollisionCategory.*;
import static io.github.nickolasddiaz.utils.MapGenerator.*;

public class EnemySystem extends IteratingSystem {
    private final ComponentMapper<EnemyComponent> enemyMapper;
    private final ComponentMapper<TransformComponent> transformMapper;
    private final ChunkComponent chunk;
    private final TransformComponent player;
    private final SettingsComponent settings;
    private final Engine engine;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BulletFactory bulletFactory;

    public EnemySystem(Engine engine, TransformComponent player, CameraComponent camera,
                       SettingsComponent settings, BulletFactory bulletFactory, ChunkComponent chunk) {
        super(Family.all(EnemyComponent.class, TransformComponent.class).get());
        this.enemyMapper = ComponentMapper.getFor(EnemyComponent.class);
        this.transformMapper = ComponentMapper.getFor(TransformComponent.class);
        this.player = player;
        this.engine = engine;
        this.settings = settings;
        this.bulletFactory = bulletFactory;
        this.chunk = chunk;
        shapeRenderer.setProjectionMatrix(camera.camera.combined);
        shapeRenderer.setColor(Color.GREEN);

        setupCollisionListener();
    }

    private void setupCollisionListener() {
        chunk.world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                handleCollision(contact);
            }

            @Override
            public void endContact(Contact contact) {}

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void handleCollision(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();

        TransformComponent transformA = (TransformComponent) bodyA.getUserData();
        TransformComponent transformB = (TransformComponent) bodyB.getUserData();

        if (transformA == null || transformB == null) return;

        short categoryA = fixtureA.getFilterData().categoryBits;
        short categoryB = fixtureB.getFilterData().categoryBits;

        // Handle different collision scenarios
        if (categoryA == ENEMY || categoryB == ENEMY ||
            categoryA == ALLY || categoryB == ALLY) {

            TransformComponent enemy = (categoryA == ENEMY || categoryA == ALLY) ?
                transformA : transformB;
            TransformComponent other = (categoryA == ENEMY || categoryA == ALLY) ?
                transformB : transformA;
            short otherCategory = (categoryA == ENEMY || categoryA == ALLY) ?
                categoryB : categoryA;

            handleEnemyCollision(enemy, other, otherCategory);
        }
    }

    private void handleEnemyCollision(TransformComponent enemy, TransformComponent other, short category) {
        switch (category) {
            case CAR:
                other.health = 0;
                enemy.body.setLinearDamping(2f); // Slow down after hitting car
                break;

            case HORIZONTAL_ROAD:
            case VERTICAL_ROAD:
                enemy.body.setLinearDamping(0f); // Remove damping on roads
                break;

            case DECORATION:
                enemy.body.setLinearDamping(1.5f); // Add more damping in decoration
                break;
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyComponent enemyComponent = enemyMapper.get(entity);
        TransformComponent transform = transformMapper.get(entity);

        transform.velocity.setZero();

        // Check if enemy is in valid chunk and alive
        Vector2 chunkPosition = chunk.getChunkPosition(transform.getPosition());
        if (!chunk.mapChunks.containsKey(chunkPosition) || transform.health <= 0) {
            transform.dispose();
            engine.removeEntity(entity);
            return;
        }

        // Handle turret rotation
        updateTurretRotation(transform, enemyComponent);

        // Handle shooting
        handleShooting(transform, enemyComponent, deltaTime);

        // Check distance to player
        float distanceToPlayer = transform.getPosition().dst(player.getPosition());
        if (distanceToPlayer <= enemyComponent.minDistance) {
            return;
        }

        // Update pathfinding
        updatePathfinding(transform, enemyComponent, deltaTime);

        // Move enemy
        if (enemyComponent.path.getCount() > 1 || enemyComponent.pathIndex < enemyComponent.path.getCount() - 1) {
            moveEnemy(transform, enemyComponent, deltaTime);

            if (settings.DEBUG) {
                renderNextPathRect(transform.getPosition(), enemyComponent.nextPathWorld, enemyComponent);
            }
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
                    //Gdx.app.log("EnemySystem", "Using previous path as fallback");
                    enemyComponent.path = enemyComponent.previousPath;
                } else {
                    // Create a simple direct path to the player if no previous path exists
                    DefaultGraphPath<GraphNode> directPath = new DefaultGraphPath<>();
                    GraphNode simpleStartNode = new GraphNode(startPos);
                    GraphNode simpleEndNode = new GraphNode(endPos);
                    directPath.add(simpleStartNode);
                    directPath.add(simpleEndNode);
                    enemyComponent.path = directPath;
                    //Gdx.app.log("EnemySystem", "Created direct path to player as fallback");
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
                    //Gdx.app.log("EnemySystem", "Path found with " + enemyComponent.path.getCount() + " nodes");
                } else {
                    //Gdx.app.log("EnemySystem", "No path found, using previous path");
                    if (enemyComponent.previousPath != null) {
                        enemyComponent.path = enemyComponent.previousPath;
                    }
                }
            }
        }


    }

    private void updateTurretRotation(TransformComponent transform, EnemyComponent enemyComponent) {
        if (enemyComponent.isAlly) {
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
    private void handleShooting(TransformComponent transform, EnemyComponent enemyComponent, float deltaTime) {
        enemyComponent.timeSinceLastShot += deltaTime;
        if (enemyComponent.timeSinceLastShot > enemyComponent.fireRate) {
            enemyComponent.timeSinceLastShot = 0f;
            if (player.getPosition().dst(transform.getPosition()) < chunkSize) {
                Vector2 bulletPosition = transform.getPosition().cpy();
                bulletFactory.createBullet(
                    bulletPosition,
                    transform.turretRotation + (chunk.random.nextFloat() - 0.5f) * 10f,
                    enemyComponent.bulletSpeed,
                    enemyComponent.bulletDamage,
                    1.5f,
                    enemyComponent.isAlly ? Color.YELLOW : Color.RED,
                    enemyComponent.isAlly
                );
            }
        }
    }

    private void moveEnemy(TransformComponent transform, EnemyComponent enemyComponent, float deltaTime) {
        Vector2 direction = new Vector2(enemyComponent.nextPathWorld).sub(transform.getPosition()).nor();

        // Calculate target angle
        float targetAngle = direction.angleDeg();
        float angleDifference = ((targetAngle - transform.rotation + 540) % 360) - 180;

        // Apply rotation
        if (Math.abs(angleDifference) > 5) {
            float turnSpeed = Math.min(enemyComponent.spinSpeed * deltaTime, Math.abs(angleDifference) * 0.5f);
            transform.rotation += Math.signum(angleDifference) * turnSpeed;
        } else {
            transform.rotation = targetAngle;
        }
        transform.rotation = (transform.rotation + 360) % 360;

        // Apply movement velocity
        transform.velocity = new Vector2(
            (float) Math.cos(transform.rotation) * enemyComponent.speed,
            (float) Math.sin(transform.rotation) * enemyComponent.speed
        );
    }


    private void renderNextPathRect(Vector2 enemy, Vector2 path, EnemyComponent enemyComponent) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.rect(
            enemyComponent.nextPathRect.x,
            enemyComponent.nextPathRect.y,
            enemyComponent.nextPathRect.width,
            enemyComponent.nextPathRect.height
        );
        shapeRenderer.line(enemy, path);
        shapeRenderer.end();
    }

    public static class ManhattanDistance implements Heuristic<GraphNode> {
        @Override
        public float estimate(GraphNode node, GraphNode endNode) {
            return Math.abs(node.position.x - endNode.position.x) +
                Math.abs(node.position.y - endNode.position.y);
        }
    }

}
