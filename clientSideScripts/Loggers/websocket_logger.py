import logging
import json
import os
from datetime import datetime
import time
import websocket

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class WebSocketLogger:
    def __init__(self):
        self.log_dir = "websocket_logs"
        self.message_count = 0
        self.start_time = None
        self.ws = None
        
        if not os.path.exists(self.log_dir):
            os.makedirs(self.log_dir)

    def write_to_log(self, data, message_type="unknown"):
        try:
            # Create a logs directory with date
            current_date = datetime.now().strftime("%Y%m%d")
            log_dir = os.path.join(self.log_dir, current_date)
            if not os.path.exists(log_dir):
                os.makedirs(log_dir)
            
            # Create both JSON and human-readable logs
            json_filename = f"websocket_log_{current_date}.json"
            readable_filename = f"websocket_log_{current_date}.txt"
            
            json_filepath = os.path.join(log_dir, json_filename)
            readable_filepath = os.path.join(log_dir, readable_filename)
            
            # Write JSON log
            with open(json_filepath, 'a') as f:
                log_entry = {
                    "timestamp": datetime.now().isoformat(),
                    "type": message_type,
                    "data": data
                }
                json.dump(log_entry, f)
                f.write('\n')
                f.flush()
            
            # Write human-readable log
            with open(readable_filepath, 'a') as f:
                f.write(f"\n{'='*80}\n")
                f.write(f"Timestamp: {datetime.now().isoformat()}\n")
                f.write(f"Type: {message_type}\n")
                f.write(f"Data: {json.dumps(data, indent=2)}\n")
                f.write(f"{'='*80}\n")
                f.flush()
                
            logger.info(f"Logged {message_type} message")
            
        except Exception as e:
            logger.error(f"Error writing to log: {e}")

    def on_message(self, ws, message):
        try:
            # Log raw message first
            self.write_to_log(message, "raw_message")
            
            logger.info(f"Received message: {message[:200]}")
            
            if message == "o":
                logger.info("SockJS connection opened")
                connect_frame = {
                    "command": "CONNECT",
                    "headers": {
                        "accept-version": "1.1,1.0",
                        "heart-beat": "10000,10000"
                    }
                }
                ws.send(json.dumps([json.dumps(connect_frame)]))
                self.write_to_log(connect_frame, "connect_frame_sent")
                return
                
            if message.startswith("a["):
                data = json.loads(message[1:])  # Remove 'a' prefix
                for frame in data:
                    if "CONNECTED" in frame:
                        logger.info("STOMP Connected, sending subscription")
                        subscribe_frame_orderbook = {
                            "command": "SUBSCRIBE",
                            "headers": {
                                "id": "sub-0",
                                "destination": "/topic/orderbook",
                                "ack": "client"
                            }
                        }
                        subscribe_frame_trades = {
                            "command": "SUBSCRIBE",
                            "headers": {
                                "id": "sub-1",
                                "destination": "/topic/matchedTrades",
                                "ack": "client"
                            }
                        }
                        ws.send(json.dumps([json.dumps(subscribe_frame_orderbook)]))
                        ws.send(json.dumps([json.dumps(subscribe_frame_trades)]))
                        self.write_to_log(subscribe_frame_orderbook, "subscribe_frame_orderbook_sent")
                        self.write_to_log(subscribe_frame_trades, "subscribe_frame_trades_sent")
                    else:
                        try:
                            frame_data = json.loads(frame)
                            if isinstance(frame_data, dict) and 'body' in frame_data:
                                body_data = json.loads(frame_data['body'])
                                
                                # Get subscription ID from headers
                                sub_id = frame_data.get('headers', {}).get('subscription', 'unknown')
                                
                                if sub_id == 'sub-1':  # Matched trades subscription
                                    logger.info(f"Received matched trade: {body_data}")
                                    self.write_to_log(body_data, "matched_trade")
                                elif sub_id == 'sub-0':  # Orderbook subscription
                                    logger.info(f"Received orderbook update: {body_data}")
                                    self.write_to_log(body_data, "orderbook_update")
                                else:
                                    logger.info(f"Received unknown data: {body_data}")
                                    self.write_to_log(body_data, "unknown_data")
                                    
                                self.message_count += 1
                        except json.JSONDecodeError:
                            logger.warning(f"Could not parse frame as JSON: {frame}")
                            self.write_to_log(frame, "unparseable_frame")

        except Exception as e:
            logger.error(f"Error in on_message: {e}", exc_info=True)

    def on_error(self, ws, error):
        logger.error(f"WebSocket error: {error}")
        self.write_to_log(str(error), "error")

    def on_close(self, ws, close_status_code, close_msg):
        logger.warning(f"WebSocket closed: {close_status_code} - {close_msg}")
        self.write_to_log(f"{close_status_code} - {close_msg}", "close")

    def on_open(self, ws):
        logger.info("WebSocket connection opened")
        self.start_time = time.time()
        self.write_to_log("Connection opened", "open")

    def start(self):
        while True:
            try:
                logger.info("Starting WebSocket logger...")
                
                ws_url = "ws://localhost:8080/ws/websocket"
                logger.info(f"Connecting to {ws_url}")
                
                self.ws = websocket.WebSocketApp(
                    ws_url,
                    on_message=self.on_message,
                    on_error=self.on_error,
                    on_close=self.on_close,
                    on_open=self.on_open,
                    header={
                        "Upgrade": "websocket",
                        "Connection": "Upgrade",
                        "Origin": "http://localhost:3000"
                    }
                )
                
                self.ws.run_forever()
                time.sleep(5)
                
            except KeyboardInterrupt:
                logger.info("Shutting down...")
                if self.ws:
                    self.ws.close()
                break
            except Exception as e:
                logger.error(f"Fatal error: {e}", exc_info=True)
                time.sleep(5)

def main():
    websocket.enableTrace(True)
    logger = WebSocketLogger()
    logger.start()

if __name__ == "__main__":
    main()