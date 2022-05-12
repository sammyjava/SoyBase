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
