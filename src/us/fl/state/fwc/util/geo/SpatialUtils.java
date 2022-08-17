package us.fl.state.fwc.util.geo;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;

public class SpatialUtils {

	
	public static double computeAvgNND(ArrayList<Coordinate> coords){
		int elements = coords.size();
		double[] nnDistances = new double[elements]; 
		double sumDistances = 0; 
		
		for (int i = 0; i<coords.size(); i++){

			Coordinate coord = coords.get(i);
			Coordinate nearestNeighbor = findNearestNeighbor(coord, coords, i); 

			double distance = CoordinateUtils.getDistance(coord, nearestNeighbor); 
			nnDistances[i] = distance;
			sumDistances += distance;
		}
		return sumDistances/elements;
	}
	
	// A crude way to find the nearest neighbor

	public static Coordinate findNearestNeighbor(Coordinate focalCoord, ArrayList<Coordinate> coords, int focalCoordIndex) {
		double mindist,dist;
		int mini = 0;
		int elements = coords.size();
		
		mindist = CoordinateUtils.getDistance(focalCoord, coords.get(mini));
		for(int j=0; j < elements; j++)
		{
			if (j != focalCoordIndex){
				dist = CoordinateUtils.getDistance(focalCoord, coords.get(j));
				if(dist < mindist)
				{
					mini = j;
					mindist = dist;
				}
			}
		}
		return coords.get(mini);
	}

	
	
}
