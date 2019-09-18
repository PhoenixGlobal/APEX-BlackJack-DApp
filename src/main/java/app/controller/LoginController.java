package app.controller;

import crypto.CPXKey;
import crypto.CryptoService;
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

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private CryptoService cryptoService;

    @GetMapping
    public String startPageGet() {
        return "login";
    }

    @PostMapping(value = "login")
    public String startPagePost(@RequestParam(value = "privateKey") String privateKeyString) {
        try {
            final ECPrivateKey privKey;
            if(privateKeyString.startsWith("K") || privateKeyString.startsWith("L")) {
                privKey = cryptoService.getECPrivateKeyFromRawString(CPXKey.getRawFromWIF(privateKeyString));
            } else {
                privKey = cryptoService.getECPrivateKeyFromRawString(privateKeyString);
            }
            httpSession.setAttribute("address", CPXKey.getPublicAddressCPX(privKey));
            httpSession.setAttribute("privateKey", privKey);
            httpSession.setMaxInactiveInterval(SESSION_EXP_SEC);
            return "redirect:/game";
        } catch (Exception e){
            return "redirect:/";
        }
    }
}
