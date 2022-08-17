package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class SimpleMapperTest {

	public final  String landFileName = "c:\\work\\data\\GISData\\BaseMaps\\FloridaBaseLayers\\fl_40k_nowater_TBClip_simpleWGS_test.shp";

	public void step(){

		double x = -82.54;
		double y = 27.74; 
		String pointsShpName = "dataTest/Points2.shp"; 
		SimpleShapefile pointsShp = new SimpleShapefile(pointsShpName);
		Coordinate[][] pointCoords = {  {new Coordinate(x, y)}  }; 
		pointsShp.createShapefile(Point.class, pointCoords, "WGS");

		x = -82.44;
		y = 27.64; 
		String pointsShpName2 = "dataTest/Points3.shp"; 
		SimpleShapefile pointsShp2 = new SimpleShapefile(pointsShpName2);
		Coordinate[][] pointCoords2 = {  {new Coordinate(x, y)}  }; 
		pointsShp2.createShapefile(Point.class, pointCoords2, "WGS");

		
		//Create Map
//		Toolkit toolkit =  Toolkit.getDefaultToolkit ();
//		Dimension dim = toolkit.getScreenSize();
		SimpleMapper map = new SimpleMapper("map test", 700, 500); 
		String[] layers = {landFileName, pointsShpName, pointsShpName2};
		Style[] layerStyles = {	SLD.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), .5f), 
				SLD.createPointStyle("circle", new Color(150,0,150), new Color(150,0,150), 1.0f, 5f), 
				SLD.createPointStyle("circle", new Color(150,150,150), new Color(150,150,150), 1.0f, 5f)};
		map.enableLayers(true);
		map.drawShapefileLayers(layers, layerStyles);  


	}

	


	  
	  
	public void step2(){
		try {

			double x = -82.54;
			double y = 27.74; 
			String pointFilename ="dataTest/points.shp"; 

			//create point shapefile
			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName("location");
			builder.setSRS("EPSG:4326"); //alternate versus CRS

			builder.add("the_geom", Point.class); //attributeTypes.get(i));

			// build the type
			SimpleFeatureType schema = builder.buildFeatureType();

/*			File newFile = null; 
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

			
			
			final SimpleFeatureType TYPE = DataUtilities.createType(
					"Location",                   // <- the name for our feature type
					"location:Point:srid=4326," + // <- the geometry attribute: Point type
					"name:String"         // <- a String attribute
			);
			
*/
			SimpleFeatureCollection collection = FeatureCollections.newCollection();
			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);
			Point point = geometryFactory.createPoint(new Coordinate(x, y));

			featureBuilder.add(point);
			SimpleFeature feature = featureBuilder.buildFeature(null);
			collection.add(feature);
			new File(pointFilename).delete(); 
			File newFile = new File(pointFilename);

			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);

			ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(schema);
			//newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

			Transaction transaction = new DefaultTransaction("create");

			String typeName2 = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName2);

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

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
			} 


			//Open land shapefile
			File file = new File(landFileName);  

			// Connection parameters
			Map<String,Serializable> connectParameters = new HashMap<String,Serializable>();
			connectParameters.put("url", file.toURI().toURL());
			connectParameters.put("create spatial index", true );

			File file2 = new File(pointFilename);  

			// Connection parameters
			Map<String,Serializable> connectParameters2 = new HashMap<String,Serializable>();
			connectParameters2.put("url", file2.toURI().toURL());
			connectParameters2.put("create spatial index", true );
			DataStore dStore2 = DataStoreFinder.getDataStore(connectParameters2);
			String[] typeNames2 = dStore2.getTypeNames();
			String typeName3 = typeNames2[0];
			FeatureSource<SimpleFeatureType, SimpleFeature> fSource2 = dStore2.getFeatureSource(typeName3);














			MapContext map = new DefaultMapContext();
			map.setTitle("test");

			//map.addLayer(fSource, SLD.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), .5f)); 
			map.addLayer(collection, SLD.createPointStyle("circle", new Color(150,0,150), new Color(150,0,150), 1.0f, 5f)); 

			JMapFrame frame = new JMapFrame(map); //.showMap(map);
			frame.setSize(500, 500);
			frame.setLocationRelativeTo(null); 
			frame.enableStatusBar(true);
			frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
			frame.enableToolBar(true);
			frame.setVisible(true);

			frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);


		}
		catch( IOException eek ){
			System.err.println("Could not connect to data store - exiting");
			eek.printStackTrace();
			System.exit(1); // die die die
		} 



	}

	public void step3(){
		SimpleMapper map = new SimpleMapper("map test", 800, 500); 
		String[] layers = {landFileName};
		Style[] layerStyles = {SLD.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), .5f)};
		JMapFrame frame = map.getFrame();
		map.enableLayers(true); 
		map.drawShapefileLayers(layers, layerStyles);  
	}

	public static void main(String[] args) {

		SimpleMapperTest test = new SimpleMapperTest();
		test.step();
	}

}
