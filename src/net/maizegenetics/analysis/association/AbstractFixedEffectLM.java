package net.maizegenetics.analysis.association;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.maizegenetics.dna.snp.GenotypeTable;
import net.maizegenetics.dna.snp.score.SiteScore;
import net.maizegenetics.matrixalgebra.Matrix.DoubleMatrix;
import net.maizegenetics.phenotype.GenotypePhenotype;
import net.maizegenetics.phenotype.Phenotype;
import net.maizegenetics.phenotype.PhenotypeAttribute;
import net.maizegenetics.phenotype.Phenotype.ATTRIBUTE_TYPE;
import net.maizegenetics.plugindef.Datum;
import net.maizegenetics.stats.linearmodels.CovariateModelEffect;
import net.maizegenetics.stats.linearmodels.FactorModelEffect;
import net.maizegenetics.stats.linearmodels.LinearModelUtils;
import net.maizegenetics.stats.linearmodels.ModelEffect;
import net.maizegenetics.stats.linearmodels.ModelEffectUtils;
import net.maizegenetics.stats.linearmodels.SweepFastLinearModel;
import net.maizegenetics.taxa.Taxon;
import net.maizegenetics.util.BitSet;
import net.maizegenetics.util.OpenBitSet;
import net.maizegenetics.util.TableReport;
import net.maizegenetics.util.TableReportBuilder;

public abstract class AbstractFixedEffectLM implements FixedEffectLM {
	protected final Datum myDatum;
	protected final GenotypePhenotype myGenoPheno;
	protected final int numberOfObservations;
	protected final int numberOfSites;
	protected final List<PhenotypeAttribute> myDataAttributes;
	protected final List<PhenotypeAttribute> myFactorAttributes;
	protected final List<PhenotypeAttribute> myCovariateAttributes;
	protected TableReportBuilder siteReportBuilder;
	protected TableReportBuilder alleleReportBuilder;
	protected int numberOfSiteReportColumns;
	protected int numberOfAlleleReportColumns;
	protected float[] allData;
	protected int myCurrentSite;
	protected double[] siteData;
	protected OpenBitSet missingObsForSite;
	protected String currentTraitName;
	protected boolean areTaxaReplicated;
	protected boolean saveToFile = false;
	protected String siteReportFilename;
	protected String alleleReportFilename;
	protected double maxP = 1.0;
	
	//fields used for permutation testing
	protected boolean permute = false;
	protected int numberOfPermutations = 0;
	protected double[] minP = null;
	protected List<DoubleMatrix> permutedData;
	protected double[] baseErrorSSdf;
	protected double[] totalcfmSSdf;
	protected double[] markerSSdf;
	protected double[] errorSSdf;
	protected List<Object[]> siteTableReportRows;
	protected int markerpvalueColumn;
	protected int permpvalueColumn;
	protected ArrayList<ModelEffect> myModel;
	protected DoubleMatrix G;
	protected ArrayList<ModelEffect> myBaseModel;
	protected int numberOfBaseEffects;
	protected int taxaEffectNumber;
	protected int randomSeed;
	protected boolean useRandomSeed = false;
	protected Random rand = null;


    protected static final Map<SiteScore.SITE_SCORE_TYPE, String> typeNameMap;
    static {
    	typeNameMap = new HashMap<SiteScore.SITE_SCORE_TYPE, String>();
    	typeNameMap.put(SiteScore.SITE_SCORE_TYPE.ProbA, "A");
    	typeNameMap.put(SiteScore.SITE_SCORE_TYPE.ProbC, "C");
    	typeNameMap.put(SiteScore.SITE_SCORE_TYPE.ProbG, "G");
    	typeNameMap.put(SiteScore.SITE_SCORE_TYPE.ProbT, "T");
    	typeNameMap.put(SiteScore.SITE_SCORE_TYPE.ProbGap, "-");
    	typeNameMap.put(SiteScore.SITE_SCORE_TYPE.ProbInsertion, "+");
    }
    
	public AbstractFixedEffectLM(Datum dataset) {
		myDatum = dataset;
		myGenoPheno = (GenotypePhenotype) myDatum.getData();
		numberOfObservations = myGenoPheno.phenotype().numberOfObservations();
		numberOfSites = myGenoPheno.genotypeTable().numberOfSites();
		
		myDataAttributes = myGenoPheno.phenotype().attributeListOfType(ATTRIBUTE_TYPE.data);
		myFactorAttributes = myGenoPheno.phenotype().attributeListOfType(ATTRIBUTE_TYPE.factor);
		myCovariateAttributes = myGenoPheno.phenotype().attributeListOfType(ATTRIBUTE_TYPE.covariate);
		initializeReportBuilders();
		siteTableReportRows = new ArrayList<Object[]>();
		testTaxaReplication();
	}

