# [Tank Game](https://github.com/nickolasddiaz/Tank_Game)

A top-down shooter game built with LibGDX using box2d and Java featuring procedurally generated terrain, multiple biomes, and dynamic combat mechanics. Players aim to survive while maximizing their score in an infinite world filled with various enemies, power-ups, and strategic challenges.
[Game Link](https://nickolasddiaz.github.io/Tank_Game/)

<p align="center">
    <img src="https://github.com/user-attachments/assets/1a796f92-9b00-48e9-a382-faf3afe9fafd">



## Features

### World Generation
- Infinite procedurally generated map using cellular noise algorithms
- Five distinct biomes:
    - Ocean
    - Tundra
    - Plains
    - Wild West
    - Desert
- Unique structures and obstacles for each biome
- Road networks and building placement using Wave Function Collapse algorithm inspired by [sea5kg](https://github.com/sea5kg/Roads2DGenerator) and [The Coding Train](https://www.youtube.com/watch?v=rI_y2GAlQFM)

### Gameplay Elements

#### Entities
- Player-controlled tanks
- Cars
- Enemy/Allied units controlled through ai pathfinding

#### Combat System
- Three weapon types:
    - Standard bullets
    - Guided missiles
    - Deployable mines
- Strategic combat with various attack patterns
- Dynamic difficulty scaling

#### Power-up System
Combat Enhancements:
- Increased bullet damage, size, and speed
- Critical hit chances and damage multipliers
- Fire rate improvements

Survival Mechanics:
- Health regeneration
- Enhanced armor
- Speed boosts
- Allied unit spawning

Special Abilities:
- Multi-directional shooting
- Structures destruction

Score Mechanics:
- Point multipliers
- Wanted level system
- High score tracking through a leaderboard

### Controls

#### Desktop
- WASD: Movement
- Mouse Click: Shoot
- P or Space-bar: Pause game

#### Mobile
- Virtual joystick for movement
- Touch controls for shooting
- Auto-fire option available

### Technical Features
- Cross-platform compatibility (Desktop and Web)
- Mobile-optimized controls
- Debug mode for testing
- Leaderboard system

## Credits
- Developed by Nickolas Diaz
- Has no previous game development knowledge
- Code located at https://github.com/nickolasddiaz/Tank_Game/tree/master/core/src/main/java/io/github/nickolasddiaz
- Assets located at https://github.com/nickolasddiaz/Tank_Game/tree/master/other
