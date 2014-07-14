package net.maizegenetics.dna.map;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Jason Wallace on 7/2/14.
 * This class stores a generic "feature" on a genome, such as a gene, transcript, exon, miRNA, etc. The intent is for
 * this information to be read in from an external file (such as an Ensembl GFF/GTF file) and collated into a
 * GenomeFeatureMap, but it could be used in other ways.
 */
public class GenomeFeature {

    private int start, stop;    //Location on the chromosome (start and stop should be inclusive)
    private HashMap<String, String> annotations;    //Hashmap of all annotations, stored as strings.

    GenomeFeature(HashMap<String, String> myannotations){
       this.annotations=myannotations;

       //Assign position values based on annotations. Lookup is 100-1000x faster this way than having to convert from String each time
       this.start = Integer.parseInt(annotations.get("start"));
       this.stop = Integer.parseInt(annotations.get("stop"));
    }

    //Various convenience methods to get the most common annotations
    public String id(){
        return getAnnotation("id");
    }

    public String type(){
        return getAnnotation("type");
    }

    public String parentId(){
        return getAnnotation("parent_id");
    }

    public String chromosome(){
        return getAnnotation("chromosome");
    }

    public int start(){
        return this.start;
    }

    public String startAsString(){
        return getAnnotation("start");
    }

    public int stop(){
        return this.stop;
    }

    public String stopAsString(){
        return getAnnotation("stop");
    }

    /**
     * Get any annotation based on its key. If this feature lacks that annotation, it returns 'NA'
     * @param key The name of the annotation to look for
     * @return The value of that annotation, or 'NA' if not found
     */
    public String getAnnotation(String key){
        if(annotations.containsKey(key)){
            return annotations.get(key);
        }else{
            return "NA";
        }
    }

    /**
     * Returns a (shallow) copy of the Hashmap that keeps all annotations for this feature. Since the hashmap just
     * stores Strings, a shallow copy is still safe to modify b/c it won't be reflected in the original.
     * @return A copy of the Hashmap storing this feature's annotations.
     */
    public HashMap<String, String> annotations(){
        HashMap<String, String> annotationsCopy = new HashMap<>(annotations);
        return annotationsCopy;
    }

}
