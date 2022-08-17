package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;

public class ParticleMapTest {


    // Main application method
    @SuppressWarnings({ "unchecked", "static-access" })
	public static void main(String[] args) throws Exception {

    	ConcurrentHashMap<Long, Particle> particleMap = new ConcurrentHashMap<Long, Particle>(); 
    	
    	String shpLandFileName = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp"; // Name of the land mask file
   
        MapContext map = new DefaultMapContext();

        File file = new File(shpLandFileName);
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        FeatureSource featureSource = store.getFeatureSource();

        // Create a map context and add our shapefile to it
        Style style = SLD.createPolygonStyle(Color.BLACK, Color.green, 0.5F);
        map.addLayer(featureSource, style);

    	JMapFrameEmpty frame = new JMapFrameEmpty(map);
        MultiParticleMapper mapPane = new MultiParticleMapper();
        mapPane.setParticleMap(particleMap);
        mapPane.setMapContext(map);
        mapPane.setRenderer(new StreamingRenderer());
        //mapPane.setLatch(0); //initialize a latch and set to 0 so will start up right away
        
        frame.addMapPane(mapPane); 
        
        frame.getContentPane().add(mapPane);
        frame.setSize(500, 800);
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrameEmpty.Tool.ZOOM);
		//frame.enableToolBar(true);


        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
       // mapPane.setVisible(true);

        Particle p = new Particle(null); 
        p.setX(-82.6);
        p.setY(27.6);
        p.setPX(-82.61);
        p.setPY(27.61);
        p.setID(0l);
        particleMap.put(p.getID(), p);

        Particle p2 = new Particle(null); 
        p2.setX(-82.6);
        p2.setY(27.6);
        p2.setPX(-82.61);
        p2.setPY(27.61);
        p2.setID(1l);
        particleMap.put(p2.getID(), p2);

        
        try { Thread.currentThread().sleep((long) (3*1000)); } catch (InterruptedException e) { e.printStackTrace(); }
        mapPane.setUpdate(); //.update();
        
        while (true){
        	try { Thread.currentThread().sleep((long) (0.3*1000)); } catch (InterruptedException e) { e.printStackTrace(); }
        	p.setPX(p.getX());
        	p.setPY(p.getY());
        	p.setX(p.getX()+Math.random()*.01-.005);
        	p.setY(p.getY()+Math.random()*.01-.005);
 
           	p2.setPX(p2.getX());
        	p2.setPY(p2.getY());
        	p2.setX(p2.getX()+Math.random()*.01-.005);
        	p2.setY(p2.getY()+Math.random()*.01-.005);
 
        	mapPane.setUpdate(); //.update();
        	
        	System.out.println("particles stepping"); 
        	
        }

    
    }
}
