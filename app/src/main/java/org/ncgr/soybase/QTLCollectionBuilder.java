package org.ncgr.soybase;

import org.ncgr.crossref.WorksQuery;
import org.ncgr.pubmed.PubMedSummary;

import java.io.File;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Builds QTL collections from the local MySQL copy of SoyBase.
 */
public class QTLCollectionBuilder {

    private static final String dbUrl = "jdbc:mysql://localhost:3306/soybase";
    private static final String dbUsername = "sam";
    private static final String dbPassword = "samspassword";
    private static final String API_KEY = "48cb39fb23bf1190394ccbae4e1d35c5c809";

    static List<String> mapPrefixes = Arrays.asList("GmComposite1999_",
                                                    "GmComposite2003_",
                                                    "GmFeChlorosis2",
                                                    "GmFeChlorosis_",
                                                    "GmRAPD-SIU_",
                                                    "GmRAPD-Sclero_",
                                                    "GmRFLP-CEW2_",
                                                    "GmRFLP-CEW3_",
                                                    "GmRFLP-CEW_",
                                                    "GmRFLP-Chiba2_",
                                                    "GmRFLP-Chiba_",
                                                    "GmRFLP-GA1996a_",
                                                    "GmRFLP-GA1996b_",
                                                    "GmRFLP-GA1998_",
                                                    "GmRFLP-JPT_",
                                                    "GmRFLP-KGL_",
                                                    "GmRFLP-USDAARS-RCS_",
                                                    "GmSCN_",
                                                    "GmSSR-MO_",
                                                    "GmSSR-SIU_",
                                                    "GmSSR-Utah2_",
                                                    "GmSSR-Utah3_",
                                                    "GmSSR-Utah_",
                                                    "GmUSDA1997_",
                                                    "GmUtah1996_");
    
