/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maizegenetics.analysis.imputation;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import net.maizegenetics.dna.map.Chromosome;
import net.maizegenetics.dna.snp.FilterGenotypeTable;
import net.maizegenetics.dna.snp.GenotypeTable;
import net.maizegenetics.dna.snp.GenotypeTableBuilder;
import net.maizegenetics.dna.snp.GenotypeTableUtils;
import net.maizegenetics.dna.snp.NucleotideGenotypeTable;
import net.maizegenetics.dna.snp.genotypecall.GenotypeCallTableBuilder;
import org.apache.commons.lang.ArrayUtils;

/**
 * A class to mask input files, hold information related to accuracy calculation,
 * and calculate accuracy for FILLINImputationPlugin
 * @author kls283
 */
public class FILLINImputationAccuracy {
    private GenotypeTable maskKey= null;
    private GenotypeTable unimp= null;
    private GenotypeTable imputed= null;
    private GenotypeTable[] donor= null;
    private String outFile= null;
    private double propSitesMask= 0.01;
    private int depthToMask= 0;
    private double propDepthSitesToMask= 0.2;
    public int[] MAF= null;
    private double[][] all= null; //arrays held ("columns"): 0-maskedMinor, 1-maskedHet, 2-maskedMajor; each array ("rows"):0-to minor, 1-to het, 2-to major, 3-unimp, 4-total for known type
    private double[][][] mafAll= null;//sam as all, but first array holds MAF category
    private double[] MAFClass= null;//new double[]{0,.02,.05,.10,.20,.3,.4,.5,1};
    private boolean verboseOutput= true;
    
    public FILLINImputationAccuracy(GenotypeTable unimp, double propSitesMask, String outFileName, boolean verbose) {
        this.unimp= unimp;
        this.propSitesMask= propSitesMask;
        this.verboseOutput= verbose;
        this.outFile= outFileName;
    }
    
    public FILLINImputationAccuracy(GenotypeTable unimp, int depthToMask, double propDepthSitesToMask, String outFileName, boolean verbose) {
        this.unimp= unimp;
        this.depthToMask= depthToMask;
        this.propDepthSitesToMask= propDepthSitesToMask;
        this.verboseOutput= verbose;
        this.outFile= outFileName;
    }
    
    public FILLINImputationAccuracy(GenotypeTable unimp, double propSitesMask, int depthToMask, double propDepthSitesToMask, String outFileName, boolean verbose) {
        this.unimp= unimp;
        this.propSitesMask= propSitesMask;
        this.depthToMask= depthToMask;
        this.propDepthSitesToMask= propDepthSitesToMask;
        this.verboseOutput= verbose;
        this.outFile= outFileName;
    }
    
    public FILLINImputationAccuracy(GenotypeTable unimp, GenotypeTable maskKey, GenotypeTable[] donor, String outFileName, boolean verbose) {
        this.unimp= unimp;
        this.maskKey= maskKey;
        this.donor= donor;
        this.verboseOutput= verbose;
        this.outFile= outFileName;
    }
    
    public FILLINImputationAccuracy(GenotypeTable unimp, GenotypeTable maskKey, GenotypeTable[] donor, double[] MAFClass, String outFileName, boolean verbose) {
        this.unimp= unimp;
        this.maskKey= maskKey;
        this.donor= donor;
        this.MAFClass= MAFClass;
        this.verboseOutput= verbose;
        this.outFile= outFileName;
    }
    
    public boolean addImputed(GenotypeTable imputed) {
        this.imputed= imputed;
        return true;
    }
    
