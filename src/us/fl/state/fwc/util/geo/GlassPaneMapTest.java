package us.fl.state.fwc.util.geo;


import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.geotools.data.CachingFeatureSource;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;

import com.vividsolutions.jts.geom.Coordinate;

public class GlassPaneMapTest {


	String shp = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp";
	public final  String shpLandFileName = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp"; // Name of the land mask file
	public final String habitatFileName = "c:\\work\\data\\BOLTs\\JustSeagrass_WGS84_TBClip2_poly.shp";// Name of the settlement polygon file

	public boolean drawParticles = true; 
	public boolean drawWithGlassPane = true; 
	public boolean drawPreviousPoint = true;
	public boolean drawWithStyle = true;
	public boolean drawHabitat = true;
	public JMapFrame frame; 
	private HashMap<Long, Particle> particleMap; 
	double mapLag = 0.01; 
	String displayBaseShpFile = shpLandFileName; // "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\EFDC_TampaToSarasota_WGS_Grid.shp";


	public static void main(String[] args) {

		GlassPaneMapTest map = new GlassPaneMapTest();
		
			try {
				map.step2();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

	
	@SuppressWarnings({ "unchecked", "static-access" })
	public void step2() throws IOException {
		
		particleMap = new HashMap<Long, Particle>(); 

		File landFile = new File(displayBaseShpFile);
		File habFile = new File(habitatFileName); 
		FileDataStore habStore;;
		FeatureSource habFSource;
		CachingFeatureSource habCache = null; 

		if (this.drawHabitat){
			habStore = FileDataStoreFinder.getDataStore(habFile);
			habFSource = habStore.getFeatureSource();
			habCache = new CachingFeatureSource(habFSource); 
		}

		FileDataStore store = FileDataStoreFinder.getDataStore(landFile);
		FeatureSource featureSource = store.getFeatureSource();
		CachingFeatureSource landCache = new CachingFeatureSource(featureSource); 

		// Create a map context and add our shapefile to it
		MapContext map = new DefaultMapContext();

		map.setTitle("test"); 
		if (this.drawWithStyle) map.addLayer(landCache, SLD.createPolygonStyle(new Color(97, 133, 70), new Color(97, 100, 70), .5f));
		else map.addLayer(landCache, null);

		if (this.drawHabitat) {
			if (this.drawWithStyle) map.addLayer(habCache, SLD.createPolygonStyle(new Color(97, 215, 70), new Color(97, 200, 70), .5f));
			else map.addLayer(habCache, null);
		}


		frame = new JMapFrame(map);  

		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN,JMapFrame.Tool.RESET);
		frame.enableToolBar(true);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);


		
		GlassPaneParticleMap partMap = new GlassPaneParticleMap(particleMap, frame);
		if (this.drawPreviousPoint) partMap.setPrintPreviousPoint(true);
		partMap.setStyle("circle", Color.red, 4);
		frame.setGlassPane(partMap);
		partMap.setVisible(true); 
		
		
		
	       Particle p = new Particle(null); 
	        p.setX(-82.6);
	        p.setY(27.6);
	        p.setPX(-82.61);
	        p.setPY(27.61);
	        p.setID(0l);
	        particleMap.put(p.getID(), p);

	        while (true){
	        	try { Thread.currentThread().sleep((long) (0.1*1000)); } catch (InterruptedException e) { e.printStackTrace(); }
	        	p.setPX(p.getX());
	        	p.setPY(p.getY());
	        	p.setX(p.getX()+Math.random()*.01-.005);
	        	p.setY(p.getY()+Math.random()*.01-.005);
	        	partMap.update(p);
	        	
	        }

		
	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	public void step() throws IOException{
		
		  
		  
	       File file = new File(shp);
	        if (file == null) {
	            return;
	        }

	        FileDataStore store = FileDataStoreFinder.getDataStore(file);
	        FeatureSource featureSource = store.getFeatureSource();
	        CachingFeatureSource cache = new CachingFeatureSource(featureSource); 
	        
	        
	        // Create a map context and add our shapefile to it
	        MapContext map = new DefaultMapContext();
	        map.setTitle("test"); 
	        map.addLayer(cache, null);
	        JMapFrame frame = new JMapFrame(map);  
	        frame.setSize(600, 600);
			frame.setLocationRelativeTo(null); 
			frame.enableStatusBar(true);
			frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
			frame.enableToolBar(true);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
			


			//JMapFrame
			/*			Point point = frame.getLocationOnScreen(); //.getLocation(); //getBounds();
			Rectangle rec = frame.getBounds(); 
			double fH = rec.getHeight();
			double fW = rec.getWidth(); 
			double fX = rec.getX();
			double fY = rec.getY();
			System.out.println(point.x + "\t" + point.y); 
			System.out.println("JMapFrame settings: \t" + fX +"\t" + fY +"\t" + fH +"\t" + fW); 

			JLayeredPane layerPane = frame.getLayeredPane(); //.getContentPane();
			Rectangle recContent = layerPane.getBounds();
			double cH = recContent.getHeight();
			double cW = recContent.getWidth(); 
			double cX = recContent.getX();
			double cY = recContent.getY();
					
			
			//JMapPane
			JMapPane mapPane = frame.getMapPane();
			Point panePoint = mapPane.getLocation(); //.getLocationOnScreen(); //getBounds();
			double pX = mapPane.getX();
			double pY = mapPane.getY();
			double pW = fW-8; //mapPane.getWidth();
			double pH = mapPane.getHeight(); 
			MapViewport view = map.getViewport(); 
			ReferencedEnvelope viewBounds = view.getBounds();
			System.out.println("JMapPane settings: \t" + pX +"\t" + pY +"\t" + pH +"\t" + pW); 
			
			

			
			//add glass pane object
			
			ReferencedEnvelope bounds = map.getAreaOfInterest(); //.getLayerBounds();
			double maxX = bounds.getMaxX();
			double minX = bounds.getMinX();
			double maxY = bounds.getMaxY();
			double minY = bounds.getMinY();
			System.out.println("old min/max on the map:\t " + minX +"\t" + maxX+"\t" + minY +"\t" + maxY); 

			double mapH = bounds.getHeight();
			double mapW = bounds.getWidth(); 
			double mapRatio = mapW/mapH;
			Coordinate centroid = bounds.centre(); 

*/
			
	        GlassPaneTest pane = new GlassPaneTest(frame);
			Coordinate coord = new Coordinate (-82.6, 27.7);
        
	        pane.addCoord(0, coord); //.setPixelLocation((int) (panePoint.x + pW/2.0) , (int) (panePoint.y + pH/2.0));
	        
	        pane.setStyle("oval", Color.red, 4);
	        frame.setGlassPane(pane);
	        pane.setVisible(true); 
	        

	        for (int i=0; i<20; i++){
	        	coord.x += (Math.random()-0.5)*.01; 
	        	coord.y +=(Math.random()-0.5)*.01;
	        	pane.repaint(); 
	        	try { Thread.currentThread().sleep((long) (.5*1000)); } catch (InterruptedException e) { e.printStackTrace(); }
	        }
	        
	}
}


