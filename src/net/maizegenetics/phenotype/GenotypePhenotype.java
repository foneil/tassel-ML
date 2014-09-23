package net.maizegenetics.phenotype;

import net.maizegenetics.dna.snp.GenotypeTable;
import net.maizegenetics.dna.snp.score.SiteScore;
import net.maizegenetics.taxa.TaxaList;
import net.maizegenetics.util.TableReport;

/**
 * This class holds phenotypes and genotypes for a set of Taxa. 
 * @author Peter Bradbury
 * 
 */

public class GenotypePhenotype implements TableReport {
	private final GenotypeTable myGenotype;
	private final Phenotype myPhenotype;
	private final String name;
	
	/**
	 * @param theGenotype	a GenotypeTable
	 * @param thePhenotype	a Phenotype
	 * @param name	the name that will be displayed for this in the GUI
	 */
	GenotypePhenotype(GenotypeTable theGenotype, Phenotype thePhenotype, String name) {
		myGenotype = theGenotype;
		myPhenotype = thePhenotype;
		this.name = name;
	}
	
	/**
	 * @return the GenotypeTable used by this object
	 */
	public GenotypeTable genotypeTable() {
		return myGenotype;
	}
	
	/**
	 * @return	the Phenotype used by this object
	 */
	public Phenotype phenotype() {
		return myPhenotype;
	}

	/**
	 * @return	true if genotypes are discrete (nucleotides), false if the genotypes are numeric
	 * The GenotypeTable backing this object may hold both types of data. The boolean indicates which is to be used in an analysis.
	 */
	public boolean areGenotypeValuesDiscrete() {
		//TODO implement once hasGenotype is implemented
		return true;
	}
	
	/**
	 * @return	true if taxa are replicated (more observations than taxa), false otherwise
	 */
	public boolean areTaxaReplicated() {
		myPhenotype.areTaxaReplicated();
		return true;
	}
	
	/**
	 * @param site	the site in the GenotypeTable
	 * @return	the genotypes corresponding to every row of the phenotype table as String values
	 */
	public String[] getStringGenotype(int site) {
		TaxaAttribute myTaxaAttr = myPhenotype.taxaAttribute();
		TaxaList myTaxaList = myGenotype.taxa();
		int numberOfObs = myPhenotype.numberOfObservations();
		String[] geno = new String[numberOfObs];
		for (int obs = 0; obs < numberOfObs; obs++) {
			int ndx = myTaxaList.indexOf(myTaxaAttr.taxon(obs));
			geno[obs] = myGenotype.genotypeAsString(ndx, site);
		}
		return geno;
	}
	
	public float[] alleleProbsOfType(SiteScore.SITE_SCORE_TYPE type, int site) {
		TaxaAttribute myTaxaAttr = myPhenotype.taxaAttribute();
		TaxaList myTaxaList = myGenotype.taxa();
		int numberOfObs = myPhenotype.numberOfObservations();
		float[] values = new float[numberOfObs];
		for (int obs = 0; obs < numberOfObs; obs++) {
			int ndx = myTaxaList.indexOf(myTaxaAttr.taxon(obs));
			values[obs] = myGenotype.alleleProbability(ndx, site, type);
		}
		return values;
	}

	
	//implement TableReport methods
	@Override
	public Object[] getTableColumnNames() {
		int numberOfPhenotypeColumns = myPhenotype.getColumnCount();
		Object[] colNames = new Object[numberOfPhenotypeColumns + 1];
		System.arraycopy(myPhenotype.getTableColumnNames(), 0, colNames, 0, numberOfPhenotypeColumns);
		colNames[numberOfPhenotypeColumns] = "Genotype";
		return null;
	}

	@Override
	public String getTableTitle() {
		return name;
	}

	@Override
	public int getColumnCount() {
		return 1 + myPhenotype.getColumnCount();
	}

	@Override
	public long getRowCount() {
		return myPhenotype.getRowCount();
	}

	@Override
	public long getElementCount() {
		return getColumnCount() * getRowCount();
	}

	@Override
	public Object[] getRow(long row) {
		int numberOfPhenotypeColumns = myPhenotype.getColumnCount();
		Object[] rowData = new Object[getColumnCount()];
		System.arraycopy(myPhenotype.getRow(row), 0, rowData, 0, numberOfPhenotypeColumns);
		rowData[numberOfPhenotypeColumns] = genotypeToDisplay((int) row);
		return rowData;
	}

	@Override
	public Object getValueAt(long row, int col) {
        int haplotypeColumn = myPhenotype.getColumnCount();
        if (col == haplotypeColumn) return genotypeToDisplay((int) row);
        return myPhenotype.getValueAt(row, col);
	}
	
	private String genotypeToDisplay(int row) {
		int genotypeRow = indexOfGenotype(row);
        int siteCount = Math.min(myGenotype.numberOfSites(), 10);
        StringBuilder builder = new StringBuilder();
        builder.append(myGenotype.genotypeAsStringRange(genotypeRow, 0, siteCount));
        if (myGenotype.numberOfSites() > 10) {
            builder.append("...");
        }
        return builder.toString();
	}
	
	private int indexOfGenotype(int phenotypeRow) {
		return myGenotype.taxa().indexOf(myPhenotype.taxa().get(phenotypeRow));
	}
}
