import websocket
import json
import logging
import threading
import time

# Set up more detailed logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class StompWebSocket:
    def __init__(self):
        self.ws = None

    def on_message(self, ws, message):
        logger.info(f"STOMP Received: {message}")
        try:
            data = json.loads(message)
            if isinstance(data, list) and len(data) > 0:
                frame = data[0]
                if "CONNECTED" in frame:
                    logger.info("STOMP Connected! Sending subscriptions...")
                    # Subscribe to orderbook
                    subscribe_orderbook = "SUBSCRIBE\nid:sub-0\ndestination:/topic/orderbook\n\n\x00"
                    ws.send(json.dumps([subscribe_orderbook]))
                    
                    # Subscribe to matched trades
                    subscribe_trades = "SUBSCRIBE\nid:sub-1\ndestination:/topic/matchedTrades\n\n\x00"
                    ws.send(json.dumps([subscribe_trades]))
        except json.JSONDecodeError:
            logger.warning(f"Could not parse STOMP message as JSON: {message}")

    def on_error(self, ws, error):
        logger.error(f"STOMP Error: {error}")

    def on_close(self, ws, close_status_code, close_msg):
        logger.info(f"STOMP Connection Closed: {close_status_code} - {close_msg}")

    def on_open(self, ws):
        logger.info("STOMP WebSocket Connected! Sending CONNECT frame...")
        # Modified CONNECT frame format
        connect_frame = [
            "CONNECT",
            "accept-version:1.1,1.0",
            "heart-beat:10000,10000",
            "",
            "\x00"
        ]
        ws.send(json.dumps(["\n".join(connect_frame)]))
        logger.info("STOMP CONNECT frame sent")

    def start(self):
        websocket.enableTrace(True)
        headers = {
            'Origin': 'http://localhost:8080'
        }
        self.ws = websocket.WebSocketApp(
            "ws://localhost:8080/ws-stomp",
            header=headers,
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close,
            subprotocols=['v10.stomp', 'v11.stomp', 'v12.stomp']  # Added all STOMP versions
        )
        self.ws.run_forever(ping_interval=10, ping_timeout=5)

class RawWebSocket:
    def __init__(self):
        self.ws = None

    def on_message(self, ws, message):
        logger.info(f"RAW Received: {message}")

    def on_error(self, ws, error):
        logger.error(f"RAW Error: {error}")
        # Add detailed error information
        if hasattr(error, 'status_code'):
            logger.error(f"Status code: {error.status_code}")
        if hasattr(error, 'headers'):
            logger.error(f"Headers: {error.headers}")
        if hasattr(error, 'message'):
            logger.error(f"Message: {error.message}")

    def on_close(self, ws, close_status_code, close_msg):
        logger.info(f"RAW Connection Closed: {close_status_code} - {close_msg}")

    def on_open(self, ws):
        logger.info("RAW WebSocket Connected!")
        # Send a test message
        ws.send("Hello from raw WebSocket!")

    def start(self):
        websocket.enableTrace(True)  # Enable detailed WebSocket traces
        self.ws = websocket.WebSocketApp(
            "ws://localhost:8080/ws-raw",
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        # Run with ping_interval to keep connection alive
        self.ws.run_forever(ping_interval=10, ping_timeout=5)

def run_stomp_websocket():
    stomp_ws = StompWebSocket()
    stomp_ws.start()

def run_raw_websocket():
    raw_ws = RawWebSocket()
    raw_ws.start()

if __name__ == "__main__":
    # Start both WebSocket connections in separate threads
    stomp_thread = threading.Thread(target=run_stomp_websocket)
    raw_thread = threading.Thread(target=run_raw_websocket)

    stomp_thread.start()
    raw_thread.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Shutting down...") 