#!/bin/sh
## GWAS phenotypes
echo "SELECT DISTINCT gwas_class AS phenotype, trait_SOY_number AS ontologyId FROM gwas_qtl WHERE trait_SOY_number LIKE '%:%';" | mysql --batch -u _www -S /tmp/mysql.sock soybase > GWAS/gwas-phenotypes.tsv
sed -i s/phenotype/#phenotype/ GWAS/gwas-phenotypes.tsv
