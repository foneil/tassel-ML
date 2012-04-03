/*
 * MutableNucleotideAlignment
 */
package net.maizegenetics.pal.alignment;

import cern.colt.GenericSorting;
import cern.colt.Swapper;
import cern.colt.function.IntComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.maizegenetics.pal.ids.IdGroup;
import net.maizegenetics.pal.ids.Identifier;
import net.maizegenetics.pal.ids.SimpleIdGroup;

/**
 *
 * @author terry
 */
public class MutableNucleotideAlignment extends AbstractAlignment implements MutableAlignment {

    private boolean myIsDirty = true;
    private byte[][] myData;
    private List<Identifier> myIdentifiers = new ArrayList<Identifier>();
    private final int myMaxTaxa;
    private final int myMaxNumSites;
    private int myNumSites = 0;
    private int[] myVariableSites;
    private List<Locus> myLocusToLociIndex = new ArrayList<Locus>();
    private int[] myLocusIndices;
    private String[] mySNPIDs;
    private int maxNumAlleles=2;

    private MutableNucleotideAlignment(Alignment a, int maxNumTaxa, int maxNumSites) {
        super(a.getAlleleEncodings());
        maxNumAlleles=a.getMaxNumAlleles();

        if (a.getAlleleEncodings().length != 1) {
            throw new IllegalArgumentException("MutableNucleotideAlignment: init: must only have one allele encoding.");
        }

        if (a.getSiteCount() > maxNumSites) {
            throw new IllegalArgumentException("MutableNucleotideAlignment: init: initial number of sites can't be more than max number of sites.");
        }

        if (a.getSequenceCount() > maxNumTaxa) {
            throw new IllegalArgumentException("MutableNucleotideAlignment: init: initial number of taxa can't be more than max number of taxa.");
        }

        myMaxTaxa = maxNumTaxa;
        myMaxNumSites = maxNumSites;
        myNumSites = a.getSiteCount();
        initData();
        initTaxa(a.getIdGroup());
        loadAlleles(a);
        loadLoci(a);
        System.arraycopy(a.getSNPIDs(), 0, mySNPIDs, 0, a.getSiteCount());
        System.arraycopy(a.getPhysicalPositions(), 0, myVariableSites, 0, a.getSiteCount());
    }

    public static MutableNucleotideAlignment getInstance(Alignment a, int maxTaxa, int maxNumSites) {
        if ((a instanceof TBitNucleotideAlignment) || (a instanceof SBitNucleotideAlignment)) {
            return new MutableNucleotideAlignment(a, maxTaxa, maxNumSites);
        } else {
            throw new IllegalArgumentException("MutableNucleotideAlignment: getInstance: alignment must be nucleotide data.");
        }
    }

    public static MutableNucleotideAlignment getInstance(Alignment a) {
        return getInstance(a, a.getSequenceCount(), a.getSiteCount());
    }

    private MutableNucleotideAlignment(IdGroup idGroup, int initNumSites, int maxNumTaxa, int maxNumSites) {
        super(NucleotideAlignmentConstants.NUCLEOTIDE_ALLELES);

        if (initNumSites > maxNumSites) {
            throw new IllegalArgumentException("MutableNucleotideAlignment: init: initial number of sites can't be more than max number of sites.");
        }

        if (idGroup.getIdCount() > maxNumTaxa) {
            throw new IllegalArgumentException("MutableNucleotideAlignment: init: initial number of taxa can't be more than max number of taxa.");
        }

        myMaxTaxa = maxNumTaxa;
        myMaxNumSites = maxNumSites;
        myNumSites = initNumSites;

        initData();
        initTaxa(idGroup);
    }

    public static MutableNucleotideAlignment getInstance(IdGroup idGroup, int initNumSites, int maxNumTaxa, int maxNumSites) {
        return new MutableNucleotideAlignment(idGroup, initNumSites, maxNumTaxa, maxNumSites);
    }