    public GenotypeTable initiateAccuracy() {
        if (this.MAFClass!=null) {//if mafClass not null, assign MAF categories to sites in unimputed
            generateMAF();
            this.mafAll= new double[this.MAFClass.length][3][5];
            if (this.verboseOutput) System.out.println("Calculating accuracy within supplied MAF categories.");
        }
        //mask file if not already masked (depth or proportion) and generate key
        if (this.maskKey!=null) {
            if (this.verboseOutput) System.out.println("File already masked. Use input key file for calculating accuracy");
            boolean goodKey= true;
            if (Arrays.equals(this.maskKey.physicalPositions(), this.unimp.physicalPositions())==false) goodKey= filterKey();
            if (!goodKey && this.verboseOutput) System.out.println("Problem with input key file. Masking unimputed input");
            else return this.unimp;
        }
        if (this.unimp.hasDepth() && this.depthToMask>0) {
            boolean works= maskFileByDepth();
            if (works) return this.unimp;
        }
        maskPropSites();
        return this.unimp;
    }

        private boolean generateMAF() {
            this.MAF= new int[this.unimp.numberOfSites()];
            for (GenotypeTable don:this.donor) {
                for (int site = 0; site < don.numberOfSites(); site++) {
                    int unimpSite= this.unimp.positions().indexOf(don.positions().get(site));
                    if (unimpSite < 0) {this.MAF[unimpSite]= -1; continue;}
                    int search= Arrays.binarySearch(this.MAFClass, don.minorAlleleFrequency(site));
                    this.MAF[unimpSite]= search<0?Math.abs(search)-1:search;
                }
            }
            return true;
        }

        private boolean maskFileByDepth() {
            if (verboseOutput) System.out.println("Masking file using depth\nSite depth to mask: "+this.depthToMask+"\nProportion of depth sites to be masked: "+this.propDepthSitesToMask);
            GenotypeCallTableBuilder mask= GenotypeCallTableBuilder.getInstance(this.unimp.numberOfTaxa(), this.unimp.numberOfSites());
            GenotypeCallTableBuilder key= GenotypeCallTableBuilder.getInstance(this.unimp.numberOfTaxa(), this.unimp.numberOfSites());

            int cnt= 0;
            for (int taxon = 0; taxon < this.unimp.numberOfTaxa(); taxon++) {
                int taxaCnt= 0;
                mask.setBaseRangeForTaxon(taxon, 0, this.unimp.genotypeAllSites(taxon));
                for (int site = 0; site < this.unimp.numberOfSites(); site++) {
                    if (GenotypeTableUtils.isEqual(NucleotideGenotypeTable.UNKNOWN_DIPLOID_ALLELE, this.unimp.genotype(taxon, site))) continue;
                    int[] currD= this.unimp.depthForAlleles(taxon, site);
                    if (currD[0]+currD[1]!=this.depthToMask) continue;
                    if (Math.random()>this.propDepthSitesToMask) continue;
                    else if ((this.unimp.isHeterozygous(taxon, site)==false) ||
                                (depthToMask > 3 && currD[0] > 1 && currD[1] > 1)|| 
                                (depthToMask < 4)) {
                        mask.setBase(taxon, site, NucleotideGenotypeTable.UNKNOWN_DIPLOID_ALLELE); key.setBase(taxon, site, this.unimp.genotype(taxon, site)); taxaCnt++;
                    }
                }
                if (this.verboseOutput) System.out.println(taxaCnt+" sites masked for "+this.unimp.taxaName(taxon)); cnt+= taxaCnt;
            }
            if (cnt<2000 && this.verboseOutput) {System.out.println("Insufficient sites masked with depth. Calculate accuracy by proportion"); return false;}
            if (this.verboseOutput) System.out.println(cnt+" sites masked at a depth of "+this.depthToMask);
            this.maskKey= GenotypeTableBuilder.getInstance(key.build(), this.unimp.positions(), this.unimp.taxa());
            this.unimp= GenotypeTableBuilder.getInstance(mask.build(), this.unimp.positions(), this.unimp.taxa());
            return true;
        }

