#!/bin/sh
# +---------+-----------+--------------+----------+
# | LocusID | LocusName | MapName      | Position |
# +---------+-----------+--------------+----------+
# |    1042 | ACTAGA176 | GmAFLP-NAU_L | 55.43    |
# +---------+-----------+--------------+----------+

mysql soybase -e "SELECT LocusName AS marker, MapName AS linkage_group, convert(Position,DECIMAL(6,2)) AS position FROM locus_map_table 
WHERE mapname LIKE '${1}_%' ORDER BY linkage_group,marker"

