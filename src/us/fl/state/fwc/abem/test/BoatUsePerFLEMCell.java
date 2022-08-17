package us.fl.state.fwc.abem.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import javolution.util.FastTable;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.identity.FeatureId;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

public class BoatUsePerFLEMCell {

	long seed = System.currentTimeMillis(); 
	MersenneTwister m = new MersenneTwister((int) seed); 
	Uniform uniform = new Uniform(m); 

	private GeometryFactory gf = new GeometryFactory();
	private SpatialIndex spatialIndex = new STRtree();

	FastTable <BoatUseFeature> FLEMCells = new FastTable <BoatUseFeature> (); 
	FastMap <FeatureId, BoatUseFeature> destinations = new FastMap <FeatureId, BoatUseFeature> (); 

	FastTable<BoatUseFeature> destList = new FastTable<BoatUseFeature>(); 

	private  PrintWriter outFile = null; 
	
	public static void main(String[] args) {



		BoatUsePerFLEMCell bu = new BoatUsePerFLEMCell(); 
		bu.initialize("dataTest/FLEMCombined.shp");
		bu.initialize("dataTest/TB_DESTINATIONS_011304_UTM.shp");

		bu.step(); 
	}



	public void step(){
		try { outFile = new PrintWriter(new FileWriter("output/BoatUseFLEMCell.txt", true));
		} catch (IOException e) {e.printStackTrace();}
		//TestingUtils.dropBreadCrumb();

		System.out.println("num of flem cells: " + FLEMCells.size()); 
		System.out.println("num of destination cells: " + destinations.size()); 
		int numOfCells = 0;
		int totalDestinations = 0; 
		// loop through all FLEM cells and get the total number of destinations (just fishing) in each cell
		
		for (int i = 0; i<FLEMCells.size(); i++){
			BoatUseFeature feat = FLEMCells.get(i); 
			Geometry geom = feat.getGeom(); 
			int numOfDest = getDestNumInGridCell(geom);
			feat.addDest(numOfDest);

			numOfCells++;
			totalDestinations += numOfDest; 
		}

		//System.out.println("totalDestinations: " + totalDestinations); 
		double avgNumDest = (double) totalDestinations / (double) numOfCells; 
		System.out.println("avgNumDest:\t" + avgNumDest); 

		for (int i = 0; i < FLEMCells.size(); i++	 ){
			BoatUseFeature feat = FLEMCells.get(i);
			FeatureId FID = feat.getFID(); 
			double deviation = (double) feat.getNumOfDest() - avgNumDest; 
			outFile.println(FID + "\t" + deviation);
		}
		
		outFile.close(); 

	}




	public void initialize(final String filename) {


		// connect to data, iterate through all features, add to spatial index (Quadtree), and store geometry in fastmap with index 
		File file = new File(filename); 


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
						//						Point point = geom.getCentroid(); 
						//						System.out.println("feature id: " + simpleFeature.getID() + "\tCentroid: (" + point.getX() + ", " + point.getY() + ")"); 
						Envelope bounds = geom.getEnvelopeInternal();
						
						if( bounds.isNull() ) {
							System.out.println("a bounds was null so returning"); 
							return; // must be empty geometry?                
						}

						// TODO -- need to output shapefile with i,j index, so that when I access netCDF for actual data, I have the i,j Point location

						// need methods 'feature.getI....' and 'feature.getJ....'  or whatever appropriate way to get correct attributes in geotools
						// then, can store into FastMap:


						if (filename.equals("dataTest/FLEMCombined.shp")){

							FeatureId FID = simpleFeature.getIdentifier(); //getAttribute("FID"); 
							//spatialIndex.insert( bounds, FID); // a spatialIndex of the PointLoc's index

							BoatUseFeature feat = new BoatUseFeature(); 
							feat.setFID(FID); 
							feat.setGeom(geom);
							feat.setType("FLEM"); 
							FLEMCells.add(feat); 
						}

						
						else if (filename.equals("dataTest/TB_DESTINATIONS_011304_UTM.shp")) {
							
							String fishing = (String) simpleFeature.getAttribute("FH"); 

							// only get the feature if it's for fishing
							if (fishing.equals("Y")){
						
								//System.out.println("bounds of destination, minX: " + bounds.getMinX() + "\tmaxX: " + bounds.getMaxX() + "\tminY: " + bounds.getMinY() + "\tmaxY: " + bounds.getMaxY() ); 
								
								FeatureId FID = simpleFeature.getIdentifier(); //.getAttribute("FID"); 
								spatialIndex.insert( bounds, FID); // a spatialIndex of the PointLoc's index

								BoatUseFeature feat = new BoatUseFeature();
								feat.setFID(FID); 
								feat.setGeom(geom);
								feat.setType("Dest"); 
								destinations.put(FID, feat);
							}
						}



					}
				}, new NullProgressListener() );

			} catch (IOException e) {
				System.out.println("something off in feature visitation"); 
				e.printStackTrace();
			}
		}
		catch( IOException eek ){
			System.err.println("Could not connect to data store - exiting");
			eek.printStackTrace();
			System.exit(1); // die die die
		}
	}



	/**
	 * Returns the number of destinations in each grid cell
	 * @param cellGeom
	 * @return
	 */
	public int getDestNumInGridCell(Geometry cellGeom) {

		Envelope bounds= cellGeom.getEnvelopeInternal();
		//System.out.println("bounds of destination, minX: " + bounds.getMinX() + "\tmaxX: " + bounds.getMaxX() + "\tminY: " + bounds.getMinY() + "\tmaxY: " + bounds.getMaxY() ); 

		int count = 0; 
		//query returns the value that is stored in the spatialIndex, in this case it's the String of the FID 
		List<FeatureId> hits = spatialIndex.query(cellGeom.getEnvelopeInternal());

		if (hits.size() == 0) {
			//System.out.println("ZERO IN GETDESTNUMINGRIDCELL"); 
			return 0; 
		}
		FeatureId index = null; 
		for (int i = 0; i < hits.size(); i++) {
			index = hits.get(i);
			Geometry geom = (Geometry) destinations.get(index).getGeom(); 
			if (cellGeom.contains(geom)) { 
				count++;  
			}
		}	
		//System.out.println("count: " + count); 
		return count;
	}




}
