from web3 import Web3
import json
import os



class TestClient:
    def __init__(self):
        # Load config from parent directory
        config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'Users', 'config.json')
        with open(config_path) as f:
            self.config = json.load(f)
        
        # Connect to local Hardhat node
        self.w3 = Web3(Web3.HTTPProvider(self.config['rpcUrl']))
        
        # Load Symmio contract
        contract_path = os.path.join(
            os.path.dirname(os.path.dirname(__file__)), 
            'Contracts', 
            'artifacts',
            'contracts',
            'symmioDeposit.sol',
            'symmioDeposit.json'
        )
        with open(contract_path) as f:
            contract_json = json.load(f)
            self.symmio_abi = contract_json['abi']
        
        # Load USDC contract
        usdc_path = os.path.join(
            os.path.dirname(os.path.dirname(__file__)), 
            'Contracts', 
            'artifacts',
            'contracts',
            'MockUSDC.sol',
            'MockUSDC.json'
        )
        with open(usdc_path) as f:
            usdc_json = json.load(f)
            self.usdc_abi = usdc_json['abi']
        
        # Initialize contracts
        self.symmio = self.w3.eth.contract(
            address=self.config['contractAddress'],
            abi=self.symmio_abi
        )
        self.usdc = self.w3.eth.contract(
            address=self.config['usdcAddress'],
            abi=self.usdc_abi
        )
        
        # Get test accounts
        self.accounts = self.w3.eth.accounts
        print(f"Connected to contracts:")
        print(f"Symmio: {self.config['contractAddress']}")
        print(f"USDC: {self.config['usdcAddress']}")

    def deposit_eth(self, account: str, amount: int):
        """Deposit ETH into the contract"""
        try:
            transaction = self.symmio.functions.deposit().build_transaction({
                'from': account,
                'value': amount,
                'gas': 200000,
                'gasPrice': self.w3.eth.gas_price,
                'nonce': self.w3.eth.get_transaction_count(account)
            })
            
            print(f"Depositing {self.w3.from_wei(amount, 'ether')} ETH from {account}")
            tx_hash = self.w3.eth.send_transaction(transaction)
            receipt = self.w3.eth.wait_for_transaction_receipt(tx_hash)
            print(f"Deposit successful! Hash: {receipt['transactionHash'].hex()}")
            return receipt
            
        except Exception as e:
            print(f"Error depositing ETH: {e}")
            return None

    def approve_usdc(self, account: str, amount: int):
        """Approve USDC spending"""
        try:
            transaction = self.usdc.functions.approve(
                self.config['contractAddress'],
                amount
            ).build_transaction({
                'from': account,
                'gas': 200000,
                'gasPrice': self.w3.eth.gas_price,
                'nonce': self.w3.eth.get_transaction_count(account)
            })
            
            print(f"Approving {amount} USDC from {account}")
            tx_hash = self.w3.eth.send_transaction(transaction)
            receipt = self.w3.eth.wait_for_transaction_receipt(tx_hash)
            print(f"Approval successful! Hash: {receipt['transactionHash'].hex()}")
            return receipt
            
        except Exception as e:
            print(f"Error approving USDC: {e}")
            return None

    def deposit_usdc(self, account: str, amount: int):
        """Deposit USDC into the contract"""
        try:
            # First approve
            self.approve_usdc(account, amount)
            
            # Then deposit
            transaction = self.symmio.functions.depositUSDC(amount).build_transaction({
                'from': account,
                'gas': 200000,
                'gasPrice': self.w3.eth.gas_price,
                'nonce': self.w3.eth.get_transaction_count(account)
            })
            
            print(f"Depositing {amount} USDC from {account}")
            tx_hash = self.w3.eth.send_transaction(transaction)
            receipt = self.w3.eth.wait_for_transaction_receipt(tx_hash)
            print(f"USDC Deposit successful! Hash: {receipt['transactionHash'].hex()}")
            return receipt
            
        except Exception as e:
            print(f"Error depositing USDC: {e}")
            return None

    def move_to_margin(self, account: str, amount: int, is_usdc: bool = False):
        """Move funds from deposit to margin account"""
        try:
            # Use different function based on token type
            if is_usdc:
                func = self.symmio.functions.moveUSDCToMargin(amount)
            else:
                func = self.symmio.functions.moveToMargin(amount)
                
            transaction = func.build_transaction({
                'from': account,
                'gas': 200000,
                'gasPrice': self.w3.eth.gas_price,
                'nonce': self.w3.eth.get_transaction_count(account)
            })
            
            token_type = "USDC" if is_usdc else "ETH"
            amount_display = amount if is_usdc else self.w3.from_wei(amount, 'ether')
            print(f"Moving {amount_display} {token_type} to margin for {account}")
            
            tx_hash = self.w3.eth.send_transaction(transaction)
            receipt = self.w3.eth.wait_for_transaction_receipt(tx_hash)
            print(f"Move to margin successful! Hash: {receipt['transactionHash'].hex()}")
            return receipt
            
        except Exception as e:
            print(f"Error moving to margin: {e}")
            return None

    def get_balance(self, account: str):
        """Get user's balance from contract"""
        balance = self.symmio.functions.users(account).call()
        return balance[0]  # Return balance from User struct

    def setup_test_accounts(self):
        """Setup two test accounts with different assets"""
        # Use first two test accounts
        eth_user = self.accounts[0]  # User A will deposit ETH
        usdc_user = self.accounts[1] # User B will deposit USDC
        
        # Amount to deposit (100 ETH and 100,000 USDC)
        eth_amount = self.w3.to_wei(100, 'ether')
        usdc_amount = 100_000_000_000  # 100,000 USDC (with 6 decimals)
        
        print("\nSetting up ETH User...")
        self.deposit_eth(eth_user, eth_amount)
        self.move_to_margin(eth_user, eth_amount)
        
        print("\nSetting up USDC User...")
        self.deposit_usdc(usdc_user, usdc_amount)
        self.move_to_margin(usdc_user, usdc_amount, is_usdc=True)
        
        print("\nSetup complete!")

def main():
    print("Initializing TestClient...")
    client = TestClient()
    client.setup_test_accounts()

if __name__ == "__main__":
    main()