    public static MutableNucleotideAlignment getInstance(IdGroup idGroup, int initNumSites) {
        return new MutableNucleotideAlignment(idGroup, initNumSites, idGroup.getIdCount(), initNumSites);
    }

    private void initData() {
        myData = new byte[myMaxTaxa][myMaxNumSites];
        for (int t = 0; t < myMaxTaxa; t++) {
            Arrays.fill(myData[t], Alignment.UNKNOWN_DIPLOID_ALLELE);
        }
        myLocusIndices = new int[myMaxNumSites];
        Arrays.fill(myLocusIndices, -1);
        myVariableSites = new int[myMaxNumSites];
        Arrays.fill(myVariableSites, -1);
        mySNPIDs = new String[myMaxNumSites];
        Arrays.fill(mySNPIDs, null);
    }

    private void initTaxa(IdGroup idGroup) {
        for (int i = 0, n = idGroup.getIdCount(); i < n; i++) {
            myIdentifiers.add(idGroup.getIdentifier(i));
        }
    }

    private void loadAlleles(Alignment a) {

        int numSites = a.getSiteCount();
        int numSeqs = a.getSequenceCount();

        for (int s = 0; s < numSites; s++) {
            for (int t = 0; t < numSeqs; t++) {
                myData[t][s] = a.getBase(t, s);
            }
        }

    }

    private void loadLoci(Alignment a) {

        Locus[] loci = a.getLoci();
        for (int i = 0; i < loci.length; i++) {
            myLocusToLociIndex.add(loci[i]);
        }

        int[] offsets = a.getLociOffsets();
        for (int i = 0; i < offsets.length - 1; i++) {
            for (int j = offsets[i]; j < offsets[i + 1]; j++) {
                myLocusIndices[j] = i;
            }
        }
        for (int j = offsets[offsets.length - 1], n = a.getSiteCount(); j < n; j++) {
            myLocusIndices[j] = offsets.length - 1;
        }
    }

    public byte getBase(int taxon, int site) {
        return myData[taxon][site];
    }

    public boolean isSBitFriendly() {
        return false;
    }

    public boolean isTBitFriendly() {
        return false;
    }

    @Override
    public int getSiteCount() {
        return myNumSites;
    }

    @Override
    public int getSequenceCount() {
        return myIdentifiers.size();
    }

    @Override
    public IdGroup getIdGroup() {
        Identifier[] ids = new Identifier[myIdentifiers.size()];
        myIdentifiers.toArray(ids);
        return new SimpleIdGroup(ids);
    }

    @Override
    public String getTaxaName(int index) {
        return myIdentifiers.get(index).getName();
    }

    @Override
    public String getFullTaxaName(int index) {
        return myIdentifiers.get(index).getFullName();
    }

    @Override
    public boolean hasReference() {
        return false;
    }

    @Override
    public byte getReferenceAllele(int site) {
        return 15;
    }

    @Override
    public byte[] getReference() {
        return null;
    }

    @Override
    public byte[] getReference(int startSite, int endSite) {
        return null;
    }

    @Override
    public boolean isPhased() {
        return false;
    }

    @Override
    public boolean isPositiveStrand(int site) {
        return true;
    }

    @Override
    public String getGenomeAssembly() {
        return "AGPV2";
    }

    @Override
    public int[] getPhysicalPositions() {
        return myVariableSites.clone();
    }

    @Override
    public int getPositionInLocus(int site) {
        try {
            return myVariableSites[site];
        } catch (Exception e) {
            return site;
        }
    }

    @Override
    public Locus getLocus(int site) {
        return myLocusToLociIndex.get(myLocusIndices[site]);
    }

    @Override
    public Locus[] getLoci() {
        Locus[] result = new Locus[myLocusToLociIndex.size()];
        for (int i = 0; i < myLocusToLociIndex.size(); i++) {
            result[i] = myLocusToLociIndex.get(i);
        }
        return result;
    }

    @Override
    public int getNumLoci() {
        return myLocusToLociIndex.size();
    }

