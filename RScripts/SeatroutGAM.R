setwd("C:/work/workspace/ABEM/data");

data = read.table("C:\\work\\workspace\\ABEM\\data\\GAMGood.txt", header = TRUE)

dataWO5<- subset(data,Zone<5 &  MornAfterZone<1 & Mature>1 , select=c(TL, SomaticWt, Zone, ZoneCat,Month, MonthDoub, DayOfYear, AM_PM,WadeLunarFull, RelCondLogs, Temp, 
				Salinity, Mature, SpawnCapable, ActiveSpawning, ActiveSpawn2, FOM, POF12, SPN_2hr, WadeLunarReflect))


#### For MGCV package ####
library(mgcv)


dataWO5$Month = dataWO5$MonthDoub
dataWO5$Size = dataWO5$TL


###########################
# Both GAMs combined
activeSpawn = gam(ActiveSpawn2~ 	
				#s(WadeLunarFull, bs="cc")+
				as.factor(Zone)+
				#s(RelCondLogs)+
				#s(Temp) +
				#s(Salinity) + 
				#s(Month, k=6, fx=TRUE) +
				#s(DayOfYear, bs="cc") + 				# this version does cyclic spline where ends match up
				s(DayOfYear) +
				#s(Size, k=1, fx=TRUE),  
				s(Size),
		#te(DayOfYear, Size), //interaction term  
		#s(SomaticWt),
		#Size,  
		family=binomial(link = "logit"), data=dataWO5)

#summary(activeSpawn)
mean(dataWO5$TL)
#plot.gam(activeSpawn, shade=TRUE, font.lab=2, 
#		pages=1, 
#		cex.axis=.6*magnify, cex.lab=.7*magnify,
#		scale=0 
#)

#datavals = data.frame(Zone=c(1,2,3,4), DayOfYear=c(200), Size=c(200))
#pred <- predict.gam(activeSpawn, datavals, se.fit=TRUE, type="terms") #type="response")
