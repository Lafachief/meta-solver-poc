import websocket
import json
import time

def on_open(ws):
    print("WebSocket opened, sending STOMP CONNECT...")
    # Send CONNECT frame
    connect_frame = "CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\x00"
    ws.send(json.dumps([connect_frame]))  # One-element array containing the STOMP frame as a string

def on_message(ws, message):
    print("Raw message:", message)
    # At this point, you should see ["CONNECTED\nversion:1.1\nheart-beat:0,0\n\n\x00"] or similar
    if "CONNECTED" in message:
        # Now send SUBSCRIBE frames
        sub_orderbook = "SUBSCRIBE\nid:sub-0\ndestination:/topic/orderbook\nack:client\n\n\x00"
        sub_trades = "SUBSCRIBE\nid:sub-1\ndestination:/topic/matchedTrades\nack:client\n\n\x00"
        ws.send(json.dumps([sub_orderbook]))
        ws.send(json.dumps([sub_trades]))
    # When a MESSAGE frame arrives, parse it similarly
    # ...

def on_error(ws, error):
    print("WebSocket error:", error)

def on_close(ws, close_status, close_msg):
    print("WebSocket closed:", close_status, close_msg)

if __name__ == "__main__":
    ws_url = "ws://localhost:8080/ws"
    ws = websocket.WebSocketApp(
        ws_url,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close,
        subprotocols=["v10.stomp", "v11.stomp", "v12.stomp"],  # Some STOMP servers require specifying
    )
    ws.run_forever()