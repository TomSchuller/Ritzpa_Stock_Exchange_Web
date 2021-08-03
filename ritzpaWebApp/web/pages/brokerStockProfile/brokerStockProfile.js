var chatVersion = 0;
var refreshRate = 2000; //milli seconds
var STOCK_PROFILE_URL = buildUrlWithContextPath("stockPage");
var TOTAL_QUANTITY_URL = buildUrlWithContextPath("totalQuantity");
var USER_LIST_URL = buildUrlWithContextPath("userslist");
var CHAT_LIST_URL = buildUrlWithContextPath("chat");
var ALERT_URL = buildUrlWithContextPath("transactionAlerts");


//**** JQUERY ****
function ajaxAlertContent() {
    $.ajax({
        url: ALERT_URL,
        success: function(data) {
            if (data !== "") {
                createAlert('Attention!','You have notifications!' , data, 'success', false, true, 'pageMessages');
            }
         },
        error: function(error) {

        }
    });
}

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
            },
            success: function(r) {
            }
        });
        $("#userstring").val("");
        return false;
    });
});

$(document).ready(function() {
    $("#pageProfile").text((new URL(document.location)).searchParams.get('stock') + "'s Profile ");
    refreshTransactionTotal();
    refreshChart();
});


$(function() {
    $("#goBack").click(function() {
        window.location.href = "../brokerFeedPage/brokerFeedPage.html";
    })
});


//activate the timer calls after the page is loaded
$(function() {
    setInterval(ajaxStockPage, 1000);
});

