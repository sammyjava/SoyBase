#!/bin/sh
echo "TaxonID	3847" > GWAS/gwas-$1.tsv
echo "Strain	Williams82" >> GWAS/gwas-$1.tsv
echo "SELECT SoyBase_ID AS Name FROM gwas_experiment WHERE experiment_id=$1" | mysql --batch --vertical -u _www -S /tmp/mysql.sock soybase >> GWAS/gwas-$1.tsv
echo "SELECT platform_name AS PlatformName FROM gwas_experiment WHERE experiment_id=$1" | mysql --batch --vertical -u _www -S /tmp/mysql.sock soybase >> GWAS/gwas-$1.tsv
echo "SELECT platform_details AS PlatformDetails FROM gwas_experiment WHERE experiment_id=$1" | mysql --batch --vertical -u _www -S /tmp/mysql.sock soybase >> GWAS/gwas-$1.tsv
## stored locally
psql -At -c "SELECT 'DOI',doi FROM gwaspubs WHERE experiment_id=$1" >> GWAS/gwas-$1.tsv

sed -i s/\:\ /\	/  GWAS/gwas-$1.tsv
sed -i s/\|/\	/  GWAS/gwas-$1.tsv
sed -i s/\*//g     GWAS/gwas-$1.tsv
sed -i s/\ //g     GWAS/gwas-$1.tsv
sed -i /1\.row/d  GWAS/gwas-$1.tsv

# echo "SELECT number_loci_tested AS NumberLociTested FROM gwas_experiment WHERE experiment_id=$1" | mysql --batch -u _www -S /tmp/mysql.sock soybase >> GWAS/gwas-$1.tsv
# echo "SELECT number_germplasm_tested AS NumberGermplasmTested FROM gwas_experiment WHERE experiment_id=$1" | mysql --batch -u _www -S /tmp/mysql.sock soybase >> GWAS/gwas-$1.tsv
# echo "SELECT sequence_coordinate_system AS Assembly FROM gwas_experiment WHERE experiment_id=$1" | mysql --batch -u _www -S /tmp/mysql.sock soybase >> GWAS/gwas-$1.tsv

# gwas_experiment
# ---------------
# experiment_id
# SoyBase_ID
# platform_name
# platform_details
# number_loci_tested
# number_germplasm_tested
# sequence_coordinate_system
#
# 1
# KGK20170714.1
# SoySNP50k
# Illumina Infinium BeadChip
# 52,041
# 12,116
# Wm82.a2.v1

# TaxonID               3847
# Name                  KGK20170714.1
# PlatformName          SoySNP50k
# PlatformDetails       Illumina Infinium BeadChip
# NumberLociTested      52041
# NumberGermplasmTested 12116
# DOI                   10.3835/plantgenome2015.04.0024
