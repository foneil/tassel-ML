/*
 * UTagCountToTagPairPlugin
 */
package net.maizegenetics.analysis.gbs;

import net.maizegenetics.plugindef.AbstractPlugin;
import net.maizegenetics.plugindef.DataSet;
import net.maizegenetics.plugindef.PluginParameter;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 *
 * @author Fei Lu
 */
public class UTagCountToTagPairPlugin extends AbstractPlugin {

    private Logger logger = Logger.getLogger(UTagCountToTagPairPlugin.class);

    private PluginParameter<Double> errorTolerance
            = new PluginParameter.Builder<>("errorTolerance", 0.03, Double.class)
            .description("What fraction of errors to tolerate when filtering by UNEAK")
            .required(false)
            .guiName("Error tolerance")
            .build();
    private PluginParameter<String> infile
            = new PluginParameter.Builder<>("inputFile", null, String.class)
            .required(true)
            .inFile()
            .guiName("Input file")
            .description("Input file of merged tag counts")
            .build();
    private PluginParameter<String> outfile
            = new PluginParameter.Builder<>("outputFile", null, String.class)
            .required(true)
            .outFile()
            .guiName("Output file")
            .description("Output file of matched tag pairs")
            .build();



    public UTagCountToTagPairPlugin() {
        super(null, false);
    }

    public UTagCountToTagPairPlugin(Frame parentFrame) {
        super(parentFrame, false);
    }


    @Override
    public DataSet processData(DataSet input) {
        File pd;
        String mergedTagCountOfAllS, tagPairS;
        mergedTagCountOfAllS = new File(inputFile()).getAbsolutePath();
        tagPairS = new File(outputFile()).getAbsolutePath();
        UNetworkFilter unf = new UNetworkFilter(mergedTagCountOfAllS, errorTolerance(), tagPairS);
        return null;
    }

    @Override
    public String pluginDescription(){
        return "This plugin takes a set of merged tag counts from the TASSEL GBS pipeline and converts it into a set " +
                "of tag pairs according to the UNEAK filter (see citation). This plugin is intended for GBS with organisms " +
                "that lack a reference genome.";
    }

    @Override
    public String getCitation(){
        return "Lu F, Lipka AE, Elshire RJ, Glaubitz JC, Cherney JH, Casler MD, Buckler ES, Costich DE. (2013)"
         + " Switchgrass genomic diversity, ploidy and evolution: novel insights from a network-based SNP discovery protocol. PLoS Genetics 9(1):e1003215.";
    }

    public UTagCountToTagPairPlugin inputFile(String filename){
        setParameter(infile.cmdLineName(), filename);
        return this;
    }

    public UTagCountToTagPairPlugin outputFile(String filename){
        setParameter(outfile.cmdLineName(), filename);
        return this;
    }

    public UTagCountToTagPairPlugin errorTolerance(Double value){
        setParameter(errorTolerance.cmdLineName(), value);
        return this;
    }

    public String inputFile(){
        return infile.value();
    }

    public String outputFile(){
        return outfile.value();
    }

    public double errorTolerance(){
        return errorTolerance.value();
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }

    @Override
    public String getButtonName() {
        return "Tag counts to tag pairs";
    }

    @Override
    public String getToolTipText() {
        return "Tag counts to tag pairs";
    }
}
