UPDATE func
SET
  func_group = '#{func_group}',
  func_code = '#{func_code}',
  func_note = '#{func_note}',
  update_at = '#{update_at}',
  update_by = '#{update_by}'
WHERE
  id = '#{id}';