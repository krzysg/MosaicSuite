package mosaic.ia;


import java.io.File;
import java.util.Vector;

import org.scijava.vecmath.Point3d;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;

import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.io.Opener;
import mosaic.ia.gui.Utils;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;

/**
 * Contains some utils for IA for reading files (images and CSV).
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FileUtils {
    /**
     * Opens dialog for user to choose a file
     * @return chosen file or null if operation cancelled
     */
    public static ImagePlus openImage() {
        return new Opener().openImage("");
    }

    /**
     * Wrapper used for reading data for Point3d class, it provides 'interface' methods for CSV reader
     */
    public static class Point3dCsvReadWrapper extends Point3d {
        private static final long serialVersionUID = 1L;
        
        public void setXX(double v) {this.x = v;}
        public double getXX() {return this.x;}
        public void setYY(double v) {this.y = v;}
        public double getYY() {return this.y;}
        public void setZZ(double v) {this.z = v;}
        public double getZZ() {return this.z;}
        
        public static CsvColumnConfig getConfig2D() { return new CsvColumnConfig(new String[]{"XX","YY"}, new CellProcessor[] { new ParseDouble(), new ParseDouble() }); }
        public static CsvColumnConfig getConfig3D() { return new CsvColumnConfig(new String[]{"XX","YY","ZZ"}, new CellProcessor[] { new ParseDouble(), new ParseDouble(), new ParseDouble() }); }
    }
    
    /**
     * Reads coordinates from CSV file, first opening dialog for user to choose it.
     * @param aTitle Title for opened dialog
     * @return read points
     */
    public static Point3d[] openCsvFile(String aTitle) {
        return openCsvFile(aTitle, null);
    }
    
    /**
     * Reads coordinates from CSV file.
     * @param aTitle Title for opened dialog (only if provided file name is null)
     * @param aFileName - absolute filename 
     * @return read points
     */
    public static Point3d[] openCsvFile(String aTitle, String aFileName) {
        File file = null;
        if (aFileName == null) {
            // Let user choose a input file
            final OpenDialog od = new OpenDialog(aTitle);
            if (od.getDirectory() == null || od.getFileName() == null) {
                return null;
            }
            file = new File(od.getDirectory() + od.getFileName());
        }
        else {
            file = new File(aFileName);
        }
        if (!file.exists()) {
            Utils.messageDialog("IA - open CSV file", "There is no file [" + file.getName() + "]");
            return null;
        }
        
        // Read it and handle errors
        CSV<Point3dCsvReadWrapper>csv = new CSV<Point3dCsvReadWrapper>(Point3dCsvReadWrapper.class);
        int numOfCols = csv.setCSVPreferenceFromFile(file.getAbsolutePath());
        if (numOfCols <= 1 || numOfCols > 3) {
            Utils.messageDialog("IA - open CSV file", "CSV file should have 2 or 3 columns of data with comma or semicolon delimieters and no header!");
            return null;
        }
        CsvColumnConfig ccc = numOfCols == 2 
                              ? Point3dCsvReadWrapper.getConfig2D()
                              : Point3dCsvReadWrapper.getConfig3D();
        final Vector<Point3dCsvReadWrapper> outdst = csv.Read(file.getAbsolutePath(), ccc, true);
        if (outdst.isEmpty()) {
            Utils.messageDialog("IA - open CSV file", "Incorrect CSV file chosen [" + file.getName() + "]\nCSV file should have 2 or 3 columns of data with comma or semicolon delimieters and no header!\n");
            return null;
        }
        
        // Return as a array
        return outdst.toArray(new Point3d[outdst.size()]);
    }
}
