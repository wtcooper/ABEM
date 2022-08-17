package us.fl.state.fwc.abem.mse;

/**Stock Assessment interface, where assessments are run at scheduled times (e.g., every few
 * years or every year).  Once assessment is complete, it influences management procedure
 * model. 
 * 
 * @author Wade.Cooper
 *
 */
public interface StockAssessment {

	public AssessmentResults getResults();
	
}