	@Override
	public void initializeReportBuilders() {
		String tableName = "GLM Statistics - " + myDatum.getName();
		String[] columnNames = siteReportColumnNames();
		numberOfSiteReportColumns = columnNames.length;
		if (saveToFile) siteReportBuilder = TableReportBuilder.getInstance(tableName, columnNames, siteReportFilename);
		else siteReportBuilder = TableReportBuilder.getInstance(tableName, columnNames);
		 
		tableName = "GLM Genotype Effects - " + myDatum.getName();
		columnNames = alleleReportColumnNames();
		numberOfAlleleReportColumns = columnNames.length;
		if (saveToFile) alleleReportBuilder = TableReportBuilder.getInstance(tableName, columnNames, alleleReportFilename);
		else alleleReportBuilder = TableReportBuilder.getInstance(tableName, columnNames);
	}
	
	@Override
	public void solve() {
		//loop through data attributes
		//	loop through sites
		for (PhenotypeAttribute dataAttribute:myDataAttributes) {
			currentTraitName = dataAttribute.name();
			OpenBitSet missingObs = new OpenBitSet(dataAttribute.missing());
			for (PhenotypeAttribute attr:myFactorAttributes) missingObs.or(attr.missing());
			for (PhenotypeAttribute attr:myCovariateAttributes) missingObs.or(attr.missing());
			allData = (float[]) dataAttribute.allValues();
			if (permute) {
				missingObsForSite = missingObs;
				createPermutedData();
			}
			for (int s = 0; s < numberOfSites; s++) {
				//updata missing obs for this site
				myCurrentSite = s;
				getGenotypeAndUpdateMissing(missingObs);
				siteData = AssociationUtils.getNonMissingDoubles(allData, missingObsForSite);
				myBaseModel = baseModel();
				numberOfBaseEffects = myBaseModel.size();
				analyzeSite();
				if (permute) updateMinP();
			}
			
			if (permute) updateReportsWithPermutationP();
		}
	}
	
	@Override
	public TableReport siteReport() {
		return siteReportBuilder.build();
	}
	
	@Override
	public TableReport alleleReport() {
		return alleleReportBuilder.build();
	}
	
	@Override
	public List<Datum> datumList() {
		List<Datum> dataList = new ArrayList<Datum>();
		StringBuilder comment = new StringBuilder();
		comment.append("GLM Output\nStatistical Tests for individual variants.\n");
		comment.append("Input data: " + myDatum.getName()).append("\n");
		dataList.add(new Datum("GLM_Stats_" + myDatum.getName(), siteReport(), comment.toString()));
		comment = new StringBuilder();
		comment.append("GLM Output\nGenotype Effect Estimates\n");
		comment.append("Input data: " + myDatum.getName()).append("\n");
		dataList.add(new Datum("GLM_Genotypes_" + myDatum.getName(), alleleReport(), comment.toString()));
		return dataList;
	}
	
	@Override
	public void permutationTest(boolean permute, int nperm) {
		this.permute = permute;
		numberOfPermutations = nperm;
	}

	/**
	 * @param missingObsBeforeSite	a BitSet with bits set for observations missing in model covariates and data
	 */
	protected abstract void getGenotypeAndUpdateMissing(BitSet missingObsBeforeSite);
	
	/**
	 * @param siteNumber		a site number
	 * This method tests the significance of this site and estimates the allele effects then appends the results to the site and allele reports.
	 */
	protected abstract void analyzeSite();
	
	protected String[] siteReportColumnNames() {
		if (permute) return new String[]{"Trait","Marker","Chr","Position","marker_F","marker_p","perm_p","marker_Rsq","add_F","add_p","dom_F","dom_p", "marker_df","marker_MS","error_df","error_MS","model_df","model_MS" };
		return new String[] {"Trait","Marker","Chr","Position","marker_F","marker_p","marker_Rsq","add_F","add_p","dom_F","dom_p", "marker_df","marker_MS","error_df","error_MS","model_df","model_MS" };
	}
	
	protected String[] alleleReportColumnNames() {
		return new String[]{"Trait","Marker","Chr","Position","Obs","Allele","Estimate"};
	}
	
	protected ArrayList<ModelEffect> baseModel() {
		int numberOfNonmissingObs = numberOfObservations - (int) missingObsForSite.cardinality();
		ArrayList<ModelEffect> modelEffects = new ArrayList<ModelEffect>();
		FactorModelEffect meanEffect = new FactorModelEffect(new int[numberOfNonmissingObs], false);
		meanEffect.setID("mean");
		modelEffects.add(meanEffect);
		
		//add factors to model
		for (PhenotypeAttribute attr:myFactorAttributes) {
			String[] factorLabels = AssociationUtils.getNonMissingValues((String[]) attr.allValues(), missingObsForSite);
			FactorModelEffect fme = new FactorModelEffect(ModelEffectUtils.getIntegerLevels(factorLabels), true, attr.name());
			modelEffects.add(fme);
		}

		//add covariates to model
		for (PhenotypeAttribute attr:myCovariateAttributes) {
			double[] values = AssociationUtils.getNonMissingDoubles((double[]) attr.allValues(), missingObsForSite);
			CovariateModelEffect cme = new CovariateModelEffect(values, attr.name());
			modelEffects.add(cme);
		}

		return modelEffects;
	}
	
