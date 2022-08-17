package us.fl.state.fwc.util.geo;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class MultiPolygonToSimple {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		MultiPolygonToSimple mp = new MultiPolygonToSimple();
		mp.makeSimple(); //.testSimple();
	}


	
	
	@SuppressWarnings("unchecked")
	public void makeSimple(){

		try {

			
			
			/*Make new
			 * 
	        final SimpleFeatureType TYPE = DataUtilities.createType(
	                "Location",                   // <- the name for our feature type
	                "location:Polygon:srid=4326," + // <- the geometry attribute: Point type
	                "name:String"         // <- a String attribute
	        );
			 */
	        
	        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	        builder.setName("Location");
	        builder.setSRS("EPSG:26917"); //builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

	        // add attributes in order
	        builder.add("Location", Polygon.class);
	        builder.length(15).add("Name", String.class); // <- 15 chars width for name field

	        // build the type
	        final SimpleFeatureType TYPE = builder.buildFeatureType();
	        

	        FeatureCollection collection = FeatureCollections.newCollection();
	        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
	        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

			
			
			
			
			
			
			
			
			
			SimpleShapefile shape = new SimpleShapefile("C:\\GISData\\Habitats\\fl_40k_nowater_TBClip_poly.shp");
			shape.openShapefile();
			

			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = shape.getFeatureSource().getFeatures();

			FeatureSource<SimpleFeatureType, SimpleFeature> fSource = shape.getFeatureSource();
			List<AttributeDescriptor> atts = fSource.getSchema().getAttributeDescriptors();
			
			
			Class[] attType = new Class[atts.size()]; 
			String[] attName = new String[atts.size()];

			ArrayList<Coordinate[]> features = new ArrayList<Coordinate[]>();  
			
			for (int i =0; i<atts.size(); i++){
				if (atts.get(i).getType().getBinding().toString().equals("com.vividsolutions.jts.geom.MultiPolygon")){
					attType[i] = Polygon.class; 
				}
				attType[i] = atts.get(i).getType().getBinding(); 
				attName[i] = atts.get(i).getName().toString(); 
				System.out.println(atts.get(i).getName() + "\t" +atts.get(i).getType().getBinding());   
			}


			System.out.println("num features" + featureCollection.size());

			FeatureIterator<SimpleFeature> iterator = featureCollection.features();
			int counter = 0; 
			while( iterator.hasNext() ){

				//System.out.println("looping through feature " + counter++);

				SimpleFeature feature = iterator.next();

				Geometry geometry = (Geometry) feature.getDefaultGeometry();

				//get the attributes for the feature; will then copy and upload these to next
				//List<Object> attribs = feature.getAttributes();

				MultiPolygon mp = (MultiPolygon) geometry;

				if (mp.getNumGeometries() > 1) {
					for (int i=0; i<mp.getNumGeometries(); i++){
						Geometry geom = mp.getGeometryN(i);
						Polygon poly = (Polygon) geom;
						System.out.println("multipolygon for " + counter );
						//Coordinate[] coords = geom.getCoordinates();
						Coordinate[] coords = poly.getExteriorRing().getCoordinates();

						for (int j=0; j<coords.length; j++){
							CoordinateUtils.convertLatLonToUTM(coords[j], 17);
						}

						Polygon polygon = geometryFactory.createPolygon(new LinearRing(new CoordinateArraySequence(coords), geometryFactory), null);
						featureBuilder.add(polygon); 
	                    Integer count = new Integer(counter);
	                    String name = count.toString(); 
	                    featureBuilder.add(name); 
	                    SimpleFeature feat = featureBuilder.buildFeature(null);
	                    collection.add(feat);

						
						for (int j=0; j<coords.length; j++){
							//CoordinateUtils.convertLatLonToUTM(coords[j], 17);
						}
	
						if (coords[0].x != coords[coords.length-1].x || coords[0].y != coords[coords.length-1].y) {
							System.out.println("first and last not equal"); 
						}

						
												features.add(coords);
												/*features[counter][0] = coords;
						features[counter][1] =feature.getAttribute(1);
						features[counter][2] =feature.getAttribute(2);
						features[counter][3] =feature.getAttribute(3);
						features[counter][4] =feature.getAttribute(4);
*/						
						counter++;
					}

				}
				else{
					//Coordinate[] coords = mp.getCoordinates(); 
					Geometry geom = mp.getGeometryN(0);
					Polygon poly = (Polygon) geom;
					Coordinate[] coords = poly.getExteriorRing().getCoordinates();
					for (int j=0; j<coords.length; j++){
						CoordinateUtils.convertLatLonToUTM(coords[j], 17);
					}
					
					if (coords[0].x != coords[coords.length-1].x || coords[0].y != coords[coords.length-1].y) {
						System.out.println("first and last not equal for feature " + counter); 
					}
					
					
					Polygon polygon = geometryFactory.createPolygon(new LinearRing(new CoordinateArraySequence(coords), geometryFactory), null);
					featureBuilder.add(polygon); 
                    Integer count = new Integer(counter);
                    String name = count.toString(); 
                    featureBuilder.add(name); 

                    SimpleFeature feat = featureBuilder.buildFeature(null);
                    collection.add(feat);

					for (int j=0; j<coords.length; j++){
						//CoordinateUtils.convertLatLonToUTM(coords[j], 17);
					}
								features.add(coords);
								/*						features[counter][0] = coords;
											features[counter][1] =feature.getAttribute(1);
											features[counter][2] =feature.getAttribute(2);
											features[counter][3] =feature.getAttribute(3);
											features[counter][4] =feature.getAttribute(4);
					*/						

					counter++; 
				}


			}
			

			iterator.close(); 

			
			
			
			
			
			//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| WRITE THE SHAPEFILE ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 
			
			//SimpleShapefile newShape = new SimpleShapefile("C:\\GISData\\Habitats\\fl_40k_nowater_TBClip_simpleWGS_test2.shp");
			//newShape.createShapefile(Polygon.class, features, "WGS"); //.createShapefile(attType, attName, null, "UTM"); //.createShapefile(Polygon.class, "WGS"); 

			//newShape.addFeatures(Polygon.class, features); 
			
	        File newFile = new File("C:\\GISData\\Habitats\\fl_40k_nowater_TBClip_simpleUTM.shp");

	        DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

	        Map<String, Serializable> params = new HashMap<String, Serializable>();
	        params.put("url", newFile.toURI().toURL());
	        params.put("create spatial index", Boolean.TRUE);

	        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
	                .createNewDataStore(params);
	        newDataStore.createSchema(TYPE);

	        /*
	         * You can comment out this line if you are using the createFeatureType
	         * method (at end of class file) rather than DataUtilities.createType
	         */
	        //newDataStore.forceSchemaCRS(builder.getCRS()); //DefaultGeographicCRS.WGS84);
	        
	        
	        
	        /*
	         * Write the features to the shapefile
	         */
	        Transaction transaction = new DefaultTransaction("create");

	        String typeName = newDataStore.getTypeNames()[0];
	        FeatureSource featureSource = newDataStore.getFeatureSource(typeName);

	        if (featureSource instanceof FeatureStore) {
	            FeatureStore featureStore = (FeatureStore) featureSource;

	            featureStore.setTransaction(transaction);
	            try {
	                featureStore.addFeatures(collection);
	                transaction.commit();

	            } catch (Exception problem) {
	                problem.printStackTrace();
	                transaction.rollback();

	            } finally {
	                transaction.close();
	            }
	            System.exit(0); // success!
	        } else {
	            System.out.println(typeName + " does not support read/write access");
	            System.exit(1);
	        }
	    
	        
	        
	     
			
		}catch (Exception problem) {
			problem.printStackTrace();
		}

	}
	
	
	public void testSimple(){
		
		try {

		SimpleShapefile shape = new SimpleShapefile("C:\\GISData\\Habitats\\fl_40k_nowater_TBClip_simpleUTM.shp");
		shape.openShapefile();
		

		FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = shape.getFeatureSource().getFeatures();

		FeatureSource<SimpleFeatureType, SimpleFeature> fSource = shape.getFeatureSource();
		List<AttributeDescriptor> atts = fSource.getSchema().getAttributeDescriptors();
		
		
		for (int i =0; i<atts.size(); i++){
			System.out.println(atts.get(i).getName() + "\t" +atts.get(i).getType().getBinding());   
		}


		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		int counter = 0; 
		while( iterator.hasNext() ){

			//System.out.println("looping through feature " + counter++);

			SimpleFeature feature = iterator.next();


			//get the attributes for the feature; will then copy and upload these to next
			List<Object> attribs = feature.getAttributes();
			for (int i =0; i<atts.size(); i++){
				if (counter<100) System.out.println(attribs.get(i).toString());
			}

				counter++; 
			}


		iterator.close(); 

		
		
		
	}catch (Exception problem) {
		problem.printStackTrace();
	}

}	

}
