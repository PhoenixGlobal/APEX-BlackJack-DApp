package app.controller;

import app.App;
import app.util.TransactionUtil;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.util.*;

@SuppressWarnings("unchecked")
@Controller
@RequestMapping("/game")
public class MainController {

    private Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private TransactionUtil transactionUtil;

    @GetMapping
    public String indexPage(Model model) throws Exception {

        final String address = (String) httpSession.getAttribute("address");
        final ECPrivateKey privateKey = (ECPrivateKey) httpSession.getAttribute("privateKey");

        HashMap<Integer,  ArrayList<String>> cardMap = (HashMap<Integer,  ArrayList<String>>) httpSession.getAttribute("cardMap");
        if(cardMap == null || cardMap.isEmpty()) {
            cardMap = getCardMap();
        }

        model.addAttribute("addressPlayer", address);
        model.addAttribute("addressContract", App.getGameAddress());
        HashMap<String, Object> accountMap = transactionUtil.getAccount(address);

        model.addAttribute("balancePlayer", transactionUtil.getAccountBalance(address));
        model.addAttribute("balanceContract", 0);

        final String tableTxId = transactionUtil.executeMethod(privateKey, transactionUtil.getAccountNonce(address), 10, 0);
        HashMap<String, Object> displayTableMap =  transactionUtil.getTxById(tableTxId);
        log.info(displayTableMap.toString());
        while(displayTableMap.isEmpty() || displayTableMap.get("output").equals("") || displayTableMap.get("output") == null){
            Thread.sleep(100L);
            displayTableMap = transactionUtil.getTxById(tableTxId);
        }
        String tableEncoded = (String) displayTableMap.get("output");
        String msg = "Welcome to APEX BlackJack";

        try {

            msg = new String(Hex.decodeHex(tableEncoded.substring(tableEncoded.length() - 128)));
            model.addAttribute("msg", transactionUtil.cleanTextContent(msg));

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
            if(idDCN1 == 0) {
                httpSession.setAttribute("dealerCardNext1", "svg-cards/back.svg");
            } else if(httpSession.getAttribute("dealerCardNext1").equals("svg-cards/back.svg")){
                int ran = new Random().nextInt(DCN1List.size());
                httpSession.setAttribute("dealerCardNext1", DCN1List.get(ran));
                DCN1List.remove(ran);
                cardMap.put(idDCN1, DCN1List);
            }

            int idDCN2 = Integer.valueOf(tableValues.get(10), 16);
            ArrayList<String> DCN2List = cardMap.get(idDCN2);
            if(idDCN2 == 0) {
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
            log.error(e.getMessage());
            model.addAttribute("msg", msg);
        }

        return "index";
    }

    @PostMapping(params = "action=cashIn")
    public String cashIn(@RequestParam("amount") double amount) {
        final String address = (String) httpSession.getAttribute("address");
        final ECPrivateKey privateKey = (ECPrivateKey) httpSession.getAttribute("privateKey");
        transactionUtil.executeMethod(privateKey, transactionUtil.getAccountNonce(address), 4, amount);
        return "redirect:/game";
    }

    @PostMapping(params = "action=cashOut")
    public String cashOut() {
        final String address = (String) httpSession.getAttribute("address");
        final ECPrivateKey privateKey = (ECPrivateKey) httpSession.getAttribute("privateKey");
        transactionUtil.executeMethod(privateKey, transactionUtil.getAccountNonce(address), 0, 0);
        return "redirect:/game";
    }

    @PostMapping(params = "action=hit")
    public String hit() {
        final String address = (String) httpSession.getAttribute("address");
        final ECPrivateKey privateKey = (ECPrivateKey) httpSession.getAttribute("privateKey");
        transactionUtil.executeMethod(privateKey, transactionUtil.getAccountNonce(address), 2, 0);
        return "redirect:/game";
    }

    @PostMapping(params = "action=stand")
    public String stand() {
        final String address = (String) httpSession.getAttribute("address");
        final ECPrivateKey privateKey = (ECPrivateKey) httpSession.getAttribute("privateKey");
        transactionUtil.executeMethod(privateKey, transactionUtil.getAccountNonce(address),8, 0);
        return "redirect:/game";
    }

    @PostMapping(params = "action=placeBet")
    public String placeBet(@RequestParam("bet") double bet) {
        httpSession.setAttribute("cardMap", getCardMap());
        final String address = (String) httpSession.getAttribute("address");
        final ECPrivateKey privateKey = (ECPrivateKey) httpSession.getAttribute("privateKey");
        transactionUtil.executeMethodWithParameters(privateKey, transactionUtil.getAccountNonce(address), 5, 0, bet);
        return "redirect:/game";
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
