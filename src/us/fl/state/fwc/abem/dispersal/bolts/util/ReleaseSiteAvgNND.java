package us.fl.state.fwc.abem.dispersal.bolts.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import us.fl.state.fwc.util.geo.SpatialUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class ReleaseSiteAvgNND {

	ArrayList<Coordinate>  releaseSiteLocs = new ArrayList<Coordinate>(); 

	
	public void step(){
		setReleaseSites("output/BOLTs/release.txt"); 
		double avgNND = SpatialUtils.computeAvgNND(releaseSiteLocs);
		System.out.println("Average nearest neighbor distance (in units of projection): " + avgNND);
	}
	
	
	/**Set's the release sites into a linked list queue
	 * 
	 * @param releaseFile
	 */
	public void setReleaseSites(String releaseFile){
		File file = new File(releaseFile); 
		BufferedReader reader; 

		try {
			reader = new BufferedReader(new FileReader(file));

			//loop over all release groups in the release.txt file
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); 

				float lat = Float.parseFloat(tokens[1]);
				float lon = Float.parseFloat(tokens[2]);
				Coordinate coord = new Coordinate(lon, lat, 0);

				releaseSiteLocs.add(coord); 

			}
			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			System.exit(1);
		}

	}

	public static void main(String[] args) {

		ReleaseSiteAvgNND a = new ReleaseSiteAvgNND();
		a.step();
	}

}
