/*
 * NumericalGenotypePlugin.java
 *
 * Created on May 8, 2008
 *
 */
package net.maizegenetics.analysis.numericaltransform;

import net.maizegenetics.dna.snp.GenotypeTable;
import net.maizegenetics.dna.snp.GenotypeTableBuilder;
import net.maizegenetics.dna.snp.GenotypeTableUtils;
import net.maizegenetics.dna.snp.score.ReferenceProbability;
import net.maizegenetics.dna.snp.score.ReferenceProbabilityBuilder;
import net.maizegenetics.plugindef.AbstractPlugin;
import net.maizegenetics.plugindef.DataSet;
import net.maizegenetics.plugindef.Datum;

import javax.swing.*;

import org.apache.log4j.Logger;

import java.awt.Frame;
import java.util.*;

/**
 * Numerical Genotype Plugin.
 *
 * @author terryc
 */
public class NumericalGenotypePlugin extends AbstractPlugin {
    private static final Logger myLogger = Logger.getLogger(NumericalGenotypePlugin.class);

    public static enum TRANSFORM_TYPE {
        as_minor, as_major, as_missing, use_all_alleles
    };
    
    private TRANSFORM_TYPE myTransformType = TRANSFORM_TYPE.as_minor;

    /** Creates a new instance of the NumericalGenotypePlugin */
    public NumericalGenotypePlugin(Frame parentFrame, boolean isInteractive){
        super(parentFrame, isInteractive);
    }

    @Override
    public DataSet processData(DataSet input) {

        List<Datum> datumList = input.getDataOfType(GenotypeTable.class);

        //check size of datumList, throw error if not equal to one
        if (datumList.size() != 1){
            throw new IllegalArgumentException("NumericalGenotypePlugin: select exactly one genotype dataset to transform.");
        }

        //load the GenotypeTable.
        GenotypeTable myGenotype = (GenotypeTable) datumList.get(0).getData();
        GenotypeTable myNewGenotype;
        StringBuilder nameBuilder = new StringBuilder(datumList.get(0).getName());
        nameBuilder.append("_with_Probability");
        StringBuilder commentBuilder = new StringBuilder(datumList.get(0).getComment());
        
        switch(myTransformType) {
        case as_minor:
        	myNewGenotype = setAlternateMinorAllelesToMinor(myGenotype);
        	commentBuilder.append("\nReference Probability computed by setting alternate minor alleles");
        	commentBuilder.append("\nto the most common minor allele.");
        	break;
        case as_major:
        	myNewGenotype = setAlternateMinorAllelesToMajor(myGenotype);
        	commentBuilder.append("\nReference Probability computed by setting alternate minor alleles");
        	commentBuilder.append("\nto the major allele.");
        	break;
        case as_missing:
        	myNewGenotype = setAlternateMinorAllelesToMissing(myGenotype);
        	commentBuilder.append("\nReference Probability computed by setting alternate minor alleles");
        	commentBuilder.append("\nto missing.");
        	break;
        case use_all_alleles:
        	myNewGenotype = transformToAlleleProbability(myGenotype);
        	commentBuilder.append("\nWith allele probabilities computed.");
        default:
        	return null;
        }
        
        Datum newDatum = new Datum(nameBuilder.toString(), myNewGenotype, commentBuilder.toString());
        return new DataSet(newDatum, this);

    }

    private GenotypeTable setAlternateMinorAllelesToMinor(GenotypeTable myGenotype) {
    	//TODO implement
        int nsites = myGenotype.numberOfSites();
        int ntaxa = myGenotype.numberOfTaxa();

        ReferenceProbability myProb;
        double[][] data = null;
        //if GenotypeTable has no ReferenceProbablity, compute the probabilities.
        if (!myGenotype.hasReferenceProbablity()) {
            data = GenotypeTableUtils.convertGenotypeToDoubleProbability(myGenotype, true);
        }

        //build new ReferenceProbability
        ReferenceProbabilityBuilder refBuilder = ReferenceProbabilityBuilder.getInstance(ntaxa, nsites, myGenotype.taxa());
        for (int t = 0; t < ntaxa; t++) {
            float[] values = new float[nsites];

            for (int s = 0; s < nsites; s++) {
                values[s] = (float) data[s][t];
            }
            refBuilder.addTaxon(t, values);
        }

        //build new GenotypeTable
        GenotypeTable numGenotype = GenotypeTableBuilder.getInstance(myGenotype.genotypeMatrix(), myGenotype.positions(),
                myGenotype.taxa(), myGenotype.depth(), myGenotype.alleleProbability(), refBuilder.build(), myGenotype.dosage(),
                myGenotype.annotations());

    	return numGenotype;
    }
    
    private GenotypeTable setAlternateMinorAllelesToMajor(GenotypeTable myGenotype) {
    	//TODO implement
    	return null;
    }

    private GenotypeTable setAlternateMinorAllelesToMissing(GenotypeTable myGenotype) {
    	//TODO implement
    	return null;
    }

    private GenotypeTable transformToAlleleProbability(GenotypeTable myGenotype) {
    	//TODO implement
    	return null;
    }

    @Override
    public String pluginDescription(){
        return "This plugin creates a  numerical genotype table for input genotype data";
    }

    @Override
    public String getCitation(){
        return null;
    }

    @Override
    public  ImageIcon getIcon(){
        return null;
    }

    @Override
    public String getButtonName(){
        return "Numerical Genotype";
    }


    @Override
    public String getToolTipText(){
        return "Numerical Genotype";
    }


}
