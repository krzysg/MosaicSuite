package mosaic.core.utils;

import mosaic.plugins.PlugInFilterExt;
import ij.ImagePlus;

/**
 * 
 * This interface define the function that a segmentation algorithm should
 * expose
 * 
 * @author Pietro Incardona
 *
 */


public interface Segmentation extends PlugInFilterExt
{
	/**
	 * 
	 * Get Mask images name output
	 * 
	 * @param aImp image
	 * @return set of possible output
	 */
	
	String[] getMask(ImagePlus imp);
	
	/**
	 * 
	 * Get CSV regions list name output
	 * 
	 * @param aImp image
	 * @return set of possible output
	 */
	
	String[] getRegionList(ImagePlus imp);
	
	/**
	 * 
	 * Close all windows and images produced
	 * 
	 */
	
	void closeAll();
	
	/**
	 * 
	 * Get the name of the segmentation plugin
	 * 
	 * @return the name of the segmentation plugin
	 * 
	 */
	
	String getName();
};