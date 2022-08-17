package us.fl.state.fwc.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.util.geo.GeometryUtils;
import us.fl.state.fwc.util.pathfinding.ArrayPathFinder;

import com.vividsolutions.jts.geom.Geometry;

public class BuildReachableCellsMap {

	HashMap<Integer, ArrayList<Int3D>>  reachables = new HashMap<Integer, ArrayList<Int3D>>();
	ArrayList<Int3D> alreadyAdded = new ArrayList<Int3D>();
	
	//the radiuses 
	int[] cellRadiusArray = {1,2,3,4,5};
	ABEMGrid grid;
	Int3D gridCellIndex;
	public ArrayList<Integer> landMaskListXs; //a set that holds just the x index values for faster check
	public HashMap<Int3D, Integer> landMaskList; 

	//output text file
	PrintWriter outFile= null; 
	String outFileName = "output/ReachableCellsMap.txt";




	public void step(){

		new File(outFileName).delete();
		try { 
			outFile= new PrintWriter(new FileWriter(outFileName, true));
		} catch (IOException e) {e.printStackTrace();}

		grid = new ABEMGrid(SchedulerParams.gridFilename, true);
		int searchRadius;	


		HashMap<Int3D, ABEMCell> cells = grid.getGridCells();
		Set<Int3D> keys = cells.keySet();
		Iterator<Int3D> it = keys.iterator();
		int counter = 0;
		while (it.hasNext() ){
			
			gridCellIndex = it.next();
			ABEMCell cell = grid.getGridCell(gridCellIndex);

			reachables.clear();
			alreadyAdded.clear();
			
			for (int i=0; i<cellRadiusArray.length; i++){

				searchRadius = cellRadiusArray[i]; 
				ArrayList<Int3D> reachableList = new ArrayList<Int3D>();
				//add to the reachablesMap
				reachables.put(searchRadius, reachableList);
				
//				outFile.print("\n" + gridCellIndex.x+","+gridCellIndex.y+"\t" + searchRadius +"\t");
//				System.out.print("\n" + gridCellIndex.x+","+gridCellIndex.y+"\t\t"+ searchRadius +"\t");


				//set up the temporary map 
				int mapDim = searchRadius;
				//set the temporary map
				int [][] map = new int[mapDim*2+1][mapDim*2+1];
				int xcount=0, ycount=0;
				for (int y=gridCellIndex.y-mapDim; y<=gridCellIndex.y+mapDim; y++){
					xcount=0;
					for (int x=gridCellIndex.x-mapDim; x<=gridCellIndex.x+mapDim; x++){
						if (grid.getGridCell(new Int3D(x, y, 0)) == null) map[ycount][xcount] = 0;
						else map[ycount][xcount] = 1;
						xcount++;
					}			
					ycount++;
				}

				//set up the pathfinder
				ArrayPathFinder pf = new ArrayPathFinder();
				Int2D startNode = new Int2D(mapDim, mapDim);


				Geometry searchBuffer = GeometryUtils.getSearchBuffer(cell.getCentroidCoord(), searchRadius*grid.getCellWidth()); 
				ArrayList<Int3D> gridTestSites = grid.getCellsWithinRange(searchBuffer); 

				for (int j = gridTestSites.size()-1; j>=0; j--){ // count these down backwards so closest points will have preference if some cells have same habQuality value
					Int3D thatIndex = gridTestSites.get(j);
					

						
						
					//if it's this cell or this cell has been added previously, then continue to end of loop
					if (thatIndex.equals(gridCellIndex) || alreadyAdded.contains(thatIndex)) continue;

					//add 1 so that will round up
					int dx = thatIndex.x - gridCellIndex.x;
					int dy = thatIndex.y - gridCellIndex.y;

					//if the cell index is outside of the rounded-up search radius, then return true
					if (Math.abs(dx) > mapDim || Math.abs(dy) > mapDim) continue;


					Int2D endNode = new Int2D(mapDim+dx,mapDim+dy);

					ArrayList<Int2D> nodes = pf.compute(map, startNode, endNode);


					//if can't be reached, then continue to the next gridTestSite
					if (nodes == null) continue; 

					//if the total number of steps to get there times a scaler for converting between
					//Manhattan to Euclidian distances (0.8) times the size of a cell is greater than the 
					//search radius, then continue to next gridTestSite
					if ((nodes.size()-1)*grid.getCellWidth()*.7 > (searchRadius+.5)*grid.getCellWidth()) continue;

					//because I have land masks in my grid (i.e., a vector barrier between two adjacent cells)
					//I need to do a secondary check here.  If the computed best path passes through a 
					//barrier, then I return true (i.e., it strikes a barrier).  Note: this assumes that can't take a different
					//path to get to the location which is erroneous, but is simplest way to deal with this rather rate 
					//exception
					boolean passesThruMask = false;
					for(int n=0; n<nodes.size()-1; n++){
						Int2D n0 = nodes.get(n);
						Int2D n1 = nodes.get(n+1);

						//use this formulation: get the origin (for x and y, respectively), then add in the array node value
						if (checkPassThruMask((gridCellIndex.x-mapDim)+n0.x, (gridCellIndex.y-mapDim)+n0.y, (gridCellIndex.x-mapDim)+n1.x, (gridCellIndex.y-mapDim)+n1.y)) {
							passesThruMask = true;
							break;
						}

					}
					//if passes through a land mask, then continue to the next gridTestSite
					if (passesThruMask) continue;

					//else, output the location as one that can be reached

					reachableList.add(new Int3D(thatIndex.x, thatIndex.y));
					alreadyAdded.add(new Int3D(thatIndex.x, thatIndex.y));
					
//					outFile.print(thatIndex.x+","+thatIndex.y+"\t"	);
//					System.out.print(thatIndex.x+","+thatIndex.y+"\t\t"	);

//					if (gridCellIndex.x == 122 && gridCellIndex.y == 195 && searchRadius == 10 && thatIndex.x == 121 && thatIndex.y == 190){
//						System.out.println("test stop");
//					}

					
				}//end of loop over all gridTestSites

			}//end of loop over all cellRadiusArrays

			System.out.println("finished looping over all cell radii for (" + gridCellIndex.x + ", " + gridCellIndex.y + "), counter: " + counter++);
			
			//print out the results
			SortedSet<Integer> mapKeys = new TreeSet<Integer>(reachables.keySet());
			Iterator<Integer> mapIt = mapKeys.iterator();
		
			while (mapIt.hasNext()){
				Integer radi = mapIt.next();
				outFile.print(gridCellIndex.x+","+gridCellIndex.y+"\t"  + radi + "\t");
				ArrayList<Int3D> reachableList = reachables.get(radi);
				for (int i = 0; i<reachableList.size();i++){
					Int3D index = reachableList.get(i);
					outFile.print(index.x+","+index.y+"\t"	);
				}
				outFile.println();
			}
			
			

		} //end of while loop over all ABEM cells

		outFile.close();

	} //end of step() 




	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

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



	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

