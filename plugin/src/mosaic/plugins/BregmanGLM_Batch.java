package mosaic.plugins;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.process.ImageProcessor;
import mosaic.bregman.Analysis;
import mosaic.bregman.Parameters;
import mosaic.bregman.GUI.GenericGUI;
import mosaic.bregman.output.CSVOutput;
import mosaic.core.psf.psf;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.SerializedDataFile;
import net.imglib2.type.numeric.real.DoubleType;

public class BregmanGLM_Batch implements Segmentation {
    private static final Logger logger = Logger.getLogger(BregmanGLM_Batch.class);
    
    private String savedSettings;
    private GenericGUI window;
    private boolean gui_use_cluster = false;

    public static boolean test_mode = false;
    public static String test_path = null;
    
    @Override
    public int setup(String arg0, ImagePlus active_img) {
        // init basic structure
        Analysis.init();

        // if is a macro get the arguments from macro arguments
        if (IJ.isMacro()) {
            arg0 = Macro.getOptions();
            if (arg0 == null) {
                arg0 = "";
            }
        }

        final String dir = IJ.getDirectory("temp");
        savedSettings = dir + "spb_settings.dat";
        Analysis.p = getConfigHandler().LoadFromFile(savedSettings, Parameters.class, Analysis.p);

        final String path = MosaicUtils.parseString("config", arg0);
        if (path != null) {
            Analysis.p = getConfigHandler().LoadFromFile(path, Parameters.class, Analysis.p);
        }

        String normmin = MosaicUtils.parseString("min", arg0);
        if (normmin != null) {
            Analysis.norm_min = Double.parseDouble(normmin);
            System.out.println("min norm " + Analysis.norm_min);
        }

        String normmax = MosaicUtils.parseString("max", arg0);
        if (normmax != null) {
            Analysis.norm_max = Double.parseDouble(normmax);
            System.out.println("max norm " + Analysis.norm_max);
        }

        // Initialize CSV format
        CSVOutput.initCSV(Analysis.p.oc_s);

        // Check the argument
        final boolean batch = GraphicsEnvironment.isHeadless();

        if (batch == true) {
            Analysis.p.dispwindows = false;
        }
        logger.debug("isHeadless = " + batch);
        logger.debug("gui_use_cluster = " + gui_use_cluster);
        logger.debug("settings dir = [" + dir + "]");
        logger.debug("config path = [" + path + "]");
        logger.debug("norm min = [" + normmin + "]");
        logger.debug("norm max = [" + normmax + "]");
        logger.debug("input img = [" + (active_img != null ? active_img.getTitle() : "<no img>") + "]");
        
        window = new GenericGUI(batch, active_img);
        window.setUseCluster(gui_use_cluster);
        window.run(active_img);

        saveConfig(savedSettings, Analysis.p);

        // Re-set the arguments
        Macro.setOptions(arg0);

        return DONE;
    }

    @Override
    public void run(ImageProcessor imp) {}

    /**
     * Returns handler for (un)serializing Parameters objects.
     */
    public static DataFile<Parameters> getConfigHandler() {
        return new SerializedDataFile<Parameters>();
    }

    /**
     * Saves Parameters objects with additional handling of unserializable PSF
     * object. TODO: It (PSF) should be verified and probably corrected.
     *
     * @param aFullFileName - absolute path and file name
     * @param aParams - object to be serialized
     */
    public static void saveConfig(String aFullFileName, Parameters aParams) {
        // Nullify PSF since it is not Serializable
        final psf<DoubleType> tempPsf = aParams.PSF;
        aParams.PSF = null;

        getConfigHandler().SaveToFile(aFullFileName, aParams);

        aParams.PSF = tempPsf;
    }

    // =================== Stuff below is used only by tests

    /**
     * Unfortunately where is not way to hide the GUI in test mode, set the
     * plugin to explicitly bypass the GUI
     */
    public void bypass_GUI() {
        GenericGUI.bypass_GUI = true;
    }

    public void setUseCluster(boolean bl) {
        gui_use_cluster = bl;
    }

    // =================== Implementation of Segmentation interface

    private enum outputF {
        MASK(2), OBJECT(0);

        private final int numVal;

        outputF(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }
    
    /**
     * Get Mask images name output
     *
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getMask(ImagePlus aImp) {
        final String[] gM = new String[2];
        gM[0] = new String(Analysis.out[outputF.MASK.getNumVal()].replace("*", "_") + File.separator
                + Analysis.out[outputF.MASK.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        gM[1] = new String(Analysis.out[outputF.MASK.getNumVal() + 1].replace("*", "_") + File.separator
                + Analysis.out[outputF.MASK.getNumVal() + 1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        return gM;
    }

    /**
     * Get CSV regions list name output
     *
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getRegionList(ImagePlus aImp) {
        final String[] gM = new String[4];
        gM[0] = new String(Analysis.out[outputF.OBJECT.getNumVal()].replace("*", "_") + File.separator + Analysis.out[outputF.OBJECT.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        gM[1] = new String(Analysis.out[outputF.OBJECT.getNumVal() + 1].replace("*", "_") + File.separator + Analysis.out[outputF.OBJECT.getNumVal() + 1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));

        // This is produced if there is a stitch operation
        gM[2] = new String(MosaicUtils.removeExtension(aImp.getTitle()) + Analysis.out[outputF.OBJECT.getNumVal()].replace("*", "_"));
        gM[3] = new String(MosaicUtils.removeExtension(aImp.getTitle()) + Analysis.out[outputF.OBJECT.getNumVal() + 1].replace("*", "_"));

        return gM;
    }

    /**
     * Get name of the plugins
     */
    @Override
    public String getName() {
        return "Squassh";
    }
}
