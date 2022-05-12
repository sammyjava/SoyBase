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
