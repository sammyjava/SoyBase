--
-- load the GRIN tab-delimited line data
--
DROP TABLE grin_soybean;
DROP TABLE grin_soybean_lines;

CREATE TABLE grin_soybean (
	accession_prefix	text,
	accession_number	integer,
	observation_value	text,
	descriptor_name		text,
	accession_suffix	text,
	origin			text,
	accession_comment	text
);

COPY grin_soybean FROM '/home/shokin/Soybase/GRIN/gmax_grin_eval_data.tsv' WITH CSV HEADER DELIMITER E'\t';

ALTER TABLE grin_soybean ADD COLUMN accession text;
UPDATE grin_soybean SET accession=accession_prefix||accession_number;
UPDATE grin_soybean SET accession=accession||accession_suffix WHERE accession_suffix IS NOT NULL;
UPDATE grin_soybean SET accession=replace(accession,'-','.');

SELECT DISTINCT accession,origin,accession_comment INTO grin_soybean_lines FROM grin_soybean;
UPDATE grin_soybean_lines SET origin=NULL WHERE origin='Unknown';

COPY grin_soybean_lines TO '/tmp/grin_soybean_lines.tsv' WITH CSV DELIMITER E'\t';
COPY grin_soybean (accession,descriptor_name,observation_value) TO '/tmp/grin_soybean_phenotypes.tsv' WITH CSV DELIMITER E'\t';
