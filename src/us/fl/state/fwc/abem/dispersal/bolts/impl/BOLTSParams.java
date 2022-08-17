package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

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

import us.fl.state.fwc.abem.dispersal.bolts.Barrier;
import us.fl.state.fwc.abem.dispersal.bolts.BarrierPool;
import us.fl.state.fwc.abem.dispersal.bolts.HorizontalMigration;
import us.fl.state.fwc.abem.dispersal.bolts.Mortality;
import us.fl.state.fwc.abem.dispersal.bolts.Movement;
import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.Settlement;
import us.fl.state.fwc.abem.dispersal.bolts.TurbVar;
import us.fl.state.fwc.abem.dispersal.bolts.VerticalMigration;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TestingUtils;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.JMapFrameEmpty;
import us.fl.state.fwc.util.geo.MultiParticleMapper;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleMapper;

import com.vividsolutions.jts.geom.Envelope;

public class BOLTSParams {

	//===========================
	//Model timing
	//===========================
	/*	public   final int startYear = 2010;
	public   final int startMonth = 6;
	public   final int startDay = 1;
	public final int startHour = 6; 
	public final int startMin = 0; 

	public   final int endYear = 2010;
	public   final int endMonth = 6;
	public   final int endDay = 10;
	 */

	//===========================
	//Computation
	//===========================
	public final int tPoolSize = 7; //Runtime.getRuntime().availableProcessors() + 1; //this is recommendation of Goetz in JCP, and seems to run fastest versus cranking up threads 
	public final int maxYear = 2010;
	public final int maxMonth = 9;
	public final int maxDay = 30;
	public long maxTime; 
	
	//===========================
	//Release file, larvae properties
	//===========================

	public  final String releaseFile =  "output/BOLTs/release_TB.txt"; // "c:\\work\\data\\BOLTs\\SeatroutRuns\\releaseTest.txt";
	public static final String releaseShpFileName = "data/BoltsRelease3_TB.shp";
	//	public   final int releaseSpacing = 90;
	//	public   final int releaseSpUnits = Calendar.DAY_OF_YEAR;
	public final String releaseDatesFile = "c:\\work\\data\\BOLTs\\SeatroutRuns\\releaseDates.txt";
	public LinkedList<Calendar> releaseDates; 
	public   final long releaseTimeStep = 20l /*minutes:*/ * (60*1000); //release time step in milliseconds  
	public   final long compTime = 21l /*days:*/ * (24*60*60*1000); //duration of competency time in milliseconds
	public   final long preCompTime = 8l /*days:*/ * (24*60*60*1000); //duration of precompetency time in milliseconds
	public  final boolean usesEffectiveMigration = true;
	public  final boolean usesVerticalMigration = true; 
	public  final boolean usesHorizontalMigration = false; 
	public final boolean usesTurbVar = true; //turn off randomness for testing purposes
	public final double preferredInTideDepth = 0;
	public final double preferredOutTideDepth = 10; 
	public final double vertMigSpeed = 0.002; //vertical migration speed, in m/s
	public  final double mortRt = 0.5;  //daily instantaneous, Z -> average of Naples and Fakahatchee Bay from Peebles and Tolly 1988
	public final long mortRtUnits = TimeUtils.SECS_PER_DAY; 

	//===========================
	//Hydro model variables
	//===========================
	public final String EFDCDirectory = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	public final String NCOMDirectory = "c:\\work\\data\\NCOM_AS\\";
	public final String cornersInFile = EFDCDirectory + "corners.inp"; 
	public final String dxdyInFile = EFDCDirectory + "dxdy.inp";
	public final String boundCellsFilename = EFDCDirectory + "EFDCBoundCells.txt";
	public final String EFDCFileName = EFDCDirectory + "TB2SB_WGS_070110-092610.nc";
	public final String bathFileName = EFDCDirectory + "TB2SB_WGS_Bathy.nc";
	public final String landFileName = EFDCDirectory + "TB2SB_WGS_LandMaskForShapefileBarrier.nc";



	public double minLon = -83.1;
	public double maxLon = -82.1;
	public double minLat = 27;
	public double maxLat = 28.22; 

