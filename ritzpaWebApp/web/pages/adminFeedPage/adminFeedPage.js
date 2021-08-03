var chatVersion = 0;
var refreshRate = 2000; //milli seconds
var USER_LIST_URL = buildUrlWithContextPath("userslist");
var STOCK_LIST_URL = buildUrlWithContextPath("stockslist");
var CHAT_LIST_URL = buildUrlWithContextPath("chat");

// handle chat messages
$(function() { // onload...do
    //add a function to the submit event
    $("#chatform").submit(function() {
        $.ajax({
            data: $(this).serialize(),
            url: this.action,
            method: 'POST',
            timeout: 2000,
            error: function() {
                console.error("Failed to submit message!");
            },
            success: function(r) {
            }
        });
        $("#userstring").val("");
        return false;
    });
});

//call the server and get the chat version
//we also send it the current chat version so in case there was a change
//in the chat content, we will get the new string as well
function ajaxChatContent() {
    $.ajax({
        url: CHAT_LIST_URL,
        data: "chatversion=" + chatVersion,
        dataType: 'json',
        timeout: 3000,
        success: function(data) {
            console.log("Server chat version: " + data.version + ", Current chat version: " + chatVersion);
            if (data.version !== chatVersion) {
                chatVersion = data.version;
                appendToChatArea(data.entries);
            }
            triggerAjaxChatContent();
        },
        error: function(error) {
            triggerAjaxChatContent();
        }
    });
}

//users = a list of usernames, essentially an array of javascript strings:
function refreshUsersList(users) {
    var usersList =  $("#userslist");
    //clear all current users
    usersList.empty();

    for(let i=0; i<users.length;i++){
        console.log("Adding user #" + i + ": " + users[i].name);
        if (users[i].type === "broker") $('<li>' + users[i].name + " is a " + users[i].type + '</li>').appendTo(usersList);
        else $('<li>' + users[i].name + " is an " + users[i].type + '</li>').appendTo(usersList);
    }
}

//stocks = a list of stockDTO, essentially an array of javascript strings:
function refreshStocksList(stocks) {
    var stocksList =  $("#stockstable");
    var oldBody =  $("#stockTablesBody");
    oldBody.empty();

    for(let i=0; i<stocks.length;i++){
        console.log("Adding stock #" + i + ": " + stocks[i].symbol);
        var index = i.valueOf()+1;

        //create a new <li> tag with a value in it and append it to the #userslist (div with id=userslist) element
        $('<tr>' +
            '<td>' + index +  '</td>' +
            '<td>' + stocks[i].company +  '</td>' +
            '<td>' + stocks[i].symbol +  '</td>' +
            '<td>' + stocks[i].value + "$" + '</td>' +
            '<td>' + stocks[i].totalCycle + "$" +  '</td>' +
            '<td>' + stocks[i].transactionsSize  +  '</td>' +
            '<td><a onclick="handleStockPage(\'' + stocks[i].symbol + '\')">' + 'Go To Stock Page' +  '</a></td>' +
            '</tr>')
            .appendTo(stocksList);
    }
}

function handleStockPage(stockName) {
    window.location = '../adminStockProfile/adminStockProfile.html?stock=' + stockName;
}

//entries = the added chat strings represented as a single string
function appendToChatArea(entries) {
//    $("#chatarea").children(".success").removeClass("success");

    // add the relevant entries
    $.each(entries || [], appendChatEntry);

    // handle the scroller to auto scroll to the end of the chat area
    var scroller = $("#chatarea");
    var height = scroller[0].scrollHeight - $(scroller).height();
    $(scroller).stop().animate({ scrollTop: height }, "slow");
}

function appendChatEntry(index, entry){
    var entryElement = createChatEntry(entry);
    $("#chatarea").append(entryElement).append("<br>");
}

function createChatEntry (entry){
    entry.chatString = entry.chatString.replace (":)", "<img class='smiley-image' src='../../common/images/smiley.png'/>");
    return $("<span class=\"success\">").append(entry.username + "> " + entry.chatString);
}

function ajaxUsersList() {
    $.ajax({
        url: USER_LIST_URL,
        success: function(users) {
            refreshUsersList(users);
        }
    });
}

function ajaxStocksList() {
    $.ajax({
        url: STOCK_LIST_URL,
        success: function(users) {
            refreshStocksList(users);
        }
    });
}

//function on load - updates the data we see evrey 2 sec
function triggerAjaxChatContent() {
    setTimeout(ajaxChatContent, refreshRate);
}

//activate the timer calls after the page is loaded
$(function() {

    //The users list is refreshed automatically every second
    setInterval(ajaxUsersList, refreshRate);

    //The chat content is refreshed only once (using a timeout) but
    //on each call it triggers another execution of itself later (1 second later)
    triggerAjaxChatContent();

    // update stock list
    setInterval(ajaxStocksList, refreshRate);
});
