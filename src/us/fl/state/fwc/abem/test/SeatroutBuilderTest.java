package us.fl.state.fwc.abem.test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.EnviroGrid;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.abem.fishing.RecFishingTithe;
import us.fl.state.fwc.abem.monitor.AnimalBiomass;
import us.fl.state.fwc.abem.organism.Organism;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.organism.builder.SeatroutBuilder;
import us.fl.state.fwc.abem.params.Parameters;
import us.fl.state.fwc.abem.params.impl.AnimalBiomassParams;
import us.fl.state.fwc.abem.params.impl.RecFishingTitheParams;
import us.fl.state.fwc.abem.params.impl.SchedulerParams;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleMapper;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;




public class SeatroutBuilderTest {

	private static Scheduler scheduler; 
	public static final String displayLandFilename =   "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp";
	public static final String EFDCDirectory = "c:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	public static final String bathFileName = EFDCDirectory + "TB2SB_WGS_Bathy.nc";
	public static final String displayBathyFilename = bathFileName; // "C:\\work\\data\\GISData\\BaseMaps\\FloridaBaseLayers\\Bathymetry\\nearshore_fl_arc.shp";




	public static void main(String[] args) {
		SeatroutBuilderTest ini = new SeatroutBuilderTest(); 
		ini.initializeImplementations(); 
		ini.buildPopulations();
		
		ini.drawMap(); 
	}




	public void initializeImplementations(){

		scheduler = new Scheduler(); 

		// the ThingFactory is the factory method which makes new agents or returns recycled ones, instead of constantly creating and destoying agents
		OrganismFactory organFac = new OrganismFactory(scheduler); 
		scheduler.setOrganismFactory(organFac);

		// here, add a 'AgentParams()' for each agent type in model
		HashMap<String, Parameters>paramMap = new HashMap<String, Parameters>();  
		SeatroutParams params = new SeatroutParams();
		params.initialize(scheduler);
		paramMap.put("Seatrout", params);
		paramMap.put("AnimalBiomass", new AnimalBiomassParams());
		paramMap.put("RecFishingTithe", new RecFishingTitheParams());
		scheduler.setParamMap(paramMap); 
		
		AnimalBiomass fb = new AnimalBiomass(); 
		fb.initialize(scheduler); 

		RecFishingTithe rec = new RecFishingTithe();
		rec.initialize(scheduler); 

		EnviroGrid grid = new ABEMGrid(SchedulerParams.gridFilename, true);
		scheduler.setGrid(grid); 


	}

	
	public void buildPopulations() {

		SeatroutBuilder sb = new SeatroutBuilder();
		sb.setScheduler(scheduler);
		sb.build(); 

	}
	
	public void drawMap() {
		
		MapContext map = new DefaultMapContext();

		map.setTitle("ABEM World"); 

		ABEMGrid grid = (ABEMGrid) scheduler.getGrid(); 
		try {

		//Density
			//set up style
			StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
			int band = 1; //RGB band to draw
			ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
			SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);
			RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
			ChannelSelection sel = sf.channelSelection(sct);
			sym.setChannelSelection(sel);
			
			//use the 
			NetCDFFile bath = new NetCDFFile(bathFileName);
			bath.setVariables("lat", "lon", "water_depth"); 
			float[][] data = new float[bath.getSingleDimension("lat")][bath.getSingleDimension("lon")]; 
			for (int i=0; i<data.length; i++){
				for (int j=0; j<data[i].length; j++){
					data[i][j] = 0; 
				}
			}

			//set up the array of densities
			HashMap<Long, Organism> organismMap = scheduler.getWorld().getOrganismMap();
			Set<Long> keys = organismMap.keySet();
			Iterator<Long> it = keys.iterator();
			while (it.hasNext()){
				Long id = it.next();
				Organism agent = organismMap.get(id); 
				Coordinate coord = agent.getCoords();
				Int3D index = grid.getGridCell(coord).getIndex();
				if (index != null) { 
					data[index.y][index.x]--; 
				}
			}
			
			
			float[][] bathArr = SimpleMapper.reflectArray(data); 
			Envelope e = new Envelope(bath.getMinLon("lon"), bath.getMaxLon("lon"), bath.getMinLat("lat"), bath.getMaxLat("lat")); //.Double(); 
			ReferencedEnvelope env = new ReferencedEnvelope(e, null); //(new Rectangle2D.Double(xOrigin, yOrigin, width, height), crs); //SimpleMapper.getRasterBounds(minLon, maxLat, width, height, crs); 
			GridCoverageFactory gcf =CoverageFactoryFinder.getGridCoverageFactory(null);
			GridCoverage2D gridCov = gcf.create("coverage", bathArr, env);
			Style style = SLD.wrapSymbolizers(sym); 

			map.addLayer(gridCov, style); 
		
		
		//Land
			File landFile = new File(displayLandFilename);
			FileDataStore store = FileDataStoreFinder.getDataStore(landFile);
			FeatureSource landFSource = store.getFeatureSource();
			map.addLayer(landFSource, SLD.createPolygonStyle(new Color(97, 133, 70), new Color(97, 100, 70), .5f));
		

		
		JMapFrame frame = new JMapFrame(map); //.showMap(map);
		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN,JMapFrame.Tool.RESET);
		frame.enableToolBar(true);
		

		frame.setVisible(true);
		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);

		} catch (IOException e1) {
			e1.printStackTrace();
		} 

	}



}
