package mosaic.region_competition.DRS;


import static mosaic.core.imageUtils.images.LabelImage.BGLabel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.plaf.synth.SynthSpinnerUI;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.energies.Energy.EnergyResult;
import mosaic.region_competition.topology.TopologicalNumber;
import mosaic.region_competition.topology.TopologicalNumber.TopologicalNumberResult;

public class AlgorithmDRS {

    private static final Logger logger = Logger.getLogger(AlgorithmDRS.class);

    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final IntensityImage iEdgeImage;
    private final ImageModel iImageModel;
    private final Settings iSettings;

    int m_Dim;
    
    // TODO: Names below are map one to one from original source code
    //       Change them to something nicer after implementation.

    // Parameters
    boolean m_MCMCuseBiasedProposal = true; // TODO: false by default in c++
    boolean m_MCMCusePairProposal = true; // TODO: false by def
    
    boolean m_AllowFission = true;
    boolean m_AllowHandles = true;
    boolean m_AllowFusion = true;

    int m_MaxNbIterations = 1;
    int m_iteration_counter = 0;
    int m_MCMCstepsize = 1;
    
    boolean m_Verbose = false;
    
    float m_OffBoundarySampleProbability = 0.1f;
    float m_MCMCburnInFactor = 0.5f;
    
    // ==========================================================

    Rng m_NumberGenerator = new Rng(1212);
    Rng m_NumberGeneratorBoost = new Rng();
    
    // Connectivities
    int m_NeighborhoodSize_FG_Connectivity;
    Point[] m_NeighborsOffsets_FG_Connectivity;
    int m_NeighborhoodSize_BG_Connectivity;
    Point[] m_NeighborsOffsets_BG_Connectivity;
    TopologicalNumber m_TopologicalNumberFunction;
    
    float[] m_MCMClengthProposalMask = null;

    LabelImage m_MCMCRegularParticlesMap; // originaly boolean but  0 / 1 approach seems to be good
    double m_MCMCZe = 0.0; // sum of edge image values
    EnumeratedDistribution<Integer> m_MCMCEdgeImageDistr = null;
    private LabelImage m_MCMCInitBackupLabelImage;

    List<Integer> m_MCMCRegionLabel = new ArrayList<>();
    // ProposalNormalizerMapType
    Map<Integer, Float> m_MCMCparentsProposalNormalizer = new HashMap<>();
    Map<Integer, Float> m_MCMCchildrenProposalNormalizer = new HashMap<>();
    double m_MCMCTotalNormalizer;

    Map<Integer, MinimalParticleIndexedSet> m_MCMCchildren = new HashMap<>();
    Map<Integer, MinimalParticleIndexedSet> m_MCMCparents = new HashMap<>();
    
    class ParticleHistoryElement {
        public ParticleHistoryElement(MinimalParticle vReplacedParticle, int aCurrentLabel, boolean b) {
            m_Particle = vReplacedParticle;
            m_OriginalLabel = aCurrentLabel;
            m_WasAdded = b;
        }
        MinimalParticle m_Particle;
        int m_OriginalLabel;
        boolean m_WasAdded;
        
        @Override
        public String toString() {return m_Particle + " / " + m_OriginalLabel + " / " + m_WasAdded + "\n";} 
    }
    
    List<ParticleHistoryElement> m_MCMCParticleInContainerHistory = new ArrayList<>();
    
    private final HashMap<Integer, LabelStatistics> iLabelStatistics = new HashMap<Integer, LabelStatistics>();
    
    List<ParticleHistoryElement> m_MCMCFloatingParticleInContainerHistory = new ArrayList<>();
    
    class LabelImageHistoryEventType {
        public LabelImageHistoryEventType(int aIndex, int aLabel) {first = aIndex; second = aLabel;}
        public int first; // LabelImageIndexType
        public int second; // LabelPixelType
    }
    List<LabelImageHistoryEventType> m_MCMCLabelImageHistory = new ArrayList<>();
    
    MinimalParticleIndexedSet m_MCMCFloatingParticles = new MinimalParticleIndexedSet();
    
    float m_FloatingParticlesProposalNormalizer = 0;
    int m_MCMCNumberOfSamplesForBiasedPropApprox = 30;
    
    
    
    // --------------------------------------
    
    
    
    public AlgorithmDRS(IntensityImage aIntensityImage, LabelImage aLabelImage, IntensityImage aEdgeImage, ImageModel aModel, Settings aSettings) {
        logger.debug("DRS algorithm created");

        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iEdgeImage = aEdgeImage;
        iImageModel = aModel;
        iSettings = aSettings;

        m_Dim = iLabelImage.getNumOfDimensions();
        
        logger.debug(mosaic.utils.Debug.getString(iIntensityImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iLabelImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iEdgeImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iImageModel.toString()));
        logger.debug(mosaic.utils.Debug.getString(iSettings.toString()));

        // Initialize label image
        iLabelImage.initBorder();
        iLabelImage.initContour();

        // init connectivities
        Connectivity connFG = iLabelImage.getConnFG();
        m_NeighborhoodSize_FG_Connectivity = connFG.getNumOfNeighbors();
        m_NeighborsOffsets_FG_Connectivity = connFG.getPointOffsets();
        Connectivity connBG = iLabelImage.getConnBG();
        m_NeighborhoodSize_BG_Connectivity = connBG.getNumOfNeighbors();
        m_NeighborsOffsets_BG_Connectivity = connBG.getPointOffsets();
        // TODO: m_TopologicalNumberFunction - set / check what is it.
        m_TopologicalNumberFunction = new TopologicalNumber(iLabelImage);
        
        // OK.. here starts algorithm
        m_MCMCRegularParticlesMap = new LabelImage(iLabelImage.getDimensions());

        Pair<Double, EnumeratedDistribution<Integer>> distPair = generateDiscreteDistribution(iEdgeImage, m_NumberGeneratorBoost);
        m_MCMCZe = distPair.getFirst();
        m_MCMCEdgeImageDistr = distPair.getSecond();

        // Prepare a fast proposal computation:
        System.out.println("m_MCMCuseBiasedProposal: " + (m_MCMCuseBiasedProposal ? "1" : "0") + " " + m_NeighborhoodSize_BG_Connectivity);
        for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
            System.out.println(vN + "_conn: " + m_NeighborsOffsets_BG_Connectivity[vN]);
        }
        if (m_MCMCuseBiasedProposal) {
            m_MCMClengthProposalMask = new float[m_NeighborhoodSize_BG_Connectivity];
            for (int i = 0; i < m_NeighborhoodSize_BG_Connectivity; ++i) {
                Point offset = m_NeighborsOffsets_BG_Connectivity[i];
                m_MCMClengthProposalMask[i] = (float) (1.0 / offset.length());
                System.out.println(i + "_mask: " + m_MCMClengthProposalMask[i]);
            }
        }

        // We need a copy of the label image for the reconstruction of the 
        // final the results (marginals). It is a backup of the initial state.
        m_MCMCInitBackupLabelImage = new LabelImage(iLabelImage);

        // Register all labels from lableImage
        Set<Integer> visitedLabels = new HashSet<>();

        // By default add background
        m_MCMCRegionLabel.add(LabelImage.BGLabel);
        m_MCMCparentsProposalNormalizer.put(LabelImage.BGLabel, 0f);
        m_MCMCchildrenProposalNormalizer.put(LabelImage.BGLabel, 0f);
        visitedLabels.add(LabelImage.BGLabel);
        m_MCMCTotalNormalizer = 0.0;

        // TODO: Remove it - only temporary input to match c++
        iLabelImage.setLabel(6, 0);
        iLabelImage.setLabel(7, 0);
        iLabelImage.setLabel(8, 1);
        iLabelImage.setLabel(11, 0);
        iLabelImage.setLabel(12, 1);
        iLabelImage.setLabel(13, 1);
        iLabelImage.setLabel(16, 1);
        iLabelImage.setLabel(17, 1);
        iLabelImage.setLabel(18, 1);
        
        final Iterator<Point> ri = new SpaceIterator(iLabelImage.getDimensions()).getPointIterator();
        while (ri.hasNext()) {
            final Point point = ri.next();
            int label = iLabelImage.getLabel(point);
            System.out.println("------------ Init label: " + label + " at " + point);
            if (iLabelImage.isBorderLabel(label)) continue;

            // Add if not added so far
            int labelAbs = iLabelImage.labelToAbs(label);
            if (!visitedLabels.contains(labelAbs)) {
                visitedLabels.add(labelAbs);
                m_MCMCRegionLabel.add(label);
                m_MCMCparentsProposalNormalizer.put(labelAbs, 0f);
                m_MCMCchildrenProposalNormalizer.put(labelAbs, 0f);
            }
            //            TODO: rest of initialization
            //            
            //            // Add all regular particles at this spot:
            MinimalParticleIndexedSet vPs = new MinimalParticleIndexedSet();
            MCMCgetRegularParticlesAtIndex(iLabelImage.pointToIndex(point), vPs);
                        
            for (int vI = 0; vI < vPs.size(); ++vI) {
                if (MCMCIsParticleTopoValid(vPs.elementAt(vI))) {
                    MCMCInsertCandidatesToContainers(vPs.elementAt(vI), label, false);
                    iLabelImage.setLabel(point, -labelAbs);
                    m_MCMCRegularParticlesMap.setLabel(vPs.elementAt(vI).iIndex, 1);
                    m_MCMCZe -= iEdgeImage.get(vPs.elementAt(vI).iIndex);
                }
            }
        }
        
        System.out.println("CHILDREN:\n" + m_MCMCchildren);
        System.out.println("PARENTS:\n" + m_MCMCparents);
        
        initStatistics();
        
        System.out.println("INIT STATS:\n" + iLabelStatistics);
        
        prepareEnergies(); // TODO: initEnergies from RC - should be handled differently (energies cleanup)
        
        // --------------- Main loop ---------------------
        
