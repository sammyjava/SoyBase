--
-- Directly query the SoyBase MySQL DB for QTLs and pub data
--

SELECT qtl_table.QTLName, qtl_table.Parent_1, qtl_table.Parent_2,
       trait_table.TraitName,
       paper_table.Original_title, paper_table.Journal, paper_table.Year, paper_table.Volume, paper_table.Page
       
FROM qtl_table,
     trait_table,
     qtl_reference_table,
     paper_table

WHERE   qtl_table.TraitName=trait_table.SOY_accn
AND     qtl_table.QTLName=qtl_reference_table.QTLName
AND     qtl_reference_table.Reference=paper_table.Soybase_ID

ORDER BY Original_title,QTLName
;
