import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Split a big QTL tab-delimited file into separate files organized by publication and mapping population with two columns for QTL and trait name. Output:
 * 
 * TaxonID           3847
 * MappingPopulation Toyoharuka_x_Toyomusume
 * Title             Variation of GmIRCHS (Glycine max inverted-repeat CHS pseudogene) is related to tolerance of low temperature-induced seed coat discoloration in yellow soybean
 * Journal
 * Year
 * Volume
 * Page
 * #QTLName	TraitName
 * Blah 1-1     Blah
 *
 * @author Sam Hokin
 */

public class QTLFileSplitter {

    public static void main(String[] args) throws IOException, FileNotFoundException {

        if (args.length!=2) {
            System.err.println("Usage: QTLFileSplitter <tab-delimited file name> <taxonId>");
            System.exit(1);
        }

        String filename = args[0];
        int taxonId = Integer.parseInt(args[1]);

        System.out.println("Processing "+filename+" for organism "+taxonId);

        Map<String,MappingPopulation> mappingPopulationMap = new LinkedHashMap<String,MappingPopulation>();
        Map<String,Set<QTL>> qtlMap = new LinkedHashMap<String,Set<QTL>>();
        Map<String,Pub> pubMap = new LinkedHashMap<String,Pub>();

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line=reader.readLine())!=null) {
            if (!line.startsWith("#")) {
                // parse the line
                String[] parts = line.split("\t");
                String qtlName = parts[0].trim();
                String parent1 = parts[1].trim().replace(" ",""); // deal with space inconsistencies
                String parent2 = parts[2].trim().replace(" ",""); 
                String traitName = parts[3].trim();
                String journal = parts[4].trim();  // any of these could be "NULL" or empty from SoyBase export
                String year = parts[5].trim();
                String volume = parts[6].trim();
                String page = parts[7].trim();
                String title = parts[8].trim();
                // mapping population
                MappingPopulation mappingPopulation = new MappingPopulation(taxonId, parent1, parent2);
                String mappingPopulationKey = mappingPopulation.getKey();
                if (mappingPopulationMap.containsKey(mappingPopulationKey)) {
                    // reset to the actual object in the map
                    mappingPopulation = mappingPopulationMap.get(mappingPopulationKey);
                } else {
                    // put new one into the map
                    mappingPopulationMap.put(mappingPopulationKey, mappingPopulation);
                }
                // publication
                Pub pub = new Pub(mappingPopulation, journal, year, volume, page, title);
                String pubKey = pub.getKey();
                if (pubMap.containsKey(pubKey)) {
                    // reset to the actual object in the map
                    pub = pubMap.get(pubKey);
                } else {
                    // put new one into the map
                    pubMap.put(pubKey, pub);
                }
                // QTL
                QTL qtl = new QTL(qtlName,traitName);
                if (qtlMap.containsKey(pubKey)) {
                    Set<QTL> qtls = qtlMap.get(pubKey);
                    qtls.add(qtl);
                } else {
                    Set<QTL> qtls = new LinkedHashSet<QTL>();
                    qtls.add(qtl);
                    qtlMap.put(pubKey, qtls);
                }
            }
        }

        // spit 'em out to separate files named by publication key
        for (String pubKey : pubMap.keySet()) {
            Pub pub = pubMap.get(pubKey);
            MappingPopulation mappingPopulation = pub.mappingPopulation;
            Set<QTL> qtls = qtlMap.get(pubKey);
            String tsvname = pubKey+".tsv";
            FileWriter fw = new FileWriter("QTL/"+tsvname);
            System.out.println(tsvname);
            fw.write(mappingPopulation.toString());
            fw.write(pub.toString());
            for (QTL qtl : qtls) {
                fw.write(qtl.toString()+"\n");
            }
            fw.close();
        }

    }
}
