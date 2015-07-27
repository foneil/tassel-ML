/*
 * DiscoverySNPCallerPluginV2
 */
package net.maizegenetics.analysis.gbs.v2;

import static net.maizegenetics.dna.snp.NucleotideAlignmentConstants.GAP_ALLELE;
import static net.maizegenetics.dna.snp.NucleotideAlignmentConstants.GAP_ALLELE_CHAR;
import static net.maizegenetics.dna.snp.NucleotideAlignmentConstants.getNucleotideAlleleByte;

import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.ImageIcon;

import net.maizegenetics.dna.map.Chromosome;
import net.maizegenetics.dna.map.GeneralPosition;
import net.maizegenetics.dna.map.GenomeSequence;
import net.maizegenetics.dna.map.GenomeSequenceBuilder;
import net.maizegenetics.dna.map.Position;
import net.maizegenetics.dna.snp.Allele;
import net.maizegenetics.dna.snp.SimpleAllele;
import net.maizegenetics.dna.tag.Tag;
import net.maizegenetics.dna.tag.TagBuilder;
import net.maizegenetics.dna.tag.TagDataSQLite;
import net.maizegenetics.dna.tag.TagDataWriter;
import net.maizegenetics.dna.tag.TaxaDistBuilder;
import net.maizegenetics.dna.tag.TaxaDistribution;
import net.maizegenetics.plugindef.AbstractPlugin;
import net.maizegenetics.plugindef.DataSet;
import net.maizegenetics.plugindef.PluginParameter;
import net.maizegenetics.util.Tuple;

import org.apache.log4j.Logger;
import org.biojava.nbio.alignment.Alignments;
import org.biojava.nbio.alignment.template.AlignedSequence;
import org.biojava.nbio.alignment.template.Profile;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.util.ConcurrencyTools;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

/**
 * This class aligns tags at the same physical location against one another,
 * calls SNPs, and then outputs the SNPs to a HapMap file.
 *
 * It is multi-threaded, as there are substantial speed increases with it.
 *
 * @author Ed Buckler
 * @author Jeff Glaubitz
 */
public class DiscoverySNPCallerPluginV2 extends AbstractPlugin {

    private static final Logger myLogger = Logger.getLogger(DiscoverySNPCallerPluginV2.class);

    private PluginParameter<String> myInputDB = new PluginParameter.Builder<>("db", null, String.class).guiName("Input GBS Database").required(true).inFile()
            .description("Input Database file if using SQLite").build();
    private PluginParameter<Double> myMinMinorAlleleFreq = new PluginParameter.Builder<>("mnMAF", 0.01, Double.class).guiName("Min Minor Allele Freq")
            .description("Minimum minor allele frequency").build();
    private PluginParameter<Double> myMinLocusCoverage = new PluginParameter.Builder<>("mnLCov", 0.1, Double.class).guiName("Min Locus Coverage")
            .description("Minimum locus coverage (proportion of Taxa with a genotype)").build();
    private PluginParameter<String> myRefGenome = new PluginParameter.Builder<>("ref", null, String.class).guiName("Reference Genome File").inFile()
            .description("Path to reference genome in fasta format. Ensures that a tag from the reference genome is always included "
                    + "when the tags at a locus are aligned against each other to call SNPs. The reference allele for each site "
                    + "is then provided in the output HapMap files, under the taxon name \"REFERENCE_GENOME\" (first taxon). "
                    + "DEFAULT: Don't use reference genome.").build();
    private PluginParameter<Chromosome> myStartChr = new PluginParameter.Builder<>("sC", null, Chromosome.class).guiName("Start Chromosome").required(false)
            .description("Start Chromosome").build();
    private PluginParameter<Chromosome> myEndChr = new PluginParameter.Builder<>("eC", null, Chromosome.class).guiName("End Chromosome").required(false)
            .description("End Chromosome").build();
    private PluginParameter<Boolean> myIncludeRareAlleles = new PluginParameter.Builder<>("inclRare", false, Boolean.class).guiName("Include Rare Alleles")
            .description("Include the rare alleles at site (3 or 4th states)").build();
    private PluginParameter<Boolean> myIncludeGaps = new PluginParameter.Builder<>("inclGaps", false, Boolean.class).guiName("Include Gaps")
            .description("Include sites where major or minor allele is a GAP").build();
    private PluginParameter<Boolean> myCallBiSNPsWGap = new PluginParameter.Builder<>("callBiSNPsWGap", false, Boolean.class).guiName("Call Biallelic SNPs with Gap")
            .description("Include sites where the third allele is a GAP (mutually exclusive with inclGaps)").build();
    private PluginParameter<Double> myGapAlignmentThreshold = new PluginParameter.Builder<>("gapAlignRatio", 1.0, Double.class).guiName("Gap Alignment Threshold")
            .description("Gap alignment threshold ratio of indel contrasts to non indel contrasts: IC/(IC + NC)."
            		+ " Any loci with a tag alignment value above this threshold will be excluded from the pool.").build();
    private PluginParameter<Integer> maxTagsPerCutSite = new PluginParameter.Builder<Integer>("maxTagsCutSite", 64, Integer.class).guiName("Max Number of Cut Sites").required(false)
            .description("Maximum number of tags per cut site").build();
    private PluginParameter<Boolean> myDeleteOldData = new PluginParameter.Builder<>("deleteOldData", false, Boolean.class).guiName("Delete Previous Discovery Data")
            .description("Delete existing SNP data from tables").build();
    
