UPDATE docs
SET
  file_name = '#{file_name}',
  location = '#{location}',
  dms_id = '#{dms_id}',
  size = '#{size}',
  doc_create_at = '#{doc_create_at}',
  doc_modified_at = '#{doc_modified_at}',
  md5 = '#{md5}',
  update_at = '#{update_at}',
  update_by = '#{update_by}'
WHERE
  id = '#{id}';
