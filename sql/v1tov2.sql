--
-- convert v1 "Sat%" markers to v2 genomic positions from corresponding 'BARC%' marker
--

SELECT a.LocusName AS primaryIdentifier, b.LocusName AS secondaryIdentifier,
       locus_table.TYPE,
       c.Chromosome, c.Start_bp AS Start, c.End_bp AS End, a.Motif, c.Assembly

FROM locus_chromosome_table a, locus_chromosome_table b, locus_chromosome_table c, locus_table

WHERE a.LocusName LIKE 'S%'
AND   b.LocusName LIKE 'BARC%'
AND   a.Chromosome=b.Chromosome AND a.Start_bp=b.Start_bp AND a.End_bp=b.End_bp
AND   b.LocusName=c.LocusName
AND   c.Assembly='Glyma2.0'
AND   a.LocusName=locus_table.LocusName

ORDER BY Chromosome,Start,End