    public static void main(String[] args) {
        try {
            // initialize DB connection and statement
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            Statement stmt = conn.createStatement();

            // query QTL references and their QTL IDs, putting into a map of lists
            // NOTE: this forms the master list of references processed
            Map<String,List<Integer>> referenceQTLIds = new LinkedHashMap<>();
            String queryReference = "SELECT * FROM qtl_reference_table ORDER BY reference";
            ResultSet rsReference = stmt.executeQuery(queryReference);
            while (rsReference.next()) {
                String reference = rsReference.getString("reference");
                int qtlId = rsReference.getInt("qtlid");
                if (referenceQTLIds.containsKey(reference)) {
                    referenceQTLIds.get(reference).add(qtlId);
                } else {
                    List<Integer> ids = new ArrayList<>();
                    ids.add(qtlId);
                    referenceQTLIds.put(reference, ids);
                }
            }

            // get the genotype and parents from the first QTL per stored reference
            // Note: some references are missing parents: use "mixed"
            Map<String,String> referenceGenotypes = new HashMap<>();
            Map<String,String> referenceParentsIds = new HashMap<>();
            for (String reference : referenceQTLIds.keySet()) {
                List<Integer> ids = referenceQTLIds.get(reference);
                int qtlId = ids.get(0);
                String queryParents = "SELECT * FROM qtl_table WHERE qtlid="+qtlId;
                ResultSet rsParents = stmt.executeQuery(queryParents);
                if (rsParents.next()) {
                    String parent1 = rsParents.getString("parent_1");
                    String parent2 = rsParents.getString("parent_2");
                    if (parent1.trim().length()>0 && parent2.trim().length()>0) {
                        String genotype =  parent1+" x "+parent2;
                        String parentsId = parent1.replaceAll("\\.","").replaceAll("\\s","").replaceAll("\\(","").replaceAll("\\)","").replaceAll("#","") +
                            "_x_"+
                            parent2.replaceAll("\\.","").replaceAll("\\s","").replaceAll("\\(","").replaceAll("\\)","").replaceAll("#","");
                        if (parent1.toLowerCase().equals("various") || parent2.toLowerCase().equals("various")) {
                            genotype = "mixed";
                            parentsId = "mixed";
                        }
                        referenceGenotypes.put(reference, genotype);
                        referenceParentsIds.put(reference, parentsId);
                    } else {
                        referenceGenotypes.put(reference, "mixed");
                        referenceParentsIds.put(reference, "mixed");
                    }
                }
            }

            // query each reference to generate collection name and other README content
            // collection identifier = parent1_x_parent2.gen.author1_author2_year
            // first pass: crossref
            // second pass: pubmed
            // if not found: create a partial collection identifier
            Map<String,String> collectionReferences = new HashMap<>(); // for back-editing collection duplicate identifiers
            Set<String> pubMedReferences = new HashSet<>();            // flags that it's a PubMed collection
            Set<String> notFoundReferences = new HashSet<>();          // flags that it wasn't found in CrossRef or PubMed
            Set<String> missingReferences = new HashSet<>();           // flags that reference is missing in paper_table
            Map<String,String> referenceCollections = new HashMap<>();
            Map<String,String> referenceDOIs = new HashMap<>();
            Map<String,String> referenceTitles = new HashMap<>();
            Map<String,String> referenceContributors = new HashMap<>();
            for (String reference : referenceQTLIds.keySet()) {
                boolean crossRefFound = false;
                boolean pubMedFound = false;
                // paper_table query
                String queryPub = "SELECT * FROM paper_table WHERE soybase_id='"+reference+"'";
                ResultSet rsPub = stmt.executeQuery(queryPub);
                if (rsPub.next()) {
                    String doi = rsPub.getString("doi");
                    String title = rsPub.getString("original_title");
                    WorksQuery wq = null;
                    PubMedSummary pms = new PubMedSummary();
                    // CrossRef search
                    if (doi==null || doi.trim().length()==0) {
                        // search for publication on the title
                        wq = new WorksQuery(null, title);
                        crossRefFound = wq.getStatus().equals("ok") && wq.isTitleMatched();
                    } else {
                        // search for publication on the DOI
                        wq = new WorksQuery(doi);
                        crossRefFound = wq.getStatus().equals("ok");
                    }
                    if (!crossRefFound) {
                        // PubMed search
                        if (doi==null || doi.trim().length()==0) {
                            // search for publication on title
                            pms.searchTitle(title, API_KEY);
                        } else {
                            // search for publication on DOI
                            pms.searchDOI(doi, API_KEY);
                        }
                        pubMedFound = (pms.id > 0);
                    }
                    // first half of collection identifier
                    String collection = referenceParentsIds.get(reference)+".gen.";
                    if (crossRefFound) {
                        // CrossRef
                        referenceDOIs.put(reference, wq.getDOI());
                        referenceTitles.put(reference, wq.getTitle().replace('\n',' ').trim().replaceAll(" +", " "));
                        List<String> authorGivenList = new ArrayList<>();
                        List<String> authorFamilyList = new ArrayList<>();
                        for (Object authorObject : wq.getAuthors()) {
                            // {"given":"D. R.","sequence":"first","affiliation":[],"family":"Panthee"}
                            JSONObject authorJSON = (JSONObject) authorObject;
                            authorGivenList.add((String) authorJSON.get("given"));
                            authorFamilyList.add((String) authorJSON.get("family"));
                        }
                        String contributors = "";
                        for (int i=0; i<authorGivenList.size(); i++) {
                            if (i>0) contributors += ", ";
                            contributors += authorGivenList.get(i)+" "+authorFamilyList.get(i);
                        }
                        referenceContributors.put(reference, contributors);
                        int year = 1111;
                        try {
                            year = wq.getJournalIssueYear();
                            if (year==0) year = wq.getIssuedYear();
                        } catch (Exception ex) {
                            // do nothing
                        }
                        // collection identifier
                        collection += authorFamilyList.get(0).replaceAll(" ","-");
                        if (authorFamilyList.size()>1) collection += "_"+authorFamilyList.get(1).replaceAll(" ","-");
                        collection += "_"+year;
                    } else if (pubMedFound) {
                        // PubMed
                        pubMedReferences.add(reference);
                        referenceDOIs.put(reference, pms.doi);
                        referenceTitles.put(reference, pms.title.replace('\n',' ').trim().replaceAll(" +", " "));
                        String contributors = pms.authorList.toString().replaceAll("\\[","").replaceAll("\\]","");
                        referenceContributors.put(reference, contributors);
                        String[] dateParts = pms.pubDate.split(" ");
                        int year = 1111;
                        try {
                            year = Integer.parseInt(dateParts[0]);
                        } catch (NumberFormatException ex) {
                            // do nothing
                        }
                        String[] author1parts = pms.authorList.get(0).split(" ");
                        collection += author1parts[0];
                        if (pms.authorList.size()>1) {
                            String[] author2parts = pms.authorList.get(1).split(" ");
                            collection += "_"+author2parts[0];
                        }
                        collection += "_"+year;
                    } else {
                        // not found
                        notFoundReferences.add(reference);
                        referenceDOIs.put(reference, doi);
                        referenceTitles.put(reference, title);
                        referenceContributors.put(reference, "");
                        collection += "Author1_Author2_YYYY";
                    }
                    // append a, b, c, d if collection identifier is not unique
                    if (collectionReferences.containsKey(collection)) {
                        // append a to previous reference/collection
                        String otherReference = collectionReferences.get(collection);
                        collectionReferences.remove(collection);
                        collectionReferences.put(collection+"a", otherReference);
                        referenceCollections.put(otherReference, collection+"a");
                        // append b to this reference/collection
                        collectionReferences.put(collection+"b", reference);
                        referenceCollections.put(reference, collection+"b");
                    } else if (collectionReferences.containsKey(collection+"b")) {
                        // append c to this reference/collection
                        collectionReferences.put(collection+"c", reference);
                        referenceCollections.put(reference, collection+"c");
                    } else if (collectionReferences.containsKey(collection+"c")) {
                        // append d to this collection
                        collectionReferences.put(collection+"d", reference);
                        referenceCollections.put(reference, collection+"d");
                    } else if (collectionReferences.containsKey(collection+"d")) {
                        // append e to this collection
                        collectionReferences.put(collection+"e", reference);
                        referenceCollections.put(reference, collection+"e");
                    } else {
                        // store this collection/reference as is
                        collectionReferences.put(collection, reference);
                        referenceCollections.put(reference, collection);
                    }
                    if (crossRefFound) {
                        System.out.println("## "+reference+"\tCROSSREF\t"+referenceCollections.get(reference));
                    } else if (pubMedFound) {
                        System.out.println("## "+reference+"\tPUBMED\t"+referenceCollections.get(reference));
                    } else {
                        System.out.println("## "+reference+"\tNOTFOUND\t"+referenceCollections.get(reference));
                    }
                } else {
                    System.out.println("## "+reference+"\tMISSING");
                    missingReferences.add(reference);
                }
            }

            // run through all the references, outputting files
            for (String reference : referenceQTLIds.keySet()) {
                if (missingReferences.contains(reference)) continue;
                String collection = referenceCollections.get(reference);
                String genotype = referenceGenotypes.get(reference);
                String parentsId = referenceParentsIds.get(reference);
                String doi = referenceDOIs.get(reference);
                String title = referenceTitles.get(reference);
                String contributors = referenceContributors.get(reference);

                // get QTL info from qtl_table for this collection
                List<Integer> qtlIds = referenceQTLIds.get(reference);
                Map<Integer,String> qtlNames = new HashMap<>();
                Map<Integer,String> qtlTraits = new HashMap<>();
                Map<Integer,Double> qtlLODs = new HashMap<>();
                Map<Integer,Double> qtlMarkerR2s = new HashMap<>();
                Map<Integer,Double> qtlTotalR2s = new HashMap<>();
                Map<String,List<String>> traitOboTerms = new HashMap<>();
                Set<String> traits = new HashSet<>();
                for (int id : qtlIds) {
                    String queryQTL = "SELECT * FROM qtl_table WHERE qtlid="+id;
                    ResultSet rsQTL = stmt.executeQuery(queryQTL);
                    if (rsQTL.next()) {
                        qtlNames.put(id, rsQTL.getString("qtlname"));
                        qtlTraits.put(id, rsQTL.getString("qtl_class"));
                        try {
                            double lod = Double.parseDouble(rsQTL.getString("interval_lod_score"));
                            qtlLODs.put(id, lod);
                        } catch (Exception ex) {
                        }
                        try {
                            double markerR2 = Double.parseDouble(rsQTL.getString("genotypic_r2"));
                            qtlMarkerR2s.put(id, markerR2);
                        } catch (Exception ex) {
                        }
                        try {
                            double totalR2 = Double.parseDouble(rsQTL.getString("interval_r2"));
                            qtlTotalR2s.put(id, totalR2);
                        } catch (Exception ex) {
                        }
                        // these are just SOY OBO terms
                        List<String> oboTerms = new ArrayList<>();
                        oboTerms.add(rsQTL.getString("traitname"));
                        traitOboTerms.put(rsQTL.getString("qtl_class"), oboTerms);
                        // unique trait set
                        traits.add(rsQTL.getString("qtl_class"));
                    }
                }

                // get QTL locations on linkage groups from qtl_position_table
                // | QTLID | QTLName            | MapName               | LeftEnd    | RightEnd | LG      | Centroid |
                // +-------+--------------------+-----------------------+------------+----------+---------+----------+
                // |   823 | Seed weight 3-4    | GmComposite1999_K     |  125.10000 |   127.10 | K       |   126.00 |
                Set<String> linkageGroups = new HashSet<>();
                Map<Integer,String> qtlLinkageGroups = new HashMap<>();
                Map<Integer,Double> qtlStarts = new HashMap<>();
                Map<Integer,Double> qtlEnds = new HashMap<>();
                Map<Integer,Double> qtlPeaks = new HashMap<>();
                for (int id : qtlIds) {
                    String queryQTL = "SELECT * FROM qtl_position_table WHERE qtlid="+id;
                    ResultSet rsQTL = stmt.executeQuery(queryQTL);
                    if (rsQTL.next()) {
                        linkageGroups.add(rsQTL.getString("mapname"));
                        qtlLinkageGroups.put(id, rsQTL.getString("mapname"));
                        qtlStarts.put(id, rsQTL.getDouble("leftend"));
                        qtlEnds.put(id, rsQTL.getDouble("rightend"));
                        qtlPeaks.put(id, rsQTL.getDouble("centroid"));
                    }
                }

                // get the genetic map name(s) from the linkage groups, display error if none found
                Set<String> geneticMaps = new HashSet<>();
                for (String linkageGroup : linkageGroups) {
                    for (String mapPrefix : mapPrefixes) {
                        if (linkageGroup.startsWith(mapPrefix)) {
                            geneticMaps.add(mapPrefix.replaceAll("_", ""));
                        }
                    }
                }
                if (geneticMaps.size()==0) {
                    System.err.println("ERROR: genetic map not found for "+collection+" linkage groups:");
                    System.err.println(linkageGroups);
                }
                    
                // get each QTL's associated markers from qtl_locus_table for qtlmrk.tsv
                Map<Integer,List<String>> qtlMarkers = new HashMap<>(); // keyed by qtl ID
                for (int id : qtlIds) {
                    String queryMarkers = "SELECT * FROM qtl_locus_table WHERE qtlid="+id;
                    ResultSet rsMarkers = stmt.executeQuery(queryMarkers);
                    List<String> markers = new ArrayList<>();
                    while (rsMarkers.next()) {
                        markers.add(rsMarkers.getString("locusname"));
                    }
                    qtlMarkers.put(id, markers);
                }

                // get QTL marker positions on the QTL linkage groups
                Map<String,String> markerLinkageGroups = new HashMap<>();
                Map<String,Double> markerPositions = new HashMap<>();
                for (int id : qtlIds) {
                    for (String marker : qtlMarkers.get(id)) {
                        String linkageGroup = qtlLinkageGroups.get(id);
                        String queryMarker = "SELECT * FROM locus_map_table WHERE locusname='"+marker+"' AND mapname='"+linkageGroup+"'";
                        ResultSet rsMarker = stmt.executeQuery(queryMarker);
                        if (rsMarker.next()) {
                            try {
                                double position = rsMarker.getDouble("position");
                                markerLinkageGroups.put(marker, linkageGroup);
                                markerPositions.put(marker, position);
                            } catch (Exception ex) {
                            }
                        }
                    }
                }

                // // get maximum marker position from locus_map_table as linkage group length for these linkage groups for lg.tsv
                // Map<String,Double> linkageGroupLengths = new HashMap<>();
                // for (String linkageGroup : linkageGroups) {
                //     String queryLG = "SELECT max(convert(position,DECIMAL(6,2))) AS length FROM locus_map_table WHERE mapname='"+linkageGroup+"'";
                //     ResultSet rsLG = stmt.executeQuery(queryLG);
                //     if (rsLG.next()) {
                //         double length = rsLG.getDouble("length");
                //         linkageGroupLengths.put(linkageGroup, length);
                //     }
                // }

                // get additional OBO terms from trait_xref_table for obo.tsv
                for (String trait : traits) {
                    String queryTraitObo = "SELECT * FROM trait_xref_table WHERE traitname='"+trait+"'";
                    ResultSet rsTraitObo = stmt.executeQuery(queryTraitObo);
                    List<String> oboTerms = traitOboTerms.get(trait);
                    while (rsTraitObo.next()) {
                        oboTerms.add(rsTraitObo.getString("db_accn"));
                    }
                }
                    
                ////////////
                // OUTPUT //
                ////////////
                    
                // create collection directory
                String path = null;
                if (notFoundReferences.contains(reference)) {
                    path = "../notfound/"+collection;
                } else if (pubMedReferences.contains(reference)) {
                    path = "../pubmed/"+collection;
                } else {
                    path = "../crossref/"+collection;
                }
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                if (!dir.exists()) {
                    System.out.println("Directory "+dir.getAbsolutePath()+" not created.");
                    System.exit(1);
                }

                // README output
                System.out.println("#### README."+collection+".yml");
                File readmeFile = new File(dir, "README."+collection+".yml");
                PrintWriter readmeWriter = new PrintWriter(readmeFile);
                readmeWriter.println("---");
                readmeWriter.println("identifier: "+collection);
                String geneticMapString = "";
                for (String geneticMap : geneticMaps) {
                    if (geneticMapString.length()>0) geneticMapString += ",";
                    geneticMapString += geneticMap;
                }
                readmeWriter.println("genetic_map: "+geneticMapString);
                readmeWriter.println("provenance: SoyBase");
                readmeWriter.println("source: \"https://soybase.org/\"");
                readmeWriter.println("synopsis: \""+title+"\"");
                readmeWriter.println("scientific_name: Glycine max");
                readmeWriter.println("taxid: 3847");
                readmeWriter.println("scientific_name_abbrev: glyma");
                readmeWriter.println("genotype:");
                readmeWriter.println("  - "+genotype);
                readmeWriter.println("description: \"Further information provided in "+doi+"\"");
                readmeWriter.println("publication_doi: "+doi);
                readmeWriter.println("publication_title: \""+title+"\"");
                readmeWriter.println("contributors: \""+contributors+"\"");
                readmeWriter.println("public_access_level: public");
                readmeWriter.println("license: Open");
                readmeWriter.close();
                
                // qtl.tsv output
                // 0              1          2             3        4      5       6                     7   8                9         10       11
                // QTL_identifier trait_name linkage_group start_cM end_cM peak_cM favored_allele_source LOD likelihood_ratio marker_r2 total_r2 additivity
                System.out.println("##### glyma."+collection+".qtl.tsv");
                File qtlFile = new File(dir, "glyma."+collection+".qtl.tsv");
                PrintWriter qtlWriter = new PrintWriter(qtlFile);
                for (int id : qtlIds) {
                    String line = qtlNames.get(id);                                                     // 0
                    line += "\t"; if (qtlTraits.get(id)!=null) line += qtlTraits.get(id);               // 1
                    line += "\t"; if (qtlLinkageGroups.get(id)!=null) line += qtlLinkageGroups.get(id); // 2
                    line += "\t"; if (qtlStarts.get(id)!=null) line += qtlStarts.get(id);               // 3
                    line += "\t"; if (qtlEnds.get(id)!=null) line += qtlEnds.get(id);                   // 4
                    line += "\t"; if (qtlPeaks.get(id)!=null) line += qtlPeaks.get(id);                 // 5
                    line += "\t";                                                                       // 6
                    line += "\t"; if (qtlLODs.get(id)!=null) line += qtlLODs.get(id);                   // 7
                    line += "\t";                                                                       // 8
                    line += "\t"; if (qtlMarkerR2s.get(id)!=null) line += qtlMarkerR2s.get(id);         // 9
                    line += "\t"; if (qtlTotalR2s.get(id)!=null) line += qtlTotalR2s.get(id);           // 10
                    line += "\t";                                                                       // 11
                    qtlWriter.println(line);
                }
                qtlWriter.close();
                    
                // qtlmrk.tsv output (if QTL markers AND traits AND linkage groups)
                if (qtlMarkers.size()>0) {
                    System.out.println("##### glyma."+collection+".qtlmrk.tsv");
                    File qtlmrkFile = new File(dir, "glyma."+collection+".qtlmrk.tsv");
                    PrintWriter qtlmrkWriter = new PrintWriter(qtlmrkFile);
                    for (int id : qtlMarkers.keySet()) {
                        if (qtlTraits.get(id)!=null && qtlLinkageGroups.get(id)!=null) {
                            String qtlName = qtlNames.get(id);
                            List<String> markers = qtlMarkers.get(id);
                            for (String marker : markers) {
                                String line = qtlName;                          // 0
                                line += "\t" + qtlTraits.get(id);               // 1
                                line += "\t" + marker;                          // 2
                                line += "\t" + qtlLinkageGroups.get(id);        // 3
                                qtlmrkWriter.println(line);
                            }
                        }
                    }
                    qtlmrkWriter.close();
                }
                    
                // // mrk.tsv output (if markers)
                // if (markerLinkageGroups.size()>0) {
                //     System.out.println("##### glyma."+collection+".mrk.tsv");
                //     File mrkFile = new File(dir, "glyma."+collection+".mrk.tsv");
                //     PrintWriter mrkWriter = new PrintWriter(mrkFile);
                //     for (String marker : markerLinkageGroups.keySet()) {
                //         String line = marker;
                //         line += "\t" + markerLinkageGroups.get(marker);
                //         line += "\t" + markerPositions.get(marker);
                //         mrkWriter.println(line);
                //     }
                //     mrkWriter.close();
                // }

                // // lg.tsv output (if LGs)
                // if (linkageGroupLengths.size()>0) {
                //     System.out.println("##### glyma."+collection+".lg.tsv");
                //     File lgFile = new File(dir, "glyma."+collection+".lg.tsv");
                //     PrintWriter lgWriter = new PrintWriter(lgFile);
                //     for (String linkageGroup : linkageGroupLengths.keySet()) {
                //         String line = linkageGroup;
                //         line += "\t"+linkageGroupLengths.get(linkageGroup);
                //         lgWriter.println(line);
                //     }
                //     lgWriter.close();
                // }

                // obo.tsv output (if OBO terms)
                if (traitOboTerms.size()>0) {
                    System.out.println("##### glyma."+collection+".obo.tsv");
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
            
            // wrap up
            stmt.close();
            conn.close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }

}
