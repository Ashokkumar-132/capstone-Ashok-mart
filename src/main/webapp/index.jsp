<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AshokMart | Foundation</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.5; margin: 0; padding: 2rem; color: #1f2937; background: #f8fafc; }
        main { max-width: 680px; margin: 3rem auto; padding: 2rem; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }
        h1 { margin-top: 0; color: #111827; }
        .tagline { color: #4b5563; font-size: 1.1rem; }
        dl { display: grid; grid-template-columns: 9rem 1fr; gap: .6rem 1rem; margin-top: 2rem; }
        dt { font-weight: 700; }
        dd { margin: 0; }
    </style>
</head>
<body>
<main>
    <h1>AshokMart</h1>
    <p class="tagline">Your marketplace, your choice.</p>

    <section aria-labelledby="status-heading">
        <h2 id="status-heading">Development Status</h2>
        <dl>
            <dt>Application:</dt><dd>AshokMart</dd>
            <dt>Status:</dt><dd>Foundation Ready</dd>
            <dt>Architecture:</dt><dd>JSP → Servlet → Service → DAO → JDBC → H2</dd>
        </dl>
    </section>
</main>
</body>
</html>
