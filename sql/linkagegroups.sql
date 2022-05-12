--
-- dump the map names; these will be used to load linkage groups and genetic maps
--

SELECT DISTINCT locus_map_table.MapName
FROM   locus_map_table
ORDER BY MapName;
