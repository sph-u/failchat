"use strict";

const failchat = {
    maxMessages: 50,
    iconsPath: "../_shared/icons/",
    hideMessageAnimationClass: null,
    origins: ["peka2tv", "twitch", "goodgame", "youtube"],
    nativeClient: false
};

const templates = {
    message: new Template("message"),
    statusMessage: new Template("status-message"),
    rasterEmoticon: new Template("emoticon-raster"),
    vectorEmoticon: new Template("emoticon-vector"),
    link: new Template("link"),
    image: new Template("image"),
    originViewersBar: new Template("origin-viewers-bar")
};

const keyCode = {
    pageUp: 33,
    home: 36,
    up: 38
};

$(() => {
    awaitForTemplates().then(() => {
        initializeFailchat()
    });
});

async function awaitForTemplates() {
    for (const templateProperty in templates) {
        if (templates.hasOwnProperty(templateProperty)) {
            await templates[templateProperty].loaded;
        }
    }
}

function initializeFailchat() {
    failchat.nativeClient = (navigator.userAgent.search("failchat") >= 0);

    const activeMessages = new LinkedList(); // head - newest message
    const originsStatus = new OriginsStatus();

    let lastSystemMessageId = -1; //goes down
    let autoScroll = true;
    let showStatusMessages = true; // show if origin-status message received before client-configuration message
    let coloredNicknames = true;
    let hideMessages = false;
    let hideMessagesAfter = 60;
    let showHiddenMessages = false;
    let hideDeletedMessages = false;
    let deletedMessagePlaceholder = {
        text: "message deleted",
        elements: []
    };

    // dom elements
    const messageContainer = $("#message-container");
    const scroller = $(failchat.baronParams.scroller);
    const scrollBar = $(failchat.baronParams.bar);
    const clickTransparencyIcon = $("#click-transparency-icon");

    // viewers bar
    const viewersBar = $(".viewers-bar");
    const viewersCountItems = {};

    failchat.origins.forEach(origin => {
        const viewersBarHtml = templates.originViewersBar.render({origin: origin, iconsPath: failchat.iconsPath});
        $(".viewers-counters").append(viewersBarHtml);
        viewersCountItems[origin] = {
            bar: $("#" + origin + "-viewers-counter"),
            counter: $("#" + origin + "-viewers-count")
        };

        //set default value
        viewersCountItems[origin].counter.text("?")
    });

    // scroll bar
    baron(failchat.baronParams);

    // auto scroll
    new ResizeSensor(messageContainer, () => {
        if (autoScroll) {
            scroller.scrollTop(messageContainer.height());
        }
    });


    // Dynamic styles
    const dynamicStyles = document.createElement("style");
    dynamicStyles.type = "text/css";
    document.head.appendChild(dynamicStyles);


    // Web socket
    const wsUrl = new URL("/ws", window.location.href);
    wsUrl.protocol = wsUrl.protocol.replace('http', 'ws');

    const paramParser = new URLSearchParams(window.location.search);
    const paramPort = paramParser.get("port");
    let port;
    if (paramPort !== null) {
        port = paramPort
    } else {
        port = "10880"
    }
    wsUrl.port = port;

    const socket = new ReconnectingWebSocket(wsUrl);
    socket.maxReconnectInterval = 5000;

    socket.onopen = function() {
        if (failchat.demoMode) {
            return;
        }
        const connectedMessage = {id: nextSystemMessageId(), "origin": "failchat", "status": "connected", "timestamp": Date.now()};
        appendStatusMessage(connectedMessage);

        socket.send(JSON.stringify({type: "client-configuration", content: {}}));
        socket.send(JSON.stringify({type: "viewers-count", content: {}}));
        socket.send(JSON.stringify({type: "origins-status", content: {}}));
    };

    socket.onclose = function() {
        if (failchat.demoMode) {
            return;
        }
        const disconnectedMessage = {id: nextSystemMessageId(), origin: "failchat", status: "disconnected", timestamp: Date.now()};
        appendStatusMessage(disconnectedMessage);
        originsStatus.reset()
    };

    socket.onmessage = function(event) {
        if (failchat.demoMode) {
            return;
        }
        handleMessage(JSON.parse(event.data));
    };

    failchat.socket = socket;


    function handleMessage(wsMessage) {
        const type = wsMessage.type;
        const content = wsMessage.content;

        switch (type) {
            case "message":
                handleChatMessage(content);
                break;
            case "origins-status":
                handleOriginsStatusMessage(content);
                break;
            case "delete-message":
                handleDeleteMessage(content);
                break;
            case "client-configuration":
                handleClientConfigurationMessage(content);
                break;
            case "clear-chat":
                handleClearChatMessage();
                break;
        }

        if (!failchat.nativeClient) return;

        if (type === "viewers-count") {
            updateViewersValues(content);
        }
    }

    failchat.handleMessage = handleMessage;

    function handleChatMessage(chatMessage) {
        chatMessage.text = renderElements(chatMessage.text, chatMessage.elements);
        chatMessage.iconsPath = failchat.iconsPath;
        chatMessage.author.coloredNicknames = coloredNicknames;

        const messageHtml = templates.message.render(chatMessage);

        appendMessage(chatMessage, messageHtml)
    }

    /** @return text with rendered elements. */
    function renderElements(text, elements) {
        elements.forEach((element, index) => {
            let elementHtml;
            switch(element.type) {
                case "emoticon":
                    if (element.format === "vector") {
                        elementHtml = templates.vectorEmoticon.render(element);
                    } else {
                        elementHtml = templates.rasterEmoticon.render(element);
                    }
                    break;
                case "link":
                    elementHtml = templates.link.render(element);
                    break;
                case "image":
                    elementHtml = templates.image.render(element);
                    break;
            }

            text = text.replace("{!" + index + "}", elementHtml);
        });

        return text;
    }

    function appendStatusMessage(message) {
        message.iconsPath = failchat.iconsPath;
        const html = templates.statusMessage.render(message);
        appendMessage(message, html);
    }

    function handleClientConfigurationMessage(config) {
        deletedMessagePlaceholder = config.deletedMessagePlaceholder;
        hideDeletedMessages = config.hideDeletedMessages;

        let clientConfig;
        if (failchat.nativeClient) {
            clientConfig = config.nativeClient;
        } else {
            clientConfig = config.externalClient;
        }

        showStatusMessages = clientConfig.showStatusMessages;
        if (failchat.nativeClient) {
            showHiddenMessages = config.showHiddenMessages;
        }

        // dynamic styles
        const bodyZoomStyle = "body { zoom: " + config.zoomPercent + "%; }";

        let originBadgesStyle = "";
        if (config.showOriginBadges === false) {
            originBadgesStyle = ".message .origin-badge { display: none; }"
        }

        let userBadgesStyle = "";
        if (config.showUserBadges === false) {
            userBadgesStyle = ".message .user-badges { display: none; }"
        }

        let bgColorStyle = "#body-wrapper { background-color: " + clientConfig.backgroundColor + "; }";

        let hiddenMessageStyle = ".hidden { display: none; }";
        if (showHiddenMessages) {
            hiddenMessageStyle = "";
        }

        dynamicStyles.innerHTML = bodyZoomStyle + originBadgesStyle + userBadgesStyle + bgColorStyle + hiddenMessageStyle;


        if (isHideMessagePropertiesChanged(clientConfig)) {
            hideMessages = clientConfig.hideMessages;
            hideMessagesAfter = clientConfig.hideMessagesAfter;
            resetHideMessageTasks();
        }

        configureColoredNicknames(clientConfig);


        // ---------- native client configuration ----------
        if (!failchat.nativeClient) return;

        // handle viewers bar configuration
        if (config.showViewersCount === true) {
            viewersBar.addClass("on");
        } else {
            viewersBar.removeClass("on");
        }

        const enabledOrigins = config.enabledOrigins;
        failchat.origins.forEach(origin => {
            if (!enabledOrigins.hasOwnProperty(origin)) return;
            if (enabledOrigins[origin] === true) {
                viewersCountItems[origin].bar.addClass("on");
            } else {
                viewersCountItems[origin].bar.removeClass("on");
            }
        });

        if (config.clickTransparency && config.showClickTransparencyIcon) {
            clickTransparencyIcon.addClass("on");
        } else {
            clickTransparencyIcon.removeClass("on");
        }
    }

    function configureColoredNicknames(clientConfig) {
        if (clientConfig.coloredNicknames === coloredNicknames) return;

        coloredNicknames = clientConfig.coloredNicknames;
        const nickElements = document.getElementsByClassName("nick");
        for (const nickElement of nickElements) {
            if (coloredNicknames) {
                const newColor = nickElement.getAttribute("color");
                if (newColor !== null) {
                    nickElement.setAttribute("style", "color: " + newColor + ";");
                }
            } else {
                nickElement.removeAttribute("style");
            }
        }
    }

    function resetHideMessageTasks() {
        for (const message of activeMessages.values()) {
            cancelHideMessageTask(message);
            createHideMessageTaskIfRequired(message);
        }
    }

    function isHideMessagePropertiesChanged(clientConfig) {
        const isSameValues = (hideMessages === clientConfig.hideMessages) &&
            (hideMessagesAfter === clientConfig.hideMessagesAfter);
        return !isSameValues
    }

    function handleDeleteMessage(deleteMessage) {
        const selector = messageSelectorById(deleteMessage.messageId);
        const message = $(selector);
        const messageText = $(selector + " .message-text");

        if (message === null || messageText === null) return;

        message.addClass("deleted-message");
        messageText.removeClass("highlighted");
        messageText.html(renderElements(deletedMessagePlaceholder.text, deletedMessagePlaceholder.elements));

        if (hideDeletedMessages) {
            const foundNode = activeMessages.findFirstBy(function (m) {
                return m.id === deleteMessage.messageId;
            });
            if (foundNode !== null) {
                hideMessage(foundNode.data)
            }
        }
    }

    function handleClearChatMessage() {
        for (const message of activeMessages.values()) {
            hideMessage(message);
        }
    }

    function handleOriginsStatusMessage(message) {
        failchat.origins.forEach(origin => {
            const oldStatus = originsStatus[origin];
            const newStatus = message[origin];

            if (newStatus === oldStatus) return;

            originsStatus[origin] = newStatus;

            if (showStatusMessages) {
                const connectedMessage = {id: nextSystemMessageId(), "origin": origin, "status": newStatus, "timestamp": Date.now()};
                appendStatusMessage(connectedMessage);
            }
        });
    }

    function updateViewersValues(counters) {
        failchat.origins.forEach(origin => {
            if (!counters.hasOwnProperty(origin)) return;

            const count = counters[origin];
            const counter = viewersCountItems[origin].counter;
            if (count === null) {
                counter.text("?");
            } else {
                counter.text(count);
            }
        });
    }

    /** Append chat message or status message to message container. */
    function appendMessage(message, html) {
        message.hidden = false;
        activeMessages.unshift(message);
        messageContainer.append(html);

        if (autoScroll) {
            deleteOldMessages()
        }

        createHideMessageTaskIfRequired(message)
    }

    function hideMessage(message) {
        const messageElement = $(messageSelector(message));

        if (failchat.hideMessageAnimationClass === null || showHiddenMessages) {
            // hide without animation
            messageElement.addClass("hidden");
            message.hidden = true;
            return;
        }

        // hide with animation
        messageElement.addClass(failchat.hideMessageAnimationClass);

        messageElement.on("animationend", null, null, () => {
            messageElement.addClass("hidden");
            messageElement.removeClass(failchat.hideMessageAnimationClass);
            message.hidden = true;
        });
        messageElement.addClass(failchat.hideMessageAnimationClass);
    }

    function removeMessageElement(message) {
        const element = $(messageSelector(message));
        if (element != null)
            element.remove();
    }

    function createHideMessageTaskIfRequired(message) {
        if (!hideMessages) return;
        if (message.hidden) return;

        const taskId = setTimeout(() => {
            hideMessage(message);
        }, hideMessagesAfter * 1000);
        message.hideTaskId = taskId;
    }

    function cancelHideMessageTask(message) {
        const taskId = message.hideTaskId;

        if (taskId instanceof Number) {
            clearTimeout(taskId)
        }
    }

    function deleteOldMessages() {
        if (activeMessages.length <= failchat.maxMessages) return;

        while (activeMessages.length >= failchat.maxMessages) {
            const removedMessage = activeMessages.pop();
            cancelHideMessageTask(removedMessage);
            removeMessageElement(removedMessage);
        }
    }

    function nextSystemMessageId() {
        return lastSystemMessageId--;
    }

    $("body,html").bind("keydown wheel mousewheel", e => {
        //checks for disabling autoscroll
        if (autoScroll) {
            if (e.originalEvent.deltaY < 0 ||
                (e.type === "keydown" && (e.keyCode === keyCode.home || e.keyCode === keyCode.pageUp || e.keyCode === keyCode.up))
            ) {
                autoScroll = false;
                scrollBar.css("visibility", "visible");
            }
            if (e.type === "mousewheel") { // for old browsers
                autoScroll = scroller.scrollTop() + scroller.height() >= Math.floor(messageContainer.height());
                if (!autoScroll) {
                    scrollBar.css("visibility", "visible");
                }
            }
        }
    });

    scroller.scroll(e => {
        //checks for enabling autoscroll
        if (!autoScroll) {
            autoScroll = scroller.scrollTop() + scroller.height() >= Math.floor(messageContainer.height());
            if (autoScroll) {
                scrollBar.css("visibility", "hidden");
            }
        }
    });
}

