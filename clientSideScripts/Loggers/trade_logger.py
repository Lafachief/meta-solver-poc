import requests
import json
import os
from datetime import datetime
import time
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class TradeLogger:
    def __init__(self):
        self.log_dir = "trade_logs"
        self.api_url = "http://localhost:8080/api/v1/orderbook/trades"  # Updated URL to match Spring endpoint
        
        if not os.path.exists(self.log_dir):
            os.makedirs(self.log_dir)

    def fetch_and_log_trades(self):
        try:
            response = requests.get(self.api_url)
            if response.status_code == 200:
                trades = response.json()
                
                # Create filename with current timestamp
                current_time = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"trades_{current_time}.json"
                filepath = os.path.join(self.log_dir, filename)
                
                # Write trades to JSON file
                with open(filepath, 'w') as f:
                    json.dump(trades, f, indent=2)
                
                logger.info(f"Logged {len(trades)} trades to {filename}")
                return len(trades)
            else:
                logger.error(f"Failed to fetch trades. Status code: {response.status_code}")
                return 0
                
        except Exception as e:
            logger.error(f"Error fetching/logging trades: {e}")
            return 0

def main():
    trade_logger = TradeLogger()
    
    while True:
        try:
            trade_count = trade_logger.fetch_and_log_trades()
            logger.info(f"Sleeping for 1 second...")
            time.sleep(1)  # Wait 1 second before next fetch
            
        except KeyboardInterrupt:
            logger.info("Shutting down trade logger...")
            break
        except Exception as e:
            logger.error(f"Unexpected error: {e}")
            time.sleep(60)  # Wait 1 minute on error before retrying

if __name__ == "__main__":
    main() 