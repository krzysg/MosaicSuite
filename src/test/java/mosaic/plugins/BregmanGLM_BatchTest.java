package mosaic.plugins;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import ij.macro.Interpreter;
import mosaic.test.framework.CommonBase;


public class BregmanGLM_BatchTest extends CommonBase {
    
    @Test
    public void testPsfCircleLowIntensity()  {
        
        // Define test data
        final String tcDirName           = "Squassh/psfCircleLowIntensity/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "psf.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] expectedFiles     = {"__ImageData.csv/psf_ImageData.csv", "__ObjectData.csv/psf_ObjectData.csv"};
        final String[] referenceFiles    = {"__ImageData.csv/psf_ImageData.csv", "__ObjectData.csv/psf_ObjectData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    // Temporarily turned off. Some tolerance on output should be introduced. It happens that estimateIntensityClustering() in AnalysePatch gives slightly
    // different results (Random based algorithm and there is no way to provide random seed).
    @Test
    @org.junit.Ignore
    public void testPsfCircleMediumIntensity()  {
        
        // Define test data
        final String tcDirName           = "Squassh/psfCircleMediumIntensity/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "psf.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/psf_outline_overlay_c1.zip", "__intensities_c1.zip/psf_intensities_c1.zip",
                                            "__mask_c1.zip/psf_mask_c1.zip", "__seg_c1.zip/psf_seg_c1.zip"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/psf_ObjectsData_c1.csv", "__ImagesData.csv/psf_ImagesData.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/psf_ObjectsData_c1.csv", "__ImagesData.csv/psf_ImagesData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testTest2dFull()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Test2dFull/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "test2d.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip", "__intensities_c1.zip/test2d_intensities_c1.zip",
                                            "__mask_c1.zip/test2d_mask_c1.zip", "__seg_c1.zip/test2d_seg_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip", "__intensities_c1.zip/test2d_intensities_c1.zip",
                                            "__mask_c1.zip/test2d_mask_c1.zip", "__seg_c1.zip/test2d_seg_c1.zip"};
        final String[] expectedFiles     = {"__ImageData.csv/test2d_ImageData.csv", "__ObjectData.csv/test2d_ObjectData.csv"};
        final String[] referenceFiles    = {"__ImageData.csv/test2d_ImageData.csv", "__ObjectData.csv/test2d_ObjectData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testTest2ChannelsWithMaskFull()  {
        
        // Define test data
        final String tcDirName           = "Squassh/2channelsWithMasks/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "moImg.tif";
        final String[] expectedImgFiles  = {"__coloc.zip/moImg_ch_0_1_coloc.zip", 
                                            "__intensities_c1.zip/moImg_intensities_c1.zip", "__intensities_c2.zip/moImg_intensities_c2.zip", 
                                            "__mask_c1.zip/moImg_mask_c1.zip", "__mask_c2.zip/moImg_mask_c2.zip", 
                                            "__outline_overlay_c1.zip/moImg_outline_overlay_c1.zip", "__outline_overlay_c2.zip/moImg_outline_overlay_c2.zip",
                                            "__seg_c1.zip/moImg_seg_c1.zip", "__seg_c2.zip/moImg_seg_c2.zip"};
        final String[] referenceImgFiles = {"__coloc.zip/moImg_coloc.zip", 
                                            "__intensities_c1.zip/moImg_intensities_c1.zip", "__intensities_c2.zip/moImg_intensities_c2.zip", 
                                            "__mask_c1.zip/moImg_mask_c1.zip", "__mask_c2.zip/moImg_mask_c2.zip", 
                                            "__outline_overlay_c1.zip/moImg_outline_overlay_c1.zip", "__outline_overlay_c2.zip/moImg_outline_overlay_c2.zip",
                                            "__seg_c1.zip/moImg_seg_c1.zip", "__seg_c2.zip/moImg_seg_c2.zip"};
        final String[] expectedFiles     = {"__ImageColoc.csv/moImg_ImageColoc.csv", "__ImageData.csv/moImg_ImageData.csv", "__ObjectColoc.csv/moImg_ObjectColoc.csv", "__ObjectData.csv/moImg_ObjectData.csv"};
        final String[] referenceFiles     = {"__ImageColoc.csv/moImg_ImageColoc.csv", "__ImageData.csv/moImg_ImageData.csv", "__ObjectColoc.csv/moImg_ObjectColoc.csv", "__ObjectData.csv/moImg_ObjectData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testSphereSmall3D()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Sphere3D/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "sphereSmall.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/sphereSmall_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/sphereSmall_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ImageData.csv/sphereSmall_ImageData.csv", "__ObjectData.csv/sphereSmall_ObjectData.csv"};
        final String[] referenceFiles    = {"__ImageData.csv/sphereSmall_ImageData.csv", "__ObjectData.csv/sphereSmall_ObjectData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testVideo3D()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Video3D/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "dropletVideoSmall.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/dropletVideoSmall_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/dropletVideoSmall_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ImageData.csv/dropletVideoSmall_ImageData.csv", "__ObjectData.csv/dropletVideoSmall_ObjectData.csv"};
        final String[] referenceFiles    = {"__ImageData.csv/dropletVideoSmall_ImageData.csv", "__ObjectData.csv/dropletVideoSmall_ObjectData.csv"};
        
        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testScriptR()  {
        
        // Define test data
        final String tcDirName           = "Squassh/ScriptR/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/1 Ctrl 1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA4_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA4_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/1 Ctrl 1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/1 Ctrl 3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/2 OA4_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl2_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/3 Ctrl3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA1_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA3_outline_overlay_c1.zip", 
                                            "__outline_overlay_c1.zip/4 OA4_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"stitch__ImageColoc.csv", "stitch__ImageData.csv", "stitch__ObjectColoc.csv", "stitch__ObjectData.csv", "R_analysis.R"};
        final String[] referenceFiles    = {"stitch__ImageColoc.csv", "stitch__ImageData.csv", "stitch__ObjectColoc.csv", "stitch__ObjectData.csv", "R_analysis.R"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("1 Ctrl 1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("1 Ctrl 2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("1 Ctrl 3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("2 OA4.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("3 Ctrl3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("4 OA4.tif", getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testColocalization()  {
        
        // Define test data
        final String tcDirName           = "Squassh/Colocalization/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json";
        final String inputFile           = "coloc_test.tif";
        final String[] expectedImgFiles  = {"coloc_test_intensities_c1.zip",
                                            "coloc_test_intensities_c2.zip",
                                            "coloc_test_mask_c1.zip",
                                            "coloc_test_mask_c2.zip",
                                            "coloc_test_outline_overlay_c1.zip",
                                            "coloc_test_outline_overlay_c2.zip",
                                            "coloc_test_seg_c1.zip",
                                            "coloc_test_seg_c2.zip"};
        final String[] referenceImgFiles = {"__intensities_c1.zip/coloc_test_intensities_c1.zip",
                                            "__intensities_c2.zip/coloc_test_intensities_c2.zip",
                                            "__mask_c1.zip/coloc_test_mask_c1.zip",
                                            "__mask_c2.zip/coloc_test_mask_c2.zip",
                                            "__outline_overlay_c1.zip/coloc_test_outline_overlay_c1.zip",
                                            "__outline_overlay_c2.zip/coloc_test_outline_overlay_c2.zip",
                                            "__seg_c1.zip/coloc_test_seg_c1.zip",
                                            "__seg_c2.zip/coloc_test_seg_c2.zip"};
        final String[] expectedFiles     = {"__ImageColoc.csv/coloc_test_ImageColoc.csv", "__ImageData.csv/coloc_test_ImageData.csv", "__ObjectColoc.csv/coloc_test_ObjectColoc.csv", "__ObjectData.csv/coloc_test_ObjectData.csv"};
        final String[] referenceFiles     = {"__ImageColoc.csv/coloc_test_ImageColoc.csv", "__ImageData.csv/coloc_test_ImageData.csv", "__ObjectColoc.csv/coloc_test_ObjectColoc.csv", "__ObjectData.csv/coloc_test_ObjectData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
//    @org.junit.Ignore
    public void testCluster()  {
        
        // Define test data
        final String tcDirName           = "Squassh/cluster/";
        final String setupString         = "run";
        final String macroOptions        = "process config=/tmp/spb_settings.json username=" + System.getProperty("user.name");
        final String inputFile           = null;
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/droplet_1_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_2_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_3_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_4_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_5_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_6_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/droplet_1_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_2_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_3_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_4_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_5_outline_overlay_c1.zip",
                                            "__outline_overlay_c1.zip/droplet_6_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ObjectData.csv/droplet_1_ObjectData.csv",
                                            "__ObjectData.csv/droplet_2_ObjectData.csv",
                                            "__ObjectData.csv/droplet_3_ObjectData.csv",
                                            "__ObjectData.csv/droplet_4_ObjectData.csv",
                                            "__ObjectData.csv/droplet_5_ObjectData.csv",
                                            "__ObjectData.csv/droplet_6_ObjectData.csv"};
        final String[] referenceFiles    = {"__ObjectData.csv/droplet_1_ObjectData.csv",
                                            "__ObjectData.csv/droplet_2_ObjectData.csv",
                                            "__ObjectData.csv/droplet_3_ObjectData.csv",
                                            "__ObjectData.csv/droplet_4_ObjectData.csv",
                                            "__ObjectData.csv/droplet_5_ObjectData.csv",
                                            "__ObjectData.csv/droplet_6_ObjectData.csv"};
        
        // Create tested plugIn
        Interpreter.batchMode = true;
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("droplet_1.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_2.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_3.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_4.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_5.tif", getTestDataPath() + tcDirName, tmpPath);
        copyTestResources("droplet_6.tif", getTestDataPath() + tcDirName, tmpPath);
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   null, null, null, null);
                   
        File dataDir = new File(getTestDataPath() + tcDirName);
        File testDir = new File(tmpPath);
        
        // compare output from plugin with reference images
        for (int i = 0; i < expectedImgFiles.length; ++i) {
            final File refJobFile = findJobFile(referenceImgFiles[i], dataDir);
            assertTrue("Reference file [" + dataDir + "/" + referenceImgFiles[i] + "] not found!", refJobFile != null);
            String refFile = refJobFile.getAbsoluteFile().toString();
            final File testJobFile = findJobFile(expectedImgFiles[i], testDir);
            assertTrue("Test file [" + testDir + "/" + expectedImgFiles[i] + "] not found!", testJobFile != null);
            String testFile = testJobFile.getAbsoluteFile().toString();
            testFile = "./" + testFile.substring(tmpPath.length(), testFile.length());
            compareImageFromIJ(refFile, testFile);
        }

        for (int i = 0; i < expectedFiles.length; ++i) {
            File refJobFile = findJobFile(referenceFiles[i], dataDir);
            assertTrue("Reference file [" + dataDir + "/" + referenceFiles[i] + "] not found!", refJobFile != null);
            String refFile = refJobFile.getAbsoluteFile().toString();
            File testJobFile = findJobFile(expectedFiles[i], testDir);
            assertTrue("Test file [" + testDir + "/" + expectedFiles[i] + "] not found!", testJobFile != null);
            String testFile = testJobFile.getAbsoluteFile().toString();
            compareCsvFiles(refFile, testFile);
        }
    }

    @Test
//    @org.junit.Ignore
    public void testClusterShort()  {

        // Define test data
        final String tcDirName           = "Squassh/test2dCluster/";
        final String setupString         = "run";
        final String macroOptions        = "config=/tmp/spb_settings.json process username=" + System.getProperty("user.name");
        final String inputFile           = "test2d.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip"};
        final String[] expectedFiles     = {"__ImageData.csv/test2d_ImageData.csv", "__ObjectData.csv/test2d_ObjectData.csv"};
        final String[] referenceFiles    = {"__ImageData.csv/test2d_ImageData.csv", "__ObjectData.csv/test2d_ObjectData.csv"};


        // Create tested plugIn
        Interpreter.batchMode = true;
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");

        // Test it
        testPlugin(plugin, tcDirName,
                macroOptions, 
                setupString, inputFile,
                null, null, null, null);

        File dataDir = new File(getTestDataPath() + tcDirName);
        File testDir = new File(tmpPath);

        // compare output from plugin with reference images
        for (int i = 0; i < expectedImgFiles.length; ++i) {
            final File refJobFile = findJobFile(referenceImgFiles[i], dataDir);
            assertTrue("Reference file [" + dataDir + referenceImgFiles[i] + "] not found!", refJobFile != null);
            String refFile = refJobFile.getAbsoluteFile().toString();
            final File testJobFile = findJobFile(expectedImgFiles[i], testDir);
            assertTrue("Test file [" + testDir + "/" + expectedImgFiles[i] + "] not found!", testJobFile != null);
            String testFile = testJobFile.getAbsoluteFile().toString();
            testFile = "./" + testFile.substring(tmpPath.length(), testFile.length());
            compareImageFromIJ(refFile, testFile);
        }

        for (int i = 0; i < expectedFiles.length; ++i) {
            String refFile = findJobFile(referenceFiles[i], dataDir).getAbsoluteFile().toString();
            String testFile = findJobFile(expectedFiles[i], testDir).getAbsoluteFile().toString();
            compareCsvFiles(refFile, testFile);
        }
    }
    
    
    @Test
    public void testFullMacroOnlyParameters() {
        // Define test data
        final String tcDirName           = "Squassh/Test2dFullMacro/";
        final String setupString         = "run";
        final String macroOptions        = "remove_background rolling_ball_window_size_(in_pixels)=22 regularization_(>0)_ch1=0.111 regularization_(>0)_ch2=0.222 minimum_object_intensity_channel_1_(0_to_1)=0.033 _channel_2_(0_to_1)=0.044 exclude_z_edge standard_deviation_xy=0.61 standard_deviation_z=1.01 remove_region_with_intensities_<=1 remove_region_with_size_<=1 local_intensity_estimation=Automatic noise_model=Poisson threshold_channel_1=0.0012 threshold_channel_2=0.0013 colored_objects objects_intensities labeled_objects outlines_overlay soft_mask save_objects_characteristics number=1";
        final String inputFile           = "test2d.tif";
        final String[] expectedImgFiles  = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip", "__intensities_c1.zip/test2d_intensities_c1.zip",
                                            "__mask_c1.zip/test2d_mask_c1.zip", "__seg_c1.zip/test2d_seg_c1.zip"};
        final String[] referenceImgFiles = {"__outline_overlay_c1.zip/test2d_outline_overlay_c1.zip", "__intensities_c1.zip/test2d_intensities_c1.zip",
                                            "__mask_c1.zip/test2d_mask_c1.zip", "__seg_c1.zip/test2d_seg_c1.zip"};
        final String[] expectedFiles     = {"__ImageData.csv/test2d_ImageData.csv", "__ObjectData.csv/test2d_ObjectData.csv"};
        final String[] referenceFiles    = {"__ImageData.csv/test2d_ImageData.csv", "__ObjectData.csv/test2d_ObjectData.csv"};

        // Create tested plugIn
        final BregmanGLM_Batch plugin = new BregmanGLM_Batch();
        
        // Copy fake config to check if it is *not* used
        copyTestResources("spb_settings.json", getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
}
