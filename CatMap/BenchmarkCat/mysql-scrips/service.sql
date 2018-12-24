CREATE TABLE IF NOT EXISTS service_benchmark (
  uid BINARY(16) PRIMARY KEY,
  start_exec BIGINT NOT NULL,
  end_exec BIGINT NOT NULL,
  elapsed INT NOT NULL,
  cat_source TEXT(5240) NOT NULL,
  mapping_name TEXT(5240)NOT NULL,
  exportID VARCHAR(255), INDEX(exportID),
  records_size BIGINT NOT NULL
) ENGINE=InnoDB;


DROP TRIGGER IF EXISTS servcie_auto_id;
CREATE TRIGGER servcie_auto_id 
  BEFORE INSERT ON service_benchmark
  FOR EACH ROW
  SET new.uid = UUID_TO_BIN(UUID());

    
LOAD DATA INFILE '/tmp/nl.uva.sne.vre4eic.model.ExportDocTask.csv' 
INTO TABLE service_benchmark 
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(@start,@end,@cat_source,@mapping_name,@exportID,@records_size)
SET start_exec = @start, 
    end_exec = @end,
    elapsed = ((@end - @start)),
    cat_source=@cat_source,
    mapping_name=@mapping_name,
    exportID=@exportID,
    records_size=@records_size;