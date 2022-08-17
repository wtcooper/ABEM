package us.fl.state.fwc.util.geo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.referencing.CRS;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JMapPane;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.ContrastMethod;

import us.fl.state.fwc.util.DeleteFiles;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SimpleMapper {

	int frameWidth=800, frameHeight=600;
	protected String SRS_UTM17N = "EPSG:26917"; 
	String title = "map"; 
	JMapFrame frame; 
	StyleFactory sf;
	FilterFactory2 ff;
	boolean enableLayers = false;


	private HashMap<String, SimpleShapefile> shpMap; // = new HashMap<String, SimpleShapefile>(); 


	public SimpleMapper() {
	}

	public SimpleMapper(String title, int frameWidth, int frameHeight) {
		this.title = title; 
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight; 
	}


	/**Maps multiple shapefiles in simple viewer
	 * Note: when creating point style, pass it a 'wellKnownName' one of: Circle, Square, Cross, X, Triangle or Star
	 * @param mapTitle
	 * @param mapLayers
	 * @param mapStyles 
	 */
	public JMapFrame  drawShapefileLayers(String[] mapLayers, Style[] mapStyles){
		MapContext map = new DefaultMapContext();
		map.setTitle(title);

		if (enableLayers){
//			SimpleShapefile baseMap = new SimpleShapefile(mapLayers[0]);
//			baseMap.openShapefile(); //.initialize(mapLayers[i], false); 
//			if ((mapStyles == null) || (mapStyles[0] == null)) map.addLayer(baseMap.getFeatureSource(), null);
//			else  map.addLayer(baseMap.getFeatureSource(), mapStyles[0]);
			
			for (int i = 0; i<mapLayers.length; i++){
				SimpleShapefile baseMap = new SimpleShapefile(mapLayers[i]);
				baseMap.openShapefile(); //.initialize(mapLayers[i], false); 
				if ((mapStyles == null) || (mapStyles[i] == null)) map.addLayer(baseMap.getFeatureSource(), null);
				else  map.addLayer(baseMap.getFeatureSource(), mapStyles[i]);
			}
		}
		else {
			for (int i = 0; i<mapLayers.length; i++){
				SimpleShapefile baseMap = new SimpleShapefile(mapLayers[i]);
				baseMap.openShapefile(); //.initialize(mapLayers[i], false); 
				if ((mapStyles == null) || (mapStyles[i] == null)) map.addLayer(baseMap.getFeatureSource(), null);
				else  map.addLayer(baseMap.getFeatureSource(), mapStyles[i]);
			}
		}

		frame = new JMapFrame(map); //.showMap(map);
		frame.setSize(frameWidth, frameHeight);
		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
		frame.enableToolBar(true);
		frame.enableLayerTable(enableLayers); 
		frame.setVisible(true);

		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);

		if (enableLayers){

			

			repaint();
		}
		return frame; 
	}





	/**Maps raster arrays 
	 * 
	 * @param rasterArrays -- here, the first array dimension is the number of raster arrays
	 * @param rasterBounds
	 * @param rasterStyles
	 * @return
	 */
	public JMapFrame  drawRasterLayers(ArrayList<float[][]> rasterArrays, ReferencedEnvelope[] rasterBounds, Style[] rasterStyles){
		//instantiate the factories
		sf = CommonFactoryFinder.getStyleFactory(null);
		ff = CommonFactoryFinder.getFilterFactory2(null);

		MapContext map = new DefaultMapContext();
		map.setTitle(title);

		GridCoverageFactory gcf =CoverageFactoryFinder.getGridCoverageFactory(null);
		for (int i = 0; i<rasterArrays.size(); i++){
			GridCoverage2D grid = gcf.create("coverage", rasterArrays.get(i), rasterBounds[i]);
			Style style = null; 
			if (rasterStyles == null) style = createGreyscaleStyle(1);
			else style = rasterStyles[i]; 
			map.addLayer(grid, style); 
		}

		frame = new JMapFrame(map); //.showMap(map);
		frame.setSize(frameWidth, frameHeight);

		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
		frame.enableToolBar(true);
		frame.setVisible(true);
		frame.enableLayerTable(enableLayers); 

		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
		return frame; 
	}



	/**Maps a single raster array 
	 * 
	 * @param rasterArray -- here, the first array dimension is the number of raster arrays
	 * @param rasterBound
	 * @param rasterStyle
	 * @return
	 */
	public JMapFrame  drawRasterLayer(float[][] rasterArray, ReferencedEnvelope rasterBound, Style rasterStyle){
		//instantiate the factories
		sf = CommonFactoryFinder.getStyleFactory(null);
		ff = CommonFactoryFinder.getFilterFactory2(null);

		final MapContext map = new DefaultMapContext();
		map.setTitle(title);

		GridCoverageFactory gcf =CoverageFactoryFinder.getGridCoverageFactory(null);
		GridCoverage2D grid = gcf.create("coverage", rasterArray, rasterBound);
		Style style = null; 
		if (rasterStyle == null) style = createGreyscaleStyle(1);
		else style = rasterStyle; 
		map.addLayer(grid, style); 

		frame = new JMapFrame(map); //.showMap(map);
		frame.setSize(frameWidth, frameHeight);
		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
		frame.enableToolBar(true);
		frame.setVisible(true);
		frame.enableLayerTable(enableLayers); 

		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
		return frame; 
	}

	/**Maps multiple shapefiles and raster data arrays in simple viewer
	 * Note: when creating point style, pass it a 'wellKnownName' one of: Circle, Square, Cross, X, Triangle or Star
	 * 
	 * @param shapefileLayers
	 * @param shapefileStyles
	 * @param rasterArrays
	 * @param rasterBounds
	 * @param rasterStyles
	 * @return
	 */
	public JMapFrame  drawMixedLayers(String[] shapefileLayers, Style[] shapefileStyles, ArrayList<float[][]>  rasterArrays, ReferencedEnvelope[] rasterBounds, Style[] rasterStyles){
		//instantiate the factories
		sf = CommonFactoryFinder.getStyleFactory(null);
		ff = CommonFactoryFinder.getFilterFactory2(null);

		MapContext map = new DefaultMapContext();
		map.setTitle(title);

		GridCoverageFactory gcf =CoverageFactoryFinder.getGridCoverageFactory(null);
		for (int i = 0; i<rasterArrays.size(); i++){
			GridCoverage2D grid = gcf.create("coverage", rasterArrays.get(i), rasterBounds[i]);
			Style style = null; 
			if (rasterStyles == null) style = createGreyscaleStyle(1);
			else style = rasterStyles[i]; 
			map.addLayer(grid, style); 
		}


		for (int i = 0; i<shapefileLayers.length; i++){
			SimpleShapefile baseMap = new SimpleShapefile(shapefileLayers[i]);
			baseMap.openShapefile(); //.initialize(mapLayers[i], false); 
			if (!(shapefileStyles == null)) map.addLayer(baseMap.getFeatureSource(), shapefileStyles[i]);
			else map.addLayer(baseMap.getFeatureSource(), null); 
		}



		frame = new JMapFrame(map); //.showMap(map);
		frame.setSize(frameWidth, frameHeight);

		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
		frame.enableToolBar(true);
		frame.setVisible(true);
		frame.enableLayerTable(enableLayers); 

		frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
		return frame; 
	}






	/**Repaints the frame
	 * 
	 */
	public void repaint(){
		frame.repaint();
	}


	/**Converts an integer double array to float double array
	 * 
	 * @param a
	 * @return
	 */
	public static float[][] int2float(int[][] a){
		float[][] b = new float[a.length][a[0].length];
		for (int i=0; i<a.length; i++){
			for (int j=0; j<a[0].length; j++){
				b[i][j] = (float) a[i][j];
			}
		}
		return b;
	}

	/**Reflects an array on the horizontal axis
	 * 
	 * @param array
	 * @return refelcted array
	 */
	public static int[][] reflectArray(int[][] array){
		int[][] temp = new int[array.length][array[0].length]; 
		for (int i=0; i<array.length; i++){
			for (int j=0; j<array[0].length; j++){
				temp[i][j] = array[(array.length-1)-i][j];
			}
		}
		return temp;
	}


	/**Reflects an array on the horizontal axis
	 * 
	 * @param array
	 * @return refelcted array
	 */
	public static float[][] reflectArray(float[][] array){
		float[][] temp = new float[array.length][array[0].length]; 
		for (int i=0; i<array.length; i++){
			for (int j=0; j<array[0].length; j++){
				temp[i][j] = array[(array.length-1)-i][j];
			}
		}
		return temp;
	}



	/**Initiates a shutdown hook to delete the given files (using a deleteByPrefix so will remove all other associated shapefiles)
	 * 
	 * @param filenames
	 */
	public void deleteFiles(final String... filenames){
		//example of adding a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {

				DeleteFiles delete = new DeleteFiles();
				delete.deleteByPrefix(filenames);
				System.out.println("deleted temp shapefiles"); 
				// while(true);
			}
		}); 
	}

	/**Adds a lag time in seconds
	 * 
	 * @param secs
	 */
	@SuppressWarnings("static-access")
	public void addLag(double secs){
		try { Thread.currentThread().sleep((long) (secs*1000)); } catch (InterruptedException e) { e.printStackTrace(); }
	}


	/** Returns the JMapFrame of the current JMapContext
	 * 
	 * @return
	 */
	public JMapFrame getFrame() {
		return frame;
	}


	/**Output a JPEG of the current JMapFrame
	 * 
	 * @param filename
	 */
	public void outputJPEG(String filename){
		JMapPane mapPane = frame.getMapPane();
		Rectangle paintArea = mapPane.getVisibleRect(); 

		BufferedImage image = new BufferedImage( 
				paintArea.width, paintArea.height, 
				BufferedImage.TYPE_INT_RGB); 

		Graphics2D gr = image.createGraphics(); 

		gr.setComposite(AlphaComposite.Src); 
		gr.setPaint(Color.WHITE); 
		gr.fill(paintArea); 

		mapPane.getRenderer().paint(gr, 
				mapPane.getVisibleRect(), mapPane.getDisplayArea(), 
				mapPane.getWorldToScreenTransform()); 

		File fileToSave = new File(filename); 

		try { 
			ImageIO.write(image, "jpeg", fileToSave); 
		} catch (IOException ex) { 
			// bummer... 
		} finally { 
			gr.dispose(); 
		} 
	}

	/** Creates a greyscale style for a raster display
	 * 
	 * @param band
	 * @return Style
	 */
	public static Style createGreyscaleStyle(int band) {
		StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

		ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
		SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);

		RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
		ChannelSelection sel = sf.channelSelection(sct);
		sym.setChannelSelection(sel);

		return SLD.wrapSymbolizers(sym);
	}


	/** Returns a ReferencedEnvelope given the bounds.  If crs=null, will set to the default CRS (UTM zone 17N)
	 * 
	 * @param xOrigin -- upper left
	 * @param yOrigin -- upper left
	 * @param width
	 * @param height
	 * @param crs
	 * @return ReferencedEnvelope
	 */
	public static ReferencedEnvelope getRasterBounds(int xOrigin, int yOrigin, int width, int height, CoordinateReferenceSystem crs ){
		if (crs == null) {
			try {
				crs = CRS.decode("EPSG:4326");
			} catch (NoSuchAuthorityCodeException e) {
				e.printStackTrace();
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		}
		return new ReferencedEnvelope(new Rectangle2D.Double(xOrigin, yOrigin, width, height), crs);
	}



	/** Returns a ReferencedEnvelope given the bounds.  If crs=null, will set to the default CRS (UTM zone 17N)
	 * 
	 * @param xOrigin -- upper left
	 * @param yOrigin -- upper left
	 * @param width
	 * @param height
	 * @param crs
	 * @return ReferencedEnvelope
	 */
	public static ReferencedEnvelope getRasterBounds(double xOrigin, double yOrigin, double width, double height, CoordinateReferenceSystem crs ){
		if (crs == null) {
			try {
				crs = CRS.decode("EPSG:4326");
			} catch (NoSuchAuthorityCodeException e) {
				e.printStackTrace();
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		}
		return new ReferencedEnvelope(new Rectangle2D.Double(xOrigin, yOrigin, width, height), crs);
	}



	/**Gets the SimpleShapefile instance for the given name
	 * 
	 * @param shpName
	 * @return
	 */
	public SimpleShapefile getShp(String shpName){
		return shpMap.get(shpName);
	}


	/**Adds the SimpleShapefile to the shapefile map
	 * 
	 * @param shpName
	 * @param shp
	 */
	public void addShp2Map(String shpName, SimpleShapefile shp){
		//lazy constructor
		if (shpMap == null){
			shpMap = new HashMap<String, SimpleShapefile>(); 
		}

		shpMap.put(shpName, shp);
	}



	/**Enables the layers table (which doesn't work anyways...)
	 * 
	 * @param enable
	 */
	public void enableLayers(boolean enable){
		if (enable == true) enableLayers = true;
	}














	// Example code for using both methods

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		//create a simple polygon which represents the boundary of the display
		//ArrayList<Class> attributeTypes = new ArrayList<Class>(); 
		//attributeTypes.add(Polygon.class); 
		//Class[] attributeTypes = {Polygon.class}; 
		//String[] attributeNames = {"polygon"}; 
		//Coordinate[][] polyCoords = {  {new Coordinate(332337,3053046), new Coordinate(333337,3053046), new Coordinate(333337,3054046), new Coordinate(332337,3054046), new Coordinate(332337,3053046)}  }; 
		//		Object[][] attributeValues = {  {new String("my square")} }; 
		//Shapefile box = new Shapefile("dataTest/box.shp", false);
		//box.createShapefile(attributeTypes, attributeNames, polyCoords, null);
		//box.closeTransaction(); //close after done creating

		//Alternate formulation w/o option for attributes attributes
		final String boxFilename = "dataTest/box.shp";
		Shapefile box = new Shapefile(boxFilename, false);
		Coordinate[][] polyCoords = {  {
			new Coordinate(332337,3053046), 
			new Coordinate(333337,3053046), 
			new Coordinate(333337,3054046), 
			new Coordinate(332337,3054046), 
			new Coordinate(332337,3053046)}  }; 
		box.createShapefile(Polygon.class, polyCoords);


		SimpleMapper map = new SimpleMapper("map test", 1000, 800); 

		String[] baseMaps = {"dataTest/TBLandClip3UTM.shp", "dataTest/box.shp"};
		String newMap = "dataTest/Test1.shp";
		Coordinate coord1 = new Coordinate(332337, 3053046); 
		Coordinate coord2 = new Coordinate(320338, 3053047); 
		Coordinate coord3 = new Coordinate(310338, 3053047); 
		Coordinate coord4 = new Coordinate(300338, 3053047); 
		Coordinate[] coords = {coord1, coord2, coord3, coord4}; 
		String[] names = {"point1", "point2", "point3", "point4" }; 
		Class[] attribTypes = {Point.class, String.class}; 
		//	ArrayList<Class> attribTypes = new ArrayList<Class>(); 
		//attribTypes.add(Point.class); 
		//attribTypes.add(String.class); 
		String[] attribNames = {"location", "names"}; 


		Style floridaStyle = SLD.createPolygonStyle(new Color(0, 125, 0), new Color(0, 125, 0), .5f);
		Style boxStyle = SLD.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), 1f);
		Style[] baseMapStyles = {floridaStyle, boxStyle};
		Style newMapStyle = SLD.createPointStyle("circle", new Color(150,0,150), new Color(150,0,150), 1.0f, 5f); 
		map.animatePoints(baseMaps, baseMapStyles, newMap, newMapStyle, attribTypes, attribNames, coords, names, 3);
		//map.drawLayers(baseMaps, baseMapStyles); 

		map.deleteFiles(boxFilename, newMap);

		//JMapFrame frame = map.mapShapefiles(baseMaps, baseMapStyles);

		/*		GenerateSVG genSVG = new GenerateSVG();
		MapContext mapContext =frame.getMapContext();

		Envelope env = new Envelope(330337, 338337, 3050046, 3056046); 
		FileOutputStream fout;
		try {
			fout = new FileOutputStream("dataTest/testOutput.svg");
			genSVG.go(mapContext, mapContext.getLayerBounds(), fout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */


		//another example usage from RKMoveTester.java
		/*		Toolkit toolkit =  Toolkit.getDefaultToolkit ();
        Dimension dim = toolkit.getScreenSize();
		SimpleMapper map = new SimpleMapper("map test", dim.width-100, dim.height-100); 
		String[] layers = {pointsShpName, vectorsShpName, gridShpName};
		Style[] layerStyles = {	//SLD.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), .5f),
				//SLD.createPolygonStyle(null, new Color(255, 0, 255), 0.01f), 
				SLD.createPointStyle("circle", new Color(150,0,150), new Color(150,0,150), 1.0f, 5f),
				SLD.createLineStyle(new Color(150,0,150), 1f),
				SLD.createLineStyle(new Color(0, 0, 0), 1f)};
		ArrayList<float[][]> rasters = new ArrayList<float[][]>();
		rasters.add(map.int2float(map.reflectArray(landGrid))); 
		map.drawMixedLayers(layers, layerStyles, rasters, new ReferencedEnvelope[]{map.getRasterBounds(0, 0, gridXDim*gridXSize, gridYDim*gridYSize, null)}, null); 
//		map.drawShapefileLayers(layers, layerStyles);

		 */ 

	}

	@SuppressWarnings({ "unchecked", "static-access" })
	public JMapFrame  animatePoints(String[] baseMapLayers, Style[] baseMapStyles, String newLayer, Style newMapStyle, Class[] /*ArrayList<Class>*/ attributeTypes, String[] attributeNames, Coordinate[] newShpCoords, String[] newShpNames, int animationRt){

		File newShp = new File(newLayer); 
		if (newShp.exists()){

		}

		MapContext map = new DefaultMapContext();
		map.setTitle(title);

		//display the basemaps
		for (int i = 0; i<baseMapLayers.length; i++){
			Shapefile baseMap = new Shapefile(baseMapLayers[i], false);
			baseMap.openShapefile();  
			map.addLayer(baseMap.getFeatureSource(), baseMapStyles[i]);
		}


		//create a new shapefile that will hold the points to animate
		Shapefile newShpfile = new Shapefile(newLayer, true); 
		newShpfile.createShapefile(attributeTypes, attributeNames, null, null); // create an empty shapefile

		Coordinate[] firstCoord = {newShpCoords[0]};
		String[] firstCoordName = {newShpNames[0]};
		newShpfile.addPoints(firstCoord, firstCoordName);
		map.addLayer(newShpfile.getFeatureSource(), newMapStyle);

		/*			// Trying to figure out how to change starting zoom programatically, but nothing below was working
		 * 			MapViewport view = map.getViewport();  //.getAreaOfInterest(); //.getLayerBounds();
			ReferencedEnvelope env = view.getBounds(); //.getScreenArea(); //.getBounds(); 
			System.out.println("view  height: " + env.getHeight() + "    view width: " + env.getWidth()); 
			view.setScreenArea(new Rectangle(280338, 350338, 2853047, 3353047)); 

			CoordinateReferenceSystem crs;
			try {
				crs = CRS.decode(SRS_UTM17N);
			ReferencedEnvelope bounds = new ReferencedEnvelope(280338, 350338, 2853047, 3353047, crs); 
			map.setAreaOfInterest(bounds);//			view.setBounds(bounds); 
			} catch (NoSuchAuthorityCodeException e1) {
				e1.printStackTrace();
			} catch (FactoryException e1) {
				e1.printStackTrace();
			}
		 */

		// Now display the map
		frame = new JMapFrame(map); //.showMap(map);
		frame.setSize(frameWidth, frameHeight);
		frame.setLocationRelativeTo(null); 
		frame.enableStatusBar(true);
		frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
		frame.enableToolBar(true);
		frame.setVisible(true);
		frame.enableLayerTable(enableLayers); 


		for (int i=1; i<newShpCoords.length; i++){

			try { Thread.currentThread().sleep(animationRt*1000); } catch (InterruptedException e) { e.printStackTrace(); }
			Coordinate[] tempCoord = {newShpCoords[i]};
			String[] tempCoordName = {newShpNames[0]};
			newShpfile.addPoints(tempCoord, tempCoordName);
			//map.addLayer(newShpfile.getFeatureSource(), newMapStyle);
		}

		return frame; 
	}



}
