#!/bin/sh
## GWASResults
echo "SELECT gwas_name AS identifier, gwas_class AS phenotype, snp_name AS marker, p_value AS pvalue FROM gwas_qtl WHERE experiment_id=$1 AND trim(p_value)!='' ORDER BY gwas_class,snp_name;" | mysql --batch -u _www -S /tmp/mysql.sock soybase > GWAS/gwas-result-$1.tsv
sed -i s/identifier/#identifier/ GWAS/gwas-result-$1.tsv

sed -i s/x10^\(/E-/ GWAS/gwas-result-$1.tsv
sed -i s/\)//      GWAS/gwas-result-$1.tsv
sed -i s/___/E/    GWAS/gwas-result-$1.tsv
sed -i s/10_/-/    GWAS/gwas-result-$1.tsv
