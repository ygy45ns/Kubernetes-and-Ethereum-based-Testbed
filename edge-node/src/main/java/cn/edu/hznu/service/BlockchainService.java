package cn.edu.hznu.service;

import cn.edu.hznu.contract.Test_sol_ReputationManagement;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.List;

@Service
public class BlockchainService {
    private final Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:7545"));// ganache默认RPC地址

    @Value("${spring.application.contractAddress}")
    private String contractAddress;

    @Value("${spring.application.privateKey}")
    private String privateKey;

    private Credentials account; // Credentials是Web3j提供的账户身份封装类，包含了私钥、公钥、地址等信息，并负责交易签名

    @Getter
    private String address;

    public void initAccount() {
        account = Credentials.create(privateKey);
        address = account.getAddress();
    }

    public Test_sol_ReputationManagement loadContract(Credentials credentials) {
        return Test_sol_ReputationManagement.load(
                contractAddress,
                web3j,
                credentials,
                new StaticGasProvider(BigInteger.valueOf(20_000_000_000L), BigInteger.valueOf(6721975))
        );
    }

    public String callInitializeNode() throws Exception {
        Test_sol_ReputationManagement contract = loadContract(account);
        String address = account.getAddress();
        long t0 = System.currentTimeMillis();
        TransactionReceipt receipt = contract.initializeNode(address).send();
        long t1 = System.currentTimeMillis();
        System.out.println("reputation update spend " + (t1 - t0) + "ms");
        return receipt.getTransactionHash();
    }

    public List<BigInteger> callGetReputations() throws Exception {
        Test_sol_ReputationManagement contract = loadContract(account);
        return contract.getReputations().send();
    }

    private final Object reputationLock = new Object();

    public void callUpdateReputation(String address, String result, Boolean isSuccess, BigInteger Wd) throws Exception {
        synchronized (reputationLock) {
            Test_sol_ReputationManagement contract = loadContract(account);
            long t0 = System.currentTimeMillis();
            contract.updateReputation(address, result, isSuccess, Wd).send();
            long t1 = System.currentTimeMillis();
            System.out.println("reputation update spend " + (t1 - t0) + "ms");
        }
    }
}
