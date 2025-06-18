SELECT
  *
FROM
  user
WHERE
  login_name = '#{login_name}'
LIMIT
  1