        private boolean maskPropSites() {
            if (this.verboseOutput) System.out.println("Masking file without depth\nMasking "+this.propSitesMask+" proportion of sites");
            GenotypeCallTableBuilder mask= GenotypeCallTableBuilder.getInstance(this.unimp.numberOfTaxa(), this.unimp.numberOfSites());
            GenotypeCallTableBuilder key= GenotypeCallTableBuilder.getInstance(this.unimp.numberOfTaxa(), this.unimp.numberOfSites());

            int presGenos= 0;
            for (int taxon = 0; taxon < this.unimp.numberOfTaxa(); taxon++) {presGenos+= this.unimp.totalNonMissingForTaxon(taxon);}
            int expected= (int)(this.propSitesMask*(double)presGenos);
            int cnt= 0;
            for (int taxon = 0; taxon < this.unimp.numberOfTaxa(); taxon++) {
                int taxaCnt= 0;
                mask.setBaseRangeForTaxon(taxon, 0, this.unimp.genotypeAllSites(taxon));
                for (int site = 0; site < this.unimp.numberOfSites(); site++) {
                    if (Math.random()<this.propSitesMask && GenotypeTableUtils.isEqual(NucleotideGenotypeTable.UNKNOWN_DIPLOID_ALLELE, this.unimp.genotype(taxon, site))==false) {
                        mask.setBase(taxon, site, NucleotideGenotypeTable.UNKNOWN_DIPLOID_ALLELE); key.setBase(taxon, site, this.unimp.genotype(taxon, site)); taxaCnt++;
                    }
                }
                cnt+= taxaCnt;
            }
            if (this.verboseOutput) System.out.println(cnt+" sites masked randomly not based on depth ("+expected+" expected at "+this.propSitesMask+")");
            this.maskKey= GenotypeTableBuilder.getInstance(key.build(), this.unimp.positions(), this.unimp.taxa());
            this.unimp= GenotypeTableBuilder.getInstance(mask.build(), this.unimp.positions(), this.unimp.taxa());
            return true;
        }

        //filters for site position and chromosome. returns null if mask has fewer chromosomes or positions in chromosomes than unimp
        private boolean filterKey() {
            if (this.verboseOutput) System.out.println("Filtering user input key file...\nsites in original Key file: "+this.maskKey.numberOfSites());
            String[] unimpNames= new String[this.unimp.numberOfSites()];
            for (int site = 0; site < this.unimp.numberOfSites(); site++) {unimpNames[site]= this.unimp.siteName(site);}
            int[] unimpPos;
            int[] keyPos;
            ArrayList<String> keepSites= new ArrayList<>();
            boolean working= true;
            if (Arrays.equals(this.unimp.chromosomes(), this.maskKey.chromosomes())==false) working= matchChromosomes();
            if (!working) {System.out.println("No overlapping chromosomes"); return false;}
            for (Chromosome chr:this.unimp.chromosomes()) {
                int[] startEndUnimp= this.unimp.firstLastSiteOfChromosome(chr); int[] startEndKey= maskKey.firstLastSiteOfChromosome(chr);
                unimpPos= Arrays.copyOfRange(this.unimp.physicalPositions(), startEndUnimp[0], startEndUnimp[1]+1);
                keyPos= Arrays.copyOfRange(this.maskKey.physicalPositions(), startEndKey[0], startEndKey[1]+1);
                for (int posOnChr = 0; posOnChr < unimpPos.length; posOnChr++) {//if input hapmap sites not in key, return null
                    if (Arrays.binarySearch(keyPos, unimpPos[posOnChr])<0) return false;
                }
                for (int posOnChr = 0; posOnChr < keyPos.length; posOnChr++) {//if key site in input hapmap, retain
                    if (Arrays.binarySearch(unimpPos, keyPos[posOnChr])>-1) {
                        keepSites.add(this.maskKey.siteName(startEndKey[0]+posOnChr));
                    }
                }
            }
            FilterGenotypeTable filter= FilterGenotypeTable.getInstance(this.maskKey, keepSites.toArray(new String[keepSites.size()]));
            this.maskKey= GenotypeTableBuilder.getGenotypeCopyInstance(filter);
            if (verboseOutput) System.out.println("Sites in new mask: "+this.maskKey.numberOfSites());
            return true;
        }

