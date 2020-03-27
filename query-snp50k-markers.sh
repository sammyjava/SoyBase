#!/bin/sh
# grab the v2 positions of all SNP50k markers
echo "SELECT dbSNP_ID AS primaryIdentifier, snp_name AS secondaryIdentifier, 'SNP' AS type, chromosome, snp_start_bp AS 'start', snp_end_bp AS 'end' FROM snp50k_sequence_position WHERE chromosome IS NOT NULL AND coordinate_system='Glyma2.0' ORDER BY dbSNP_ID" | mysql --batch -u _www -S /tmp/mysql.sock soybase >> markers-snp50k.tsv
