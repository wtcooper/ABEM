package us.fl.state.fwc.abem.hydro.efdc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class EFDCShapefileToLandMask {


	int xOffset = 313295;
	int yOffset	= 3113157;
	double cellSize=10; 

	private  PrintWriter outFile = null; 

	public static void main(String[] args) {
		EFDCShapefileToLandMask frc = new EFDCShapefileToLandMask();
		frc.step("dataTest/EFDCLandMask.shp"); 
	}

	
	public void step(String filename) {
		
		// connect to data, iterate through all features, add to spatial index (Quadtree), and store geometry in fastmap with index 
		File file = new File(filename); 


		try { 
			outFile = new PrintWriter(new FileWriter("output/EFDCLandMask_gross.p2d", true));
		} catch (IOException e) {e.printStackTrace();}

		
		try {
			// Connection parameters
			Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
			connectParameters.put("url", file.toURI().toURL());
			connectParameters.put("create spatial index", true );
			DataStore dStore = DataStoreFinder.getDataStore(connectParameters);
			String[] typeNames = dStore.getTypeNames();
			String typeName = typeNames[0];
			FeatureSource<SimpleFeatureType, SimpleFeature> fSource = dStore.getFeatureSource(typeName);
		

			try {

				FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
				features.accepts( new FeatureVisitor(){
					public void visit(Feature feature) {
						SimpleFeature simpleFeature = (SimpleFeature) feature;
						Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
						String id = simpleFeature.getID(); 
						Coordinate[] coords = geom.getCoordinates(); 

						//print out the corners
						outFile.println(id); 
						for (int i=0; i<coords.length; i++){
							outFile.println(coords[i].x + "\t" + coords[i].y + "\t" + 0); 
						}

					}
				}, new NullProgressListener() );

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		catch( IOException eek ){
			System.err.println("Could not connect to data store - exiting");
			eek.printStackTrace();
			System.exit(1); // die die die
		}
		
		outFile.close(); 
	}



	
	
}
