package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import ucar.ma2.Array;
import us.fl.state.fwc.abem.dispersal.bolts.Barrier;
import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.NetCDFFile;

/**Performs a reflection off of a raster netCDF land layer
 * Note: the raster must be uniform and not rotated (e.g., based on NCOM, HYCOM models)
 * 
 * Performs best when land grid is same as model's active cells, so that the particles follow
 * the modeled flows.  For example, if use the shapefile barrier class, the particles have the potential
 * to get 'stuck' behind some small land jetties (i.e., smaller than hydromodel's cell resolution) that are 
 * considered water in the hydromodel.  
 *  
 * @author Wade.Cooper
 *
 */
public class GridBarrier implements Barrier {

	protected short landVal = 1; //default is 1 
	protected boolean neglon;
	protected String fileName;
	protected NetCDFFile ncFile; 
	protected final int landSearchRadius = 2; //will search 2 cell boundary to see if land
	private double[] lats,lons;
	private double gridXSize = 0.004, gridYSize = 0.004; 
	short[][] landGrid; 
	
	ArrayList<Integer> landMaskListXs; //a set that holds just the x index values for faster check
	HashMap<Int3D, Integer> landMaskList; 
	
	/**Sets the data -- reads in a netCDF land mask, and stores the landMask variable
	 * and lats/lons as Java arrays for fast access
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void setDataSource(String filename) throws IOException {

		this.fileName = filename;
		try {
			ncFile = new NetCDFFile(filename);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		ncFile.setVariables("lat", "lon", "landMask"); 
		
		landGrid = new short[ncFile.getSingleDimension("lat")][ncFile.getSingleDimension("lon")];
		
		for (int i=0; i<ncFile.getSingleDimension("lat"); i++ ){
			for (int j=0; j<ncFile.getSingleDimension("lon"); j++ ){
				Number val = ncFile.getValue("landMask", new int[]{i,j});
				landGrid[i][j] = ((Short) val).shortValue(); 
			}
		}
		
		//store the lats and lons as permanent for speed lookup
		lats = toJArray(ncFile.getVariable("lat").read());
		lons = toJArray(ncFile.getVariable("lon").read());
	}

	
	/**Copies netCDF arrays to java arrays
	 * 
	 * @param arr
	 * @return
	 */
	private double[] toJArray(Array arr){
		double[] ja;
		if (arr.getElementType() == Float.TYPE) {

			float[] fa = (float[]) arr.copyTo1DJavaArray();
			ja = new double[fa.length];
			for (int i = 0; i < ja.length; i++) {
				ja[i] = fa[i];
			}
		return ja;
		} else {

			return (double[]) arr.copyTo1DJavaArray();
		}
	}
	
	
	/**Checks if a particle should bounce off of land, and if so, it bounces
	 * and sets a new particle location
	 * 
	 */
	@Override
	public void checkReflect(Particle p) {

		
		double px = p.getPX();
		double py = p.getPY();
		double x = p.getX();
		double y = p.getY();

		if (neglon) {
			px = cvt2Neg(px);
			x = cvt2Neg(x);
		}
		
		//endPoint index
		int xIndex = locate(lons, x);
		int yIndex  = locate(lats, y);

		//startPointIndex -- NOTE: this is same as gridID.x & gridID.y in GPU code
		//Int3D gridID = new Int3D(0,0,0); 
		int pxIndex = locate(lons, px);
		int pyIndex = locate(lats, py);

		// if the end point is in the same grid cell as the start point, then don't go through this set of steps
		//if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
		if ( (pxIndex == xIndex )  && (pyIndex == yIndex) ) return; 
		
		
		//checks to see if particle is near land -- if it is, will have to check every box it passes through to make sure it doesn't 'jump over' a sliver of land during movement
		boolean nearLand = false; 
		for (int i=pyIndex-landSearchRadius; i<pyIndex+landSearchRadius; i++ ){
			boolean flagBreak = false; 
			for (int j=pxIndex-landSearchRadius; j<pxIndex+landSearchRadius; j++ ){
				if ( (i>=0) && (j>=0) && (i<landGrid.length) && (j<landGrid[0].length) 
						&& ( (landGrid[i][j] == landVal) || landMaskListXs.contains(new Integer(j)) ) ) {
					nearLand = true;  
					flagBreak = true;
					break;
				}
			}
			if (flagBreak) break; 
		}
		
		
		//while the particle's end point is either on land, or if the particle is in the vicinity of land, then loop through
		
		
		
		
		int counter = 0; 
		
		while ( (landGrid[yIndex][xIndex] == landVal)  || nearLand ) { //|| checkPassThruMask(xIndex, yIndex, pxIndex, pyIndex)){
			double vecSlope = (y-py)/(x-px); //endPoint.y-startPoint.y)/(endPoint.x-startPoint.x);
			double vecIntercept = py-(vecSlope*px); //(startPoint.y)-(vecSlope*startPoint.x);

			//TODO -- need to catch error where u-velocity = 0, leading to vecSlope = infinity

			int moveType = 0;  //0=still in water and haven't moved; 1=still in water but have moved; 2=have moved and on land

			//step through cells along path until find one that is land, bounces off a land mask, 
			//or until reach the final endpoint cell without encountering land or land mask
			while (moveType == 0){

				//========================================
				//check N boundary 
				//if ( ((endPoint.y-startPoint.y) > 0) && ((( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize+gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize+gridXSize)) + vecIntercept) - (gridID.y*gridYSize + gridYSize) ) ) < 0) ) {
				if ( ( ((y-py) > 0) && 
						((( (vecSlope*((lons[pxIndex]-gridXSize/2)) + vecIntercept) - (lats[pyIndex]+gridYSize/2)  ) * 
								( (vecSlope*((lons[pxIndex]+gridXSize/2)) + vecIntercept) - (lats[pyIndex]+gridYSize/2) ) ) < 0) )
								//catch the situation where u-vel == 0, so x-px == 0, leading to vecSlope == infinity
								|| ( (y-py) > 0) && (x-px) == 0) {

					pyIndex++; //gridID.y = gridID.y + 1;
					//gridID.x = gridID.x;

					//if have moved out of grid, then set p = lost, and return
					if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
						p.setLost(true);
						return;
					}

					if (landGrid[pyIndex][pxIndex] == landVal || checkPassThruMask(pxIndex, pyIndex, pxIndex, pyIndex-1)/*TODO -- check if the boundary is a landmask*/ ){

						//simple reflection calculation for uniform nonrotated grid -- trust the math!  
						y = 2*(lats[pyIndex]-gridYSize/2)-y; //endPoint.y = 2*(gridID.y*gridYSize)-endPoint.y; 
						
						//reset startPoint to the point of intercept where particle hits land
						py = lats[pyIndex]-gridYSize/2; //startPoint.y = (gridID.y*gridYSize);
						px = (((lats[pyIndex]-gridYSize/2)-vecIntercept)/vecSlope); //startPoint.x = (((gridID.y*gridYSize) - vecIntercept)/vecSlope); 

						//note: bring back the pyIndex, so that when checks the checkPassThruMask() in main while() loop,
						//will accurately reflect that the starting point is the point of intercept, but in the cell where the particle came from
						//before reflecting
						pyIndex--; 
						
						//if have moved out of grid, then set p = lost, and return
						if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
							p.setLost(true);
							return;
						}

						moveType = 2;
					}
					
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					//else if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
					else if ( (pxIndex == xIndex)  && (pyIndex == yIndex) ) {
						moveType=2;
						nearLand=false; 
					}
					
					else {
						moveType = 1; 
					}
				}


				//========================================
				//check E boundary

				//if ( (moveType < 1) && ((endPoint.x-startPoint.x) > 0) &&  (( (vecSlope*((gridID.x*gridXSize+gridXSize)) + vecIntercept) - (gridID.y*gridYSize + gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize + gridXSize)) + vecIntercept) - (gridID.y*gridYSize) ) ) < 0 ) {
				if ( (moveType < 1) && 
						( ((x-px) > 0) &&  
						(( (vecSlope*((lons[pxIndex]+gridXSize/2)) + vecIntercept) - (lats[pyIndex] + gridYSize/2)  ) * 
								( (vecSlope*((lons[pxIndex]+gridXSize/2)) + vecIntercept) - (lats[pyIndex] - gridYSize/2) ) ) < 0 ) 
								|| ((x-px) > 0) && ((y-py) == 0)) {

					pxIndex++; //gridID.x = gridID.x + 1;
					//gridID.y = gridID.y; 					
					
					//if have moved out of grid, then set p = lost, and return
					if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
						p.setLost(true);
						return;
					}

					
					

					if (landGrid[pyIndex][pxIndex] == landVal || checkPassThruMask(pxIndex, pyIndex, pxIndex-1, pyIndex)/*TODO -- check if the boundary is a landmask*/){

						//simple calculation for uniform nonrotated grid 
						x = 2*(lons[pxIndex]-gridXSize/2)-x; //endPoint.x = 2*(gridID.x*gridXSize)-endPoint.x;  
						px = lons[pxIndex]-gridXSize/2; //startPoint.x = (gridID.x*gridXSize);  
						
						//TODO -- I had this as y = vecSlope....  WHY did I do this, or was it a typo??
						py = vecSlope*(lons[pxIndex]-gridXSize/2)+vecIntercept; //startPoint.y = (vecSlope*((gridID.x*gridXSize)) + vecIntercept);

						//note: bring back the p-Index, so that when checks the checkPassThruMask() in main while() loop,
						//will accurately reflect that the starting point is the point of intercept, but in the cell where the particle came from
						//before reflecting
						pxIndex--; 
						//if have moved out of grid, then set p = lost, and return
						if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
							p.setLost(true);
							return;
						}


						moveType = 2; 

					}
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					else if ( (pxIndex == xIndex)  && (pyIndex == yIndex) ) {
						moveType=2;
						nearLand=false; 
					}
					
