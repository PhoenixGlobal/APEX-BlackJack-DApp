package app.controller;

import app.App;
import crypto.CPXKey;
import crypto.CryptoService;
import message.request.cmd.GetAccountCmd;
import message.request.cmd.GetContractByIdCmd;
import message.request.cmd.SendRawTransactionCmd;
import message.transaction.FixedNumber;
import message.transaction.Transaction;
import message.transaction.TransactionType;
import message.util.RequestCallerService;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.solidity.Abi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.*;

@SuppressWarnings("unchecked")
@Controller
@RequestMapping("/game")
public class MainController {

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private RequestCallerService caller;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private JacksonJsonParser parser;

    @GetMapping
    public String indexPage(Model model) throws DecoderException {
        String address = (String) httpSession.getAttribute("address");

        HashMap<Integer,  ArrayList<String>> cardMap = (HashMap<Integer,  ArrayList<String>>) httpSession.getAttribute("cardMap");
        if(cardMap == null || cardMap.isEmpty()) {
            cardMap = getCardMap();
        }

        model.addAttribute("addressPlayer", address);
        model.addAttribute("addressContract", App.getGameAddress());
        model.addAttribute("balancePlayer", getAccountBalance(address).get("balance"));
        model.addAttribute("balanceContract", 0);

        String tableEncoded = displayTable();
        String msg = "Welcome to APEX BlackJack";
        try {

        msg = new String(Hex.decodeHex(tableEncoded.substring(tableEncoded.length() - 128)));
        model.addAttribute("msg", cleanTextContent(msg));

        tableEncoded = tableEncoded.substring(0, tableEncoded.length() - 128);
        ArrayList<String> tableValues = new ArrayList<>();
        while (!tableEncoded.equals("")){
            tableValues.add(tableEncoded.substring(0, 64));
            tableEncoded = tableEncoded.substring(64);
        }
        model.addAttribute("currentPot", new BigInteger(tableValues.get(1), 16)
                .divide(BigDecimal.valueOf(1000000000000000000L).toBigInteger()));


        int idPC1 = Integer.valueOf(tableValues.get(2), 16);
        ArrayList<String> PC1List = cardMap.get(idPC1);
        if(idPC1 == 0 || httpSession.getAttribute("playerCard1") == null) {
            httpSession.setAttribute("playerCard1", "svg-cards/back.svg");
        } else if(httpSession.getAttribute("playerCard1").equals("svg-cards/back.svg")) {
            int ran = new Random().nextInt(PC1List.size());
            httpSession.setAttribute("playerCard1", PC1List.get(ran));
            PC1List.remove(ran);
            cardMap.put(idPC1, PC1List);
        }

        int idPC2 = Integer.valueOf(tableValues.get(3), 16);
        ArrayList<String> PC2List = cardMap.get(idPC2);
        if(idPC2 == 0 || httpSession.getAttribute("playerCard2") == null) {
            httpSession.setAttribute("playerCard2", "svg-cards/back.svg");
        } else if(httpSession.getAttribute("playerCard2").equals("svg-cards/back.svg")){
            int ran = new Random().nextInt(PC2List.size());
            httpSession.setAttribute("playerCard2", PC2List.get(ran));
            PC2List.remove(ran);
            cardMap.put(idPC2, PC2List);
        }

        int idPN = Integer.valueOf(tableValues.get(4), 16);
        ArrayList<String> PNList = cardMap.get(idPN);
        if(idPN == 0 || httpSession.getAttribute("playerCardNext") == null) {
            httpSession.setAttribute("playerCardNext", "svg-cards/back.svg");
        } else if(httpSession.getAttribute("playerCardNext").equals("svg-cards/back.svg")){
            int ran = new Random().nextInt(PNList.size());
            httpSession.setAttribute("playerCardNext", PNList.get(ran));
            PNList.remove(ran);
            cardMap.put(idPN, PNList);
        }

        int idDC1 = Integer.valueOf(tableValues.get(7), 16);
        ArrayList<String> DC1List = cardMap.get(idDC1);
        if(idDC1 == 0 || httpSession.getAttribute("dealerCard1") == null) {
            httpSession.setAttribute("dealerCard1", "svg-cards/back.svg");
        } else if(httpSession.getAttribute("dealerCard1").equals("svg-cards/back.svg")){
            int ran = new Random().nextInt(DC1List.size());
            httpSession.setAttribute("dealerCard1", DC1List.get(ran));
            DC1List.remove(ran);
            cardMap.put(idDC1, DC1List);
        }

        int idDC2 = Integer.valueOf(tableValues.get(8), 16);
        ArrayList<String> DC2List = cardMap.get(idDC2);
        if(idDC2 == 0 || httpSession.getAttribute("dealerCard2") == null) {
            httpSession.setAttribute("dealerCard2", "svg-cards/back.svg");
        } else if(httpSession.getAttribute("dealerCard2").equals("svg-cards/back.svg")){
            int ran = new Random().nextInt(DC2List.size());
            httpSession.setAttribute("dealerCard2", DC2List.get(ran));
            DC2List.remove(ran);
            cardMap.put(idDC2, DC2List);
        }

        int idDCN1 = Integer.valueOf(tableValues.get(9), 16);
        ArrayList<String> DCN1List = cardMap.get(idDCN1);
        if(idDCN1 == 0 || httpSession.getAttribute("dealerCardNext1") == null) {
            httpSession.setAttribute("dealerCardNext1", "svg-cards/back.svg");
        } else if(httpSession.getAttribute("dealerCardNext1").equals("svg-cards/back.svg")){
            int ran = new Random().nextInt(DCN1List.size());
            httpSession.setAttribute("dealerCardNext1", DCN1List.get(ran));
            DCN1List.remove(ran);
            cardMap.put(idDCN1, DCN1List);
        }

        int idDCN2 = Integer.valueOf(tableValues.get(10), 16);
        ArrayList<String> DCN2List = cardMap.get(idDCN2);
        if(idDCN2 == 0 || httpSession.getAttribute("dealerCardNext2") == null) {
            httpSession.setAttribute("dealerCardNext2", "svg-cards/back.svg");
        } else if(httpSession.getAttribute("dealerCardNext2").equals("svg-cards/back.svg")){
            int ran = new Random().nextInt(DCN2List.size());
            httpSession.setAttribute("dealerCardNext2", DCN2List.get(ran));
            DCN2List.remove(ran);
            cardMap.put(idDCN2, DCN2List);
        }

        model.addAttribute("balanceContract", new BigInteger(tableValues.get(12), 16)
                .divide(BigDecimal.valueOf(1000000000000000000L).toBigInteger()));

        httpSession.setAttribute("cardMap", cardMap);
        } catch (IndexOutOfBoundsException e){
            model.addAttribute("msg", msg);
        }
        return "index";
    }

