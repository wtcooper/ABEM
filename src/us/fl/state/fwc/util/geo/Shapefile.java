package us.fl.state.fwc.util.geo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

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
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.index.strtree.STRtree;

public  class Shapefile {


	protected DataStore dStore; 
	protected FeatureSource<SimpleFeatureType, SimpleFeature> fSource;
	protected FeatureStore<SimpleFeatureType, SimpleFeature> fStore; 
	protected SimpleFeatureType schema;
	protected FeatureIterator<SimpleFeature> iterator;
	protected DefaultTransaction transaction;
	protected GeometryFactory gf = new GeometryFactory();
	protected SpatialIndex spatialIndex;
	protected String[] typeNames;  
	protected String typeName; 

	protected boolean isDynamic = true; 
	protected String HABITAT_SHPFILE_NAME; 
	protected String defaultSRS = "EPSG:26917"; // this is UTM 17N
	protected String filename; 


	/**Constructor which sets the filename and whether it's a dynamic file 
	 * (i.e., one with features to be added, removed, or modified, thereby requiring 
	 * a Quadtree versus STRtree spatial index)
	 * 
	 * @param filename
	 * @param isDynamic
	 */
	public Shapefile(String filename, boolean isDynamic){
		this.filename = filename; 
		this.isDynamic = isDynamic; 
	}


	//========================================================================
	//============initialize()
	//========================================================================

	/*	public void initialize(boolean isDynamic){
		this.isDynamic = isDynamic; 
		openShapefile(); 
	}
	 */


	//========================================================================
	//============openShapefile()
	//========================================================================

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


