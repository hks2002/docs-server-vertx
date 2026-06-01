WITH
  file_rn AS (
    SELECT
      id,
      file_name,
      size,
      md5,
      doc_create_at,
      doc_modified_at,
      create_at,
      ROW_NUMBER() OVER (
        PARTITION BY
          md5
        ORDER BY
          id ASC
      ) AS row_num
    FROM
      docs
    WHERE
      md5 IS NOT NULL
      AND md5 != ''
  )
SELECT
  orig.id AS original_id,
  orig.file_name AS original_file_name,
  orig.create_at AS original_create_at,
  dup.id AS dup_id,
  dup.file_name AS dup_file_name,
  dup.create_at AS dup_create_at,
  orig.size,
  orig.md5
FROM
  file_rn orig
  JOIN file_rn dup ON orig.md5 = dup.md5
  AND orig.row_num = 1
  AND dup.row_num > 1
ORDER BY
  orig.md5,
  dup.id;