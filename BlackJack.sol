pragma solidity ^0.5.2;

//RULES:
//Split Under 21 Rule
//Modified split: If either of the player's hand
//in a split beats dealer, player wins bet on both hands automatically
//But if Player busts on either deck, Dealer wins bet on both decks.
//If player's first hand has a standoff with dealer, player's other hand
//must beat dealer, otherwise dealer wins
//If player's second hand stands off with dealer,
//player gets original bet back
//Player can either double down or split, player cannot split
//then double down and vice versa

// Reworked contract from: https://github.com/JayOfemi/BlackJack/blob/master/BlackJack.sol

contract BlackJack {

    mapping (address => address) playerMap;

    mapping (address => uint) nonceMap;

    mapping (address => bool) roundInProgressMap;

    mapping (address => bool) displayUpdateMap;

    mapping (address => bool) dDownMap;

    mapping (address => bool) insuranceMap;

    mapping (address => bool) insuredMap;

    mapping (address => bool) splitMap;

    mapping (address => bool) splittingMap;

    mapping (address => uint256) safeBalanceMap;

    mapping (address => uint256) origBalanceMap;

    mapping (address => uint256) splitCountMap;

    mapping (address => uint256) rngCounterMap;

    mapping (address => uint256) randNumMap;

    mapping (address => uint256) pBetMap;

    mapping (address => uint256) pCard1Map;

    mapping (address => uint256) pCard2Map;

    mapping (address => uint256) pNewCardMap;

    mapping (address => uint256) pCardTotalMap;

    mapping (address => uint256) pSplitTotalMap;

    mapping (address => uint256) dCard1Map;

    mapping (address => uint256) dCard2Map;

    mapping (address => uint256[2]) dNewCardMap;

    mapping (address => uint256) dCardTotalMap;

    mapping (address => uint256) gamesPlayedMap;

    mapping (address => string) dMsgMap;

    ///******constructor********///
    constructor() public payable {}

    ///--------Modifiers----------///
    //make sure address is Valid
    modifier isValidAddr() {
        require(msg.sender != address(0), "Invalid Address");
        _;
    }

    //make sure address is Player
    modifier isPlayer() {
        require(playerMap[msg.sender] != address(0), "Only Player can use this function");
        _;
    }

    //make sure address is Player
    modifier isNotPlayer() {
        require(playerMap[msg.sender] == address(0), "Only non registered addresses can use this function");
        _;
    }

    //make sure function can only be used while round in progress
    modifier playerTurn() {
        require(roundInProgressMap[msg.sender] == true, "This Function can only be used while round is in progress");
        _;
    }

    //make sure function cannot be used while round in progress
    modifier newRound() {
        require(roundInProgressMap[msg.sender] == false, "This Function cannot be used while round is in progress");
        _;
    }

    function registerPlayer() isNotPlayer public returns (string memory){
        playerMap[msg.sender] = msg.sender;
        roundInProgressMap[msg.sender] = false;
        rngCounterMap[msg.sender] = 1;
        gamesPlayedMap[msg.sender] = 0;
        pCard1Map[msg.sender] = 0;
        pCard2Map[msg.sender]= 0;
        pNewCardMap[msg.sender] = 0;
        pCardTotalMap[msg.sender] = 0;
        pSplitTotalMap[msg.sender] = 0;
        dCard1Map[msg.sender] = 0;
        dCard2Map[msg.sender] = 0;
        dNewCardMap[msg.sender][0] = 0;
        dNewCardMap[msg.sender][1] = 0;
        dCardTotalMap[msg.sender] = 0;
        dDownMap[msg.sender] = false;
        splitMap[msg.sender] = false;
        insuranceMap[msg.sender] = false;
        splitCountMap[msg.sender] = 0;
        dMsgMap[msg.sender] = "Registered successfully. Pay Contract";
        return dMsgMap[msg.sender];
    }

    ///************pay the contract*************///
    function payContract() isValidAddr isPlayer newRound public payable returns (string memory) {
        safeBalanceMap[msg.sender] += msg.value;
        origBalanceMap[msg.sender] += msg.value;
        dMsgMap[msg.sender] = "Contract Paid: Place bet";
        return dMsgMap[msg.sender];
    }


    ///*********************RNG************************///
    //Generates a random number from 0 to 13 based on the last block hash
    //11 = Joker, 12 = Queen, 13 = King, Each worth 10 points
    //counter is added to "now" so that RNG doesnt produce same number if called twice in the same second
    function RNG() internal returns (uint randomNumber) {
        uint ran = uint(keccak256(abi.encodePacked(now, msg.sender, nonceMap[msg.sender]))) % 13 + 1;
        nonceMap[msg.sender]++;
        //J, Q, K => 10
        if (ran > 10)
            ran = 10;
        return ran;
    }

    ///-------------Game Interface-----------------///

    ///***************Place a bet**************///
    function placeBet(uint256 bet) isValidAddr isPlayer newRound public returns (string memory) {
        uint256 betEth;

        //only reset player's bet if not a double down or split or insurance bet
        if (dDownMap[msg.sender] == false && splitMap[msg.sender] == false && insuranceMap[msg.sender] == false)
            pBetMap[msg.sender] = 0;

        betEth = bet;

        //make sure player can afford bet
        require(betEth <= safeBalanceMap[msg.sender], "Invalid Balance: Place bet");

        //update balance
        safeBalanceMap[msg.sender] -= betEth;

        //don't replace original bet with insurance bet
        if (insuranceMap[msg.sender] == false)
            pBetMap[msg.sender] += betEth;

        //start round
        roundInProgressMap[msg.sender] = true;

        //update game counter
        gamesPlayedMap[msg.sender] += 1;

        //only deal cards if this is not a Double Down or split or insurance bet
        if (dDownMap[msg.sender] == false && splitMap[msg.sender] == false && insuranceMap[msg.sender] == false)
            return deal();
        else {
            //make sure player can only insure once
            if (insuranceMap[msg.sender] == true)
                insuranceMap[msg.sender] = false;
            dMsgMap[msg.sender] = "Bet Placed: Player turn";
            return dMsgMap[msg.sender];
        }
    }


    ///***********Cash Out**************///
    function cashOut() isValidAddr isPlayer newRound
    public
    returns (string memory) {

        uint256 tempBalance = 0;
        origBalanceMap[msg.sender] = 0;
        tempBalance = safeBalanceMap[msg.sender];
        safeBalanceMap[msg.sender] = 0;

        address(msg.sender).transfer(tempBalance);
        dMsgMap[msg.sender] = "Cash out successful: Pay contract";
        return dMsgMap[msg.sender];
    }


    ///************deal cards**************///
    function deal() internal returns (string memory) {

        //clear previous hand
        pCard1Map[msg.sender] = 0;
        pCard2Map[msg.sender]= 0;
        pNewCardMap[msg.sender] = 0;
        pCardTotalMap[msg.sender] = 0;
        pSplitTotalMap[msg.sender] = 0;
        dCard1Map[msg.sender] = 0;
        dCard2Map[msg.sender] = 0;
        dNewCardMap[msg.sender][0] = 0;
        dNewCardMap[msg.sender][1] = 0;
        dCardTotalMap[msg.sender] = 0;
        dDownMap[msg.sender] = false;
        splitMap[msg.sender] = false;
        insuranceMap[msg.sender] = false;
        splitCountMap[msg.sender] = 0;


        //player card 1
        pCard1Map[msg.sender] = RNG();
        //Ace
        if (pCard1Map[msg.sender] == 1)
            pCard1Map[msg.sender] = 11;

        //dealer card 1
        dCard1Map[msg.sender] = RNG();

        //player card 2
        pCard2Map[msg.sender] = RNG();
        //Ace is 1 unless Player has a total less than 11
        if (pCard2Map[msg.sender] == 1 && pCard1Map[msg.sender] < 11) {
            //Ace = 11
            pCard2Map[msg.sender] = 11;
        }

        //player's total
        pCardTotalMap[msg.sender] = pCard1Map[msg.sender] + pCard2Map[msg.sender];

        //Insurance
        if (dCard1Map[msg.sender] == 1) {
            dCard1Map[msg.sender] = 11;
            insuranceMap[msg.sender] = true;
        }

        //dealer's total
        dCardTotalMap[msg.sender] = dCard1Map[msg.sender] + dCard2Map[msg.sender];


        //BlackJack - Natural (1.5*Bet returned to player)
        if (pCardTotalMap[msg.sender] == 21) {
            //if there might be a standoff
            if (dCard1Map[msg.sender] == 10) {
                //show dealer's second card
                dCard2Map[msg.sender] = RNG();
                //Ace is always 11 in this case
                if (dCard2Map[msg.sender] == 1)
                    dCard2Map[msg.sender] = 11;

                dCardTotalMap[msg.sender] = dCard1Map[msg.sender] + dCard2Map[msg.sender];
            }
            //choose winner
            if (dCardTotalMap[msg.sender] == pCardTotalMap[msg.sender]) {
                //update balance: bet
                dMsgMap[msg.sender] = "Standoff: Place bet";
                safeBalanceMap[msg.sender] += pBetMap[msg.sender];
                roundInProgressMap[msg.sender] = false;
            } else {
                dMsgMap[msg.sender] = "Player wins: Place bet";
                //update balance: bet * 2.5 = original bet + bet * 1.5
                safeBalanceMap[msg.sender] += ((pBetMap[msg.sender] * 2) + (pBetMap[msg.sender] / 2));
                roundInProgressMap[msg.sender] = false;
            }
        }
        //Normal turn
        else
            dMsgMap[msg.sender] = "Next turn: Player turn";

        //split
        if (pCard1Map[msg.sender] == pCard2Map[msg.sender]) {
            if (insuranceMap[msg.sender] == true)
                dMsgMap[msg.sender] = "Called spilt: Player turn";
            else
                dMsgMap[msg.sender] = "Cannot call spilt: Player turn";
            splitMap[msg.sender] = true;
        }

        //Double down - Reno Rule (9 or 10 or 11)
        if (pCardTotalMap[msg.sender] == 9 || pCardTotalMap[msg.sender] == 10 || pCardTotalMap[msg.sender] == 11) {
            dDownMap[msg.sender] = true;
            dMsgMap[msg.sender] = "Next turn: Player turn";
        }

        return dMsgMap[msg.sender];
    }


    ///***********************Hit*************************///
    function hit() isValidAddr isPlayer playerTurn public returns (string memory) {

        //handle double down, Insurance and Splitting
        dDownInsSplit();

        pNewCardMap[msg.sender] = RNG();
        //Ace is 1 unless Player has a total less than 11
        if (pNewCardMap[msg.sender] == 1 && pCardTotalMap[msg.sender] < 11) {
            //Ace = 11
            pNewCardMap[msg.sender] = 11;
        }

        //choose for 1st round winner during split
        if (splittingMap[msg.sender] == true) {
            pSplitTotalMap[msg.sender] += pNewCardMap[msg.sender];

            //handle hit Win
            hitWin(pSplitTotalMap[msg.sender]);

        } else {
            //choose winner for normal play or second round during split
            pCardTotalMap[msg.sender] += pNewCardMap[msg.sender];

            //handle hit win
            hitWin(pCardTotalMap[msg.sender]);
        }
        return dMsgMap[msg.sender];
    }


    ///*******************stand***********************///
    function stand() isValidAddr isPlayer playerTurn public returns (string memory) {

        //handle double down, Insurance and Splitting
        dDownInsSplit();

        //Dealer's turn
        if (splitCountMap[msg.sender] < 2) {
            //show Dealer Card 2
            dCard2Map[msg.sender] = RNG();
            //Ace
            if (dCard2Map[msg.sender] == 1 && dCard1Map[msg.sender] < 11) {
                //Ace = 11
                dCard2Map[msg.sender] = 11;
            }

            //update Dealer's card Total
            dCardTotalMap[msg.sender] = dCard1Map[msg.sender] + dCard2Map[msg.sender];

            uint256 dCardIndex = 0;
            //Dealer must Hit to 16 and Stand on all 17's
            while (dCardTotalMap[msg.sender] < 17) {
                dNewCardMap[msg.sender][dCardIndex] = RNG();
                //Ace
                if (dNewCardMap[msg.sender][dCardIndex] == 1 && dCardTotalMap[msg.sender] < 11) {
                    //Ace = 11
                    dNewCardMap[msg.sender][dCardIndex] = 11;
                }

                dCardTotalMap[msg.sender] += dNewCardMap[msg.sender][dCardIndex];
                dCardIndex += 1;
                if (dCardIndex > 1)
                    dCardIndex = 0;
            }
        }

        //choose winner
        if (dCardTotalMap[msg.sender] == 21) {
            //for double down play
            if (pCardTotalMap[msg.sender] == 21 || pSplitTotalMap[msg.sender] == 21) {
                dMsgMap[msg.sender] = "Standoff: Place bet";
                //update balance
                safeBalanceMap[msg.sender] += pBetMap[msg.sender];
            } else {
                if (splittingMap[msg.sender] == true) {
                    splitCountMap[msg.sender] += 1;
                    dMsgMap[msg.sender] = "Splitted pot: Player turn";
                }
                else {
                    dMsgMap[msg.sender] = "Dealer wins: Place bet";
                    roundInProgressMap[msg.sender] = false;
                    if (insuredMap[msg.sender] == true) {
                        insuredMap[msg.sender] = false;
                        //bet has doubled so insurance is 1/2 * bet
                        safeBalanceMap[msg.sender] += (pBetMap[msg.sender] / 2);
                    }
                }
            }


        } else if (dCardTotalMap[msg.sender] > 21) {
            if (splittingMap[msg.sender] == true) {
                splitCountMap[msg.sender] += 1;
                dMsgMap[msg.sender] = "Splitted pot: Player turn";
                //update balance
                safeBalanceMap[msg.sender] += (pBetMap[msg.sender] * 2);
            } else {
                dMsgMap[msg.sender] = "Dealer busted: Place bet";
                //update balance: bet * 2
                safeBalanceMap[msg.sender] += (pBetMap[msg.sender] * 2);
                roundInProgressMap[msg.sender] = false;
            }
        } else {
            if (pCardTotalMap[msg.sender] <= 21) {
                //if dealer wins
                if ((21 - dCardTotalMap[msg.sender]) < (21 - pCardTotalMap[msg.sender])) {
                    if (splittingMap[msg.sender] == true) {
                        splitCountMap[msg.sender] += 1;
                        dMsgMap[msg.sender] = "Next turn: Player turn";
                    } else {
                        dMsgMap[msg.sender] = "Dealer wins: Place bet";
                        roundInProgressMap[msg.sender] = false;
                    }
                    //if player wins
                } else if ((21 - dCardTotalMap[msg.sender]) > (21 - pCardTotalMap[msg.sender])) {
                    if (splittingMap[msg.sender] == true) {
                        splitCountMap[msg.sender] += 1;
                        dMsgMap[msg.sender] = "Next turn: Player turn";
                        //update balance
                        safeBalanceMap[msg.sender] += (pBetMap[msg.sender] * 2);
                    } else {
                        dMsgMap[msg.sender] = "Player wins: Place bet";
                        //update balance: bet * 2
                        safeBalanceMap[msg.sender] += (pBetMap[msg.sender] * 2);
                        roundInProgressMap[msg.sender] = false;
                    }
                    //if its a standoff
                } else {
                    if (splittingMap[msg.sender] == true) {
                        splitCountMap[msg.sender] += 1;
                        dMsgMap[msg.sender] = "Next turn: Player turn";
                        //update balance
                        safeBalanceMap[msg.sender] += pBetMap[msg.sender];
                    } else {
                        dMsgMap[msg.sender] = "Standoff: Place bet";
                        //end round
                        roundInProgressMap[msg.sender] = false;
                        //update balance: bet
                        safeBalanceMap[msg.sender] += pBetMap[msg.sender];
                    }
                }
                //player card can only be greater than 21 on double down hand
            } else {
                dMsgMap[msg.sender] = "Player busted: Place bet";
            }
        }

        return dMsgMap[msg.sender];
    }


    ///*********************Double Down*************************///
    function doubleDown() isValidAddr isPlayer playerTurn
    public returns (string memory) {
        //make sure player can double down
        require(dDownMap[msg.sender] == true, "Player cannot Double Down right now: Player turn");

        //if player has a chance to split but doubles down
        if (splitMap[msg.sender] == true) {
            //remove chance to split
            splitMap[msg.sender] = false;
        }
        //if player has a chance to get insurance but doesn't
        if (insuranceMap[msg.sender] == true) {
            //remove chance to get insurance
            insuranceMap[msg.sender] = false;
        }

        //place same amount as original Bet
        uint256 bet = pBetMap[msg.sender];

        //pause game to place Bet
        roundInProgressMap[msg.sender] = false;

        //place Bet and resume game
        placeBet(bet);

        //deal extra card
        pNewCardMap[msg.sender] = RNG();
        //Ace is 1 unless Player has a total less than 11
        if (pNewCardMap[msg.sender] == 1 && pCardTotalMap[msg.sender] < 11) {
            //Ace = 11
            pNewCardMap[msg.sender] = 11;
        }

        //update player's card total
        pCardTotalMap[msg.sender] += pNewCardMap[msg.sender];

        //let dealer finish his hand and end round
        return stand();
    }


    ///************************Split*****************************///
    function split() public isValidAddr isPlayer playerTurn {
        //make sure player can double down
        require(splitMap[msg.sender] == true, "Player cannot Split right now: Player turn");

        //if player has a chance to double down but splits
        if (dDownMap[msg.sender] == true) {
            //remove chance to double down
            dDownMap[msg.sender] = false;
        }
        //if player has a chance to get insurance but doesn't
        if (insuranceMap[msg.sender] == true) {
            //remove chance to get insurance
            insuranceMap[msg.sender] = false;
        }

        //update balances
        if (pCard1Map[msg.sender] == 11) {
            pCardTotalMap[msg.sender] = 11;
            pSplitTotalMap[msg.sender] = 11;
        }
        else {
            pCardTotalMap[msg.sender] = pCardTotalMap[msg.sender] / 2;
            pSplitTotalMap[msg.sender] = pCardTotalMap[msg.sender];
        }

        //place same amount as original Bet
        uint256 bet = pBetMap[msg.sender];

        //pause game to place Bet
        roundInProgressMap[msg.sender] = false;

        //place Bet and resume game
        placeBet(bet);

        //turn splitting on
        splittingMap[msg.sender] = true;

        //turn chance to split again off
        splitMap[msg.sender] = false;

        //If player's cards are both Aces
        if (pCard1Map[msg.sender] == 11) {
            //deal only one more card for card 1
            pNewCardMap[msg.sender] = RNG();
            //Ace is always 1 in this case

            //update split card total
            pSplitTotalMap[msg.sender] += pNewCardMap[msg.sender];

            //then stand
            stand();

            //turn splitting off
            splittingMap[msg.sender] = false;
            //make sure dealer doesn't draw again
            splitCountMap[msg.sender] = 2;

            //deal only one more card for card 2
            pNewCardMap[msg.sender] = RNG();
            //Ace is always 1 in this case

            //update player split total
            pCardTotalMap[msg.sender] += pNewCardMap[msg.sender];

            //then stand
            stand();
        }
    }


    ///********************Insurance*********************///
    function insurance() public isValidAddr isPlayer playerTurn {
        //make sure player can have insurance
        require(insuranceMap[msg.sender] == true, "Player cannot have insurance right now: Player turn");

        //place half amount as original Bet
        uint256 bet = pBetMap[msg.sender] / 2;

        //insure
        insuredMap[msg.sender] = true;

        //pause game to place Bet
        roundInProgressMap[msg.sender] = false;

        //place Bet and resume game
        placeBet(bet);
    }

    ///***************dDownInsSplit***************///
    //handle double down, Insurance and split chances
    //for hit and stand functions
    function dDownInsSplit() internal {
        //if player has a chance to double down but hits
        if (dDownMap[msg.sender] == true) {
            //remove chance to double down
            dDownMap[msg.sender] = false;
        }

        //if player has a chance to split
        if (splitMap[msg.sender] == true || splittingMap[msg.sender] == true) {
            if (splitCountMap[msg.sender] >= 2) {
                //remove chance to split after splitting
                splittingMap[msg.sender] = false;
                splitMap[msg.sender] = false;
            }
            else if (splittingMap[msg.sender] == true) {
                //start split counter if player is splitting
                splitCountMap[msg.sender] = 1;
            } else {

                //if not splitting, remove chance to split
                splitMap[msg.sender] = false;
            }
        }

        //if player has a chance to get insurance but hits
        if (insuranceMap[msg.sender] == true) {
            //remove chance to get insurance
            insuranceMap[msg.sender] = false;
        }
    }

    ///***************hitWin*****************///
    //handle checking for winner for hit function
    function hitWin(uint256 _cTotal) internal {

        //BlackJack or bust
        if (_cTotal == 21) {
            //if there might be a standoff
            if (dCard1Map[msg.sender] >= 10) {
                //show dealer's second card
                dCard2Map[msg.sender] = RNG();
                //update dealer card total
                dCardTotalMap[msg.sender] = dCard1Map[msg.sender] + dCard2Map[msg.sender];
            }
            //choose winner
            if (dCardTotalMap[msg.sender] == _cTotal) {
                dMsgMap[msg.sender] = "Standoff: Place bet";
                //update balance
                if (insuredMap[msg.sender] == true) {
                    insuredMap[msg.sender] = false;
                    safeBalanceMap[msg.sender] += (pBetMap[msg.sender] / 2);
                }
                safeBalanceMap[msg.sender] += pBetMap[msg.sender];
                roundInProgressMap[msg.sender] = false;
            } else {
                dMsgMap[msg.sender] = "Player Wins: Place bet";
                //update balance: bet * 2
                safeBalanceMap[msg.sender] += (pBetMap[msg.sender] * 2);
                roundInProgressMap[msg.sender] = false;
            }
        } else if (_cTotal > 21) {
            dMsgMap[msg.sender] = "Player busted: Place bet";

            //if player was insured
            if (insuredMap[msg.sender] == true) {
                insuredMap[msg.sender] = false;
                //show dealer's second card
                dCard2Map[msg.sender] = RNG();
                //update dealer card total
                dCardTotalMap[msg.sender] = dCard1Map[msg.sender] + dCard2Map[msg.sender];
                //update balance
                if (dCardTotalMap[msg.sender] == 21)
                    safeBalanceMap[msg.sender] += pBetMap[msg.sender];
            }
            roundInProgressMap[msg.sender] = false;
        } else {
            dMsgMap[msg.sender] = "Next turn: Player turn";
        }
    }

    ///********************show the table***********************///
    function displayTable()
    public isPlayer isValidAddr
    view
    returns (string memory Message, uint256 PlayerBet, uint256 PlayerCard1, uint256 PlayerCard2,
        uint256 PlayerNewCard, uint256 PlayerCardTotal, uint256 PlayerSplitTotal,
        uint256 DealerCard1, uint256 DealerCard2, uint256 DealerNewCard1,
        uint256 DealerNewCard2, uint256 DealerCardTotal, uint256 Pot) {
        return (dMsgMap[msg.sender], pBetMap[msg.sender], pCard1Map[msg.sender], pCard2Map[msg.sender], pNewCardMap[msg.sender],
        pCardTotalMap[msg.sender], pSplitTotalMap[msg.sender], dCard1Map [msg.sender], dCard2Map [msg.sender], dNewCardMap[msg.sender][0],
        dNewCardMap[msg.sender][1], dCardTotalMap[msg.sender], safeBalanceMap[msg.sender]);
    }

}