const hre = require("hardhat");
const fs = require('fs');
const path = require('path');
const { ethers } = require("hardhat");

async function main() {
    console.log("Starting deployment...");

    // Deploy Mock USDC
    console.log("Deploying Mock USDC...");
    const MockUSDC = await ethers.getContractFactory("MockUSDC");
    const mockUsdc = await MockUSDC.deploy();
    console.log(`MockUSDC deployed to: ${mockUsdc.target}`);
    
    // Mint USDC for test accounts (1 million USDC each)
    const amount = hre.ethers.parseUnits("1000000", 6);
    await mockUsdc.mint("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266", amount);
    await mockUsdc.mint("0x70997970C51812dc3A010C7d01b50e0d17dc79C8", amount);
    console.log("Minted 1,000,000 USDC for test accounts\n");

    // Deploy Symmio with constructor arguments
    console.log("Deploying Symmio...");
    const SymmioDeposit = await ethers.getContractFactory("symmioDeposit");
    const symmio = await SymmioDeposit.deploy(mockUsdc.target);
    console.log(`Symmio deployed to: ${symmio.target}\n`);

    const [owner, user1, user2] = await ethers.getSigners();
    const BROKER_ROLE = await symmio.BROKER_ROLE();

    // Grant BROKER_ROLE to owner address
    console.log("\nSetting up BROKER_ROLE...");
    console.log(`Owner address: ${owner.address}`);
    console.log(`BROKER_ROLE hash: ${BROKER_ROLE}`);
    
    // Grant role and wait for transaction
    const grantTx = await symmio.grantRole(BROKER_ROLE, owner.address);
    await grantTx.wait();
    
    // Verify role was granted
    const hasRole = await symmio.hasRole(BROKER_ROLE, owner.address);
    console.log(`Role granted successfully: ${hasRole}`);
    
    if (!hasRole) {
        throw new Error("Failed to grant BROKER_ROLE to owner!");
    }

    // Get private keys from hardhat's default accounts
    const privateKeys = [
        "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80",  // owner/broker
        "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d",  // user1
        "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a"   // user2
    ];

    // Log addresses and roles for verification
    console.log("\nAccount setup:");
    console.log(`Broker/Owner address: ${owner.address}`);
    console.log(`User1 address: ${user1.address}`);
    console.log(`User2 address: ${user2.address}`);
    console.log(`BROKER_ROLE: ${BROKER_ROLE}`);

    console.log("\nDeployment addresses:");
    console.log(`USDC: ${mockUsdc.target}`);
    console.log(`Symmio: ${symmio.target}`);

    // Create user config with explicit contract addresses
    const userConfig = {
        rpcUrl: "http://localhost:8545",
        contractAddress: symmio.target,
        usdcAddress: mockUsdc.target,
        user_a: user1.address,
        user_b: user2.address,
        privateKey_a: privateKeys[1],
        privateKey_b: privateKeys[2]
    };

    // Log the config before writing
    console.log("\nUser config to be written:", JSON.stringify(userConfig, null, 2));

    // Create broker config
    const brokerConfig = {
        rpcUrl: "http://localhost:8545",
        contractAddress: symmio.target,
        usdcAddress: mockUsdc.target,
        brokerPrivateKey: privateKeys[0],  // Make sure this matches owner's address
        brokerRole: BROKER_ROLE,
        user_a: user1.address,
        user_b: user2.address
    };

    // Log config for verification
    console.log("\nBroker config to be written:");
    console.log(JSON.stringify(brokerConfig, null, 2));

    // Get the root project directory (2 levels up from the script)
    const projectRoot = path.resolve(__dirname, '..', '..');

    // Write configs
    const userConfigPath = path.join(projectRoot, 'Users', 'config.json');
    const brokerConfigPath = path.join(projectRoot, 'Meta-Broker', 'config', 'config.json');

    // Create directories if they don't exist
    fs.mkdirSync(path.dirname(userConfigPath), { recursive: true });
    fs.mkdirSync(path.dirname(brokerConfigPath), { recursive: true });

    // Write configs with verification
    fs.writeFileSync(userConfigPath, JSON.stringify(userConfig, null, 2));
    console.log("\nVerifying written files:");
    console.log("User config written:", fs.readFileSync(userConfigPath, 'utf8'));

    fs.writeFileSync(brokerConfigPath, JSON.stringify(brokerConfig, null, 2));
    console.log("Broker config written:", fs.readFileSync(brokerConfigPath, 'utf8'));

    console.log("\nConfig files created successfully!\n");

    console.log("\nDeployment Summary:");
    console.log("-------------------");
    console.log(`USDC Address: ${mockUsdc.target}`);
    console.log(`Symmio Address: ${symmio.target}`);
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    }); 