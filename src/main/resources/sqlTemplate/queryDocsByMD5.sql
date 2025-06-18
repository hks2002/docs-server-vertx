SELECT
  *
FROM
  docs
WHERE
  md5 = '#{md5}'
ORDER BY
  id DESC
LIMIT
  #{limit}
OFFSET
  #{offset}
