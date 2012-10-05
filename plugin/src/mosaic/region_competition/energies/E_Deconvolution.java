package mosaic.region_competition.energies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Iterator;

import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Point;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.algorithm.fft2.*;

public class E_Deconvolution extends ExternalEnergy
{	
	final private Img< FloatType > vModelImage;
	final private Img< FloatType > DevImage;
	final private Img< FloatType > m_PSF;
	private HashMap<Integer, LabelInformation> labelMap;
	private IntensityImage aDataImage;
	
	public E_Deconvolution(IntensityImage aDI, HashMap<Integer, LabelInformation> labelMap, ImgFactory< FloatType > imgFactory, int dim[])
	{
		super(null, null);
		vModelImage = imgFactory.create(dim, new FloatType());
		DevImage = imgFactory.create(dim, new FloatType());
		m_PSF = imgFactory.create(dim, new FloatType());
		this.labelMap = labelMap;
		aDataImage = aDI;
	}
	
	@Override
	public Object atStart()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	private static float Median(ArrayList<Float> values)
	{
	    Collections.sort(values);
	 
	    if (values.size() % 2 == 1)
	    	return values.get((values.size()+1)/2-1);
	    else
	    {
	    	float lower = values.get(values.size()/2-1);
	    	float upper = values.get(values.size()/2);
	 
	    	return (lower + upper) / 2.0f;
	    }	
	}
	
	public void GenerateModelImage(Img< FloatType > aPointerToResultImage,
			LabelImage aLabelImage,
		    Img< FloatType > aPSFImage,
		    HashMap<Integer, LabelInformation> labelMap)
	{ 
		Cursor< FloatType > cVModelImage = vModelImage.cursor();
        int size = aLabelImage.getSize();
		for(int i=0; i < size && cVModelImage.hasNext() ; i++)
		{
			cVModelImage.fwd();
			int vLabel = aLabelImage.getLabel(i);
            if (vLabel == aLabelImage.forbiddenLabel)
            {
                vLabel = 0; // Set Background value ??
            }
			cVModelImage.get().set(vLabel);
		}

//        typedef ConvolutionImageFilter<InternalImageType, InternalImageType> ConvolutionFilterType;
        
		
		new FFTConvolution< FloatType > (vModelImage,aPSFImage);
	}
	
