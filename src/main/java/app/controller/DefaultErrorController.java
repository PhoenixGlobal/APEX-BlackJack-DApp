package app.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DefaultErrorController implements ErrorController {

    @RequestMapping(value = "/error")
    public ModelAndView returnError(){
        return new ModelAndView("login");
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}
