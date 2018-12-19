SELECT num_of_consumers, AVG(elapsed/1000.0), std(elapsed/1000.0), records_size FROM main_benchmark group by num_of_consumers, records_size order by num_of_consumers;
