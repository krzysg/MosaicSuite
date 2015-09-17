package mosaic.bregman.GUI;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Checkbox;
import java.awt.Font;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mosaic.bregman.Analysis;


public class ColocalizationGUI implements ItemListener, ChangeListener, TextListener {

    ImagePlus imgch1;
    ImagePlus imgch2;
    int ni, nj, nz, nc;

    JSlider t1, t2;
    JSlider tz1, tz2;// slider for z stack preview

    JLabel l1, lz1, l2, lz2;
    TextField v1, v2;
    Checkbox m1, m2;

    boolean init1 = false;
    boolean init2 = false;
    ImagePlus maska_im1, maska_im2;

    // max and min intensity values in channel 1 and 2
    double max = 0;
    double min = Double.POSITIVE_INFINITY;
    double max2 = 0;
    double min2 = Double.POSITIVE_INFINITY;

    double val1, val2;

    boolean fieldval = false;
    boolean sliderval = false;
    boolean boxval = false;
    boolean refreshing = false;

    double minrange = 0.001;
    double maxrange = 1;
    double logmin = Math.log10(minrange);
    double logspan = Math.log10(maxrange) - Math.log10(minrange);
    int maxslider = 1000;

    JLabel warning = new JLabel("");
    int ns1, ns2; // slices position of imgaes whenlaunched
    int posx, posy;

    public ColocalizationGUI(ImagePlus ch1, ImagePlus ch2, int ParentPosx, int ParentPosy) {
        imgch1 = ch1;
        imgch2 = ch2;

        posx = ParentPosx + 20;
        posy = ParentPosy + 20;

    }

    public void run(String arg) {
        Font bf = new Font(null, Font.BOLD, 12);

        GenericDialog gd = new GenericDialog("Cell masks");

        gd.setInsets(-10, 0, 3);
        gd.addMessage("Cell masks (two channels images)", bf);

        String sgroup3[] = { "Cell_mask_channel_1", "Cell_mask_channel_2" };
        boolean bgroup3[] = { false, false };

        bgroup3[0] = Analysis.p.usecellmaskX;
        bgroup3[1] = Analysis.p.usecellmaskY;

        t1 = new JSlider();
        t2 = new JSlider();

        tz1 = new JSlider();
        tz2 = new JSlider();

        l1 = new JLabel("threshold value   ");
        l2 = new JLabel("threshold value   ");

        lz1 = new JLabel("z position preview");
        lz2 = new JLabel("z position preview");

        gd.addCheckboxGroup(1, 2, sgroup3, bgroup3);

        gd.addNumericField("Threshold_channel_1 (0 to 1)", Analysis.p.thresholdcellmask, 4);
        Panel p1 = new Panel();
        p1.add(l1);
        p1.add(t1);
        gd.addPanel(p1);

        Panel pz1 = new Panel();
        pz1.add(lz1);
        pz1.add(tz1);
        gd.addPanel(pz1);

        gd.addNumericField("Threshold_channel_2 (0 to 1)", Analysis.p.thresholdcellmasky, 4);

        Panel p2 = new Panel();
        p2.add(l2);
        p2.add(t2);
        gd.addPanel(p2);

        Panel pz2 = new Panel();
        pz2.add(lz2);
        pz2.add(tz2);
        gd.addPanel(pz2);

        v1 = (TextField) gd.getNumericFields().elementAt(0);
        v2 = (TextField) gd.getNumericFields().elementAt(1);

        m1 = (Checkbox) gd.getCheckboxes().elementAt(0);
        m2 = (Checkbox) gd.getCheckboxes().elementAt(1);

        t1.addChangeListener(this);
        t2.addChangeListener(this);

        v1.addTextListener(this);
        v2.addTextListener(this);

        m1.addItemListener(this);
        m2.addItemListener(this);

        t1.setMinimum(0);
        t1.setMaximum(maxslider);

        t2.setMinimum(0);
        t2.setMaximum(maxslider);

        t1.setValue((int) logvalue(Analysis.p.thresholdcellmask));
        t2.setValue((int) logvalue(Analysis.p.thresholdcellmasky));

        val1 = new Double((v1.getText()));
        val2 = new Double((v2.getText()));

        if (imgch1 != null) {
            int nslices = imgch1.getNSlices();
            if (nslices > 1) {
                ns1 = imgch1.getSlice();
                tz1.setMinimum(1);
                tz1.setMaximum(nslices);
                tz1.setValue(1);
                tz1.addChangeListener(this);
            }
            else {
                tz1.setEnabled(false);
            }
        }
        else {
            tz1.setEnabled(false);
        }

        if (imgch2 != null) {
            int nslices = imgch2.getNSlices();
            if (nslices > 1) {
                ns2 = imgch2.getSlice();
                tz2.setMinimum(1);
                tz2.setMaximum(nslices);
                tz2.setValue(1);
                tz2.addChangeListener(this);
            }
            else {
                tz2.setEnabled(false);
            }
        }
        else {
            tz2.setEnabled(false);
        }

        if (Analysis.p.usecellmaskX && imgch1 != null) {
            maska_im1 = new ImagePlus();
            initpreviewch1(imgch1);
            previewBinaryCellMask(new Double((v1.getText())), imgch1, maska_im1, 1);
            init1 = true;

        }

        if (Analysis.p.usecellmaskY && imgch2 != null) {
            maska_im2 = new ImagePlus();
            initpreviewch2(imgch2);
            previewBinaryCellMask(new Double((v2.getText())), imgch2, maska_im2, 2);
            init2 = true;
        }

        gd.centerDialog(false);
        gd.setLocation(posx, posy);
        gd.showDialog();

        if (maska_im1 != null) {
            maska_im1.close();
        }
        if (maska_im2 != null) {
            maska_im2.close();
        }

        if (imgch1 != null) {
            imgch1.setSlice(ns1);
            imgch1 = null;
        }

        if (imgch2 != null) {
            imgch2.setSlice(ns2);
            imgch2 = null;
        }

        if (gd.wasCanceled()) {
            return;
        }

        Analysis.p.usecellmaskX = gd.getNextBoolean();
        Analysis.p.usecellmaskY = gd.getNextBoolean();
        Analysis.p.thresholdcellmask = gd.getNextNumber();
        Analysis.p.thresholdcellmasky = gd.getNextNumber();

    }

