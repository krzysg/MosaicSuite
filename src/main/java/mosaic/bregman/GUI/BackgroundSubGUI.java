package mosaic.bregman.GUI;


import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.gui.GenericDialog;
import mosaic.bregman.Parameters;


class BackgroundSubGUI {
    public static int getParameters(Parameters aParameters) {
        final GenericDialog gd = new GenericDialog("Background subtractor options");
        gd.setInsets(-10, 0, 3);
        
        gd.addMessage("Background subtractor", new Font(null, Font.BOLD, 12));

        final Panel p = new Panel();
        GenericGUI.addButton(p, "help", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final Point p = gd.getLocationOnScreen();
                new BackgroundSubHelp(p.x, p.y);
            }
        });
        gd.addPanel(p);

        gd.addCheckbox("Remove_background", aParameters.removebackground);
        gd.addNumericField("rolling_ball_window_size_(in_pixels)", aParameters.size_rollingball, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return -1;
        }

        aParameters.removebackground = gd.getNextBoolean();
        aParameters.size_rollingball = (int) gd.getNextNumber();

        return 0;
    }
}