        private boolean matchChromosomes() {
            Chromosome[] unimpChr= this.unimp.chromosomes();
            Chromosome[] keyChr= this.maskKey.chromosomes();
            ArrayList<Integer> keepSites= new ArrayList<>();
            for (Chromosome chr:unimpChr) { //if any of the chromosomes in input do not exist in key, return false (which then masks proportion)
                if (Arrays.binarySearch(keyChr, chr)<0) return false;
            }
            for (Chromosome chr:keyChr) { //keep sites on key that are on matching chromosomes
                if (Arrays.binarySearch(unimpChr, chr)>-1) {
                    int[] startEnd = this.maskKey.firstLastSiteOfChromosome(chr);
                    for (int site = startEnd[0]; site <= startEnd[1]; site++) {
                        keepSites.add(site);
                    }
                }
            }
            FilterGenotypeTable filter= FilterGenotypeTable.getInstance(this.maskKey, ArrayUtils.toPrimitive(keepSites.toArray(new Integer[keepSites.size()])));
            this.maskKey= GenotypeTableBuilder.getGenotypeCopyInstance(filter);
            return true;
        }
        
            //this is the sample multiple r2.
        private double pearsonR2(double[][] all) {
            int size= 0;
            for (int x = 0; x < 3; x++) {size+= (all[x][4]-all[x][3]);}
            double[][] xy= new double[2][size]; //0 is x, 1 is y
            int last= 0;//the last index filled
            for (double x = 0; x < 3; x++) { for (double y = 0; y < 3; y++) {
                    for (int fill = last; fill < last+all[(int)x][(int)y]; fill++) {
                        xy[0][fill]= x;
                        xy[1][fill]= y;
                    }
                    last= last+(int)all[(int)x][(int)y];
                }}
            double meanX= 0; double meanY= 0; double varX= 0; double varY= 0; double covXY= 0; double r2= 0.0;
            for (int i = 0; i < xy[0].length; i++) {meanX+=xy[0][i]; meanY+= xy[1][i];}
            meanX= meanX/(xy[0].length-1); meanY= meanY/(xy[1].length-1);
            double currX, currY;
            for (int i = 0; i < xy[0].length; i++) {
                currX= xy[0][i]-meanX; currY= xy[1][i]-meanY;
                varX+= currX*currX; varY+= currY*currY;
                covXY+= currX*currY;
            }
            r2= (covXY/(Math.sqrt(varX)*Math.sqrt(varY)))*(covXY/(Math.sqrt(varX)*Math.sqrt(varY)));
            if (this.verboseOutput) System.out.println("Unadjusted R2 value for "+size+" comparisons: "+r2);
            return r2;
        }