    @PostMapping(params = "action=cashIn")
    public String cashIn(@RequestParam("amount") double amount){
        executeMethod(8, amount);
        return "redirect:/game";
    }

    @PostMapping(params = "action=placeBet")
    public String placeBet(@RequestParam("bet") double bet){
        httpSession.setAttribute("playerCard1", null);
        httpSession.setAttribute("playerCard2", null);
        httpSession.setAttribute("playerCardNext", null);
        httpSession.setAttribute("dealerCard1", null);
        httpSession.setAttribute("dealerCard2", null);
        httpSession.setAttribute("dealerCardNext1", null);
        httpSession.setAttribute("dealerCardNext2", null);
        httpSession.setAttribute("cardMap", getCardMap());
        executeMethodWithParameters(0, 0, bet);
        return "redirect:/game";
    }

    @PostMapping(params = "action=cashOut")
    public String cashOut(){
        executeMethod(3, 0);
        return "redirect:/game";
    }

    @PostMapping(params = "action=showCards")
    public String showCards(){
        return "redirect:/game";
    }

    @PostMapping(params = "action=hit")
    public String hit(){
        executeMethod(1, 0);
        return "redirect:/game";
    }

    @PostMapping(params = "action=stand")
    public String stand(){
        executeMethod(7, 0);
        return "redirect:/game";
    }

    private String displayTable(){
        try {
            byte[] payContractSignature = Abi.fromJson(App.getContractAbi()).get(4).fingerprintSignature();
            String txId = executeTransaction(TransactionType.CALL, 0, payContractSignature);
            Thread.sleep(8000L);
            return getTransactionOutput(txId);
        } catch (InterruptedException e){
            return "";
        }
    }

    private String executeMethod(int abiIndex, double amount) {
        try {
            byte[] payContractSignature = Abi.fromJson(App.getContractAbi()).get(abiIndex).fingerprintSignature();
            String txId = executeTransaction(TransactionType.CALL, amount, payContractSignature);
            Thread.sleep(8000L);
            return getTransactionOutput(txId);
        } catch (InterruptedException e){
            return "";
        }
    }

