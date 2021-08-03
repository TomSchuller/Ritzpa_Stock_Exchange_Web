var chatVersion = 0;
var refreshRate = 2000; //milli seconds
var STOCK_LIST_URL = buildUrlWithContextPath("stockslist");
var USER_LIST_URL = buildUrlWithContextPath("userslist");
var CHAT_LIST_URL = buildUrlWithContextPath("chat");
var Total_Money_URL = buildUrlWithContextPath("totalMoney");
var ALERT_URL = buildUrlWithContextPath("transactionAlerts");


// **** JQUERY ****

function ajaxAlertContent() {
    $.ajax({
        url: ALERT_URL,
        success: function (data) {
            if (data !== "") {
                createAlert('Attention!','You have notifications!' , data, 'success', false, true, 'pageMessages');
            }
            },
        error: function (error) {

        }
    });
}

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

//activate the timer calls after the page is loaded
$(function() {

    //The users list is refreshed automatically every second
    setInterval(ajaxUsersList, refreshRate);
    setInterval(ajaxAlertContent, refreshRate);

    //The chat content is refreshed only once (using a timeout) but
    //on each call it triggers another execution of itself later (1 second later)
    triggerAjaxChatContent();

    // update stock list
    setInterval(ajaxStocksList, refreshRate);

    // update total Maney label
    setInterval(ajaxTotalMoneyLabel, refreshRate);
});

// function on load- takes care of the load xml format
$(function() { // onload...do
    $("#LoadXML").submit(function() {
        var file = this[0].files[0];
        var formData = new FormData();
        formData.append("fake-key", file);
        $.ajax({
            method:'POST',
            data: formData,
            url: this.action,
            processData: false, // Don't process the files
            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
            timeout: 4000,
            error: function(e) {
                var ss = e.responseText
                console.log(ss)
                createAlert('Attention!','',ss,'danger',false,true,'pageMessages')
            },
            success: function(r) {
                createAlert('Success!','Added XML file!',r,'success',false,true,'pageMessages')
            }
        });
        return false;
    })
})



// function on load- takes care of the load xml format
$(function() { // onload...do
    $("#TransferMoneyForm").submit(function() {
        $.ajax({
            method:'POST',
            data: $(this).serialize(),
            url: this.action,
            timeout: 4000,
            error: function(e) {
            },
            success: function(r) {
            }
        });
        $("#transferMoney").val("");
        return false;
    })
})

$(function() {
    $("#createCompanyForm").submit(function() {
        $.ajax({
            method:'POST',
            data: $(this).serialize(),
            url: this.action,
            timeout: 4000,
            error: function(e) {
                var ss = e.responseText
                console.log(ss)
                createAlert('Attention!','',ss,'danger',false,true,'pageMessages')
            },
            success: function(r) {
                createAlert('Success!','Company Created!',r,'success',false,true,'pageMessages')
            }
        });
        $("#stockCompany").val("");
        $("#stockQuantity").val("");
        $("#stockSymbol").val("");
        $("#stockValue").val("");
        return false;
    })
})

$(function() {
    $("#loadXmlButton").click(function (){
        $("#LoadXML").toggle();
        $(this).text($(this).text() === 'Load XML File' ? 'Cancel File Load' : 'Load XML File');

    })
    $("#createCompanyButton").click(function (){
        $("#createCompanyForm").toggle();
        $(this).text($(this).text() === 'New Company' ? 'Cancel Company Creation' : 'New Company');

    })
    $("#transferMoneyButton").click(function (){
        $("#TransferMoneyForm").toggle();
        $(this).text($(this).text() === 'Transfer Money' ? 'Cancel Money Transfer' : 'Transfer Money');
    })
})

//**** FUNCTIONS ****

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
            '<td>' + stocks[i].totalCycle + "$" + '</td>' +
            '<td>' + stocks[i].transactionsSize  +  '</td>' +
            '<td><a onclick="handleStockPage(\'' + stocks[i].symbol + '\')">' + 'Go To Stock Page' +  '</a></td>' +
            '</tr>')
            .appendTo(stocksList);
    }
}

function handleStockPage(stockName) {
    window.location = '../brokerStockProfile/brokerStockProfile.html?stock=' + stockName;
}

function refreshTotalMoneyLabel(money) {
    console.log("Adding money #" + ": " + money);

    $("#totalUserMoney")[0].innerHTML =  money + '$';
    // totalLabel.innerHTML = (money);
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

function ajaxTotalMoneyLabel() {
    $.ajax({
        url: Total_Money_URL,
        success: function(user) {
            refreshTotalMoneyLabel(user.money);
            refreshInvestmentMoneyLabel(user.investmentMoney);
            refreshUserActions(user.userActions);
        }
    });
}

function refreshInvestmentMoneyLabel(money) {
    console.log("Adding investment money #" + ": " + money);

    $("#investmentMoney")[0].innerHTML = money + '$';
    // totalLabel.innerHTML = (money);
}

function refreshUserActions(userActions) {
    var userActionsList =  $("#userActions");
    var userActionsBody =  $("#userActionsBody");
    userActionsBody.empty();

    for(let i=0; i<userActions.length;i++){
        var index = i.valueOf()+1;
        console.log("Adding user action #" + i);
        $('<tr>' +
            '<td>' + index +  '</td>' +
            '<td>' + userActions[i].description +  '</td>' +
            '<td>' + userActions[i].timestamp +  '</td>' +
            '<td>' + userActions[i].actionFee + "$" +  '</td>' +
            '<td>' + userActions[i].beforeBalance + "$" +  '</td>' +
            '<td>' + userActions[i].afterBalance + "$" +  '</td>' +
            '</tr>')
            .appendTo(userActionsList);
    }
}

//function on load - updates the data we see evrey 2 sec
function triggerAjaxChatContent() {
    setTimeout(ajaxChatContent, refreshRate);
}

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
        }, 5000);
    }
}