		if( dStore == null ){
			try {
				// Connection parameters
				Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
				connectParameters.put("url", file.toURI().toURL());
				connectParameters.put("create spatial index", true );
				dStore = DataStoreFinder.getDataStore(connectParameters);
				typeNames = dStore.getTypeNames();
				typeName = typeNames[0];

				fSource = dStore.getFeatureSource(typeName);
				schema = fSource.getSchema();



				if (isDynamic){ // only open up fStore and transpactions if it's an editable shapefile
					fStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) fSource;
					transaction = new DefaultTransaction();
					fStore.setTransaction( transaction );
				}

				buildSpatialIndex(); // builds the index after all stores are set
			}
			catch( IOException eek ){
				System.err.println("Could not connect to data store - exiting");
				eek.printStackTrace();
				System.exit(1); // die die die
			}
		}
	}


	//========================================================================
	//============createShapefile()
	//========================================================================


	/**Creates a new shapefile with the given name, attribute types, and names 
	 * 
	 * @param url
	 * @param attributeTypes -- the Class of attribute types, with Geometry first (Point, Polygon, or LineString)
	 * @param attributeNames -- the name of the attribute
	 * @param coords-- Coordinates for creating n features (Point: coords[n][1] / Polygons,LineString: coords[n][x], where x are #vertices per feature)
	 * @param attributeValues -- Values for the attributes of n features (attributeValues[n][numAttributes]  
	 */

	@SuppressWarnings("unchecked")
	public void createShapefile(Class[] attributeTypes, /* ArrayList<Class> attributeTypes, */String[] attributeNames, Coordinate[][] coords, Object[][] attributeValues){
		try {

			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName(filename);
			builder.setSRS(defaultSRS); //alternate versus CRS

			for (int i=0; i<attributeTypes.length; i++){
				builder.add(attributeNames[i], attributeTypes[i]); //attributeTypes.get(i));
			}

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			dStore = dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);

			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			//add features
			if (!(coords == null)){
				addFeatures(attributeTypes[0], coords, attributeValues); //.get(0), coords, attributeValues);
			}

			//close this shapefile if not dynamic
			if (!isDynamic) closeTransaction(); 
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

			builder.add("polygon", attribType); //attributeTypes.get(i));

			// build the type
			schema = builder.buildFeatureType();

			File newFile = null; 
			if (!(filename == null)) newFile = new File(filename);  
			else newFile = getNewShapeFile(); // to have user select a file


			DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);
			dStore = dataStoreFactory.createNewDataStore(params);
			dStore.createSchema(schema);

			typeName = dStore.getTypeNames()[0];
			fSource = dStore.getFeatureSource(typeName);

			//add features
			if (!(coords == null)){
				addFeatures(attribType, coords, null); //.get(0), coords, attributeValues);
			}
			
			//close this shapefile if not dynamic
			if (!isDynamic) closeTransaction(); 

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	//========================================================================
	//============addPoints()
	//========================================================================

	/**Adds features (point, linestring, or polygon) to the shapefile
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void addFeatures(Class featureType, Coordinate[][] coords, Object[][] attributeValues) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = FeatureCollections.newCollection();

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
				Polygon polygon = gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords[i]), gf), null);
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
				problem.printStackTrace();
				try {
					transaction.rollback();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} finally {
				//try {
				//	transaction.close();
				//} catch (IOException e) {
				//	e.printStackTrace();
				//}
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
	}


	//========================================================================
	//============addPoints()
	//========================================================================

	/**Adds points to the shapefile
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void addPoints(Coordinate[] coords, Object[] attributeValues) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = FeatureCollections.newCollection();

		/*
		 * GeometryFactory will be used to create the geometry attribute of each feature (a Point
		 * object for the location)
		 */
		gf = JTSFactoryFinder.getGeometryFactory(null);
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);

		for (int i=0; i<coords.length; i++) {
			Point point = gf.createPoint(coords[i]);
			featureBuilder.add(point);

			if (!(attributeValues == null)){
				for (int j=0; j<attributeValues.length; j++){
					featureBuilder.add(attributeValues[i]);
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
				problem.printStackTrace();
				try {
					transaction.rollback();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} finally {
				//try {
				//	transaction.close();
				//} catch (IOException e) {
				//	e.printStackTrace();
				//}
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
	}




	//========================================================================
	//============buildSpatialIndex()
	//========================================================================


	/**	Builds the spatial index 
	 * 
	 */
	private  void buildSpatialIndex(){

		if (isDynamic)	spatialIndex =new Quadtree(); // if the shapefile changes, use a Quadtree spatial index which is slower but allows changes
		else spatialIndex = new STRtree(); // if the shapefile is static (i.e., no edits to it), use a STRtree which is faster but can't change after its initialized
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**Returns the spatial index for searching purposes
	 * 
	 * @return
	 */
	public SpatialIndex getSpatialIndex(){
		return spatialIndex; 
	}

	//========================================================================
	//============getFeatureAttribute()
	//========================================================================

	/** Gets the specified attribute value for a feature at a given coordinate.  Note, no features may overlap in space.
	 * 
	 * @param coord - the coordinate location to query
	 * @param attribute - the attribute type to return
	 */

	public Object getFeatureAttribute(Coordinate coord, String attribute){

		Object attribValue =null; 

		SimpleFeature feature = getFeature(coord);
		if (feature != null){
			attribValue = feature.getAttribute(attribute);
		}
		else attribValue = null;
		return attribValue;
	}



	//========================================================================
	//============getFeature()
	//========================================================================

	/** Returns a feature at a specific coordinate.  Must be only one feature at the location, else it will return the first one it comes to
	 * 
	 * @param coord - the Coordinate location of the query 
	 */
	@SuppressWarnings("unchecked")
	public  SimpleFeature getFeature(Coordinate coord) {

		Point location = gf.createPoint(coord);
		List<SimpleFeature> hits = spatialIndex.query(location.getEnvelopeInternal());
		if (hits.size() == 0) {
			return null; 
		}
		SimpleFeature feature;
		for (int i = 0; i < hits.size(); i++) {
			feature= hits.get(i);
			Geometry geom = (Geometry) feature.getAttribute(schema.getGeometryDescriptor().getLocalName());
			if (location.intersects(geom)) {
				return feature;
			}
		}
		return null; 
	}


	//========================================================================
	//============getFeature()
	//========================================================================

	/** Returns a feature at a specific coordinate.  Must be only one feature at the location, else it will return the first one it comes to
	 * 
	 * @param location - the Point location of the query 
	 */
	@SuppressWarnings("unchecked")
	public  SimpleFeature getFeature(Point location) {

		List<SimpleFeature> hits = spatialIndex.query(location.getEnvelopeInternal());
		if (hits.size() == 0) {
			return null; 
		}
		SimpleFeature feature;
		for (int i = 0; i < hits.size(); i++) {
			feature= hits.get(i);
			Geometry geom = (Geometry) feature.getAttribute(schema.getGeometryDescriptor().getLocalName());
			if (location.intersects(geom)) {
				return feature;
			}
		}
		return null; 
	}


	//========================================================================
	//============getFeatures()
	//========================================================================

	/** Returns a list of all features that intersect the supplied geometry.  
	 * 
	 * @param geometry
	 */
	@SuppressWarnings("unchecked")
	public List<SimpleFeature> getFeatures(Geometry geometry) {
		List<SimpleFeature> hits = spatialIndex.query(geometry.getEnvelopeInternal());
		List<SimpleFeature> collection = new ArrayList(); 
		if (hits.size() == 0) {
			return null; 
		}
		SimpleFeature feature;
		for (int i = 0; i < hits.size(); i++) {
			feature= hits.get(i);
			Geometry geom = (Geometry) feature.getAttribute(schema.getGeometryDescriptor().getLocalName());
			if (geometry.intersects(geom)) {
				collection.add(feature); 
			}
		}
		return collection; 
	}



	//========================================================================
	//============addPolygon()
	//========================================================================

	/**	Creates a new feature if no existing features the same spatial location.  Else, if features exist, then will either merge
	 * the new feature if the attribute of interest is the same, or if attribute is different, will modify the existing feature to cut 
	 * out the new feature and add the new feature.  If multiple features in the same spatial location, will either merge or cut 
	 * out depending on the attributes of the existing features
	 * 
	 * @param attribArray -- an array of Objects which represent all of the attribute values for each attribute in the feature source (need prior knowledge to sort these correctly)
	 * @param compareAttribute -- the attribute type which is the basis for comparison among features that the new feature will intersect
	 * @throws IOException 
	 * @throws IOException
	 */

	public void addPolygon(Object[] attribArray, String compareAttrib)  {

		//FeatureStore<SimpleFeatureType, SimpleFeature> fStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) fSource;
		//DefaultTransaction transaction = new DefaultTransaction();
		//fStore.setTransaction( transaction );

		try {

			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints()  );
			Set<FeatureId> removeFeatureSet = new HashSet<FeatureId>();
			Filter removeFilter = null; 
			Set<FeatureId> modifyFeatureSet = new HashSet<FeatureId>();
			Filter modifyFilter = null; 

			// set Geometry based on coordArray
			Geometry oldGeometry = (Geometry) attribArray[schema.indexOf(schema.getGeometryDescriptor().getLocalName())]; 

			FilterFactory2 ff2 = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints()  );
			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
			Filter filter= ff2.intersects(ff2.property(geometryPropertyName), ff2.literal(oldGeometry)); 


			FeatureCollection<SimpleFeatureType, SimpleFeature> fCollection = fStore.getFeatures(filter);


			// Old way to get features using Quadtree which doesn't work
			// get all the features that overlap the bounding box based on spatial index query
			//List<SimpleFeature> fCollection = getFeatures(oldGeometry);


			if (fCollection != null){

				SimpleFeature feature; 
				iterator = fCollection.features();

				try {
					while( iterator.hasNext()){
						feature = iterator.next();


						if (((String) feature.getAttribute(compareAttrib)).equals((String) attribArray[schema.indexOf(compareAttrib)])){

							Geometry newGeometry = oldGeometry.union((Geometry) feature.getAttribute(schema.getGeometryDescriptor().getLocalName()));
							attribArray[schema.indexOf("the_geom")] = newGeometry; 

							// here, adds the features ID to a HashSet; will then use 
							// Filter filter = ff.id( fids );  fStore.removeFeatures(filter); 
							// at end of method to remove all of these features
							removeFeatureSet.add( ff.featureId(feature.getID())); 
							removeFilter = ff.id( removeFeatureSet);
							fStore.removeFeatures(removeFilter);
							removeFeatureSet.clear();

							// remove the old feature that is to be merged from the spatial index; a new spatial index will be added with the newly merged feature is created below
							Geometry geom = (Geometry) feature.getDefaultGeometry();
							Point centroid =geom.getCentroid(); 
							SimpleFeature indexFeature = getFeature(centroid);
							Envelope bounds = geom.getEnvelopeInternal();
							if( bounds.isNull() ) return; // must be empty geometry?                
							spatialIndex.remove( bounds, indexFeature);


							// reset other necessary attributes as needed, e.g., area, etc., whatever needed for that shapefile
						}

						// if the attribute value is different, then 
						else {

							// remove this modified feature from the spatial index; will then add it later with its new bounding box
							Geometry geom = (Geometry) feature.getDefaultGeometry();
							Point centroid =geom.getCentroid(); 
							SimpleFeature indexFeature = getFeature(centroid);
							Envelope bounds = geom.getEnvelopeInternal();
							if( bounds.isNull() ) return; // must be empty geometry?                
							spatialIndex.remove( bounds, indexFeature);

							Geometry newGeometry = (geom.difference(oldGeometry));
							modifyFeatureSet.add(ff.featureId(feature.getID()));
							modifyFilter = ff.id(modifyFeatureSet);

							fStore.modifyFeatures(schema.getName(), newGeometry, modifyFilter);
							feature.setDefaultGeometry(newGeometry);
							modifyFeatureSet.clear();

							// add the modified feature back to the spatial index with the correct bounding box
							Envelope newBounds = newGeometry.getEnvelopeInternal();
							if( newBounds .isNull() ) return; // must be empty geometry?                
							spatialIndex.insert( newBounds , feature);

						}
						//}
					}
				}
				finally{
					//fCollection.close(iterator); 
					//if( iterator != null ){
					iterator.close(); //this was necessary change in upgrade to 2.7 -- the fCollection.close(iterator) became deprecated
					//}
				}
			}

			// build the new feature here -- it'll either be the original geometry as passed in, or the new geometry if it was merged
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
			builder.addAll( attribArray );
			SimpleFeature newFeature = builder.buildFeature(null);
			fStore.addFeatures(DataUtilities.collection( newFeature ));

			// add new merged feature to spatial Index with the new geometry
			Geometry geom = (Geometry) newFeature.getDefaultGeometry();
			Envelope bounds = geom.getEnvelopeInternal();
			if( bounds.isNull() ) return; // must be empty geometry?                
			spatialIndex.insert( bounds, newFeature );

			transaction.commit();
			System.out.println("create feature attribute transaction committed.");

		}
		catch( Exception eek){
			try {
				transaction.rollback();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(eek);
			System.out.println("createFeature() error");
		}
		finally {
			//transaction.close();
		}
	}


	//========================================================================
	//============removePolygon()
	//========================================================================

	/** Removes a feature at the specificed coordinate.  Note: should only be a single feature at any given coordinate in a single shapefile
	 * 
	 * @param dStore
	 * @param coord
	 * @param EPSG
	 * @throws IOException 
	 * @throws IOException
	 */
	public void removePolygon(Coordinate coord)  {

		//FeatureStore<SimpleFeatureType, SimpleFeature> fStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) fSource;
		//DefaultTransaction transaction = new DefaultTransaction();
		//fStore.setTransaction( transaction );

		try {

			Point location = gf.createPoint(coord);
			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints()  );
			Filter filter= ff.intersects(ff.property(geometryPropertyName), ff.literal(location)); 
			FeatureCollection<SimpleFeatureType, SimpleFeature> fCollection = fStore.getFeatures(filter);

			// Old way using Quadtree
			//SimpleFeature feature = getFeature(coord); 

			Set<FeatureId> removeFeatureSet = new HashSet<FeatureId>();

			if (fCollection != null){
				iterator = fCollection.features();
				// step through iterator
				// if the same attribute set, then merge Geometries and get new Geometry (using geometry.symDifference() method)

				try{
					while( iterator.hasNext()){
						SimpleFeature feature = iterator.next();

						removeFeatureSet.add( ff.featureId(feature.getID())); 

						// remove this feature from the spatial index
						Geometry geom = (Geometry) feature.getDefaultGeometry();
						Point centroid =geom.getCentroid(); 
						SimpleFeature indexFeature = getFeature(centroid);
						Envelope bounds = geom.getEnvelopeInternal();
						if( bounds.isNull() ) return; // must be empty geometry?                
						spatialIndex.remove( bounds, indexFeature);

					}
				}
				finally{
					//fCollection.close(iterator); 
					//if( iterator != null ){
					iterator.close(); //this was necessary change in upgrade to 2.7 -- the fCollection.close(iterator) became deprecated
					//}
				}

				Filter removeFilter = ff.id( removeFeatureSet);
				fStore.removeFeatures(removeFilter);

				transaction.commit();
				System.out.println("remove feature attribute transaction committed.");

			}
		}

		catch( Exception eek){
			try {
				transaction.rollback();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(eek);
			System.out.println("removeFeature() error");
		}
		finally {
			//transaction.close();
		}
	}


	//========================================================================
	//============modifyPolygon()
	//========================================================================

	/** Modifies the attributes of a feature(s) at a specified location.  Will set all features to the same new attribute value, but in 
	 * most cases this method will be performed on only a single feature unless they overlap in the same file
	 * 
	 * @param coord
	 * @param attribute
	 * @param newValue
	 * @throws IOException 
	 * @throws IOException
	 */

	public void modifyPolygon(Coordinate coord, String attribute, Object newValue) {

		//FeatureStore<SimpleFeatureType, SimpleFeature> fStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) fSource;
		//DefaultTransaction transaction = new DefaultTransaction();
		//fStore.setTransaction( transaction );

		try {

			Point location = gf.createPoint(coord);
			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints()  );
			Filter filter= ff.intersects(ff.property(geometryPropertyName), ff.literal(location)); 
			FeatureCollection<SimpleFeatureType, SimpleFeature> fCollection = fStore.getFeatures(filter);

			Set<FeatureId> modifyFeatureSet = new HashSet<FeatureId>();

			//SimpleFeature feature = getFeature(coord);

			if (fCollection != null){
				iterator = fCollection.features();
				// step through iterator
				// if the same attribute set, then merge Geometries and get new Geometry (using geometry.symDifference() method)

				try {
					while( iterator.hasNext()){
						SimpleFeature feature = iterator.next();

						// if the attribute is the geometry
						if(attribute.equals(schema.getGeometryDescriptor().getLocalName())) {

							// first remove the feature from the spatial index with the old geometry
							Geometry geom = (Geometry) feature.getDefaultGeometry();
							Point centroid =geom.getCentroid(); 
							SimpleFeature indexFeature = getFeature(centroid);
							Envelope bounds = geom.getEnvelopeInternal();
							if( bounds.isNull() ) return; // must be empty geometry?                
							spatialIndex.remove( bounds, indexFeature);

							// then add the feature back to the spatial index with the new geometry
							Geometry newGeometry = (Geometry) newValue;
							Envelope newBounds = newGeometry.getEnvelopeInternal();
							if( newBounds.isNull() ) return; // must be empty geometry?                
							spatialIndex.insert( newBounds, feature);

						}

						modifyFeatureSet.add( ff.featureId(feature.getID()));
						Filter modifyFilter = ff.id( modifyFeatureSet);
						fStore.modifyFeatures(schema.getName(), newValue, modifyFilter); 
						transaction.commit();
						System.out.println("modify feature attribute transaction committed.");
					}
				}
				finally{
					//fCollection.close(iterator); 
					//if( iterator != null ){
					iterator.close(); //changed for 2.7 -- see use above
					//}
				}
			}
		}
		catch( Exception eek){
			try {
				transaction.rollback();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(eek);
			System.out.println("modifyFeature() error");
		}
		finally {
			//transaction.close();
		}
	}


	//========================================================================
	//============makePolygon()
	//========================================================================

	/**	Makes a new polygon geometry with the coordinates in the array.  Note, the coords must be closed, 
	 * i.e., the first and last element in the array must be the same coordinate.
	 * 
	 * @param coords
	 * @return
	 */
	public  Geometry makePolygon(Coordinate[] coords){

		Geometry newGeometry = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 

		return newGeometry; 
	}


	//========================================================================
	//============makePoint()
	//========================================================================

	/**Makes a point geometry with the given coordinate
	 * 
	 * @param coord
	 * @return
	 */
	public  Geometry makePoint(Coordinate coord){
		Geometry newGeometry = (Geometry) gf.createPoint(coord); 
		return newGeometry; 
	}


	//========================================================================
	//============makeLine()
	//========================================================================

	/**Creates a line given an array of coordinates
	 * 
	 * @param coords
	 * @return
	 */
	public  Geometry makeLine(Coordinate[] coords){
		Geometry newGeometry = (Geometry) gf.createLineString(coords);  
		return newGeometry; 
	}

	public int getNumFeatures(){
		int count = 0; 
		try {
			fSource = dStore.getFeatureSource(typeName);
			count = fSource.getFeatures().size();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return count; 
	}



	//========================================================================
	//============getIndexSize()
	//========================================================================

	public int getIndexSize(){
		if (spatialIndex instanceof Quadtree) {
			Quadtree tree = (Quadtree) spatialIndex; 
			return tree.size(); 
		}
		else {
			STRtree tree = (STRtree ) spatialIndex; 
			return tree.size(); 
		}
	}


	//========================================================================
	//============closeTransaction()
	//========================================================================

	public void closeTransaction(){
		transaction.close();
	}


	//========================================================================
	//============getFeatureSource()
	//========================================================================

	public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {
		return fSource;
	}


	//========================================================================
	//============getNewShapefile()
	//========================================================================

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
