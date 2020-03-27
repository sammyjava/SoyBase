DROP TABLE gwaspubs;
CREATE TABLE gwaspubs (
       experiment_id  int,
       name           text,
       author_year    text,
       title          text,
       journal        text,
       doi            text,
       url            text
       );

COPY gwaspubs FROM '/home/shokin/SoyBase/GWAS-pubs.tsv';
