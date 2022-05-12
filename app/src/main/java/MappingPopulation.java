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
