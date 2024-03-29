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
            transactionUtil.executeMethod(privateKey, address, 6, 0L);
            httpSession.setAttribute("address", address);
            httpSession.setAttribute("privateKey", privateKey);
            httpSession.setAttribute("playerCard1", null);
            httpSession.setAttribute("playerCard2", null);
            httpSession.setAttribute("playerCardNext", null);
            httpSession.setAttribute("dealerCard1", null);
            httpSession.setAttribute("dealerCard2", null);
            httpSession.setAttribute("dealerCardNext1", null);
            httpSession.setAttribute("dealerCardNext2", null);
            httpSession.setAttribute("cardMap", transactionUtil.getCardMap());
            httpSession.setMaxInactiveInterval(SESSION_EXP_SEC);
            return "redirect:/game";
        } catch (Exception e){
            log.error(e.getMessage());
            return "redirect:/";
        }
    }

}