        private void accuracyOut(double time) {
            DecimalFormat df = new DecimalFormat("0.########");
            double r2= pearsonR2(this.all);
            try {
                File outputFile = new File(this.outFile.substring(0, this.outFile.indexOf(".hmp")) + "DepthAccuracy.txt");
                DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
                outStream.writeBytes("##Taxon\tTotalSitesMasked\tTotalSitesCompared\tTotalPropUnimputed\tNumMinor\tCorrectMinor\tMinorToHet\tMinorToMajor\tUnimpMinor"
                        + "\tNumHets\tHetToMinor\tCorrectHet\tHetToMajor\tUnimpHet\tNumMajor\tMajorToMinor\tMajorToHet\tCorrectMajor\tUnimpMajor\tR2\n");
                outStream.writeBytes("##TotalByImputed\t"+(this.all[0][4]+this.all[1][4]+this.all[2][4])+"\t"+(this.all[0][4]+this.all[1][4]+this.all[2][4]-this.all[0][3]-this.all[1][3]-this.all[2][3])+"\t"+
                        ((this.all[0][3]+this.all[1][3]+this.all[2][3])/(this.all[0][4]+this.all[1][4]+this.all[2][4]))+"\t"+this.all[0][4]+"\t"+this.all[0][0]+"\t"+this.all[0][1]+"\t"+this.all[0][2]+"\t"+this.all[0][3]+
                        "\t"+this.all[1][4]+"\t"+this.all[1][0]+"\t"+this.all[1][1]+"\t"+this.all[1][2]+"\t"+this.all[1][3]+"\t"+this.all[2][4]+"\t"+this.all[2][0]+"\t"+this.all[2][1]+"\t"+this.all[2][2]+
                        "\t"+this.all[2][3]+"\t"+r2+"\n");
                outStream.writeBytes("#Minor=0,Het=1,Major=2;x is masked(known), y is predicted\nx\ty\tN\tprop\n"
                        +0+"\t"+0+"\t"+this.all[0][0]+"\t"+df.format((this.all[0][0])/(this.all[0][0]+this.all[0][1]+this.all[0][2]))+"\n"
                        +0+"\t"+.5+"\t"+this.all[0][1]+"\t"+df.format((this.all[0][1])/(this.all[0][0]+this.all[0][1]+this.all[0][2]))+"\n"
                        +0+"\t"+1+"\t"+this.all[0][2]+"\t"+df.format((this.all[0][2])/(this.all[0][0]+this.all[0][1]+this.all[0][2]))+"\n"
                        +.5+"\t"+0+"\t"+this.all[1][0]+"\t"+df.format((this.all[1][0])/(this.all[1][0]+this.all[1][1]+this.all[1][2]))+"\n"
                        +.5+"\t"+.5+"\t"+this.all[1][1]+"\t"+df.format((this.all[1][1])/(this.all[1][0]+this.all[1][1]+this.all[1][2]))+"\n"
                        +.5+"\t"+1+"\t"+this.all[1][2]+"\t"+df.format((this.all[1][2])/(this.all[1][0]+this.all[1][1]+this.all[1][2]))+"\n"
                        +1+"\t"+0+"\t"+this.all[2][0]+"\t"+df.format((this.all[2][0])/(this.all[2][0]+this.all[2][1]+this.all[2][2]))+"\n"
                        +1+"\t"+.5+"\t"+this.all[2][1]+"\t"+df.format((this.all[2][1])/(this.all[2][0]+this.all[2][1]+this.all[2][2]))+"\n"
                        +1+"\t"+1+"\t"+this.all[2][2]+"\t"+df.format((this.all[2][2])/(this.all[2][0]+this.all[2][1]+this.all[2][2]))+"\n");
                outStream.writeBytes("#Proportion unimputed:\n#minor <- "+this.all[0][3]/this.all[0][4]+"\n#het<- "+this.all[1][3]/this.all[1][4]+"\n#major<- "+this.all[2][3]/this.all[2][4]+"\n");
                outStream.writeBytes("#Time to impute and calculate accuracy: "+time+" seconds");
                if (this.verboseOutput) System.out.println("##Taxon\tTotalSitesMasked\tTotalSitesCompared\tTotalPropUnimputed\tNumMinor\tCorrectMinor\tMinorToHet\tMinorToMajor\tUnimpMinor"
                        + "\tNumHets\tHetToMinor\tCorrectHet\tHetToMajor\tUnimpHet\tNumMajor\tMajorToMinor\tMajorToHet\tCorrectMajor\tUnimpMajor\tR2");
                if (this.verboseOutput) System.out.println("TotalByImputed\t"+(this.all[0][4]+this.all[1][4]+this.all[2][4])+"\t"+(this.all[0][4]+this.all[1][4]+this.all[2][4]-this.all[0][3]-this.all[1][3]-this.all[2][3])+"\t"+
                        ((this.all[0][3]+this.all[1][3]+this.all[2][3])/(this.all[0][4]+this.all[1][4]+this.all[2][4]))+"\t"+this.all[0][4]+"\t"+this.all[0][0]+"\t"+this.all[0][1]+"\t"+this.all[0][2]+"\t"+this.all[0][3]+
                        "\t"+this.all[1][4]+"\t"+this.all[1][0]+"\t"+this.all[1][1]+"\t"+this.all[1][2]+"\t"+this.all[1][3]+"\t"+this.all[2][4]+"\t"+this.all[2][0]+"\t"+this.all[2][1]+"\t"+this.all[2][2]+
                        "\t"+this.all[2][3]+"\t"+r2);
                if (this.verboseOutput) System.out.println("Proportion unimputed:\nminor: "+this.all[0][3]/this.all[0][4]+"\nhet: "+this.all[1][3]/this.all[1][4]+"\nmajor: "+this.all[2][3]/this.all[2][4]);
                if (this.verboseOutput) System.out.println("#Minor=0,Het=1,Major=2;x is masked(known), y is predicted\nx\ty\tN\tprop\n"
                        +0+"\t"+0+"\t"+this.all[0][0]+"\t"+(this.all[0][0])/(this.all[0][0]+this.all[0][1]+this.all[0][2])+"\n"
                        +0+"\t"+.5+"\t"+this.all[0][1]+"\t"+(this.all[0][1])/(this.all[0][0]+this.all[0][1]+this.all[0][2])+"\n"
                        +0+"\t"+1+"\t"+this.all[0][2]+"\t"+(this.all[0][2])/(this.all[0][0]+this.all[0][1]+this.all[0][2])+"\n"
                        +.5+"\t"+0+"\t"+this.all[1][0]+"\t"+(this.all[1][0])/(this.all[1][0]+this.all[1][1]+this.all[1][2])+"\n"
                        +.5+"\t"+.5+"\t"+this.all[1][1]+"\t"+(this.all[1][1])/(this.all[1][0]+this.all[1][1]+this.all[1][2])+"\n"
                        +.5+"\t"+1+"\t"+this.all[1][2]+"\t"+(this.all[1][2])/(this.all[1][0]+this.all[1][1]+this.all[1][2])+"\n"
                        +1+"\t"+0+"\t"+this.all[2][0]+"\t"+(this.all[2][0])/(this.all[2][0]+this.all[2][1]+this.all[2][2])+"\n"
                        +1+"\t"+.5+"\t"+this.all[2][1]+"\t"+(this.all[2][1])/(this.all[2][0]+this.all[2][1]+this.all[2][2])+"\n"
                        +1+"\t"+1+"\t"+this.all[2][2]+"\t"+(this.all[2][2])/(this.all[2][0]+this.all[2][1]+this.all[2][2])+"\n");
                outStream.close();
            } catch (Exception e) {
                if (this.verboseOutput) System.out.println(e);
            }
        }

