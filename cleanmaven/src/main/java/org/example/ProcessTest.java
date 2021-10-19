package org.example;

import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.cdi.KSession;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.web3j.protocol.core.DefaultBlockParameter;
//import org.web3j.codegen.SolidityFunctionWrapperGenerator;
import org.web3j.protocol.core.DefaultBlockParameterName;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.drools.core.SessionConfiguration;
import org.example.ethereum.*;

//import com.sample.Process;



public class ProcessTest {

    BigInteger lastEventBlockNumber = BigInteger.valueOf(0L);
    BigInteger lastRuleBlockNumber = BigInteger.valueOf(0L);

    public static void main(String[] args) throws Exception {
        ProcessTest te = new ProcessTest();
        BlockchainUtils blockchainUtil = new BlockchainUtils();

       te.subToMonitor();
        //te.subToRules();
       // te.subToMessages("0xacc5c5b63c1ab16ae703a7e6d7c31fc4527e0234");
        //te.executeMessage(blockchainUtil);
        /*blockchainUtil.compile("ProcessTemplate");
        blockchainUtil.wrapper("ProcessTemplate");
        blockchainUtil.wrapper("ProcessMonitor");*/
        
        
        
        

    }

    public void subToMonitor(){
        BlockchainUtils blockchainUtil = new BlockchainUtils();
        ProcessMonitor monitor = blockchainUtil.loadMonitor();
        monitor.newContractEventFlowable(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST).
                subscribe((eventResponse) ->{
                    String newContractAddress = eventResponse.newContract;
                    System.out.println("New contract captured: " + newContractAddress);
                    subToMessages(newContractAddress);
                    //subToRules(newContractAddress);
                });
    }

    public void subToRules(String address) throws Exception {
        BlockchainUtils blockchainUtil = new BlockchainUtils();
        BigInteger latestBlock = blockchainUtil.getLatestBlockNumber();
        System.out.println("Listening from block number: " + latestBlock);

        Utils util = new Utils();
        ProcessTemplate contract = blockchainUtil.loadContract(address);
        System.out.println("Listening at address: " + contract.getContractAddress());
        contract.newRuleEventFlowable(DefaultBlockParameter.valueOf(latestBlock), DefaultBlockParameterName.PENDING).
                subscribe( (eventResponse) -> {
                    System.out.println("Listening from block number: " + latestBlock);
                    int checkLastEvent = lastRuleBlockNumber.compareTo(eventResponse.log.getBlockNumber());
                    if( checkLastEvent == -1) {
                        lastRuleBlockNumber = eventResponse.log.getBlockNumber();
                        //getting ids and rules from the event
                        ArrayList messageId = (ArrayList) eventResponse.messageId;
                        ArrayList rules = (ArrayList) eventResponse.rule;
                        //creating the final string list for ids and rules
                        List<String> idList = new ArrayList<>();
                        List<String> ruleList = new ArrayList<>();
                        //for each id cast id and rule first to byte then to string and finally add them to the lists
                        for(int i = 0; i < messageId.size(); i++) {
                            byte[] idByte = ((Utf8String) messageId.get(i)).getValue().getBytes(StandardCharsets.UTF_8);
                            byte[] ruleByte = ((Utf8String) rules.get(i)).getValue().getBytes(StandardCharsets.UTF_8);
                            String idString = new String(idByte, StandardCharsets.UTF_8);
                            String ruleString = new String(ruleByte, StandardCharsets.UTF_8);
                            idList.add(idString);
                            ruleList.add(ruleString);
                            System.out.println("id: " + idString);
                            System.out.println("rule: " + ruleString);

                        }
                       // util.insertRules(ruleList);
                    }
                });
    }


    public void subToMessages(String address) throws Exception {
        Utils u = new Utils();
        BlockchainUtils blockchainUtil = new BlockchainUtils();
        ProcessTemplate contract = blockchainUtil.loadContract(address);
        BigInteger latestBlock = blockchainUtil.getLatestBlockNumber();
        System.out.println("Listening from block number: " + latestBlock);
        contract.messageExecuteEventFlowable(DefaultBlockParameter.valueOf(latestBlock), DefaultBlockParameterName.LATEST).
                subscribe( (eventResponse) -> {
                    System.out.println("Listening from block number: " + latestBlock);
                    int checkLastEvent = lastEventBlockNumber.compareTo(eventResponse.log.getBlockNumber());
                    if( checkLastEvent == -1) {
                        lastEventBlockNumber = eventResponse.log.getBlockNumber();
                        String messageId = eventResponse.messageId;
                        ArrayList inputs = (ArrayList) eventResponse.inputs;
                        List<String> stringList = new ArrayList<>();
                        for(int i = 0; i < inputs.size(); i++) {
                            byte[] byteValue = ((Utf8String) inputs.get(i)).getValue().getBytes(StandardCharsets.UTF_8);
                            String stringValue = new String(byteValue, StandardCharsets.UTF_8);
                            stringList.add(stringValue);
                            System.out.println("valore in stringa: " + stringValue);
                        }
                        u.insertRules(blockchainUtil.getRule(messageId));
                        System.out.println(lastEventBlockNumber);
                        blockchainUtil.setMessageInputs(stringList);
                        blockchainUtil.getRule(messageId);
                        //System.out.println(blockchainUtil.getSingleInput(1));
                        executeMessage(blockchainUtil);
                    }
                });
    }

    public void executeMessage(BlockchainUtils blockchainUtil){
    	
    	 KieServices ks = KieServices.Factory.get();
         KieRepository kr = ks.getRepository();
         KieFileSystem kfs = ks.newKieFileSystem();
     
  
         try {
			kfs.write("src/main/resources/r1.drl", new String ( Files.readAllBytes( Paths.get(Utils.ruleFile.getAbsolutePath()))));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
         // Add KieFileSystem to KieBuilder
         KieBuilder kb = ks.newKieBuilder(kfs);
  
                 
         kb.buildAll();
         
         
         
         if (kb.getResults().hasMessages(Message.Level.ERROR)) {
             throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
         }
  
        
          
    	
    
         KieContainer kc=ks.newKieContainer(kr.getDefaultReleaseId());
      
        KieSession kSession = kc.newKieSession();
    

        kSession.insert( blockchainUtil );

        kSession.fireAllRules();
        kSession.dispose();
        System.out.println("Rules executed");
        
 
        
        
    }

    public void reviewexecuteMessage(BlockchainUtils blockchainUtil) {



        KieServices ks = KieServices.Factory.get();
        System.out.println("ks: "+ ks);
        //File f = new File("META-INF/kmodule.xml");
        //ClassLoader c = f.getClass().getClassLoader();
        //KieContainer kc = ks.getKieClasspathContainer(this.getClass().getClassLoader());
        //KieContainer kc = ks.getKieClasspathContainer();
        KieContainer kc = ks.newKieClasspathContainer();
        Results result = kc.verify();
        if(!result.hasMessages(Message.Level.ERROR)){
            KieSession ksession = kc.newKieSession("HelloWorldKS");
            ksession.insert(blockchainUtil);
            System.out.println(result);
            ksession.fireAllRules();
            System.out.println("Rules executed");
            ksession.dispose();
        }else
            System.out.println(result);

    }
}
