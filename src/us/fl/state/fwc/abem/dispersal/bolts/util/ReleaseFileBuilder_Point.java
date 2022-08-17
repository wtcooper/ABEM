package us.fl.state.fwc.abem.dispersal.bolts.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Point;

public class ReleaseFileBuilder_Point {

	String shpFileName = "data/BoltsRelease3_TB.shp";
	String outFileName = "output/BOLTs/release_TB.txt"; 
	int numParts = 100000; 
	double startDepth = 0.0;

	PrintWriter outFile= null; //need Time 	Speed	Direction, in meters per second


	public void step(){
		
		//2	27.7643	-82.8498	1.0	2	JohnsPass
		SimpleShapefile shpFile = new SimpleShapefile(shpFileName);
		
		new File(outFileName).delete(); 
		
		try { 
			outFile= new PrintWriter(new FileWriter(outFileName, true));
		} catch (IOException e) {e.printStackTrace();}

		int counter = 0 ; 

		try {
			FeatureSource<SimpleFeatureType, SimpleFeature> fSource = shpFile.getFeatureSource(); 

			FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
			 FeatureIterator<SimpleFeature> iterator = features.features();
		        try {
		            while (iterator.hasNext()) {
		                SimpleFeature simpleFeature = iterator.next();
						Point point = (Point) simpleFeature.getDefaultGeometry();
						String name = (String) simpleFeature.getAttribute("NAME");

						
						outFile.println(counter++ + 
								"\t" + point.getY() + 
								"\t" + point.getX() + 
								"\t" + startDepth + "\t" + numParts + "\t" + name);  

		            }
		        } finally {
		            iterator.close(); // IMPORTANT
		        }

		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
	
		outFile.close(); 
	}

	
	
	public static void main(String[] args) {
		ReleaseFileBuilder_Point r = new ReleaseFileBuilder_Point();
		r.step(); 
	}


}
