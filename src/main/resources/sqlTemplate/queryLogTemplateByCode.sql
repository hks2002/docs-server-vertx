SELECT
  *
FROM
  log_template
WHERE
  template_code = '#{template_code}'
LIMIT
  1
  