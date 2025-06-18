--- keep last/max id of the file name
WITH T1 AS (
	SELECT   
		ROW_NUMBER() OVER (PARTITION BY file_name ORDER BY id DESC) AS ROWNUM,
		id
	FROM docs
),
T2 AS (
  SELECT * FROM T1
  WHERE T1.ROWNUM > 1
)
DELETE FROM docs
WHERE id IN (
 SELECT id FROM T2
)