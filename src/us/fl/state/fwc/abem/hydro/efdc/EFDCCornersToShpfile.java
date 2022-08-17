package us.fl.state.fwc.abem.hydro.efdc;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class EFDCCornersToShpfile {

	String directory = "C:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	String shpName = "EFDC_CH3D_TB.shp";

	String defaultSRS = "EPSG:26917"; // this is UTM 17N
	String WGS84_SRS = "EPSG:4326";
	String crsCode = WGS84_SRS; 

	
	private File fFile; // input file reader
	private HashMap <Int3D, EFDCCell> gridCells; //  = new FastMap <PointLoc, EFDCCell>(); 
	Transaction transaction; 
	
	public static void main(String[] args) {

		EFDCCornersToShpfile shp = new EFDCCornersToShpfile(); 
		shp.initialize(); 
		shp.writeShapefile();
	}

	public void initialize(){
		EFDCGrid grid = new EFDCGrid();
		grid.initialize(directory + "corners.inp"); // set up the grid
		gridCells =grid.getGridCells(); 

		fFile = new File(directory + shpName);  

	}


	
	@SuppressWarnings("unchecked")
	public void writeShapefile() {
		
        try {

		CoordinateReferenceSystem crs = CRS.decode(crsCode); // EPSG code for UTM 17N
		SimpleFeatureType TYPE = createFeatureType(crs); 
	    FeatureCollection<SimpleFeatureType, SimpleFeature> collection = FeatureCollections.newCollection();
	    //GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

	    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);


	    Set<Int3D> set = gridCells.keySet();
		Iterator<Int3D> it = set.iterator();
		while (it.hasNext()) {
			
			Int3D index = it.next();
			Geometry cellGeom = gridCells.get(index).getGeom(); 
			String name = index.x + "_" + index.y; 
			
            featureBuilder.add(cellGeom);
            featureBuilder.add(name);
            SimpleFeature feature = featureBuilder.buildFeature(null);
            collection.add(feature);

		}
	    
		
		/*		
        ShapefileDataStore newShapefileDataStore = new ShapefileDataStore(new URL(outFile));
        newShapefileDataStore.createSchema(TYPE);
        newShapefileDataStore.forceSchemaCRS(crs);
           */     
        DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", fFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(TYPE);
        newDataStore.forceSchemaCRS(crs);

        
        // docs break transaction
        /*
         * Write the features to the shapefile
         */
        transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore =
                (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore.getFeatureSource(typeName);

        featureStore.setTransaction(transaction);
            featureStore.addFeatures(collection);
            transaction.commit();

        } catch (Exception problem) {
            problem.printStackTrace();
            try {
				transaction.rollback();
			} catch (IOException e) {
				e.printStackTrace();
			}

        } finally {
            try {
				transaction.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}

	
    /**
     * Here is how you can use a SimpleFeatureType builder to create the schema
     * for your shapefile dynamically.
     * <p>
     * This method is an improvement on the code used in the main method above
     * (where we used DataUtilities.createFeatureType) because we can set a
     * Coordinate Reference System for the FeatureType and a a maximum field
     * length for the 'name' field
     * dddd
     */
    private static SimpleFeatureType createFeatureType(CoordinateReferenceSystem crs) {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("GridCell");
        builder.setCRS(crs); // <- Coordinate reference system

        // add attributes in order
        builder.add("CellGeometry", Polygon.class);
        builder.length(25).add("Name", String.class); // <- 15 chars width for name field

        // build the type
        final SimpleFeatureType LOCATION = builder.buildFeatureType();
        
        return LOCATION;
    }

}
