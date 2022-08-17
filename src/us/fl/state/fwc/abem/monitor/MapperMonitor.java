package us.fl.state.fwc.abem.monitor;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.ContrastMethod;

import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.params.impl.MapperMonitorParams;
import us.fl.state.fwc.util.geo.JMapFrameEmpty;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.OrganismMapper;
import us.fl.state.fwc.util.geo.SimpleMapper;

import com.vividsolutions.jts.geom.Envelope;


public class MapperMonitor extends Monitor {

	public JMapFrameEmpty frame; 
	public OrganismMapper mapPane; 

	

	
	//initialize map
	@SuppressWarnings("unchecked")
	public void initiateMap(){
		// Create a map context and add our shapefile to it
		MapContext map = new DefaultMapContext();

		map.setTitle("ABEM World"); 

		try {

		//Bathymetry
		if (MapperMonitorParams.drawBathy){
			StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
			int band = 1; //RGB band to draw
			ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
			SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);
			RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
			ChannelSelection sel = sf.channelSelection(sct);
			sym.setChannelSelection(sel);
			
			NetCDFFile bath = new NetCDFFile(MapperMonitorParams.bathFileName);
			bath.setVariables("lat", "lon", "water_depth"); 
			float[][] temp = (float[][])  bath.getArray("water_depth").copyToNDJavaArray(); 
			float missingVal = bath.getMissingValue("water_depth").floatValue(); 
			for (int i=0; i<temp.length; i++){
				for (int j=0; j<temp[i].length; j++){
					if (temp[i][j] == missingVal) temp[i][j] = 0; 
				}
			}
			float[][] bathArr = SimpleMapper.reflectArray(temp); 
			Envelope e = new Envelope(bath.getMinLon("lon"), bath.getMaxLon("lon"), bath.getMinLat("lat"), bath.getMaxLat("lat")); //.Double(); 
			ReferencedEnvelope env = new ReferencedEnvelope(e, null); //(new Rectangle2D.Double(xOrigin, yOrigin, width, height), crs); //SimpleMapper.getRasterBounds(minLon, maxLat, width, height, crs); 
			GridCoverageFactory gcf =CoverageFactoryFinder.getGridCoverageFactory(null);
			GridCoverage2D grid = gcf.create("coverage", bathArr, env);
			Style style = SLD.wrapSymbolizers(sym); 

			map.addLayer(grid, style); 
		}
		
		
		//Land
		if (MapperMonitorParams.drawLand){
			File landFile = new File(MapperMonitorParams.displayLandFilename);
			FileDataStore store = FileDataStoreFinder.getDataStore(landFile);
			FeatureSource landFSource = store.getFeatureSource();
			map.addLayer(landFSource, SLD.createPolygonStyle(new Color(97, 133, 70), new Color(97, 100, 70), .5f));
		}

		//Land
		if (MapperMonitorParams.drawGrid){
			File gridFile = new File(MapperMonitorParams.seagrassABEMGridName);
			FileDataStore store = FileDataStoreFinder.getDataStore(gridFile);
			FeatureSource gridFSource = store.getFeatureSource();
			map.addLayer(gridFSource, SLD.createPolygonStyle(new Color(102, 133, 200), new Color(102, 100, 200), .3f));
		}
		
		//Habitat
		if (MapperMonitorParams.drawHabitat){
			File habFile = new File(MapperMonitorParams.habitatFileName); 
			FileDataStore habStore = FileDataStoreFinder.getDataStore(habFile);
			FeatureSource habFSource = habStore.getFeatureSource();
			map.addLayer(habFSource, SLD.createPolygonStyle(new Color(97, 215, 70), new Color(97, 200, 70), .5f));
		}

		
		frame = new JMapFrameEmpty(map);  
		frame.setSize(MapperMonitorParams.frameWidth, MapperMonitorParams.frameHeight);
		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrameEmpty.Tool.ZOOM, JMapFrameEmpty.Tool.PAN,JMapFrameEmpty.Tool.RESET);
		frame.enableToolBar(true);
		
		mapPane = new OrganismMapper();
		mapPane.setOrganismMap(scheduler.getWorld().getOrganismMap()); 
		mapPane.setMapContext(map);
		mapPane.setRenderer(new StreamingRenderer());
		frame.addMapPane(mapPane); 
		frame.getContentPane().add(mapPane);

		frame.setVisible(true);
		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);

		} catch (IOException e1) {
			e1.printStackTrace();
		} 

	}

	/**Removes the agent from the map
	 * 
	 * @param agent
	 */
	public void remove(Organism agent){
		mapPane.remove(agent);
	}
	
	
	/**Set's an update to be called
	 * 
	 */
	public void update(){
		mapPane.setUpdate();
		if (MapperMonitorParams.displayLag > 0) try { Thread.currentThread().sleep((long) (MapperMonitorParams.displayLag*1000)); } catch (InterruptedException e) { e.printStackTrace(); }

	}
	
	
	@Override
	public void run() {
		update();
	}
	
	

		
}
