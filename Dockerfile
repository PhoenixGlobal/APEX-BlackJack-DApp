FROM adoptopenjdk/openjdk12:jdk-12.0.1_12-slim

ARG RPC_URL
ARG CONTRACT

RUN     mkdir -p /dapp
WORKDIR /dapp

RUN     apt-get update -y && \
        apt-get install git -y && \
        apt-get install wget -y && \
        apt-get install maven -y

RUN     git clone https://github.com/yuomii/bj-dapp.git && \
        cd bj-dapp && \
        mvn -U clean install && \
        cp target/bj-dapp-1.0-SNAPSHOT.jar /dapp/app.jar && \
        cp BlackJackAbi.json /dapp/BlackJackAbi.json

RUN echo "java -jar /dapp/app.jar ${RPC_URL} ${CONTRACT}" > entrypoint.sh && \
    chmod +x entrypoint.sh

CMD ["/bin/bash", "entrypoint.sh"]