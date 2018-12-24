


SET @ex_id := (select exportID from main_benchmark where num_of_consumers=@consumers order by start_exec desc limit 1); 
SET @t0 := (SELECT main_benchmark.start_exec FROM main_benchmark where main_benchmark.exportID = @ex_id);

SELECT 
	'Main', 
	(start_exec-@t0) AS start, 
	elapsed AS elapsed, 
	start_exec, 
	((start_exec-@t0)+elapsed) AS end, 
	CONCAT('consumers',@consumers)
FROM 
	main_benchmark 
where 
	exportID = @ex_id 
UNION 
	SELECT 
		'Service', 
		(start_exec-@t0) AS start,  
		elapsed AS elapsed , 
		start_exec, 
		((start_exec-@t0)+elapsed) AS end, 
		CONCAT('consumers',@consumers)
	FROM 
		service_benchmark 
    where 
		exportID = @ex_id 
	UNION 
		SELECT 
        CONCAT('Worker',worker_id), 
        (start_exec-@t0) AS start, 
        elapsed AS elapsed,  
        start_exec, 
        ((start_exec-@t0)+elapsed) AS end, 
        CONCAT('consumers',@consumers)
		FROM 
			worker_benchmark 
		where 
			exportID = @ex_id 
		ORDER BY 
			start_exec; 


