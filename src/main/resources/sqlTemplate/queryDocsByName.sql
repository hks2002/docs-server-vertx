SELECT
  *
FROM
  docs
WHERE
  file_name like '#{file_name}'
ORDER BY
  id DESC
LIMIT
  #{limit}
OFFSET
  #{offset}
