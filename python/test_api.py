from flask import Flask, request, jsonify

app = Flask(__name__)

# Add this to handle the DS "GET /" request
@app.route('/', methods=['GET'])
def index():
    print("DS connected to root!")
    return "Hello from Flask! Connection Successful.\n"

@app.route('/ping', methods=['POST'])
def ping():
    data = request.get_json()
    print("Received from DS:", data)
    return jsonify({"response": "pong", "received": data})

if __name__ == '__main__':
    # Use debug=True to see errors in the terminal
    app.run(host='0.0.0.0', port=8080, debug=True)