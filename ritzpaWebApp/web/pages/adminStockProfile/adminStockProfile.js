var chatVersion = 0;
var refreshRate = 2000; //milli seconds
var STOCK_PROFILE_URL = buildUrlWithContextPath("stockPage");
var USER_LIST_URL = buildUrlWithContextPath("userslist");
var CHAT_LIST_URL = buildUrlWithContextPath("chat");

//**** JQUERY ****

$(document).ready(function() {
    $("#pageProfile").text((new URL(document.location)).searchParams.get('stock') + "'s Profile ");
    refreshChart();
});


// handle chat messages
$(function() { // onload...do
    //add a function to the submit event
    $("#chatform").submit(function() {
        $.ajax({
            method: 'POST',
            data: $(this).serialize(),
            url: this.action,
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


$(function() {
    $("#goBack").click(function() {
        window.location.href = "../adminFeedPage/adminFeedPage.html";
    })
});


//activate the timer calls after the page is loaded
$(function() {
    setInterval(ajaxStockPage, refreshRate);
});

function refreshPurchasesTable(purchases) {
    var purchasesTable =  $("#pendingBuyTable");
    var oldBody =  $("#pendingBuyTableBody");
    oldBody.empty();

    for(let i=0; i<purchases.length;i++){
        console.log("Adding transaction #" + i + ": ");
        var index = i.valueOf()+1;
        //create a new <li> tag with a value in it and append it to the #userslist (div with id=userslist) element
        $('<tr xmlns="http://www.w3.org/1999/html">' +
            '<td>' + index +  '</td>' +
            '<td>' + purchases[i].buyerName +  '</td>' +
            '<td>' + purchases[i].timeStamp +  '</td>' +
            '<td>' + purchases[i].quantity +  '</td>' +
            '<td>' + purchases[i].value + '$' +  '</td>' +
            '</tr>')
            .appendTo(purchasesTable);
    }
}

function refreshSellsTable(sells) {
    var sellsTable =  $("#pendingSellTable");
    var oldBody =  $("#pendingSellTableBody");
    oldBody.empty();

    for(let i=0; i< sells.length;i++){
        console.log("Adding transaction #" + i + ": ");
        var index = i.valueOf()+1;
        //create a new <li> tag with a value in it and append it to the #userslist (div with id=userslist) element
        $('<tr xmlns="http://www.w3.org/1999/html">' +
            '<td>' + index +  '</td>' +
            '<td>' + sells[i].sellerName +  '</td>' +
            '<td>' + sells[i].timeStamp +  '</td>' +
            '<td>' + sells[i].quantity +  '</td>' +
            '<td>' + sells[i].value + '$' +  '</td>' +
            '</tr>')
            .appendTo(sellsTable);
    }
}

function ajaxStockPage() {
    let params = (new URL(document.location)).searchParams;
    let stockName = params.get('stock');
    $.get(STOCK_PROFILE_URL, { stock : stockName}, function(stock) {
        refreshStockInformation(stock);
        refreshTransactionsTable(stock.allTransactions);
        refreshPurchasesTable(stock.purchases);
        refreshSellsTable(stock.sells);
    }, 'json');
}

function refreshStockInformation(stock) {
    console.log("Updating " + stock.symbol + " information");
    $("#StockInfoSymbol").text(" " + stock.symbol);

    $("#StockInfoCompany").text(" " + stock.company);

    $("#StockInfoValue").text(" " + stock.value + "$");

    $("#StockInfoCycle").text(" " + stock.totalCycle + "$");
}

function refreshTransactionsTable(transactions) {
    console.log("Updating transactions");
    var transactionsTable =  $("#transactionsTable");
    var oldBody =  $("#transactionsTableBody");
    oldBody.empty();
    var rowNum = $("#transactionsTableBody tr").length;

    var newRowNum = transactions.length;

    for(let i=0; i<transactions.length;i++){
        console.log("Adding transaction #" + i + ": ");
        var index = i.valueOf()+1;
        //create a new <li> tag with a value in it and append it to the #userslist (div with id=userslist) element
        $('<tr xmlns="http://www.w3.org/1999/html">' +
            '<td>' + index +  '</td>' +
            '<td>' + transactions[i].buyerName +  '</td>' +
            '<td>' + transactions[i].sellerName +  '</td>' +
            '<td>' + transactions[i].timeStamp +  '</td>' +
            '<td>' + transactions[i].quantity +  '</td>' +
            '<td>' + transactions[i].value + '$' + '</td>' +
            '</tr>')
            .appendTo(transactionsTable);
    }

    if (rowNum !== newRowNum){
        refreshChart();
    }

}

function refreshChart() {
    console.log("Refreshing Value Chart");
    let params = (new URL(document.location)).searchParams;
    let stockName = params.get('stock');
    $.get(STOCK_PROFILE_URL, {stock: stockName}, function (stock) {
        var labels = Object.keys(stock.valueHistory);
        labels.sort().reverse();
        for (var i = labels.length-1; i>=0; i--){
            key = labels[i];
            valueEntry = stock.valueHistory[key];
            updateChart(valueEntry, key);
        }
    });
}

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

//function on load - updates the data we see evrey 2 sec
function triggerAjaxChatContent() {
    setTimeout(ajaxChatContent, refreshRate);
}
$(function() {

    //The users list is refreshed automatically every second
    setInterval(ajaxUsersList, refreshRate);

    //The chat content is refreshed only once (using a timeout) but
    //on each call it triggers another execution of itself later (1 second later)
    triggerAjaxChatContent();

});



