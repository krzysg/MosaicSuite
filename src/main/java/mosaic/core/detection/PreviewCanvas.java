package mosaic.core.detection;


import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;

import ij.ImagePlus;
import ij.gui.ImageCanvas;


/**
 * Defines an overlay Canvas for a given <code>ImagePlus</code> on which the detected particles from
 * a <code>MyFrame</code> are displayed for preview
 */
public class PreviewCanvas extends ImageCanvas {

    private static final long serialVersionUID = 1L;
    private MyFrame preview_frame;
    private int magnificationFactor = 1;
    private int preview_slice_calculated;
    private int radius;

    /**
     * Constructor. <br>
     * Creates an instance of PreviewCanvas from a given <code>ImagePlus</code> <br>
     * Displays the detected particles from the given <code>MyFrame</code>
     * 
     * @param aimp - the given image plus on which the detected particles are displayed
     * @param preview_f - the <code>MyFrame</code> with the detected particles to display
     * @param mag - the magnification factor of the <code>ImagePlus</code> relative to the initial
     */
    PreviewCanvas(ImagePlus aimp, double mag) {
        super(aimp);
        this.preview_frame = null;
        this.magnificationFactor = (int) mag;
    }

    public void setPreviewFrame(MyFrame aPreviewFrame) {
        this.preview_frame = aPreviewFrame;
    }

    public void setPreviewParticleRadius(int radius) {
        this.radius = radius;
    }

    public void setPreviewSliceCalculated(int slice_calculated) {
        this.preview_slice_calculated = slice_calculated;
    }

    /*
     * (non-Javadoc)
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        final int frameToDisplay = getFrameNumberFromSlice(this.imp.getCurrentSlice());
        Vector<Particle> particlesToDisplay = null;
        if (frameToDisplay == getFrameNumberFromSlice(preview_slice_calculated)) {
            // the preview display color is set to red
            g.setColor(Color.red);
            if (preview_frame != null) {
                particlesToDisplay = preview_frame.getParticles();
                circleParticles(g, particlesToDisplay);
            }
        }
    }

    /**
     * Inner class method <br>
     * Invoked from the <code>paint</code> overwritten method <br>
     * draws a dot and circles the detected particle directly of the given <code>Graphics</code>
     * 
     * @param g
     */
    private void circleParticles(Graphics g, Vector<Particle> particlesToDisplay) {
        if (particlesToDisplay == null || g == null) {
            return;
        }

        // get the slice number
        final int c_slice = this.imp.getCurrentSlice() % imp.getNSlices();

        this.magnificationFactor = (int) Math.round(imp.getWindow().getCanvas().getMagnification());
        // go over all the detected particle
        for (int i = 0; i < particlesToDisplay.size(); i++) {
            // draw a dot at the detected particle position (oval of height and width of 0)
            // the members x, y of the Particle object are opposite to the screen X and Y axis
            // The x-axis points top-down and the y-axis is oriented left-right in the image plane.
            final double z = particlesToDisplay.elementAt(i).iZ + 1;

            if (z <= c_slice + 1 && z >= c_slice - 1) {
                g.drawOval(this.screenXD(particlesToDisplay.elementAt(i).iY), this.screenYD(particlesToDisplay.elementAt(i).iX), 0, 0);
                // circle the the detected particle position according to the set radius
                g.drawOval(this.screenXD(particlesToDisplay.elementAt(i).iY - radius / 1.0), this.screenYD(particlesToDisplay.elementAt(i).iX - radius / 1.0), 2 * radius * this.magnificationFactor - 1, 2
                        * radius * this.magnificationFactor - 1);
            }
        }
    }

    /**
     * @param sliceIndex: 1..#slices
     * @return a frame index: 1..#frames
     */
    private int getFrameNumberFromSlice(int sliceIndex) {
        return (sliceIndex - 1) / imp.getNSlices() + 1;
    }
}
