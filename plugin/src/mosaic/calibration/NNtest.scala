package mosaic.calibration

import scalala.Scalala._;
import scalala.tensor.dense._
import weka.core.DenseInstance


object NNtest {
  def main(args : Array[String]) : Unit = {
	  
	  //NNBasic
	  logLikliTest
	  //mseTest
  }
  
  def NNBasic {
	  val d = 2

	  val nn = new NearestNeighbour(d)
	  
	  val nbrRefPoints = 100
//	  val nbrRefpoints = 10000000 //uses approx. 1 GB ram
	  // independent randomly placed reference objects
//	  var refPoints = Array.fill(nbrRefPoints)(rand(d).toArray)
	  // regularly placed reference objects
	  var refPoints = nn.getMesh(1, 5, 1, 5)
	  
	  val nbrQueryPoints = 1000
	  // independent randomly placed query objects
//	  val queryPoints = Array.fill(nbrQueryPoints)(rand(d).toArray)
	  // regularly placed query objects
	  val queryPoints = nn.getMesh(1, 1000, 1, 2000)
	  
	  
	  val time = (new java.util.Date()).getTime()
	  // generate KDTree
	  nn.setReferenceGroup(refPoints)
	  println("Generation KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	  // find NN
      val dist = nn.getDistances(queryPoints) 
      println("Generation + Search KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	  dist.slice(0,5).map(println(_))
	  
	  
	  // estimate q(d)
	  val est = new KernelDensityEstimator()
	  est.addValues(dist)
	  
	  val maxDist = dist.reduceLeft(Math.max(_,_))
	  val minDist = dist.reduceLeft(Math.min(_,_))
	  
	  val x = linspace(minDist, maxDist, 100)
	  val prob = est.getProbabilities(x.toArray)
	  plot(x, new DenseVector(prob))
	  
  }
  
  def logLikliTest() {
	  
	  val intervalls = new DenseVector(10)(Iterator.range(1,10))
	  // loglikelihood optimization
	  //val likli = new LikelihoodOptimizer(intervalls, x)
	   
		//	   % single cell fits to get strength
		//
		//results = resultsAd2;
		//
		//shape = @f_plummer;

		//
		//    d = q_of_d(:,1);
		//    q = q_of_d(:,2);
		//    D = results(i).D;
		//    D(D<d(1)) = d(1);
		//    D(D>d(end)) = d(end);
		//    
		//    epsilon_0 = 1;
		//    sigma = 1.1476;
		//    epsilon = fminsearch(@(epsilon) negloglik(q,d,[epsilon sigma],shape,D),epsilon_0);
		//    nll = negloglik(q,d,[epsilon sigma],shape,D);
		//    
		//    pCell{i} = [epsilon sigma nll];        
  }
  
  def mseTest {
	   import org.apache.commons.math.stat.regression._

	   val cali = new mosaic.plugins.CalibriScala_
	   
		val reg = new SimpleRegression();
	    reg.addData(0, 0)
	    reg.addData(1, 0)
	    reg.addData(2, 0)
	    reg.addData(2, 1)
	    reg.addData(2, -1)
	    println("MSE" + reg.getMeanSquareError)
	    reg.removeData(2,1)
	    println(reg.getMeanSquareError)
	    reg.removeData(2,-1)
	    println(reg.getMeanSquareError)
	    reg.addData(2, 0.00001)
	    println("MSE" + reg.getMeanSquareError)
	    
  }
}