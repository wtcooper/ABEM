package us.fl.state.fwc.abem.environ.impl;

import java.io.IOException;
import java.util.ArrayList;

import us.fl.state.fwc.util.DeleteFiles;
import us.fl.state.fwc.util.geo.NetCDFFile;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class CreateABEMGridFromNetCDF {

	String EFDCDirectory = "c:\\Java\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	String EFDCBathFileName = EFDCDirectory + "TB2SB_WGS_Bathym.nc";
	private NetCDFFile ncFile; 
	private String latName = "lat";
	private String lonName = "lon";
	private String bathName = "water_depth";


	@SuppressWarnings("unchecked")
	public void step(){

		try {
			ncFile = new NetCDFFile(EFDCBathFileName); 
			//			EFDCLandFile = new NetCDFFile(EFDCLandFileName); 
		} catch (IOException e) {}

		ncFile.setInterpolationAxes(latName, lonName);
		ncFile.setVariables(latName, lonName, bathName); 
		//		EFDCLandFile.setVariables(latName, lonName, landName);

		ArrayList<Coordinate[]> featureCoords = new ArrayList<Coordinate[]>(); 
		ArrayList<Object[]> featureVals = new ArrayList<Object[]>(); 
		
		//Attributes:
		//geom, latIndex, lonIndex, bathymetry, habitat, SAVArea, fishingEffort
		Class[] attributeTypes = {Polygon.class, Integer.class, Integer.class, Double.class, Integer.class, Double.class}; //new Class[2];
		String[] attributeNames = {"the_geom", "latIndex", "lonIndex", "bathym", "habType", "SAVCover"}; //new String[2];
		String EPSG_code = "WGS84"; 

		
		

		
		int latDim = ncFile.getSingleDimension(latName);
		int lonDim = ncFile.getSingleDimension(lonName); 
		
		for (int i=0; i<latDim; i++){
			for (int j=0; j<lonDim; j++){

				Coordinate midCoord = new Coordinate(ncFile.getValue(lonName, new int[] {j}).doubleValue(), ncFile.getValue(latName, new int[] {i}).doubleValue(), 0); 
				Coordinate[] coords = new Coordinate[5];  
				coords[0] = new Coordinate(midCoord.x-0.002, midCoord.y-0.002, 0);
				coords[1] = new Coordinate(midCoord.x+0.002, midCoord.y-0.002, 0);
				coords[2] = new Coordinate(midCoord.x+0.002, midCoord.y+0.002, 0);
				coords[3] = new Coordinate(midCoord.x-0.002, midCoord.y+0.002, 0);
				coords[4] = new Coordinate(midCoord.x-0.002, midCoord.y-0.002, 0);
				
				int latIndex = i; 
				int lonIndex = j;
				
				double bath = ncFile.getValue(bathName, new int[] {i, j}).doubleValue();
				
				//TODO -- need to set the habitat appropriately for each cell
				int hab = 0; 
				
				Object[] vals = {latIndex, lonIndex, bath, hab}; 

				//only do for those not on land
				if (bath<0 && bath>-999){
					featureCoords.add(coords);
					featureVals.add(vals);
				}
			}
		}
		
		
		
		DeleteFiles delete = new DeleteFiles();
		delete.deleteByPrefix("output", "ABEMGridTest3");
		SimpleShapefile shape = new SimpleShapefile("output/ABEMGridTest3.shp"); 
		shape.createShapefile(attributeTypes, attributeNames, featureCoords, featureVals, EPSG_code); //.createShapefile(attributeTypes, attributeNames, attributeValues, EPSG_code); //.createShapefile(Polygon.class, featuresConv, "WGS84"); 




	}











	public static void main(String[] args) {
		CreateABEMGridFromNetCDF a = new CreateABEMGridFromNetCDF();
		a.step();
	}
}
