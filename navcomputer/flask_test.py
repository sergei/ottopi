import connexion
from wsgiref.simple_server import make_server

app = connexion.App(__name__, specification_dir='openapi/')
app.add_api('ottopi.yaml')

# Use FLASK development server to host connexion app
# app.run(port=8080)

# Use builtin simple WSGI HTTP server to host connexion app
httpd = make_server('', 8080, app)
print("Serving on port 8000...")

# Serve until process is killed
httpd.serve_forever()
