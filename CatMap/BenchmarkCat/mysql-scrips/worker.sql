CREATE TABLE IF NOT EXISTS worker_benchmark (
  uid BINARY(16) PRIMARY KEY,
  start_exec DATETIME NOT NULL,
  end_exec DATETIME NOT NULL,
  elapsed BIGINT NOT NULL,
  worker_id VARCHAR(255), INDEX(worker_id),
  cat_source TEXT(5240) NOT NULL,
  mapping_name TEXT(5240)NOT NULL,
  exportID VARCHAR(255), INDEX(exportID),
  records_size BIGINT NOT NULL,
  message_count BIGINT NOT NULL,
  record_id VARCHAR(255), INDEX(record_id),
  rdf_file_size INT  NOT NULL
) ENGINE=InnoDB;


DROP TRIGGER IF EXISTS worker_auto_id;
CREATE TRIGGER worker_auto_id 
  BEFORE INSERT ON worker_benchmark
  FOR EACH ROW
  SET new.uid = UUID_TO_BIN(UUID());


    
LOAD DATA INFILE '/tmp/nl.uva.sne.vre4eic.cat_exporter.Worker.csv' 
INTO TABLE worker_benchmark 
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS


(@start,@end,@worker_id,@cat_source,@mapping_name,@exportID,@records_size,@message_count,@record_id,@rdf_file_size)
SET start_exec = FROM_UNIXTIME(@start/1000), 
    end_exec = FROM_UNIXTIME(@end/1000),
    elapsed = ((@end - @start) / 1000),
    worker_id = @worker_id,
    cat_source=@cat_source,
    mapping_name=@mapping_name,
    exportID=@exportID,
    records_size=@records_size,
    message_count=@message_count,
    record_id=@record_id,
    rdf_file_size=@rdf_file_size;
