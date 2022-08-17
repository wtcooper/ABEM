package us.fl.state.fwc.abem.mse.assess;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import us.fl.state.fwc.abem.mse.AssessmentResults;
import us.fl.state.fwc.abem.mse.StockAssessment;
import us.fl.state.fwc.util.WindowsCMD;

public class SeatroutADMB implements StockAssessment {

	String modelPath; // = {"c:/work/workspace/ADMB_Models/FWRI_SAs/sst2010/workingCpy/sst2010x.exe"};
	
	SeatroutADMBData datFile;
	SeatroutADMBReport repFile;
	

	public double getSPR() {
		return 0;
	}
	
	/**
	 * Set the path for the model
	 */
	public void setModel(String modelPath){
		this.modelPath = modelPath;
		datFile = new SeatroutADMBData(modelPath.replace("exe", "dat"));
		repFile = new SeatroutADMBReport(modelPath.replace("exe", "rep"));
	}
	
	
	/**
	 * Run the model
	 */
	public void runModel() {
		WindowsCMD exe = new WindowsCMD();
		exe.setOutputOn(false);
		
		exe.execute(new String[] {modelPath});

		//after done executing, get the report file
		repFile.open();
	}

	
	@Override
	public AssessmentResults getResults() {
		return repFile;
	}

	
	
	//Test run
	public static void main(String[] args) {
		SeatroutADMB assess = new SeatroutADMB();
		assess.setModel("c:/work/workspace/ADMB_Models/FWRI_SAs/sst2010/workingCpy/sst2010x.exe");
//		ass.runModel();
	}


	
}
