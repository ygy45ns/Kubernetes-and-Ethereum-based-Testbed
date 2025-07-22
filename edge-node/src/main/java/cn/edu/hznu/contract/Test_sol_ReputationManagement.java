package cn.edu.hznu.contract;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.3.0.
 */
public class Test_sol_ReputationManagement extends Contract {
    private static final String BINARY = "6080604052605a6002553480156013575f5ffd5b50610ee8806100215f395ff3fe608060405234801561000f575f5ffd5b5060043610610086575f3560e01c806372016f751161005957806372016f7514610116578063cb3b89d814610134578063d673de4614610150578063eced55261461018057610086565b80630f4f8c4d1461008a57806314491ade146100a657806317825273146100c45780631c53c280146100e2575b5f5ffd5b6100a4600480360381019061009f919061097e565b61019e565b005b6100ae61046e565b6040516100bb9190610a0d565b60405180910390f35b6100cc610474565b6040516100d99190610add565b60405180910390f35b6100fc60048036038101906100f79190610afd565b6105ae565b60405161010d959493929190610b28565b60405180910390f35b61011e6105ee565b60405161012b9190610a0d565b60405180910390f35b61014e60048036038101906101499190610b79565b6105f9565b005b61016a60048036038101906101659190610b79565b610755565b6040516101779190610a0d565b60405180910390f35b61018861076a565b6040516101959190610a0d565b60405180910390f35b5f60015f8673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205490505f8103610221576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161021890610bfe565b60405180910390fd5b60018161022e9190610c49565b90505f5f828154811061024457610243610c7c565b5b905f5260205f209060050201905060405180602001604052805f81525080519060200120858051906020012050505f8461027e575f610281565b60015b60ff1690506001826004015f82825461029a9190610ca9565b9250508190555084156102c3576001826003015f8282546102bb9190610ca9565b925050819055505b5f8260040154606484600301546102da9190610cdc565b6102e49190610d4a565b90505f60648387846102f69190610cdc565b6103009190610cdc565b61030a9190610d4a565b6064600254865f015461031d9190610cdc565b6103279190610d4a565b6103319190610ca9565b90505f60648460016103439190610c49565b84606460016103529190610cdc565b61035c9190610c49565b6103669190610cdc565b6103709190610d4a565b606460025487600101546103849190610cdc565b61038e9190610d4a565b6103989190610ca9565b905081855f01819055508085600101819055505f8560010154865f01546103bf9190610ca9565b90505f8111610403576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103fa90610dc4565b60405180910390fd5b806064875f01546104149190610cdc565b61041e9190610d4a565b8660020181905550867ea4f8488cd701f9a77136eb5c91d5e2edef3c38001ca7148525acf8a262648187600201546040516104599190610a0d565b60405180910390a25050505050505050505050565b60025481565b60605f60015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054036104f5576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104ec90610e2c565b60405180910390fd5b5f5f8054905090505f8167ffffffffffffffff811115610518576105176107f2565b5b6040519080825280602002602001820160405280156105465781602001602082028036833780820191505090505b5090505f5f90505b828110156105a5575f818154811061056957610568610c7c565b5b905f5260205f2090600502016002015482828151811061058c5761058b610c7c565b5b602002602001018181525050808060010191505061054e565b50809250505090565b5f81815481106105bc575f80fd5b905f5260205f2090600502015f91509050805f0154908060010154908060020154908060030154908060040154905085565b5f5f80549050905090565b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205414610678576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161066f90610e94565b60405180910390fd5b5f5f8054905090505f6040518060a001604052806032815260200160328152602001603281526020015f81526020015f815250908060018154018082558091505060019003905f5260205f2090600502015f909190919091505f820151815f01556020820151816001015560408201518160020155606082015181600301556080820151816004015550506001816107109190610ca9565b60015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20819055505050565b6001602052805f5260405f205f915090505481565b606481565b5f604051905090565b5f5ffd5b5f5ffd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6107a982610780565b9050919050565b6107b98161079f565b81146107c3575f5ffd5b50565b5f813590506107d4816107b0565b92915050565b5f5ffd5b5f5ffd5b5f601f19601f8301169050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b610828826107e2565b810181811067ffffffffffffffff82111715610847576108466107f2565b5b80604052505050565b5f61085961076f565b9050610865828261081f565b919050565b5f67ffffffffffffffff821115610884576108836107f2565b5b61088d826107e2565b9050602081019050919050565b828183375f83830152505050565b5f6108ba6108b58461086a565b610850565b9050828152602081018484840111156108d6576108d56107de565b5b6108e184828561089a565b509392505050565b5f82601f8301126108fd576108fc6107da565b5b813561090d8482602086016108a8565b91505092915050565b5f8115159050919050565b61092a81610916565b8114610934575f5ffd5b50565b5f8135905061094581610921565b92915050565b5f819050919050565b61095d8161094b565b8114610967575f5ffd5b50565b5f8135905061097881610954565b92915050565b5f5f5f5f6080858703121561099657610995610778565b5b5f6109a3878288016107c6565b945050602085013567ffffffffffffffff8111156109c4576109c361077c565b5b6109d0878288016108e9565b93505060406109e187828801610937565b92505060606109f28782880161096a565b91505092959194509250565b610a078161094b565b82525050565b5f602082019050610a205f8301846109fe565b92915050565b5f81519050919050565b5f82825260208201905092915050565b5f819050602082019050919050565b610a588161094b565b82525050565b5f610a698383610a4f565b60208301905092915050565b5f602082019050919050565b5f610a8b82610a26565b610a958185610a30565b9350610aa083610a40565b805f5b83811015610ad0578151610ab78882610a5e565b9750610ac283610a75565b925050600181019050610aa3565b5085935050505092915050565b5f6020820190508181035f830152610af58184610a81565b905092915050565b5f60208284031215610b1257610b11610778565b5b5f610b1f8482850161096a565b91505092915050565b5f60a082019050610b3b5f8301886109fe565b610b4860208301876109fe565b610b5560408301866109fe565b610b6260608301856109fe565b610b6f60808301846109fe565b9695505050505050565b5f60208284031215610b8e57610b8d610778565b5b5f610b9b848285016107c6565b91505092915050565b5f82825260208201905092915050565b7f4e6f6465206e6f7420696e697469616c697a65640000000000000000000000005f82015250565b5f610be8601483610ba4565b9150610bf382610bb4565b602082019050919050565b5f6020820190508181035f830152610c1581610bdc565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f610c538261094b565b9150610c5e8361094b565b9250828203905081811115610c7657610c75610c1c565b5b92915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52603260045260245ffd5b5f610cb38261094b565b9150610cbe8361094b565b9250828201905080821115610cd657610cd5610c1c565b5b92915050565b5f610ce68261094b565b9150610cf18361094b565b9250828202610cff8161094b565b91508282048414831517610d1657610d15610c1c565b5b5092915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601260045260245ffd5b5f610d548261094b565b9150610d5f8361094b565b925082610d6f57610d6e610d1d565b5b828204905092915050565b7f4469766973696f6e206279207a65726f000000000000000000000000000000005f82015250565b5f610dae601083610ba4565b9150610db982610d7a565b602082019050919050565b5f6020820190508181035f830152610ddb81610da2565b9050919050565b7f4e6f6465206e6f742072656769737465726564000000000000000000000000005f82015250565b5f610e16601383610ba4565b9150610e2182610de2565b602082019050919050565b5f6020820190508181035f830152610e4381610e0a565b9050919050565b7f4e6f646520616c726561647920696e697469616c697a656400000000000000005f82015250565b5f610e7e601883610ba4565b9150610e8982610e4a565b602082019050919050565b5f6020820190508181035f830152610eab81610e72565b905091905056fea264697066735822122056ba7899221862a92413de44c3cbad14764982153103e1b469f2cb3e7839ed8664736f6c634300081d0033";