    private TagDataWriter tagDataWriter = null;
    private boolean includeReference = false;
    private static GenomeSequence myRefSequence = null; 
    private boolean customSNPLogging = true;  // a custom SNP log that collects useful info for filtering SNPs through machine learning criteria
//    private CustomSNPLog myCustomSNPLog = null;
    private boolean customFiltering = false;

    public DiscoverySNPCallerPluginV2() {
        super(null, false);
    }

    public DiscoverySNPCallerPluginV2(Frame parentFrame, boolean isInteractive) {
        super(parentFrame, isInteractive);
    }

    @Override
    public DataSet processData(DataSet input) {
        myLogger.info("Finding SNPs in " + inputDB() + ".");
        myLogger.info(String.format("StartChr:%s EndChr:%s %n", startChromosome(), endChromosome()));
        tagDataWriter =new TagDataSQLite(inputDB());
 
        if (deleteOldData()) {
            myLogger.info("deleteOldData is TRUE: Clearing previous Discovery and SNPQuality data");
            tagDataWriter.clearSNPQualityData();
            tagDataWriter.clearDiscoveryData();
        }
        // Get list of stored chromosomes, we'll process a subset of this list
        List<Chromosome> myChroms = tagDataWriter.getChromosomesFromCutPositions();
        if (myChroms == null || myChroms.size() == 0) {
            myLogger.error("No Chromosomes found in cutPosition tables");
            try{
                ((TagDataSQLite)tagDataWriter).close();
            } catch (Exception ex) {ex.printStackTrace();}
            return null;
        }
        Collections.sort(myChroms); // put in order
       
        Chromosome startChrom = myStartChr.isEmpty() ? myChroms.get(0) : startChromosome();
        Chromosome endChrom = myEndChr.isEmpty() ? myChroms.get(myChroms.size()-1) : endChromosome();
        if (startChrom.compareTo(endChrom) > 0) {
            String message = "The start chromosome " + startChrom.getName() 
                    + " is larger than the end chromosome " + endChrom.getName();
            myLogger.error(message);
            try{
                ((TagDataSQLite)tagDataWriter).close();
            } catch (Exception ex) {ex.printStackTrace();}
            return null;
        } 
        List<Chromosome> chromsToProcess=myChroms.stream()
                .filter(chrom -> { 
                    // Only keep chromosomes within our range.  The chromosome 
                    // constructor stored the string as a name, but also attempted
                    // to create an integer from the name.  
                    //
                    // The "compareTo" method of Chromosome compares the chrom number if
                    // available, otherwise does a string compare on the name. 
                    return (chrom.compareTo(startChrom) >=0 && chrom.compareTo(endChrom) <=0);
                })
                .collect(Collectors.toList());

        chromsToProcess.stream()
            .forEach(chr -> {
            	        myLogger.info("Start processing chromosome " + chr + "\n");
                        Multimap<Tag, Allele> chromosomeAllelemap = HashMultimap.create();
                        tagDataWriter.getCutPositionTagTaxaMap(chr, -1, -1).entrySet().stream()
                                .forEach(emp -> {
                                    Multimap<Tag, Allele> tm = findAlleleByAlignment(emp.getKey(), emp.getValue(), chr);
                                    if (tm != null) chromosomeAllelemap.putAll(tm);
                                }); 
                        myLogger.info("Finished processing chromosome " + chr + "\n\n");
                        tagDataWriter.putTagAlleles(chromosomeAllelemap);                       
                    }
            );
        ConcurrencyTools.shutdown();
        try{
            ((TagDataSQLite)tagDataWriter).close();
        } catch (Exception ex) {ex.printStackTrace();}
        return null;
    }

