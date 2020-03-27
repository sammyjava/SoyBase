--
-- dump the markers, types and genomic location information for markers that are mapped to the v2 assembly
--

SELECT locus_table.LocusName AS primaryIdentifier,
       locus_table.LocusName AS secondaryIdentifier,
       locus_table.TYPE AS type,
       locus_chromosome_table.Chromosome AS chromosome,
       locus_chromosome_table.Start_bp AS start,
       locus_chromosome_table.End_bp AS end,
       locus_chromosome_table.Motif AS motif

FROM   locus_table, locus_chromosome_table

WHERE locus_chromosome_table.LocusName=locus_table.LocusName
AND   locus_chromosome_table.Assembly='Glyma2.0'

ORDER BY Chromosome,Start,End,primaryIdentifier;
