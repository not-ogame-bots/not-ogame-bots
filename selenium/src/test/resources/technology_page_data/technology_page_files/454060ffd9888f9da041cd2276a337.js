ogame.messagecounter={countData:{chat:0,messages:0,buddy:0},newChats:Array(),type_chat:10,type_message:11,type_buddy:12,currentLinkSelector:null,currentType:0,currentPlayer:null,sumNewChatMessages:0,initialize:function(b,a){var c=ogame.messagecounter;if(typeof a=="undefined"&&b!==c.type_chat){c.currentPlayer=0}if(typeof a=="undefined"&&b==c.type_chat){return false}if(typeof a!=="undefined"){c.currentPlayer=a}c.currentType=b;switch(b){case c.type_chat:c.currentLinkSelector=$("a.comm_menu.chat");break;case c.type_message:c.currentLinkSelector=$("a.comm_menu.messages");break;case c.type_buddy:c.currentLinkSelector=$("a.comm_menu.buddies");break;default:return false}c.update()},initChatCounter:function(a){var b=ogame.messagecounter;b.currentLinkSelector=$("a.comm_menu.chat");b.currentType=b.type_chat;b.setCount(a);b.update()},update:function(c){var b=ogame.messagecounter;var a;if(c===undefined){a=chatLoca.X_NEW_CHATS}else{a=c}changeTooltip(b.currentLinkSelector,a.replace("#+#",b.getCount()))},resetCounterByType:function(c,e){var d=ogame.messagecounter;var a=d.getIconSelectorByType(c);var b;if(e===undefined){b=""}else{b=e}changeTooltip(a,b.replace("#+#",0))},getCountSelectorByType:function(a){var c=ogame.messagecounter;var b="";switch(a){case c.type_chat:b=$("a.comm_menu.chat .new_msg_count");break;case c.type_message:b=$("a.comm_menu.messages .new_msg_count");break;case c.type_buddy:b=$("a.comm_menu.buddies .new_msg_count")}return b},getIconSelectorByType:function(b){var c=ogame.messagecounter;var a="";switch(b){case c.type_chat:a=$("a.comm_menu.chat");break;case c.type_message:a=$("a.comm_menu.messages");break;case c.type_buddy:a=$("a.comm_menu.buddies")}return a},getCounterHtml:function(b){var a='<span class="new_msg_count">'+b+"</span>";return a},getCount:function(){var a=ogame.messagecounter;switch(a.currentType){case a.type_chat:return a.countData.chat;case a.type_message:return a.countData.messages;case a.type_buddy:return a.countData.buddy}},setCount:function(a){var b=ogame.messagecounter;switch(b.currentType){case b.type_chat:b.countData.chat=a;break;case b.type_message:b.countData.messages=a;break;case b.type_buddy:b.countData.buddy=a;break}},updateCountData:function(){var c=ogame.messagecounter;if(c.isOpen()){c.setCount(0)}else{if(c.shouldAddCounter()){var b=1}else{var a=c.getCountSelectorByType(c.currentType);var b=a.html();b=parseInt(b)+1}c.setCount(b)}},shouldAddCounter:function(){var d=ogame.messagecounter;var b=d.getCountSelectorByType(d.currentType);var a=b.html();var c=false;if(typeof a=="undefined"){c=true}return c},setNewCounter:function(a,b){a.html(b)},isOpen:function(){var b=ogame.messagecounter;var a=false;switch(b.currentType){case b.type_chat:a=ogame.chat.isOpen(b.currentPlayer);break;case b.type_message:a=(location.href.indexOf("page=messages")>-1);break;case b.type_buddy:a=(location.href.indexOf("page=ingame&component=buddies")>-1);break}return a}};