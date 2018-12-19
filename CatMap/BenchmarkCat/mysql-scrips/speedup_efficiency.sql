SET @t1 := (SELECT AVG(elapsed) FROM main_benchmark group by num_of_consumers, records_size order by num_of_consumers limit 1);
SELECT num_of_consumers, (@t1 / AVG(elapsed/1000.0)) AS speedup, (@t1 / AVG(elapsed/1000.0))/num_of_consumers AS efficiency,records_size FROM main_benchmark group by num_of_consumers, records_size order by num_of_consumers;
