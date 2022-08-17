package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import us.fl.state.fwc.util.geo.CoordinateUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**	**************NOTE**************
 * This doesn't work -- for whatever reason the outfile writer stops writing at line 1433 and can't figure out why
 * So manually printed out anything greater than 1433 and then cut and paste in the .p2d file.....pain in butt.....
 * 
 * @author wade.cooper
 *
 */
public class EFDCConvertP2DCoords {


	private  PrintWriter outFile = null; 



	public static void main(String[] args) {
		EFDCConvertP2DCoords frc = new EFDCConvertP2DCoords();
		frc.step("dataTest/counties100KLandMask.p2d"); 
	}


	public void step(String filename) {

		try { 
			new File("output/counties100KLandMask_new.p2d").delete();
			
			outFile = new PrintWriter(new FileWriter("output/counties100KLandMask_new.p2d", true));
		} catch (IOException e) {e.printStackTrace();}

		int counter=1;
		
		try {

			BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); //split("[ ]+"); 

				
				if (tokens[0].contains("counties100KLandMaskPolys")){
					outFile.println(line);
				}
				else {
					 
					Coordinate coord = new Coordinate(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]), 0); 
					CoordinateUtils.convertUTMToLatLon(coord, 17, false);

					outFile.println(coord.x + "\t" + coord.y + "\t" + 0);

					if (counter >= 20906){
						System.out.println(coord.x + "\t" + coord.y + "\t" + 0); 
					}
				}


				counter++;

			}


		} catch (IOException e) {
			System.out.println("error!");
			e.printStackTrace();
		}

	}



}
