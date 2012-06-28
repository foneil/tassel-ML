package net.maizegenetics.gwas.imputation;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import net.maizegenetics.baseplugins.FileLoadPlugin;
import net.maizegenetics.pal.alignment.Alignment;
import net.maizegenetics.pal.alignment.ExportUtils;
import net.maizegenetics.pal.alignment.FilterAlignment;
import net.maizegenetics.pal.alignment.TBitAlignment;
import net.maizegenetics.pal.ids.SimpleIdGroup;
import net.maizegenetics.plugindef.AbstractPlugin;
import net.maizegenetics.plugindef.DataSet;
import net.maizegenetics.plugindef.Datum;
import net.maizegenetics.plugindef.PluginEvent;

public class CallParentAllelesPlugin extends AbstractPlugin {
	private static final Logger myLogger = Logger.getLogger(CallParentAllelesPlugin.class);
	private String pedfileName = null;
	private int minAlleleCount = 2; //minimum allele count for the minor allele to consider that a site might be polymorphic
	private int windowSize = 100;  //the number of sites to be used a window for determining the original set of snps in LD
	private int numberToTry = 10; //the number of different windows to check for snps in LD
	private double cutHeightSnps = 0.2;  //the tree cut height used to find the largest cluster of correlated SNPs
	private double minRforSnps = 0.5;  //the minimum R used to judge whether a snp is in ld with a test group
	
	public CallParentAllelesPlugin(Frame parentFrame) {
        super(parentFrame, false);
	}
	
	@Override
	public DataSet performFunction(DataSet input) {
		if (pedfileName == null) {
			myLogger.error(getUsage());
			return null;
		}
		
		List<Datum> inputAlignments = input.getDataOfType(Alignment.class);
		ArrayList<PopulationData> familyList = PopulationData.readPedigreeFile(pedfileName);
		LinkedList<Datum> datumList = new LinkedList<Datum>();

		for (Datum d : inputAlignments) {
			Alignment align = (Alignment) d.getData();
			for (PopulationData family : familyList) {
				myLogger.info("Calling parent alleles for family " + family.name + ", chromosome " + align.getLocusName(0) + ".");
				
				String[] ids = new String[family.members.size()];
				family.members.toArray(ids);
				family.original =  FilterAlignment.getInstance(align, new SimpleIdGroup(ids), false);
//				ImputationUtils.printAlleleStats(family.original, family.name);
//				NucleotideImputationUtils.callParentAlleles(family, minAlleleCount, windowSize, numberToTry, cutHeightSnps, minRforSnps);
				NucleotideImputationUtils.callParentAllelesByWindow(family, 0.75, windowSize, numberToTry, cutHeightSnps, minRforSnps);
				String comment = "Parent Calls for family " + family.name + " from " + d.getName() + ".";
				datumList.add(new Datum(family.name, family, comment));
			}
		}
		
		DataSet resultDS =  new DataSet(datumList, this);
		fireDataSetReturned(new PluginEvent(resultDS, CallParentAllelesPlugin.class));
		return resultDS;
	}

	@Override
	public void setParameters(String[] args) {
		if (args == null || args.length == 0) {
			myLogger.error(getUsage());
			return;
		}
		
		int narg = args.length;
		for (int i = 0; i < narg - 1; i++) {
			if (args[i].equals("-p") || args[i].equalsIgnoreCase("-pedigrees")) {
				pedfileName = args[++i];
			}
			else if (args[i].equals("-a") || args[i].equalsIgnoreCase("-minAlleleCount")) {
				minAlleleCount = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-w") || args[i].equalsIgnoreCase("-windowSize")) {
				windowSize = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-t") || args[i].equalsIgnoreCase("-numberToTry")) {
				numberToTry = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-h") || args[i].equalsIgnoreCase("-cutHeight")) {
				cutHeightSnps = Double.parseDouble(args[++i]);
			}
			else if (args[i].equals("-r") || args[i].equalsIgnoreCase("-minR")) {
				minRforSnps = Double.parseDouble(args[++i]);
			}
			else if (args[i].equals("-l") || args[i].equalsIgnoreCase("-logconfig")) {
				DOMConfigurator.configure(args[++i]);
			}
			else if (args[i].equals("?")) myLogger.error(getUsage());
		}
	}
	
	public void setPedfileName(String pedfileName) {
		this.pedfileName = pedfileName;
	}

	public void setMinAlleleCount(int minAlleleCount) {
		this.minAlleleCount = minAlleleCount;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public void setNumberToTry(int numberToTry) {
		this.numberToTry = numberToTry;
	}

	public void setCutHeightSnps(double cutHeightSnps) {
		this.cutHeightSnps = cutHeightSnps;
	}

	public void setMinRforSnps(double minRforSnps) {
		this.minRforSnps = minRforSnps;
	}

	@Override
	public ImageIcon getIcon() {
		return null;
	}

	@Override
	public String getButtonName() {
		return "Call Parents";
	}

	@Override
	public String getToolTipText() {
		return null;
	}

	private String getUsage() {
		StringBuilder usage = new StringBuilder("The CallParentAllelesPlugin requires the following parameter:\n");
		usage.append("-p or -pedigrees : a file containing pedigrees of the individuals to be imputed\n");
		usage.append("The following parameters are optional:\n");
		usage.append("-a or -minAlleleCount : the minimum minor allele count for a site to be considered polymorphic (default = 2).\n");
		usage.append("-w or -windowSize : the number of SNPs to examine for the initial LD cluster (default = 100)\n");
		usage.append("-t or -numberToTry : the number of windows to test for the initial SNP cluster (default = 3)\n");
		usage.append("-h or -cutHeight : the height at which to cut the SNP tree (default = 0.3)\n");
		usage.append("-r or -minR : minimum R used to test SNPs for LD (default = 0.8, good for RILs, try 0.4 for F2s)\n");
		usage.append("-l or -logconfig : an xml configuration file for the logger. Default will be to print all messages to console.\n");
		usage.append("? : print the parameter list.\n");

		return usage.toString();
	}
}