import connexion
from flask_cors import CORS

app = connexion.App(__name__, specification_dir='openapi/')
app.add_api('ottopi.yaml')
CORS(app.app)

# Use FLASK development server to host connexion app
app.run(port=5555)

