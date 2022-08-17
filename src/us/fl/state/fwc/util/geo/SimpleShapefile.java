package us.fl.state.fwc.util.geo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import us.fl.state.fwc.util.TestingUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

public  class SimpleShapefile {


	protected ShapefileDataStore dStore; 
	protected FeatureSource<SimpleFeatureType, SimpleFeature> fSource;
	protected FeatureStore<SimpleFeatureType, SimpleFeature> fStore; 
	protected SimpleFeatureType schema;
	protected FeatureIterator<SimpleFeature> iterator;
	protected DefaultTransaction transaction;
	protected GeometryFactory gf; // = new GeometryFactory();
	protected SpatialIndex spatialIndex;
	protected String[] typeNames;  
	protected String typeName; 

	protected boolean isDynamic = false; 
	protected String HABITAT_SHPFILE_NAME; 
	protected String defaultSRS = "EPSG:26917"; // this is UTM 17N
	protected String WGS84_SRS = "EPSG:4326";
	protected String UTM17N_SRS =defaultSRS; 
	protected String UTM16N_SRS ="EPSG:26916"; 
	protected String filename; 

	protected boolean fileOpen = false; 
	/**Constructor which sets the filename and whether it's a dynamic file 
	 * (i.e., one with features to be added, removed, or modified, thereby requiring 
	 * a Quadtree versus STRtree spatial index)
	 * 
	 * @param filename
	 * @param isDynamic
	 */
	public SimpleShapefile(String filename){
		this.filename = filename; 
	}


	//==========================================================
	//============initialize()
	//==========================================================

	/*	public void initialize(boolean isDynamic){
		this.isDynamic = isDynamic; 
		openShapefile(); 
	}
	 */


	//==========================================================
	//============openShapefile()
	//==========================================================

