INSERT INTO
  docs (
    file_name,
    location,
    dms_id,
    size,
    doc_create_at,
    doc_modified_at,
    md5,
    create_at,
    create_by
  )
VALUES
  (
    '#{file_name}',
    '#{location}',
    '#{dms_id}',
    '#{size}',
    '#{doc_create_at}',
    '#{doc_modified_at}',
    '#{md5}',
    '#{create_at}',
    '#{create_by}'
  );
