SELECT
  *
FROM
  docs
WHERE
  file_name like '#{file_name}'
ORDER BY
  doc_modified_at DESC
LIMIT
  #{limit}
OFFSET
  #{offset}