    @Override
    public void postProcessParameters() {

        if (myInputDB.isEmpty() || !Files.exists(Paths.get(inputDB()))) {
            throw new IllegalArgumentException("DiscoverySNPCallerPlugin: postProcessParameters: Input DB not set or found");
        }
        if (!myRefGenome.isEmpty()) {
            includeReference = true;
            myRefSequence = GenomeSequenceBuilder.instance(referenceGenomeFile());
        }
        if (callBiallelicSNPsWithGap() && includeGaps()) {
            throw new IllegalArgumentException("The callBiSNPsWGap option is mutually exclusive with the inclGaps option.");
        }
        if (!myStartChr.isEmpty() && !myEndChr.isEmpty()) {
            if (startChromosome().compareTo(endChromosome()) > 0) {
                throw new IllegalArgumentException("The start chromosome is larger than the end chromosome.");
            } 
        } 
        myLogger.info(String.format("MinMAF:%g %n", minMinorAlleleFreq()));
        myLogger.info(String.format("includeRare:%s includeGaps:%s %n", includeRareAlleles(), includeGaps()));
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }

    @Override
    public String getButtonName() {
        return "Discovery SNP Caller";
    }

    @Override
    public String getToolTipText() {
        return "Discovery SNP Caller";
    }


    /**
     * Method takes all tags and their taxa distributions at a single cut Position and then identifies the segregating
     * SNPs relative to the reference genome.  Each tag can result in multiple allele being called (Allele contain SNP
     * Position as one of their attributes).  It does not record, which taxa have which SNPs.
     * The steps are:
     * 1. Ensure more than 1 tag (otherwise would be monomorphic).
     * 2. Get the reference sequence
     * 3. Ensure meets minimal coverage
     * 4. Align using BioJava 
     * 5. Filters the aligned tags against a user defined gap alignment ratio threshold (default is include all tags)
     * 6. convert remaining tags to Guava Table 
     * 7. Evaluate whether SNPs meet MAF and coverage levels.
     * 8. Return SNPs in Tag -> Allele map
     * genome.
     * @param cutPosition the cut position that all tags start with
     * @param tagTaxaMap  map of Tag -> Tuple (Boolean if reference, TaxaDistribution)
     * @return multimap of tag -> allele
     */
    Multimap<Tag,Allele> findAlleleByAlignment(Position cutPosition, Map<Tag,Tuple<Boolean,TaxaDistribution>> tagTaxaMap, Chromosome chromosome) {
        if(tagTaxaMap.isEmpty()) return null;  //todo why would this be empty?
        final int numberOfTaxa=tagTaxaMap.values().stream().findFirst().get().y.maxTaxa();
        if((minMinorAlleleFreq()>0) && (tagTaxaMap.size()<2)) {//homozygous
            return null;  //consider reporting homozygous
        }
        if(!tagTaxaMap.keySet().stream().anyMatch(Tag::isReference)) {
        	if (myRefSequence != null) {
               	tagTaxaMap = createReferenceTag(cutPosition.getPosition(), tagTaxaMap, chromosome, numberOfTaxa);
               	if (tagTaxaMap == null) return null;
        	} else {
        		tagTaxaMap=setCommonToReference(tagTaxaMap);
        	}
        }
        final double taxaCoverage=tagTaxaMap.values().stream().mapToInt(t -> t.y.numberOfTaxaWithTag()).sum()/(double)numberOfTaxa;  //todo this could be changed to taxa with tag
        if(taxaCoverage < myMinLocusCoverage.value()) {
            return null;  //consider reporting low coverage
        }
        Map<Tag,String> alignedTagsUnfiltered=alignTags(tagTaxaMap,maxTagsPerCutSite());
        if (alignedTagsUnfiltered == null || alignedTagsUnfiltered.size() == 0) {
        	// Errors related to CompoundNotFound were logged in alignTags. 
        	return null;
        }        
        // Filter the aligned tags.  Throw out all tags from a loci
        // that has any tag with a gap ratio that exceeds the threshold
        Map<Tag,String> alignedTags = filterAlignedTags(alignedTagsUnfiltered, cutPosition, myGapAlignmentThreshold.value());
        if (alignedTags == null || alignedTags.size() == 0) return null;
        Table<Position, Byte, List<TagTaxaDistribution>> tAlign=convertAlignmentToTagTable(alignedTags, tagTaxaMap,  cutPosition);
        List<Position> positionToKeep=tAlign.rowMap().entrySet().stream()
                .filter(entry -> (double) numberTaxaAtSiteIgnoreGaps(entry.getValue()) / (double) numberOfTaxa > minLocusCoverage())
                .filter(entry -> {
                    List<Tuple<Byte,Integer>> aC=alleleTaxaCounts(entry.getValue());
                    if(minMinorAlleleFreq()<=0) return true;  //permits export of monomorphic SNPs
                    //if(aC.size()>1) System.out.printf("%s %d %g %g %g%n",entry.getKey().toString(),aC.get(1).y, minMinorAlleleFreq(),taxaCoverage,(minMinorAlleleFreq()*taxaCoverage*(double)numberOfTaxa));
                    return (aC.size()>1 && aC.get(1).y>(minMinorAlleleFreq()*taxaCoverage*(double)numberOfTaxa));
                })
                .map(Map.Entry::getKey) //get Position
                .collect(Collectors.toList());
        //todo convert to stream
        Multimap<Tag,Allele> tagAllelemap= HashMultimap.create();
        for (Position position: positionToKeep) {
            for (Map.Entry<Byte, List<TagTaxaDistribution>> entry : tAlign.row(position).entrySet()) {
                Allele allele=new SimpleAllele(entry.getKey(),position);
                for (TagTaxaDistribution tagTaxaDistribution : entry.getValue()) {
                	Tag currentTag = tagTaxaDistribution.tag();
                	if (currentTag.isReference() && myRefSequence != null) ; //don't add ref tag from ref genome to map
                	else
                        tagAllelemap.put(currentTag,allele);
                }
            }
        }
        //System.out.printf("%s SNPNum:%d \n",cutPosition.toString(),tagAllelemap.size());
        return tagAllelemap;
    }

