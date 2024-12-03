import os
from PIL import Image

# Define tile atlas parameters
TILE_SIZE = 8  # Each tile is 8x8 pixels
TILES_PER_ROW = 13  # 13 tiles per row
TILES_TOTAL = 169  # Total number of tiles

# List of tile names
TILE_NAMES = [
    "DESSERT", "PLAINS", "WILD_WEST", "TUNDRA",
    	"PLAIN_TREE", "PLAIN_SHRUB", "PLAINS_STONE",
    	"DESSERT_SHRUB", "PALM_TREE", "DESSERT_STONE",
    	"TUMBLEWEED", "CACTUS", "DEAD_TREE",
    	"ICICLE", "TUNDRA_TREE", "TUNDRA_ROCK",
    	"PLAINS_BUILDING1", "PLAINS_BUILDING2", "PLAINS_BUILDING3", "PLAINS_BUILDING4", "PLAINS_BUILDING5", "PLAINS_BUILDING6", "PLAINS_BUILDING7", "PLAINS_BUILDING8", "PLAINS_BUILDING9", "PLAINS_BUILDING10", "PLAINS_BUILDING11", "PLAINS_BUILDING12",  
 	"PLAINS_BUILDING1_1", "PLAINS_BUILDING1_2", "PLAINS_BUILDING1_3", "PLAINS_BUILDING1_4", "PLAINS_BUILDING1_5", "PLAINS_BUILDING1_6", "PLAINS_BUILDING1_7", "PLAINS_BUILDING1_8", "PLAINS_BUILDING1_9", "PLAINS_BUILDING1_10", "PLAINS_BUILDING1_11", "PLAINS_BUILDING1_12",  
	"PLAINS_BUILDING2_1", "PLAINS_BUILDING2_2", "PLAINS_BUILDING2_3", "PLAINS_BUILDING2_4", "PLAINS_BUILDING2_5", "PLAINS_BUILDING2_6", "PLAINS_BUILDING2_7", "PLAINS_BUILDING2_8", "PLAINS_BUILDING2_9", "PLAINS_BUILDING2_10", "PLAINS_BUILDING2_11", "PLAINS_BUILDING2_12", 
    	"DESSERT_BUILDING1", "DESSERT_BUILDING2", "DESSERT_BUILDING3", "DESSERT_BUILDING4", "DESSERT_BUILDING5", "DESSERT_BUILDING6", "DESSERT_BUILDING7", "DESSERT_BUILDING8", "DESSERT_BUILDING9", "DESSERT_BUILDING10", "DESSERT_BUILDING11", "DESSERT_BUILDING12",  
 	"DESSERT_BUILDING1_1", "DESSERT_BUILDING1_2", "DESSERT_BUILDING1_3", "DESSERT_BUILDING1_4", "DESSERT_BUILDING1_5", "DESSERT_BUILDING1_6", "DESSERT_BUILDING1_7", "DESSERT_BUILDING1_8", "DESSERT_BUILDING1_9", "DESSERT_BUILDING1_10", "DESSERT_BUILDING1_11", "DESSERT_BUILDING1_12",  
	"DESSERT_BUILDING2_1", "DESSERT_BUILDING2_2", "DESSERT_BUILDING2_3", "DESSERT_BUILDING2_4", "DESSERT_BUILDING2_5", "DESSERT_BUILDING2_6", "DESSERT_BUILDING2_7", "DESSERT_BUILDING2_8", "DESSERT_BUILDING2_9", "DESSERT_BUILDING2_10", "DESSERT_BUILDING2_11", "DESSERT_BUILDING2_12",
    	"WILD_WEST_BUILDING1", "WILD_WEST_BUILDING2", "WILD_WEST_BUILDING3", "WILD_WEST_BUILDING4", "WILD_WEST_BUILDING5", "WILD_WEST_BUILDING6", "WILD_WEST_BUILDING7", "WILD_WEST_BUILDING8", "WILD_WEST_BUILDING9", "WILD_WEST_BUILDING10", "WILD_WEST_BUILDING11", "WILD_WEST_BUILDING12",  
 	"WILD_WEST_BUILDING1_1", "WILD_WEST_BUILDING1_2", "WILD_WEST_BUILDING1_3", "WILD_WEST_BUILDING1_4", "WILD_WEST_BUILDING1_5", "WILD_WEST_BUILDING1_6", "WILD_WEST_BUILDING1_7", "WILD_WEST_BUILDING1_8", "WILD_WEST_BUILDING1_9", "WILD_WEST_BUILDING1_10", "WILD_WEST_BUILDING1_11", "WILD_WEST_BUILDING1_12",  
	"WILD_WEST_BUILDING2_1", "WILD_WEST_BUILDING2_2", "WILD_WEST_BUILDING2_3", "WILD_WEST_BUILDING2_4", "WILD_WEST_BUILDING2_5", "WILD_WEST_BUILDING2_6", "WILD_WEST_BUILDING2_7", "WILD_WEST_BUILDING2_8", "WILD_WEST_BUILDING2_9", "WILD_WEST_BUILDING2_10", "WILD_WEST_BUILDING2_11", "WILD_WEST_BUILDING2_12",
    	"TUNDRA_BUILDING1", "TUNDRA_BUILDING2", "TUNDRA_BUILDING3", "TUNDRA_BUILDING4", "TUNDRA_BUILDING5", "TUNDRA_BUILDING6", "TUNDRA_BUILDING7", "TUNDRA_BUILDING8", "TUNDRA_BUILDING9", "TUNDRA_BUILDING10", "TUNDRA_BUILDING11", "TUNDRA_BUILDING12",  
 	"TUNDRA_BUILDING1_1", "TUNDRA_BUILDING1_2", "TUNDRA_BUILDING1_3", "TUNDRA_BUILDING1_4", "TUNDRA_BUILDING1_5", "TUNDRA_BUILDING1_6", "TUNDRA_BUILDING1_7", "TUNDRA_BUILDING1_8", "TUNDRA_BUILDING1_9", "TUNDRA_BUILDING1_10", "TUNDRA_BUILDING1_11", "TUNDRA_BUILDING1_12",  
	"TUNDRA_BUILDING2_1", "TUNDRA_BUILDING2_2", "TUNDRA_BUILDING2_3", "TUNDRA_BUILDING2_4", "TUNDRA_BUILDING2_5", "TUNDRA_BUILDING2_6", "TUNDRA_BUILDING2_7", "TUNDRA_BUILDING2_8", "TUNDRA_BUILDING2_9", "TUNDRA_BUILDING2_10", "TUNDRA_BUILDING2_11", "TUNDRA_BUILDING2_12",
    	"OCEAN",
    	"ROAD_LEFT","ROAD_RIGHT","ROAD_TOP","ROAD_BOTTOM","ROAD_TOP_LEFT","ROAD_TOP_RIGHT","ROAD_BOTTOM_LEFT","ROAD_BOTTOM_RIGHT"
]

