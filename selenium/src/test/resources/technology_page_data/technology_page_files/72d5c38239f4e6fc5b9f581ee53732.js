$(function(){var b=(function(){var d=document.createElement("style");d.appendChild(document.createTextNode(""));document.head.appendChild(d);return d.sheet})();for(var a=0;a<=100;a++){b.insertRule('.technology[data-status="active"][data-progress="'+a+'"] .icon::after { height: '+(100-a)+"%; }",0)}$(document).on("click",".technology .icon button.upgrade",function(d){d.preventDefault();d.stopPropagation();var f=$(this).data("target");var e=$(this).data("is-spaceprovider")==1;if(planetMoveInProgress){errorBoxDecision(LocalizationStrings.attention,LocalizationStrings.planetMoveBreakUpWarning,LocalizationStrings.yes,LocalizationStrings.no,function(){window.location.href=f})}else{if(lastBuildingSlot.showWarning==false||e){window.location.href=f}else{errorBoxDecision(LocalizationStrings.notice,lastBuildingSlot.slotWarning,LocalizationStrings.yes,LocalizationStrings.no,function(){window.location.href=f})}}});var c=$('.technology[data-status="active"]');if(c.length>0){setInterval(function(){c.each(function(){var d=$(this);d.attr("data-progress",Math.round((1-((d.data("end")-Math.floor((Date.now()+window.timeDiff+window.timeZoneDiffSeconds*1000)/1000))/(d.data("end")-d.data("start"))))*100))})},1000)}});