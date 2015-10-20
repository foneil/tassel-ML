/**
 *
 */
package net.maizegenetics.dna.map;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.SetMultimap;
import net.maizegenetics.util.GeneralAnnotation;
import net.maizegenetics.util.Utils;

/**
 * Utilities for reading and writing Position Lists
 *
 * @author lcj34
 * @author zrm22
 *
 */
public class PositionListIOUtils {

    private PositionListIOUtils() {
    }

    /**
     * Returns a PositionList from a tab-delimited text SNP Conserve file. The
     * input file has 2 tab-delimited fields indicating Chromosome Number and
     * Position A header row is the first line and looks like this: #CHROM	POS
     * The remaining rows contains integer values as below: 9	18234
     *
     * @param fileName with complete path
     * @return PositionList
     */
    public static PositionList readSNPConserveFile(String fileName) {
        try {
            BufferedReader fileIn = Utils.getBufferedReader(fileName, 1000000);
            PositionListBuilder plb = new PositionListBuilder();
            //parse SNP position rows
            // First value is Chromosome number, second is position
            String line = fileIn.readLine(); // read/skip header
            while ((line = fileIn.readLine()) != null) {
                String[] tokens = line.split("\\t");
                if (tokens.length != 2) {
                    System.err.println("Error in SNP Conserve File format:" + fileName);
                    System.err.println("Expecting tab-delimited file with 2 integer values per row");
                }
                Chromosome chrom = new Chromosome(tokens[0]);
                int pos = Integer.parseInt(tokens[1]);
                Position position = new GeneralPosition.Builder(chrom, pos).build();
                plb.add(position);
            }
            return plb.build();
        } catch (Exception e) {
            System.err.println("Error in Reading SNP Conserve File:" + fileName);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Returns a PositionList from a tab-delimited text SNP Quality Score file. The
     * input file has 3 tab-delimited fields indicating Chromosome Number and Quality Score
     * Position A header row is the first line and looks like this: CHROM	POS	QUALITYSCORE
     * The remaining rows contains integer values as below: 9	18234	15.5
     * NOTE: the CHROM field is a string.
     *
     * @param fileName with complete path
     * @return PositionList
     */
    public static PositionList readQualityScoreFile(String fileName) {
    	if (fileName == null) return null;
        try {
            BufferedReader fileIn = Utils.getBufferedReader(fileName, 1000000);
            PositionListBuilder plb = new PositionListBuilder();
            //parse SNP position rows
            // First value is Chromosome string, second is position, third is qualityScore
            String line = fileIn.readLine(); // read/skip header
            while ((line = fileIn.readLine()) != null) {
                String[] tokens = line.split("\\t");
                if (tokens.length != 3) {
                    System.err.println("Error in SNP Position QualityScore file format:" + fileName);
                    System.err.println("Expecting tab-delimited file with 2 integer and 1 float value per row "
                    		+ " with header values CHROM POS QUALITYSCORE");
                }
                Chromosome chrom = new Chromosome(tokens[0]);
                int pos = Integer.parseInt(tokens[1]);
                double qscore = Double.parseDouble(tokens[2]);
                Position position = new GeneralPosition.Builder(chrom, pos)
                		.addAnno("QualityScore",qscore).build();
                plb.add(position);
            }
            return plb.build();
        } catch (Exception e) {
            System.err.println("Error in Reading Quality Score File:" + fileName);
            e.printStackTrace();
        }
        return null;   	
    }
    //Method to convert a positionList with annotations to a table form and export it to the file
    public static void exportTabDelimPosList(PositionList posList, String fileName) {
        HashMap<String,Integer> globalKeyIndexMap = new HashMap<String,Integer>();
        //Get each Annotation and keep track of each key
        for(Position pos : posList) {
            GeneralAnnotation annos = pos.getAnnotation();
            Set<String> annoKeys = annos.getAnnotationKeys();
            for(String key : annoKeys) {
                if(!globalKeyIndexMap.keySet().contains(key)) {
                    globalKeyIndexMap.put(key, globalKeyIndexMap.size());
                }
            }
        }

        //Loop through the positions once again to convert the positions to tab format and write to file
        try {
            BufferedWriter fileOut = new BufferedWriter(new FileWriter(fileName));
            //Write out the headers in the form
            //chr   pos     strand  anno1   anno2   anno3
            fileOut.write("chr\tpos\tstrand\t");
            String[] annotationBuffer = new String[globalKeyIndexMap.size()];
            Set<String> annoNames = globalKeyIndexMap.keySet();
            for(String name : annoNames) {
                int keyIndex = globalKeyIndexMap.get(name);
                annotationBuffer[keyIndex] = name;
            }
            fileOut.write(Arrays.stream(annotationBuffer).collect(Collectors.joining("\t")));
            fileOut.newLine();

            for (Position pos : posList) {
                //Use a buffer to hold sparse annotations
                annotationBuffer = new String[globalKeyIndexMap.size()];
                GeneralAnnotation annos = pos.getAnnotation();

                //Add in chr, pos and strand values
                int chromIndex = pos.getChromosome().getChromosomeNumber();
                int posIndex = pos.getPosition();
                String strandString = pos.getStrandStr();
                fileOut.write(chromIndex+"\t"+posIndex+"\t"+strandString+"\t");

                Set<String> annoKeys = annos.getAnnotationKeys();
                SetMultimap<String, String> annoMap = annos.getAnnotationAsMap();
                for(String key: annoKeys) {
                    //lookup the key index
                    int keyIndex = globalKeyIndexMap.get(key);
                    annotationBuffer[keyIndex] = annoMap.get(key).stream().collect(Collectors.joining(":"));
                }
                fileOut.write(Arrays.stream(annotationBuffer).collect(Collectors.joining("\t")));
                fileOut.newLine();
            }

            fileOut.close();
        }
        catch(IOException e) {
            System.out.println(e);
        }
    }

}
