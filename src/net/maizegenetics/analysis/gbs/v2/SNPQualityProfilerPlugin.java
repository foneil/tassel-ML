package net.maizegenetics.analysis.gbs.v2;


import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import net.maizegenetics.dna.snp.Allele;
import net.maizegenetics.dna.snp.NucleotideAlignmentConstants;
import net.maizegenetics.dna.tag.*;
import net.maizegenetics.plugindef.AbstractPlugin;
import net.maizegenetics.plugindef.DataSet;
import net.maizegenetics.plugindef.GeneratePluginCode;
import net.maizegenetics.plugindef.PluginParameter;
import net.maizegenetics.taxa.TaxaList;
import net.maizegenetics.taxa.TaxaListIOUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Scores all discovered SNPs for various coverage, depth, and genotypic statistics for a given set of taxa (samples).
 * For each subset of taxa, there are expectations for segregation that can be used to determine whether the SNP is
 * behaving appropriately.
 *
 * @author Ed Buckler
 */
public class SNPQualityProfilerPlugin extends AbstractPlugin {

    private static final Logger myLogger = Logger.getLogger(SAMToGBSdbPlugin.class);

    private PluginParameter<String> myTaxaFile = new PluginParameter.Builder<String>("taxa", null, String.class).guiName("Taxa List File").required(true).inFile()
            .description("Name of taxa list input file in taxa list format").build();
    private PluginParameter<String> myDBFile = new PluginParameter.Builder<String>("o", null, String.class).guiName("GBS DB File").required(true).outFile()
            .description("Name of output file (e.g. GBSv2.db)").build();
    private TagDataSQLite tagDataWriter;

    public SNPQualityProfilerPlugin() {
        super(null, false);
    }

    public SNPQualityProfilerPlugin(Frame parentFrame, boolean isInteractive) {
        super(parentFrame, isInteractive);
    }

    @Override
    public DataSet processData(DataSet input) {
        tagDataWriter =new TagDataSQLite(gBSDBFile());
        TaxaList taxaList=tagDataWriter.getTaxaList();
        TaxaList subTaxa=TaxaListIOUtils.readTaxaAnnotationFile(taxaListFile(),"<NAME>");
        System.out.println("sublist");


        subTaxa.stream().filter(t -> taxaList.indexOf(t)<0).forEach(t-> System.err.println("Missing taxon from master:" + t));
        int[] subsetIndices=subTaxa.stream().mapToInt(taxaList::indexOf).filter(i -> i > -1).sorted().toArray();
       // int[] subsetIndices= IntStream.range(0,taxaList.numberOfTaxa()).toArray();  //for testing using all taxa
        Comparator<int[]> arrayCompare=Comparator.comparing(depths -> -Arrays.stream(depths).sum());

        LongAdder adder=new LongAdder();
        tagDataWriter.getSNPPositions().stream()
                .forEach(position -> {
                    Multimap<Allele,TaxaDistribution> aTDMMap=tagDataWriter.getAllelesTaxaDistForSNP(position);  //this is slow
                    Map<Allele, int[]> subDepths=convertToSubsetMap(aTDMMap,subsetIndices);
                    List<int[]> depthsInOrder=subDepths.values().stream().sorted(arrayCompare).collect(Collectors.toList());
                    JSONObject jsonObject=new JSONObject();
                    //depth stats
                    int[] alleleDepths=depthsInOrder.stream().mapToInt(depths -> Arrays.stream(depths).sum()).toArray();
                    double totalDepth=(double)Arrays.stream(alleleDepths).sum();
                    jsonObject.put("avgDepth",totalDepth/(double)subsetIndices.length);
                    if(totalDepth>0) {
                        jsonObject.put("minorDepthProp", alleleDepths.length > 1 ? alleleDepths[1] / totalDepth : 0.0);
                        jsonObject.put("minor2DepthProp", alleleDepths.length > 2 ? alleleDepths[2] / totalDepth : 0.0);
                        int gapDepth = subDepths.entrySet().stream()
                                .filter(ent -> ent.getKey().allele() == NucleotideAlignmentConstants.GAP_ALLELE)
                                .mapToInt(ent -> Arrays.stream(ent.getValue()).sum()).sum();
                        Arrays.stream(subDepths.getOrDefault(NucleotideAlignmentConstants.GAP_ALLELE, new int[0])).sum();
                        jsonObject.put("gapDepthProp", (double) gapDepth / totalDepth);
                        //coverage stats
                        int[] coverage=new int[subsetIndices.length];
                        for (int[] depths : depthsInOrder) {
                            for (int i = 0; i < depths.length; i++) coverage[i]+=depths[i];
                        }
                        jsonObject.put("propCovered", (double)Arrays.stream(coverage).filter(d -> d>0).count()/(double)coverage.length);
                        jsonObject.put("propCovered2", (double)Arrays.stream(coverage).filter(d -> d>1).count()/(double)coverage.length);
                        jsonObject.put("taxaCntWithMinorAlleleGE2", alleleDepths.length > 1 ?  Arrays.stream(depthsInOrder.get(1)).filter(d -> d>1).count() : 0);

                        //genotypic stats
                        GenotypeStats genotypeCnt=callGenotypes(depthsInOrder.get(0),depthsInOrder.get(1));
                        jsonObject.put("genotypeCnt",genotypeCnt.totalCnt);
                        jsonObject.put("hetFreq_DGE2",genotypeCnt.hetCnt);
                        jsonObject.put("inbredF_DGE2",genotypeCnt.f);


                        //System.out.println("jsonObject:" + jsonObject.toJSONString());
                        if((Double)jsonObject.getOrDefault("inbredF_DGE2",0.0)>0.9 && (Integer)jsonObject.getOrDefault("genotypeCnt",0)>10) {
                            System.out.println(adder.intValue()+"\t"+jsonObject.get("inbredF_DGE2")+"\t"+jsonObject.get("minorDepthProp"));
                        }
                        adder.increment();
                        //System.out.println(adder.intValue());
                    }
                });


        System.out.println(Arrays.toString(subsetIndices));

        //TaxaListIOUtils.exportAnnotatedTaxaListTable();
        return null;
    }

