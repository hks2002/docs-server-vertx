WITH
  docs_admin AS (
    SELECT
      id AS admin_id,
      user_id,
      ENABLE AS admin_enable
    FROM
      user_func
    WHERE
      func_id = 1
  ),
  docs_read AS (
    SELECT
      id AS read_id,
      user_id,
      ENABLE AS read_enable
    FROM
      user_func
    WHERE
      func_id = 2
  ),
  docs_write AS (
    SELECT
      id AS write_id,
      user_id,
      ENABLE AS write_enable
    FROM
      user_func
    WHERE
      func_id = 3
  )
SELECT
  user.id,
  user.login_name,
  user.first_name,
  user.last_name,
  user.email,
  docs_admin.admin_id,
  docs_admin.admin_enable,
  docs_read.read_id,
  docs_read.read_enable,
  docs_write.write_id,
  docs_write.write_enable
FROM
  user
  LEFT JOIN docs_admin ON user.id = docs_admin.user_id
  LEFT JOIN docs_read ON user.id = docs_read.user_id
  LEFT JOIN docs_write ON user.id = docs_write.user_id
ORDER BY
  user.id
LIMIT
  #{limit}
OFFSET
  #{offset}