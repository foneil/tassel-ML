package net.maizegenetics.gwas.imputation;

import java.util.HashMap;

import net.maizegenetics.taxa.Taxon;


public class SubpopulationFinder {
	final static HashMap<String, Integer> taxaToPopMap = new HashMap<String, Integer>();
	
	static {
		taxaToPopMap.put("Z001E0215",0);
		taxaToPopMap.put("Z001E0229",0);
		taxaToPopMap.put("Z001E0262",2);
		taxaToPopMap.put("Z001E0274",2);
		taxaToPopMap.put("Z001E0279",2);
		taxaToPopMap.put("Z002E0207",0);
		taxaToPopMap.put("Z002E0217",0);
		taxaToPopMap.put("Z002E0271",2);
		taxaToPopMap.put("Z003E0212",0);
		taxaToPopMap.put("Z003E0220",0);
		taxaToPopMap.put("Z003E0264",2);
		taxaToPopMap.put("Z003E0266",2);
		taxaToPopMap.put("Z003E0272",2);
		taxaToPopMap.put("Z004E0208",0);
		taxaToPopMap.put("Z004E0232",1);
		taxaToPopMap.put("Z005E0204",0);
		taxaToPopMap.put("Z005E0210",0);
		taxaToPopMap.put("Z005E0211",0);
		taxaToPopMap.put("Z005E0212",0);
		taxaToPopMap.put("Z005E0213",0);
		taxaToPopMap.put("Z005E0215",0);
		taxaToPopMap.put("Z005E0233",1);
		taxaToPopMap.put("Z005E0249",1);
		taxaToPopMap.put("Z005E0267",2);
		taxaToPopMap.put("Z005E0269",2);
		taxaToPopMap.put("Z005E0270",2);
		taxaToPopMap.put("Z006E0203",0);
		taxaToPopMap.put("Z006E0204",0);
		taxaToPopMap.put("Z006E0206",0);
		taxaToPopMap.put("Z006E0215",0);
		taxaToPopMap.put("Z006E0216",0);
		taxaToPopMap.put("Z006E0219",0);
		taxaToPopMap.put("Z006E0224",0);
		taxaToPopMap.put("Z006E0225",0);
		taxaToPopMap.put("Z006E0226",0);
		taxaToPopMap.put("Z006E0228",0);
		taxaToPopMap.put("Z006E0232",1);
		taxaToPopMap.put("Z006E0233",1);
		taxaToPopMap.put("Z006E0245",1);
		taxaToPopMap.put("Z006E0255",1);
		taxaToPopMap.put("Z007E0213",0);
		taxaToPopMap.put("Z007E0217",0);
		taxaToPopMap.put("Z007E0219",0);
		taxaToPopMap.put("Z007E0223",0);
		taxaToPopMap.put("Z007E0230",1);
		taxaToPopMap.put("Z007E0284",2);
		taxaToPopMap.put("Z008E0253",1);
		taxaToPopMap.put("Z008E0261",2);
		taxaToPopMap.put("Z009E0214",0);
		taxaToPopMap.put("Z009E0219",0);
		taxaToPopMap.put("Z009E0223",0);
		taxaToPopMap.put("Z009E0243",1);
		taxaToPopMap.put("Z009E0257",2);
		taxaToPopMap.put("Z009E0259",2);
		taxaToPopMap.put("Z009E0266",2);
		taxaToPopMap.put("Z009E0284",2);
		taxaToPopMap.put("Z010E0214",0);
		taxaToPopMap.put("Z010E0267",2);
		taxaToPopMap.put("Z010E0280",2);
		taxaToPopMap.put("Z010E0283",2);
		taxaToPopMap.put("Z011E0217",0);
		taxaToPopMap.put("Z011E0226",0);
		taxaToPopMap.put("Z011E0230",1);
		taxaToPopMap.put("Z012E0231",1);
		taxaToPopMap.put("Z012E0260",2);
		taxaToPopMap.put("Z012E0278",2);
		taxaToPopMap.put("Z013E0204",2);
		taxaToPopMap.put("Z013E0207",2);
		taxaToPopMap.put("Z013E0208",0);
		taxaToPopMap.put("Z013E0211",0);
		taxaToPopMap.put("Z013E0218",0);
		taxaToPopMap.put("Z013E0221",2);
		taxaToPopMap.put("Z013E0222",0);
		taxaToPopMap.put("Z013E0224",2);
		taxaToPopMap.put("Z013E0225",2);
		taxaToPopMap.put("Z013E0226",0);
		taxaToPopMap.put("Z013E0227",2);
		taxaToPopMap.put("Z013E0228",2);
		taxaToPopMap.put("Z013E0229",2);
		taxaToPopMap.put("Z013E0231",2);
		taxaToPopMap.put("Z013E0232",2);
		taxaToPopMap.put("Z013E0233",2);
		taxaToPopMap.put("Z013E0234",2);
		taxaToPopMap.put("Z013E0235",2);
		taxaToPopMap.put("Z013E0236",2);
		taxaToPopMap.put("Z013E0238",2);
		taxaToPopMap.put("Z013E0239",2);
		taxaToPopMap.put("Z013E0240",2);
		taxaToPopMap.put("Z013E0241",2);
		taxaToPopMap.put("Z013E0242",1);
		taxaToPopMap.put("Z013E0247",1);
		taxaToPopMap.put("Z013E0249",2);
		taxaToPopMap.put("Z013E0250",2);
		taxaToPopMap.put("Z013E0251",2);
		taxaToPopMap.put("Z013E0253",2);
		taxaToPopMap.put("Z013E0256",2);
		taxaToPopMap.put("Z013E0285",2);
		taxaToPopMap.put("Z013E0286",2);
		taxaToPopMap.put("Z013E0287",2);
		taxaToPopMap.put("Z013E0288",2);
		taxaToPopMap.put("Z013E0289",2);
		taxaToPopMap.put("Z013E0290",2);
		taxaToPopMap.put("Z013E0291",2);
		taxaToPopMap.put("Z013E0292",2);
		taxaToPopMap.put("Z013E0293",2);
		taxaToPopMap.put("Z013E0294",2);
		taxaToPopMap.put("Z013E0295",2);
		taxaToPopMap.put("Z013E0296",2);
		taxaToPopMap.put("Z013E0297",2);
		taxaToPopMap.put("Z013E0298",2);
		taxaToPopMap.put("Z013E0299",2);
		taxaToPopMap.put("Z013E0300",2);
		taxaToPopMap.put("Z013E0301",2);
		taxaToPopMap.put("Z013E0302",2);
		taxaToPopMap.put("Z013E0303",2);
		taxaToPopMap.put("Z013E0304",2);
		taxaToPopMap.put("Z013E0305",2);
		taxaToPopMap.put("Z013E0306",2);
		taxaToPopMap.put("Z013E0307",2);
		taxaToPopMap.put("Z013E0308",2);
		taxaToPopMap.put("Z013E0309",2);
		taxaToPopMap.put("Z013E0310",2);
		taxaToPopMap.put("Z013E0311",2);
		taxaToPopMap.put("Z013E0312",2);
		taxaToPopMap.put("Z013E0313",2);
		taxaToPopMap.put("Z013E0314",2);
		taxaToPopMap.put("Z013E0315",2);
		taxaToPopMap.put("Z013E0316",2);
		taxaToPopMap.put("Z013E0317",2);
		taxaToPopMap.put("Z013E0318",2);
		taxaToPopMap.put("Z013E0319",2);
		taxaToPopMap.put("Z013E0320",2);
		taxaToPopMap.put("Z013E0321",2);
		taxaToPopMap.put("Z013E0322",2);
		taxaToPopMap.put("Z013E0323",2);
		taxaToPopMap.put("Z013E0324",2);
		taxaToPopMap.put("Z014E0206",0);
		taxaToPopMap.put("Z014E0208",0);
		taxaToPopMap.put("Z014E0259",2);
		taxaToPopMap.put("Z015E0203",0);
		taxaToPopMap.put("Z015E0204",1);
		taxaToPopMap.put("Z015E0230",1);
		taxaToPopMap.put("Z015E0232",1);
		taxaToPopMap.put("Z015E0235",1);
		taxaToPopMap.put("Z015E0237",1);
		taxaToPopMap.put("Z015E0238",1);
		taxaToPopMap.put("Z015E0242",1);
		taxaToPopMap.put("Z015E0243",1);
		taxaToPopMap.put("Z015E0282",2);
		taxaToPopMap.put("Z016E0213",0);
		taxaToPopMap.put("Z016E0220",0);
		taxaToPopMap.put("Z016E0238",1);
		taxaToPopMap.put("Z016E0251",1);
		taxaToPopMap.put("Z016E0254",1);
		taxaToPopMap.put("Z016E0274",2);
		taxaToPopMap.put("Z018E0220",0);
		taxaToPopMap.put("Z018E0223",0);
		taxaToPopMap.put("Z018E0228",0);
		taxaToPopMap.put("Z018E0273",2);
		taxaToPopMap.put("Z019E0209",0);
		taxaToPopMap.put("Z019E0226",0);
		taxaToPopMap.put("Z019E0263",2);
		taxaToPopMap.put("Z020E0208",0);
		taxaToPopMap.put("Z020E0232",1);
		taxaToPopMap.put("Z020E0234",1);
		taxaToPopMap.put("Z020E0270",2);
		taxaToPopMap.put("Z020E0281",2);
		taxaToPopMap.put("Z021E0205",0);
		taxaToPopMap.put("Z021E0216",0);
		taxaToPopMap.put("Z021E0235",1);
		taxaToPopMap.put("Z021E0239",1);
		taxaToPopMap.put("Z021E0241",1);
		taxaToPopMap.put("Z021E0270",2);
		taxaToPopMap.put("Z021E0275",2);
		taxaToPopMap.put("Z021E0278",2);
		taxaToPopMap.put("Z021E0283",2);
		taxaToPopMap.put("Z022E0207",0);
		taxaToPopMap.put("Z022E0215",0);
		taxaToPopMap.put("Z022E0219",0);
		taxaToPopMap.put("Z022E0221",0);
		taxaToPopMap.put("Z022E0272",2);
		taxaToPopMap.put("Z023E0217",0);
		taxaToPopMap.put("Z023E0225",0);
		taxaToPopMap.put("Z023E0229",0);
		taxaToPopMap.put("Z023E0230",1);
		taxaToPopMap.put("Z023E0231",1);
		taxaToPopMap.put("Z023E0265",2);
		taxaToPopMap.put("Z023E0269",2);
		taxaToPopMap.put("Z023E0272",2);
		taxaToPopMap.put("Z023E0274",2);
		taxaToPopMap.put("Z024E0203",0);
		taxaToPopMap.put("Z024E0205",0);
		taxaToPopMap.put("Z024E0212",0);
		taxaToPopMap.put("Z024E0258",2);
		taxaToPopMap.put("Z024E0264",2);
		taxaToPopMap.put("Z024E0268",2);
		taxaToPopMap.put("Z024E0273",2);
		taxaToPopMap.put("Z024E0276",2);
		taxaToPopMap.put("Z024E0282",2);
		taxaToPopMap.put("Z024E0287",2);
		taxaToPopMap.put("Z024E0289",2);
		taxaToPopMap.put("Z025E0211",0);
		taxaToPopMap.put("Z025E0213",0);
		taxaToPopMap.put("Z025E0218",0);
		taxaToPopMap.put("Z025E0226",0);
		taxaToPopMap.put("Z025E0238",1);
		taxaToPopMap.put("Z025E0245",1);
		taxaToPopMap.put("Z025E0246",1);
		taxaToPopMap.put("Z025E0250",1);
		taxaToPopMap.put("Z025E0270",2);
		taxaToPopMap.put("Z026E0222",0);
		taxaToPopMap.put("Z026E0225",0);
		taxaToPopMap.put("Z026E0230",1);
		taxaToPopMap.put("Z026E0231",1);
		taxaToPopMap.put("Z026E0232",1);
		taxaToPopMap.put("Z026E0263",2);
		taxaToPopMap.put("Z026E0269",2);
		taxaToPopMap.put("Z026E0277",2);
		taxaToPopMap.put("Z026E0280",2);
	}
	
	public static int getNamSubPopulation(String taxonname) {
		String shortname;
		int colonpos = taxonname.indexOf(':');
		if (colonpos < 0) shortname = taxonname;
		else shortname = taxonname.substring(0, colonpos);
		
		int entry = Integer.parseInt(shortname.substring(5, 9));
		if (entry < 68) return 0;
		else if (entry < 135) return 1;
		else if (entry < 201) return 2;
		
		Integer pop = taxaToPopMap.get(shortname);
		if (pop == null) return -1;
		else return pop;
	}
	
	public static int getNamSubPopulation(Taxon taxon) {
		return getNamSubPopulation(taxon.getName());
	}
}