	public void RenewDeconvolution(LabelImage aInitImage)
	{
        //        std::cout << "iteration " << m_iteration_counter << std::endl;
        //        std::cout << "Begin of renew deconv stats:" << std::endl;
        //        std::cout << "BG mean: "  << m_Means[0] << std::endl;


        /**
         * Generate a model image using rough estimates of the intensities. Here,
         * we use the old intensity values.
         * For all FG regions (?), find the median of the scaling factor.
         */

        /// The BG region is not fitted above(since it may be very large and thus
        /// the mean is a good approx), set it to the mean value:
        double vOldBG = aInitImage.getLabelMap().get(0).mean;
        //        m_Intensities[0] = m_Means[0];


        //        std::stringstream vSS;
        //        vSS << "DivisionImage_" << m_iteration_counter << ".tif";
        //        WriteInputImageTypeToFile(vSS.str().c_str(), vDivisionFilter->GetOutput(), 1000);

        // Using ITK: median to inaccurate since limited to bin accuracy? speed?

        //        typedef itk::AbsImageFilter<LabelImageType, LabelImageType> AbsImageFilterType;
        //        typename AbsImageFilterType::Pointer vAbsFilter = AbsImageFilterType::New();
        //        vAbsFilter->SetInput(aInitImage);
        //
        //        /**
        //         * get statistics of each label
        //         */
        //        typedef itk::LabelStatisticsImageFilter<InputImageType, LabelImageType>
        //                LabelStatisticsFilterType;
        //        typename LabelStatisticsFilterType::Pointer vLabelStats = LabelStatisticsFilterType::New();
        //        vLabelStats->SetInput(aDataImage);
        //        vLabelStats->SetLabelInput(vAbsFilter->GetOutput());
        //        vLabelStats->SetUseHistograms(true); // we're interested in the median.
        //        vLabelStats->Update();


        // Time vs. Memory:
        // Memory efficient: iterate the label image: for all new seed points (new label found),
        // iterate through the label usinga floodfill iterator. While iterating,
        // read out the data and model image. Put the quotient of those into an
        // 'new' array. Sort the array, read out the median and delete the array.
        //
        // Time efficient: iterate the label image. Store all quotients found in
        // an array corresponding to the label. This is another 32-bit copy of
        // the image.



        /// Set up a map datastructure that maps from labels to arrays of data
        /// values. These arrays will be sorted to read out the median.
        
        HashMap<Integer, Integer> vLabelCounter = new HashMap<Integer, Integer> ();
        HashMap<Integer, Float> vIntensitySum = new HashMap<Integer, Float> ();

        HashMap <Integer, ArrayList<Float> > vScalings3 = new HashMap <Integer, ArrayList<Float> > ();
        
        /// For all the active labels, create an entry in the map and initialize
        /// an array as the corresponding value.
        Iterator< Map.Entry < Integer, LabelInformation > > vActiveLabelsIt = aInitImage.getLabelMap().entrySet().iterator();
        while (vActiveLabelsIt.hasNext())
        {
        	Map.Entry<Integer, LabelInformation > Label = vActiveLabelsIt.next();
        	int vLabel = Label.getKey();
            if (vLabel == aInitImage.forbiddenLabel)  {continue;}
            vScalings3.put(vLabel,new ArrayList<Float>());
            vLabelCounter.put(vLabel,0);
            vIntensitySum.put(vLabel,0.0f);
        }

		Cursor< FloatType > cVDevImage = DevImage.cursor();
		int size = aInitImage.getSize();
		for( int i = 0 ; i < size && cVDevImage.hasNext() ; i++)
		{
			cVDevImage.fwd();
			int vLabelAbs = aInitImage.getLabelAbs(i);
            if (vLabelAbs == aInitImage.forbiddenLabel) {continue;}
            vLabelCounter.put(vLabelAbs, vLabelCounter.get(vLabelAbs)+1);

            if (vLabelAbs == 0) 
            {
            	float vBG = aDataImage.get(i) - (cVDevImage.get().get() - (float)vOldBG);
                ArrayList<Float> arr = vScalings3.get(vLabelAbs);
                arr.add(vBG);
            } 
            else 
            {
            	float vScale = (aDataImage.get(i) - (float)vOldBG)/(cVDevImage.get().get() - (float)vOldBG);
                ArrayList<Float> arr = vScalings3.get(vLabelAbs);
                arr.add(vScale);
            }
        }

        /// For all the labels (except the BG ?) sort the scalar factors for all
        /// the pixel. The median is in the middle of the sorted list.
        /// TODO: check if fitting is necessary for the BG.
        /// TODO: Depending on the size of the region, sorting takes too long and
        ///       a median of medians algorithm (O(N)) could provide a good
        ///       approximation of the median.

        Iterator<Map.Entry<Integer, ArrayList<Float> > > vScaling3It = vScalings3.entrySet().iterator();
        while (vScaling3It.hasNext())
        {
        	Map.Entry<Integer, ArrayList<Float> > vLabel = vScaling3It.next();
            float vMedian;
            if (aInitImage.getLabelMap().get(vLabel.getKey()).count > 2)
            {
                vMedian = Median(vScalings3.get(vLabel.getKey()));
            }
            else 
            {
                vMedian = (float)aInitImage.getLabelMap().get(vLabel.getKey()).mean;
            }


            /// TESTING: REMOVE THE NEXT LINE
            //            vMedian = vIntensitySum[vLabelAbs] / vLabelCounter[vLabelAbs];

            /// Correct the old intensity values.
            if (vLabel.getKey() == 0) 
            {
                if (vMedian < 0) 
                {
                    vMedian = 0;
                    //                    if (this->GetDebug()) {
                    //                        std::cout <<
                    //                                "In itk::FrontsCompetitionImageFilter::RenewDeconvolutionStatistics: "
                    //                                << "BG intensity estimation negative. Reset to 0." << std::endl;
                    //                    }
                }
                aInitImage.getLabelMap().get(vLabel.getKey()).median = vMedian;
            }
            else
            {
            	aInitImage.getLabelMap().get(vLabel.getKey()).median =
                        (aInitImage.getLabelMap().get(vLabel.getKey()).median - vOldBG) * vMedian + aInitImage.getLabelMap().get(0).median;
            }

            //            std::cout << "Array read out in label " << vLabelAbs << " until " <<
            //                    vLabelCounter[vLabelAbs] << std::endl;
            //
            //            std::cout << "m_Intensities[" << vLabelAbs << "]: "
            //                    << m_Intensities[vLabelAbs] << "\tmedian: " << vMedian
            //                    << "\tcount[" << vLabelAbs << "] = " << m_Count[vLabelAbs] << std::endl;

        }

        /// Update the model image with the new intensities:
        /// - Remove the old BG value
        /// - Scale the signal with the correction factor (median from above)
        /// - Add the new BG value.
        //        ImageRegionIterator<InputImageType> vModelImageIt(m_DeconvolutionModelImage,
        //                m_DeconvolutionModelImage->GetBufferedRegion());
        //
        //        for (vLabelIt.GoToBegin(), vModelImageIt.GoToBegin(); !vModelImageIt.IsAtEnd();
        //                ++vLabelIt, ++vModelImageIt) {
        //            LabelAbsPixelType vLabelAbs = abs(vLabelIt.Get());
        //            InternalPixelType vNewValue = vModelImageIt.Get() - vOldBG;
        //            if (vLabelAbs != 0 && vLabelAbs != static_cast<unsigned int>(m_ForbiddenRegionLabel)) {
        //                vNewValue *= vMedians[vLabelAbs];
        //            }
        //            vNewValue += m_Intensities[0];
        //            vModelImageIt.Set(vNewValue);
        //        }

        //        m_Intensities[0] = 100;
        //        m_Intensities[1] = 250.0;
        //        m_Intensities[2] = 234.0;

        // The model image has to be renewed as well to match the new statistic values:
        GenerateModelImage(DevImage, aInitImage, m_PSF, aInitImage.getLabelMap());
	}


