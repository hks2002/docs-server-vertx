SELECT
  *
FROM
  user_func
  INNER JOIN user ON user.id = user_func.user_id
  INNER JOIN func ON func.id = user_func.func_id
WHERE
  user.login_name = '#{login_name}'
  AND func.func_code = '#{func_code}'
  AND user_func.enable = 1
LIMIT
  1
