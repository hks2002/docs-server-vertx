UPDATE user_func
SET
  user_id = '#{user_id}',
  func_id = '#{func_id}',
  enable = #{enable},
  update_at = '#{update_at}',
  update_by = '#{update_by}'
WHERE
  id = '#{id}';
