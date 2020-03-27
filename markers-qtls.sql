--
-- dump the markers (locus) and QTLs
--

SELECT LocusName, QTLName
FROM   qtl_locus_table
ORDER BY LocusName, QTLName
