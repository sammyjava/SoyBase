--
-- dump the markers and map names
--

SELECT locus_map_table.LocusName, locus_map_table.MapName, locus_map_table.Position
FROM   locus_map_table
ORDER BY MapName, Position;
