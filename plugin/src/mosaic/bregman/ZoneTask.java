package mosaic.bregman;


import java.util.concurrent.CountDownLatch;


class ZoneTask implements Runnable {

    private final CountDownLatch ZoneDoneSignal;
    private final CountDownLatch Sync1;
    private final CountDownLatch Sync2;
    private final CountDownLatch Sync3;
    private final CountDownLatch Sync4;
    private final CountDownLatch Sync5;
    private final CountDownLatch Sync6;
    private final CountDownLatch Sync7;
    private final CountDownLatch Sync8;
    private final CountDownLatch Sync9;
    private final CountDownLatch Sync10;
    private final CountDownLatch Sync11;
    private final CountDownLatch Sync12;
    private final CountDownLatch Dct;
    private final ASplitBregmanSolverTwoRegionsPSF AS;
    private final int iStart, iEnd, jStart, jEnd;
    private final int num;
    private final Tools LocalTools;

    ZoneTask(CountDownLatch ZoneDoneSignal, CountDownLatch Sync1, CountDownLatch Sync2, CountDownLatch Sync3, CountDownLatch Sync4, CountDownLatch Dct, CountDownLatch Sync5, CountDownLatch Sync6,
            CountDownLatch Sync7, CountDownLatch Sync8, CountDownLatch Sync9, CountDownLatch Sync10, CountDownLatch Sync11, CountDownLatch Sync12, int iStart, int iEnd, int jStart, int jEnd, int num,
            ASplitBregmanSolverTwoRegionsPSF AS, Tools tTools) {
        this.LocalTools = tTools;
        this.ZoneDoneSignal = ZoneDoneSignal;
        this.Sync1 = Sync1;
        this.Sync2 = Sync2;
        this.Sync3 = Sync3;
        this.Sync4 = Sync4;
        this.Sync5 = Sync5;
        this.Sync6 = Sync6;
        this.Sync7 = Sync7;
        this.Sync8 = Sync8;
        this.Sync9 = Sync9;
        this.Sync10 = Sync10;
        this.Sync11 = Sync11;
        this.Sync12 = Sync12;
        this.num = num;
        this.Dct = Dct;
        this.AS = AS;
        this.iStart = iStart;
        this.jStart = jStart;
        this.iEnd = iEnd;
        this.jEnd = jEnd;
    }

    @Override
    public void run() {
        try {
            doWork();
        }
        catch (final InterruptedException ex) {}

        ZoneDoneSignal.countDown();
    }

