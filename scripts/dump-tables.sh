#!/bin/sh
for tablename in qtl_table qtl_reference_table qtl_locus_table trait_table trait_xref_table locus_table locus_chromosome_table locus_map_table paper_table
do
    echo $tablename
    mysqldump -u shokin --socket /tmp/mysql.sock --single-transaction soybase $tablename > $tablename.dmp
done