    private double expvalue(double slidervalue) {

        return (Math.pow(10, (slidervalue / maxslider) * logspan + logmin));

    }

    private double logvalue(double tvalue) {

        return (maxslider * (Math.log10(tvalue) - logmin) / logspan);

    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource(); // Identify checkbox that was clicked

        boxval = true;
        if (source == m1) {
            boolean b = m1.getState();
            if (b) {
                if (imgch1 != null) {
                    if (maska_im1 == null) {
                        maska_im1 = new ImagePlus();
                    }
                    initpreviewch1(imgch1);
                    previewBinaryCellMask(new Double((v1.getText())), imgch1, maska_im1, 1);
                    init1 = true;
                }
                else {
                    warning.setText("Please open an image first.");
                }// not used anymore, needed ?
            }
            else {
                // hide and clean
                if (maska_im1 != null) {
                    maska_im1.hide();
                }
                // maska_im1=null;
                init1 = false;
            }
        }

        if (source == m2) {
            boolean b = m2.getState();
            if (b) {
                if (imgch2 != null) {
                    if (maska_im2 == null) {
                        maska_im2 = new ImagePlus();
                    }
                    initpreviewch2(imgch2);
                    previewBinaryCellMask(new Double((v2.getText())), imgch2, maska_im2, 2);
                    init2 = true;
                }
                else {
                    warning.setText("Please open an image with two channels first.");// not used anymore
                }
            }
            else {
                // close and clean
                if (maska_im2 != null) {
                    maska_im2.hide();
                }
                // maska_im2.close();
                // maska_im2=null;
                init2 = false;
            }

        }
        // IJ.log("boxval to false");
        boxval = false;
    }