    public EnergyResult CalculateEnergyDifference(
    Point aIndex,
    ContourParticle contourParticle,
    int aToLabel)
    {

    	/**
    	 * Do not really subtract / add the scaled PSF to the 'ideal image'.
    	 * Example for the case where the energyDiff is calculated for a possible
    	 * change to the background (removal):
    	 * Calc (J' - I)^2  -  (J - I)^2
    	 * = (J - (c-BG) * PSF - I)^2 - (J - I)^2
    	 */

    	/// Define the region of influence:
    	
    	int aFromLabel = contourParticle.label;
    	long [] pixls =  new long [m_PSF.numDimensions()];
    	m_PSF.dimensions(pixls);

    	EnergyResult vEnergyDiff = new EnergyResult(0.0,false);
    	vEnergyDiff.energyDifference = 0.0;

    	float vIntensity_FromLabel = (float)labelMap.get(aFromLabel).median;
    	float vIntensity_ToLabel = (float)labelMap.get(aToLabel).median;

    	/// Iterate through the region and subtract the psf from the conv image.
    
    	Cursor< FloatType > vPSF = m_PSF.localizingCursor();
    	RandomAccess< FloatType > vModelIt = vModelImage.randomAccess();
    
    	long dimlen[] = new long [m_PSF.numDimensions()];;
    	m_PSF.dimensions(dimlen);
    
    	// middle coord
    
    	for (int i = 0 ; i < m_PSF.numDimensions() ; i++)	{dimlen[i] = dimlen[i] / 2;}
    	Point middle = new Point(dimlen);
    	middle.add(aIndex);
    
    	Point pos = new Point(dimlen);
    	int loc[] = new int [m_PSF.numDimensions()];
    
    	while (vPSF.hasNext())
    	{
    		vPSF.fwd();
    		// Add aindex to cursor
    	
    		pos.zero();    	
			vPSF.localize(loc);
			pos.add(new Point (loc));
    		
    		vModelIt.setPosition(pos.x);
    	
    		float vEOld = (vModelIt.get().get() - aDataImage.get(pos));
    		vEOld = vEOld * vEOld;
    		//            vEOld = fabs(vEOld);
    		float vENew = (vModelIt.get().get() - (vIntensity_FromLabel - vIntensity_ToLabel) *
                vPSF.next().get()) - aDataImage.get(pos);
    		vENew = vENew * vENew;

    		vEnergyDiff.energyDifference += vENew - vEOld;
    	}

    	return vEnergyDiff;
    }


