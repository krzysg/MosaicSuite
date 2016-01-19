package mosaic.bregman;


import Skeletonize3D_.Skeletonize3D_;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import mosaic.core.psf.GaussPSF;
import net.imglib2.type.numeric.real.DoubleType;

class ObjectProperties implements Runnable {

    private double intmin, intmax;
    private final double[][][] image;
    private final Region region;
    private double cin;
    private double[][][] patch;
    private final short[][][] regions;

    private int sx, sy, sz;// size for object
    private final int nx, ny, nz;// size of full oversampled work zone
    private final Parameters p;
    private int cx, cy, cz;// coord of patch in full work zone (offset)
    private final int osxy, osz;
    private double[][][] mask;// nregions nslices ni nj

    public ObjectProperties(double[][][] im, Region reg, int nx, int ny, int nz, Parameters p1, int osxy, int osz, short[][][] regs) {
        this.regions = regs;
        this.p = new Parameters(p1);
        this.image = im;
        this.region = reg;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;

        this.osxy = osxy;
        this.osz = osz;

        set_patch_geom(region);

        // set size
        p.ni = sx;
        p.nj = sy;
        p.nz = sz;
        // set psf
        if (p.nz > 1) {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(3, DoubleType.class);
            final DoubleType[] var = new DoubleType[3];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            var[2] = new DoubleType(p.sigma_gaussian / p.zcorrec);
            psf.setVar(var);
            p.PSF = psf;
        }
        else {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(2, DoubleType.class);
            final DoubleType[] var = new DoubleType[2];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            psf.setVar(var);
            p.PSF = psf;
        }
    }

    @Override
    public void run() {
        fill_patch(image);
        normalize();
        fill_mask(region);
        estimate_int(mask);
        region.intensity = cin * (intmax - intmin) + intmin;
        if (p.nz == 1) {
            region.rsize = (float) Tools.round((region.pixels.size()) / ((float) osxy * osxy), 3);
        }
        else {
            region.rsize = (float) Tools.round((region.pixels.size()) / ((float) osxy * osxy * osxy), 3);
        }

        if (p.save_images) {
            setIntensitiesandCenters(region, image);
            setPerimeter(region, regions);
            setlength(region, regions);
        }
    }

