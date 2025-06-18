UPDATE user
SET
  login_name = '#{login_name}',
  first_name = '#{first_name}',
  last_name = '#{last_name}',
  email = '#{email}',
  update_at = '#{update_at}',
  update_by = '#{update_by}'
WHERE
  id = '#{id}';