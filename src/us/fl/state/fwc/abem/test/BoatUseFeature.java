package us.fl.state.fwc.abem.test;


import org.opengis.filter.identity.FeatureId;

import com.vividsolutions.jts.algorithm.CentroidArea;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class BoatUseFeature {

	String type; 
	private Geometry geom; 
	FeatureId FID; 
	int numOfDest = 0; 

	
	public Geometry getGeom() {
		return geom;
	}
	public void setGeom(Geometry geom) {
		this.geom = geom;
	}

	public Coordinate getCentroidCoord(){
	      CentroidArea cent = new CentroidArea();
	      cent.add(geom);
	      Coordinate centPt = cent.getCentroid();
		return centPt; 
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public FeatureId getFID() {
		return FID;
	}
	public void setFID(FeatureId fID) {
		FID = fID;
	}
	
	public void addDest(int num){
		numOfDest += num; 
	}
	
	public int getNumOfDest(){
		return numOfDest; 
	}
}
