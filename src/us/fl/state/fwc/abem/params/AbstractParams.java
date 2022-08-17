package us.fl.state.fwc.abem.params;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import us.fl.state.fwc.abem.Scheduler;
import cern.jet.random.Normal;

import com.vividsolutions.jts.geom.Coordinate;

public class AbstractParams implements Parameters {

	//give all parameters access to scheduler
	protected Scheduler scheduler;

	
	@Override
	public void initialize(Scheduler sched) {
		scheduler = sched;
	}
	
	
	@Override
	public int getAgeAtRecruitment() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAgeMax() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getBaseFecundity(int yearClass, double mass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCarryCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCommBagLimit(int year) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCommInstMortality(int year, double length, int yearClass,
			int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCommOpenSeasonNumDays(int year) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCruiseSpeed(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCullPeriod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<String> getDependentsTypeList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getDirectionalVariability() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getEndOfBreeding() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFallowPeriod(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFallowPeriodSD() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFecunditySizeScaler(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSchoolSize() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public double getHabQualFunction(String string, int yearClass, double value) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getHabitatSearchComplexity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getIniMass(int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLengthAtAge(double age, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public HashMap<String, Long> getInteractionTicks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getLengthAtMass(double mass, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Normal> getLunarNormals(int yearClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLunarPeakShift(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Double> getLunarPeakSizes(int yearClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Integer> getLunarPeaks(int yearClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getMassAtLength(double getLength, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMassCatchUpRate() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxDepth(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxMass(int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxSpeed(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMergeRadius() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMinDepth(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getNatInstMort(int yearClass, double size, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getNormalTick() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumPointsPerSpiral() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumTestSites() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getPreferredDepth(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getProbOfCatchAndRelease(int year, double length,
			int yearClass, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getQueueType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRecBagLimit(int year) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRecInstCatchRate(int year, double length, int yearClass,
			int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getRecOpenSeasonNumDays(int year) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRecReleaseMortality(int year, double length,
			int yearClass, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getRunPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getScaleOfPerception(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSearchRadiusExponent(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Normal> getSeasonNormals(int yearClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Double> getSeasonPeakSizes(int yearClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Integer> getSeasonPeaks(int yearClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Calendar> getSetRunTimes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getSexRatio(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSizeAtMaturityAvg() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSizeAtMaturitySD() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Coordinate> getSpawnAggregationList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getSpawnAggregationSpecificity(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getStartOfBreeding() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Integer> getSuitableHabitatList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<Integer, Double> getSuitableHabitats() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasHomeRange() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean haveSpawnAggregations() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCommLegalSize(int year, double length) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCommOpenSeason(Calendar currentDate) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isMultipleSpawner() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReactive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRecLeagalSize(int year, double length) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRecOpenSeason(Calendar currentDate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSetRunTimes() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getHomeRanges(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSpawnAggregationWeight(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPopulationAbundance(int year, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAvgPLD() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAvgEPSD() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFertilizationRtAvg(int yearClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFertilizationRtSD() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public boolean isRunnable() {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public double getMAdultVmax(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getMSettlerVmax(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getMAdultKm(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getMSettlerKm(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getMAdultExpon(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getMSettlerExpon(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getEPSM(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getPropSetSizeAtMat() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getCondInflOnSpawn() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getSizeInflOnSpawnIntcpt() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getSizeInflOnSpawnKm() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getFishInstMort(int year, int yearClass, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getMassStDevScalar(int sex) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getDDLengthAtAge(double age, int sex, double resource,
			double biomass) {
		// TODO Auto-generated method stub
		return 0;
	}





	@Override
	public double getSizeDepFishMort(int year, double size, double expectedAge,
			int sex) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getSizeDepNatMort(double size, double expectedAge, int sex) {
		// TODO Auto-generated method stub
		return 0;
	}





}
