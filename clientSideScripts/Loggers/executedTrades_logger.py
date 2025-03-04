import requests
import json
import os
from datetime import datetime
import time
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class ExecutedTradesLogger:
    def __init__(self):
        self.log_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "executed_trades")
        self.api_url = "http://localhost:8080/api/v1/orderbook/trades"
        self.last_trade_count = 0
        
        if not os.path.exists(self.log_dir):
            os.makedirs(self.log_dir)
            logger.info(f"Created log directory at: {self.log_dir}")

    def fetch_and_log_trades(self):
        try:
            response = requests.get(self.api_url)
            if response.status_code == 200:
                trades = response.json()
                current_trade_count = len(trades)
                
                # Only process new trades
                if current_trade_count > self.last_trade_count:
                    new_trades = trades[self.last_trade_count:]
                    
                    # Create a log entry with timestamp and trades
                    log_entry = {
                        "timestamp": datetime.now().isoformat(),
                        "trades": new_trades
                    }
                    
                    # Save JSON log with timestamp in filename
                    current_time = datetime.now().strftime("%Y%m%d_%H%M%S")
                    filename = f"executed_trades_{current_time}.json"
                    filepath = os.path.join(self.log_dir, filename)
                    
                    with open(filepath, 'w') as f:
                        json.dump(log_entry, f, indent=2)
                    
                    logger.info(f"Logged {len(new_trades)} new trades to {filename}")
                    self.last_trade_count = current_trade_count
                    return len(new_trades)
                
                return 0
            else:
                logger.error(f"Failed to fetch trades. Status code: {response.status_code}")
                return 0
                
        except Exception as e:
            logger.error(f"Error fetching/logging trades: {e}")
            return 0

def main():
    trade_logger = ExecutedTradesLogger()
    
    logger.info(f"Starting Executed Trades Logger... (Saving to {trade_logger.log_dir})")
    
    while True:
        try:
            new_trade_count = trade_logger.fetch_and_log_trades()
            time.sleep(5)  # Check every 5 seconds
            
        except KeyboardInterrupt:
            logger.info("Shutting down Executed Trades Logger...")
            break
        except Exception as e:
            logger.error(f"Unexpected error: {e}")
            time.sleep(30)  # Wait 30 seconds on error before retrying

if __name__ == "__main__":
    main() 