    @Override
    public int[] getLociOffsets() {

        if (isDirty()) {
            throw new IllegalStateException("MutableNucleotideAlignment: getLociOffsets: this alignment is dirty.");
        }

        List<Integer> result = new ArrayList<Integer>();
        int current = myLocusIndices[0];
        result.add(0);
        for (int i = 0, n = getSiteCount(); i < n; i++) {
            if (myLocusIndices[i] != current) {
                result.add(i);
                current = i;
            }
        }
        int[] offsets = new int[result.size()];
        for (int i = 0, n = result.size(); i < n; i++) {
            offsets[i] = result.get(i);
        }
        return offsets;

    }

    private int[] getStartAndEndOfLocus(Locus locus) {

        if (isDirty()) {
            throw new IllegalStateException("MutableNucleotideAlignment: getStartAndEndOfLocus: this alignment is dirty.");
        }

        Locus[] loci = getLoci();
        int[] lociOffsets = getLociOffsets();
        for (int i = 0; i < getNumLoci(); i++) {
            if (locus == loci[i]) {
                int end = 0;
                if (i == getNumLoci() - 1) {
                    end = getSiteCount();
                } else {
                    end = lociOffsets[i + 1];
                }
                return new int[]{lociOffsets[i], end};
            }
        }
        throw new IllegalArgumentException("AbstractAlignment: getStartAndEndOfLocus: this locus not defined: " + locus.getName());
    }

    @Override
    public String[] getSNPIDs() {
        return mySNPIDs;
    }

    @Override
    public String getSNPID(int site) {
        return mySNPIDs[site];
    }