        private void accuracyMAFOut() {
            DecimalFormat df = new DecimalFormat("0.########");
            if (this.MAF!=null && this.MAFClass!=null) try {
                File outputFile = new File(this.outFile.substring(0, this.outFile.indexOf(".hmp")) + "DepthAccuracyMAF.txt");
                DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
                outStream.writeBytes("##\tMAFClass\tTotalSitesMasked\tTotalSitesCompared\tTotalPropUnimputed\tNumHets\tHetToMinor\tHetToMajor\tCorrectHet\tUnimpHet\tNumMinor\tMinorToMajor\tMinorToHet\tCorrectMinor\t"
                        + "UnimpMinor\tNumMajor\tMajorToMinor\tMajorToHet\tCorrectMajor\tUnimputedMajor\tr2\n");
                for (int i= 0; i<this.MAFClass.length;i++) {
                    outStream.writeBytes("##TotalByImputed\t"+this.MAFClass[i]+"\t"+(this.mafAll[i][0][4]+this.mafAll[i][1][4]+this.mafAll[i][2][4])+"\t"+(this.mafAll[i][0][4]+this.mafAll[i][1][4]+this.mafAll[i][2][4]-this.mafAll[i][0][3]-this.mafAll[i][1][3]-this.mafAll[i][2][3])+"\t"+
                        ((this.mafAll[i][0][3]+this.mafAll[i][1][3]+this.mafAll[i][2][3])/(this.mafAll[i][0][4]+this.mafAll[i][1][4]+this.mafAll[i][2][4]))+"\t"+this.mafAll[i][0][4]+"\t"+this.mafAll[i][0][0]+"\t"+this.mafAll[i][0][1]+"\t"+this.mafAll[i][0][2]+"\t"+this.mafAll[i][0][3]+
                        "\t"+this.mafAll[i][1][4]+"\t"+this.mafAll[i][1][0]+"\t"+this.mafAll[i][1][1]+"\t"+this.mafAll[i][1][2]+"\t"+this.mafAll[i][1][3]+"\t"+this.mafAll[i][2][4]+"\t"+this.mafAll[i][2][0]+"\t"+this.mafAll[i][2][1]+"\t"+this.mafAll[i][2][2]+
                        "\t"+this.mafAll[i][2][3]+"\t"+pearsonR2(this.mafAll[i])+"\n");
                }
                outStream.writeBytes("#MAFClass,Minor=0,Het=1,Major=2;x is masked(known), y is predicted\nMAF\tx\ty\tN\tprop\n");
                for (int i= 0; i<this.MAFClass.length;i++) { outStream.writeBytes(
                        this.MAFClass[i]+"\t"+0+"\t"+0+"\t"+this.mafAll[i][0][0]+"\t"+df.format((this.mafAll[i][0][0])/(this.mafAll[i][0][0]+this.mafAll[i][0][1]+this.mafAll[i][0][2]))+"\n"
                        +this.MAFClass[i]+"\t"+0+"\t"+.5+"\t"+this.mafAll[i][0][1]+"\t"+df.format((this.mafAll[i][0][1])/(this.mafAll[i][0][0]+this.mafAll[i][0][1]+this.mafAll[i][0][2]))+"\n"
                        +this.MAFClass[i]+"\t"+0+"\t"+1+"\t"+this.mafAll[i][0][2]+"\t"+df.format((this.mafAll[i][0][2])/(this.mafAll[i][0][0]+this.mafAll[i][0][1]+this.mafAll[i][0][2]))+"\n"
                        +this.MAFClass[i]+"\t"+.5+"\t"+0+"\t"+this.mafAll[i][1][0]+"\t"+df.format((this.mafAll[i][1][0])/(this.mafAll[i][1][0]+this.mafAll[i][1][1]+this.mafAll[i][1][2]))+"\n"
                        +this.MAFClass[i]+"\t"+.5+"\t"+.5+"\t"+this.mafAll[i][1][1]+"\t"+df.format((this.mafAll[i][1][1])/(this.mafAll[i][1][0]+this.mafAll[i][1][1]+this.mafAll[i][1][2]))+"\n"
                        +this.MAFClass[i]+"\t"+.5+"\t"+1+"\t"+this.mafAll[i][1][2]+"\t"+df.format((this.mafAll[i][1][2])/(this.mafAll[i][1][0]+this.mafAll[i][1][1]+this.mafAll[i][1][2]))+"\n"
                        +this.MAFClass[i]+"\t"+1+"\t"+0+"\t"+this.mafAll[i][2][0]+"\t"+df.format((this.mafAll[i][2][0])/(this.mafAll[i][2][0]+this.mafAll[i][2][1]+this.mafAll[i][2][2]))+"\n"
                        +this.MAFClass[i]+"\t"+1+"\t"+.5+"\t"+this.mafAll[i][2][1]+"\t"+df.format((this.mafAll[i][2][1])/(this.mafAll[i][2][0]+this.mafAll[i][2][1]+this.mafAll[i][2][2]))+"\n"
                        +this.MAFClass[i]+"\t"+1+"\t"+1+"\t"+this.mafAll[i][2][2]+"\t"+df.format((this.mafAll[i][2][2])/(this.mafAll[i][2][0]+this.mafAll[i][2][1]+this.mafAll[i][2][2]))+"\n");
                }
                outStream.writeBytes("#Proportion unimputed:\n#MAF\tminor\thet\tmajor\n");
                for (int i= 0; i<this.MAFClass.length;i++) { 
                    outStream.writeBytes("#"+this.MAFClass[i]+"\t"+this.mafAll[i][0][3]/this.mafAll[i][0][4]+"\t"+this.mafAll[i][1][3]/this.mafAll[i][1][4]+"\t"+this.mafAll[i][2][3]/this.mafAll[i][2][4]+"\n");
                }
                outStream.flush();
                outStream.close();
            } catch (Exception e) {
                if (this.verboseOutput) System.out.println(e);
            }
        }

