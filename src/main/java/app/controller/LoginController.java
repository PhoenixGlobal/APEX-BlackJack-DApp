package app.controller;

import app.util.TransactionUtil;
import crypto.CPXKey;
import crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.security.interfaces.ECPrivateKey;
import java.util.HashMap;

@Controller
@RequestMapping("/")
public class LoginController {

    private static final int SESSION_EXP_SEC = 1800;

    private Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private TransactionUtil transactionUtil;

    @GetMapping
    public String startPageGet() {
        return "login";
    }

    @PostMapping(value = "login")
    public String startPagePost(@RequestParam(value = "privateKey") String privateKeyString) {
        try {
            final ECPrivateKey privateKey;
            if(privateKeyString.startsWith("K") || privateKeyString.startsWith("L")) {
                privateKey = cryptoService.getECPrivateKeyFromRawString(CPXKey.getRawFromWIF(privateKeyString));
            } else {
                privateKey = cryptoService.getECPrivateKeyFromRawString(privateKeyString);
            }
            final String address = CPXKey.getPublicAddressCPX(privateKey);
            transactionUtil.executeMethod(privateKey, getAccountNonce(address), 6, 0L);
            Thread.sleep(2000L);
            httpSession.setAttribute("address", address);
            httpSession.setAttribute("privateKey", privateKey);
            httpSession.setMaxInactiveInterval(SESSION_EXP_SEC);
            return "redirect:/game";
        } catch (Exception e){
            log.error(e.getMessage());
            return "redirect:/";
        }
    }

    private long getAccountNonce(String address) throws InterruptedException {
        HashMap<String, Object> accountMap = transactionUtil.getAccountBalance(address);
        while (accountMap.isEmpty()){
            Thread.sleep(100L);
            accountMap = transactionUtil.getAccountBalance(address);
        }
        log.info(String.valueOf((long) accountMap.get("nextNonce")));
        return (long) accountMap.get("nextNonce");
    }

}