    private void fill_patch(double[][][] image) {
        this.patch = new double[sz][sx][sy];
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    this.patch[z][i][j] = image[(cz + z) / osz][(cx + i) / osxy] [(cy + j) / osxy];
                }
            }
        }
    }

    private void estimate_int(double[][][] mask) {
        double[][][] temp1 = new double[sz][sx][sy];
        double[][][] temp2 = new double[sz][sx][sy];
        double[][][] temp3 = new double[sz][sx][sy];
        RegionStatisticsSolver RSS = new RegionStatisticsSolver(temp1, temp2, temp3, patch, 10, p);
        RSS.eval(mask);

        cin = RSS.betaMLEin;
    }

    private void set_patch_geom(Region r) {
        Pix[] mm = r.getMinMaxCoordinates();
        Pix min = mm[0]; Pix max = mm[1];
        int xmin = min.px;
        int ymin = min.py;
        int zmin = min.pz;
        int xmax = max.px;
        int ymax = max.py;
        int zmax = max.pz;
        
        final int margin = 5;
        final int zmargin = 2;
        xmin = Math.max(0, xmin - margin);
        xmax = Math.min(nx, xmax + margin + 1);

        ymin = Math.max(0, ymin - margin);
        ymax = Math.min(ny, ymax + margin + 1);

        if (nz > 1) {
            zmin = Math.max(0, zmin - zmargin);
            zmax = Math.min(nz, zmax + zmargin + 1);
        }

        this.sx = (xmax - xmin);// todo :correct :+1 : done
        this.sy = (ymax - ymin);// correct :+1

        cx = xmin;
        cy = ymin;

        if (nz == 1) {
            this.sz = 1;
            cz = 0;
        }
        else {
            this.sz = (zmax - zmin);
            cz = zmin;
        }
    }

    private void normalize() {
        intmin = Double.MAX_VALUE;
        intmax = 0;
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (patch[z][i][j] > intmax) {
                        intmax = patch[z][i][j];
                    }
                    if (patch[z][i][j] < intmin) {
                        intmin = patch[z][i][j];
                    }
                }
            }
        }

        // rescale between 0 and 1
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    patch[z][i][j] = (patch[z][i][j] - intmin) / (intmax - intmin);
                }
            }
        }

    }

    private void fill_mask(Region r) {
        mask = new double[sz][sx][sy];
        
        for (Pix p : r.pixels) {
            int rz = (p.pz - cz);
            int rx = (p.px - cx);
            int ry = (p.py - cy);
            mask[rz][rx][ry] = 1;
        }
    }

    private void setIntensitiesandCenters(Region r, double[][][] image) {
        regionIntensityAndCenter(r, image);
    }

    private void setPerimeter(Region r, short[][][] regionsA) {
        if (p.nz == 1) {
            regionPerimeter(r, regionsA);
        }
        else {
            regionPerimeter3D(r, regionsA);
        }
    }

    private void regionPerimeter(Region r, short[][][] regionsA) {
        // 2 Dimensions only
        double pr = 0;

        for (Pix v : r.pixels) {
            // count number of free edges
            int edges = 0;
            if (v.px != 0 && v.px != nx - 1 && v.py != 0 && v.py != ny - 1) {
                // not on edges of image
                if (regionsA[v.pz][v.px - 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px + 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py - 1] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py + 1] == 0) {
                    edges++;// !=rvalue
                }
            }
            else {
                edges++;
            }

            pr += edges; // real number of edges (should be used with the subpixel)
        }

        r.perimeter = pr;

        if (Analysis.p.subpixel) {
            r.perimeter = pr / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
        }
    }

    private void regionPerimeter3D(Region r, short[][][] regionsA) {
        // 2 Dimensions only
        double pr = 0;

        for (Pix v : r.pixels) {
            // count number of free edges
            int edges = 0;
            // not on edges of image
            if (v.px != 0 && v.px != nx - 1 && v.py != 0 && v.py != ny - 1 && v.pz != 0 && v.pz != nz - 1) {
                if (regionsA[v.pz][v.px - 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px + 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py - 1] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py + 1] == 0) {
                    edges++;
                }
                if (regionsA[v.pz + 1][v.px][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz - 1][v.px][v.py] == 0) {
                    edges++;
                }
            }
            else {
                edges++;
            }
            pr += edges;
        }

        r.perimeter = pr;
        if (osxy > 1) {
            if (p.nz == 1) {
                r.perimeter = pr / (osxy);
            }
            else {
                r.perimeter = pr / (osxy * osxy);
            }
        }
    }

    private void regionIntensityAndCenter(Region r, double[][][] image) {
        double sum = 0;
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (Pix p : r.pixels) {
            if (!Analysis.p.refinement) {
                sum += image[p.pz][p.px][p.py];
            }

            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
        }
        
        int count = r.pixels.size();

        if (!Analysis.p.refinement) {
            r.intensity = (sum / (count));
        }// done in refinement

        r.cx = (float) (sumx / count);
        r.cy = (float) (sumy / count);
        r.cz = (float) (sumz / count);

        if (Analysis.p.subpixel) {
            r.cx = r.cx / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
            r.cy = r.cy / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
            r.cz = r.cz / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
        }
    }

    private void setlength(Region r, short[][][] regionsA) {
        final ImageStack is = new ImageStack(sx, sy);
        for (int k = 0; k < sz; k++) {
            final byte[] mask_bytes = new byte[sx * sy];
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (regionsA[cz + k][cx + i][cy + j] > 0) {
                        mask_bytes[j * sx + i] = (byte) 255;
                    }
                    else {
                        mask_bytes[j * sx + i] = (byte) 0;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(sx, sy);
            bp.setPixels(mask_bytes);
            is.addSlice(bp);
        }

        final ImagePlus skeleton = new ImagePlus("", is);

        // do voronoi in 2D on Z projection
        Skeletonize3D_ skel = new Skeletonize3D_();
        skel.setup("", skeleton);
        skel.run(skeleton.getProcessor());
        
        regionlength(r, skeleton);
    }

    private void regionlength(Region r, ImagePlus skel) {
        int length = 0;
        final ImageStack is = skel.getStack();

        for (Pix v : r.pixels) {
            // count number of pixels in skeleton
            if (is.getProcessor(v.pz - cz + 1).getPixel(v.px - cx, v.py - cy) != 0) {
                length++;
            }
        }

        r.length = length;
        // osz assumed to me == osxy
        if (osxy > 1) {
            r.length = ((double) length) / osxy;
        }
    }
}