    private static Map<Tag,Tuple<Boolean,TaxaDistribution>> setCommonToReference(Map<Tag,Tuple<Boolean,TaxaDistribution>> tagTaxaMap) {
        Tag commonTag=tagTaxaMap.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().y.numberOfTaxaWithTag()))
                .map(e -> e.getKey())
                .get();
        Tuple<Boolean,TaxaDistribution> commonTD=tagTaxaMap.get(commonTag);
        Tag refTag=TagBuilder.instance(commonTag.seq2Bit(),commonTag.seqLength()).reference().build();
        ImmutableMap.Builder<Tag,Tuple<Boolean,TaxaDistribution>> tagTaxaMapBuilder=new ImmutableMap.Builder<>();
        for (Map.Entry<Tag, Tuple<Boolean, TaxaDistribution>> entry : tagTaxaMap.entrySet()) {
            if(entry.getKey()!=commonTag) {tagTaxaMapBuilder.put(entry);}
            else {tagTaxaMapBuilder.put(refTag,commonTD);}
        }
        return tagTaxaMapBuilder.build();
    }

    private static Map<Tag,Tuple<Boolean,TaxaDistribution>> createReferenceTag(int cutPosition, 
            Map<Tag,Tuple<Boolean,TaxaDistribution>> tagTaxaMap, Chromosome myChrom, int numberOfTaxa) {

        Tag refTag = null;
        // Find the longest sequence length of all tags
        short longestTag = tagTaxaMap.keySet().stream()
                .max(Comparator.comparingInt(key -> key.seqLength()))
                .map(key -> key.seqLength())
                .get();

        // Create reference tag from reference genome, where start position=cutPosition,
        // and end position = cutPosition+longestTag-1 
        byte[] seqInBytes = myRefSequence.chromosomeSequence(myChrom, cutPosition, cutPosition + longestTag-1 );
        if (seqInBytes == null) {
            String msg = "Error creating reference tag at position " + cutPosition + " with length " + longestTag
                    + ". Position not found in reference file.  " 
                    + ". Please verify the reference file used for the plugin matches the reference file used for the aligner.";
            myLogger.error(msg);
            return null;
        }
        refTag = TagBuilder.instance(seqInBytes,longestTag).reference().build();  // setting reference in the tag     
        ImmutableMap.Builder<Tag,Tuple<Boolean,TaxaDistribution>> tagTaxaMapBuilder=new ImmutableMap.Builder<>();
        TaxaDistribution refTD=TaxaDistBuilder.create(numberOfTaxa); // is numberOfTaxa an appropriate value to use?  		
        tagTaxaMapBuilder.put(refTag,new Tuple<>(true,refTD));
        for (Map.Entry<Tag, Tuple<Boolean, TaxaDistribution>> entry : tagTaxaMap.entrySet()) {
            tagTaxaMapBuilder.put(entry);
        }
        return tagTaxaMapBuilder.build();
    }

    private static List<Tuple<Byte,Integer>> alleleTaxaCounts(Map<Byte, List<TagTaxaDistribution>> alleleDistMap) {
        List<Tuple<Byte,Integer>> alleleCnt = new ArrayList<>();
        for (Map.Entry<Byte, List<TagTaxaDistribution>> entry : alleleDistMap.entrySet()) {
            alleleCnt.add(new Tuple<>(entry.getKey(),numberOfTaxa(entry.getValue())));
        }
        Collections.sort(alleleCnt, (Tuple<Byte, Integer> a, Tuple<Byte, Integer> b) -> b.y.compareTo(a.y));  //a,b reversed so common allele  first
        return alleleCnt;
    }

    /**
     * Aligns a set of tags anchored to the same reference position.
     * @return map with tag(values) mapping to String with alignment
     */
    private static Map<Tag,String> alignTags(Map<Tag,Tuple<Boolean,TaxaDistribution>> tags, int maxTagsPerCutSite) {
        List<DNASequence> lst=new ArrayList<>();
 
//        tags.forEach((tag, dir) -> {
//            String sequence = (dir.x) ? tag.sequence() : tag.toReverseComplement();
//            try {
//                DNASequence ds = new DNASequence(sequence);
//                ds.setUserCollection(ImmutableList.of(tag));
//                lst.add(ds);
//            } catch (CompoundNotFoundException ex) {
//                // Skip any tag whose sequence could not be made into a DNASequence object.
//                myLogger.error("DSNPCaller:alignTags, compoundNotFound exception from DNASequence call for: " + sequence);
//                myLogger.debug(ex.getMessage(),ex); 
//                return;
//            }
//        }); 
        
        // Replacing the  streams code above with the old style forEach due to
        // the biojava 4 CompoundNotFoundException.  Group consensus is that a
        // CompoundNotFoundException should halt processing and return.
        // Streams "forEach" does not allow a "break" or "return with a value". 
        for (Map.Entry<Tag, Tuple<Boolean, TaxaDistribution>> entry : tags.entrySet())
        {
        	Tag tag = entry.getKey();
        	Tuple<Boolean, TaxaDistribution> dir = entry.getValue();
            String sequence = (dir.x) ? tag.sequence() : tag.toReverseComplement();
            try {
                DNASequence ds = new DNASequence(sequence);
                ds.setUserCollection(ImmutableList.of(tag));
                lst.add(ds);
            } catch (CompoundNotFoundException ex) {
                // This shouldn't occur and indicates a coding error in previous processing
                myLogger.error("DSNPCaller:alignTags, compoundNotFound exception from DNASequence call for: " + sequence);
                myLogger.debug(ex.getMessage(),ex); 
                return null;
            }
        }        
        ImmutableMap.Builder<Tag,String> result=new ImmutableMap.Builder<>();
        if(lst.size()==1) {
            Tag tag=(Tag)((ImmutableList)lst.get(0).getUserCollection()).get(0);
            result.put(tag,tag.sequence());
            return result.build();
        }
        
        if (lst.size() > maxTagsPerCutSite) {
            // biojava getMultipleSequenceAligment() can handle aligning only so many tags.
            return null;
        }
        // Alignments.getmultipleSequenceAlignment aligns the tags against each other using
        // the ClustalW algorithm
        Profile<DNASequence, NucleotideCompound> profile = Alignments.getMultipleSequenceAlignment(lst);
        for (AlignedSequence<DNASequence, NucleotideCompound> compounds : profile) {
            ImmutableList tagList=(ImmutableList)compounds.getOriginalSequence().getUserCollection();
            result.put((Tag)tagList.get(0),compounds.getSequenceAsString());
        }
        return result.build();
    }
    
    /**
     * Takes the aligned tags and compares to the reference genome. If there are too many indels
     * for the tag to be a good match,throw out the alignment. Threshold is user defined via the  
     * myGapAlignmentThreshold plugin parameter.  Default value is 1.0 (keep all tags)
     *   Algorithm:  gap alignment ratio = IC/(IC + NC)
     */
    private static Map<Tag,String> filterAlignedTags(Map<Tag,String> alignedTags, Position refStartPosition, double threshold) {
    	Map<Tag,String> filteredTags = new HashMap<Tag, String>();
        final List<Optional<Position>> referencePositions=referencePositions(refStartPosition,alignedTags);
        // Java 8 "alignedTags.forEach(tag,value) -> {...} " is not used as it does not support "break".
        for (Map.Entry<Tag, String> entry : alignedTags.entrySet())
        {
        	Tag tag = entry.getKey();
        	String value = entry.getValue();
            int IC = 0;
            int NC = 0;
            for (int index = 0; index < value.length(); index++) {
                byte allele=getNucleotideAlleleByte(value.charAt(index));
                Optional<Position> p=referencePositions.get(index);
                if(!p.isPresent()){
                    // If both are missing, continue.  OTherwise increment indel-contrast
                    if (allele == GAP_ALLELE) continue;
                    IC++;
                } else {
                    if (allele == GAP_ALLELE) IC++;
                    else NC++;
                }                
            }
            
            double alignValue = (double)(IC / (double)(IC + NC));
            if (alignValue <= threshold) {
            	filteredTags.put(tag, value);
            } else {
            	// Toss the entire loci if any of the tags fail the alignment threshold.
            	filteredTags.clear();
            	return null;
            }
        }
        return filteredTags;
    }
    /**
     * Converts at TagAlignment to a Guava Table with
     * @param alignedTags
     * @param tagTaxaDistMap
     * @param refStartPosition
     * @return
     */
    private static Table<Position, Byte, List<TagTaxaDistribution>> convertAlignmentToTagTable(Map<Tag,String> alignedTags,
                        Map<Tag,Tuple<Boolean,TaxaDistribution>> tagTaxaDistMap, Position refStartPosition) {
        Table<Position, Byte, List<TagTaxaDistribution>> alignT= TreeBasedTable.create(); //These could be sorted by depth
        final List<Optional<Position>> referencePositions=referencePositions(refStartPosition,alignedTags);
        alignedTags.forEach((t,s) -> {
            TagTaxaDistribution td=new TagTaxaDistribution(t,tagTaxaDistMap.get(t).y);
            for (int i = 0; i < s.length(); i++) {
                byte allele=getNucleotideAlleleByte(s.charAt(i));
                Optional<Position> p=referencePositions.get(i);
                if(!p.isPresent()) continue;  //skip alignment sites not in reference
                List tdList=alignT.get(p.get(),allele);
                if(tdList!=null) {tdList.add(td);} // Adding more taxaDist for this allele at this position.
                else {
                    List<TagTaxaDistribution> ttdL=new ArrayList<>();
                    ttdL.add(td);
                    alignT.put(p.get(),allele, ttdL);
                }
            }
        });
        return alignT;
    }

    private static String toString(Table<Position, Byte, List<TagTaxaDistribution>> snpTagTable) {
        StringBuilder sb=new StringBuilder();
        sb.append(String.format("Rows %d Columns %d\n", snpTagTable.rowKeySet().size(), snpTagTable.columnKeySet().size()));
        sb.append("Columns bases:");
        snpTagTable.columnKeySet().stream().forEach(b -> sb.append(b+","));
        sb.append("\n");
        snpTagTable.rowMap().forEach((p, ttd) -> {
            sb.append(String.format("Position: %d", p.getPosition()));
            for (Map.Entry<Byte, List<TagTaxaDistribution>> byteListEntry : ttd.entrySet()) {
                sb.append(String.format("\tAllele: %d ->", byteListEntry.getKey()));
                byteListEntry.getValue().forEach(tt -> sb.append(String.join(",",tt.tag().sequence()+"#"+tt.taxaDist().numberOfTaxaWithTag()+" ")));//not doing this correctly
            }
            sb.append("\n");

        });
        return sb.toString();
    }

    private static int numberOfTaxa(List<TagTaxaDistribution> tagTaxaDistributions) {
        return tagTaxaDistributions.stream().mapToInt(t -> t.taxaDist().numberOfTaxaWithTag()).sum();
    }

    private static int numberTaxaAtSite(Map<Byte,List<TagTaxaDistribution>> tagTaxaDistributions) {
        return tagTaxaDistributions.values().stream()
                .flatMap(tList -> tList.stream())
                .mapToInt(t -> t.taxaDist().numberOfTaxaWithTag())
                .sum();
    }

    private static int numberTaxaAtSiteIgnoreGaps(Map<Byte,List<TagTaxaDistribution>> tagTaxaDistributions) {
        return tagTaxaDistributions.entrySet().stream()
                .filter(e -> e.getKey()!=GAP_ALLELE)
                .flatMap(tList -> tList.getValue().stream())
                .mapToInt(tag -> tag.taxaDist().numberOfTaxaWithTag())
                .sum();
    }

    private static List<Optional<Position>> referencePositions(Position refStartPosition, Map<Tag,String> alignedTags){
        Tag refTag=alignedTags.keySet().stream()
                .filter(Tag::isReference)
                .findFirst().orElseThrow(() -> new IllegalStateException("Reference not found"));
        //System.out.println("LCJ - rfp: ref tag: " + alignedTags.get(refTag));
        AtomicInteger start=new AtomicInteger(refStartPosition.getPosition());
        return alignedTags.get(refTag).chars()
                .mapToObj(c -> (c == GAP_ALLELE_CHAR) ? Optional.<Position>empty() :
                        Optional.<Position>of(new GeneralPosition.Builder(refStartPosition.getChromosome(), start.getAndIncrement()).build()))
                .collect(Collectors.toList());
    }

    // The following getters and setters were auto-generated.
    // Please use this method to re-generate.
    //
    // public static void main(String[] args) {
    //     GeneratePluginCode.generate(DiscoverySNPCallerPluginV2.class);
    // }

    /**
     * Convenience method to run plugin with one return object.
     */
