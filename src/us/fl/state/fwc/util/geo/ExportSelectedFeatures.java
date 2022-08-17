package us.fl.state.fwc.util.geo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;

public class ExportSelectedFeatures {

	protected DataStore dStore; 
	protected FeatureSource<SimpleFeatureType, SimpleFeature> fSource;
	protected SimpleFeatureType schema;
	protected FeatureIterator<SimpleFeature> iterator;
	protected DefaultTransaction transaction;
	protected GeometryFactory gf = new GeometryFactory();
	protected String[] typeNames;  
	protected String typeName; 

	ArrayList<SimpleFeature> featuresList =new ArrayList<SimpleFeature>();  
	
	String existingFileName, newFileName;
	
	
	
	public static void main( String[] args ){
		ExportSelectedFeatures e = new ExportSelectedFeatures("C:\\work\\data\\GISData\\Habitat\\WFS\\seagrass_swfwmd_2008_poly_WGS84.shp", "C:\\work\\data\\GISData\\Habitat\\WFS\\JustSeagrass_WGS84.shp"); 
		e.export("FLUCCS_COD", new Object[]{"9113", "9116"});
	}
	
	
	public ExportSelectedFeatures(String oldFilename, String newFilename){
		
		this.existingFileName = oldFilename;
		this.newFileName = newFilename; 
		
		File file = new File(existingFileName);  


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


			}
			catch( IOException eek ){
				System.err.println("Could not connect to data store - exiting");
				eek.printStackTrace();
				System.exit(1); // die die die
			}
		}
	}

	
	
	public void export(final String attribute, final Object[] values){

		
 
		
		try {
			FeatureCollection<SimpleFeatureType, SimpleFeature> features = fSource.getFeatures();
			
			
			
			features.accepts( new FeatureVisitor(){
				public void visit(Feature feature) {
					SimpleFeature simpleFeature = (SimpleFeature) feature;
					
					System.out.println("visiting feature" + simpleFeature.getID()); 
					
					Object attribValue = simpleFeature.getAttribute(attribute);
					
					for (int i=0; i<values.length; i++){
						
					if (attribValue.equals(values[i])){
							featuresList.add(simpleFeature); 
						}
					}
					}
				
			}, new NullProgressListener() );

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new File(newFileName).delete();
		SimpleShapefile shape = new SimpleShapefile(newFileName); 
		shape.createShapefile(featuresList, "WGS84");
		
	}
}
