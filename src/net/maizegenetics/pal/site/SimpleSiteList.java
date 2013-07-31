package net.maizegenetics.pal.site;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import net.maizegenetics.pal.alignment.Locus;

/**
 * Created with IntelliJ IDEA.
 * User: edbuckler
 * Date: 7/30/13
 * Time: 5:22 PM
 */
public class SimpleSiteList implements SiteList {

    private final List<AnnotatedSite> mySiteList = new ArrayList<AnnotatedSite>();

    @Override
    public byte getReferenceAllele(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public byte[] getReference(int startSite, int endSite) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] getReference() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasReference() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getSNPIDs() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSNPID(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSiteCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getLocusSiteCount(Locus locus) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int[] getStartAndEndOfLocus(Locus locus) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPositionInLocus(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSiteOfPhysicalPosition(int physicalPosition, Locus locus) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSiteOfPhysicalPosition(int physicalPosition, Locus locus, String snpID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int[] getPhysicalPositions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte getPositionType(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] getPositionTypes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocusName(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Locus getLocus(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Locus getLocus(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Locus[] getLoci() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNumLoci() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int[] getLociOffsets() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getIndelSize(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isIndel(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte getMajorAllele(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getMajorAlleleAsString(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte getMinorAllele(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getMinorAlleleAsString(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] getMinorAlleles(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] getAlleles(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMinorAlleleFrequency(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMajorAlleleFrequency(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getGenomeAssembly() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPositiveStrand(int site) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int size() {
        return mySiteList.size();
    }

    @Override
    public boolean isEmpty() {
        return mySiteList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return mySiteList.contains(o);
    }

    @Override
    public Iterator<AnnotatedSite> iterator() {
        return mySiteList.iterator();
    }

    @Override
    public Object[] toArray() {
        return mySiteList.toArray();
    }

    @Override
    public <AnnotatedSite> AnnotatedSite[] toArray(AnnotatedSite[] a) {
        return mySiteList.toArray(a);
    }

    @Override
    public boolean add(AnnotatedSite e) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return mySiteList.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends AnnotatedSite> c) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public boolean addAll(int index, Collection<? extends AnnotatedSite> c) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public AnnotatedSite get(int index) {
        return mySiteList.get(index);
    }

    @Override
    public AnnotatedSite set(int index, AnnotatedSite element) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public void add(int index, AnnotatedSite element) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public AnnotatedSite remove(int index) {
        throw new UnsupportedOperationException("This Class is Immutable.");
    }

    @Override
    public int indexOf(Object o) {
        return mySiteList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return mySiteList.lastIndexOf(o);
    }

    @Override
    public ListIterator<AnnotatedSite> listIterator() {
        return mySiteList.listIterator();
    }

    @Override
    public ListIterator<AnnotatedSite> listIterator(int index) {
        return mySiteList.listIterator(index);
    }

    @Override
    public List<AnnotatedSite> subList(int fromIndex, int toIndex) {
        return mySiteList.subList(fromIndex, toIndex);
    }

}
