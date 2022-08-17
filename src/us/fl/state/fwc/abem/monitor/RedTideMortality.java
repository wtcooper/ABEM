package us.fl.state.fwc.abem.monitor;

import org.geotools.geometry.jts.JTSFactoryFinder;

import us.fl.state.fwc.abem.organism.Fish;
import us.fl.state.fwc.abem.params.impl.RedTideMortalityParams;
import us.fl.state.fwc.util.geo.CoordinateUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


public class RedTideMortality extends Monitor {

	protected GeometryFactory gf; // = new GeometryFactory();


	@Override
	public void run() {

		for (int i=0; i<monitorees.size(); i++){
			Fish animal = (Fish) monitorees.get(i); 

			if (RedTideMortalityParams.usePointMortality){
				Coordinate[] focalPoints = RedTideMortalityParams.focalPoints;
				double probOfMort = 0; 

				for (int j=0; j<focalPoints.length; j++){

					double dist = 
						CoordinateUtils.getDistance(animal.getCoords(), focalPoints[j]);

					//add the probabilities together
					probOfMort += ((-1)*(1/(1+Math.exp(-100*(dist-0.04)))-0.5)*2+1)/2.0;
				}

				//kill off the whole school


				if (uniform.nextDoubleFromTo(0, 1) < probOfMort)
					scheduler.getOrganismFactory().recycleAgent(animal); 

			}
			
			
			//else, use polygon
			else {
				if (gf == null) gf = JTSFactoryFinder.getGeometryFactory(null);

				Coordinate[][] polygons = RedTideMortalityParams.polygons;
				for (int j=0; j<polygons.length; j++){
					Coordinate[] polygon = polygons[j];
					Polygon geom = gf.createPolygon(new LinearRing(new CoordinateArraySequence(polygon), gf), null);

					Point point = gf.createPoint(animal.getCoords());
					
					//can get distance with point.distance(geom) 
					if (point.intersects(geom)) {

						//need to get avgLength so can reset the biomass after mortality below
						double avgLength = params.getLengthAtMass(animal.getGroupBiomass()
								/animal.getGroupAbundance(), animal.getGroupSex());

						
						//loop through all and decrement the group abundance
						//this will avoid killing off more or less than should be killed since schools
						//are unequal in size
						
						int numToDie=0;
						int groupAbundance = animal.getGroupAbundance();
						for (int k=0; k<groupAbundance; k++){
							//testing:
							if (uniform.nextDoubleFromTo(0, 1) 
									< RedTideMortalityParams.polygonMortRt)
								numToDie++; 
						}

						groupAbundance -= numToDie; 
						
						if (groupAbundance < 1)
							scheduler.getOrganismFactory().recycleAgent(animal); 
						
						else {
							animal.setGroupAbundance(groupAbundance);
							//reset the groupBiomass
							double groupBiomass = 
								animal.getParams().getMassAtLength(avgLength, animal.getGroupSex())*groupAbundance;
							animal.setGroupBiomass(groupBiomass);
							animal.setNominalBiomass(groupBiomass);

						}
					}

				
				}
			}


		}


	}



}