    //Calculate the genotypic classes for all taxa with a depth greater than 2.
    // Returned in a array of counts [homozygous major, homozygous minor, heterozygous]
    private GenotypeStats callGenotypes(int[] majorAlleleDepth, int[] minorAlleleDepth) {
        int[] genotypes=new int[3];
        for (int i = 0; i < majorAlleleDepth.length; i++) {
            if(majorAlleleDepth[i]+minorAlleleDepth[i]<2) continue;
            int genotype=(majorAlleleDepth[i]>0)?1:0;
            genotype+=(minorAlleleDepth[i]>0)?2:0;
            genotypes[genotype-1]++;
        }
        return new GenotypeStats(genotypes[0],genotypes[2],genotypes[1]);
    }

    private class GenotypeStats {
        double majorFreq, minorFreq;
        int homoMajorCnt, hetCnt, homoMinorCnt;
        int totalCnt;
        double f;

        private GenotypeStats(int homoMajorCnt, int hetCnt, int homoMinorCnt) {
            this.homoMajorCnt = homoMajorCnt;
            this.hetCnt = hetCnt;
            this.homoMinorCnt = homoMinorCnt;
            totalCnt=homoMajorCnt+hetCnt+homoMinorCnt;
            majorFreq=((double)homoMajorCnt+(double)hetCnt*0.5)/(double)totalCnt;
            minorFreq=1-majorFreq;
            double expHets = 2.0 * minorFreq * majorFreq;
            double propHets=(double)hetCnt/(double)totalCnt;
            f = 1.0 - (propHets / expHets);
        }
    }



    /*
    Reduces the TaxaDistributions to depths by allele-> selected subset of taxa
    Some alleles are represented by multiple tags and they are collapsed.
     */
    private Map<Allele, int[]> convertToSubsetMap(Multimap<Allele,TaxaDistribution> aTDMMap, int[] subsetIndices) {
        Map<Allele, int[]> result=new HashMap<>();
        for (Allele allele : aTDMMap.keySet()) {
            int[] subDepths=new int[subsetIndices.length];
            for (TaxaDistribution taxaDistribution : aTDMMap.get(allele)) {
                int[] depths=taxaDistribution.depths();
                for (int i = 0; i < subDepths.length; i++) {
                    subDepths[i]+=depths[subsetIndices[i]];
                }
            }
            result.put(allele,subDepths);
        }
        return result;
    }




//    The following getters and setters were auto-generated.
//    Please use this method to re-generate.
//
//    public static void main(String[] args) {
//         GeneratePluginCode.generate(SNPQualityProfilerPlugin.class);
//    }


    @Override
    public ImageIcon getIcon() {
        return null;
    }

    @Override
    public String getButtonName() {
        return "SNP Quality Profiler";
    }

    @Override
    public String getToolTipText() {
        return "SNP Quality Profiler";
    }

    // The following getters and setters were auto-generated.
    // Please use this method to re-generate.
    //
    // public static void main(String[] args) {
    //     GeneratePluginCode.generate(SNPQualityProfilerPlugin.class);
    // }

    /**
     * Convenience method to run plugin with one return object.
     */
    public TagData runPlugin(DataSet input) {
        return (TagData) performFunction(input).getData(0).getData();
    }

    /**
     * Name of taxa list input file in taxa list format
     *
     * @return Taxa List File
     */
    public String taxaListFile() {
        return myTaxaFile.value();
    }

    /**
     * Set Taxa List File. Name of taxa list input file in
     * taxa list format
     *
     * @param value Taxa List File
     *
     * @return this plugin
     */
    public SNPQualityProfilerPlugin taxaListFile(String value) {
        myTaxaFile = new PluginParameter<>(myTaxaFile, value);
        return this;
    }

    /**
     * Name of output file (e.g. GBSv2.db)
     *
     * @return GBS DB File
     */
    public String gBSDBFile() {
        return myDBFile.value();
    }

    /**
     * Set GBS DB File. Name of output file (e.g. GBSv2.db)
     *
     * @param value GBS DB File
     *
     * @return this plugin
     */
    public SNPQualityProfilerPlugin gBSDBFile(String value) {
        myDBFile = new PluginParameter<>(myDBFile, value);
        return this;
    }
}
