$(() => {
    awaitForTemplates().then(() => {
        sendExampleMessages()
    });
});


function sendExampleMessages() {

    failchat.nativeClient = true;

    failchat.handleMessage({
        "type": "client-configuration",
        "content": {
            "showViewersCount": true,
            "showOriginBadges": true,
            "showUserBadges": true,
            "zoomPercent": 100,
            "hideDeletedMessages" : false,
            "showHiddenMessages": false,
            "nativeClient": {
                "backgroundColor": "#ff000000",
                "coloredNicknames": true,
                "hideMessages" : false,
                "hideMessagesAfter" : 60,
                "showStatusMessages" : true
            },
            "externalClient": {
                "backgroundColor": "#333333ff",
                "coloredNicknames": true,
                "hideMessages" : true,
                "hideMessagesAfter" : 20,
                "showStatusMessages" : false
            },
            "enabledOrigins": {
                "peka2tv": true,
                "twitch": true,
                "goodgame": true,
                "youtube": true
            },
            "deletedMessagePlaceholder": {
                "text": "message deleted {!0}",
                "elements": [
                    {
                        "type": "emoticon",
                        "code": "Kappa",
                        "url": "https://static-cdn.jtvnw.net/emoticons/v1/25/2.0",
                        "format": "raster"
                    }
                ]
            }
        }
    });

    failchat.handleMessage({
        "type": "viewers-count",
        "content": {
            "peka2tv": 5,
            "goodgame": 25,
            "twitch": 250,
            "youtube": 113
        }
    });

    sendAllMessages()
}

let lastId = 1;

function sendAllMessages() {
    exampleMessages.forEach(message => {
        failchat.handleMessage(message);
    });
}

const exampleMessages = [
    {
        "type": "message",
        "content": {
            "id": 1,
            "origin": "twitch",
            "author": {"name": "TheStreamer", "id": "TheStreamer", "color": "#00ff00"},
            "text": "Just launched the chat {!0}",
            "timestamp": 1529402393227,
            "highlighted": false,
            "highlightedBackground": false,
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/2",
                "description": "Broadcaster"
            }],
            "elements": [{
                "type": "emoticon",
                "code": "KEKW",
                "url": "https://cdn.frankerfacez.com/emoticon/381875/1",
                "format": "raster"
            }]
        }
    },
    {
        "type": "message",
        "content": {
            "id": 25994,
            "origin": "twitch",
            "author": {"name": "kotyn_", "id": "kotyn_", "color": "#daa520ff"},
            "text": "@UncleBjorn Привет, приблизительно на сколько минут миссия?",
            "timestamp": 1612688238161,
            "highlighted": true,
            "highlightedBackground": false,
            "elements": [],
            "badges": []
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26034,
            "origin": "twitch",
            "author": {"name": "XpanecX", "id": "XpanecX", "color": "#0000ffff"},
            "text": "после прохождения этой игры можно будет уже на турнир по фарминг симулятору ехать {!0}",
            "timestamp": 1612688336464,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [{
                "type": "emoticon",
                "code": "StreamerDoesntKnow",
                "url": "https://cdn.betterttv.net/emote/5e7cdc1d6d485d372b29e733/2x",
                "format": "raster"
            }],
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/1d4b03b9-51ea-42c9-8f29-698e3c85be3d/2",
                "description": "GlitchCon 2020"
            }]
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26035,
            "origin": "twitch",
            "author": {"name": "D1mosaur", "id": "D1mosaur", "color": "#2e8b57ff"},
            "text": "{!0}",
            "timestamp": 1612688341769,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [{
                "type": "emoticon",
                "code": "sadCat",
                "url": "https://cdn.betterttv.net/emote/5b96e7f1bbf4663f648795b1/2x",
                "format": "raster"
            }],
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/1d4b03b9-51ea-42c9-8f29-698e3c85be3d/2",
                "description": "GlitchCon 2020"
            }]
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26042,
            "origin": "twitch",
            "author": {"name": "homekadzered", "id": "homekadzered", "color": "#b22222ff"},
            "text": "уменьшение яйчеек {!0}",
            "timestamp": 1612688366731,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [{
                "type": "emoticon",
                "code": "Pog",
                "url": "//cdn.frankerfacez.com/emote/210748/1",
                "format": "raster"
            }],
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/c957cf31-f9b1-40f5-898a-c511fd0b4a73/2",
                "description": "1.5-Year Subscriber"
            }]
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26065,
            "origin": "twitch",
            "author": {"name": "sortir", "id": "sortir", "color": "#9acd32ff"},
            "text": "{!0}",
            "timestamp": 1612688431642,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [{
                "type": "emoticon",
                "code": "4HEader",
                "url": "//cdn.frankerfacez.com/emote/165784/1",
                "format": "raster"
            }],
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://static-cdn.jtvnw.net/badges/v1/1d4b03b9-51ea-42c9-8f29-698e3c85be3d/2",
                "description": "GlitchCon 2020"
            }]
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26082,
            "origin": "youtube",
            "author": {"name": "Viva La Vida", "id": "UCXE54mRbGyGb_hM7uL6QQEg", "color": "#107516ff"},
            "text": "@Jams Here it is{!0}",
            "timestamp": 1612688507752,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [{
                "type": "emoticon",
                "code": "teacup without handle",
                "url": "https://cdnjs.cloudflare.com/ajax/libs/twemoji/13.0.1/svg/1f375.svg",
                "format": "vector"
            }],
            "badges": [{
                "type": "image",
                "format": "raster",
                "url": "https://yt3.ggpht.com/zFwnQLo7173TsBHhOvFbNbnRqAsI9Mm5Q1tCS--LRan6JLNpoq9GSruy2D8NFpkOsvLlZHCSTQk=s32-c-k",
                "description": "Member (6 months)"
            }]
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26107,
            "origin": "youtube",
            "author": {"name": "Prolq", "id": "UCi_JkMtLVjx3mM_rxFJm3qg", "color": null},
            "text": "hola{!0}{!1}",
            "timestamp": 1612688523330,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [{
                "type": "emoticon",
                "code": "regional indicator symbol letter m + regional indicator symbol letter x",
                "url": "https://cdnjs.cloudflare.com/ajax/libs/twemoji/13.0.1/svg/1f1f2-1f1fd.svg",
                "format": "vector"
            }, {
                "type": "emoticon",
                "code": "call me hand",
                "url": "https://cdnjs.cloudflare.com/ajax/libs/twemoji/13.0.1/svg/1f919.svg",
                "format": "vector"
            }],
            "badges": []
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26140,
            "origin": "youtube",
            "author": {"name": "Bartek Michaluk", "id": "UCuzaPSaPJF4m8JxT72i4adg", "color": null},
            "text": "what's up?",
            "timestamp": 1612688543413,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [],
            "badges": []
        }
    },
    {
        "type": "message",
        "content": {
            "id": 26242,
            "origin": "goodgame",
            "author": {"name": "Virtix", "id": "Virtix", "color": "#8781bdff"},
            "text": "UshiNoMimi, забавнее через рывок посейдона, огневую поддержку разрушение столкновение с припятствиями и пять рывков))",
            "timestamp": 1612688704109,
            "highlighted": false,
            "highlightedBackground": false,
            "elements": [],
            "badges": [{"type": "character", "htmlEntity": "&#58896;", "color": "#8781bdff"}]
        }
    }
];
