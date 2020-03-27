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

/**
 * Encapsulate a mapping population.
 */
class MappingPopulation {

    int taxonId;
    String parent1;
    String parent2;

    public MappingPopulation(int taxonId, String parent1, String parent2) {
        this.taxonId = taxonId;
        this.parent1 = parent1;
        this.parent2 = parent2;
    }

    public String toString() {
        String output = "";
        output += "TaxonID\t"+taxonId+"\n";
        output += "MappingPopulation\t"+getKey()+"\n";
        return output;
    }

    // form mapping population key from parents
    public String getKey() {
        return parent1.replace(" ","")+"_x_"+parent2.replace(" ","");
    }

}

/**
 * Encapsulate a QTL.
 */
class QTL {
    
    String name;
    String trait;
    
    public QTL(String name, String trait) {
        this.name = name;
        this.trait = trait;
    }
    
    public String toString() {
        return name+"\t"+trait;
    }
    
}

/**
 * Encapsulate a publication.
 */
class Pub {

    MappingPopulation mappingPopulation;

    // these are null if not present in the file
    String journal;
    String year;
    String volume;
    String pages;
    String title;

    public Pub(MappingPopulation mappingPopulation, String journal, String year, String volume, String pages, String title) {
        this.mappingPopulation = mappingPopulation;
        if (journal!=null && journal.length()>0 && !journal.equals("NULL")) this.journal = journal;
        if (year!=null && year.length()>0 && !year.equals("NULL")) this.year = year;
        if (volume!=null && volume.length()>0 && !volume.equals("NULL")) this.volume = volume;
        if (pages!=null && pages.length()>0 && !pages.equals("NULL")) this.pages = pages;
        if (title!=null && title.length()>0 && !title.equals("NULL")) this.title = title;
    }

    public String toString() {
        String output = "";
        if (journal!=null) output += "Journal\t"+journal+"\n";
        if (year!=null) output += "Year\t"+year+"\n";
        if (volume!=null) output += "Volume\t"+volume+"\n";
        if (pages!=null) output += "Pages\t"+pages+"\n";
        if (title!=null) output += "Title\t"+title+"\n";
        return output;
    }

    public String getKey() {
        String key = "";
        if (year!=null) key += year;
        if (journal!=null) key += clean(journal);
        if (volume!=null) key += clean(volume);
        if (pages!=null) key += clean(pages);
        if (title!=null) key += clean(title);
        return key;
    }

    String clean(String dirty) {
        return dirty.replace(" ","_").replace("/","_").replace("'","").replace("(","").replace(")","").replace("[","").replace("]","").replace("&","").replace(",","");
    }

}
    