    public static final String FUNC_SCALE = "SCALE";

    public static final String FUNC_WF = "Wf";

    public static final String FUNC_GETNODELENGTH = "getNodeLength";

    public static final String FUNC_GETREPUTATIONS = "getReputations";

    public static final String FUNC_INITIALIZENODE = "initializeNode";

    public static final String FUNC_NODEINDEX = "nodeIndex";

    public static final String FUNC_NODES = "nodes";

    public static final String FUNC_UPDATEREPUTATION = "updateReputation";

    public static final Event REPUTATIONUPDATED_EVENT = new Event("ReputationUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected Test_sol_ReputationManagement(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Test_sol_ReputationManagement(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Test_sol_ReputationManagement(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Test_sol_ReputationManagement(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<ReputationUpdatedEventResponse> getReputationUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REPUTATIONUPDATED_EVENT, transactionReceipt);
        ArrayList<ReputationUpdatedEventResponse> responses = new ArrayList<ReputationUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ReputationUpdatedEventResponse typedResponse = new ReputationUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.nodeId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newReputation = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ReputationUpdatedEventResponse> reputationUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ReputationUpdatedEventResponse>() {
            @Override
            public ReputationUpdatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REPUTATIONUPDATED_EVENT, log);
                ReputationUpdatedEventResponse typedResponse = new ReputationUpdatedEventResponse();
                typedResponse.log = log;
                typedResponse.nodeId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.newReputation = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ReputationUpdatedEventResponse> reputationUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REPUTATIONUPDATED_EVENT));
        return reputationUpdatedEventFlowable(filter);
    }

    public RemoteCall<TransactionReceipt> SCALE() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SCALE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> Wf() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_WF, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> getNodeLength() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_GETNODELENGTH, 
                Arrays.<Type>asList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<List<BigInteger>> getReputations() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_GETREPUTATIONS,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {
                })
        );
        return new RemoteFunctionCall<>(function,
                () -> {
                    List<Type> results = executeCallMultipleValueReturn(function);
                    List<BigInteger> reputations = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<Uint256> rawList = ((DynamicArray<Uint256>) results.get(0)).getValue();
                    for (Uint256 rep : rawList) {
                        reputations.add(rep.getValue());
                    }
                    return reputations;
                }
        );
    }

    public RemoteCall<TransactionReceipt> initializeNode(String _nodeAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_INITIALIZENODE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_nodeAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> nodeIndex(String param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_NODEINDEX,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> nodes(BigInteger param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_NODES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> updateReputation(String _nodeAddress, String result, Boolean _isSuccess, BigInteger _Wd) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_UPDATEREPUTATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_nodeAddress), 
                new org.web3j.abi.datatypes.Utf8String(result), 
                new org.web3j.abi.datatypes.Bool(_isSuccess), 
                new org.web3j.abi.datatypes.generated.Uint256(_Wd)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static Test_sol_ReputationManagement load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Test_sol_ReputationManagement(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Test_sol_ReputationManagement load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Test_sol_ReputationManagement(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Test_sol_ReputationManagement load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Test_sol_ReputationManagement(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Test_sol_ReputationManagement load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Test_sol_ReputationManagement(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Test_sol_ReputationManagement> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Test_sol_ReputationManagement.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Test_sol_ReputationManagement> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Test_sol_ReputationManagement.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<Test_sol_ReputationManagement> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Test_sol_ReputationManagement.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Test_sol_ReputationManagement> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Test_sol_ReputationManagement.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class ReputationUpdatedEventResponse {
        public Log log;

        public BigInteger nodeId;

        public BigInteger newReputation;
    }
}
