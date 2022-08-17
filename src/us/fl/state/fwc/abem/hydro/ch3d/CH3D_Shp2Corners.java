package us.fl.state.fwc.abem.hydro.ch3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.Int2D;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class CH3D_Shp2Corners {


	String filename =
		//"C:\\work\\data\\GISData\\BaseMaps\\HydroModels\\CH3D_CH.shp"
		"C:\\work\\data\\GISData\\BaseMaps\\HydroModels\\CH3D_TB_working.shp"
		; 

	String outFile;
	PrintWriter out= null; 


	public void run() {

		try {

			/**Read in X/Y from netCDF, store in array, along with I/J pointers
			 * Export shp(), storing the I/J pointers
			 */

			//####################################
			//Set up output file
			String path = null; 
			if (filename.contains("CH3D_TB")) path = "C:\\work\\workspace\\EFDC\\TampaBayCH3DGrid";
			else path = "C:\\work\\workspace\\EFDC\\CharlotteHarborCH3DGrid";
			outFile  = path + "/CH3DCornerstest.txt";
			new File(outFile).delete();

			out= new PrintWriter(new FileWriter(outFile, true));

			SimpleShapefile shp = new SimpleShapefile(filename);
			FeatureSource fSource = shp.getFeatureSource();

			FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
			features.accepts( new FeatureVisitor(){
				int counter = 0; 
				public void visit(Feature feature) {
					SimpleFeature simpleFeature = (SimpleFeature) feature;
					Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
					Coordinate[] coords = geom.getCoordinates(); 
					Double iIndex = (Double) simpleFeature.getAttribute("IGRID");
					Double jIndex = (Double) simpleFeature.getAttribute("JGRID");

					int iIn = (int) (Math.round(iIndex.doubleValue()));
					int jIn = (int) (Math.round(jIndex.doubleValue()));
					
					if (iIn == 94 && jIn == 82) {
						System.out.println();
					}


					for (int i=0; i<coords.length-1; i++){
						//Coordinate newCoord = new Coordinate(coords[i].x, coords[i].y, 0);
						//CoordinateUtils.convertLatLonToUTM(newCoord, 17);

						double x = coords[i].x;
						double y = coords[i].y;

						double xScaled = x * 1000;
						xScaled = Math.round(xScaled);
						xScaled= xScaled/ 1000;

						double yScaled = y * 1000;
						yScaled = Math.round(yScaled);
						yScaled= yScaled/ 1000;

						out.print(xScaled + "\t" + yScaled + "\t" ); 
					}

					out.println();

					String id = simpleFeature.getID(); 

					System.out.println("finished with feature " + id);
				}
			}, new NullProgressListener() );

			out.close();

		} catch (IOException e) {
			System.out.println("error");
			e.printStackTrace();
		}

	}




	public static void main(String[] args) {
		CH3D_Shp2Corners c = new CH3D_Shp2Corners();
		c.run();
	}
}