//    // TODO: Replace <Type> with specific type.
//    public <TagDataWriter> runPlugin(DataSet input) {
//        return (TagDataWriter) performFunction(input).getData(0).getData();
//    }

    /**
     * Input TagsByTaxa file (if hdf5 format, use .hdf or
     * .h5 extension)
     *
     * @return Input Tags by Taxa File
     */
    public String inputDB() {
        return myInputDB.value();
    }

    /**
     * Set Input Tags by Taxa File. Input TagsByTaxa file
     * (if hdf5 format, use .hdf or .h5 extension)
     *
     * @param value Input Tags by Taxa File
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 inputDB(String value) {
        myInputDB = new PluginParameter<>(myInputDB, value);
        return this;
    }

    /**
     * Minimum minor allele frequency
     *
     * @return Min Minor Allele Freq
     */
    public Double minMinorAlleleFreq() {
        return myMinMinorAlleleFreq.value();
    }

    /**
     * Set Min Minor Allele Freq. Minimum minor allele frequency
     *
     * @param value Min Minor Allele Freq
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 minMinorAlleleFreq(Double value) {
        myMinMinorAlleleFreq = new PluginParameter<>(myMinMinorAlleleFreq, value);
        return this;
    }

    /**
     * Minimum locus coverage (proportion of Taxa with a genotype)
     *
     * @return Min Locus Coverage
     */
    public Double minLocusCoverage() {
        return myMinLocusCoverage.value();
    }

    /**
     * Set Min Locus Coverage. Minimum locus coverage (proportion
     * of Taxa with a genotype)
     *
     * @param value Min Locus Coverage
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 minLocusCoverage(Double value) {
        myMinLocusCoverage = new PluginParameter<>(myMinLocusCoverage, value);
        return this;
    }

    /**
     * Path to reference genome in fasta format. Ensures that
     * a tag from the reference genome is always included
     * when the tags at a locus are aligned against each other
     * to call SNPs. The reference allele for each site is
     * then provided in the output HapMap files, under the
     * taxon name "REFERENCE_GENOME" (first taxon). DEFAULT:
     * Don't use reference genome.
     *
     * @return Reference Genome File
     */
    public String referenceGenomeFile() {
        return myRefGenome.value();
    }

    /**
     * Set Reference Genome File. Path to reference genome
     * in fasta format. Ensures that a tag from the reference
     * genome is always included when the tags at a locus
     * are aligned against each other to call SNPs. The reference
     * allele for each site is then provided in the output
     * HapMap files, under the taxon name "REFERENCE_GENOME"
     * (first taxon). DEFAULT: Don't use reference genome.
     *
     * @param value Reference Genome File
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 referenceGenomeFile(String value) {
        myRefGenome = new PluginParameter<>(myRefGenome, value);
        return this;
    }

    /**
     * Start Chromosome
     *
     * @return Start Chromosome
     */
    public Chromosome startChromosome() {
        return myStartChr.value();
    }

    /**
     * Set Start Chromosome. Start Chromosome
     *
     * @param value Start Chromosome
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 startChromosome(Chromosome value) {
        myStartChr = new PluginParameter<>(myStartChr, value);
        return this;
    }

    /**
     * End Chromosome
     *
     * @return End Chromosome
     */
    public Chromosome endChromosome() {
        return myEndChr.value();
    }

    /**
     * Set End Chromosome. End Chromosome
     *
     * @param value End Chromosome
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 endChromosome(Chromosome value) {
        myEndChr = new PluginParameter<>(myEndChr, value);
        return this;
    }

    /**
     * Include the rare alleles at site (3 or 4th states)
     *
     * @return Include Rare Alleles
     */
    public Boolean includeRareAlleles() {
        return myIncludeRareAlleles.value();
    }

    /**
     * Set Include Rare Alleles. Include the rare alleles
     * at site (3 or 4th states)
     *
     * @param value Include Rare Alleles
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 includeRareAlleles(Boolean value) {
        myIncludeRareAlleles = new PluginParameter<>(myIncludeRareAlleles, value);
        return this;
    }

    /**
     * Include sites where major or minor allele is a GAP
     *
     * @return Include Gaps
     */
    public Boolean includeGaps() {
        return myIncludeGaps.value();
    }

    /**
     * Set Include Gaps. Include sites where major or minor
     * allele is a GAP
     *
     * @param value Include Gaps
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 includeGaps(Boolean value) {
        myIncludeGaps = new PluginParameter<>(myIncludeGaps, value);
        return this;
    }

    /**
     * Include sites where the third allele is a GAP (mutually
     * exclusive with inclGaps)
     *
     * @return Call Biallelic SNPs with Gap
     */
    public Boolean callBiallelicSNPsWithGap() {
        return myCallBiSNPsWGap.value();
    }

    /**
     * Set Call Biallelic SNPs with Gap. Include sites where
     * the third allele is a GAP (mutually exclusive with
     * inclGaps)
     *
     * @param value Call Biallelic SNPs with Gap
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 callBiallelicSNPsWithGap(Boolean value) {
        myCallBiSNPsWGap = new PluginParameter<>(myCallBiSNPsWGap, value);
        return this;
    }

    /**
     * Maximum gap alignment allowed from the equation:
     *   IndelContrast / (IndelContrast/Non-IndelContrast)
     *   
     *   IC=Indel contrasts=Sum the number ACGT vs - 
     *   NC=non-indel constrasts = Sum the number of ACGT vs ACGT
     *   ignore = - vs -
     * Gapped Alignment ratio = IC/(IC+NC)
     *
     * @return Maxmimum Gap alignment ratio
     */
    public Double gapAlignmentThreshold() {
        return myGapAlignmentThreshold.value();
    }

    /**
     * Maximum gap alignment allowed from the equation:
     *   IndelContrast / (IndelContrast/Non-IndelContrast)
     *   
     *   IC=Indel contrasts=Sum the number ACGT vs - 
     *   NC=non-indel constrasts = Sum the number of ACGT vs ACGT
     *   ignore = - vs -
     * Gapped Alignment ratio = IC/(IC+NC)
     *
     * @param value Max gap alignment ratio
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 gapAlignmentThreshold(Double value) {
        myGapAlignmentThreshold = new PluginParameter<>(myGapAlignmentThreshold, value);
        return this;
    }
    
    /**
     * Maximum number of tags per cut site (for alignment)
     *
     * @return MaxTagsPerCutSite
     */
    public Integer maxTagsPerCutSite() {
        return maxTagsPerCutSite.value();
    }

    /**
     * Set maxTagsPerCutSite. This is the maximum number of tags
     * allowed per cute site when performaing an alignment.  Too
     * many tags and biojava 4 getMultipleSequenceAlignment grinds to a halt.
     *
     * @param value Max number of tags per cut site
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 maxTagsPerCutSite(Integer value) {
    	maxTagsPerCutSite = new PluginParameter<>(maxTagsPerCutSite, value);
        return this;
    }

    private static Map<String,Integer> keyFileStringToInt = null;

    // For junit testing.  Used for ReferenceGenomeSequence:readReferenceGenomeChr() tests
    // that want to read from a key file.
    public static void setKeyFileStringToInt (Map<String,Integer> inputKeyFile) {
        keyFileStringToInt = inputKeyFile;
    }

    /**
     * Delete exisiting Discovery data from DB
     *
     * @return deleteOldData
     */
    public Boolean deleteOldData() {
        return myDeleteOldData.value();
    }

    /**
     * Set Delete old data flag.  True indicates we want the
     * db tables cleared
     *
     * @param value true/false - whether to delete data
     *
     * @return this plugin
     */
    public DiscoverySNPCallerPluginV2 deleteOldData(Boolean value) {
        myDeleteOldData = new PluginParameter<>(myDeleteOldData, value);
        return this;
    }
    /**
     * GIven a chromosome string value, search for it's corresponding
     * number from the keyFileStringToInt map
     *
     * @return integer version of Chromosome
     */
    public static int keyFileReturnChromInt(String chromosome) {
        if (keyFileStringToInt != null) {
                if (keyFileStringToInt.containsKey(chromosome))
                        return(keyFileStringToInt.get(chromosome));
        } else {
                System.out.println("LCJ - DiscoverySNP .. keyFileStringToInt is NULL");
        }
        return -1; // failure case
    }


}

class TagTaxaDistribution {
    private final Tag myTag;
    private final TaxaDistribution myTaxaDist;

    public TagTaxaDistribution(Tag myTag, TaxaDistribution myTaxaDist) {
        this.myTag = myTag;
        this.myTaxaDist = myTaxaDist;
    }

    public Tag tag() {
        return myTag;
    }

    public TaxaDistribution taxaDist() {
        return myTaxaDist;
    }
}

