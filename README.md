# Meta-Solver: Decentralized Trading Engine with ZK-Proofs

## Overview
Meta-Solver is a high-performance decentralized trading engine that combines traditional order matching with blockchain security and zero-knowledge proofs. It serves as a meta-broker layer between traders and the Symmio protocol, offering fast off-chain matching with on-chain settlement.

## Key Features
- High-performance order matching engine
- Real-time WebSocket updates for order book and trades
- Ethereum-based authentication using signed messages
- Zero-knowledge proof integration for trade verification
- Margin account management with meta-broker oversight
- USDC and ETH balance tracking
- Self-trade prevention
- Batch order processing

## Architecture

### Smart Contracts
- `symmioDeposit.sol`: Main contract handling deposits, withdrawals, and margin accounts
  - Supports both ETH and USDC deposits
  - ZK-proof verification for withdrawals
  - Meta-broker signature verification
  - Role-based access control

### Java Backend
- **Core Components**:
  - `MatchingEngine`: High-performance order matching logic
  - `OrderBookService`: REST API for order management
  - `UserSessionService`: Ethereum signature-based authentication
  - `MarginAccountReader`: Smart contract interaction layer
  - `TradeProofService`: ZK-proof generation and verification

- **Models**:
  - `Order`: Order book entry with price-time priority
  - `Trade`: Executed trade information
  - `OrderBookState`: Current state of the order book

### WebSocket Integration
- Real-time updates for:
  - Order book changes
  - Trade executions
  - Balance updates

## Setup

### Prerequisites
- Java 17+
- Node.js 16+
- Hardhat
- Maven
- Web3j

### Configuration
1. Create `config/config.json`:
```json
{
    "rpcUrl": "YOUR_ETHEREUM_NODE_URL",
    "contractAddress": "DEPLOYED_CONTRACT_ADDRESS",
    "usdcAddress": "USDC_CONTRACT_ADDRESS",
    "brokerPrivateKey": "META_BROKER_PRIVATE_KEY",
    "brokerRole": "BROKER_ROLE_HASH"
}
```

### Installation

1. Deploy Smart Contracts:
```bash
cd Contracts
npm install
npx hardhat compile
npx hardhat run scripts/deployAll.js --network <your-network>
```

2. Build Java Backend:
```bash
cd Meta-Broker
mvn clean install
```

3. Start the System:
```bash
./start-exchange.ps1  # Windows
./start-exchange.sh   # Linux/Mac
```

## API Endpoints

### REST API
- `POST /api/v1/orderbook/session`: Create trading session
- `POST /api/v1/orderbook/order`: Submit new order
- `DELETE /api/v1/orderbook/order/{orderId}`: Cancel order
- `GET /api/v1/orderbook/status`: Get order book status
- `GET /api/v1/orderbook/trades`: Get trade history
- `GET /api/v1/orderbook/balance`: Get user balance

### WebSocket Topics
- `/topic/orderbook`: Order book updates
- `/topic/matchedTrades`: Real-time trade notifications

## Security Features
- Ethereum signature verification for authentication
- Zero-knowledge proofs for withdrawal verification
- Meta-broker co-signing for margin withdrawals
- Role-based access control for administrative functions
- Self-trade prevention mechanisms
- Secure balance management

## Testing
- Run test client:
```bash
cd clientSideScripts
python testClient.py
```

## Monitoring
- Use `BalanceChecker` tool to verify exchange balances:
```bash
java -cp target/meta-broker.jar com.metasolver.tools.BalanceChecker
```

## License
MIT License

## Contributing
1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request 