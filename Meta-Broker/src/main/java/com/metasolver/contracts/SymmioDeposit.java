package com.metasolver.contracts;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import java.math.BigInteger;
import java.util.List;
import java.util.Arrays;
import org.web3j.abi.datatypes.Bool;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import java.util.Collections;


public class SymmioDeposit extends Contract {
    public static final String BINARY = "YOUR_CONTRACT_BINARY_HERE";

    public SymmioDeposit(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    public RemoteCall<List<BigInteger>> getBalances(String account) {
        final Function function = new Function("getBalances",
            List.of(new Address(account)),
            List.of(new TypeReference<Uint256>() {},
                   new TypeReference<Uint256>() {},
                   new TypeReference<Uint256>() {},
                   new TypeReference<Uint256>() {})
        );
        return new RemoteCall<>(() -> executeCallMultipleValueReturn(function)
            .stream()
            .map(val -> (BigInteger) val.getValue())
            .toList());
    }

    public RemoteFunctionCall<Boolean> hasBrokerRole(String account) {
        final Function function = new Function(
            "hasBrokerRole", 
            Arrays.asList(new Address(account)),
            Arrays.asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> settleBalances(String account) {
        final Function function = new Function(
            "settleBalances", 
            Arrays.asList(new Address(account)),
            Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static SymmioDeposit load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SymmioDeposit(contractAddress, web3j, credentials, contractGasProvider);
    }
} 