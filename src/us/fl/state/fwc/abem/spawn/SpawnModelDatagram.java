package us.fl.state.fwc.abem.spawn;

import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.TextFileIO;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnModelDatagram {

	private int seed;
	private SeatroutParams params; 
	private SeatroutBuilderNonSpatial builder ; 
	private TextFileIO globalAvgTEPOut;
	private TextFileIO globalAvgNumSpawnsOut;
	private int startYear;
	private int sizeFreqYear;
	private int abundYear;
	private int fishMortYear;
	private int mortOn;
	private String gamSizeFunc;
	private double gamInterceptOffset;
	private boolean outputPSpawn;
	private boolean outputTEP;
	private boolean outputSSB;
	private double resource;
	
	

	public SeatroutParams getParams() {
		return params;
	}
	public void setParams(SeatroutParams params) {
		this.params = params;
	}
	public SeatroutBuilderNonSpatial getBuilder() {
		return builder;
	}
	public void setBuilder(SeatroutBuilderNonSpatial builder) {
		this.builder = builder;
	}
	public TextFileIO getGlobalAvgTEPOut() {
		return globalAvgTEPOut;
	}
	public void setGlobalAvgTEPOut(TextFileIO globalAvgTEPOut) {
		this.globalAvgTEPOut = globalAvgTEPOut;
	}
	public TextFileIO getGlobalAvgNumSpawnsOut() {
		return globalAvgNumSpawnsOut;
	}
	public void setGlobalAvgNumSpawnsOut(TextFileIO globalAvgNumSpawnsOut) {
		this.globalAvgNumSpawnsOut = globalAvgNumSpawnsOut;
	}
	public int getStartYear() {
		return startYear;
	}
	public void setStartYear(int startYear) {
		this.startYear = startYear;
	}
	public int getSizeFreqYear() {
		return sizeFreqYear;
	}
	public void setSizeFreqYear(int sizeFreqYear) {
		this.sizeFreqYear = sizeFreqYear;
	}
	public int getAbundYear() {
		return abundYear;
	}
	public void setAbundYear(int abundYear) {
		this.abundYear = abundYear;
	}
	public int getFishMortYear() {
		return fishMortYear;
	}
	public void setFishMortYear(int fishMortYear) {
		this.fishMortYear = fishMortYear;
	}
	public int getMortOn() {
		return mortOn;
	}
	public void setMortOn(int mortOn) {
		this.mortOn = mortOn;
	}
	public String getGamSizeFunc() {
		return gamSizeFunc;
	}
	public void setGamSizeFunc(String gamSizeFunc) {
		this.gamSizeFunc = gamSizeFunc;
	}
	public double getGamInterceptOffset() {
		return gamInterceptOffset;
	}
	public void setGamInterceptOffset(double gamInterceptOffset) {
		this.gamInterceptOffset = gamInterceptOffset;
	}
	public boolean isOutputPSpawn() {
		return outputPSpawn;
	}
	public void setOutputPSpawn(boolean outputPSpawn) {
		this.outputPSpawn = outputPSpawn;
	}
	public boolean isOutputTEP() {
		return outputTEP;
	}
	public void setOutputTEP(boolean outputTEP) {
		this.outputTEP = outputTEP;
	}
	public boolean isOutputSSB() {
		return outputSSB;
	}
	public void setOutputSSB(boolean outputSSB) {
		this.outputSSB = outputSSB;
	}
	public void setSeed(int seed) {
		this.seed = seed;
	}

	public int getSeed() {
		return seed;
	}
	public double getResource() {
		return resource;
	}
	public void setResource(double resource) {
		this.resource = resource;
	}
	
}
