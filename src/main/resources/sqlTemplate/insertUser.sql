INSERT INTO
  user (
    login_name,
    first_name,
    last_name,
    email,
    create_at,
    create_by,
    update_at,
    update_by
  )
VALUES
  (
    '#{login_name}',
    '#{first_name}',
    '#{last_name}',
    '#{email}',
    '#{create_at}',
    '#{create_by}',
    '#{update_at}',
    '#{update_by}'
  )