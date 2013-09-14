/*
 *  TaxaListUtils
 */
package net.maizegenetics.pal.taxa;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 *
 * @author Terry Casstevens
 */
public class TaxaListUtils {

    private TaxaListUtils() {
        // utility class
    }

    /**
     * Intersect joins the specified groups.
     *
     * @param group1 an TaxaList
     * @param group2 another TaxaList
     *
     * @return the taxa in the intersection of groups 1 and 2, sorted in
     * ascending order
     */
    public static TaxaList getCommonTaxa(TaxaList group1, TaxaList group2) {
        return getCommonTaxa(new TaxaList[]{group1, group2});
    }

    /**
     * Intersect joins the specified groups.
     *
     * @param groups groups to join.
     * @return The taxa from the intersect join, sorted in ascending order
     */
    public static TaxaList getCommonTaxa(TaxaList[] groups) {

        if ((groups == null) || (groups.length == 0)) {
            return null;
        }

        TreeSet<AnnotatedTaxon> intersectIds = new TreeSet<AnnotatedTaxon>();
        for (int x = 0; x < groups[0].getTaxaCount(); x++) {
            intersectIds.add(groups[0].get(x));
        }
        for (int i = 1; i < groups.length; i++) {
            List<AnnotatedTaxon> temp = new ArrayList<AnnotatedTaxon>();
            for (int j = 0; j < groups[i].getTaxaCount(); j++) {
                temp.add(groups[i].get(j));
            }
            intersectIds.retainAll(temp);
        }

        TaxaListBuilder builder = new TaxaListBuilder();
        builder.addAll(intersectIds);
        return builder.build();

    }

    /**
     * Union joins the specified groups.
     *
     * @param group1 an TaxaList
     * @param group2 another TaxaList
     *
     * @return	the taxa in the union of groups 1 and 2, sorted in ascending
     * order
     */
    public static TaxaList getAllTaxa(TaxaList group1, TaxaList group2) {
        return getAllTaxa(new TaxaList[]{group1, group2});
    }

    /**
     * Union joins the specified groups.
     *
     * @param groups groups to join.
     *
     * @return The taxa from the union join, sorted in ascending order
     */
    public static TaxaList getAllTaxa(TaxaList[] groups) {

        if ((groups == null) || (groups.length == 0)) {
            return null;
        }

        TreeSet<AnnotatedTaxon> allIds = new TreeSet<AnnotatedTaxon>();
        for (int i = 0; i < groups.length; i++) {
            int n = groups[i].getTaxaCount();
            for (int j = 0; j < n; j++) {
                allIds.add(groups[i].get(j));
            }
        }

        TaxaListBuilder builder = new TaxaListBuilder();
        builder.addAll(allIds);
        return builder.build();

    }
}
