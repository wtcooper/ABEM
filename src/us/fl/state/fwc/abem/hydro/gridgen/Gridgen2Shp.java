package us.fl.state.fwc.abem.hydro.gridgen;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.data.JFileDataStoreChooser;

import ucar.ma2.InvalidRangeException;
import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.geo.SimpleMapper;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class Gridgen2Shp {

	public boolean useFileChooser = false;
	public boolean showGrid = true;
	static String inFile = "dataTest/grid.7"; 
	public Coordinate[][] coords; 

	public String filename;
	
	
	public void readFile(String infile) throws IOException, InvalidRangeException{ 

		BufferedReader reader = new BufferedReader(new FileReader(new File(infile)));
		filename = infile;
		
		if (useFileChooser) {
			File file = JFileDataStoreChooser.showOpenFile("0", null);
			reader = new BufferedReader(new FileReader(file));
			filename = file.getPath(); 
		}

		String line = reader.readLine(); 
		String inTokens[] = line.split("[ ]+");
		int xDim = Integer.parseInt(inTokens[1]);
		int yDim = Integer.parseInt(inTokens[3]);
		coords = new Coordinate[yDim][xDim]; 

		double counter = 0;
		for (line = reader.readLine(); line != null; line = reader.readLine()) {

			int yDimCount = (int) ((double) counter / (double) xDim );
			int xDimCount = (int) ((double) counter % (double) xDim);
				
			String tokens[] = line.split("[ ]+"); 
			if (!tokens[0].equals("NaN")){
				double lon = Double.parseDouble(tokens[0]);
				double lat = Double.parseDouble(tokens[1]);
				Coordinate coord = new Coordinate(lon, lat, 0);
				coords[yDimCount][xDimCount] = coord;
			}
			counter++;
		}
	}

	
	
	public void makeShp(){
	
		ArrayList<Coordinate[]> featureCoords = new ArrayList<Coordinate[]>(); 
		String EPSG_code = "WGS84"; 
		
		
		
		for (int i=0; i<coords.length-1; i++){
			for (int j=0; j<coords[0].length-1; j++){
				if (coords[i][j] != null 
						&& coords[i][j+1] != null
						&& coords[i+1][j] != null
						&& coords[i+1][j+1] != null) {
					Coordinate[] coordTemp = 
					{coords[i][j], coords[i][j+1], coords[i+1][j+1], coords[i+1][j],coords[i][j]}; 
					featureCoords.add(coordTemp); 
				}
			}
		}
		
		//get out file name
		String filename2[] = filename.split("\\."); 
		String prefix = filename2[0];

		DeleteFiles delete = new DeleteFiles();
		delete.deleteByPrefix(prefix + ".shp");
		SimpleShapefile shape = new SimpleShapefile(prefix + ".shp"); 
		shape.createShapefile(Polygon.class, featureCoords, EPSG_code);  

		if (showGrid){
		SimpleMapper map = new SimpleMapper("grid", 800, 600); 
		String[] layers = {prefix + ".shp"};
		Style[] layerStyles = 
		{	SLD.createPolygonStyle(new Color(255, 0, 255), new Color(255, 0, 255), .5f)};
		map.drawShapefileLayers(layers, layerStyles);  
		}
	
	}

	
	
	
	public static void main(String[] args) throws IOException, InvalidRangeException {

		Gridgen2Shp g = new Gridgen2Shp();
		g.readFile(inFile);
		g.makeShp();

	}
}
