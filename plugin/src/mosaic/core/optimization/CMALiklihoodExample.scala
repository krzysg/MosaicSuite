package mosaic.core.optimization

import cma._
import cma.fitness._
import mosaic.calibration._
import mosaic.interaction._

object CMALiklihoodExample {
	
	def main(args : Array[String]) : Unit = {
		val i = 0
		
		val DCell = ReadMat.getVectorFromCellFile(ReadMat.sketchPath + "DCell.mat", "DCell",i)
		val ddCell = ReadMat.getVectorFromCellFile(ReadMat.sketchPath +"ddCell.mat", "dCell",i)
		val qCell = ReadMat.getVectorFromCellFile(ReadMat.sketchPath +"qCell.mat", "qCell" ,i)
		
		def fitfun: AbstractObjectiveFunction = new LikelihoodOptimizer(qCell, ddCell,DCell, PotentialFunctions.potentialShape(1));
		
		CMAOptimization.optimize(fitfun,1)
		
		println(fitfun.valueOf(Array(-0.021777343750001)))
	} // main  
}
