--
-- Directly query the SoyBase MySQL DB for distinct QTL paper titles for later loading into one file per paper.
--

SELECT DISTINCT paper_table.Original_title
       
FROM qtl_table,
     qtl_reference_table,
     paper_table

WHERE qtl_table.QTLName=qtl_reference_table.QTLName
AND   qtl_reference_table.Reference=paper_table.Soybase_ID

ORDER BY Original_title
;
