SELECT
  *
FROM
  user_func
  INNER JOIN user ON user.id = user_func.user_id
  INNER JOIN func ON func.id = user_func.func_id
WHERE
  user.id = #{user_id}
  AND func.id = #{func.id}
LIMIT
  1