	//===========================
	//Landscape variables (habitat, land mask, bathymetry
	//===========================
	public  final boolean negPolyCoord = true; //Does the shapefile layer use negative coordinates? 
	public  final boolean negOceanCoord = true;// Does the oceanographic model have negative coordinate values?
	private  boolean negLandCoord = true;			// Does the landmask use negative coordinates?
	public final String habitatFileName =  "C:\\work\\data\\GISData\\Habitat\\Seagrass\\2008\\JustSeagrass2008_TBClip.shp";// Name of the settlement polygon file
	public final String habitatKey = "OBJECTID"; // Index field of the settlement polygon file
	public final String habitatLookup = "OBJECTID"; // Field used in the release file to identify the source population

	public boolean useGridBarrier = true; 
	public final  String gridLandFileName = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\TB2SB_WGS_LandMaskForGridBarrier.nc"; //"c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp"; // Name of the land mask file

	public ArrayList<Integer> landMaskListXs; //a set that holds just the x index values for faster check
	public HashMap<Int3D, Integer> landMaskList; 
	public final String maskFile = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\maskExpanded.inp";

	public final  String shpLandFileName = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp"; // Name of the land mask file
	public final  String landKey = "FID";
	public final  String landLookup = "FID";

	public  final int landPoolSize = 100; 

	//public final String bathymetryFileName = "c:\\work\\data\\BOLTs\\USGSBathWGSClip.nc";
	public final boolean negBathymCoord = true;
	public final String bathVarName = "water_depth";
	public final String latVarName = "lat";
	public final String lonVarName = "lon"; 


	//===========================
	//Interface declarations
	//===========================
	private BarrierPool landPool; 
	private Mortality mort; 
	private VerticalMigration vertMigr;
	private Settlement settle;
	private Movement move;
	private TurbVar turbVar;
	private HorizontalMigration horMigr; 


	//===========================
	//Output variables
	//===========================

	public boolean outputParticleSteps = false; 
	public  final String outputFolder = "c:\\work\\data\\BOLTs\\SeatroutRuns\\output\\";
	private SettleSumWriter settleSumWrite; 
	private SettleAvgPLDWriter avgPLDWrite; 
	private TrajectoryWriter trajWrite; 
	private SettlementWriter settlementWrite; 

	public boolean writeSettleSums = false; 
	public  boolean writeSettleLocs = false;
	public  boolean writeTrajs = false; 
	public boolean writeSettleAvgPLD = true;

	public  final long outputFreq = /*minutes:*/ 60   *(60*1000); //amount of time in ms when output will be written


	//===========================
	//Display variables
	//===========================
	public boolean drawParticles = false; 
	//	public boolean drawWithGlassPane = true; 
	//	public boolean drawPreviousPoint = true;
	//	public boolean drawWithStyle = true;

	public boolean drawLand = true;
	public boolean drawHabitat = true;
	public boolean drawEFDCGrid = false;
	public boolean drawBathy = true; 
	public boolean drawLandMask = true; 
	public double displayLag = 0.0; 

	public JMapFrameEmpty frame; 
	//public ParticleMap mapPane; 
	public MultiParticleMapper mapPane; 
	private ConcurrentHashMap<Long, Particle> particleMap; 
	//double mapLag = 0.01; 
	String displayLandFilename =  shpLandFileName;
	String displayBathyFilename = bathFileName; // "C:\\work\\data\\GISData\\BaseMaps\\FloridaBaseLayers\\Bathymetry\\nearshore_fl_arc.shp";
	String displayGridFilename = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\EFDC_TampaToSarasota_WGS_Grid.shp";
	String displayLandMaskFilename = gridLandFileName; //"c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\EFDC_TampaToSarasota_WGS_Grid.shp";


	//===========================
	//Date declarations
	//===========================
	//public Calendar startRelease = new GregorianCalendar(startYear, startMonth-1, startDay, startHour, startMin); 
	//public Calendar endRelease = new GregorianCalendar(endYear, endMonth-1, endDay); 


	//TODO -- add in all the supporting classes (RKMove, TurbVel, a writer