        public double calcAccuracy(GenotypeTable imputed, double runtime) {
            this.imputed= imputed;
            this.all= new double[3][5];
            byte diploidN= GenotypeTable.UNKNOWN_DIPLOID_ALLELE;
            boolean use= false; boolean mafOn= false; int maf= -1;
            if (this.mafAll!=null) {use= true; mafOn= true;}
            for (int taxon = 0; taxon < this.imputed.numberOfTaxa(); taxon++) {
                int keyTaxon= maskKey.taxa().indexOf(this.imputed.taxaName(taxon));//if key file contains fewer taxa, or different numbers, or different order
                if (keyTaxon<0) continue;//if doesn't exist, skip
                for (int site = 0; site < this.imputed.numberOfSites(); site++) {
                    use= (mafOn && this.MAF[site] > -1)?true:false;
                    if (use) maf= this.MAF[site];
                    byte known = maskKey.genotype(keyTaxon, site);
                    if (known == diploidN) continue;
                    byte imp = this.imputed.genotype(taxon, site);
                    if (GenotypeTableUtils.isHeterozygous(known) == true) {
                        this.all[1][4]++; if (use) this.mafAll[maf][1][4]++;
                        if (imp == diploidN) {this.all[1][3]++; if (use) this.mafAll[maf][1][3]++;}
                        else if (GenotypeTableUtils.isEqual(imp, known) == true) {this.all[1][1]++; if (use) this.mafAll[maf][1][1]++;}
                        else if (GenotypeTableUtils.isHeterozygous(imp) == false && GenotypeTableUtils.isPartiallyEqual(imp, this.unimp.minorAllele(site)) == true) {this.all[1][0]++; if (use) this.mafAll[maf][1][0]++;}//to minor 
                        else if (GenotypeTableUtils.isHeterozygous(imp) == false && GenotypeTableUtils.isPartiallyEqual(imp, this.unimp.majorAllele(site)) == true) {this.all[1][2]++; if (use) this.mafAll[maf][1][2]++;}
                        else {this.all[1][4]--;  if (use) this.mafAll[maf][1][4]--;}//implies >2 allele states at given genotype
                    } else if (known == GenotypeTableUtils.getDiploidValue(this.unimp.minorAllele(site),this.unimp.minorAllele(site))) {
                        this.all[0][4]++; if (use) this.mafAll[maf][0][4]++;
                        if (imp == diploidN) {this.all[0][3]++;  if (use) this.mafAll[maf][0][3]++;}
                        else if (GenotypeTableUtils.isEqual(imp, known) == true) {this.all[0][0]++;  if (use) this.mafAll[maf][0][0]++;}
                        else if (GenotypeTableUtils.isHeterozygous(imp) == true && GenotypeTableUtils.isPartiallyEqual(imp, known) == true) {this.all[0][1]++;  if (use) this.mafAll[maf][0][1]++;}
                        else {this.all[0][2]++;  if (use) this.mafAll[maf][0][3]++;}
                    } else if (known == GenotypeTableUtils.getDiploidValue(this.unimp.majorAllele(site),this.unimp.majorAllele(site))) {
                        this.all[2][4]++;  if (use) this.mafAll[maf][2][4]++;
                        if (imp == diploidN) {this.all[2][3]++;  if (use) this.mafAll[maf][2][3]++;}
                        else if (GenotypeTableUtils.isEqual(imp, known) == true) {this.all[2][2]++; if (use) this.mafAll[maf][2][2]++;}
                        else if (GenotypeTableUtils.isHeterozygous(imp) == true && GenotypeTableUtils.isPartiallyEqual(imp, known) == true) {this.all[2][1]++;  if (use) this.mafAll[maf][2][1]++;}
                        else {this.all[2][0]++; if (use) this.mafAll[maf][2][0]++;}
                    } else continue;
                }
            }
            accuracyOut(runtime);
            if (this.MAFClass!=null) accuracyMAFOut();
            return pearsonR2(this.all);
        }
          
}