    @Override
    public int getSiteOfPhysicalPosition(int physicalPosition, Locus locus) {

        if (isDirty()) {
            throw new IllegalStateException("MutableNucleotideAlignment: getStartAndEndOfLocus: this alignment is dirty.");
        }

        try {
            if (locus == null) {
                locus = myLocusToLociIndex.get(0);
            }
            int[] startEnd = getStartAndEndOfLocus(locus);
            return Arrays.binarySearch(myVariableSites, startEnd[0], startEnd[1], physicalPosition);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public boolean retainsRareAlleles() {
        return false;
    }

    @Override
    public int getMaxNumAlleles() {
        return maxNumAlleles;
    }

    @Override
    public String getBaseAsString(int taxon, int site) {
        return NucleotideAlignmentConstants.getNucleotideIUPAC(getBase(taxon, site));
    }

    @Override
    public String getDiploidAsString(int site, byte value) {
        return NucleotideAlignmentConstants.getNucleotideIUPAC(value);
    }

    // Mutable Methods...
    public void setBase(int taxon, int site, byte newBase) {
        myData[taxon][site] = newBase;
    }

    public void setBaseRange(int taxon, int startSite, byte[] newBases) {
        for (int i = 0; i < newBases.length; i++) {
            myData[taxon][startSite++] = newBases[i];
        }
    }

    public void addSite(int site) {

        if (myMaxNumSites < myNumSites + 1) {
            throw new IllegalStateException("MutableNucleotideAlignment: addSite: this exceeds max num of sites: " + myMaxNumSites);
        }

        for (int t = 0, n = getSequenceCount(); t < n; t++) {
            for (int s = myNumSites; s > site; s--) {
                myData[t][s] = myData[t][s - 1];
            }
            myData[t][site] = Alignment.UNKNOWN_DIPLOID_ALLELE;
        }

        for (int s = myNumSites; s > site; s--) {
            myVariableSites[s] = myVariableSites[s - 1];
            myLocusIndices[s] = myLocusIndices[s - 1];
            mySNPIDs[s] = mySNPIDs[s - 1];
        }
        myVariableSites[site] = -1;
        myLocusIndices[site] = -1;
        mySNPIDs[site] = null;

        myNumSites++;

        myIsDirty = true;

    }

    public void removeSite(int site) {

        myNumSites--;

        for (int t = 0, n = getSequenceCount(); t < n; t++) {
            for (int s = site; s < myNumSites; s--) {
                myData[t][s] = myData[t][s + 1];
            }
            myData[t][myNumSites] = Alignment.UNKNOWN_DIPLOID_ALLELE;
        }

        for (int s = site; s < myNumSites; s--) {
            myVariableSites[s] = myVariableSites[s + 1];
            myLocusIndices[s] = myLocusIndices[s + 1];
            mySNPIDs[s] = mySNPIDs[s + 1];
        }
        myVariableSites[myNumSites] = -1;
        myLocusIndices[myNumSites] = -1;
        mySNPIDs[myNumSites] = null;

    }

    public void addTaxon(Identifier id) {
        if (getSequenceCount() + 1 > myMaxTaxa) {
            throw new IllegalStateException("MutableNucleotideAlignment: addTaxon: this exceeds max num of taxa: " + myMaxTaxa);
        }
        myIdentifiers.add(id);
    }

    public void removeTaxon(int taxon) {

        myIdentifiers.remove(taxon);

        int numTaxa = getSequenceCount();
        for (int s = 0; s < myNumSites; s++) {
            for (int t = taxon; t < numTaxa; t--) {
                myData[t][s] = myData[t + 1][s];
            }
            myData[numTaxa][s] = Alignment.UNKNOWN_DIPLOID_ALLELE;
        }

    }

    public void clean() {
        sortSitesByPhysicalPosition();
        myIsDirty = false;
    }

    public boolean isDirty() {
        return myIsDirty;
    }

    private void sortSitesByPhysicalPosition() {

        Swapper swapperPos = new Swapper() {

            public void swap(int a, int b) {
                int it;
                it = myLocusIndices[a];
                myLocusIndices[a] = myLocusIndices[b];
                myLocusIndices[b] = it;

                byte bt;
                for (int t = 0, n = getSequenceCount(); t < n; t++) {
                    bt = getBase(t, a);
                    setBase(t, a, getBase(t, b));
                    setBase(t, b, bt);
                }

                it = myVariableSites[a];
                myVariableSites[a] = myVariableSites[b];
                myVariableSites[b] = it;

                String st = mySNPIDs[a];
                mySNPIDs[a] = mySNPIDs[b];
                mySNPIDs[b] = st;
            }
        };
        IntComparator compPos = new IntComparator() {

            public int compare(int a, int b) {
                if (myLocusIndices[a] < myLocusIndices[b]) {
                    return -1;
                }
                if (myLocusIndices[a] > myLocusIndices[b]) {
                    return 1;
                }
                if (myVariableSites[a] < myVariableSites[b]) {
                    return -1;
                }
                if (myVariableSites[a] > myVariableSites[b]) {
                    return 1;
                }
                return 0;
            }
        };

        GenericSorting.quickSort(0, this.getSiteCount(), compPos, swapperPos);

    }

    public void setPositionOfSite(int site, int position) {
        if ((site < 0) || (site >= myNumSites)) {
            throw new IllegalArgumentException("MutableNucleotideAlignment: setPositionOfSite: site outside of range: " + site);
        }
        myVariableSites[site] = position;

        myIsDirty = true;
    }

    public void setLocusOfSite(int site, Locus locus) {
        if ((site < 0) || (site >= myNumSites)) {
            throw new IllegalArgumentException("MutableNucleotideAlignment: setLocusOfSite: site outside of range: " + site);
        }
        int index = getLocusIndex(locus);
        if (index < 0) {
            myLocusToLociIndex.add(locus);
            myLocusIndices[site] = myLocusToLociIndex.size() - 1;
        } else {
            myLocusIndices[site] = index;
        }

        myIsDirty = true;
    }

    private int getLocusIndex(Locus locus) {
        for (int i = 0; i < myLocusToLociIndex.size(); i++) {
            if (myLocusToLociIndex.get(i) == locus) {
                return i;
            }
        }
        return -1;
    }
}
