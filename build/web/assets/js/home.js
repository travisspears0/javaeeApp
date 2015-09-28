$(document).ready(function(){
    
    /**
     * INITIAL ACTIONS
     * */
    
    (function(){
        $("#loginField").val(getLoginCookie());
        $("#loginField").focus();
    })();
    
    /**
     * VARIABLES
     * */
    
    var ws = false;
    var loggedIn = false;
    var connected = false ;
    var sections = ["login","rooms","room"];
    var currentSection = 0;
    var rooms = [];
    var messages = [];
    var MAX_MESSAGES = 20;
    var error = false;
    var alrt = false;
    var actions = ["+","*","next","pass","end"];
    var currentTimerPercent = 100;
    var currentAction = "";
    var showReadyButton = true;
    var showActionsDiv = false;
    
    /**
     * HELPING FUCTIONS
     * */
    
    function write(info) {
        console.log(info);
    }
    
    function sendToServer(data,type) {
        var type = ( typeof type === 'undefined' )? 'message' : type ;
        var info = JSON.stringify({
            type: type,
            data: data,
        });
        ws.send(info);
    }
    
    function switchSection(section,callback) {
        var closeSection = sections[currentSection];
        var openSection = sections[section];
        $("#"+ closeSection +"-wrapper").removeClass("fadeInLeft");
        $("#"+ closeSection +"-wrapper").addClass("fadeOutLeft");
        setTimeout(function(){
            $("#"+ closeSection +"-wrapper").addClass("zero-height");
            $("#" + openSection + "-wrapper").removeClass("zero-height");
            $("#" + openSection + "-wrapper").removeClass("fadeOutLeft");
            $("#" + openSection + "-wrapper").addClass("fadeInLeft");
        },300);
        if( typeof callback !== 'undefined' ) {
            callback();
        }
        currentSection = section;
    }
    
    function receiveFromServer(data) {
        var data = JSON.parse(data);
        var type = data['type'];
        var data = data['data'];
        switch(type) {
            case "loggedIn":
                switchSection(1,function(){
                    loggedIn = data;
                    $("#logged-in-as").html(data);
                    $("#chat-wrapper").css("height","200px");
                });
                break;
            case "rooms":
                var counter = 0;
                data = JSON.parse(data);
                if( $("#room-0").length ) {
                    for( var i in data ) {
                        var room = JSON.parse(data[i]);
                        $("#people-in-room-"+i).html(room['users'] +"/"+ room['slots']);
                    }
                    updateRooms();
                    return;
                }
                for( var i in data ) {
                    var room = JSON.parse(data[i]);
                    var roomName = room['name'];
                    var peopleInRoom = room['users'] +"/"+ room['slots'];
                    rooms[counter] = roomName;
                    
                    //DOM elements
                    var roomDiv = document.createElement("div");
                    roomDiv.className = "col-xs-6 col-md-4 col-lg-3 room";
                    roomDiv.id = "room-" + i;
                    
                    var titleDiv = document.createElement("div");
                    titleDiv.className = "col-xs-12 title";
                    $(titleDiv).html(roomName);
                    $(roomDiv).append(titleDiv);
                    
                    var numPeopleDiv = document.createElement("div");
                    numPeopleDiv.className = "col-xs-12";
                    $(numPeopleDiv).append("<div class='glyphicon glyphicon-user'></div>");
                    $(numPeopleDiv).append("<div id='people-in-room-"+ counter +"'>"+ peopleInRoom +"</div>");
                    $(roomDiv).append(numPeopleDiv);
                    
                    var joinButtonDiv = document.createElement("div");
                    joinButtonDiv.className = "col-xs-12 join-button";
                    //$(joinButtonDiv).append("<input type='button' id='join-button-"+ counter +"' class='join-button' value='join' />");
                    var joinButton = document.createElement("input");
                    joinButton.id = "join-button-"+counter;
                    $(joinButton).attr("value","join");
                    $(joinButton).attr("type","button");
                    $(joinButton).click(joinButtonClick);
                    $(joinButtonDiv).append(joinButton);
                    $(roomDiv).append(joinButtonDiv);
                    
                    $("#rooms-container").append(roomDiv);
                    ++counter;
                }
                updateRooms();
                break;
            case "joinedRoom":
                switchSection(2,function(){
                    $("#room-name").html(data);
                    $(".number-div").html("_");
                    $("#house-number-div").html("_");
                    setNumbers();
                });
                break;
            case "leftRoom":
                switchSection(1,function(){
                    showReadyButton = true;
                });
                break;
            case "loggedOut":
                switchSection(0,function(){
                    ws = false;
                    $("#chat-wrapper").css("height","0");
                    showActionsDiv = false;
                    showReadyButton = true;
                    $("#actions-div").css("display","none");
                    $("#ready-button").css("display","initial");
                    setTimer(0);
                });
                break;
            case "message":
                receiveMessage(data);
                break;
            case "room":
                data = JSON.parse(data);
                $("#room-div").empty();
                var readyButtonCreated = false;
                for( var i in data ) {
                    var userData = ( data[i] === 'empty' ) ? false : JSON.parse(data[i]);
                    var opacity = ( userData === false || userData['state'] === 'READY' ) ? 1 : .3 ;
                    var readyOrNot = ( opacity === 1 ) ? "" : "(not ready)" ;
                    var name = ( userData === false ) ? "empty" : userData['name'] ;
                    var points = ( userData === false ) ? "-" : userData['points'] ;
                    var currentNumber = ( userData === false ) ? "-" : userData['currentNumer'] ;
                    var thisUser = ( loggedIn === name );
                    var pickedAction = ( userData === false ) ? "-" : userData['action'] ;
                    pickedAction = ( typeof pickedAction === 'undefined' ) ? "" : pickedAction ;
                    var userColor = ( userData === false ) ? "rgba(0,0,0,.1)" : userData['color'] ;
                    var userStack = ( userData === false ) ? "-" : userData['numbersStack'] ;
                    var isBlind = ( userData === false ) ? false : userData['blind'] ;
                    
                    name += readyOrNot;
                    var slotDiv = document.createElement("div");
                    slotDiv.className = "col-xs-6 slot" ;
                    $(slotDiv).css("background",userColor);
                    
                    var nameDiv = document.createElement("div");
                    nameDiv.className = "slot-label" ;
                    $(nameDiv).html(name);
                    $(nameDiv).css("opacity",opacity);
                    $(slotDiv).append(nameDiv);
                    
                    var pickedActionDiv = document.createElement("div");
                    pickedActionDiv.className = "slot-label" ;
                    $(pickedActionDiv).html("ACTION: " + pickedAction);
                    $(slotDiv).append(pickedActionDiv);
                    
                    var pointsDiv = document.createElement("div");
                    pointsDiv.className = "slot-label" ;
                    $(pointsDiv).html("POINTS: " + points);
                    $(slotDiv).append(pointsDiv);
                    
                    var numbersSlotDiv = document.createElement("div");
                    numbersSlotDiv.className = "slot-label" ;
                    $(numbersSlotDiv).html("stack: " + userStack);
                    $(slotDiv).append(numbersSlotDiv);
                    
                    var currentNumberDiv = document.createElement("div");
                    currentNumberDiv.className = "slot-label" ;
                    $(currentNumberDiv).html("current number: " + currentNumber);
                    $(slotDiv).append(currentNumberDiv);
                    
                    if( isBlind ) {
                        var blindDiv = document.createElement("div");
                        blindDiv.id = "blind-div";
                        $(blindDiv).html("START");
                        $(slotDiv).append(blindDiv);
                    }
                    
                    if( thisUser ) {
                        currentAction = pickedAction;
                        var actionsDiv = document.createElement("div");
                        actionsDiv.className = "slot-label" ;
                        actionsDiv.id = "actions-div";
                        var display = ( showActionsDiv ) ? "initial" : "none" ;
                        $(actionsDiv).css("display",display);
                        for( var i in actions ) {
                            var action = actions[i];
                            var input = document.createElement("input");
                            $(input).attr("type","button");
                            $(input).addClass("pick-action-button");
                            $(input).attr("value",action);
                            (function(val){
                                $(input).click(function(){
                                    sendToServer(val,"action");
                                    $(".pick-action-button").css("color","#000");
                                    $(this).css("color","#F00");
                                });
                            })(action);
                            $(actionsDiv).append(input);
                        }
                        $(slotDiv).append(actionsDiv);

                        readyButtonCreated = document.createElement("input");
                        readyButtonCreated.id = "ready-button";
                        $(readyButtonCreated).attr("type","button");
                        var disp = ( showReadyButton ) ? "initial" : "none" ;
                        $(readyButtonCreated).css("display",disp);
                        $(readyButtonCreated).val("ready");
                        $(readyButtonCreated).click(function(){
                            sendToServer("ready","ready");
                        });
                        $(slotDiv).append(readyButtonCreated);
                    }
                    
                    $("#room-div").append(slotDiv);
                }
                break;
            case "error":
                $("#error-wrapper").html(data);
                $("#error-wrapper").css("top","0");
                if( error === false ) {
                    $("#error-wrapper").removeClass("fadeOutUp");
                    $("#error-wrapper").addClass("fadeInUp");
                    $("#error-wrapper").css("opacity","1");
                } else {
                    clearTimeout(error);
                }
                error = setTimeout(function(){
                    if( error === false ) {
                        return;
                    }
                    error = false;
                    $("#error-wrapper").removeClass("fadeInUp");
                    $("#error-wrapper").addClass("fadeOutUp");
                },2500);
                break;
            case "alert":
                displayAlert(data);
                break;
            case "gameState":
                data = JSON.parse(data);
                var numbers = data['numbers'];
                var house = data['house'];
                var status = data['status'];
                setNumbers(numbers);
                $("#house-number-div").html(house);
                
                var displayActionsDiv = ( status === "WAITING_FOR_USERS_ACTIONS" ) ?
                    "initial" : "none" ;
                $("#actions-div").css("display",displayActionsDiv);
                //write("game state: " + status);
                if( currentAction === "END" ) {
                    showActionsDiv = false;
                    showReadyButton = false;
                    $("#ready-button").css("display","none");
                    $("#actions-div").css("display","none");
                }
                switch( status ) {
                    case "WAITING_FOR_USERS_ACTIONS":
                        if( currentAction === "END" ) {
                            break;
                        }
                        displayAlert("pick the action");
                        showActionsDiv = true;
                        showReadyButton = false;
                        $("#actions-div").css("display","initial");
                        $("#ready-button").css("display","none");
                        break;
                    case "WAITING_FOR_NEXT_ROUND":
                        if( currentAction === "END" ) {
                            break;
                        }
                        displayAlert("wait for next round");
                        showActionsDiv = false;
                        showReadyButton = false;
                        $("#actions-div").css("display","none");
                        $("#ready-button").css("display","none");
                        break;
                    case "FINISHED":
                        showActionsDiv = false;
                        showReadyButton = true;
                        $("#actions-div").css("display","none");
                        $("#ready-button").css("display","initial");
                        $(".number-div").html("");
                        $("#house-number-div").html("");
                        receiveMessage("game ended");
                        break;
                }
                break;
            case "timer":
                data = data.split("/");
                var part = parseInt(data[0]);
                var max = parseInt(data[1]);
                setTimer(part/max*100);
                break;
            default :
                write("unrecognized data: " + data);
        }
    }
    
    function setNumbers(numbers) {
        $("#numbers-div").empty();
        if( typeof numbers === 'undefined' ) {
            return;
        }
        numbers = JSON.parse(numbers);
        var elementsToInsert = [];
        for( var i in numbers ) {
            var numberData = JSON.parse(numbers[i]);
            var value = numberData['value'];
            var color = numberData['color'];
            var numberDiv = document.createElement("div");
            numberDiv.id = "number-div-" + i ;
            $(numberDiv).addClass("col-xs-1");
            $(numberDiv).html(value);
            $(numberDiv).addClass("number-div");
            $(numberDiv).css("background",color);
            elementsToInsert[elementsToInsert.length] = numberDiv ;
        }
        for( var i=elementsToInsert.length-1 ; i>=0 ; --i ) {
            $("#numbers-div").append(elementsToInsert[i]);
        }
        var numberDiv = document.createElement("div");
        numberDiv.id = "number-div-arrow";
        $(numberDiv).addClass("col-xs-1");
        $(numberDiv).addClass("number-div");
        $(numberDiv).addClass("glyphicon");
        $(numberDiv).addClass("glyphicon-arrow-right");
        $("#numbers-div").append(numberDiv);
    }
    
    function receiveMessage(data) {
        messages[messages.length] = data;
        if( messages.length > MAX_MESSAGES ) {
            messages.shift();
        }
        updateMessages();
    }
    
    function setTimer(percent) {
        var percent = ( typeof percent === 'undefined' ) ? currentTimerPercent : percent ;
        currentTimerPercent = percent;
        var d = $("#timer-div").find("div")[0];
        var max = parseInt($("#timer-div").css("width"));
        $(d).css("width",(max*(percent/100))+"px");
    }
    
    function displayAlert(info) {
        if( !info.length ) {
            $("#alert-wrapper").css("display","none");
            clearTimeout(alrt);
            return;
        }
        if( alrt !== false ) {
            clearTimeout(alrt);
        }
        $("#alert-wrapper").html(info);
        $("#alert-wrapper").css("display","initial");
        alrt = setTimeout(function(){
            $("#alert-wrapper").css("display","none");
            alrt = false;
        },2000);
    }
    
    function updateMessages() {
        var scrollH = $("#chat-messages")[0].scrollHeight;
        var scrolldown = ($("#chat-messages").scrollTop()+$("#chat-messages").height() >= scrollH);
        $("#chat-messages").empty();
        for( var i in messages ) {
            var msgDiv = document.createElement("div");
            $(msgDiv).html(messages[i]);
            $("#chat-messages").append(msgDiv);
        }
        if( scrolldown ) {
            scrollH = $("#chat-messages")[0].scrollHeight;
            $("#chat-messages").scrollTop( scrollH );
        }
    }
    
    function updateRooms() {
        for( var i in rooms ) {
            var peopleInRoom = $("#people-in-room-"+i).html().split("/");
            var maxPeople = parseInt(peopleInRoom[1]);
            peopleInRoom = parseInt(peopleInRoom[0]);
            $("#join-button-"+i).css("visibility","visible");
            if( peopleInRoom >= maxPeople ) {
                //$("#join-button-"+i).css("visibility","hidden");
            }
        }
    }
    
    function setLoginCookie(login) {
        document.cookie = "login=" + login + ";expires=;path=/";
    }
    
    function getLoginCookie() {
        var cookies = document.cookie.split(";");
        for( var i=0,size=cookies.length ; i<size ; ++i ) {
            var cookie = cookies[i].split("=");
            if( cookie[0] === "login" ) return cookie[1];
        }
        return "";
    }
    
    /**
     * EVENTS
     * */
    
    $(window).resize(function(){
        setTimer();
    });
    
    $("#chat-send-button").click(function(){
        var msg = $("#chat-field").val();
        if( !msg.length ) {
            return;
        }
        $("#chat-field").val("");
        $("#chat-field").focus();
        sendToServer(msg);
    });
    
    $("#chat-field").keydown(function(e){
        var key = e.keyCode || e.which;
        if( key === 13 ) {
            $("#chat-send-button").click();
        }
    });
    
    $("#loginField").keydown(function(e){
        var key = e.keyCode || e.which;
        if( key === 13 ) {
            $("#loginButton").click();
        }
    });
    
    $("#logout-button").click(function(){
        sendToServer("logout","logout");
    });
    
    $("#loginButton").click(function(){
        var name = $("#loginField").val();
        if( !name.length ) {
            return;
        }
        if( ws === false ) {
            ws = new WebSocket("ws://localhost:8080/WebSocketsTest/server");
            wsAttachEvents(name);
        } else {
            login(name);
        }
    });
    
    $("#leave-room-button").click(function(){
        var currentRoom = $("#room-name").html();
        sendToServer(currentRoom,"leaveRoom");
    });
    
    function joinButtonClick(e) {
        var index = e.target.id.split("-")[2];
        sendToServer(rooms[index],"enterRoom")
    }
    
    /**
    * WEBSOCKETS CLIENT FUNCTIONS
    * */
   
    function login(name) {
        setLoginCookie(name);
        sendToServer(name,'login');
    }
    
    function wsAttachEvents(name) {

        ws.onopen = function() {
            connected = true;
            write("open");
            login(name);
        };

        ws.onmessage = function (e) {
            receiveFromServer(e.data);
        };

        ws.onclose = function() {
            connected = false;
            write("close");
        };
    }
    
    
});