    private void doWork() throws InterruptedException {
        LocalTools.subtab(AS.temp1, AS.w2xk, AS.b2xk, iStart, iEnd);
        LocalTools.subtab(AS.temp2, AS.w2yk, AS.b2yk, iStart, iEnd);

        Tools.synchronizedWait(Sync1);

        LocalTools.mydivergence(AS.temp3, AS.temp1, AS.temp2, AS.temp4, Sync2, iStart, iEnd, jStart, jEnd);// , temp3[l]);

        Tools.synchronizedWait(Sync12);

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.temp2[z][i][j] = AS.w1k[z][i][j] - AS.b1k[z][i][j] - AS.iBetaMleOut;
                }
            }
        }
        
        Tools.synchronizedWait(Sync3);

        Tools.convolve2Dseparable(AS.temp4[0], AS.temp2[0], AS.ni, AS.nj, AS.p.PSF, AS.temp1[0], iStart, iEnd);

        Tools.synchronizedWait(Sync11);

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.temp1[z][i][j] = -AS.temp3[z][i][j] + AS.w3k[z][i][j] - AS.b3k[z][i][j] + (AS.iBetaMleIn - AS.iBetaMleOut) * AS.temp4[z][i][j];
                }
            }
        }

        Sync4.countDown();
        Dct.await();

        Tools.convolve2Dseparable(AS.temp2[0], AS.temp1[0], AS.ni, AS.nj, AS.p.PSF, AS.temp3[0], iStart, iEnd);

        Tools.synchronizedWait(Sync10);

        for (int i = iStart; i < iEnd; i++) {
            for (int j = 0; j < AS.nj; j++) {
                AS.temp2[0][i][j] = (AS.iBetaMleIn - AS.iBetaMleOut) * AS.temp2[0][i][j] + AS.iBetaMleOut;
            }
        }

        // %-- w1k subproblem
        if (AS.p.noise_model == 0) {
            // poisson
            // temp3=detw2
            // detw2 = (lambda*gamma.*weightData-b2k-muk).^2+4*lambda*gamma*weightData.*image;

            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.temp3[0][i][j] = Math.pow(((AS.p.ldata / AS.p.lreg_[AS.channel]) * AS.p.gamma - AS.b1k[0][i][j] - AS.temp2[0][i][j]), 2) + 4
                                * (AS.p.ldata / AS.p.lreg_[AS.channel]) * AS.p.gamma * AS.image[0][i][j];
                    }
                }
            }

            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.w1k[0][i][j] = 0.5 * (AS.b1k[z][i][j] + AS.temp2[z][i][j] - (AS.p.ldata / AS.p.lreg_[AS.channel]) * AS.p.gamma + Math.sqrt(AS.temp3[z][i][j]));
                    }
                }
            }
        }
        else {
            // gaussian
            // w2k = (b2k+muk+2*lambda*gamma*weightData.*image)./(1+2*lambda*gamma*weightData);
            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.w1k[0][i][j] = (AS.b1k[z][i][j] + AS.temp2[z][i][j] + 2 * (AS.p.ldata / AS.p.lreg_[AS.channel]) * AS.p.gamma * AS.image[0][i][j])
                                / (1 + 2 * (AS.p.ldata / AS.p.lreg_[AS.channel]) * AS.p.gamma);
                    }
                }
            }

        }

        // %-- w3k subproblem
        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.w3k[z][i][j] = Math.max(Math.min(AS.temp1[z][i][j] + AS.b3k[z][i][j], 1), 0);
                }
            }
        }

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.b1k[z][i][j] = AS.b1k[z][i][j] + AS.temp2[z][i][j] - AS.w1k[z][i][j];
                    AS.b3k[z][i][j] = AS.b3k[z][i][j] + AS.temp1[z][i][j] - AS.w3k[z][i][j];
                }
            }
        }

        Tools.synchronizedWait(Sync5);

        LocalTools.fgradx2D(AS.temp3, AS.temp1, jStart, jEnd);
        LocalTools.fgrady2D(AS.temp4, AS.temp1, iStart, iEnd);

        Tools.synchronizedWait(Sync6);

        LocalTools.addtab(AS.w2xk, AS.temp3, AS.b2xk, iStart, iEnd);
        LocalTools.addtab(AS.w2yk, AS.temp4, AS.b2yk, iStart, iEnd);
        LocalTools.shrink2D(AS.w2xk, AS.w2yk, AS.w2xk, AS.w2yk, AS.p.gamma, iStart, iEnd);
        //
        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.b2xk[z][i][j] = AS.b2xk[z][i][j] + AS.temp3[z][i][j] - AS.w2xk[z][i][j];
                    AS.b2yk[z][i][j] = AS.b2yk[z][i][j] + AS.temp4[z][i][j] - AS.w2yk[z][i][j];
                }
            }
        }

        Tools.synchronizedWait(Sync7);

        if (AS.stepk % AS.p.energyEvaluationModulo == 0 || AS.stepk == AS.p.max_nsb - 1) {
            AS.energytab2[num] = LocalTools.computeEnergyPSF(AS.temp1, AS.w3k, AS.temp3, AS.temp4, AS.p.ldata, AS.p.lreg_[AS.channel], AS.p.PSF, AS.iBetaMleOut, AS.iBetaMleIn, AS.image, iStart,
                    iEnd, jStart, jEnd, Sync8, Sync9);
        }
    }
}
