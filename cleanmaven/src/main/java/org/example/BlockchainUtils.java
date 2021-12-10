package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.example.ethereum.ProcessMonitor;
import org.example.ethereum.ProcessTemplate;
import org.web3j.codegen.SolidityFunctionWrapperGenerator;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainIdLong;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

public class BlockchainUtils {

    String projectPath = "src" + File.separator + "main" + File.separator + "java" + File.separator + "org" + File.separator + "example";

    Web3j web3 = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/165cba6af9774e1aaa692e914a0cfbbb"));
    Admin adm = Admin.build(new HttpService("http://193.205.92.133:8545"));


    Credentials credentials = Credentials.create("787e08ed227770a88bc41b6abc6422e199e34a2609ffde66387b4d617098fa71");
    ProcessTemplate contract;

    private List<String> messageInputs = new ArrayList<>();

    //Credentials credentials = WalletUtils.loadCredentials("password", "/path/to/walletfile");


    public void wrapper(String fileName) throws Exception {
        String path = projectPath + File.separator + "ethereum" + File.separator;
        String abiPath = path + parseName(fileName, ".abi");
        String binPath = path + parseName(fileName, ".bin");

        String[] args2 = {"-a", abiPath, "-b", binPath, "-o", projectPath + File.separator, "-p",
                "ethereum",};

        SolidityFunctionWrapperGenerator.main(args2);
    }

    public void compile(String fileName) throws Exception {
        String fin = parseName(fileName, ".sol");
        String solPath = projectPath + File.separator + "ethereum" + File.separator + fin;
        String destinationPath = projectPath + File.separator + "ethereum";
        String[] comm = {"solc", solPath, "--bin", "--abi", "--overwrite", "-o", destinationPath};
        Runtime rt = Runtime.getRuntime();
        java.lang.Process p = rt.exec(comm);
        BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line;
        while ((line = bri.readLine()) != null) {
            System.out.println(line);
        }
        bri.close();
        while ((line = bre.readLine()) != null) {
            System.out.println(line);
        }
        bre.close();
        p.waitFor();
    }

    // 0x62A18a87Ba55FB2c26bBB18ccD07F9847e24C29d

    public ProcessMonitor loadMonitor() {
        TransactionManager m = new RawTransactionManager(web3, credentials, ChainIdLong.RINKEBY);
        ContractGasProvider c = new DefaultGasProvider();
        return ProcessMonitor.load("0x48C49978328758080f2fB8ABA79205EF97Bf806E",
                web3,
                m,
                c);
    }

    /*public String deployContract() throws Exception {
        ProcessMonitor monitor = loadMonitor();
        TransactionReceipt receipt = monitor.instantiateProcess().send();
        List<ProcessMonitor.NewContractEventResponse> response = monitor.getNewContractEvents(receipt);
        System.out.println(response.get(0).newContract);
        return response.get(0).newContract;
    }*/

    public static String parseName(String name, String extension) {
        String[] oldName = name.split("\\.");
        String newName = oldName[0] + extension;
        return newName;
    }

    public ProcessTemplate loadContract(String address) {
        TransactionManager m = new RawTransactionManager(web3, credentials, ChainIdLong.RINKEBY);
        BigInteger GAS_PRICE = BigInteger.valueOf(18_000_000_000L);
        BigInteger GAS_LIMIT = BigInteger.valueOf(9_000_000L);
        ContractGasProvider c = new DefaultGasProvider();
        return contract = ProcessTemplate.load(address,
                web3,
                m,
                c);
        /*return contract = ProcessTemplate.load(
                "0xe60c566C37A7DF703CDE71E81837232513836ee8",
                web3,
                credentials,
                GAS_PRICE,
                GAS_LIMIT);*/
    }

    public BigInteger getLatestBlockNumber() throws Exception {
        return web3.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock().getNumber();
    }


    public String getStringFromContract(String variable) throws Exception {
        String result = contract.getString(variable).send();
        return result;
    }

    public BigInteger getIntFromContract(String variable) throws Exception {
        BigInteger result = contract.getInt(variable).send();
        return result;
    }

    public Boolean getBoolFromContract(String variable) throws Exception {
        Boolean result = contract.getBool(variable).send();
        return result;
    }

    public BigInteger getState(String variable) throws Exception {
        BigInteger state = contract.getMessage(variable).send();
        return state;
    }

   /* public void setMessageToContract(String messageId) throws Exception {
        contract.setMessage(messageId).send();
    }*/

    public void setVarialesToContract(List<String> types, List<String> variables, List<String> values, String messageId) throws Exception {
        //contract.setVariables(stringVar, stringVal, uintVar, uintVal, boolVar, boolVal).send();
        contract.setVariables(types, variables, values, messageId).send();
    }

    public void setRulesToContract(List<String> elementId, List<String> ruleToAdd) throws Exception {
        contract.setRules(elementId, ruleToAdd).send();
    }

    public List<String> getMessageInputs() {
        return messageInputs;
    }

    public void setMessageInputs(List<String> stringList) {
        this.messageInputs = stringList;
    }

    public String getSingleInput(int index) {
        return messageInputs.get(index);
    }

    public String getRule(String messageId) throws Exception {
        return contract.getRule(messageId).send();
    }

}

