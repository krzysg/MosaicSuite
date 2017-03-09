package mosaic.region_competition.DRS;


import org.apache.log4j.Logger;

import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.region_competition.energies.ImageModel;

public class AlgorithmDRS {

    private static final Logger logger = Logger.getLogger(AlgorithmDRS.class);

    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final IntensityImage iEdgeImage;
    private final ImageModel iImageModel;
    private final Settings iSettings;

    public AlgorithmDRS(IntensityImage aIntensityImage, LabelImage aLabelImage, IntensityImage aEdgeImage, ImageModel aModel, Settings aSettings) {
        logger.debug("DRS algorithm created");
        
        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iEdgeImage = aEdgeImage;
        iImageModel = aModel;
        iSettings = aSettings;
        
        logger.debug(mosaic.utils.Debug.getString(iIntensityImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iLabelImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iEdgeImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iImageModel.toString()));
        logger.debug(mosaic.utils.Debug.getString(iSettings.toString()));
        
        // Initialize label image
        iLabelImage.initBorder();
        iLabelImage.initContour();
        
        
    }

    public boolean performIteration() {
        boolean convergence = true;
        
        // TODO: real job strats here
        
        return convergence;
    }
    
    public int getBiggestLabel() {
        return 10;
    }
}
