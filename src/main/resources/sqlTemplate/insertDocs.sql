INSERT INTO
  docs (
    file_name,
    location,
    size,
    doc_create_at,
    doc_modified_at,
    md5,
    create_at,
    create_by,
    update_at,
    update_by
  )
VALUES
  (
    '#{file_name}',
    '#{location}',
    '#{size}',
    '#{doc_create_at}',
    '#{doc_modified_at}',
    '#{md5}',
    '#{create_at}',
    '#{create_by}',
    '#{update_at}',
    '#{update_by}'
  );