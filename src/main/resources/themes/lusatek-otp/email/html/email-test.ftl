<!DOCTYPE html>
<html lang="${locale!'en'}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${msg("emailTestSubject", realmName)}</title>
</head>
<body>
${msg("emailTestBodyHtml", realmName?html)?no_esc}
</body>
</html>
