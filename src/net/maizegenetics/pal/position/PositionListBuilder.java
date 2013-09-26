package net.maizegenetics.pal.position;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import com.google.common.base.Preconditions;
import net.maizegenetics.pal.alignment.HapMapHDF5Constants;

import java.util.*;

/**
 * A builder for creating immutable PositionList.  Can be used for either an in memory or HDF5 list.
 *
 * <p>Example:
 * <pre>   {@code
 *   PositionListBuilder b=new PositionArrayList.Builder();
 *   for (int i = 0; i <size; i++) {
 *       Position ap=new CoreAnnotatedPosition.Builder(chr[chrIndex[i]],pos[i]).refAllele(refSeq[i]).build();
 *       b.add(ap);
 *       }
 *   PositionList instance=b.build();}
 * <p></p>
 * If being built separately from the genotypes, then use validate ordering to make sure sites are added in the
 * indended order.  This list WILL be sorted.
 * <p>Builder instances can be reused - it is safe to call {@link #build}
 * multiple times to build multiple lists in series. Each new list
 * contains the one created before it.
 *
 * HDF5 Example
 * <p>Example:
 * <pre>   {@code
 *   PositionList instance=new PositionHDF5List.Builder("fileName").build();
 *   }
 *
 * <p>Builder instances can be reused - it is safe to call {@link #build}
 */
public class PositionListBuilder {

    private ArrayList<Position> contents = new ArrayList<Position>();
    private boolean isHDF5=false;
    private IHDF5Reader reader;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link }.
     */
    public PositionListBuilder() {}

    /**
     * Adds {@code element} to the {@code PositionList}.
     *
     * @param element the element to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     */
    public PositionListBuilder add(Position element) {
        if(isHDF5) throw new UnsupportedOperationException("Positions cannot be added to existing HDF5 alignments");
        Preconditions.checkNotNull(element, "element cannot be null");
        contents.add(element);
        return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code PositionList}.
     *
     * @param elements the {@code Iterable} to add to the {@code PositionList}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is or contains null
     */
    public PositionListBuilder addAll(Iterable<? extends Position> elements) {
        if(isHDF5) throw new UnsupportedOperationException("Positions cannot be added to existing HDF5 alignments");
        if (elements instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<? extends Position> collection = (Collection<? extends Position>) elements;
            contents.ensureCapacity(contents.size() + collection.size());
        }
        for (Position elem : elements) {
            Preconditions.checkNotNull(elem, "elements contains a null");
            contents.add(elem);
        }
        return this;
    }

    /**
     * Replaces the element at the specified position in this list with
     * the specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return this {@code Builder} object
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public PositionListBuilder set(int index, Position element) {
        if(isHDF5) throw new UnsupportedOperationException("Positions cannot be edited to existing HDF5 alignments");
        contents.set(index,element);
        return this;
    }

    /*
    Returns whether List is already ordered.  Important to check this if genotype and sites are separately built, as the
     PositionArrayList must be sorted, and will be with build.
     */
    public boolean validateOrdering() {
        boolean result=true;
        Position startAP=contents.get(0);
        for (Position ap:contents) {
            if(ap.compareTo(startAP)<0) return false;
            startAP=ap;
        }
        return result;
    }

    /**
     * Returns the size (number of positions) in the current list
     * @return current size
     */
    public int size() {
        return contents.size();
    }


    /**
     * Creates a new builder based on an existing HDF5 file.
     */
    public PositionListBuilder(String hdf5FileName) {
        isHDF5=true;
        this.reader = HDF5Factory.openForReading(hdf5FileName);
    }

    /**
     * Creates a new builder based on an existing HDF5 file reader.
     */
    public PositionListBuilder(IHDF5Reader reader) {
        isHDF5=true;
        this.reader = reader;
    }

    /**
     * Creates a positionList in a new HDF5 file.
     */
    public PositionListBuilder(IHDF5Writer h5w, PositionList a) {
        //this.hdf5FileName=h5w.getFile().getName();
        //      IHDF5Writer h5w= HDF5Factory.open(hdf5FileName);
        h5w.writeStringArray(HapMapHDF5Constants.SNP_IDS, a.getSNPIDs());  //TODO consider adding compression & blocks

        h5w.setIntAttribute(HapMapHDF5Constants.DEFAULT_ATTRIBUTES_PATH, HapMapHDF5Constants.NUM_SITES, a.size());

        String[] lociNames = new String[a.getNumChromosomes()];
        Map<Chromosome, Integer> locusToIndex=new HashMap<>(10);
        Chromosome[] loci = a.getChromosomes();
        for (int i = 0; i < a.getNumChromosomes(); i++) {
            lociNames[i] = loci[i].getName();
            locusToIndex.put(loci[i],i);
        }
        h5w.createStringVariableLengthArray(HapMapHDF5Constants.LOCI, a.getNumChromosomes());
        h5w.writeStringVariableLengthArray(HapMapHDF5Constants.LOCI, lociNames);
        int[] locusIndicesArray = new int[a.getSiteCount()];
        for (int i = 0; i < locusIndicesArray.length; i++) {
            locusIndicesArray[i] = locusToIndex.get(a.getChromosome(i));
        }
        HDF5IntStorageFeatures features = HDF5IntStorageFeatures.createDeflation(2);
        h5w.createIntArray(HapMapHDF5Constants.LOCUS_INDICES, a.getSiteCount(),features);
        h5w.writeIntArray(HapMapHDF5Constants.LOCUS_INDICES, locusIndicesArray,features);

        h5w.createIntArray(HapMapHDF5Constants.POSITIONS, a.size());
        h5w.writeIntArray(HapMapHDF5Constants.POSITIONS, a.getPhysicalPositions());
        this.reader = h5w;
        isHDF5=true;
    }

    /**
     * Returns a newly-created {@code ImmutableList} based on the contents of
     * the {@code Builder}.
     */
    public PositionList build() {
        if(isHDF5) return new PositionHDF5List(reader);
        if(!validateOrdering()) {
            System.out.println("Beginning Sort of Position List");
            Collections.sort(contents);
            System.out.println("Finished Sort of Position List");
        }
        return new PositionArrayList(contents);
    }

}
