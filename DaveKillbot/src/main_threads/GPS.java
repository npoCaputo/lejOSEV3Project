package main_threads;

import java.util.BitSet;

import lejos.hardware.lcd.GraphicsLCD;


/**
 * 
 * @author CGreen
 *
 */
public class GPS {
	// orientation is: 0=N, 1=E, 2=S, 3=W
	private int x, y, orientation, numRows, numColumns;
	private BitSet[][] wallData; // [N, E, S, W, goneN, goneE, goneS, goneW, visited]
	private MazeRevealMap mazeMap;
	
	/**
	 * Constructs a GPS object, which keeps track of the robot's position in the maze,
	 * its orientation (the direction in which its body faces), and the size of the maze.
	 * Also manages the map that draws the maze on the robot's LCD screen.
	 * 
	 * Orientation follows the code: 0=North, 1=East, 2=South, 3=West. Direction is absolute, 
	 * relative to the robot's starting position. The initial orientation of the robot is always
	 * north. Regardless of orientation afterward, north remains north, and 0 will always denote
	 * north; likewise for the rest of the cardinal directions.
	 * 
	 * 'Direction' is often different from 'orientation.' In general, the variable 'direction' refers to one of the cardinal directions, and does
	 * not necessarily reflect the orientation of the robot's body. The robot's head may be turned
	 * in the direction of 'direction', or 'direction' may refer to a side of a cell.
	 * 
	 * @param screen - the leJOS robot's lcd screen
	 * @param startX - starting x position
	 * @param startY - starting y position 
	 * @param startOrientation - starting orientation (0=N, 1=E, 2=S, 3=W)
	 * @param numRows - number of rows of cells in the maze
	 * @param numColumns - number of columns of cells in the maze
	 */
	public GPS(GraphicsLCD screen, int startX, int startY, int startOrientation, int numRows, int numColumns) {
		x = startX;
		y = startY;
		orientation = startOrientation % 4;
		
		while (orientation < 0) {
			orientation+=4;
		}
		
		this.numRows = numRows;
		this.numColumns = numColumns;
		wallData = new BitSet[numRows][numColumns];
		mazeMap = new MazeRevealMap(screen, numRows, numColumns);
		
		for (int i = 0; i < wallData.length; i++) {
			
			for (int j = 0; j < wallData[i].length; j++) {
				wallData[i][j] = new BitSet(9);
				// set walls on the maze boundaries
				if (i == 0) {
					wallData[i][j].set(3);
				}
				
				if (j == 0) {
					wallData[i][j].set(2);
				}
				
				if (i == numColumns-1) {
					wallData[i][j].set(1);
				}
				
				if (j == numRows-1) {
					wallData[i][j].set(0);
				}
				
			}
			
		}
		
	}

	/**
	 * Updates the GPS with the robot's new orientation.
	 * 
	 * @param newOrientation - the new orientation (0=N, 1=E, 2=S, 3=W)
	 * @return the new orientation
	 */
	public int updateOrientation(int newOrientation) {
		orientation = newOrientation % 4;
		while (orientation < 0) {
			orientation+=4;
		}
		return orientation;
	}
	
	/**
	 * Updates the GPS with new coordinates based on how many cells
	 * were traversed by the robot in the forwards direction in a straight
	 * line.
	 * 
	 * The current position should be updated after each time the robot
	 * moves successfully from one cell to another.
	 * 
	 * @param step - the number of cells traversed. Negative values
	 * denote movement in the reverse direction.
	 * 
	 * @return the new coordinates
	 */
	public int[] updatePosition(int step) {
		switch (orientation) {
		case 0: // robot moves north
			y += step;
			break;
		case 1: // robot moves east
			x += step;
			break;
		case 2: // robot moves south
			y -= step;
			break;
		case 3: // robot moves west
			x -= step;
			break;
		}
		return getCoordinates();
	}
	
