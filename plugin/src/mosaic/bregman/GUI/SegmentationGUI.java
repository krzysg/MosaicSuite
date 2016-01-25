package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import mosaic.bregman.Analysis;


class SegmentationGUI {
    static int getParameters() {
        final Font bf = new Font(null, Font.BOLD, 12);

        final GenericDialog gd = new GenericDialog("Segmentation options");
        gd.setInsets(-10, 0, 3);
        gd.addMessage("    Segmentation parameters ", bf);

        final Button help_b = new Button("help");
        help_b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final Point p = gd.getLocationOnScreen();
                new SegmentationGUIHelp(p.x, p.y);
            }
        });

        final Panel pp = new Panel();
        pp.add(help_b);
        gd.addPanel(pp);

        gd.addNumericField("Regularization_(>0)_ch1", Analysis.iParams.lreg_[0], 3);
        gd.addNumericField("Regularization_(>0)_ch2", Analysis.iParams.lreg_[1], 3);

        gd.addNumericField("Minimum_object_intensity_channel_1_(0_to_1)", Analysis.iParams.min_intensity, 3);
        gd.addNumericField("                        _channel_2_(0_to_1)", Analysis.iParams.min_intensityY, 3);

        gd.addCheckbox("Subpixel_segmentation", Analysis.iParams.subpixel);
        gd.addCheckbox("Exclude_Z_edge", Analysis.iParams.exclude_z_edges);

        final String choice1[] = { "Automatic", "Low", "Medium", "High" };
        gd.addChoice("Local_intensity_estimation ", choice1, choice1[Analysis.iParams.mode_intensity]);

        final String choice2[] = { "Poisson", "Gauss" };
        gd.addChoice("Noise_Model ", choice2, choice2[Analysis.iParams.noise_model]);

        gd.addMessage("PSF model (Gaussian approximation)", bf);

        gd.addNumericField("standard_deviation_xy (in pixels)", Analysis.iParams.sigma_gaussian, 2);
        gd.addNumericField("standard_deviation_z  (in pixels)", Analysis.iParams.sigma_gaussian / Analysis.iParams.zcorrec, 2);

        gd.addMessage("Region filter", bf);
        gd.addNumericField("Remove_region_with_intensities_<", Analysis.iParams.min_region_filter_intensities, 0);

        Panel p = new Panel();
        final Button b = new Button("Patch position");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final OpenDialog od = new OpenDialog("(Patch file", "");
                final String directory = od.getDirectory();
                final String name = od.getFileName();
                if (directory != null && name != null) Analysis.iParams.patches_from_file = directory + name;
            }

        });
        p.add(b);
        gd.addPanel(p);

        final Button bp = new Button("Estimate PSF from objective properties");
        bp.addActionListener(new PSFOpenerActionListener(gd));
        p = new Panel();
        p.add(bp);
        gd.addPanel(p);

        gd.centerDialog(false);

        if (GenericGUI.bypass_GUI == false) {
            gd.showDialog();
            if (gd.wasCanceled()) {
                return -1;
            }

            Analysis.iParams.lreg_[0] = gd.getNextNumber();
            Analysis.iParams.lreg_[1] = gd.getNextNumber();
            Analysis.iParams.min_intensity = gd.getNextNumber();
            Analysis.iParams.min_intensityY = gd.getNextNumber();
            Analysis.iParams.subpixel = gd.getNextBoolean();
            Analysis.iParams.exclude_z_edges = gd.getNextBoolean();
            Analysis.iParams.sigma_gaussian = gd.getNextNumber();
            Analysis.iParams.zcorrec = Analysis.iParams.sigma_gaussian / gd.getNextNumber();
            Analysis.iParams.min_region_filter_intensities = gd.getNextNumber();
            Analysis.iParams.mode_intensity = gd.getNextChoiceIndex();
            Analysis.iParams.noise_model = gd.getNextChoiceIndex();
        }

        Analysis.iParams.refinement = true;
        Analysis.iParams.max_nsb = 151;

        if (!Analysis.iParams.subpixel) {
            Analysis.iParams.oversampling2ndstep = 1;
            Analysis.iParams.interpolation = 1;
        }

        return 0;
    }
}
