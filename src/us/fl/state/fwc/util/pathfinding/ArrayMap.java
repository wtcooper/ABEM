package us.fl.state.fwc.util.pathfinding;


/**
 * The data map from our example game. This holds the state and context of each tile
 * on the map. It also implements the interface required by the path finder. It's implementation
 * of the path finder related methods add specific handling for the types of units
 * and terrain in the example game.
 * 
 * @author Kevin Glass
 */
public class ArrayMap implements TileBasedMap {
	/** The map width in tiles */
	public int WIDTH; // = 10;
	/** The map height in tiles */
	public int HEIGHT; // = 10;
	
	/** The terrain settings for each tile in the map */
	private int[][] terrain; 
	
	/** Indicator if a given tile has been visited during the search */
	private boolean[][] visited; // = new boolean[HEIGHT][WIDTH];
	
	/**
	 * Create a new test map with some default configuration
	 */
	public ArrayMap() {

		//here I would set the terrain map
	}

	
	/**
	 * Clear the array marking which tiles have been visted by the path 
	 * finder.
	 */
	public void clearVisited() {
		for (int x=0;x<getWidthInTiles();x++) {
			for (int y=0;y<getHeightInTiles();y++) {
				visited[y][x] = false;
			}
		}
	}
	
	
	public boolean blocked(Mover mover, int x, int y) {
		// if theres a unit at the location, then it's blocked
		if (terrain[y][x] == 0) return true;
		else return false;
	}
	
	
	
	/**
	 * @see TileBasedMap#visited(int, int)
	 */
	public boolean visited(int x, int y) {
		return visited[y][x];
	}
	
	/**
	 * Get the terrain at a given location
	 * 
	 * @param x The x coordinate of the terrain tile to retrieve
	 * @param y The y coordinate of the terrain tile to retrieve
	 * @return The terrain tile at the given location
	 */
	public int getTerrain(int x, int y) {
		return terrain[y][x];
	}
	
	public void setTerrain(int[][] terrain){
		this.terrain = terrain;
		this.HEIGHT = terrain.length;
		this.WIDTH = terrain[0].length;
		
		visited = new boolean[terrain.length][terrain[0].length];
		
	}

	/**
	 * @see TileBasedMap#getCost(Mover, int, int, int, int)
	 */
	public float getCost(Mover mover, int sx, int sy, int tx, int ty) {
		return 1;
	}

	/**
	 * @see TileBasedMap#getHeightInTiles()
	 */
	public int getHeightInTiles() {
		return HEIGHT;
	}

	/**
	 * @see TileBasedMap#getWidthInTiles()
	 */
	public int getWidthInTiles() {
		return WIDTH;
	}

	/**
	 * @see TileBasedMap#pathFinderVisited(int, int)
	 */
	public void pathFinderVisited(int x, int y) {
		visited[y][x] = true;
	}
	
	
}
