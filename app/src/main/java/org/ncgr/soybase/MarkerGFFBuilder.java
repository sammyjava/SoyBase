package org.ncgr.soybase;

import org.ncgr.crossref.WorksQuery;

import java.io.File;
import java.io.PrintWriter;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Output a GFF files for markers from the local MySQL copy of SoyBase for a supplied marker prefix and assembly.
 */
public class MarkerGFFBuilder {

    static String dbUrl = "jdbc:mysql://localhost:3306/soybase";
    static String dbUsername = "sam";
    static String dbPassword = "samspassword";

    public static void main(String[] args) {
        // usage is 
        if (args.length!=2) {
            System.err.println("Usage: MarkerGFFBuilder marker_prefix gnm1|gnm2");
            System.exit(1);
        }
        String markerPrefix = args[0];
        String gnm = args[1];
        String assembly = null;
        if (gnm.equals("gnm1")) {
            assembly = "Glyma 1.0";
        } else if (gnm.equals("gnm2")) {
            assembly = "Glyma2.0";
        } else {
            System.err.println("Supplied gnm="+gnm+". The requested genome assembly can only be gnm1 or gnm2.");
            System.exit(1);
        }
        String yuckPrefix = "glyma.Wm82."+gnm+".";
        
        try {
            // initialize DB connection and statement
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            Statement stmt = conn.createStatement();
            // load data into maps keyed by name
            Map<String,String> chromosomes = new LinkedHashMap<>(); // preserve order
            Map<String,Integer> starts = new HashMap<>();
            Map<String,Integer> ends = new HashMap<>();
            Map<String,String> motifs = new HashMap<>();
            // the query for the supplied marker prefix
            String query = "SELECT * FROM locus_chromosome_table WHERE LocusName LIKE '"+markerPrefix+"%' and Assembly='"+assembly+"' ORDER BY Chromosome,Start_bp";
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                // LocusID LocusName Chromosome Start_bp End_bp Motif Germplasm_of_sequence Polymorphic_in_8_cultivars_set_1 Assembly
                String name = rs.getString("LocusName");
                chromosomes.put(name, rs.getString("Chromosome"));
                starts.put(name, rs.getInt("Start_bp"));
                ends.put(name, rs.getInt("End_bp"));
                motifs.put(name, rs.getString("Motif"));
            }

            System.err.println("## FOUND "+chromosomes.size()+" RECORDS.");

            for (String name : chromosomes.keySet()) {
                String chromosome = chromosomes.get(name);
                int start = starts.get(name);
                int end = ends.get(name);
                String motif = motifs.get(name);
                String seqname = yuckPrefix+chromosome;
                String id = yuckPrefix+name;
                // GFF line
                // seqname   source   featureType   start   end   score   strand   frame   attributes
                String line = seqname+"\t"+"SoyBase"+"\t"+"genetic_marker"+"\t"+start+"\t"+end+"\t"+"."+"\t"+"."+"\t"+"."+"\t"+"ID="+id+";Name="+name;
                if (motif!=null) line += ";motif="+motif;
                // query aliases
                String queryAlias = "SELECT LocusName FROM locus_chromosome_table " +
                    "WHERE Assembly='"+assembly+"' AND Chromosome='"+chromosome+"' AND Start_bp="+start+" AND End_bp="+end+" AND LocusName!='"+name+"'";
                ResultSet rsAlias = stmt.executeQuery(queryAlias);
                String alias = "";
                while (rsAlias.next()) {
                    if (alias.length()>0) alias += ",";
                    alias += rsAlias.getString("LocusName");
                }
                if (alias.length()>0) line += ";alias="+alias;
                // OUTPUT
                System.out.println(line);
            }
            
            // wrap up
            stmt.close();
            conn.close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }
}