        /* Main loop */
        int vAcceptedMoves = 0;
        int vTempAcceptedMoves = 0;
        while (m_MaxNbIterations > m_iteration_counter) {
            m_iteration_counter++;
            int vAccepted = MCMCDoIteration() ? 1 : 0;
            vAcceptedMoves += vAccepted;
            vTempAcceptedMoves += vAccepted;

            /// Some output:
            if(m_Verbose) {
                PrintIterationInfo(vTempAcceptedMoves); // Resets vTempAcceptedMoves back to 0.
            }
        }
        
    }
    
    boolean MCMCDoIteration() {
        PrepareEnergyCalculationForEachIteration();
        
        float vTotEnergyDiff = 0;
        boolean vHardReject = false;
        
        /// These list will help to revert the move in case it gets rejected.
        //  ParticleHistoryElementListType vHistoryOfMovedParticles;
        m_MCMCParticleInContainerHistory.clear();
        m_MCMCFloatingParticleInContainerHistory.clear();
        m_MCMCLabelImageHistory.clear();
        
        /// The following check is theoretically not needed as there should
        /// always be a floating particle somewhere (given the algorithm
        /// got initialized with more than one region). But in the burn-in phase
        /// we delete floating particles.
        if (m_MCMCRegionLabel.size() < 2) {
            // TODO: Should be handled differently than RuntimeEx?
            new RuntimeException("No active region for MCMC available");
            return false;
        }
        
        /// Sample a region number (a FG region number; without BG)
        int vAbsLabelIndex = m_NumberGenerator.GetIntegerVariate(m_MCMCRegionLabel.size() - 2) + 1;
        /// Find the corresponding label
        int vAbsLabel = m_MCMCRegionLabel.get(vAbsLabelIndex);

        if(m_AllowFission && m_AllowFusion) {
            // linear annealing
            float vOffboundaryPerc = m_OffBoundarySampleProbability * (1.0f - m_iteration_counter / (m_MCMCburnInFactor * m_MaxNbIterations));

            boolean vOffBoundarySampling = (vOffboundaryPerc > 0) ? m_NumberGenerator.GetVariate() < vOffboundaryPerc : false;
            if (vOffBoundarySampling) {
                boolean vGrowth = m_NumberGenerator.GetUniformVariate(0,1) < 0.5;
                return MCMCOffBoundarySample(vGrowth, vAbsLabel);
            }
        }
        
        
        /// Figure out if Particle A will cause growth, shrinkage or be floating particle
        double vProbabilityToProposeAFloatingParticle = //(m_MCMCFloatingParticles.size()>0)?0.9:0;
        m_FloatingParticlesProposalNormalizer /
        (m_MCMCTotalNormalizer + m_FloatingParticlesProposalNormalizer);
//        double vZetaCorrectionFactor = (m_MCMCFloatingParticles.size()>0)?0.5:0;
        boolean vParticleAIsFloating = false;
        MinimalParticleIndexedSet vActiveCandidates = null;
        double vR = m_NumberGenerator.GetUniformVariate(0.0,1.0);
        if(vR < vProbabilityToProposeAFloatingParticle) { // + vZetaCorrectionFactor) {
            /// We will choose one out of the floating particles
            vActiveCandidates = m_MCMCFloatingParticles;
            vParticleAIsFloating = true;
        } else if (vR < 0.5*(vProbabilityToProposeAFloatingParticle + 1)) { // + vZetaCorrectionFactor)) {
            /// We will grow and hence choose one out of the children list
            vActiveCandidates = m_MCMCchildren.get(vAbsLabel);
        } else {
            vActiveCandidates = m_MCMCparents.get(vAbsLabel);
        }
        
        if (vActiveCandidates.size() == 0) {
            /// This is an empty region. Maybe there exists a floating particle
            /// with no future for this region. But if m_Count == 0, it will not
            /// be accepted according to the definition of the energy. We hence
            /// cleanup the statistics (kill the region).

            MCMCUpdateRegionLabelVector();
            return false;
        }
        

        /// For each particle within the region, calculate the proposal
//        List<Float> vAllParticlesFwdProposals = new ArrayList<>();
        ArrayList<Pair<Integer, Double>> vAllParticlesFwdProposals = new ArrayList<>();
        
        /// Create a discrete distribution over particles
        int vIndexOffset = 0;
        boolean vApproxedIndex = false;
//        typedef boost::random::discrete_distribution<int, float> DiscreteDistType;
//        DiscreteDistType* vDiscreteDistr;
        EnumeratedDistribution<Integer> vDiscreteDistr = null;

        if (m_MCMCuseBiasedProposal && !vParticleAIsFloating &&
            (m_MCMCNumberOfSamplesForBiasedPropApprox == 0 || m_MCMCNumberOfSamplesForBiasedPropApprox >= vActiveCandidates.size())) {
//            vAllParticlesFwdProposals.reserve(vActiveCandidates.size());

            int index = 0;
            for (int vI = 0; vI < vActiveCandidates.size(); vI++) {
                vAllParticlesFwdProposals.add(new Pair<>(index++, (double)vActiveCandidates.elementAt(vI).iProposal));
            }

            vDiscreteDistr = new EnumeratedDistribution<>(m_NumberGeneratorBoost, vAllParticlesFwdProposals); //new DiscreteDistType(vAllParticlesFwdProposals.begin(), vAllParticlesFwdProposals.end());

        } 
        else if(m_MCMCuseBiasedProposal && !vParticleAIsFloating) {
            vApproxedIndex = true;
//            vAllParticlesFwdProposals.reserve(m_MCMCNumberOfSamplesForBiasedPropApprox);
            vIndexOffset = m_NumberGenerator.GetIntegerVariate(vActiveCandidates.size()-1);
            int index = 0;
            for (int vI = 0; vI < m_MCMCNumberOfSamplesForBiasedPropApprox; vI++) {
                int vApproxParticleIndex = (vIndexOffset + vI) % vActiveCandidates.size();
                vAllParticlesFwdProposals.add(new Pair<>(index++, (double)vActiveCandidates.elementAt(vApproxParticleIndex).iProposal));
            }

            vDiscreteDistr = new EnumeratedDistribution<>(m_NumberGeneratorBoost, vAllParticlesFwdProposals);
        }

        System.out.println("DISCRETEE !!!!!!!!!!!!!!!!!!!\n" + vDiscreteDistr.getPmf());
        
        /// Draw n particles from the discrete distribution
        ArrayList<MinimalParticle> vCandidateMoveVec = new ArrayList<>(Arrays.asList(new MinimalParticle[m_MCMCstepsize]));
        ArrayList<MinimalParticle> vPartnerMoveVec = new ArrayList<>(Arrays.asList(new MinimalParticle[m_MCMCstepsize]));

        // TODO: Can thos be changed to regular primitive arrays? And must they be pre-filled?
        ArrayList<Float> vq_A = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vq_A, 0f);
        ArrayList<Float> vq_B = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vq_B, 0f);
        ArrayList<Float> vq_A_B = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vq_A_B, 0f);
        ArrayList<Float> vq_B_A = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vq_B_A, 0f);
        ArrayList<Float> vqb_A = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vqb_A, 0f);
        ArrayList<Float> vqb_B = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vqb_B, 0f);
        ArrayList<Float> vqb_A_B = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vqb_A_B, 0f);
        ArrayList<Float> vqb_B_A = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vqb_B_A, 0f);
        ArrayList<Float> vLabelsBeforeJump_A = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vLabelsBeforeJump_A, 0f);
        ArrayList<Float> vLabelsBeforeJump_B = new ArrayList<>(Arrays.asList(new Float[m_MCMCstepsize])); Collections.fill(vLabelsBeforeJump_B, 0f);

        ArrayList<Boolean> vParticle_Ab_IsFloating = new ArrayList<>(Arrays.asList(new Boolean[m_MCMCstepsize])); Collections.fill(vParticle_Ab_IsFloating, Boolean.FALSE);
        ArrayList<Boolean> vParticle_Bb_IsFloating = new ArrayList<>(Arrays.asList(new Boolean[m_MCMCstepsize])); Collections.fill(vParticle_Bb_IsFloating, Boolean.FALSE);
        ArrayList<Boolean> vParticle_A_IsFloating = new ArrayList<>(Arrays.asList(new Boolean[m_MCMCstepsize])); Collections.fill(vParticle_A_IsFloating, Boolean.FALSE);

        boolean vSingleParticleMoveForPairProposals = false;
        
        /// Find particle A:
        for (int vPC = 0; vPC < m_MCMCstepsize; vPC++) {

            int vParticleIndex;
            if (m_MCMCuseBiasedProposal && !vParticleAIsFloating) {
                vParticleIndex = vDiscreteDistr.sample();
                vq_A.set(vPC, vAllParticlesFwdProposals.get(vParticleIndex).getSecond().floatValue());

                if (vApproxedIndex) {
                    vParticleIndex = (vIndexOffset + vParticleIndex) % vActiveCandidates.size();
                }
            } else {
                vParticleIndex = m_NumberGenerator.GetIntegerVariate(vActiveCandidates.size()-1);
                vq_A.set(vPC, 1.0f);
            }

            MinimalParticle vParticle = vActiveCandidates.elementAt(vParticleIndex);
            if (vParticleAIsFloating) {
                /// A floating particle was selected.
                vParticle_A_IsFloating.set(vPC, true);

                // Immediately accept the move (self transition) if the label
                // at the particular position is the same. Since this is a
                // self transition we keep the particle in the proposals.
                if (Math.abs(iLabelImage.getLabel(vParticle.iIndex)) == vParticle.iCandidateLabel) {
                    return false; // reject.
                }

                // METHOD 4: reject (because the backward prob is 0) --> 49.2 %
                if (MCMCIsRegularParticle(vParticle, Math.abs(iLabelImage.getLabel(vParticle.iIndex)))) {
                    return false;
                }
                // if we are here, do the job:
                MCMCEraseFloatingParticle(vParticle, true);
            }
            /// Fill in some properties for this particles:
            vLabelsBeforeJump_A.set(vPC, (float) Math.abs(iLabelImage.getLabel(vParticle.iIndex)));
            if (vParticle_A_IsFloating.get(vPC)) {
                vq_A.set(vPC, vq_A.get(vPC) / m_FloatingParticlesProposalNormalizer);
            }
            else {
                vq_A.set(vPC, vq_A.get(vPC) / MCMCGetProposalNormalizer(vLabelsBeforeJump_A.get(vPC).intValue(), vParticle.iCandidateLabel));
            }
            vCandidateMoveVec.set(vPC, vParticle);
        }
        System.out.println("vLabelsBeforeJump_A: " + vLabelsBeforeJump_A);
        System.out.println("vLabelsBeforeJump_B: " + vLabelsBeforeJump_B);
        System.out.println("vCandidateMoveVec:\n" + vCandidateMoveVec);  
        System.out.println("vq_A:\n" + vq_A);
            
        if (m_MCMCuseBiasedProposal && !vParticleAIsFloating) {
            vDiscreteDistr = null;
        }

        
        
        /// In case of pair proposals, we find a partner for each proposed particle.
        /// We now know A and Q(A). Now it needs to build
        /// another (2nd step) discrete proposal distribution. We sample from it
        /// to determine the partner particle B. Furthermore we calculate the
        /// conditional proposal probability Q(B|A). In a second step we calculate
        /// the conditional Q(A|B). The same then needs to be done for the backward
        /// probabilities Qb(A), Qb(B), Qb(A|B) and Qb(B|A).
        /// Notation:
        /// - Q is the forward and Qb the backward probability. A is
        ///   a forward praticle and B' the backward particle.
        /// - Qb always assumes backward particles as its arguments! Hence,
        ///   Qb_A_B is the probabily Qb(A'|B').
        System.out.println("m_MCMCusePairProposal: " + m_MCMCusePairProposal);
        if (m_MCMCusePairProposal) {

            /// Iterate over particles A:
            for (int vPC = 0; vPC < vCandidateMoveVec.size(); ++vPC) {
                MinimalParticle vPartIt = vCandidateMoveVec.get(vPC);
                MinimalParticle vA = new MinimalParticle(vPartIt);
                
                /// TODO: the next line shouldn't change anything?
                vA.iProposal = MCMCproposal(vA.iIndex, vA.iCandidateLabel);

                ArrayList<MinimalParticle> vUpdatedParticlesInNeighborhood = new ArrayList<>();
                System.out.println("MCMCApplyParticle1");
                MCMCApplyParticle(vPartIt, true);
                System.out.println("vCandidateMoveVec1: " + vCandidateMoveVec);

                /// BTW we have to remember if A' is a floating particle in the
                /// state x -> A.
                vParticle_Ab_IsFloating.set(vPC, MCMCParticleHasFloatingProperty(vPartIt.iIndex, vPartIt.iCandidateLabel));

                /// Get the particles involved in the second step of the
                /// proposal (with updated proposals)
                MinimalParticleIndexedSet vParts_Q_BgivenA = new MinimalParticleIndexedSet();
                MCMCgetPartnerParticleSet(vA, vParts_Q_BgivenA);

                /// Find B:
                MinimalParticle vB = new MinimalParticle();

                /// Choose B from Q(B|A) and calculate Q(B|A).
                if (m_MCMCuseBiasedProposal) {
                    ArrayList<Float> vProposalsVector = new ArrayList<>(vParts_Q_BgivenA.size());
                    float vNormalizer_Q_B_A = 0;

                    for (int vPI = 0; vPI < vParts_Q_BgivenA.size(); vPI++) {
                        MinimalParticle vCondParticle = vParts_Q_BgivenA.elementAt(vPI);
                        vProposalsVector.add(vCondParticle.iProposal);
                        vNormalizer_Q_B_A += vCondParticle.iProposal;
                    }
                    EnumeratedDistribution<Integer> vQ_B_A = discreteDistFromList(vProposalsVector);
                    int vCondIndex = vQ_B_A.sample();
                    vB = vParts_Q_BgivenA.elementAt(vCondIndex);
                    /// The value m_Proposal of vB is currently equal to Q_B_A.
                    vq_B_A.set(vPC, vB.iProposal / vNormalizer_Q_B_A);
                } 
                else {
                    int vPartI = m_NumberGenerator.GetIntegerVariate(vParts_Q_BgivenA.size() - 1);
                    vB = vParts_Q_BgivenA.elementAt(vPartI);
                    vq_B_A.set(vPC, 1.0f / vParts_Q_BgivenA.size());
                }

                /// store B (and its original label).
                vPartnerMoveVec.set(vPC, vB);
                vLabelsBeforeJump_B.set(vPC, (float) Math.abs(iLabelImage.getLabel(vB.iIndex)));
                System.out.println("vB vs vA=    " + vB.iIndex + " " + vA.iIndex + " " + vB.iCandidateLabel + " " + vA.iCandidateLabel );
                if (vB.iIndex == vA.iIndex && vB.iCandidateLabel == vA.iCandidateLabel) {
                    vSingleParticleMoveForPairProposals = true;
                    vLabelsBeforeJump_B.set(vPC, vLabelsBeforeJump_A.get(vPC));
                }

                /// Get the reverse particle of A (without proposal update as it
                /// is not necessary):
                MinimalParticle vReverseParticleA = new MinimalParticle(vPartIt);
                System.out.println("vReverseParticleA bef: " + vReverseParticleA);
                vReverseParticleA.iCandidateLabel = vLabelsBeforeJump_A.get(vPC).intValue();
                System.out.println("vReverseParticleA aft: " + vReverseParticleA);
                
                /// In case that vA == vB, we must already undo the simulated
                /// move in order to calculate Q'(B'|A') (== Q'(A'|B'))
                if (vSingleParticleMoveForPairProposals) {
                    System.out.println("MCMCApplyParticle2");
                    MCMCApplyParticle(vReverseParticleA, true);
                }

                /// Get the reverse particle of B:
                MinimalParticle vReverseB = vB;
                vReverseB.iCandidateLabel = vLabelsBeforeJump_B.get(vPC).intValue();
                vReverseB.iProposal = MCMCproposal(vReverseB.iIndex, vReverseB.iCandidateLabel);

                /// in the current state of the label image and the
                /// containers we can calculate qb_A'_B' as well. We assume now
                /// that B' was applied and we calculate the probability for A'.
                MinimalParticleIndexedSet vParts_Qb_AgivenB = new MinimalParticleIndexedSet();
                MCMCgetPartnerParticleSet(vB, vParts_Qb_AgivenB);

                if (m_MCMCuseBiasedProposal) {
                    float vNormalizer_Qb_A_B = 0;
                    for (int vPI = 0; vPI < vParts_Qb_AgivenB.size(); ++vPI){
                        vNormalizer_Qb_A_B += vParts_Qb_AgivenB.elementAt(vPI).iProposal;
                    }

                    float vqb_A_B_unnorm = MCMCproposal(vA.iIndex, vLabelsBeforeJump_A.get(vPC).intValue());
                    vqb_A_B.set(vPC, vqb_A_B_unnorm / vNormalizer_Qb_A_B);
                } else {
                    vqb_A_B.set(vPC, 1.0f / vParts_Qb_AgivenB.size());
                }

                if (!vSingleParticleMoveForPairProposals) {

                    /// undo the simulated move.
                    System.out.println("MCMCApplyParticle3");
                    MCMCApplyParticle(vReverseParticleA, true);

                    /// Now we can calculate Q_B (as we now know B and the original
                    /// state has been recovered).
                    if (!MCMCIsRegularParticle(vB, vLabelsBeforeJump_B.get(vPC).intValue())) {
                        vq_B.set(vPC, 0f);
                    } else {
                        if (m_MCMCuseBiasedProposal) {
                            /// vB.mProposal needs to be recalculated here:
                            vq_B.set(vPC, MCMCproposal(vB.iIndex, vB.iCandidateLabel) /MCMCGetProposalNormalizer(vLabelsBeforeJump_B.get(vPC).intValue(), vB.iCandidateLabel));
                        } else {
                            vq_B.set(vPC, 1.0f / MCMCGetProposalNormalizer(vLabelsBeforeJump_B.get(vPC).intValue(), vB.iCandidateLabel));
                        }
                    } /// end if particle B exists
                }
            } /// end loop particles (all the A particles)
        } /// end if (pair proposal) condition

        /// Currently it is possible that the same candidate is in the move set.
        /// Hence we store the applied moves to avoid duplicates.
        MinimalParticleIndexedSet vAppliedParticles = new MinimalParticleIndexedSet();
        ArrayList<Integer> vAppliedParticleOrigLabels = new ArrayList<>();

        /// Iterate the candidates, calculate the energy and perform the moves.
        for (int vPC = 0; vPC < vCandidateMoveVec.size(); ++vPC) {
            MinimalParticle vParticleA = vCandidateMoveVec.get(vPC);
            MinimalParticle vParticleB = vPartnerMoveVec.get(vPC);

            /// apply particle A and B, start with B:
            int vN = (m_MCMCusePairProposal && !vSingleParticleMoveForPairProposals) ? 2 : 1;
            for (; vN > 0; --vN) {

                /// it is necessary that we start with particle B as we have
                /// to calculate Q(A|B) and Qb(B|A).
                MinimalParticle vCurrentMinimalParticle = null;
                int vOriginalLabel;
                if(vN > 1) {
                    System.out.println("vN > 1");
                    vCurrentMinimalParticle = vParticleB;
                    vOriginalLabel = vLabelsBeforeJump_B.get(vPC).intValue();
                } else {
                    System.out.println("vN <= 1");
                    vCurrentMinimalParticle = vParticleA;
                    vOriginalLabel = vLabelsBeforeJump_A.get(vPC).intValue();
                }


                /// We calculate the energy and apply them move iff
                /// - the move has not been performed beforehand (a particle
                ///   was sampled twice)
                /// - THE FOLLOWING IS CURRENTLY A NON ISSUE AS B CANNOT BE A':
                ///   particle B is not the reverse particle of particle A (in
                ///   case of m_usePairProposals, i.e. vN > 1). This is important
                ///   because the energy update gets corrupted as particle B is
                ///   not a valid particle before A was applied. To resolve this
                ///   we just perform a one particle move (we don't apply B).

                if (vAppliedParticles.find(vCurrentMinimalParticle) == vAppliedParticles.size()) {
                    /// Calculate the energy difference when changing this candidate:
                    System.out.println("vCurrentMinimalParticle: " + vCurrentMinimalParticle);
                    vTotEnergyDiff += CalculateEnergyDifference(
                            vCurrentMinimalParticle.iIndex,
                            vOriginalLabel,
                            vCurrentMinimalParticle.iCandidateLabel,
                            iIntensityImage.get(vCurrentMinimalParticle.iIndex));
                    System.out.println("vTotEnergyDiff: " + vTotEnergyDiff);
                    /// Finally, perform the (particle-)move
                    System.out.println("MCMCApplyParticle4\n");
                    MCMCApplyParticle(vCurrentMinimalParticle,  false);

                    vAppliedParticles.insert(vCurrentMinimalParticle);
                    vAppliedParticleOrigLabels.add(vOriginalLabel);
                }

                /// Calculate Q(A|B) and Qb(B|A) in case we moved B only; this is
                /// when vN == 2.
//                if(vN == 2) {
//                    /// Get the neighbors (conditional particles) and sum up
//                    /// their proposal values; this is the normalizer for the
//                    /// discrete probability Q(A|B)
//                    MinimalParticleIndexedSetType vParts_Q_AgivenB;
//                    MCMCgetParticlesInFGNeighborhood(vParticleB.m_Index, &vParts_Q_AgivenB);
//                    /// add particle B as this is always a candidate as well
//                    vParticleB.m_Proposal = MCMCproposal(vParticleB.m_Index, vParticleB.m_CandidateLabel);
//                    vParts_Q_AgivenB.insert(vParticleB);
//
//                    if (m_MCMCuseBiasedProposal) {
//                        float vNormalizer_Q_A_B = 0;
//                        for (unsigned int vPI = 0; vPI < vParts_Q_AgivenB.size(); vPI++) {
//                            vNormalizer_Q_A_B += vParts_Q_AgivenB[vPI].m_Proposal;
//                        }
//                        /// vParticleA.m_Proposal is not valid anymore. Particle A
//                        /// got a new proposal when applying particle B.
//                        float vProposalA = MCMCproposal(vParticleA.m_Index, vParticleA.m_CandidateLabel);
//                        vq_A_B[vPC] = vProposalA / vNormalizer_Q_A_B;
//                    } else {
//                        vq_A_B[vPC] = 1.0f / vParts_Q_AgivenB.size();
//                    }
//
//                    /// create A'
//                    MinimalParticleType vReverseParticleA = vParticleA;
//                    vReverseParticleA.m_CandidateLabel = vLabelsBeforeJump_A[vPC];
//                    vReverseParticleA.m_Proposal = MCMCproposal(
//                            vReverseParticleA.m_Index, vReverseParticleA.m_CandidateLabel);
//
//                    /// Calculate Qb(B'|A')
//                    MinimalParticleIndexedSetType vParts_Qb_BgivenA;
//                    MCMCgetParticlesInFGNeighborhood(vParticleA.m_Index, &vParts_Qb_BgivenA);
//                    vParts_Qb_BgivenA.insert(vReverseParticleA);
//                    if (m_MCMCuseBiasedProposal) {
//                        float vNormalizer_Qb_B_A = 0;
//                        for (unsigned int vPI = 0; vPI < vParts_Qb_BgivenA.size();vPI++) {
//                            vNormalizer_Qb_B_A += vParts_Qb_BgivenA[vPI].m_Proposal;
//                        }
//                        /// the proposal of the backward particle (given A) is:
//                        float vProposalBb = MCMCproposal(vParticleB.m_Index,
//                                vLabelsBeforeJump_B[vPC]);
//                        vqb_B_A[vPC] = vProposalBb / vNormalizer_Qb_B_A;
//                    } else {
//                        vqb_B_A[vPC] = 1.0f / vParts_Qb_BgivenA.size();
//                    }
//                }
            }
        }
        
        System.out.println("================= IMPL END =================================");
        //IMPL
        
        // TDOO: TO bo removed after impl. is done
        return true;
    }
    
    float CalculateEnergyDifference(int aIndex, int aCurrentLabel, int aToLabel, float aImgValue) {

//        float vEnergy = 0;
        ContourParticle contourCandidate = new ContourParticle(aCurrentLabel, aImgValue);
        final EnergyResult energyResult = iImageModel.calculateDeltaEnergy(iLabelImage.indexToPoint(aIndex), contourCandidate, aToLabel, iLabelStatistics);
        
//        for(unsigned int vI = 0; vI < m_ExtEnergies.size(); vI++) {
//            vEnergy += m_ExtEnergies[vI]->EvaluateEnergyDifference(
//                    aIndex, aCurrentLabel, aToLabel, aImgValue).first;
//        }
//
//        for(unsigned int vI = 0; vI < m_IntEnergies.size(); vI++) {
//            vEnergy += m_IntEnergies[vI]->EvaluateEnergyDifference(
//                    aIndex, aCurrentLabel, aToLabel);
//        }
        
        return energyResult.energyDifference.floatValue();
    }

    void MCMCApplyParticle(MinimalParticle aCandidateParticle, boolean aDoSimulate){
        System.out.println("-------------------------------------------------------------------------- MCMCApplyParicle");
        System.out.println(mosaic.utils.Debug.getStack(3));
        /// Maintain the regular particle container and the label image:
        MCMCAddAndRemoveParticlesWhenMove(aCandidateParticle);

        ///
        /// Update the label image. The new point is either a contour particle or 0,
        /// therefore the negative label value is set.
        int vFromLabel = Math.abs(iLabelImage.getLabel(aCandidateParticle.iIndex));

        assert(vFromLabel != aCandidateParticle.iCandidateLabel);

        int vSign = -1; /// standard sign of the label image of boundary particles
        if (iLabelImage.isEnclosedByLabelBgConnectivity(aCandidateParticle.iIndex, aCandidateParticle.iCandidateLabel)) {
            /// we created a floating particle.
            vSign = 1;
        }
        
        MCMCPushLabelImageHistory(aCandidateParticle.iIndex);
        iLabelImage.setLabel(aCandidateParticle.iIndex, vSign * aCandidateParticle.iCandidateLabel);

        ///
        /// Update the statistics of the propagating and the loser region.
        if (!aDoSimulate) { // use updateLabelStatistics(...)?
            // TODO: Not sure if correct - test it!
//            UpdateStatisticsWhenJump(aCandidateParticle.iIndex,
//                    iIntensityImage.get(aCandidateParticle.iIndex),
//                    vFromLabel,
//                    aCandidateParticle.iCandidateLabel);
            updateLabelStatistics(iIntensityImage.get(aCandidateParticle.iIndex), vFromLabel, aCandidateParticle.iCandidateLabel);
        }

        /// Update the proposals for all particles in the neighborhood (as they
        /// might have changed).
        if(m_MCMCuseBiasedProposal || (!m_AllowFission || !m_AllowHandles)) {
            MCMCupdateProposalsAndFilterTopologyInNeighborhood(aCandidateParticle);
        }
    }
    
    
    /// Updates the DS (label image and the regular particle containers) when
    /// applying a particle. Note that floating particles will not be updated!
    /// The method only ensures that L and the regular  particles are correct.
    /// The method expects the label image NOT to be updated already. Else the
    /// operations performed will be wrong.
    void MCMCAddAndRemoveParticlesWhenMove(MinimalParticle aCandidateParticle) {
        // Could be split into the following methods:
        //        MCMCRemoveOrphinsAfterMove();
        //        MCMCAddNewParentsAfterMove();
        //        MCMCRemoveInteriorPointsAfterMove();
        //        MCMCAddNewCandidatesAfterMove();

        /// Initilize an neighborhooditerator for fast access to the label image
        /// in the vicinity of the particle of interest.
//        typedef NeighborhoodIterator<LabelImageType> LabelImageNeighborhoodIteratorType;
//        typedef typename LabelImageNeighborhoodIteratorType::RadiusType LabelImageNeighborhoodIteratorRadiusType;
//
//        LabelImageNeighborhoodIteratorRadiusType vLabelImageIteratorRadius;
//        vLabelImageIteratorRadius.Fill(2);
//        LabelImageNeighborhoodIteratorType vLabelImageIterator(
//                vLabelImageIteratorRadius,
//                m_LabelImage,
//                m_LabelImage->GetBufferedRegion());
//        vLabelImageIterator.SetLocation(aCandidateParticle.iIndex);

        /// Initialize locals from the particle
        int vAbsLabelFrom = Math.abs(iLabelImage.getLabel(aCandidateParticle.iIndex)); //vLabelImageIterator.GetCenterPixel());
        int vAbsLabelTo = aCandidateParticle.iCandidateLabel;
        
        /// Initialize a contour index
        int vCandidateIndex = aCandidateParticle.iIndex;
        System.out.println( "      ---   LF: " + vAbsLabelFrom + " LT: " + vAbsLabelTo + " vCandidateIndex: " + vCandidateIndex);

        /// We remove the particle and insert the reverse particle:
        /// Here we cannot decide if the reverse particle is indeed
        /// a floating particle (this is because we apply B first, then A).
        /// The solution is that this method only works on the regular particle
        /// set. Floating particles will be detected  and treated outside of
        /// this method. Here we omit the potential insertion of floating
        /// particles.
        /// In order to (maybe) replace the particle with its reverse particle we:
        /// Simulate the move, calculate the proposal for the backward particle,
        /// create the backward particle, check if the backward particle is
        /// floating, and finally restore the label image:

        int vStoreLabel = iLabelImage.getLabel(aCandidateParticle.iIndex);
        iLabelImage.setLabel(aCandidateParticle.iIndex, -vAbsLabelTo);
        float vProposal = MCMCproposal(vCandidateIndex, vAbsLabelFrom);
        MinimalParticle vReverseParticle = new MinimalParticle(vCandidateIndex, vAbsLabelFrom, vProposal);
        boolean vReverseParticleIsFloating = MCMCParticleHasFloatingProperty(vReverseParticle.iIndex, vReverseParticle.iCandidateLabel);
        
        iLabelImage.setLabel(aCandidateParticle.iIndex, vStoreLabel); //        vLabelImageIterator.SetCenterPixel(vStoreLabel);

        int currIdx = aCandidateParticle.iIndex; // TODO : helper var to be removed later

        /// Insert the reverse particle (if its not floating, see above):
        if (!vReverseParticleIsFloating) {
            MCMCInsertCandidatesToContainers(vReverseParticle, vAbsLabelTo, true);
        }

        /// erase the currently applied particle (if its floating this will not hurt
        /// to call erase here).
        float vDummyProposal2 = 0;//MCMCproposal(vCandidateIndex, vAbsLabelTo);
        MCMCEraseCandidatesFromContainers(new MinimalParticle(vCandidateIndex, vAbsLabelTo, vDummyProposal2), vAbsLabelFrom, true);


        /// Since we are storing the parents separately for each region, the
        /// following patch is needed. We need to shift the mother particle into
        /// the parents container of each others region:
        System.out.println("BEFORE: " + vAbsLabelFrom + "/" + vAbsLabelTo + " " + m_MCMCchildrenProposalNormalizer);
        if (vAbsLabelFrom != 0 && vAbsLabelTo != 0) {
            float vProposal1 = MCMCproposal(vCandidateIndex, 0);
            MCMCEraseCandidatesFromContainers(new MinimalParticle(vCandidateIndex, 0, vProposal1), vAbsLabelFrom, true);
            MCMCInsertCandidatesToContainers(new MinimalParticle(vCandidateIndex, 0, vProposal1), vAbsLabelTo, true);
            System.out.println("AFTER : " + m_MCMCchildrenProposalNormalizer);
        }

        /// What particle would be added or removed to the contour lists
        /// of the currently shrinking region:
        /// (compare to also AddNeighborsAtRemove(..))
        if (vAbsLabelFrom != 0) { // a FG region is shrinking

            for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
                Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];
                int vL = iLabelImage.getLabel(currIdx + iLabelImage.pointToIndex(vOff));
                System.out.println("            vL:" + vL);
                /// Check if new points enter the contour (internal point becomes a parent):
                /// Internal points of this region are positive:
                if (vL > 0 && Math.abs(vL) == vAbsLabelFrom) {
                    float vProposal1 = MCMCproposal(vCandidateIndex + iLabelImage.pointToIndex(vOff), 0);
                    boolean vNotExisted = MCMCInsertCandidatesToContainers(new MinimalParticle(vCandidateIndex + iLabelImage.pointToIndex(vOff), 0, vProposal1), vAbsLabelFrom, true);
                    if (vNotExisted) {
                        iLabelImage.setLabel(currIdx + iLabelImage.pointToIndex(vOff), -vAbsLabelTo);
                        MCMCPushLabelImageHistory(aCandidateParticle.iIndex + iLabelImage.pointToIndex(vOff), vL);
                    }
                }
            }

            for (int vN = 0; vN < m_NeighborhoodSize_FG_Connectivity; vN++) {
                Point vOff = m_NeighborsOffsets_FG_Connectivity[vN];
                int vL = iLabelImage.getLabel(currIdx + iLabelImage.pointToIndex(vOff));
                System.out.println("            vL2:" + vL);
                if (iLabelImage.isBorderLabel(vL)) continue;

                /// check if there are FG-neighbors with no other mother of the
                //  same label --> orphin.
                /// we first 'simulate' the move:
                int vStoreLabel1 = iLabelImage.getLabel(currIdx);
                iLabelImage.setLabel(currIdx, -vAbsLabelTo);
//                vLabelImageIterator.SetCenterPixel(-vAbsLabelTo);

                if (Math.abs(vL) != vAbsLabelFrom) {
                    // check if this neighbor has other mothers from this label:
                    boolean vHasOtherMother = false;
                    for (int vM = 0; vM < m_NeighborhoodSize_FG_Connectivity; vM++) {
                        Point vOff2 = m_NeighborsOffsets_FG_Connectivity[vM];
                        int vL2 = iLabelImage.getLabel(currIdx + iLabelImage.pointToIndex(vOff2.add(vOff)));
                        if (Math.abs(vL2) == vAbsLabelFrom) {
                            vHasOtherMother = true;
                            break;
                        }
                    }
                    if (!vHasOtherMother) {
                        /// The orphin has label equal to what we read from the
                        /// label image and has a candidate label of the
                        /// currently shrinking region.

                        // the proposal is not important; we delete the value
                        float vProposal1 = 0;//MCMCproposal(vCandidateIndex + vOff, vAbsLabelFrom);
                        if (MCMCEraseCandidatesFromContainers(new MinimalParticle(vCandidateIndex + iLabelImage.pointToIndex(vOff), vAbsLabelFrom, vProposal1), Math.abs(vL), true)) {

                        }
                    }
                }
                iLabelImage.setLabel(currIdx, vStoreLabel1);
            }
        }
        System.out.println("===========HISTORY:\n" + m_MCMCParticleInContainerHistory + "\n\n");


        /// Growing: figure out the changes of candidates for the expanding region
        if (vAbsLabelTo != 0) { // we are growing
            /// Neighbors: Figure out what (neighboring)mother points are going
            /// to be interior points:

            /// simulate the move
            int vStoreLabel1 = iLabelImage.getLabel(currIdx);
            iLabelImage.setLabel(currIdx, -vAbsLabelTo);

            for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
                Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];
                int vL = iLabelImage.getLabel(currIdx + iLabelImage.pointToIndex(vOff)); // vLabelImageIterator.GetPixel(vOff);
                // TODO: the isEnclosedByLabel method could use the above iterator
                if (vL == -vAbsLabelTo && iLabelImage.isEnclosedByLabelBgConnectivity(vCandidateIndex + iLabelImage.pointToIndex(vOff), vAbsLabelTo)) {
                    /// Remove the parent that got enclosed; it had a the label
                    /// of the currently expanding region and a candidate label
                    /// of 0.
                    float vProposal1 = 0;//MCMCproposal(vCandidateIndex + vOff, 0);
                    if (MCMCEraseCandidatesFromContainers(new MinimalParticle(vCandidateIndex + iLabelImage.pointToIndex(vOff), 0, vProposal1), vAbsLabelTo, true)) {
                        ///nothing
                    }
                    /// update the label image (we're not using the update
                    /// mechanism of the optimizer anymore):
                    iLabelImage.setLabel(currIdx + iLabelImage.pointToIndex(vOff), Math.abs(vL));
                    
                    MCMCPushLabelImageHistory(aCandidateParticle.iIndex + iLabelImage.pointToIndex(vOff), vL);
                }
            }
            iLabelImage.setLabel(currIdx, vStoreLabel1);

            /// Figure out if a point renders to a candidate. These are
            /// all the FG-neighbors with a different label that are not yet
            /// candidates of the currently expanding region.
            for (int vN = 0; vN < m_NeighborhoodSize_FG_Connectivity; vN++) {
                Point vOff = m_NeighborsOffsets_FG_Connectivity[vN];
                int vL = iLabelImage.getLabel(currIdx + iLabelImage.pointToIndex(vOff)); 
                if (Math.abs(vL) != vAbsLabelTo && !iLabelImage.isBorderLabel(Math.abs(vL))) {
                    /// check if there is no other mother (hence the particle is
                    /// not in the container yet). This we could do by checking the
                    /// neighborhood of the label image or by checking the
                    /// cointainers.
                    /// Here: we check the (not yet updated!) label image.
                    boolean vHasOtherMother = false;
                    for (int vM = 0; vM < m_NeighborhoodSize_FG_Connectivity; vM++) {
                        Point vOff2 = m_NeighborsOffsets_FG_Connectivity[vM];
                        int vL2 = iLabelImage.getLabel(currIdx + iLabelImage.pointToIndex(vOff2.add(vOff)));//vLabelImageIterator.GetPixel(vOff + vOff2);
                        if (Math.abs(vL2) == vAbsLabelTo) {
                            vHasOtherMother = true;
                            break;
                        }
                    }
                    if (!vHasOtherMother) {
                        /// This is a new child. It's current label we have to read
                        /// from the label image, the candidate label is the label of
                        /// the currently expanding region.
                        float vProposal1 = MCMCproposal(vCandidateIndex + iLabelImage.pointToIndex(vOff), vAbsLabelTo);
                        boolean vNotExisted = MCMCInsertCandidatesToContainers(new MinimalParticle(vCandidateIndex + iLabelImage.pointToIndex(vOff), vAbsLabelTo, vProposal1), Math.abs(vL), true);
                        if (vNotExisted) {
                            iLabelImage.setLabel(currIdx + iLabelImage.pointToIndex(vOff), -Math.abs(iLabelImage.getLabel(currIdx + iLabelImage.pointToIndex(vOff))));
                            MCMCPushLabelImageHistory(aCandidateParticle.iIndex + iLabelImage.pointToIndex(vOff), vL);
                        }
                    }
                }
            }
        }
        System.out.println("===========HISTORY2:\n" + m_MCMCParticleInContainerHistory + "\n\n");

    }
    
    void MCMCupdateProposalsAndFilterTopologyInNeighborhood(MinimalParticle aParticle) {
        for (int vN = -1; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
            Point vOff = new Point(new int[m_Dim]);
            if(vN < 0) {
                vOff.zero(); //.Fill(0);
            } else {
               vOff =  m_NeighborsOffsets_BG_Connectivity[vN];
            }

            MinimalParticleIndexedSet vParticleSet = new MinimalParticleIndexedSet();
            MCMCgetRegularParticlesAtIndex(aParticle.iIndex+ iLabelImage.pointToIndex(vOff), vParticleSet);

            for(int vI = 0; vI < vParticleSet.size(); vI++) {
                int vContainerLabel = (vParticleSet.elementAt(vI).iCandidateLabel == 0) ? Math.abs(iLabelImage.getLabel(vParticleSet.elementAt(vI).iIndex)) : vParticleSet.elementAt(vI).iCandidateLabel;

                boolean vTopoOkay = MCMCIsParticleTopoValid(vParticleSet.elementAt(vI));

                if (vTopoOkay) {
                    /// replace the particle with the new propsal
                    float vProposal = MCMCproposal(vParticleSet.elementAt(vI).iIndex, vParticleSet.elementAt(vI).iCandidateLabel);
                    MinimalParticle vUpdatedParticle = new MinimalParticle(vParticleSet.elementAt(vI).iIndex,vParticleSet.elementAt(vI).iCandidateLabel, vProposal);
                    MCMCInsertCandidatesToContainers(vUpdatedParticle, vContainerLabel, true);
                } else {
                    /// remove the particle
                    MCMCEraseCandidatesFromContainers(vParticleSet.elementAt(vI), vContainerLabel, true);
                }
            }
        }
    }
    
    boolean MCMCEraseCandidatesFromContainers(MinimalParticle aParticle, int aCurrentLabel, boolean aDoRecord) {
            boolean vExisted = false;
            MinimalParticle vReplacedParticle = null;

            if (aParticle.iCandidateLabel == 0) {
                MinimalParticleIndexedSet vParticles = m_MCMCparents.get(aCurrentLabel);
                vExisted = vParticles.erase(aParticle);
                if (vExisted) {
                    vReplacedParticle = vParticles.getLastDeletedElement();
                    m_MCMCparentsProposalNormalizer.put(aCurrentLabel, m_MCMCparentsProposalNormalizer.get(aCurrentLabel) - vReplacedParticle.iProposal);
                    m_MCMCTotalNormalizer -= vReplacedParticle.iProposal;
                }
            } else {
                MinimalParticleIndexedSet vParticles = m_MCMCchildren.get(aParticle.iCandidateLabel);
                vExisted = vParticles.erase(aParticle);
                if (vExisted) {
                    vReplacedParticle = vParticles.getLastDeletedElement();
                    m_MCMCchildrenProposalNormalizer.put(aParticle.iCandidateLabel,  m_MCMCchildrenProposalNormalizer.get(aParticle.iCandidateLabel)- vReplacedParticle.iProposal);
                    m_MCMCTotalNormalizer -= vReplacedParticle.iProposal;
                }
            }
            if (vExisted && aDoRecord) {
                ParticleHistoryElement vPHE = new ParticleHistoryElement(vReplacedParticle, aCurrentLabel, false);
                m_MCMCParticleInContainerHistory.add(vPHE);
            }

            return vExisted;
    }
    
    /// Store an event (a change) on the label image. If the Metropolis algorithm
    /// refuses the current move, the m_MCMCLabelImageHistory will help to
    /// undo the changes on the label image and hence help to undo the move.
    void MCMCPushLabelImageHistory(int aI) {
        int vOrigLabel = iLabelImage.getLabel(aI);
        LabelImageHistoryEventType vEvent = new LabelImageHistoryEventType(aI, vOrigLabel);
        m_MCMCLabelImageHistory.add(vEvent);
    }
    
    /// The same as <code>MCMCPushLabelImageHistory(LabelImageIndexType aI)</code>
    /// but we omit a label image lookup if the original label is known.
    void MCMCPushLabelImageHistory(int aI, int aOrigLabel) {
        LabelImageHistoryEventType vEvent = new LabelImageHistoryEventType(aI, aOrigLabel);
        m_MCMCLabelImageHistory.add(vEvent);
    }
    
    // Helper function for creating indexed discrete distributions (starting from 0)
    EnumeratedDistribution<Integer> discreteDistFromList(ArrayList<Float> aList) {
      ArrayList<Pair<Integer, Double>>  pmf = new ArrayList<>();
      
      int index = 0;
      for (int vI = 0; vI < aList.size(); vI++) {
          pmf.add(new Pair<>(index++, (double)aList.get(vI)));
      }

      return new EnumeratedDistribution<>(m_NumberGeneratorBoost, pmf);
    }
      
    void MCMCgetParticlesInBGNeighborhood(int aIndex, MinimalParticleIndexedSet aList) {
        for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
            Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];

            /// append particles in the set
            // TODO: Conversion to index from offset point should be checked? or done nicer?
            MCMCgetRegularParticlesAtIndex(aIndex + iLabelImage.pointToIndex(vOff), aList);
        }
    }
    
    /// Removes all non-simple points from the particle set
    void MCMCTopologyFiltering(MinimalParticleIndexedSet aParticleSet) {
        ArrayList<MinimalParticle> vParticlesToDelete = new ArrayList<>();
        for(int vI = 0; vI < aParticleSet.size(); vI++) {
            MinimalParticle vP = aParticleSet.elementAt(vI);
            if (!MCMCIsParticleTopoValid(vP)) {
                vParticlesToDelete.add(vP);
            }
        }

        for(int vI = 0; vI < vParticlesToDelete.size() ;vI++) {
            aParticleSet.erase(vParticlesToDelete.get(vI));
        }
    }
    
    void MCMCgetPartnerParticleSet(MinimalParticle aParticleA, MinimalParticleIndexedSet aSet) {

        MinimalParticleIndexedSet vConditionalPartsSet = new MinimalParticleIndexedSet();
        MCMCgetParticlesInBGNeighborhood(aParticleA.iIndex, vConditionalPartsSet);
        for(int vI = 0; vI < vConditionalPartsSet.size(); vI++) {
            if(vConditionalPartsSet.elementAt(vI).iCandidateLabel != aParticleA.iCandidateLabel) {
                aSet.insert(vConditionalPartsSet.elementAt(vI));
            }
        }

        /// filter the points to meet topological constraints:
        if(!m_AllowFission || !m_AllowHandles) {
            MCMCTopologyFiltering(aSet);
        }

        /// Insert particle A in the set for B such that it is possible
        /// that we have a single particle move.
        aSet.insert(aParticleA);
    }
    
    boolean MCMCParticleHasFloatingProperty(int aIndex, int aCandLabel) {
        if (aCandLabel > 0) {
            /// This is a daughter. It is floating if there is no mother, i.e. if
            /// there is no corresponding region in the FG neighborhood.
            return iLabelImage.isSingleFgPoint(aIndex, aCandLabel);
        }
        /// else: this is a potential mother (candidate label is 0). According
        /// to the BG connectivity it fulfills the floating property
        /// only if there is no other region in the BG neighborhood. Otherwise
        /// this pixel might well go to the BG label without changing the topo.
        return iLabelImage.isEnclosedByLabelBgConnectivity(aIndex, Math.abs(iLabelImage.getLabel(aIndex)));
    }
    
    float MCMCGetProposalNormalizer(int aCurrentLabel, int aCandidateLabel) {
        if (aCandidateLabel == 0) {
            return m_MCMCparentsProposalNormalizer.get(aCurrentLabel);
        }
        return m_MCMCchildrenProposalNormalizer.get(aCandidateLabel);
    }
    
    boolean MCMCIsRegularParticle(MinimalParticle aParticle, int aCurrentLabel) {
        if (aParticle.iCandidateLabel == LabelImage.BGLabel) {
            return m_MCMCparents.get(aCurrentLabel).contains(aParticle);
        }
        return m_MCMCchildren.get(aParticle.iCandidateLabel).contains(aParticle);
    }
    
    /// Set up a vector MCMCRegionLabel that maps natural numbers (index of
    /// the vector to the region labels). We iterate the count statistics keys
    /// to get the region labels.
    void MCMCUpdateRegionLabelVector() {

        m_MCMCRegionLabel.clear();

//        typename MinimalParticleIndexedSetMapType::iterator vActiveLabelsIt = m_MCMCparents.begin();
//        typename MinimalParticleIndexedSetMapType::iterator vActiveLabelsItEnd = m_MCMCparents.end();
        for (Entry<Integer, MinimalParticleIndexedSet> e : m_MCMCparents.entrySet()) {
            m_MCMCRegionLabel.add(e.getKey());
        }
    }
    
    boolean MCMCOffBoundarySample(boolean aGrowth, int aLabel) {

        /// For the insertion and deletion of floating particles, we use a
        /// constant energy, therefore the MH ratio be equal to the FBR.
        /// Note that we also could use the energy as if we would do the job as
        /// proposal distribution, but we would have to calculate the backward
        /// energy as well.
        float vPdeletion = 0;
        float vPinsertion = 0;

        int vNumberOfFloatingParticles = m_MCMCFloatingParticles.size();
        MinimalParticle vParticle = new MinimalParticle();

        double vProbabilityChild = 0.5; // the target-distribution probability ratio
        boolean vChild = m_NumberGenerator.GetUniformVariate(0, 1) > 0.5;
        if (vChild) {
            vParticle.iCandidateLabel = aLabel;
        } else {
            vParticle.iCandidateLabel = 0;
        }

        /// sample a location
        vParticle.iIndex = MCMCGetIndexFromEdgeDensity();
        int vListIndex = m_MCMCFloatingParticles.find(vParticle);
        boolean vParticleExists = !(vListIndex == m_MCMCFloatingParticles.size());

        if(!aGrowth && vParticleExists) {
            MCMCEraseFloatingParticle(vParticle, false);
            // the backward probability:
            vPinsertion = 1;
            // the forward probability:
            vPdeletion = 1;
        } else if(aGrowth && !vParticleExists) {
            vParticle.iProposal = MCMCproposal(vParticle.iIndex, vParticle.iCandidateLabel);
            MCMCInsertFloatingParticle(vParticle, false);
            // the forward probability:
            vPinsertion = 1;
            // the backward probability:
            vPdeletion = 1;
        } else {
            // reject
            return false;
        }

        return true;
    }
    
    /**
     * Insert a floating particle only if it didn't exist. If it existed, the
     * return value will be false.
     */
    boolean MCMCInsertFloatingParticle(MinimalParticle aParticle, boolean aDoRecord) {
        int vIndex = m_MCMCFloatingParticles.find(aParticle);
        if (m_MCMCFloatingParticles.size() != vIndex) {
            // the particle already exists
            return false;
        }

        m_MCMCFloatingParticles.insert(aParticle);

        if (aDoRecord) {
            ParticleHistoryElement vPHE = new ParticleHistoryElement(aParticle, 0, true);
            m_MCMCFloatingParticleInContainerHistory.add(vPHE);
        }

        m_FloatingParticlesProposalNormalizer += aParticle.iProposal;

        return true;
    }
    
    void MCMCEraseFloatingParticle(MinimalParticle aParticle, boolean aDoRecord) {
        int vIndex = m_MCMCFloatingParticles.find(aParticle);
        if (m_MCMCFloatingParticles.size() == vIndex) {
            return;
        } 
        if(aDoRecord) {
            ParticleHistoryElement vPHE = new ParticleHistoryElement(aParticle, 0, false);
            m_MCMCFloatingParticleInContainerHistory.add(vPHE);
        }

        MinimalParticle vUpdatedParticle = m_MCMCFloatingParticles.elementAt(vIndex);

        if (vUpdatedParticle.iProposal - aParticle.iProposal > Math.ulp(1.0f)*10) {
            /// the particle has still some proposal left
            m_FloatingParticlesProposalNormalizer -= aParticle.iProposal;
            vUpdatedParticle.iProposal -= aParticle.iProposal;
            m_MCMCFloatingParticles.insert(vUpdatedParticle);
        } else {
            /// the particle gets deleted. We only remove the amount from the
            /// normalizer that has been stored in the set.
            m_FloatingParticlesProposalNormalizer -= vUpdatedParticle.iProposal;
            m_MCMCFloatingParticles.erase(vUpdatedParticle);
        }
    }
    
    int MCMCGetIndexFromEdgeDensity() {
        int vIndex = -1;
        
        boolean vNonBoundaryPointFound = false;
        while (!vNonBoundaryPointFound) {
            
            int vLinIndex = m_MCMCEdgeImageDistr.sample();
            
            if (m_Dim == 2 || m_Dim == 3) {
                vIndex = vLinIndex;
            } else {
                // TODO: Should runtime be thrown?
                throw new RuntimeException("Current dirty implementation only supports dimension 2 and 3.");
            }
            if (!iLabelImage.isBorderLabel(iLabelImage.getLabel(vIndex))) {
                vNonBoundaryPointFound = true;
            }
        }
        return vIndex;
    }
    
    void PrepareEnergyCalculationForEachIteration() {
        // Same as in RC algorithm
        if (iSettings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }
    
    void PrintIterationInfo(int aNumberOfAcceptedMoves) {
        // TODO: Add debug info same as in cpp
        System.out.println("aNumberOfAcceptedMoves: " + aNumberOfAcceptedMoves);
    }
    
    void prepareEnergies() {
        // TODO: it is a'la initEnergies from RC
        if (iSettings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
            // Deconvolution: - Alocate and initialize the 'ideal image'
            // TODO: This is not OOP, handling energies should be redesigned
            ((E_Deconvolution) iImageModel.getEdata()).GenerateModelImage(iLabelImage, iLabelStatistics);
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }
    
    /**
     * Initializes statistics. For each found label creates LabelStatistics object and stores it in labelStatistics container.
     * TODO: Same as in AlgorithmRC - reuse somehow.
     */
    private void initStatistics() {
        // First create all LabelStatistics for each found label and calculate values needed for later
        // variance/mean calculations
        int maxUsedLabel = 0;
        for (int i = 0; i < iLabelImage.getSize(); i++) {
            final int absLabel = iLabelImage.getLabelAbs(i);

            if (!iLabelImage.isBorderLabel(absLabel)) {
                if (maxUsedLabel < absLabel) maxUsedLabel = absLabel;

                LabelStatistics stats = iLabelStatistics.get(absLabel);
                if (stats == null) {
                    stats = new LabelStatistics(absLabel, iLabelImage.getNumOfDimensions());
                    iLabelStatistics.put(absLabel, stats);
                }
                final double val = iIntensityImage.get(i);
                System.out.println("label/data: " + absLabel + " " + val);
                stats.iLabelCount++;
                stats.iMeanIntensity += val;
                stats.iVarIntensity += val * val;
            }
        }

        // If background label do not exist add it to collection
        LabelStatistics stats = iLabelStatistics.get(BGLabel);
        if (stats == null) {
            stats = new LabelStatistics(BGLabel, iLabelImage.getNumOfDimensions());
            iLabelStatistics.put(BGLabel, stats);
        }

        // Finally - calculate variance, median and mean for each found label
        for (final LabelStatistics stat : iLabelStatistics.values()) {
            final int n = stat.iLabelCount;
            stat.iVarIntensity = n > 1 ? ((stat.iVarIntensity - stat.iMeanIntensity * stat.iMeanIntensity / n) / (n - 1)) : 0;
            stat.iMeanIntensity = n > 0 ? (stat.iMeanIntensity / n) : 0;
            // Median on start set equal to mean
            stat.iMedianIntensity = stat.iMeanIntensity;
        }

        // Make sure that labelDispenser will not produce again any already used label
        // Safe to search with 'max' we have at least one value in container (background)
//        labelDispenser.setMaxValueOfUsedLabel(maxUsedLabel);
    }
    //TODO: Same as in AlgorithmRC - reuse somehow.
    private void updateLabelStatistics(float aIntensity, int aFromLabelIdx, int aToLabelIdx) {
        final LabelStatistics toLabelStats = iLabelStatistics.get(aToLabelIdx);
        final LabelStatistics fromLabelStats = iLabelStatistics.get(aFromLabelIdx);
        final double toCount = toLabelStats.iLabelCount;
        final double fromCount = fromLabelStats.iLabelCount;

        // Before changing the mean, compute the sum of squares of the samples:
        final double vToLabelSumOfSq = toLabelStats.iVarIntensity * (toCount - 1.0) + toCount * toLabelStats.iMeanIntensity * toLabelStats.iMeanIntensity;
        final double vFromLabelSumOfSq = fromLabelStats.iVarIntensity * (fromCount - 1.0) + fromCount * fromLabelStats.iMeanIntensity * fromLabelStats.iMeanIntensity;

        // Calculate the new means for the background and the label:
        final double vNewMeanToLabel = (toLabelStats.iMeanIntensity * toCount + aIntensity) / (toCount + 1.0);

        // TODO: divide by zero. why does this not happen at itk?
        double vNewMeanFromLabel = (fromCount > 1) ? ((fromCount * fromLabelStats.iMeanIntensity - aIntensity) / (fromCount - 1.0)) : 0.0;

        // Calculate the new variances:
        double newToVar = ((1.0 / (toCount))
                * (vToLabelSumOfSq + aIntensity * aIntensity - 2.0 * vNewMeanToLabel * (toLabelStats.iMeanIntensity * toCount + aIntensity) + (toCount + 1.0) * vNewMeanToLabel * vNewMeanToLabel));

        double newFromVar = (fromCount != 2) ? (1.0 / (fromCount - 2.0))
                * (vFromLabelSumOfSq - aIntensity * aIntensity - 2.0 * vNewMeanFromLabel * (fromLabelStats.iMeanIntensity * fromCount - aIntensity) + (fromCount - 1.0) * vNewMeanFromLabel * vNewMeanFromLabel)
                : 0.0;

        // Update stats
        toLabelStats.iVarIntensity = newToVar;
        fromLabelStats.iVarIntensity = newFromVar;
        toLabelStats.iMeanIntensity = vNewMeanToLabel;
        fromLabelStats.iMeanIntensity = vNewMeanFromLabel;

        // Add a sample point to the BG and remove it from the label-region:
        toLabelStats.iLabelCount++;
        fromLabelStats.iLabelCount--;
    }
    
    boolean MCMCInsertCandidatesToContainers(MinimalParticle aParticle, int aCurrentLabel, boolean aDoRecord) {
        boolean vReplace = false;
        MinimalParticle vReplacedParticle = null;

        if (aParticle.iCandidateLabel == 0) {

            MinimalParticleIndexedSet partSet = m_MCMCparents.get(aCurrentLabel);
            if (partSet == null) {
                partSet = new MinimalParticleIndexedSet();
                m_MCMCparents.put(aCurrentLabel, partSet);
            }
            int vSizeBefore = partSet.size();
            partSet.insert(aParticle);

            if (vSizeBefore < partSet.size()) {
                m_MCMCparentsProposalNormalizer.replace(aCurrentLabel, m_MCMCparentsProposalNormalizer.get(aCurrentLabel) + aParticle.iProposal);
                m_MCMCTotalNormalizer += aParticle.iProposal;
            } else {
                /// this is a replacement:
                float vDiff = aParticle.iProposal - partSet.getLastDeletedElement().iProposal;
                m_MCMCparentsProposalNormalizer.replace(aCurrentLabel, m_MCMCparentsProposalNormalizer.get(aCurrentLabel) + vDiff);
                m_MCMCTotalNormalizer += vDiff;

                vReplace = true;
                vReplacedParticle = partSet.getLastDeletedElement();
            }
        } else {
            System.out.println("MCMCInsertCandidatesToContainers: " + aParticle + " " + m_MCMCchildren.size());
            
            MinimalParticleIndexedSet partSet = m_MCMCchildren.get(aParticle.iCandidateLabel);
            if (partSet == null) {
                partSet = new MinimalParticleIndexedSet();
                m_MCMCchildren.put(aParticle.iCandidateLabel, partSet);
            }
            int vSizeBefore = partSet.size();
            partSet.insert(aParticle);
            if (vSizeBefore < partSet.size()) {
                if (!m_MCMCchildrenProposalNormalizer.containsKey(aParticle.iCandidateLabel)) {m_MCMCchildrenProposalNormalizer.put(aParticle.iCandidateLabel, 0f);}
                m_MCMCchildrenProposalNormalizer.replace(aParticle.iCandidateLabel, m_MCMCchildrenProposalNormalizer.get(aParticle.iCandidateLabel) + aParticle.iProposal);
                m_MCMCTotalNormalizer += aParticle.iProposal;
            } else {
                /// this is a replacement:
                float vDiff = aParticle.iProposal - partSet.getLastDeletedElement().iProposal;
                // TODO: next line might fail - do solution from above (check key and insert if necesarry) Intentinally left not fixed for now.
                m_MCMCchildrenProposalNormalizer.replace(aParticle.iCandidateLabel, m_MCMCchildrenProposalNormalizer.get(aParticle.iCandidateLabel) + vDiff);
                m_MCMCTotalNormalizer += vDiff;

                vReplace = true;
                vReplacedParticle = partSet.getLastDeletedElement();
            }
        }

        if (aDoRecord) {
            /// Note that the order here is important: first insert the particle
            /// that gets replaced. When restoring the state afterwards, the
            /// particle history is iterated in reverse order.
            if (vReplace) {
                ParticleHistoryElement vPHE2 = new ParticleHistoryElement(vReplacedParticle, aCurrentLabel, false);
                m_MCMCParticleInContainerHistory.add(vPHE2);
            }

            ParticleHistoryElement vPHE = new ParticleHistoryElement(aParticle, aCurrentLabel, true);
            m_MCMCParticleInContainerHistory.add(vPHE);
        }

        return !vReplace;
    }
    
    /// Returns false if topology is changed when applying this particle. This
    /// is based on the current state of the label image.
    /// TODO: this method should be changed to achieve full topological control.
    ///       now the particle is rejected if it changes somehow the topology.
    boolean MCMCIsParticleTopoValid(MinimalParticle aParticle) {
        // TODO: Currently not effiecient but seems to be correct with output. Must be checked how to find topo number wihtout looping after reults from getTopologicalNumbersForAllAdjacentLabels
        if(!m_AllowFission || !m_AllowHandles){
            /// get the correct label to access the container of the particle
            int vContainerLabel = (aParticle.iCandidateLabel == 0) ? 
                    Math.abs(iLabelImage.getLabel(aParticle.iIndex)) : aParticle.iCandidateLabel;

            List<TopologicalNumberResult> topologicalNumbersForAllAdjacentLabels = m_TopologicalNumberFunction.getTopologicalNumbersForAllAdjacentLabels(iLabelImage.indexToPoint(aParticle.iIndex));
            TopologicalNumberResult vTopoNb = null;
            for (TopologicalNumberResult tnr : topologicalNumbersForAllAdjacentLabels) {
                if (tnr.iLabel == vContainerLabel) {
                    vTopoNb = tnr;
                    break;
                }
            }
            
            //FGandBGTopoNbPairType vTopoNb = m_TopologicalNumberFunction->EvaluateFGTNOfLabelAtIndex(aParticle.m_Index, vContainerLabel);
            if (vTopoNb == null) {System.out.println("NULL TOPO NUMBER !!!!!!!!!!!!! 1st"); return false;}
            System.out.println("TN: " + vTopoNb.iNumOfConnectedComponentsFG + " " + vTopoNb.iNumOfConnectedComponentsBG);
            if (!(vTopoNb.iNumOfConnectedComponentsFG == 1 && vTopoNb.iNumOfConnectedComponentsBG == 1)) {
                return false;
            }

            /// if the both labels are not 0, we must calculate the
            /// topo numbers for the current and the candidate label.
            if(0 != aParticle.iCandidateLabel) {
//                vTopoNb = m_TopologicalNumberFunction->EvaluateFGTNOfLabelAtIndex(aParticle.iIndex, aParticle.iCandidateLabel);
                vTopoNb = null;
                for (TopologicalNumberResult tnr : topologicalNumbersForAllAdjacentLabels) {
                    if (tnr.iLabel == aParticle.iCandidateLabel) {
                        vTopoNb = tnr;
                        break;
                    }
                }
                if (vTopoNb == null) {System.out.println("NULL TOPO NUMBER !!!!!!!!!!!!!  2nd"); return false;}    
                if (!(vTopoNb.iNumOfConnectedComponentsFG == 1 && vTopoNb.iNumOfConnectedComponentsBG == 1)) {
                    return false;
                }
            }
        }

        return true;
    }
    
    float MCMCproposal(int aIndex, int aCandLabel) {
        if (!m_MCMCuseBiasedProposal) return 1;

        // Length-prior driven proposal:
        float vLength = 0;
        boolean vFloating = true;
        int vL = iLabelImage.getLabelAbs(aIndex);
        Point p = iLabelImage.indexToPoint(aIndex);
        // TODO: maybe change to neighbor iterator
        for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; ++vN) {
            Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];
            int vLN = iLabelImage.getLabelAbs(vOff.add(p));
            if(vLN != vL) {
                vLength += m_MCMClengthProposalMask[vN];
                vFloating = false;
            }
        }

        if (vFloating) {
            vLength = MCMCGetUnnormalizedFloatingParticleProposal();
        }
        return vLength;
    }

    float MCMCGetUnnormalizedFloatingParticleProposal() {
        // floating particles need a proposal > 0: We take half of the smallest element in the mask 
        // (hack:we assume the smallest element to  be at position 0).
        if (!m_MCMCuseBiasedProposal) return 1;
        return m_MCMClengthProposalMask[0]/2.0f;
    }

    //    typedef ConstIndexedHashSet<
    //    MinimalParticleType,
    //    MinimalParticleHashFunctorType,
    //    MinimalParticleHashFunctorType> MinimalParticleIndexedSetType;
    //    
    void MCMCgetRegularParticlesAtIndex(int aIndex, MinimalParticleIndexedSet aList) {
        int currentLabel = iLabelImage.getLabel(aIndex);
        System.out.println("       V: " + iLabelImage.indexToPoint(aIndex) + " = " + currentLabel);
        if (currentLabel == LabelImage.BorderLabel) return;
        int absCurrentLabel = iLabelImage.labelToAbs(currentLabel);

        boolean vParentInserted = false; /// just a speed up
        // TODO: Maybe MaskOnSpaceMapper should be used with pre-computed mask.
        for (Integer idx : iLabelImage.iterateNeighbours(aIndex)) {
            int neighborLabel = iLabelImage.getLabel(idx);
            
            System.out.println("idx: " + iLabelImage.indexToPoint(aIndex) + " n: " + iLabelImage.indexToPoint(idx) + " vNeighPixel: " + neighborLabel);
            
            int absNeighborLabel = iLabelImage.labelToAbs(neighborLabel);
            if (!iLabelImage.isBorderLabel(neighborLabel) && absNeighborLabel != absCurrentLabel) {
                // here should be a particle since two neighboring pixel have a different label.

                // Insert FG labels have a daughter placed at this spot.
                if (neighborLabel != LabelImage.BGLabel) {
                    float vProposal = MCMCproposal(aIndex, absNeighborLabel);
                    aList.insert(new MinimalParticle(aIndex, absNeighborLabel, vProposal));

                }
                
                if (currentLabel != LabelImage.BGLabel && !vParentInserted) {
                    float vProposal = MCMCproposal(aIndex, 0);
                    aList.insert(new MinimalParticle(aIndex, 0, vProposal));
                    System.out.println("  ParentInserted");
                    vParentInserted = true;
                }
            }
        }

        /// Check the BG neighborhood now if we need to insert a parent.
        if(!vParentInserted && currentLabel != LabelImage.BGLabel){ /// speed-up
            for (Integer idx : iLabelImage.iterateBgNeighbours(aIndex)) {
                
                int vNeighPixel = iLabelImage.getLabel(idx);
                System.out.println("--- xxx: " + iLabelImage.indexToPoint(idx) + " " + vNeighPixel);
                if (iLabelImage.labelToAbs(vNeighPixel) != absCurrentLabel) {
                    /// This is a FG pixel with a neighbor of a different label.
                    /// finally, insert a parent particle
                    float vProposal = MCMCproposal(aIndex, 0);
                    aList.insert(new MinimalParticle(aIndex, 0, vProposal));
                    vParentInserted = true;
                    break;
                }
            }
            // C code:
//            for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
//                Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];
//                System.out.println("xxx: " + vOff);
//                int vNeighPixel = iLabelImage.getLabelAbs(vOff);
//
//                if (vNeighPixel != absCurrentLabel) {
//                    /// This is a FG pixel with a neighbor of a different label.
//                    /// finally, insert a parent particle
//                    float vProposal = MCMCproposal(aIndex, 0);
//                    aList.insert(new MinimalParticle(aIndex, 0, vProposal));
//                    vParentInserted = true;
//                    break;
//                }
//            }
        }
    }

    public boolean performIteration() {
        boolean convergence = true;

        // TODO: real job strats here

        return convergence;
    }

    public int getBiggestLabel() {
        return 10;
    }
    
    // DONE so FAR
    
    /**
     * Generates distribution from input image.ds
     * NOTICE: in case if values in input image are too small (< 10*eps(1)) it will provide flat
     * distribution and will change all pixels of input image to 1.0
     * @param aImg input image to generate distribution from
     * @return pair (sum of all pixels in image, generated distribution)
     */
    static Pair<Double, EnumeratedDistribution<Integer>> generateDiscreteDistribution(IntensityImage aImg, Rng am_NumberGeneratorBoost) {
        ArrayList<Pair<Integer, Double>> pmf = new ArrayList<>(aImg.getSize());
        Iterator<Point> ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
        int index = 0;
        double m_MCMCZe = 0;
        while (ri.hasNext()) {
            final Point point = ri.next();
            double value = aImg.get(point);
            m_MCMCZe += value; 
            pmf.add(new Pair<Integer, Double>(index++, value));
        }
        // if sum of all pixels is too small  (< 10 * epislon(1f) as in original code) then just use flat distribution 
        if (m_MCMCZe < 10 * Math.ulp(1.0f)) {
            m_MCMCZe = aImg.getSize();
            index = 0;
            ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
            while (ri.hasNext()) {
                final Point point = ri.next();
                aImg.set(point, 1.0f);
                pmf.set(index, new Pair<Integer, Double>(index, 1.0));
                ++index;
            }
        }

        EnumeratedDistribution<Integer> m_MCMCEdgeImageDistr = new EnumeratedDistribution<>(am_NumberGeneratorBoost, pmf);

        return new Pair<> (m_MCMCZe, m_MCMCEdgeImageDistr);
    }
}
