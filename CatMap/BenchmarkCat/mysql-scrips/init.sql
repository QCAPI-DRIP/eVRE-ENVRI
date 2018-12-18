SET GLOBAL sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''));
source main.sql;
source service.sql;
source worker.sql;
   