// Add user to ignore list and delete message
function ignore(messageNode) {
    deleteMessage(messageNode);
    failchat.socket.send(JSON.stringify(
        {
            type: "ignore-author",
            content: {
                authorId: messageNode.getAttribute("author-id")
            }
        }
    ));
}

// Just delete message
function deleteMessage(messageNode) {
    failchat.socket.send(JSON.stringify(
        {
            type: "delete-message",
            content: {
                messageId: messageNode.getAttribute("message-id")
            }
        }
    ));
}

function toggleModButtonsDisplay(messageId) {
    const modButtons = $("#message-" + messageId + " .mod-buttons")[0];
    if (modButtons.style.display === "") { //default is display: none
        modButtons.style.display = "inline"
    } else {
        modButtons.style.display = ""
    }
}

$.views.converters("time", val => {
    const d = new Date(val);
    const h = d.getHours().toString();
    let m = d.getMinutes().toString();
    let s = d.getSeconds().toString();
    if (m.length === 1) {
        m = "0" + m;
    }
    if (s.length === 1) {
        s = "0" + s;
    }
    return h + ":" + m + ":" +  s;
});


/** Asynchronously initialized template. */
function Template(name) {
    this.jsrTemplate = null;

    const xhr = new XMLHttpRequest();
    xhr.open("GET", "../_shared/templates/" + name + ".tmpl.html");

    this.loaded = new Promise((resolve, reject) => {
        xhr.onload = () => {
            if (xhr.readyState !== 4) return;
            if (xhr.status === 200) {
                this.jsrTemplate = $.templates(xhr.responseText);
            } else {
                console.error("Failed to load template '" + name + "'. " + JSON.stringify(xhr));
            }
            resolve();
        };
        xhr.onerror = () => {
            console.error("Error during '" + name + "'template request. " + JSON.stringify(xhr));
            reject();
        }
    });

    xhr.send();

    this.render = function (parameters) {
        if (this.jsrTemplate === null) {
            console.error("Can't render template '" + name + "', cause: not initialized");
            return null;
        }
        return this.jsrTemplate.render(parameters)
    }
}

function messageSelector(message) {
    return messageSelectorById(message.id);
}

function messageSelectorById(id) {
    return "#message-" + id;
}

const status = {
    connected: "connected",
    disconnected: "disconnected"
};

function OriginsStatus() {
    this.reset = function() {
        failchat.origins.forEach((origin) => {
            this[origin] = status.disconnected;
        });
    };

    this.reset()
}