	protected void createPermutedData() {
		permutedData = new LinkedList<>();
		double[] y = AssociationUtils.getNonMissingDoubles(allData, missingObsForSite);
		SweepFastLinearModel sflm = new SweepFastLinearModel(baseModel(), y);
		DoubleMatrix residuals = sflm.getResiduals();
		DoubleMatrix predicted = sflm.getPredictedValues();
		baseErrorSSdf = sflm.getResidualSSdf();
		totalcfmSSdf = new double[2];
		double[] modelSSdf = sflm.getModelcfmSSdf();
		totalcfmSSdf[0] = baseErrorSSdf[0] + modelSSdf[0];
		totalcfmSSdf[1] = baseErrorSSdf[1] + modelSSdf[1];
		
		if (rand == null) {
			if (useRandomSeed) rand = new Random(randomSeed);
			else rand = new Random();
		}
		
		for (int p = 0; p < numberOfPermutations; p++) {
			LinearModelUtils.shuffle(residuals, rand);
			DoubleMatrix permdm = predicted.plus(residuals);
			permutedData.add(permdm);
		}
		
		minP = new double[numberOfPermutations];
		Arrays.fill(minP, 1.0);
	}

	protected void updateReportsWithPermutationP() {
		Arrays.sort(minP);
		for (Object[] row : siteTableReportRows) {
			double pval = ((Double) row[markerpvalueColumn]);
			int ndx = Arrays.binarySearch(minP, pval);
			if (ndx < 0) ndx = -(ndx + 1);
			row[permpvalueColumn] = new Double((double) (ndx + 1) / numberOfPermutations);
		}
	}

	protected ModelEffect taxaEffect() {
        Taxon[] myTaxa = myGenoPheno.phenotype().taxaAttribute().allTaxa();
        Taxon[] myNonMissingTaxa = AssociationUtils.getNonMissingValues(myTaxa, missingObsForSite);
        int[] taxaLevels = ModelEffectUtils.getIntegerLevels(myNonMissingTaxa);
        FactorModelEffect taxaEffect = new FactorModelEffect(taxaLevels, true, "Taxon");
        return taxaEffect;
	}
	
	protected void testTaxaReplication() {
		int numberOfObservations = myGenoPheno.phenotype().numberOfObservations();
		int numberOfTaxa = myGenoPheno.genotypeTable().numberOfTaxa();
		if (numberOfTaxa < numberOfObservations) areTaxaReplicated = true;
		else areTaxaReplicated = false;
	}
	
	protected void updateMinP() {
		if (areTaxaReplicated) {
			//if taxa are replicated
			int iter = 0;
			for (DoubleMatrix pdata : permutedData) {
		        SweepFastLinearModel sflm = new SweepFastLinearModel(myModel, pdata.to1DArray());
		        markerSSdf = sflm.getIncrementalSSdf(numberOfBaseEffects);
		        errorSSdf = sflm.getIncrementalSSdf(taxaEffectNumber);
		        double F = markerSSdf[0] / markerSSdf[1] / errorSSdf[0] * errorSSdf[1];
		        double p;
		        try {
		        	p = LinearModelUtils.Ftest(F, markerSSdf[1], errorSSdf[1]);
		        	if (minP[iter] > p) minP[iter] = p;
		        } catch (Exception e) {
		        	//do nothing
		        }
			}
		} else {
			int iter = 0;
			for (DoubleMatrix pdata : permutedData) {
				DoubleMatrix thisData = AssociationUtils.getNonMissingValues(pdata, missingObsForSite);
				double yty = pdata.crossproduct(thisData).get(0, 0);
				DoubleMatrix Xty = myModel.get(0).getX().crossproduct(thisData);
				double ssmodel = Xty.crossproduct(G).mult(Xty).get(0, 0);
				double sse = yty - ssmodel;
				double ssmarker = baseErrorSSdf[0] - sse;
				double permF = ssmarker / markerSSdf[1] / sse * errorSSdf[1];
				double permP;
				try {
					permP = LinearModelUtils.Ftest(permF, markerSSdf[1], errorSSdf[1]);
					if (minP[iter] > permP) minP[iter] = permP;
				} catch (Exception e) {
					//do nothing
				}
				iter++;
			}
		}
		
	}
	
	@Override
	public void maxP(double maxP) {
		this.maxP = maxP;
	}

	@Override
	public void siteReportFilepath(String savefile) {
		siteReportFilename = savefile;
	}

	@Override
	public void alleleReportFilepath(String savefile) {
		alleleReportFilename = savefile;
	}

	/**
	 * This method is used mainly for testing in order to generate reproducible permutation results. 
	 * If the seed is not set, the current time is used to initialize the random number generator.
	 * @param seed	the seed used to initialize the random number generator used by permutation
	 */
	public void setRandomSeed(int seed) {
		randomSeed = seed;
		useRandomSeed = true;
	}
}