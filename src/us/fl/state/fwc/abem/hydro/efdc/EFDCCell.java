package us.fl.state.fwc.abem.hydro.efdc;

import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.algorithm.CentroidArea;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class EFDCCell {

	private Geometry geom; 
	private double rugosity=0, depth = 0, roughness = 0.02, vegetation=0;
	private Int3D index;
	private int L; 
	private int numLayers; 
	private boolean isOpenBoundCell; 
	private double dx, dy; 
	
	public Geometry getGeom() {
		return geom;
	}
	public void setGeom(Geometry geom) {
		this.geom = geom;
	}
	public double getRugosity() {
		return rugosity;
	}
	public void setRugosity(double rugosity) {
		this.rugosity = rugosity;
	}
	public double getDepth() {
		return depth;
	}
	
	/**Returns the layer number for a given depth
	 * Note: depth should be provided as positive down
	 * 
	 * @param depth
	 * @return
	 */
	public int getDepthLayer(double depth){
		return (int) (depth/(this.depth/numLayers)); 
	}
	
	/**Returns the depth (positive down) at the centroid of a given layer
	 * 
	 * @param layer
	 * @return
	 */
	public double getLayerCentroid(int layer){
		double layerThickness = depth/(double) numLayers; 
		return layerThickness*layer + layerThickness/2.0; 
	}
	
	
	public void setDepth(double depth) {
		this.depth = depth;
	}
	public double getRoughness() {
		return roughness;
	}
	public void setRoughness(double roughness) {
		this.roughness = roughness;
	}
	public double getVegetation() {
		return vegetation;
	}
	public void setVegetation(double vegetation) {
		this.vegetation = vegetation;
	}
	public Int3D getIndex() {
		return index;
	}
	public void setIndex(Int3D index) {
		this.index = index;
	}

	public int getL() {
		return L;
	}
	public void setL(int l) {
		L = l;
	}
	public Coordinate getCentroidCoord(){
	      CentroidArea cent = new CentroidArea();
	      cent.add(geom);
	      Coordinate centPt = cent.getCentroid();
		return centPt; 
	}
	
	/**Sets the number of vertical layers used in the model
	 * 
	 * @param numLayers
	 */
	public void setNumLayers(int numLayers){
		this.numLayers = numLayers;
	}
	public boolean isOpenBoundCell() {
		return isOpenBoundCell;
	}
	public void setOpenBoundCell(boolean isOpenBoundCell) {
		this.isOpenBoundCell = isOpenBoundCell;
	}
	public double getDx() {
		return dx;
	}
	public void setDx(double dx) {
		this.dx = dx;
	}
	public double getDy() {
		return dy;
	}
	public void setDy(double dy) {
		this.dy = dy;
	}
	
}