function ajaxStockPage() {
    let params = (new URL(document.location)).searchParams;
    let stockName = params.get('stock');
    $.get(STOCK_PROFILE_URL, { stock : stockName}, function(stock) {
        refreshStockInformation(stock);
        refreshTransactionsTable(stock.allTransactions);
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
    var rowNum = $("#transactionsTableBody tr").length;
    oldBody.empty();

    var newRowNum = transactions.length;

    for(let i=transactions.length-1; i>=0; --i){
        console.log("Adding transaction #" + i + ": ");
        var index = i.valueOf()+1;

        $('<tr xmlns="http://www.w3.org/1999/html">' +
            '<td>' + index +  '</td>' +
            '<td>' + transactions[i].timeStamp +  '</td>' +
            '<td>' + transactions[i].quantity +  '</td>' +
            '<td>' + transactions[i].value + "$" +  '</td>' +
            '</tr>')
            .appendTo(transactionsTable);
    }

    if (rowNum !== newRowNum){
        refreshChart();
    }
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
    setInterval(refreshUserQuantity, 1000);

    //The chat content is refreshed only once (using a timeout) but
    //on each call it triggers another execution of itself later (1 second later)
    triggerAjaxChatContent();
    setInterval(ajaxAlertContent, refreshRate);
});

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



/// **** FORM REFRESH AND VALIDATION ***

// Refresh the sliders amount
function refreshTransactionTotal() {
    $("#transactionTotal").text(($("#priceRange").val() * $("#amountRange").val()));
    $("#transactionTotal").text(($("#priceRange").val() * $("#amountRange").val()) + "$");
    $("#amount").text( $("#amountRange").val());
    $("#price").text($("#priceRange").val() + "$");
}

function refreshUserQuantity() {
    let params = (new URL(document.location)).searchParams;
    let stockName = params.get('stock');
    $.ajax({
        url: TOTAL_QUANTITY_URL,
        data: { stock : stockName},
        success: function(quantity) {
            if (quantity !== undefined){
                if (quantity === 0){
                    $("#userAmount").text("You don't own any stocks");
                }

                else {
                    $("#userAmount").text("You own: " + quantity + " stocks");
                }
            }
        },
        error: function (e) {
            $("#userAmount").text("You don't own any stocks");
        }
    });
}

// Submit Form
$(function() {
    $("#order").submit(function() {
        if (!$("input[name='transactionDirection']:checked").val()) {
            var message = "Error! Please choose a transaction direction:"  + '\n' + "Buy or Sell";
            createAlert('Attention!','',message,'danger',false,true,'pageMessages');
            document.getElementById("lmtChoice").checked = false;
            document.getElementById("mktChoice").checked = false;
            document.getElementById("fokChoice").checked = false;
            document.getElementById("iocChoice").checked = false;
            document.getElementById("buyChoice").checked = false;
            document.getElementById("sellChoice").checked = false;

            document.getElementById("amountRange").setAttribute("max", "1000");
            document.getElementById("amountRange").value = 500;
            document.getElementById("amount").innerHTML = "500";

            document.getElementById("priceRange").setAttribute("max", "1000");
            document.getElementById("priceRange").value = 500;
            document.getElementById("price").innerHTML = "500$";
            return false;
        }
        if (!$("input[name='transactionType']:checked").val()) {
            var message1 = "Error! Please choose a transaction type:" + '\n' + "LMT, MKT, FOK or IOC";
            createAlert('Attention!','',message1,'danger',false,true,'pageMessages');

            document.getElementById("lmtChoice").checked = false;
            document.getElementById("mktChoice").checked = false;
            document.getElementById("fokChoice").checked = false;
            document.getElementById("iocChoice").checked = false;
            document.getElementById("buyChoice").checked = false;
            document.getElementById("sellChoice").checked = false;

            document.getElementById("amountRange").setAttribute("max", "1000");
            document.getElementById("amountRange").value = 500;
            document.getElementById("amount").innerHTML = "500";

            document.getElementById("priceRange").setAttribute("max", "1000");
            document.getElementById("priceRange").value = 500;
            document.getElementById("price").innerHTML = "500$";
            return false;
        }

        if ($("input[name='amount']").val() <= 0) {
            var message = "Error! You dont hold the stock, therefore you can't sell it";
            createAlert('Attention!', '', message, 'danger', false, true, 'pageMessages');
            document.getElementById("lmtChoice").checked = false;
            document.getElementById("mktChoice").checked = false;
            document.getElementById("fokChoice").checked = false;
            document.getElementById("iocChoice").checked = false;
            document.getElementById("buyChoice").checked = false;
            document.getElementById("sellChoice").checked = false;

            document.getElementById("amountRange").setAttribute("max", "1000");
            document.getElementById("amountRange").value = 500;
            document.getElementById("amount").innerHTML = "500";

            document.getElementById("priceRange").setAttribute("max", "1000");
            document.getElementById("priceRange").value = 500;
            document.getElementById("price").innerHTML = "500$";
            return false;
        }
        let params = (new URL(document.location)).searchParams;
        let stockName = params.get('stock');
        var data = $(this).serializeArray();
        data.push({name: "stock", value: stockName});


        $.ajax({
            method:'POST',
            url: this.action,
            data: $.param(data),
            timeout: 2000,
            error: function(e) {
                var ss = e.responseText;
                console.log(ss)
                createAlert('Attention!','',ss,'danger',false,true,'pageMessages');
            },
            success: function(r) {
                $("#commitTrade").trigger( "click" );
                var ss = r.responseText;
                console.log(ss)
                createAlert('Success!','',r,'success',false,true,'pageMessages');

            }
        });

        document.getElementById("lmtChoice").checked = false;
        document.getElementById("mktChoice").checked = false;
        document.getElementById("fokChoice").checked = false;
        document.getElementById("iocChoice").checked = false;
        document.getElementById("buyChoice").checked = false;
        document.getElementById("sellChoice").checked = false;

        document.getElementById("amountRange").setAttribute("max", "1000");
        document.getElementById("amountRange").value = 500;
        document.getElementById("amount").innerHTML = "500";

        document.getElementById("priceRange").setAttribute("max", "1000");
        document.getElementById("priceRange").value = 500;
        document.getElementById("price").innerHTML = "500$";

        $("#priceRange").show();
        $("#price").show();
        $("#priceLabel").show();
        $("#transTotal").show();
        $("#transactionTotal").show();

        return false;
    })
})

// Toggle the commit trade button
$(function() {
    $("#commitTrade").click(function() {
        $("#order").toggle();
        $(this).text($(this).text() === 'Commit Trade' ? 'Cancel Trade' : 'Commit Trade');
    })
})

// Set the Buy and Sell buttons
$(function () {
    $('input:radio[name="transactionDirection"]').change(
        function(){
            if ($(this).is(':checked') && $(this).val() === 'SELL') {
                adjustSellAmount();
            }
            else {
                document.getElementById("amountRange").setAttribute("max", "1000");
                document.getElementById("amountRange").setAttribute("min", "1");
                document.getElementById("amountRange").setAttribute("val", "1");
                document.getElementById("priceRange").setAttribute("max", "1000");
            }
            refreshTransactionTotal($(this).val());
        });
})

// user can sell only what he has
function adjustSellAmount() {
    let params = (new URL(document.location)).searchParams;
    let stockName = params.get('stock');
    $.ajax({
        url: TOTAL_QUANTITY_URL,
        data: { stock : stockName},
        success: function(quantity) {
            if (quantity !== undefined) {
                document.getElementById("amountRange").setAttribute("max", quantity);
                document.getElementById("amountRange").setAttribute("min", "0");
                document.getElementById("amountRange").value = quantity;
                document.getElementById("amount").innerHTML = quantity;
            }
        },
        error: function (e) {
            document.getElementById("amountRange").setAttribute("max", "0");
            document.getElementById("amountRange").setAttribute("min", "0");
            document.getElementById("amountRange").value = 0;
            document.getElementById("amount").innerHTML = "0";
        }
    });
}

//
$(function () {
    $('input:radio[name="transactionType"]').change(
        function(){
            if ($(this).is(':checked') && $(this).val() === 'MKT') {
                $("#priceRange").hide();
                $("#price").hide();
                $("#priceLabel").hide();
                $("#transTotal").hide();
                $("#transactionTotal").hide();
            }
            else {
                $("#priceRange").show();
                $("#price").show();
                $("#priceLabel").show();
                $("#transTotal").show();
                $("#transactionTotal").show();
            }
        });
})

// ALERTS

function createAlert(title, summary, details, severity, dismissible, autoDismiss, appendToId) {
    var iconMap = {
        info: "fa fa-info-circle",
        success: "fa fa-thumbs-up",
        warning: "fa fa-exclamation-triangle",
        danger: "fa ffa fa-exclamation-circle"
    };

    var iconAdded = false;

    var alertClasses = ["alert", "animated", "flipInX"];
    alertClasses.push("alert-" + severity.toLowerCase());

    if (dismissible) {
        alertClasses.push("alert-dismissible");
    }

    var msgIcon = $("<i />", {
        "class": iconMap[severity] // you need to quote "class" since it's a reserved keyword
    });

    var msg = $("<div />", {
        "class": alertClasses.join(" ") // you need to quote "class" since it's a reserved keyword
    });

    if (title) {
        var msgTitle = $("<h4 />", {
            html: title
        }).appendTo(msg);

        if(!iconAdded){
            msgTitle.prepend(msgIcon);
            iconAdded = true;
        }
    }

    if (summary) {
        var msgSummary = $("<strong />", {
            html: summary
        }).appendTo(msg);

        if(!iconAdded){
            msgSummary.prepend(msgIcon);
            iconAdded = true;
        }
    }

    if (details) {
        var msgDetails = $("<p />", {
            html: details
        }).appendTo(msg);

        if(!iconAdded){
            msgDetails.prepend(msgIcon);
            iconAdded = true;
        }
    }


    if (dismissible) {
        var msgClose = $("<span />", {
            "class": "close", // you need to quote "class" since it's a reserved keyword
            "data-dismiss": "alert",
            html: "<i class='fa fa-times-circle'></i>"
        }).appendTo(msg);
    }

    $('#' + appendToId).prepend(msg);

    if(autoDismiss){
        setTimeout(function(){
            msg.addClass("flipOutX");
            setTimeout(function(){
                msg.remove();
            },1000);
        }, 10000);
    }
}