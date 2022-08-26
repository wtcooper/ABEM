
east = read.table("RonReproEastCoast.txt", header = TRUE)
west = read.table("RonReproWestCoast.txt", header = TRUE)

#### For MGCV package ####
library(mgcv)

#West coast

### - try different GAM packages and switch model order
### check length versus sample  - likely small fish active spawning 
activeWest = gam(ActiveSpawn~ 	
				#s(TL, k=1, fx=TRUE) +
				s(TL) + 
				s(DayOfYear),
		family=binomial(link = "logit"), data=west)

# East coast
activeEast = gam(ActiveSpawn~ 	
				s(DayOfYear) +
				s(TL),
		family=binomial(link = "logit"), data=east)
