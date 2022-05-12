package org.ncgr.soybase;

import org.ncgr.crossref.WorksQuery;

import java.io.File;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Builds GWAS collections from the local MySQL copy of SoyBase.
 */
public class GWASCollectionBuilder {

    static String dbUrl = "jdbc:mysql://localhost:3306/soybase";
    static String dbUsername = "sam";
    static String dbPassword = "samspassword";
    
    public static void main(String[] args) {
        try {
            // initialize DB connection and statement
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            Statement stmt = conn.createStatement();

            // query GWAS experiments, key maps by experiment_id
            Map<Integer,String> soybaseIds = new HashMap<>();
            Map<Integer,String> platforms = new HashMap<>();
            Map<Integer,Integer> germplasmNumbers = new HashMap<>();
            String queryExperiment = "SELECT * FROM gwas_experiment ORDER BY soybase_id";
            ResultSet rsExperiment = stmt.executeQuery(queryExperiment);
            while (rsExperiment.next()) {
                int id = rsExperiment.getInt("experiment_id");
                soybaseIds.put(id, rsExperiment.getString("soybase_id"));
                platforms.put(id, rsExperiment.getString("platform_name"));
                try {
                    int num = Integer.parseInt(rsExperiment.getString("number_germplasm_tested"));
                    germplasmNumbers.put(id, num);
                } catch (NumberFormatException ex) {
                    germplasmNumbers.put(id, 0);
                }
            }

            // query each experiment reference and output data if it has a DOI
            for (int experimentId : soybaseIds.keySet()) {
                String soybaseId = soybaseIds.get(experimentId);
                String queryPub = "SELECT * FROM paper_table WHERE soybase_id='"+soybaseId+"'";
                ResultSet rsPub = stmt.executeQuery(queryPub);
                if (rsPub.next()) {
                    String doi = rsPub.getString("doi");
                    String title = rsPub.getString("original_title");
                    boolean found = false;
                    WorksQuery wq = null;
                    try {
                        if (doi==null || doi.trim().length()==0) {
                            // search for publication on the title
                            wq = new WorksQuery(null, title);
                            found = wq.getStatus().equals("ok") && wq.isTitleMatched();
                        } else {
                            // search for publication on the DOI
                            wq = new WorksQuery(doi);
                            found = wq.getStatus().equals("ok");
                        }
                    } catch (Exception ex) {
                        System.err.println(ex.toString());
                        System.err.println("In WorksQuery constructor");
                        System.exit(1);
                    }
                    if (!found) continue;
                    // update DOI and title
                    doi = wq.getDOI();
                    title = wq.getTitle().replace('\n',' ').trim().replaceAll(" +", " ");
                    // authors
                    List<String> authorGivenList = new ArrayList<>();
                    List<String> authorFamilyList = new ArrayList<>();
                    for (Object authorObject : wq.getAuthors()) {
                        // {"given":"D. R.","sequence":"first","affiliation":[],"family":"Panthee"}
                        JSONObject authorJSON = (JSONObject) authorObject;
                        authorGivenList.add((String) authorJSON.get("given"));
                        authorFamilyList.add((String) authorJSON.get("family"));
                    }
                    // article year
                    int year = 1111;
                    try {
                        year = wq.getJournalIssueYear();
                        if (year==0) year = wq.getIssuedYear();
                    } catch (Exception ex) {
                        // do nothing
                    }
                    // form collection identifier
                    String collection = "mixed.gwas."+authorFamilyList.get(0).replaceAll(" ","-");
                    if (authorFamilyList.size()>1) collection += "_"+authorFamilyList.get(1).replaceAll(" ","-");
                    collection += "_"+year;
                    // contributors
                    String contributors = "";
                    for (int i=0; i<authorGivenList.size(); i++) {
                        if (i>0) contributors += ", ";
                        contributors += authorGivenList.get(i)+" "+authorFamilyList.get(i);
                    }

                    // get "GWAS QTL" info from gwas_qtl for this collection; maps are keyed by snp_id
                    // | gwas_id | gwas_name         | other_name | gwas_family    | gwas_class   | experiment_id | trait_SOY_number
                    // |       1 | Seed protein 3-g1 |            | Seed protein 3 | Seed protein |             1 | SOY:0001676      
                    // | QTL_type    | QTL_category               | snp_id | snp_name    | p_value  | LOD  | R2   | comments
                    // | QTL_protein | seed_composition_and_yield | 119892 | ss715637225 | 3.24E-06 |      |      | This GWAS QTL was originally reported on Gm14...
                    Map<Integer,String> snpNames = new HashMap<>();
                    Map<Integer,String> snpTraits = new HashMap<>();
                    Map<Integer,Double> snpPValues = new HashMap<>();
                    Map<String,List<String>> traitOboTerms = new HashMap<>();
                    Set<String> traits = new HashSet<>();
                    String querySNP = "SELECT * FROM gwas_qtl WHERE experiment_id="+experimentId;
                    ResultSet rsSNP = stmt.executeQuery(querySNP);
                    // skip experiments that don't have p-values
                    boolean missingPValues = false;
                    while (rsSNP.next()) {
                        int snpId = rsSNP.getInt("snp_id");
                        snpNames.put(snpId, rsSNP.getString("snp_name"));
                        snpTraits.put(snpId, rsSNP.getString("gwas_class"));
                        // unique trait set
                        traits.add(rsSNP.getString("gwas_class"));
                        try {
                            double pValue = Double.parseDouble(rsSNP.getString("p_value"));
                            snpPValues.put(snpId, pValue);
                        } catch (Exception ex) {
                            missingPValues = true;
                        }
                        // these are just SOY OBO terms
                        List<String> oboTerms = new ArrayList<>();
                        oboTerms.add(rsSNP.getString("trait_SOY_number"));
                        traitOboTerms.put(rsSNP.getString("gwas_class"), oboTerms);
                    }

                    if (!missingPValues) {
                        // get additional OBO terms from trait_xref_table
                        for (String trait : traits) {
                            String queryTraitObo = "SELECT * FROM trait_xref_table WHERE traitname='"+trait+"'";
                            ResultSet rsTraitObo = stmt.executeQuery(queryTraitObo);
                            List<String> oboTerms = traitOboTerms.get(trait);
                            while (rsTraitObo.next()) {
                                oboTerms.add(rsTraitObo.getString("db_accn"));
                            }
                        }

                        // ////////////
                        // // OUTPUT //
                        // ////////////
                    
                        // create collection directory
                        String path = "../genetic/"+collection;
                        File dir = new File(path);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        if (!dir.exists()) {
                            System.out.println("Directory "+dir.getAbsolutePath()+" not created.");
                            System.exit(1);
                        }

                        // README output
                        System.err.println("##### README."+collection+".yml");
                        File readmeFile = new File(dir, "README."+collection+".yml");
                        PrintWriter readmeWriter = new PrintWriter(readmeFile);
                        readmeWriter.println("---");
                        readmeWriter.println("identifier: "+collection);
                        readmeWriter.println("provenance: SoyBase");
                        readmeWriter.println("source: \"https://soybase.org/\"");
                        readmeWriter.println("synopsis: \"GWAS experiment reported in "+doi+"\"");
                        readmeWriter.println("scientific_name: Glycine max");
                        readmeWriter.println("taxid: 3847");
                        readmeWriter.println("scientific_name_abbrev: glyma");
                        readmeWriter.println("genotype:");
                        readmeWriter.println("  - "+germplasmNumbers.get(experimentId)+" germplasms");
                        readmeWriter.println("genotyping_platform: "+platforms.get(experimentId));
                        readmeWriter.println("description: \"GWAS experiment reported in "+doi+"\"");
                        readmeWriter.println("publication_doi: "+doi);
                        readmeWriter.println("publication_title: \""+title+"\"");
                        readmeWriter.println("contributors: \""+contributors+"\"");
                        readmeWriter.println("public_access_level: public");
                        readmeWriter.println("license: Open");
                        readmeWriter.close();

                        // result.tsv output
                        // 0     1      2
                        // trait marker p-value
                        System.err.println("##### glyma."+collection+".result.tsv");
                        File resultFile = new File(dir, "glyma."+collection+".result.tsv");
                        PrintWriter resultWriter = new PrintWriter(resultFile);
                        for (int id : snpTraits.keySet()) {
                            String line = snpTraits.get(id);
                            line += "\t" + snpNames.get(id);
                            line += "\t" + snpPValues.get(id);
                            resultWriter.println(line);
                        }
                        resultWriter.close();

                        // obo.tsv output
                        System.err.println("##### glyma."+collection+".obo.tsv");
                        File oboFile = new File(dir, "glyma."+collection+".obo.tsv");
                        PrintWriter oboWriter = new PrintWriter(oboFile);
                        for (String trait : traitOboTerms.keySet()) {
                            List<String> oboTerms = traitOboTerms.get(trait);
                            for (String oboTerm : oboTerms) {
                                String line = trait;
                                line += "\t" + oboTerm;
                                oboWriter.println(line);
                            }
                        }
                        oboWriter.close();
                    }                    
                }
            }
            
            // wrap up
            stmt.close();
            conn.close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }
    
}