if len(TILE_NAMES) != TILES_TOTAL:
    raise ValueError(f"Expected {TILES_TOTAL} names, but got {len(TILE_NAMES)}.")

# Load the tile atlas
atlas_path = "C:\\Users\\nicko\\Desktop\\Newfolder\\tank_game.png"
atlas_image = Image.open(atlas_path)

# Validate dimensions
width, height = atlas_image.size
expected_width = TILES_PER_ROW * TILE_SIZE
expected_height = TILES_TOTAL // TILES_PER_ROW * TILE_SIZE

if width < expected_width or height < expected_height:
    raise ValueError("Atlas image dimensions are smaller than expected.")


output_dir = r"C:\Users\nicko\Desktop\Newfolder\output"
os.makedirs(output_dir, exist_ok=True)

# Save the tiles
for i, name in enumerate(TILE_NAMES):
    row = i // TILES_PER_ROW
    col = i % TILES_PER_ROW
    left = col * TILE_SIZE
    upper = row * TILE_SIZE
    right = left + TILE_SIZE
    lower = upper + TILE_SIZE

    # Crop the tile
    tile = atlas_image.crop((left, upper, right, lower))

    # Save the tile with its name
    tile.save(os.path.join(output_dir, f"{name}.png"), "PNG")

print(f"All {TILES_TOTAL} tiles have been saved successfully.")
