SELECT 
    num_of_consumers, AVG(elapsed), std(elapsed), records_size 
FROM 
    main_benchmark 
group by 
    num_of_consumers, records_size 
order by 
    num_of_consumers;
