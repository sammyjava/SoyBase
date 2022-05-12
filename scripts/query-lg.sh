#!/bin/sh
mysql soybase -e "SELECT mapname AS linkage_group,max(convert(position,DECIMAL(6,2))) AS length FROM locus_map_table 
WHERE mapname LIKE '${1}_%' GROUP BY linkage_group ORDER BY linkage_group"