    public void UpdateConvolvedImage(Point aIndex,
    		LabelImage aLabelImage,
    		int aFromLabel,
    		int aToLabel) 
    {
    	/**
    	 * Subtract a scaled psf from the ideal image
    	 */
    	/*    InputImageRegionType vRegion;
    InputImageSizeType vRegionSize = m_PSF->GetLargestPossibleRegion().GetSize();

    InputImageOffsetType vOffset;
    vOffset.Fill(vRegionSize[0] / 2); // we assume the region to be a hypercube.
    vRegion.SetSize(vRegionSize);
    vRegion.SetIndex(aIndex - vOffset);

    /// Move the PSF window such that the center is on aIndex
    InputImageRegionType vPSFRegion = m_PSF->GetLargestPossibleRegion().GetSize();
    vPSFRegion.SetIndex(vRegion.GetIndex());
    m_PSF->SetBufferedRegion(vPSFRegion);

    /// After the cropping at the data-image boundaries, vRegion will be the
    /// region to treat in the data-image space.
    vRegion.Crop(m_DeconvolutionModelImage->GetBufferedRegion());*/

    	/// Iterate through the region and subtract the psf from the conv image.
    
    	Cursor< FloatType > vPSF = m_PSF.localizingCursor();
    	RandomAccess< FloatType > vIt = DevImage.randomAccess();
    
    	long dimlen[] = new long [m_PSF.numDimensions()];
    	m_PSF.dimensions(dimlen);
    
    	// middle coord
    
    	for (int i = 0 ; i < m_PSF.numDimensions() ; i++)	{dimlen[i] = dimlen[i] / 2;}
    	Point middle = new Point(dimlen);
    	middle.add(aIndex);
    	
    	Point pos = new Point(m_PSF.numDimensions());
    	int loc[] = new int [m_PSF.numDimensions()];
    
    	if (aToLabel == 0)
    	{ // ...the point is removed and set to BG
    		// To avoid the operator map::[] in the loop:
    		float vIntensity_FromLabel = (float)aLabelImage.getLabelMap().get(aFromLabel).median;
    		float vIntensity_BGLabel = (float)aLabelImage.getLabelMap().get(aToLabel).median;
    		while (vPSF.hasNext())
    		{
    			vPSF.fwd();
            
    			pos.zero();
    			pos.add(middle);
    			
    			vPSF.localize(loc);
    			pos.add(new Point (loc));
        	
    			vIt.setPosition(pos.x);
        	
    			vIt.get().set(vIt.get().get() - (vIntensity_FromLabel - vIntensity_BGLabel) * vPSF.get().get());
    		}
    	}
    	else
    	{    		
    		float vIntensity_ToLabel = (float)aLabelImage.getLabelMap().get(aToLabel).median;
    		float vIntensity_BGLabel = (float)aLabelImage.getLabelMap().get(0).median;
    		while (vPSF.hasNext())
    		{
    			vIt.get().set(vIt.get().get() + (vIntensity_ToLabel - vIntensity_BGLabel) * vPSF.get().get());
    		}
    	}
    }


}

