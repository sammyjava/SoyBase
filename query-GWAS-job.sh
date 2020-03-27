#!/bin/bash
for i in {1..56}
do
    echo $i
    ./query-GWAS.sh  $i
    ./query-GWAS-results.sh $i
done
