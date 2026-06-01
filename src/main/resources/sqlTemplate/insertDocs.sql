INSERT INTO
  docs (
    file_name,
    dms_id,
    size,
    is_link,
    doc_create_at,
    doc_modified_at,
    md5,
    create_at,
    create_by
  )
VALUES
  (
    '#{file_name}',
    '#{dms_id}',
    '#{size}',
    '#{is_link}',
    '#{doc_create_at}',
    '#{doc_modified_at}',
    '#{md5}',
    '#{create_at}',
    '#{create_by}'
  );