    @Override
    public void textValueChanged(TextEvent e) {
        Object source = e.getSource();

        if (!boxval && !sliderval) {// prevents looped calls
            fieldval = true;
            if (source == v1 && init1) {
                double v = new Double((v1.getText()));
                if (!sliderval && val1 != v && !refreshing) {
                    val1 = v;
                    previewBinaryCellMask(v, imgch1, maska_im1, 1);
                    int vv = (int) (logvalue(v));
                    t1.setValue(vv);

                }

            }
            else if (source == v2 && init2) {
                double v = new Double((v2.getText()));
                if (!sliderval && val2 != v && !refreshing) {
                    val2 = v;
                    previewBinaryCellMask(v, imgch2, maska_im2, 2);
                    int vv = (int) (logvalue(v));
                    t2.setValue(vv);

                }
            }
            else if (source == v1 && !init1) {
                double v = new Double((v1.getText()));
                if (!sliderval) {

                    int vv = (int) (logvalue(v));
                    t1.setValue(vv);

                }

            }
            else if (source == v2 && !init2) {
                double v = new Double((v2.getText()));
                if (!sliderval) {

                    int vv = (int) (logvalue(v));
                    t2.setValue(vv);

                }
            }

            fieldval = false;
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        Object origin = e.getSource();

        if (origin == tz1 && maska_im1 != null) {
            maska_im1.setZ(tz1.getValue());
            imgch1.setZ(tz1.getValue());
            return;
        }

        if (origin == tz2 && maska_im2 != null) {
            maska_im2.setZ(tz2.getValue());
            imgch2.setZ(tz2.getValue());
            return;
        }

        if (!boxval && !fieldval) {// prevents looped calls
            sliderval = true;
            if (origin == t1 && init1 && !t1.getValueIsAdjusting()) {
                double value = t1.getValue();
                double vv = expvalue(value);

                if (!fieldval && val1 != vv) {
                    v1.setText(String.format(Locale.US, "%.4f", vv));
                    val1 = vv;
                    previewBinaryCellMask(vv, imgch1, maska_im1, 1);
                }
                refreshing = false;
            }

            if ((origin == t1 && init1 && t1.getValueIsAdjusting()) || (origin == t1 && !init1)) {
                double value = t1.getValue();
                double vv = expvalue(value);
                if (!fieldval) {
                    v1.setText(String.format(Locale.US, "%.4f", vv));
                }
                refreshing = true;
            }

            if (origin == t2 && init2 && !t2.getValueIsAdjusting()) {
                double value = t2.getValue();
                double vv = expvalue(value);
                if (!fieldval && val2 != vv) {
                    v2.setText(String.format(Locale.US, "%.4f", vv));
                    previewBinaryCellMask(vv, imgch2, maska_im2, 2);
                    val2 = vv;
                }
                refreshing = false;
            }

            if ((origin == t2 && init2 && t2.getValueIsAdjusting()) || (origin == t2 && !init2)) {
                double value = t2.getValue();
                double vv = expvalue(value);
                if (!fieldval) {
                    v2.setText(String.format(Locale.US, "%.4f", vv));
                }
                refreshing = true;
            }

            sliderval = false;

        }

    }

    // find min and max values in channel 1
    private void initpreviewch1(ImagePlus img) {

        ni = img.getWidth();
        nj = img.getHeight();
        nz = img.getNSlices();

        ImageProcessor imp;
        for (int z = 0; z < nz; z++) {
            img.setSlice(z + 1);
            imp = img.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (imp.getPixel(i, j) > max) {
                        max = imp.getPixel(i, j);
                    }
                    if (imp.getPixel(i, j) < min) {
                        min = imp.getPixel(i, j);
                    }
                }
            }
        }

    }

    // find min and max values in channel 2
    private void initpreviewch2(ImagePlus img) {

        ni = img.getWidth();
        nj = img.getHeight();
        nz = img.getNSlices();

        ImageProcessor imp;
        for (int z = 0; z < nz; z++) {
            img.setSlice(z + 1);
            imp = img.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (imp.getPixel(i, j) > max2) {
                        max2 = imp.getPixel(i, j);
                    }
                    if (imp.getPixel(i, j) < min2) {
                        min2 = imp.getPixel(i, j);
                    }
                }
            }
        }
    }

    // compute and display cell mask
    private void previewBinaryCellMask(double threshold_i, ImagePlus img, ImagePlus maska_im, int channel) {

        int ns = img.getSlice();
        double threshold;

        ImageProcessor imp;

        if (channel == 1) {
            threshold = threshold_i * (max - min) + min;
        }
        else {
            threshold = threshold_i * (max2 - min2) + min2;
        }

        ImageStack maska_ims = new ImageStack(ni, nj);

        for (int z = 0; z < nz; z++) {
            img.setSlice(z + 1);
            imp = img.getProcessor();
            byte[] maska_bytes = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (imp.getPixel(i, j) > threshold) {
                        maska_bytes[j * ni + i] = (byte) 255;
                    }
                    else {
                        maska_bytes[j * ni + i] = 0;
                    }

                }
            }
            ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(maska_bytes);
            maska_ims.addSlice("", bp);
        }

        maska_im.setStack("Cell mask channel " + channel, maska_ims);

        IJ.run(maska_im, "Invert", "stack");
        IJ.run(maska_im, "Fill Holes", "stack");
        IJ.run(maska_im, "Open", "stack");
        IJ.run(maska_im, "Invert", "stack");

        maska_im.updateAndDraw();
        maska_im.changes = false;

        maska_im.show();
        img.setSlice(ns);
    }
}
