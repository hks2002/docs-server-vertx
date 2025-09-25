WITH T0 AS (
	SELECT 
	  v3 AS file_name,
	  log_at AS access_time,
	  ROW_NUMBER() OVER (PARTITION BY v3, DATE(log_at) ORDER BY log_at DESC) AS rn
	FROM log
	WHERE log.template_id = 9 
	AND v1 = '#{login_name}'
	ORDER BY id DESC
)

SELECT
   T0.file_name,
   docs.location,
   T0.access_time
FROM T0
LEFT JOIN docs
   ON T0.file_name = docs.file_name
WHERE 
   rn = 1
LIMIT
  #{limit}
OFFSET
  #{offset}