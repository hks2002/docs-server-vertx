SELECT
  BPCode,
  BPName
FROM
  bp
WHERE
  BPCode = '#{BPCode}'
LIMIT 1
