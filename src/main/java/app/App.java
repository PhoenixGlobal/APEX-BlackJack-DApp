package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@Import(AppInitializer.class)
public class App {

    private static String rpcUrl;
    private static String gameAddress;
    private static String contractAbi;

    public static void main(String[] args) throws IOException {
        rpcUrl = args[0];
        gameAddress = args[1];
        contractAbi = new String(Files.readAllBytes(Paths.get("BlackJackAbi.json")));
        SpringApplication.run(App.class, args);
    }

    public static String getRpcUrl() {
        return rpcUrl;
    }

    public static String getGameAddress() {
        return gameAddress;
    }

    public static String getContractAbi() {
        return contractAbi;
    }
}
