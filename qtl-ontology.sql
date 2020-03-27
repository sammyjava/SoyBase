--
-- Directly query the SoyBase MySQL DB for QTLs and ontology terms
--

SELECT qtl_table.QTLName,
       trait_xref_table.db_accn AS OntologyIdentifier
       
FROM qtl_table,
     trait_table,
     trait_xref_table

WHERE qtl_table.TraitName=trait_table.SOY_accn
AND   trait_table.TraitName=trait_xref_table.TraitName
AND   (trait_xref_table.db_id='TO' OR trait_xref_table.db_id='PO' OR trait_xref_table.db_id='GO')

ORDER BY QTLName,OntologyIdentifier
;