    private String executeMethodWithParameters(int abiIndex, double amount, double param) {
        try {
            Abi.Function c = (Abi.Function) Abi.fromJson(App.getContractAbi()).get(abiIndex);
            byte [] params = c.encode(new FixedNumber(param).getValue());
            String txId = executeTransaction(TransactionType.CALL, amount, params);
            Thread.sleep(8000L);
            return getTransactionOutput(txId);
        } catch (InterruptedException e){
            return "";
        }
    }

    private String executeTransaction(byte txType, double amount, byte [] data){
        try{
            final Transaction tx = Transaction.builder()
                    .txType(txType)
                    .fromPubKeyHash(CPXKey.getScriptHash((ECPrivateKey) httpSession.getAttribute("privateKey")))
                    .toPubKeyHash(CPXKey.getScriptHashFromCPXAddress(App.getGameAddress()))
                    .amount(new FixedNumber(amount))
                    .nonce(Long.parseLong(getAccountBalance((String) httpSession.getAttribute("address")).get("nonce")))
                    .data(data)
                    .gasPrice(new FixedNumber(0.0000003))
                    .gasLimit(BigInteger.valueOf(300000L))
                    .version(1)
                    .executeTime(Instant.now().toEpochMilli())
                    .build();
            final SendRawTransactionCmd cmd = new SendRawTransactionCmd(tx.getBytes(cryptoService,
                    (ECPrivateKey) httpSession.getAttribute("privateKey")));
            final String response = caller.postRequest(App.getRpcUrl(), cmd);
            final Map<String, Object> responseMap = parser.parseMap(response);
            final LinkedHashMap<String, String> resultMap = (LinkedHashMap<String, String>) responseMap.get("result");
            if(resultMap.get("txId") != null){
                return resultMap.get("txId");
            }
        } catch (Exception e){
            return "";
        }
        return "";
    }

    private HashMap<String, String> getAccountBalance(String address){
        HashMap<String, String> accMap = new HashMap<>();
        accMap.put("balance", "0");
        accMap.put("nonce", "0");
        try {
            final GetAccountCmd cmd = new GetAccountCmd(address);
            final String responseString = caller.postRequest(App.getRpcUrl(), cmd);
            final Map<String, Object> response = parser.parseMap(responseString);
            final LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) response.get("result");
            if(resultMap.get("balance") != null)
            accMap.put("balance", String.valueOf(resultMap.get("balance")));
            if(resultMap.get("nextNonce") != null)
            accMap.put("nonce", String.valueOf(resultMap.get("nextNonce")));
            return accMap;
        } catch (Exception e){
            return accMap;
        }
    }

    private String getTransactionOutput(String txId){
        try {
            final GetContractByIdCmd cmd = new GetContractByIdCmd(txId);
            final Map<String, Object> response = parser.parseMap(caller.postRequest(App.getRpcUrl(), cmd));
            final LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) response.get("result");
            return String.valueOf(resultMap.get("output"));
        } catch (Exception e){
            return "";
        }
    }

    private static String cleanTextContent(String text) {
        // strips off all non-ASCII characters
        text = text.replaceAll("[^\\x00-\\x7F]", "");
        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        // removes non-printable characters from Unicode
        text = text.replaceAll("\\p{C}", "");
        return text.trim();
    }

    private HashMap<Integer, ArrayList<String>> getCardMap(){
        HashMap<Integer, ArrayList<String>> map = new HashMap<>();
        for(int i = 1; i < 14; i++){
            ArrayList<String> list;
            if(map.get(i) == null){
                list = new ArrayList<>();
            } else {
                list = map.get(i);
            }
            list.add("svg-cards/"+i+"c.svg");
            list.add("svg-cards/"+i+"d.svg");
            list.add("svg-cards/"+i+"h.svg");
            list.add("svg-cards/"+i+"s.svg");
            if(i >= 10){
                map.put(10, list);
            } else {
                map.put(i, list);
            }
        }
        ArrayList<String> listAce = new ArrayList<>();
        listAce.add("svg-cards/1c.svg");
        listAce.add("svg-cards/1d.svg");
        listAce.add("svg-cards/1h.svg");
        listAce.add("svg-cards/1s.svg");
        map.put(11, listAce);
        return map;
    }

}
