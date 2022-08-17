package us.fl.state.fwc.abem.mse.model;

/**
 * Simple population model for MSE 
 *  
 * @author wade.cooper
 *
 */
public class SeatroutModel {

	/*
	 * 	
	 * ## Justification:
	 * 
	 * Is current management procedure robust to environmental variability (disturbances, habitat change)?  
	 * How do changes in SPR target amount affect resilience?  
	 * 
	 * Approach:
	 * 
	 * Assume density and habitat affect growth (via resource limitiation), and density affects
	 * recruitment via canabalism [recruitment as defined as recruit to adult also affected 
	 * by resources through growth, but this will be captured by growth)
	 * 
	 * Other approach is to simplify and go simple pred-prey dynamics in space as true IBM, 
	 * including bioenergetics,so can avoid trying to come up with equations to represent
	 * simple processes of feeding and predation; e.g., as start to starve, will start searching in 
	 * larger arena outside of safety zone (foraging arena) and thus become susceptible 
	 * to predation -- most fish probably don't starve to death but instead get eaten when searching
	 * more heavily for food
	 * 
	 *   If came up with simple production model for bait fish, could grid this out spatially, and include
	 *   mortality on production model from individual fish in area
	 *   
	 *   Thus growth and movement of fish will be determined by food availability
	 * 
	 * Currently, management can only adjust bag/slot limits, MPAs aren't really an option and
	 * for rec species probably wouldn't do anything anyways (why would I write this?).
	 * 
	 * Habitat protection is vital however, but not under control of fisheries managers -- more 
	 * controlled by coastal development / WQ influences
	 * 
	 * Habitat changes could occur in two way: 
	 * 		(a) Quantity, which could impact recruitment settlement rates
	 * 		(b) Quality, which could impact food sources and as such growth
	 * 
	 *   **Seatrout are up the food chain, so not many top-town processes besides harvest, therefore
	 *   		bottom-up processes would probably mainly impact through growth
	 *   		
	 *   
	 *   ** I could thus do simple model incorporating both habitat loss and tropic interactions
	 *   	by modeling habitat loss effects on recruitment and trophic interactions on growth.
	 *   
	 *   ** Since i don't want to explicity represent bottom-up trophics, I could do theoretical look
	 *   		at it by relating the habitat quality and DD impact on trophics via Lorenzen's growht formulation (1996)
	 *   		with density dependence, where the DD parameter is just a function of habitat quality and density
	 *   			-  do this by having Linf influenced by both the DD and competition coefficient
	 *   
	 *   ** Then represent recruitment via Crow's double DD formulation, where DD parameter
	 *   		is function of habitat quantity
	 *   
	 *   ** Run simulations with different combinations of habitat quality and quantity relationships,
	 *   		with time series of changes in quality and/or quantity, 
	 *   		and see how robust management procedures are for protecting resilience due to
	 *   		chronic disturbances (changes in habitat)
	 *   		
	 *   
	 *   ** Run additional simulations with different levels of acute distrubances, and see
	 *   		how robust management is to acute distrubances, holding he chronic distrubances 
	 *   		still
	 *   
	 *   ** Run final simulations where put in both chronic disturbances in both habitat quality and quantity, 
	 *   		and acute distrubances, and see how robust procedures are
	 *    
	 *    
	 *    ** could do this in a simple spatial manner by assigning each individual to a habitat quality grouping; 
	 *    then don't need to represent movement, etc, but each individual can still have individual variability, and be in a unique
	 *    environment to affect its growth
	 *    
	 *    ** when it releases eggs, those eggs are independently assigned to a habitat grouping to determine their recruitment
	 *    		** could make this autocorrelated to reflect a dispersal distance without having to explicity map out
	 *    			the environment or dispersal
	 *    
	 *    
	 *    
	 * 
	 * Could look at robustness of current management procedures -- bags/slots -- using SPR as 
	 * benchmark, given potential changes to habitat and acute recurring disburbances (cold snaps, red tides)
	 * 
	 * Need process-based link between habitat and seatrout populations
	 * 	(1) Recruitment
	 * 			- as habitat decreases, should lead to theoretical decline in asymptote of SR relationships
	 * 	(2) Growth
	 * 			- as habitat declines, more individuals will be concentrated in smaller areas potentially, 
	 * 				leading to DD in individual growth
	 * 
	 * Could model habitat loss in both in terms of DD in recruitment and growth, where decreasing
	 * habitat would decrease the 'carrying capacity' for both
	 * 		- do this as theoretical range in DD impact with habitat
	 * 		- see Crow's paper on DD and economic efficacy -- presents double DD function for including
	 * 				DD from both juvenile cohort and adults
	 * 			- not quite the same as what I envision, because the term computes recruitment rate as 
	 * 				the proportion of settlers that recruit, given the settler density and adult density
	 * 			- in my case, I have settler density, juvenile density (recent setters hogging up space), and adult density
	 * 			- so a bit different, but may be able to adapt assumptions and use his formulation
	 * 		- bring in Shima and Osenberg habitat influences to adjust DD
	 * 
	 * Do simple MSE, where adjust theoretical habitat and fishing rates over the years
	 * 
	 * Test how robust SPR approach is to 
	 * 
	 * Environmental constraints:
	 * 		Habitat -- need seagrass for feeding, protection, may impact weight and thus spawning
	 * 		Temp, salinity -- affect spawning season
	 * 		Trophics -- related to habitat, 
	 * 		
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * MSE
	 * 	- include 1 index of monitoring where is known with error as per Beth's approach
	 * - include SPR calcs as per my size dependence 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * (1) Build Pop -- use spawn model builder
	 * 
	 * (2) Run pop with simple processes
	 * 		(a) Growth
	 * 				- implement new weight at length optimization to give variable weight -- dW/dt is pulled from N(mean, sd)
	 * 				- use double von bert to get length, where all equal length since no data on age in days 
	 * 
	 * 		(b) Nat mortality
	 * 				- simple age-based vector
	 * 
	 * 		(c) Fish mortality
	 * 				- do as length based for selectivity, where selectivity is based on slot/bag limits
	 * 				- need a process-based feedback between management options and F
	 * 				2 processes affecting selectivity:
	 * 					(a) catchability
	 * 					(b) compliance with regulations
	 * 				- also add in simple release mortality
	 * 
	 *  	(d) Spawning
	 *  			- use GAM approach, but include environmental predictors
	 *  				- temp, salinity, weight, day of year
	 *  
	 *  			- also, could parameterize GAM on weight versus length so that can be better tie in 
	 *  					with variable condition of fish 
	 *  
	 *  	(e) Recruitment
	 *  			- DD, 
	 * 
	 * 
	 *  
	 */
	
}
