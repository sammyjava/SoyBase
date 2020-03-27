--
-- Directly query the SoyBase MySQL DB for QTLs, ontology terms and publication data
--

SELECT qtl_table.QTLName, qtl_table.Parent_1, qtl_table.Parent_2,
       trait_table.TraitName, trait_table.SOY_accn
--       trait_xref_table.db_accn AS OntologyIdentifier
--       paper_table.Original_title, paper_table.Journal, paper_table.Year, paper_table.Volume, paper_table.Page
       
FROM qtl_table,
     trait_table
--     trait_xref_table,
--     qtl_reference_table,
--     paper_table,

WHERE  qtl_table.TraitName=trait_table.SOY_accn
-- AND    trait_table.TraitName=trait_xref_table.TraitName
-- AND    (trait_xref_table.db_id='TO' OR trait_xref_table.db_id='PO')
-- AND    qtl_table.QTLName=qtl_reference_table.QTLName
-- AND    qtl_reference_table.Reference=paper_table.Soybase_ID

ORDER BY QTLName
;
