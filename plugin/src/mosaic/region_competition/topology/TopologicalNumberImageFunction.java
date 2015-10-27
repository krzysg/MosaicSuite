package mosaic.region_competition.topology;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import mosaic.core.image.Connectivity;
import mosaic.core.image.LabelImage;
import mosaic.core.image.Point;


public class TopologicalNumberImageFunction {

    private final Connectivity TFGConnectivity;
    private final Connectivity TBGConnectivity;

    private final UnitCubeCCCounter m_ForegroundUnitCubeCCCounter;
    private final UnitCubeCCCounter m_BackgroundUnitCubeCCCounter;

    private final char[] m_SubImage; // binary subimage (switching fg/bg)
    private final Point[] m_Offsets; // maps indexes to Points
    private final int[] m_DataSubImage; // cached input image

    private final LabelImage labelImage;
    private final int dimension;
    private final int imageSize;

    private final int Zero = 0;

    public TopologicalNumberImageFunction(LabelImage aLabelImage, Connectivity aTFGConnectivity, Connectivity aTBGConnectivity) {
        this.TFGConnectivity = aTFGConnectivity;
        this.TBGConnectivity = aTBGConnectivity;

        this.m_ForegroundUnitCubeCCCounter = new UnitCubeCCCounter(TFGConnectivity);
        this.m_BackgroundUnitCubeCCCounter = new UnitCubeCCCounter(TBGConnectivity);

        this.labelImage = aLabelImage;
        this.dimension = aLabelImage.getNumOfDimensions();

        imageSize = TFGConnectivity.getNeighborhoodSize();
        m_DataSubImage = new int[imageSize];

        m_SubImage = new char[imageSize];
        m_Offsets = new Point[imageSize];

        initOffsets();
    }

    private void initOffsets() {
        // allocate points
        for (int i = 0; i < imageSize; i++) {
            m_Offsets[i] = new Point(new int[dimension]);
        }

        // get the ofs for the whole neighborhood.
        for (int i = 0; i < imageSize; ++i) {
            m_Offsets[i] = TFGConnectivity.toPoint(i);
        }
    }

    private void readImageData(Point p) {
        for (int i = 0; i < imageSize; ++i) {
            // TODO CubeIterator at Connectivity?
            m_DataSubImage[i] = labelImage.getLabelAbs(p.add(m_Offsets[i]));
            if (labelImage.isForbiddenLabel(m_DataSubImage[i])) {
                m_DataSubImage[i] = Zero;
            }
        }
    }

    /**
     * @param index
     * @return List(AbjacentLabel, (nFGconnected, nBGconnected))
     */
    public LinkedList<TopologicalNumberResult> EvaluateAdjacentRegionsFGTNAtIndex(Point index) {

        final LinkedList<TopologicalNumberResult> vTNvector = new LinkedList<TopologicalNumberResult>();
        final Set<Integer> vAdjacentLabels = new HashSet<Integer>();

        readImageData(index);

        for (final int vLinearIndex : TFGConnectivity.itOfsInt()) {
            if (m_DataSubImage[vLinearIndex] != Zero) {
                vAdjacentLabels.add(m_DataSubImage[vLinearIndex]);
            }
        }

        for (final int vLabelsIt : vAdjacentLabels) {
            for (int i = 0; i < imageSize; ++i) {
                m_SubImage[i] = (char) ((m_DataSubImage[i] == vLabelsIt) ? 255 : 0);
            }
            final int middle = imageSize / 2;
            m_SubImage[middle] = 0;

            final TopologicalNumberPair vFGBGTopoPair = new TopologicalNumberPair(0, 0);

            // Topological number in the foreground
            m_ForegroundUnitCubeCCCounter.SetImage(m_SubImage);
            vFGBGTopoPair.FGNumber = m_ForegroundUnitCubeCCCounter.connectedComponents();

            // Invert the sub-image
            for (int bit = 0; bit < middle; ++bit) {
                m_SubImage[bit] = (char) (255 - m_SubImage[bit]);
            }
            for (int bit = middle; bit < imageSize - 1; bit++) {
                m_SubImage[bit + 1] = (char) (255 - m_SubImage[bit + 1]);
            }

            // Topological number in the background

            m_BackgroundUnitCubeCCCounter.SetImage(m_SubImage);
            vFGBGTopoPair.BGNumber = m_BackgroundUnitCubeCCCounter.connectedComponents();
            vTNvector.add(new TopologicalNumberResult(vLabelsIt, vFGBGTopoPair));

        }
        return vTNvector;
    }

    public static class TopologicalNumberPair {

        public int FGNumber;
        public int BGNumber;

        protected TopologicalNumberPair(int fg, int bg) {
            this.FGNumber = fg;
            this.BGNumber = bg;
        }
    }

    public static class TopologicalNumberResult {

        public final int label;
        public final TopologicalNumberPair topologicalNumberPair;

        protected TopologicalNumberResult(int label, TopologicalNumberPair tn) {
            this.label = label;
            this.topologicalNumberPair = tn;
        }
    }

}