	public  void setLandMaskListXs() {
		landMaskListXs = new ArrayList<Integer>(); //a set that holds just the x index values for faster check

		landMaskListXs.add(84);
		landMaskListXs.add(85);
		landMaskListXs.add(86);
		landMaskListXs.add(87);
		landMaskListXs.add(88);
		landMaskListXs.add(131);
		landMaskListXs.add(130);
		landMaskListXs.add(127);
		landMaskListXs.add(126);
		landMaskListXs.add(122);
		landMaskListXs.add(121);
		landMaskListXs.add(124);
		landMaskListXs.add(125);
	}




	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

	public  void setLandMaskList() {
		landMaskList = new HashMap<Int3D, Integer>(); 

		landMaskList.put(new Int3D(85,173,0), 1);
		landMaskList.put(new Int3D(85,172,0), 1);
		landMaskList.put(new Int3D(86,170,0), 1);
		landMaskList.put(new Int3D(87,168,0), 3);
		landMaskList.put(new Int3D(88,170,0), 1);
		landMaskList.put(new Int3D(131,64,0), 1);
		landMaskList.put(new Int3D(131,65,0), 2);
		landMaskList.put(new Int3D(127,191,0), 1);
		landMaskList.put(new Int3D(127,190,0), 1);
		landMaskList.put(new Int3D(127,189,0), 1);
		landMaskList.put(new Int3D(122,197,0), 2);
		landMaskList.put(new Int3D(121,197,0), 2);
		landMaskList.put(new Int3D(124,201,0), 2);
		landMaskList.put(new Int3D(125,201, 0), 2);
	}



	public static void main(String[] args) {
		BuildReachableCellsMap b = new BuildReachableCellsMap();
		b.setLandMaskList();
		b.setLandMaskListXs();
		b.step();
	}

}
