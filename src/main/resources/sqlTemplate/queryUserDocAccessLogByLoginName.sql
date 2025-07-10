WITH T0 AS (
	SELECT 
	  v3 AS filename,
	  log_at AS access_time,
	  ROW_NUMBER() OVER (PARTITION BY v3, DATE(log_at) ORDER BY log_at DESC) AS rn
	FROM log
	WHERE log.template_id = 9 
	AND v1 = '#{loginName}'
	ORDER BY id DESC
)

SELECT
   T0.filename,
   T0.access_time
FROM T0
WHERE 
   rn = 1
LIMIT
  #{limit}
OFFSET
  #{offset}