	public BOLTSParams(String readFromFileName) {

		Calendar maxDate = new GregorianCalendar(maxYear, maxMonth-1, maxDay);
		maxDate.setTimeZone(TimeZone.getTimeZone("GMT"));
		maxTime = maxDate.getTimeInMillis();
		
		setReleaseDates(releaseDatesFile); 

		if (drawParticles)
			try {
				initializeMap();
			} catch (IOException e1) {
				e1.printStackTrace();
			} 

			if (readFromFileName == null){

				//	startRelease.setTimeZone(TimeZone.getTimeZone("GMT")); 
				//	endRelease.setTimeZone(TimeZone.getTimeZone("GMT")); 

				//|||||||||||||||||||||||||||||||||||||||||||||| Set up implementing classes |||||||||||||||||||||||||||||||||||||||||||||| 
				try {
					/*				ShapefileBarrier landmask = new ShapefileBarrier();
				landmask.setDataSource(shpLandFileName);
				landmask.setLookupField(landKey);
				landmask.setNegLon(negLandCoord);
					 */	
					GridBarrier landmask = new GridBarrier();
					landmask.setDataSource(gridLandFileName);
					landmask.setNegLon(negLandCoord);
					landmask.setLandVal((short) 1);
					setLandMaskSets(maskFile);
					landmask.setLandMaskList(this.landMaskList);
					landmask.setLandMaskSet(this.landMaskListXs);
					this.useGridBarrier = true; 

					landPool = new BarrierPool(landmask,landPoolSize);


					TBRateMortality mort = new TBRateMortality( mortRt/ ((double) (mortRtUnits*1000)/ (double) releaseTimeStep));
					this.mort = mort; 

					//set up the shapefile habitat used for the settlement
					ShapefileHabitat sh = new ShapefileHabitat();
					sh.setDataSource(habitatFileName);
					sh.setLookupField(habitatKey);
					sh.setNegLon(negPolyCoord);

					TBSimpleSettlement ssm = new TBSimpleSettlement();
					ssm.setSettlementPolys(sh);
					ssm.setPolyCheckStart(preCompTime);
					settle = ssm;

					TBNestedNetCDFVelocityReader nnvr = new TBNestedNetCDFVelocityReader();
					nnvr.initialize(); 
					nnvr.setNegOceanCoord(negOceanCoord);
					nnvr.setNegPolyCoord(negPolyCoord);
					//nnvr.setLandFile(landFileName);

					RK4Movement rk4 = new RK4Movement();
					rk4.setVr(nnvr);
					rk4.setH(releaseTimeStep);
					move = rk4;

					SimpleTurbVar var = new SimpleTurbVar(releaseTimeStep);
					var.setH(releaseTimeStep); 
					turbVar = var; 

					SeatroutVerticalMigration tb = new SeatroutVerticalMigration(this, bathFileName); 
					tb.setPreferredIncomingDepth(this.preferredInTideDepth);
					tb.setPreferredOutgoingDepth(this.preferredOutTideDepth); 
					tb.setVertMigSpeed(this.vertMigSpeed);
					//TBTextVerticalMigration tb = new TBTextVerticalMigration(this, bathFileName); 
					vertMigr = tb;

					

				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("catching error " + e);
					TestingUtils.dropBreadCrumb(); 
				}

			}
			else {
				readFromFile(readFromFileName); 
			}
	}



	public void setReleaseDates(String releaseDatesFile){

		releaseDates = new LinkedList<Calendar>(); 
		
		File file = new File(releaseDatesFile); 
		BufferedReader reader; 

		try {
			reader = new BufferedReader(new FileReader(file));

			//loop over all release groups in the release.txt file
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t");
				String dateString = tokens[0];
				String dateTokens[] = dateString.split("/"); 
				int month = Integer.parseInt(dateTokens[0]); 
				int day = Integer.parseInt(dateTokens[1]); 
				int year = Integer.parseInt(dateTokens[2]) ;

				String timeString = tokens[1]; 
				String timeTokens[] = timeString.split(":");
				int hour = Integer.parseInt(timeTokens[0]);
				int min = Integer.parseInt(timeTokens[1]); 
				
				GregorianCalendar releaseDate = new GregorianCalendar(year, month-1, day, hour, min); 
				releaseDate.setTimeZone(TimeZone.getTimeZone("GMT"));
				releaseDates.add(releaseDate); 
				
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}

	}