					else {
						moveType = 1; 
					}
				}


				//========================================
				//check S boundary
				//if ( (moveType < 1) && ((endPoint.y-startPoint.y) < 0) &&  (( (vecSlope*((gridID.x*gridXSize + gridXSize)) + vecIntercept) - (gridID.y*gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize) ) ) < 0 ) {
				if ( (moveType < 1) && 
						( ((y-py) < 0) &&  
						(( (vecSlope*((lons[pxIndex] + gridXSize/2)) + vecIntercept) - (lats[pyIndex] - gridYSize/2) ) * 
								( (vecSlope*((lons[pxIndex] - gridXSize/2)) + vecIntercept) - (lats[pyIndex] - gridYSize/2) ) ) < 0 )
								|| ( (y-py) < 0) && (x-px) == 0) {

					pyIndex--; //gridID.y = gridID.y - 1;
					//gridID.x = gridID.x; 
					
					//if have moved out of grid, then set p = lost, and return
					if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
						p.setLost(true);
						return;
					}


					if (landGrid[pyIndex][pxIndex] == landVal || checkPassThruMask(pxIndex, pyIndex, pxIndex, pyIndex+1)/*TODO -- check if the boundary is a landmask*/){

						//simple calculation for uniform nonrotated grid 
						y = 2*(lats[pyIndex]+gridYSize/2)-y; //endPoint.y = 2*(gridID.y*gridYSize+gridYSize)-endPoint.y;  
						py = lats[pyIndex]+gridYSize/2; //startPoint.y = (gridID.y*gridYSize + gridYSize); 
						px = (((lats[pyIndex]+gridYSize/2)-vecIntercept)/vecSlope); //startPoint.x = (((gridID.y*gridYSize + gridYSize) - vecIntercept)/vecSlope); 

						//note: bring back the p-Index, so that when checks the checkPassThruMask() in main while() loop,
						//will accurately reflect that the starting point is the point of intercept, but in the cell where the particle came from
						//before reflecting
						pyIndex++; 
						
						//if have moved out of grid, then set p = lost, and return
						if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
							p.setLost(true);
							return;
						}


						moveType = 2; 

					}
					
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					//else if ( (gridID.x == (int) (endPoint.x/(double) gridXSize))  && (gridID.y == (int) (endPoint.y/(double) gridYSize)) ) {
					else if ( (pxIndex == xIndex)  && (pyIndex == yIndex) ) {
						moveType=2;
						nearLand=false; 
					}
					
					else {
						moveType = 1; 
					}
				}


				//========================================
				//check W boundary
				//if ( (moveType < 1) && ((endPoint.x-startPoint.x) < 0) &&  (( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize)  ) * ( (vecSlope*((gridID.x*gridXSize)) + vecIntercept) - (gridID.y*gridYSize + gridYSize) ) ) < 0 ) {
				if ( (moveType < 1) && 
						( ((x-px) < 0) &&  
						(( (vecSlope*((lons[pxIndex]-gridXSize/2)) + vecIntercept) - (lats[pyIndex]-gridYSize/2)  ) * 
								( (vecSlope*((lons[pxIndex]-gridXSize/2)) + vecIntercept) - (lats[pyIndex]+gridYSize/2) ) ) < 0 )
								|| ((x-px) < 0) && ((y-py) == 0)) {

					pxIndex--; //gridID.x = gridID.x - 1;
					//gridID.y = gridID.y; 
					
					//if have moved out of grid, then set p = lost, and return
					if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
						p.setLost(true);
						return;
					}


					if (landGrid[pyIndex][pxIndex] == landVal || checkPassThruMask(pxIndex, pyIndex, pxIndex+1, pyIndex)/*TODO -- check if the boundary is a landmask*/){

						//simple calculation for uniform nonrotated grid 
						x = 2*(lons[pxIndex]+gridXSize/2)-x; //endPoint.x = 2*(gridID.x*gridXSize+gridXSize)-endPoint.x;  
						px = lons[pxIndex]+gridXSize/2; //startPoint.x = (gridID.x*gridXSize + gridXSize);  
						py = vecSlope*((lons[pxIndex]+gridXSize/2))+vecIntercept; //startPoint.y = (vecSlope*((gridID.x*gridXSize + gridXSize)) + vecIntercept); //HERE'S WHERE IS MESSED UP

						//note: bring back the p-Index, so that when checks the checkPassThruMask() in main while() loop,
						//will accurately reflect that the starting point is the point of intercept, but in the cell where the particle came from
						//before reflecting
						pxIndex++; 
						
						//if have moved out of grid, then set p = lost, and return
						if (pyIndex > lats.length-1 || pyIndex < 0 || pxIndex > lons.length-1 || pxIndex < 0) {
							p.setLost(true);
							return;
						}

						moveType = 2; 

					}
					// if we've reached the final grid cell where the endPoint is, and this cell isn't on land, then kick out of loop
					else if ( (pxIndex == xIndex)  && (pyIndex == yIndex) ) {
						moveType=2;
						nearLand=false; 
					}
				}

				if (moveType ==1 ) moveType = 0; //reset to 0 so will continue loop, but if it's set to 2 (has encountered land or land mask), then will break out

			} // end while (moveType == 0)

			//reset the x and y index here after it breaks free of while loop to the new reflected location
			// note: leave pxIndex and pyIndex as is, because it is set to the point of intersection on the side where particle came from
			xIndex = locate(lons, x);
			yIndex  = locate(lats, y);
			
			//check if the new reflected location indices are equal, and if not, then set nearLand = true 
			//so will continue following the particle path until it either encounters land, a land mask, or
			//enters the same cell as the final destination
			if ( ! ((pxIndex == xIndex)  && (pyIndex == yIndex) ) ) nearLand = true;
			else nearLand = false;
		
			counter++;
			
			//not sure how stuff get's in here, but is happening every few percent, 
			if (counter > 100){
				System.out.println("loosing particles in reflection method still"); 
				p.setLost(true);
				return;
			}
		}//end while (end point is on land)
		
		//set new particle position
		p.setPX(cvt2Pos(px));
		p.setPY(py); 
		p.setX(cvt2Pos(x));
		p.setY(y);
	}

	
	
	/**Locates the closest array value to the given value, and returns
	 * the index of the array (uses binary search)
	 * 
	 * @param ja
	 * @param val
	 * @return
	 */
	public int locate(double[] ja, double val) {

		// Use binary search to look for the value.
		int idx = Arrays.binarySearch(ja, val);

		if (idx < 0) {

			// Error check
			if (idx == -1) {
				//throw new IllegalArgumentException(var.getName() + " value "
				//+ val + " does not fall in the range " + ja[0] + " : "
				//+ ja[ja.length - 1] + ".");
				return 0;
			}

			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= ja.length) {
				return ja.length-1;
			}

			double spval = (ja[-(idx + 2)] + ja[-(idx + 1)]) / 2d;
			if (val < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;
	}

	
	/**Checks if the particle's movement passes through a Land Mask Barrier
	 * (i.e., where water movement between two adjacent cells is blocked)
	 * 
	 * @param x
	 * @param y
	 * @param px
	 * @param py
	 * @return
	 */
	public boolean checkPassThruMask(int xIndex, int yIndex, int pxIndex, int pyIndex){

		
		//if the xIndex is not in the set of x index which are land masks, then return
		if (!landMaskListXs.contains(new Integer(xIndex))) return false; 
		
		//else, if it's in there, then check each one to see if the current location and previous location
		//straddle a barrier
		Set<Int3D> list = landMaskList.keySet(); 
		Iterator<Int3D> it = list.iterator();
		while (it.hasNext()){
			Int3D thisIndex = it.next();
			int type = landMaskList.get(thisIndex);
			Int3D thatIndex = new Int3D(); 
			
			//if type = 1, then is a western mask
			if (type == 1){
				thatIndex.x = thisIndex.x-1;
				thatIndex.y = thisIndex.y;

				if ( (thisIndex.x == xIndex && thatIndex.x ==pxIndex && thisIndex.y == yIndex && thatIndex.y ==pyIndex ) ||
						(thisIndex.x == pxIndex && thatIndex.x ==xIndex && thisIndex.y == pyIndex && thatIndex.y ==yIndex )){
					return true;
				}
			}

			//if type=2, then is a southern mask
			else if (type == 2){
				thatIndex.x = thisIndex.x;
				thatIndex.y = thisIndex.y-1;

				if ( (thisIndex.x == xIndex && thatIndex.x ==pxIndex && thisIndex.y == yIndex && thatIndex.y ==pyIndex ) ||
						(thisIndex.x == pxIndex && thatIndex.x ==xIndex && thisIndex.y == pyIndex && thatIndex.y ==yIndex )){
					return true;
				}
			}

			//if type=3, then is BOTH a western  and southern mask
			else if (type == 3){
				thatIndex.x = thisIndex.x-1;
				thatIndex.y = thisIndex.y;

				if ( (thisIndex.x == xIndex && thatIndex.x ==pxIndex && thisIndex.y == yIndex && thatIndex.y ==pyIndex ) ||
						(thisIndex.x == pxIndex && thatIndex.x ==xIndex && thisIndex.y == pyIndex && thatIndex.y ==yIndex )){
					return true;
				}
				
				thatIndex.x = thisIndex.x;
				thatIndex.y = thisIndex.y-1;

				if ( (thisIndex.x == xIndex && thatIndex.x ==pxIndex && thisIndex.y == yIndex && thatIndex.y ==pyIndex ) ||
						(thisIndex.x == pxIndex && thatIndex.x ==xIndex && thisIndex.y == pyIndex && thatIndex.y ==yIndex )){
					return true;
				}

			}
			
		}
		
		return false;
	}
	
	
	/**Must be a short
	 * 
	 * @param val
	 */
	public void setLandVal(short val){
		landVal = val; 
	}
	
	
	public void setNegLon(boolean neglon) {
		this.neglon = neglon;
	}
	
	/**
	 * Converts from positive Longitude coordinates to negative Longitude
	 * coordinates
	 * 
	 * @param oldlon
	 * @return
	 */

	protected synchronized double cvt2Neg(double oldlon) {
		if (oldlon > 180) {
			return -(360d - oldlon);
		} else
			return oldlon;
	}
	
	/**
	 * Converts from negative Longitude coordinates to positive Longitude
	 * coordinates
	 * 
	 * @param oldlon
	 * @return
	 */

	protected synchronized double cvt2Pos(double oldlon) {
		if (oldlon < 0) {
			return (360 + oldlon%360);
		} else
			return oldlon;
	}

	
	/**Sets the arrays (land mask grid, lats, and lons) for cloning
	 * 
	 * @param landGrid
	 * @param lats
	 * @param lons
	 */
	public void setArrays(short[][] landGrid, double[] lats, double[] lons){
		this.landGrid = landGrid;
		this.lats = lats;
		this.lons = lons;
	}
	
	
	public void setGridSizes(double lonSize, double latSize){
		this.gridXSize = lonSize;
		this.gridYSize = latSize; 
	}
	
	/**Clones this instance and returns a new instance of GridBarrier
	 * 
	 */
	@SuppressWarnings("unchecked")
	public GridBarrier clone(){
		GridBarrier out = new GridBarrier();
		short[][] landGridCopy = landGrid.clone();
		double[] latsCopy = lats.clone();
		double[] lonsCopy = lons.clone();
		HashMap<Int3D, Integer> list = (HashMap<Int3D, Integer>) landMaskList.clone();
		ArrayList<Integer> listXs = (ArrayList<Integer>) landMaskListXs.clone(); 
		out.setLandMaskList(list);
		out.setLandMaskSet(listXs);
		out.setArrays(landGridCopy, latsCopy, lonsCopy); 
		out.setGridSizes(gridXSize, gridYSize);
		out.setLandVal(landVal); 
		out.neglon = neglon; 
		return out;
	}
	
	
	public void setLandMaskSet(ArrayList<Integer> landMaskListXs){
		this.landMaskListXs = landMaskListXs; 
	}
	
	
	public void setLandMaskList(HashMap<Int3D, Integer> list){
		this.landMaskList = list; 
	}


	@Override
	public void closeConnections() {
		if (ncFile != null) ncFile.closeFile();
	}
}
