UPDATE bp
SET
  BPCode = '#{BPCode}',
  BPName = '#{BPName}',
  update_at = '#{update_at}',
  update_by = '#{update_by}'
WHERE
  id = '#{id}';