	/**
	 * Resets the GPS, blanking out walls on the map and placing the robot in a
	 * new starting position.
	 * 
	 * @param startX - the new starting position x value
	 * @param startY - the new starting position y value
	 * @param orientation - the robot's new starting orientation
	 */
	public void resetGPS(int startX, int startY, int orientation) {
		mazeMap.reset();
		x = startX;
		y = startY;
		this.orientation = orientation;
		for (int i = 0; i < wallData.length; i++) {
			for (int j = 0; j < wallData[i].length; j++) {
				wallData[i][j].clear();
			}
		}
	}
	
	/**
	 * Readies the GPS map display by drawing a blank map.
	 */
	public void prepGPS() {
		mazeMap.reset();
	}
	
	/**
	 * Set whether the cell has been visited already. This should be
	 * done immediately before the robot leaves the cell.
	 * 
	 * @param x - x coordinate of the current cell
	 * @param y - y coordinate of the current cell
	 */
	public void setVisited(int x, int y) {
		wallData[x][y].set(8);
	}
	
	/**
	 * Keep track of which direction was taken to/from a cell.
	 * 
	 * @param x - x coordinate of the current cell
	 * @param y - y coordinate of the current cell
	 * @param direction - cardinal direction
	 */
	public void setDirectionTaken(int x, int y, int direction) {
		if (direction <= 3 && direction >= 0) {
			wallData[x][y].set(direction+4);
		}
	}
	
	/**
	 * Updates the map data with the wall layout of the current cell.
	 * 
	 * @param x
	 * @param y
	 * @param walls
	 * @return - success or failure
	 */
	public boolean setWalls(int x, int y, BitSet walls) {
		if (!wallData[x][y].get(8)) {
			for (int index = 0; index < 4; index++) {
				if (walls.get(index)) {
					wallData[x][y].set(index);
					mazeMap.drawWall(x, y, index);
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Check whether this cell has been visited.
	 * 
	 * @param x - x coordinate of the current cell
	 * @param y - y coordinate of the current cell
	 * @return
	 */
	public boolean getVisited(int x, int y) {
		return wallData[x][y].get(8);
	}
	
	/**
	 * Returns whether a neighboring cell in view has been visited.
	 * 
	 * @param direction - cardinal direction in which the neighbor cell is located
	 * @return - whether the neighboring cell has been visited
	 */
	public boolean getVisitedNeighbor(int direction) {
		int neighborX = x;
		int neighborY = y;
		switch (direction) {
		case 0:
			neighborY+=1;
			break;
		case 1:
			neighborX+=1;
			break;
		case 2:
			neighborY-=1;
			break;
		case 3:
			neighborX-=1;
			break;
		}
		if (neighborX < 0 || neighborY < 0 || neighborX >= numColumns || neighborY >= numRows) {
			throw new IllegalArgumentException("Attempted to look out-of-bounds standing at (" + x + ", " + y + ") looking at (" + neighborX + ", " + neighborY + ") facing " + direction);
		}
		return wallData[neighborX][neighborY].get(8);
	}
	/**
	 * Check what paths have been taken to or from a particular cell.
	 * 
	 * @param x - x coordinate of the cell
	 * @param y - y coordinate of the cell
	 * @return - BitSet containing the boolean value for each direction
	 */
	public BitSet getDirectionsTaken(int x, int y) {
		return wallData[x][y].get(4, 8);
	}
	
	/**
	 * Check the layout of a cell's walls.
	 * 
	 * @param x - y coordinate of the cell
	 * @param y - y coordinate of the cell
	 * @return - BitSet containing booleans denoting the presence of a wall
	 */
	public BitSet getWalls(int x, int y) {
		return wallData[x][y].get(0, 4);
	}
	
	/**
	 * Get the current coordinates.
	 * 
	 * @return an int array containing the current coordinates.
	 */
	public int[] getCoordinates() {
		int[] coords = new int[2];
		coords[0] = x;
		coords[1] = y;
		return coords;
	}
	
	/**
	 * Get the current direction the robot's body is facing.
	 * 
	 * @return - the current orientation of the robot
	 */
	public int getOrientation() {
		return orientation;
	}
	
	/**
	 * Get the x-value of the current coordinates.
	 * 
	 * @return
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * Get the y-value of the current coordinates.
	 * 
	 * @return
	 */
	public int getY() {
		return y;
	}
	
}
