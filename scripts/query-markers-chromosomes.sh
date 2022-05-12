#!/bin/sh
echo "SELECT locus_table.LocusName AS primaryIdentifier, '' AS secondaryIdentifier, locus_table.TYPE AS type, locus_chromosome_table.Chromosome AS chromosome, locus_chromosome_table.Start_bp AS start, locus_chromosome_table.End_bp AS end FROM locus_table, locus_chromosome_table WHERE locus_chromosome_table.LocusName=locus_table.LocusName AND locus_chromosome_table.Assembly='Glyma2.0' ORDER BY locus_table.LocusName;" | mysql --batch -u _www -S /tmp/mysql.sock soybase > markers-chromosomes.tsv

sed -i s/primaryIdentifier/#primaryIdentifier/ markers-chromosomes.tsv