	public void setLandMaskSets(String maskFile) throws IOException{
		//read in the mask.inp, and store each x inde	
		//here, just figure out what they are and input directly, versus reading in mask.inp since will be
		//different depending on how the greater model area is set

		landMaskListXs = new ArrayList<Integer>(); //a set that holds just the x index values for faster check

		landMaskList = new HashMap<Int3D, Integer>(); 

		File file = new File(maskFile); 

		BufferedReader reader = new BufferedReader(new FileReader(file));

		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

			int i1 = Integer.parseInt(tokens[0]);
			int j1 = Integer.parseInt(tokens[1]);
			int type = Integer.parseInt(tokens[2]); 


			int i2 = 0; 
			@SuppressWarnings("unused")
			int j2 = 0; 

			//if type=1, then is a western mask, so add the cell to the west
			if (type == 1){
				i2 = i1-1;
				j2 = j1;

				landMaskListXs.add(i1);
				landMaskListXs.add(i2);
				Int3D index1 = new Int3D(i1, j1, 0);
				landMaskList.put(index1, type); 

			}

			//if type=2, then is a southern mask, so add the cell to the south
			else if (type == 2){
				i2 = i1;
				j2 = j1-1;

				landMaskListXs.add(i1);
				landMaskListXs.add(i2);
				Int3D index1 = new Int3D(i1, j1, 0);
				landMaskList.put(index1, type); 
			}

			//if type=3, then is BOTH a western  and southern mask, so add both cells to west and south
			else if (type == 3){
				i2 = i1-1;
				j2 = j1;

				landMaskListXs.add(i1);
				landMaskListXs.add(i2);
				Int3D index1 = new Int3D(i1, j1, 0);
				landMaskList.put(index1, type); 

				i2 = i1;
				j2 = j1-1;

				landMaskListXs.add(i1);
				landMaskListXs.add(i2);
			}




		}//end for loop


	}




	@SuppressWarnings("unchecked")
	public void initializeMap() throws IOException{
		// Create a map context and add our shapefile to it
		MapContext map = new DefaultMapContext();

		map.setTitle("BOLTs Mapper"); 

		particleMap = new ConcurrentHashMap<Long, Particle>(); 


		//CoordinateReferenceSystem crs = null; 

		//Bathymetry
		if (this.drawBathy){
			StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
			int band = 1; //RGB band to draw
			ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
			SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);
			RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
			ChannelSelection sel = sf.channelSelection(sct);
			sym.setChannelSelection(sel);

			NetCDFFile bath = new NetCDFFile(this.bathFileName); 
			bath.setVariables(latVarName, lonVarName, bathVarName); 
			float[][] temp = (float[][])  bath.getArray(this.bathVarName).copyToNDJavaArray(); 
			float missingVal = bath.getMissingValue(bathVarName).floatValue(); 
			for (int i=0; i<temp.length; i++){
				for (int j=0; j<temp[i].length; j++){
					if (temp[i][j] == missingVal) temp[i][j] = 0; 
				}
			}
			float[][] bathArr = SimpleMapper.reflectArray(temp); 
			Envelope e = new Envelope(bath.getMinLon(lonVarName), bath.getMaxLon(lonVarName), bath.getMinLat(latVarName), bath.getMaxLat(latVarName)); //.Double(); 
			ReferencedEnvelope env = new ReferencedEnvelope(e, null); //(new Rectangle2D.Double(xOrigin, yOrigin, width, height), crs); //SimpleMapper.getRasterBounds(minLon, maxLat, width, height, crs); 
			GridCoverageFactory gcf =CoverageFactoryFinder.getGridCoverageFactory(null);
			GridCoverage2D grid = gcf.create("coverage", bathArr, env);
			Style style = SLD.wrapSymbolizers(sym); 

			map.addLayer(grid, style); 

		}



		//Land
		if (this.drawLand){
			File landFile = new File(displayLandFilename);
			FileDataStore store = FileDataStoreFinder.getDataStore(landFile);
			FeatureSource landFSource = store.getFeatureSource();
			//CachingFeatureSource landCache = new CachingFeatureSource(featureSource);
			map.addLayer(landFSource, SLD.createPolygonStyle(new Color(97, 133, 70), new Color(97, 100, 70), .5f));
			//crs = map.getCoordinateReferenceSystem(); 
		}

		//Habitat
		if (this.drawHabitat){
			File habFile = new File(habitatFileName); 
			FileDataStore habStore = FileDataStoreFinder.getDataStore(habFile);
			FeatureSource habFSource = habStore.getFeatureSource();
			map.addLayer(habFSource, SLD.createPolygonStyle(new Color(97, 215, 70), new Color(97, 200, 70), .5f));
		}


		//EFDC Grid
		if (this.drawEFDCGrid){
			File gridFile = new File(displayGridFilename); 
			FileDataStore gridStore = FileDataStoreFinder.getDataStore(gridFile);
			FeatureSource gridFSource = gridStore.getFeatureSource();
			map.addLayer(gridFSource, SLD.createPolygonStyle(new Color(40, 60, 255), new Color(40, 60, 255), .2f));
		}






		frame = new JMapFrameEmpty(map);  
		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrameEmpty.Tool.ZOOM, JMapFrameEmpty.Tool.PAN,JMapFrameEmpty.Tool.RESET);
		frame.enableToolBar(true);

		//mapPane = new ParticleMap();
		mapPane = new MultiParticleMapper();
		mapPane.setParticleMap(particleMap); 
		mapPane.setMapContext(map);
		mapPane.setRenderer(new StreamingRenderer());
		frame.addMapPane(mapPane); 
		frame.getContentPane().add(mapPane);

		frame.setVisible(true);
		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);


		/*		GlassPaneParticleMap partMap = new GlassPaneParticleMap(particleMap, frame);
			if (this.drawPreviousPoint) partMap.setPrintPreviousPoint(true);
			partMap.setStyle("circle", Color.red, 4);
			frame.setGlassPane(partMap);
			partMap.setVisible(true); 
		 */		
	}

	public synchronized void addToMap(Particle p){
		particleMap.put(p.getID(), p);	
	}

	public synchronized void removeFromMap(Particle p){
		mapPane.remove(p);
	}


	@SuppressWarnings("static-access")
	public synchronized void updateMap(Particle p){

		//mapPane.update(p);
		mapPane.setUpdate(); 
		//frame.getGlassPane().repaint(); 
		try { Thread.currentThread().sleep((long) (0.05*1000)); } catch (InterruptedException e) { e.printStackTrace(); }
	}
	



	public void readFromFile(String readFromFileName){
		//TODO -- set up so can set all parameters from a file
		//follow JKool's approach but have the Field reader working since not through interface
	}

	/**Returns one of the multiple Barriers in the BarrierPool, 
	 * where the index value to return from the pool changes after
	 * each call to BarrierPool.getBarrier()
	 * @return
	 */
	public Barrier getLandMask() {
		return landPool.getBarrier();
	}


	/**Returns an singleton implementation of the Mortality class which
	 * uses a synchronized apply() method
	 * @return
	 */
	public Mortality getMort() {
		return mort;
	}

	public VerticalMigration getVertMigrClone() {
		return vertMigr.clone();
	}

	public HorizontalMigration getHorMigrClone() {
		return horMigr.clone();
	}

	/**Returns an singleton implementation of the Settlement class which
	 * uses a synchronized apply() method
	 * @return
	 */
	public Settlement getSettle() {
		return settle;
	}


	public Movement getMoveClone() {
		return move.clone();
	}


	public TurbVar getTurbVarClone() {
		return turbVar.clone();
	}


	public SettlementWriter getSettlementWrite() {
		return settlementWrite;
	}


	public void setSettlementWrite(SettlementWriter settlementWrite) {
		this.settlementWrite = settlementWrite;
	}

	public SettleSumWriter getSettleSumWrite() {
		return settleSumWrite;
	}


	public void setSettleSumWrite(SettleSumWriter settleSumWrite) {
		this.settleSumWrite = settleSumWrite;
	}

	public void setSettleAvgPLDWriter(SettleAvgPLDWriter pld) {
		this.avgPLDWrite = pld;
	}

	public SettleAvgPLDWriter getSettleAvgPLDWriter(){
		return this.avgPLDWrite;
	}

	public TrajectoryWriter getTrajWrite() {
		return trajWrite;
	}


	public void setTrajWrite(TrajectoryWriter trajWrite) {
		this.trajWrite = trajWrite;
	}



	public void closeConnections() {
		landPool.closeConnections(); 
		vertMigr.closeConnections();
		if (this.usesHorizontalMigration) horMigr.closeConnections();
		move.closeConnections();
	}








}
