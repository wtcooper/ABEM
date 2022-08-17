package us.fl.state.fwc.abem.monitor;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
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
import org.geotools.styling.ColorMap;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.ContrastMethod;

import us.fl.state.fwc.abem.params.impl.FishGridMapperParams;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleMapper;

import com.ibm.icu.util.Calendar;
import com.vividsolutions.jts.geom.Envelope;


public class FishGridMapper extends Monitor {

	public JMapFrame frame; 
	MapContext map;



	//initialize map
	@SuppressWarnings("unchecked")
	public void drawMap(){
		// Create a map context and add our shapefile to it
		map = new DefaultMapContext();

		map.setTitle("ABEM World"); 

		try {

			//################################################
			//First draw the base maps
			//################################################




			//Land
			if (FishGridMapperParams.drawLand){
				File landFile = new File(FishGridMapperParams.displayLandFilename);
				FileDataStore store = FileDataStoreFinder.getDataStore(landFile);
				FeatureSource landFSource = store.getFeatureSource();
				map.addLayer(landFSource, SLD.createPolygonStyle(new Color(97, 133, 70), new Color(97, 100, 70), .5f));
			}



			frame = new JMapFrame(map);  
			frame.setSize(FishGridMapperParams.frameWidth, FishGridMapperParams.frameHeight);
			frame.setLocationRelativeTo(null); 
			frame.enableStatusBar(true);
			frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN,JMapFrame.Tool.RESET);
			frame.enableToolBar(true);
			frame.enableLayerTable(true); 
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);

			
			

			//################################################
			//Second draw the display maps to go in layers table
			//################################################


			//Bathymetry
			if (FishGridMapperParams.drawBathy){
				StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
				FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
				int band = 1; //RGB band to draw
				ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
				SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);
				RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
				ChannelSelection sel = sf.channelSelection(sct);
				sym.setChannelSelection(sel);

				NetCDFFile bath = new NetCDFFile(FishGridMapperParams.bathFileName);
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