	/** Makes and returns a DataStore connection to a specified shapefile.
	 * 
	 * @param store
	 * @param dataFileName
	 * @return
	 */
	public  synchronized  void openShapefile(){

		// Read in file
		File file = null;  
		if (!(filename == null)) file = new File(filename);  
		else file = getNewShapeFile(); // to have user select a file


		try {
			// Connection parameters
			Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
			connectParameters.put("url", file.toURI().toURL());
			connectParameters.put("create spatial index", true );
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) 
				dataStoreFactory.createNewDataStore(connectParameters);
			typeNames = dStore.getTypeNames();
			typeName = typeNames[0];

			fSource = dStore.getFeatureSource(typeName);
			schema = fSource.getSchema();

			fileOpen = true; 

			//				if (isDynamic){ // only open up fStore and transpactions if it's an editable shapefile
			//					fStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) fSource;
			//					transaction = new DefaultTransaction();
			//					fStore.setTransaction( transaction );
			//				}

			//				buildSpatialIndex(); // builds the index after all stores are set
		}
		catch( IOException eek ){
			System.err.println("Could not connect to data store - exiting");
			eek.printStackTrace();
			System.exit(1); // die die die
		}
	}


	//==========================================================
	//============createShapefile()
	//==========================================================

	/**Creates a new shapefile with the given attribute type and EPSG projection
	 * 
	 * @param attribType -- Polygon.class, LineString.class, or Point.class
	 * @param EPSG_code -- project as text string
	 */
	@SuppressWarnings("unchecked")
	public void createShapefile(ArrayList<SimpleFeature> features, String EPSG_code) {
		if (EPSG_code == null) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("WGS84")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM17N")) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("UTM16N")) EPSG_code = UTM16N_SRS;
		else if (EPSG_code.equals("WGS")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM")) EPSG_code = defaultSRS;

		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(EPSG_code); //alternate versus CRS


			builder.addAll(features.get(0).getFeatureType().getAttributeDescriptors()); 

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			//DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);
			dStore.forceSchemaCRS(builder.getCRS()); 

			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;


			FeatureCollection<SimpleFeatureType, SimpleFeature> collection = 
				FeatureCollections.newCollection();


			for (int i=0; i<features.size(); i++){
				collection.add(features.get(i));
			}


			transaction = new DefaultTransaction("create");
			if (fSource instanceof FeatureStore) {
				fStore = (FeatureStore) fSource;
				fStore.setTransaction(transaction);

				try {
					fStore.addFeatures(collection);
					transaction.commit();

				} catch (Exception problem) {
					problem.printStackTrace();
					System.out.println("catching error " + problem);
					TestingUtils.dropBreadCrumb(); 
					try {
						transaction.rollback();
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("catching error " + e);
						TestingUtils.dropBreadCrumb(); 
					}

				} finally {
					transaction.close();
				}
			} else {
				System.out.println(typeName + " does not support read/write access");
				System.exit(1);
			}


		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}



	//==========================================================
	//============createShapefile()
	//==========================================================

	/**Creates a new shapefile with the given attribute type and EPSG projection
	 * 
	 * @param attribType -- Polygon.class, LineString.class, or Point.class
	 * @param EPSG_code -- project as text string
	 */
	@SuppressWarnings("unchecked")
	public void createShapefile(Class attribType, String EPSG_code) {
		if (EPSG_code == null) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("WGS84")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM17N")) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("UTM16N")) EPSG_code = UTM16N_SRS;
		else if (EPSG_code.equals("WGS")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM")) EPSG_code = defaultSRS;


		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(EPSG_code); //alternate versus CRS



			builder.add("geometry", attribType); //attributeTypes.get(i));

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file



			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);
			if (EPSG_code.equals(WGS84_SRS)) 
				dStore.forceSchemaCRS(DefaultGeographicCRS.WGS84); 


			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}



	/**Creates a new shapefile with the given name, attribute types, and names 
	 * 
	 * @param url
	 * @param attributeTypes[atts] -- 
	 * 		the Class of attribute types, with Geometry first (Point, Polygon, or LineString)
	 * @param attributeNames[atts] -- 
	 * 		the name of the attribute 
	 * @param coords[features][vertices]-- 
	 * 		Coordinates for creating n features (Point: coords[n][1] / Polygons,LineString: 
	 * 		coords[n][x], where x are #vertices per feature)
	 * @param attributeValues[features][atts] -- 
	 * 		Values for the attributes of n features (attributeValues[n][numAttributes]  
	 */

	@SuppressWarnings("unchecked")
	public void createShapefile(Class[] attributeTypes, String[] attributeNames, 
			Coordinate[][] coords, Object[][] attributeValues, String EPSG_code){
		if (EPSG_code == null) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("WGS84")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM17N")) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("UTM16N")) EPSG_code = UTM16N_SRS;
		else if (EPSG_code.equals("WGS")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM")) EPSG_code = defaultSRS;



		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(EPSG_code); //alternate versus CRS

			for (int i=0; i<attributeTypes.length; i++){
				builder.add(attributeNames[i], attributeTypes[i]); //attributeTypes.get(i));
			}

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);
			if (EPSG_code.equals(WGS84_SRS)) 
				dStore.forceSchemaCRS(DefaultGeographicCRS.WGS84); 


			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;

			//add features
			if (!(coords == null)){
				addFeatures(attributeTypes[0], coords, attributeValues); 
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}

	
	
	/**Creates a new shapefile with the given attribute type and coordinates for adding feature(s)
	 * 
	 * @param attribType -- Polygon.class, LineString.class, or Point.class
	 * @param coords -- coords[numFeatures][coordsInEachFeature]
	 */
	@SuppressWarnings("unchecked")
	public void createShapefile(Class[] attributeTypes, String[] attributeNames, 
			ArrayList<Coordinate[]> coords, ArrayList<Object[]> attVals, String EPSG_code) {
		if (EPSG_code == null) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("WGS84")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM17N")) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("UTM16N")) EPSG_code = UTM16N_SRS;
		else if (EPSG_code.equals("WGS")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM")) EPSG_code = defaultSRS;


		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(EPSG_code); //alternate versus CRS

			for (int i=0; i<attributeTypes.length; i++){
				builder.length(25).add(attributeNames[i], attributeTypes[i]); //attributeTypes.get(i));
			}

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);
			if (EPSG_code.equals(WGS84_SRS)) 
				dStore.forceSchemaCRS(DefaultGeographicCRS.WGS84); 


			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;

			//add features
			if (!(coords == null)){
				addFeatures(attributeTypes[0], coords, attVals); //.get(0), coords, attributeValues);
			}


		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}

	
	

	//==========================================================
	//============createShapefile()
	//==========================================================


	/**Creates a new shapefile with the given name, attribute types, and names 
	 * 
	 * @param url
	 * @param attributeTypes[atts] -- 
	 * 		the Class of attribute types, with Geometry first (Point, Polygon, or LineString)
	 * @param attributeNames[atts] -- 
	 * 		the name of the attribute 
	 * @param coords[features][vertices]-- 
	 * 		Coordinates for creating n features (Point: coords[n][1] / Polygons,LineString: 
	 * 		coords[n][x], where x are #vertices per feature)
	 * @param attributeValues[features][atts] -- 
	 * 		Values for the attributes of n features (attributeValues[n][numAttributes]  
	 */

	@SuppressWarnings("unchecked")
	public void createShapefile(Class[] attributeTypes, String[] attributeNames, 
			Object[][] attributeValues, String EPSG_code){
		if (EPSG_code == null) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("WGS84")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM17N")) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("UTM16N")) EPSG_code = UTM16N_SRS;
		else if (EPSG_code.equals("WGS")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM")) EPSG_code = defaultSRS;



		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(EPSG_code); //alternate versus CRS

			for (int i=0; i<attributeTypes.length; i++){
				builder.add(attributeNames[i], attributeTypes[i]); //attributeTypes.get(i));
			}

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);
			if (EPSG_code.equals(WGS84_SRS)) 
				dStore.forceSchemaCRS(DefaultGeographicCRS.WGS84); 


			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;

			//add features
			if (!(attributeValues == null)){
				addFeatures(attributeTypes[0], attributeValues); //.get(0), coords, attributeValues);
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}

	/**Creates a new shapefile with the given attribute type and coordinates for adding feature(s)
	 * 
	 * @param attribType -- Polygon.class, LineString.class, or Point.class
	 * @param coords -- coords[numFeatures][coordsInEachFeature]
	 */
	@SuppressWarnings("unchecked")
	public void createShapefile(Class attribType, Coordinate[][] coords) {
		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(defaultSRS); //alternate versus CRS

			builder.add("geometry", attribType); //attributeTypes.get(i));

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);


			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;

			//add features
			if (!(coords == null)){
				addFeatures(attribType, coords, null); //.get(0), coords, attributeValues);
			}


		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}



	/**Creates a new shapefile with the given attribute type and coordinates for adding feature(s)
	 * 
	 * @param attribType -- Polygon.class, LineString.class, or Point.class
	 * @param coords -- coords[numFeatures][coordsInEachFeature]
	 */
	@SuppressWarnings("unchecked")
	public void createShapefile(Class attribType, Coordinate[][] coords, String EPSG_code) {
		if (EPSG_code == null) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("WGS84")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM17N")) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("UTM16N")) EPSG_code = UTM16N_SRS;
		else if (EPSG_code.equals("WGS")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM")) EPSG_code = defaultSRS;


		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(EPSG_code); //alternate versus CRS

			builder.add("geometry", attribType); //attributeTypes.get(i));

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);
			if (EPSG_code.equals(WGS84_SRS)) 
				dStore.forceSchemaCRS(DefaultGeographicCRS.WGS84); 

			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;

			//add features
			if (!(coords == null)){
				addFeatures(attribType, coords, null); //.get(0), coords, attributeValues);
			}


		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}

	}


	/**Creates a new shapefile with the given attribute type and coordinates for adding feature(s)
	 * 
	 * @param attribType -- Polygon.class, LineString.class, or Point.class
	 * @param coords -- coords[numFeatures][coordsInEachFeature]
	 */
	@SuppressWarnings("unchecked")
	public void createShapefile(Class attribType, 
			ArrayList<Coordinate[]> coords, String EPSG_code) {
		if (EPSG_code == null) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("WGS84")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM17N")) EPSG_code = defaultSRS;
		else if (EPSG_code.equals("UTM16N")) EPSG_code = UTM16N_SRS;
		else if (EPSG_code.equals("WGS")) EPSG_code = WGS84_SRS; 
		else if (EPSG_code.equals("UTM")) EPSG_code = defaultSRS;


		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(EPSG_code); //alternate versus CRS

			builder.add("geometry", attribType); //attributeTypes.get(i));

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			dStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);
			if (EPSG_code.equals(WGS84_SRS)) 
				dStore.forceSchemaCRS(DefaultGeographicCRS.WGS84); 


			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			fileOpen = true;

			//add features
			if (!(coords == null)){
				addFeatures(attribType, coords, null); //.get(0), coords, attributeValues);
			}


		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}

	
	



	//==========================================================
	//============addFeatures()
	//==========================================================

	/**Adds features (point, linestring, or polygon) to the shapefile
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void addFeatures(Class featureType, 
			Coordinate[][] coords, Object[][] attributeValues) {

		if (!fileOpen) openShapefile(); 

		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = 
			FeatureCollections.newCollection();

		/*
		 * GeometryFactory will be used to create the geometry attribute of each feature (a Point
		 * object for the location)
		 */
		gf = JTSFactoryFinder.getGeometryFactory(null);
		SimpleFeatureBuilder featureBuilder; 
		for (int i=0; i<coords.length; i++) { // loop through all features
			featureBuilder = new SimpleFeatureBuilder(schema);

			if (featureType == Point.class){
				Point point = gf.createPoint(coords[i][0]);
				featureBuilder.add(point);
			}
			else if (featureType == LineString.class){
				LineString linestring = gf.createLineString(coords[i]); 
				featureBuilder.add(linestring); 
			}
			else if (featureType == Polygon.class){
				Polygon polygon = gf.createPolygon(new LinearRing(
						new CoordinateArraySequence(coords[i]), gf), null);
				featureBuilder.add(polygon); 
			}

			if (!(attributeValues == null)){
				for (int j=0; j<attributeValues.length; j++){
					featureBuilder.add(attributeValues[i][j]);
				}
			}

			SimpleFeature feature = featureBuilder.buildFeature(null);
			collection.add(feature);
		}


		transaction = new DefaultTransaction("create");
		if (fSource instanceof FeatureStore) {
			fStore = (FeatureStore) fSource;
			fStore.setTransaction(transaction);

			try {
				fStore.addFeatures(collection);
				transaction.commit();

			} catch (Exception problem) {
				System.out.println("catching error " + problem);
				TestingUtils.dropBreadCrumb(); 
				try {
					transaction.rollback();
				} catch (IOException e) {
					System.out.println("catching error " + e);
					TestingUtils.dropBreadCrumb(); 
				}

			} finally {
				transaction.close();
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
	}


	//==========================================================
	//============addFeatures()
	//==========================================================

	/**Adds features (point, linestring, or polygon) to the shapefile
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void addFeatures(Class featureType, Object[][] attributeValues) {

		if (!fileOpen) openShapefile(); 

		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = 
			FeatureCollections.newCollection();

		/*
		 * GeometryFactory will be used to create the geometry attribute of each feature (a Point
		 * object for the location)
		 */
		gf = JTSFactoryFinder.getGeometryFactory(null);
		SimpleFeatureBuilder featureBuilder; 
		for (int i=0; i<attributeValues.length; i++) { // loop through all features
			featureBuilder = new SimpleFeatureBuilder(schema);

			if (featureType == Point.class){
				Point point = gf.createPoint( (Coordinate) ((Coordinate[])attributeValues[i][0])[0]);
				featureBuilder.add(point);
			}
			else if (featureType == LineString.class){
				LineString linestring = gf.createLineString((Coordinate[])attributeValues[i][0]); 
				featureBuilder.add(linestring); 
			}
			else if (featureType == Polygon.class){
				Polygon polygon = gf.createPolygon(new LinearRing(
						new CoordinateArraySequence((Coordinate[])attributeValues[i][0]), gf), null);
				featureBuilder.add(polygon); 
			}

			if (!(attributeValues == null)){
				for (int j=1; j<attributeValues[i].length; j++){
					featureBuilder.add(attributeValues[i][j]);
				}
			}

			SimpleFeature feature = featureBuilder.buildFeature(null);
			collection.add(feature);
		}


		transaction = new DefaultTransaction("create");
		if (fSource instanceof FeatureStore) {
			fStore = (FeatureStore) fSource;
			fStore.setTransaction(transaction);

			try {
				fStore.addFeatures(collection);
				transaction.commit();

			} catch (Exception problem) {
				System.out.println("catching error " + problem);
				TestingUtils.dropBreadCrumb(); 
				try {
					transaction.rollback();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("catching error " + e);
					TestingUtils.dropBreadCrumb(); 
				}

			} finally {
				transaction.close();
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
	}



	//==========================================================
	//============addPoints()
	//==========================================================

	/**Adds features (point, linestring, or polygon) to the shapefile
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void addFeatures(Class featureType, ArrayList<Coordinate[]> coords, 
			ArrayList<Object[]> attributeValues) {

		if (!fileOpen) openShapefile(); 

		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = 
			FeatureCollections.newCollection();

		/*
		 * GeometryFactory will be used to create the geometry attribute of each feature (a Point
		 * object for the location)
		 */
		gf = JTSFactoryFinder.getGeometryFactory(null);
		SimpleFeatureBuilder featureBuilder; 
		for (int i=0; i<coords.size(); i++) { // loop through all features
			featureBuilder = new SimpleFeatureBuilder(schema);

			if (featureType == Point.class){
				Point point = gf.createPoint(coords.get(i)[0]);
				featureBuilder.add(point);
			}
			else if (featureType == LineString.class){
				LineString linestring = gf.createLineString(coords.get(i)); 
				featureBuilder.add(linestring); 
			}
			else if (featureType == Polygon.class){
				Polygon polygon = gf.createPolygon(new LinearRing(
						new CoordinateArraySequence(coords.get(i)), gf), null);
				featureBuilder.add(polygon); 
			}

			if (!(attributeValues == null)){
				for (int j=0; j<attributeValues.get(0).length; j++){
					featureBuilder.add(attributeValues.get(i)[j]);
				}
			}

			SimpleFeature feature = featureBuilder.buildFeature(null);
			collection.add(feature);
			
			System.out.println("created shapefile for feature i: " + i);

		}


		transaction = new DefaultTransaction("create");
		if (fSource instanceof FeatureStore) {
			fStore = (FeatureStore) fSource;
			fStore.setTransaction(transaction);

			try {
				fStore.addFeatures(collection);
				transaction.commit();

			} catch (Exception problem) {
				problem.printStackTrace();
				System.out.println("catching error " + problem);
				TestingUtils.dropBreadCrumb(); 

				try {
					transaction.rollback();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("catching error " + e);
					TestingUtils.dropBreadCrumb(); 

				}

			} finally {
				transaction.close();
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
	}


	@SuppressWarnings("unchecked")
	public void updatePointCoord(Coordinate coord){

		openShapefile();
		transaction = new DefaultTransaction("create");
		try {

			FeatureWriter writer= dStore.getFeatureWriter(transaction);
			while( writer.hasNext() ){
				writer.next();
				writer.remove();
			}
			writer.close();

			transaction.commit();

		} catch (Exception problem) {
			problem.printStackTrace();
			System.out.println("catching error " + problem);
			TestingUtils.dropBreadCrumb(); 

			try {
				transaction.rollback();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("catching error " + e);
				TestingUtils.dropBreadCrumb(); 

			}

		} finally {
			transaction.close();
		}


		addFeatures(Point.class, new Coordinate[][]{{coord}}, null); 

	}

	
	
	@SuppressWarnings("unchecked")
	public void updatePointCoords(Coordinate[] coords){

		openShapefile();
		transaction = new DefaultTransaction("create");
		//first, remove all old points
		try {

			FeatureWriter writer= dStore.getFeatureWriter(transaction);
			while( writer.hasNext() ){
				writer.next();
				writer.remove();
			}
			writer.close();

			transaction.commit();

		} catch (Exception problem) {
			problem.printStackTrace();
			System.out.println("catching error " + problem);
			TestingUtils.dropBreadCrumb(); 

			try {
				transaction.rollback();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("catching error " + e);
				TestingUtils.dropBreadCrumb(); 

			}

		} finally {
			transaction.close();
		}


		addPoints(coords); 

	}
	
	
	//==========================================================
	//============addPoints()
	//==========================================================

	/**Adds points to the shapefile
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void addPoints(Coordinate[] coords) {

		if (!fileOpen) openShapefile(); 

		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = FeatureCollections.newCollection();

		/*
		 * GeometryFactory will be used to create the geometry attribute of each feature (a Point
		 * object for the location)
		 */
		gf = JTSFactoryFinder.getGeometryFactory(null);
		SimpleFeatureBuilder featureBuilder; 
		for (int i=0; i<coords.length; i++) { // loop through all features
			featureBuilder = new SimpleFeatureBuilder(schema);

				Point point = gf.createPoint(coords[i]);
				featureBuilder.add(point);


			SimpleFeature feature = featureBuilder.buildFeature(null);
			collection.add(feature);
		}


		transaction = new DefaultTransaction("create");
		if (fSource instanceof FeatureStore) {
			fStore = (FeatureStore) fSource;
			fStore.setTransaction(transaction);

			try {
				fStore.addFeatures(collection);
				transaction.commit();

			} catch (Exception problem) {
				problem.printStackTrace();
				System.out.println("catching error " + problem);
				TestingUtils.dropBreadCrumb(); 

				try {
					transaction.rollback();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("catching error " + e);
					TestingUtils.dropBreadCrumb(); 

				}

			} finally {
				transaction.close();
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
	}

	
	
	
	//==========================================================
	//============getFeatureSource()
	//==========================================================

	public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {
		if (fSource == null) openShapefile(); 
		return fSource;
	}


	
	
	//==========================================================
	//============buildSpatialIndex()
	//==========================================================


	/**	Builds the spatial index with the geometry Envelope bounds as index and the 
	 * SimpleFeature as the indexed object.
	 * 
	 */
	public void buildSpatialIndex(){

		if (!fileOpen) openShapefile(); 
		
		spatialIndex = new STRtree(); // if the shapefile is static (i.e., no edits to it), use a STRtree 
													//which is faster but can't change after its initialized
		try {
			FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
			features.accepts( new FeatureVisitor(){
				public void visit(Feature feature) {
					SimpleFeature simpleFeature = (SimpleFeature) feature;
					Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
					Envelope bounds = geom.getEnvelopeInternal();
					if( bounds.isNull() ) return; // must be empty geometry?                
					spatialIndex.insert( bounds, simpleFeature);
				}
			}, new NullProgressListener() );

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 

		}
	}
	
	
	/**Returns the spatial index for searching purposes
	 * 
	 * @return
	 */
	public SpatialIndex getSpatialIndex(){
		if (spatialIndex == null ) buildSpatialIndex();
		return spatialIndex; 
	}
	
	//==========================================================
	//============getNewShapefile()
	//==========================================================

	/**
	 * Prompt the user for the name and path to use for the output shapefile
	 * 
	 * @return name and path for the shapefile as a new File object
	 */
	private static File getNewShapeFile() {

		JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
		chooser.setDialogTitle("Save shapefile");

		int returnVal = chooser.showSaveDialog(null);

		if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
			// the user cancelled the dialog
			System.exit(0);
		}

		File newFile = chooser.getSelectedFile();

		return newFile;
	}

}