				addLayer(grid, style, "Bathymetry"); 
			}
			
			
			//Habitat
			if (FishGridMapperParams.drawHabitat){
				File habFile = new File(FishGridMapperParams.habitatFileName); 
				FileDataStore habStore = FileDataStoreFinder.getDataStore(habFile);
				FeatureSource habFSource = habStore.getFeatureSource();
				addLayer(habFSource, SLD.createPolygonStyle(new Color(97, 215, 70), new Color(97, 200, 70), .5f), "Seagrass");
			}

			
			
			//Set up color displays
			int numInts = 9; 
			ColorBrewer brewer = ColorBrewer.instance();
			brewer.loadPalettes();
			BrewerPalette[] palettes = brewer.getPalettes  (ColorBrewer.SEQUENTIAL);

			
			int year = scheduler.getCurrentDate().get(Calendar.YEAR) - 1;
			
			//Abundance
			if (FishGridMapperParams.drawAbundance){

				Object[] array = 
					scheduler.getGrid().getGridCov("abundance", "Seatrout", year);
				
				GridCoverage2D grid =(GridCoverage2D) array[0];  

				Color[] colors = palettes[0].getColors(numInts);
				double min = ((Double) array[1]).doubleValue(); 
				double max = ((Double) array[2]).doubleValue(); 
				double[] breaks = new double[numInts];
				for (int i=0; i<breaks.length; i++){
					breaks[i] = min + i*(Math.abs(max-min)/numInts); 
				}
				StyleBuilder sb = new StyleBuilder();
				ColorMap cmap = sb.createColorMap(new String[numInts] , breaks, colors,
				ColorMap.TYPE_RAMP);
				Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

				addLayer(grid, style, "Abundance"); 
			}
			

			//Biomass
			if (FishGridMapperParams.drawBiomass){

				Object[] array = 
					scheduler.getGrid().getGridCov("biomass", "Seatrout", year); 

				GridCoverage2D grid =(GridCoverage2D) array[0];  

				Color[] colors = palettes[2].getColors(numInts);
				double min = ((Double) array[1]).doubleValue(); 
				double max = ((Double) array[2]).doubleValue(); 
				double[] breaks = new double[numInts];
				for (int i=0; i<breaks.length; i++){
					breaks[i] = min + i*(Math.abs(max-min)/numInts); 
				}
				StyleBuilder sb = new StyleBuilder();
				ColorMap cmap = sb.createColorMap(new String[numInts] , breaks, colors,
				ColorMap.TYPE_RAMP);
				Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

				addLayer(grid, style, "Biomass"); 
			}
			
			
			//SSB
			if (FishGridMapperParams.drawSSB){

				Object[] array = 
					scheduler.getGrid().getGridCov("SSB", "Seatrout", year); 

				GridCoverage2D grid =(GridCoverage2D) array[0];  

				Color[] colors = palettes[3].getColors(numInts);
				double min = ((Double) array[1]).doubleValue(); 
				double max = ((Double) array[2]).doubleValue(); 
				double[] breaks = new double[numInts];
				for (int i=0; i<breaks.length; i++){
					breaks[i] = min + i*(Math.abs(max-min)/numInts); 
				}
				StyleBuilder sb = new StyleBuilder();
				ColorMap cmap = sb.createColorMap(new String[numInts] , breaks, colors,
				ColorMap.TYPE_RAMP);
				Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

				addLayer(grid, style, "SSB"); 
			}
			
			
			//TEP
			if (FishGridMapperParams.drawTEP){

				Object[] array = 
					scheduler.getGrid().getGridCov("TEP", "Seatrout", year); 

				GridCoverage2D grid =(GridCoverage2D) array[0];  

				Color[] colors = palettes[4].getColors(numInts);
				double min = ((Double) array[1]).doubleValue(); 
				double max = ((Double) array[2]).doubleValue(); 
				double[] breaks = new double[numInts];
				for (int i=0; i<breaks.length; i++){
					breaks[i] = min + i*(Math.abs(max-min)/numInts); 
				}
				StyleBuilder sb = new StyleBuilder();
				ColorMap cmap = sb.createColorMap(new String[numInts] , breaks, colors,
				ColorMap.TYPE_RAMP);
				Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

				addLayer(grid, style, "TEP"); 
			}


			
			//Recruitment
			if (FishGridMapperParams.drawRecruitment){

				Object[] array = 
					scheduler.getGrid().getGridCov("recruitment", "Seatrout", year); 

				GridCoverage2D grid =(GridCoverage2D) array[0];  

				Color[] colors = palettes[5].getColors(numInts);
				double min = ((Double) array[1]).doubleValue(); 
				double max = ((Double) array[2]).doubleValue(); 
				double[] breaks = new double[numInts];
				for (int i=0; i<breaks.length; i++){
					breaks[i] = min + i*(Math.abs(max-min)/numInts); 
				}
				StyleBuilder sb = new StyleBuilder();
				ColorMap cmap = sb.createColorMap(new String[numInts] , breaks, colors,
				ColorMap.TYPE_RAMP);
				Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));
				
				addLayer(grid, style, "Recruitment"); 
			}
			
			
			
			//SSB:TEP
/*			if (FishGridMapperParams.drawSSB2TEPRatio){

				System.out.println();
				
				Object[] array = 
					scheduler.getGrid().getGridCov("SSB:TEP", "Seatrout", year); 

				GridCoverage2D grid =(GridCoverage2D) array[0];  

				Color[] colors = palettes[6].getColors(numInts);
				double min = ((Double) array[1]).doubleValue(); 
				double max = ((Double) array[2]).doubleValue(); 
				double[] breaks = new double[numInts];
				for (int i=0; i<breaks.length; i++){
					breaks[i] = min + i*(Math.abs(max-min)/numInts); 
				}
				StyleBuilder sb = new StyleBuilder();
				ColorMap cmap = sb.createColorMap(new String[numInts] , breaks, colors,
				ColorMap.TYPE_RAMP);
				Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

				addLayer(grid, style, "SSB:TEP"); 
			}
*/


		
		} catch (IOException e1) {
			e1.printStackTrace();
		} 




	}


	/**Add a shapefile layer to the display
	 * 
	 * @param featureSource
	 * @param style
	 */
	public void addLayer(FeatureSource featureSource, Style style, String title) {
		map.addLayer(featureSource, style); 
		map.getLayer(map.getLayerCount()-1).setTitle(title);
		frame.repaint();
	}


	/**Add a grid coverage to the display
	 * 
	 * @param arg0
	 * @param arg1
	 */
	public void addLayer(GridCoverage2D grid, Style style, String title) {
		map.addLayer(grid, style); 
		map.getLayer(map.getLayerCount()-1).setTitle(title);
		frame.repaint();
	}


	/**Add a grid coverage to the display
	 * 
	 * @param arg0
	 * @param arg1
	 */
	public void addLayer(float[][] data) {
		frame.repaint();

	}




	@Override
	public void run() {
		//update();
	}




}
