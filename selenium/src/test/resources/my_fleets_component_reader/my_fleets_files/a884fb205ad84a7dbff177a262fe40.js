function tsdpkt(f)
{
    var vz = "";
    if (f < 0)
    {
        vz = "-";
    }
    f = Math.abs(f);
    var r = f % 1000;
    while (f >= 1000)
    {
        var k1 = "";
        if ((f % 1000) < 100)
        {
            k1 = "0";
        }
        if ((f % 1000) < 10)
        {
            k1 = "00";
        }
        if ((f % 1000) == 0)
        {
            k1 = "00";
        }
        f = Math.abs((f-(f % 1000)) / 1000);
        r = f % 1000 + LocalizationStrings['thousandSeperator'] + k1 + r;
    }
    r = vz + r;
    return r;
}

function formatTime(seconds)
{
    var hours = Math.floor(seconds / 3600);
    seconds -= hours * 3600;

    var minutes = Math.floor(seconds / 60);
    seconds -= minutes * 60;

    if (minutes < 10) minutes = "0" + minutes;
    if (seconds < 10) seconds = "0" + seconds;

    return hours + ":" + minutes  + ":" + seconds;
}

function round(x, n) {
    if (n < 1 || n > 14) return false;
    var e = Math.pow(10, n);
    var k = (Math.round(x * e) / e).toString();
    if (k.indexOf('.') == -1) k += '.';
    k += e.toString().substring(1);
    return k.substring(0, k.indexOf('.') + n+1);
}

function show_hide_menus(element)
{
    if ($(element).is(':visible')) {
        $(element).hide();
    } else {
        $(element).show();
    }
}

function change_class(ele)
{
    if (document.getElementById(ele).className == "closed")
    {
        document.getElementById(ele).className = "opened";
    }
    else
    {
        document.getElementById(ele).className = "closed";
    }
}

function show_hide_tbl(id)
{
    var el= document.getElementById(id);
    try
    {
        if(el) el.style.display= (el.style.display == "none" ? "table-row" : "none");
    }
    catch(e)
    {
        // Der IE bis V7 kann kein table-row, deshalb Fallback auf 'Block'
        el.style.display= "block";
    }
}


function cntchar(inputField, m) {
    var $inputField = $(inputField);
    if($inputField.val().length > m) {
        $inputField.val($inputField.val().substr(0, m));
    }
    $inputField.parents("form").find(".cntChars").text($inputField.val().length);
}

function showGalaxy(galaxy, system, planet) {
    openParentLocation("index.php?page=ingame&component=galaxy&no_header=1&galaxy=" + galaxy + "&system=" + system + "&planet=" + planet);
}

function openParentLocation(url) {
    try {
        window.opener.document.location.href=url;
    } catch (error) {
        try {
            window.parent.document.location.href=url;
        } catch (error) {
            document.location.href=url;
        }
    }

}

function submitOnEnter(ev)
{
    var keyCode;

    if(window.event)
    {
        keyCode = window.event.keyCode;
    }
    else if(ev)
    {
        keyCode = ev.which;
    }
    else
    {
        return true;
    }

    if(keyCode == 13)
    {
        trySubmit();
        return false;
    }
    else
    {
        return true;
    }
}

function setMaxIntInput(formElement, data)
{
    for(var techID in data) {
        if (!$(formElement).find("#ship_"+techID).attr("disabled")) {
            $(formElement).find("#ship_"+techID).val(data[techID]);
            checkIntInput($(formElement).find("ship_"+techID), 0, data[techID]);
        }
    }
}

function clearInput(id)
{
    $(id).val("");
}

function checkIntInput(id, minVal, maxVal)
{
    var value = $(id).val();

    if (typeof value != "undefined" && value != "") {
        intVal = Math.abs(getValue(value));

        if (maxVal != null) {
            intVal = Math.min(intVal, maxVal);
        }

        $(id).val(intVal);
    }
}

function clampInt(val, minVal, maxVal, allowEmpty)
{
    if (allowEmpty && (val === '' || val === 0)) {
        return ''
    }
    let intVal = parseInt(val);
    if(isNaN(intVal)) {
        return minVal
    }
    intVal = Math.min(intVal,maxVal)
    intVal = Math.max(intVal,minVal)

    return intVal
}

function clampFloat(val, minVal, maxVal)
{
    let floatVal = parseFloat(val);
    if(isNaN(floatVal)) {
        return minVal;
    }
    floatVal = Math.max(floatVal,minVal)
    floatVal = Math.min(floatVal,maxVal);

    return floatVal
}

function handlerToSubmitAjaxForm(form)
{
    var submitFunction = "submit_" + String(form);

    if ($.isFunction(window[submitFunction])) {
        window[submitFunction]();
    }

    return false;
}

function ajaxCall(url, targetSelector, callback)
{
    if (typeof targetSelector === 'string') {
        let $targetHTMLObj = $(targetSelector);
        $targetHTMLObj.find('select').ogameDropDown('destroy');
        $targetHTMLObj.html('<p class=\"ajaxLoad\"></p>');
    }
    $.post(url, function(data){
        if (typeof targetSelector === 'string') {
            let $targetHTMLObj = $(targetSelector);
            $targetHTMLObj.html(data);
            $targetHTMLObj.find('select').ogameDropDown();
        }
        if (typeof callback === 'function') {
            callback();
        }
    });
}

function ajaxSubmit(url, formDataOrSelector, targetSelector, callback)
{
    if (typeof targetSelector === 'string') {
        let $targetHTMLObj = $(targetSelector);
        $targetHTMLObj.find('select').ogameDropDown('destroy');
        $targetHTMLObj.html("<p class=\"ajaxLoad\"><?=LOCA_ALL_AJAXLOAD ?></p>");
    }
    let formData = (typeof formDataOrSelector === 'string') ?
        $(formDataOrSelector).serialize() :
        formDataOrSelector;
    $.post(url, formData, function(data) {
            if (typeof targetSelector === 'string') {
                let $targetHTMLObj = $(targetSelector);
                $targetHTMLObj.html(data);
                $targetHTMLObj.find('select').ogameDropDown();
            }
            if (typeof callback === 'function') {
                callback();
            }
    });
}

Number.prototype.isBetween=function(min, max){
    return this >= min && this <= max;
};

function getValue(value) {
    result = parseInt(
        value.toString()
        .replace(/^k$/, "1000")
        .replace(/k/, "000")
        .replace(/^0+/, "")
        .replace(/[^0-9]/g, "")
        );
    return isNaN(result)?0:result;
}

/**
 * loads an external js script and calls a function when it is loaded
 * @param url url of the script to load
 * @param callback function to call when script is loaded
 * @url http://www.nczonline.net/blog/2009/07/28/the-best-way-to-load-external-javascript/
 */
function loadScript(url, callback){

    if (typeof(loadScript.loadedScripts) == 'undefined') {
        loadScript.loadedScripts = {};
    }

    if (typeof(loadScript.loadedScripts[url]) == 'undefined') {
        loadScript.loadedScripts[url] = true;
        var script = document.createElement("script");
        script.type = "text/javascript";

        if (script.readyState){  //IE
            script.onreadystatechange = function(){
                if (script.readyState == "loaded" ||
                    script.readyState == "complete"){
                    script.onreadystatechange = null;
                    callback();
                }
            };
        } else {  //Others
            script.onload = function(){
                callback();
            };
        }

        script.src = url;
        var head = document.getElementsByTagName("head")[0];
        head.appendChild(script);
    } else {
        callback();
    }
}

function formatNumber(object, value) {
    var formattedValue = number_format(getValue(value), 0, LocalizationStrings['decimalPoint'], LocalizationStrings['thousandsSeparator']);
    var $thisObj = $(object);
    var range = $thisObj.getSelection();
    if ($thisObj.val() != formattedValue) {
        if ($thisObj.val().length != formattedValue.length) {
            range.start = range.start + (formattedValue.length - $thisObj.val().length);
            range.end = range.end + (formattedValue.length - $thisObj.val().length);
        }
    }
    $thisObj.val(formattedValue);
    if ($thisObj.is(":focus")) {
        $thisObj.setSelection(range);
    }
}

function initToggleHeader(name) {
    $('a.toggleHeader[data-name='+name+']').click(function(e){
        e.preventDefault();
        let toggleState = $(e.currentTarget).closest('.planet-header').hasClass('shortHeader')
        $(e.currentTarget).closest('.planet-header').toggleClass('shortHeader');
        $(".c-left").toggleClass('shortCorner');
        $(".c-right").toggleClass('shortCorner');
        changeSetting('headerImage', name + '|' + toggleState);
    });
}

function initFormValidation()
{
    $("form.formValidation").validationEngine({
        validationEventTrigger:"keyup blur",
        promptPosition: "centerRight"
    });
}

Function.prototype.clone = function() {
    var fct = this;
    var clone = function() {
        return fct.apply(this, arguments);
    };
    clone.prototype = fct.prototype;
    for (var property in fct) {
        if (fct.hasOwnProperty(property) && property !== 'prototype') {
            clone[property] = fct[property];
        }
    }
    return clone;
};

function hideTipsOnTabChange() {
    $("select").ogameDropDown('hide');
    Tipped.hideAll();
}

jQuery.fn.slideFadeToggle = function(speed, easing, callback) {
    return this.animate({opacity: 'toggle', width: 'toggle'}, speed, easing, callback);
};

function focusOnTabChange(element, focusOnReady) {
    var focusFunction = function() {
        $(element).focus();
    };
    if (focusOnReady == true) {
        $(document).ready(focusFunction);
    }
    $(window).unbind('blur').bind('blur', focusFunction);
}

/**
 * @see http://obvcode.blogspot.de/2007/11/easiest-way-to-check-ie-version-with.html
 * @return {Number}
 */
function getIEVersion() {
    var version = 999;
    if (navigator.appVersion.indexOf("MSIE") != -1)
        version = parseFloat(navigator.appVersion.split("MSIE")[1]);
    return version;
}


ogame.tools = {
    /**
     * adds a hover effect to given selectors
     * @param {String} selector - selector with elements to apply the style to
     * @returns {undefined}
     */
    addHover: function(selector) {
        $(selector).on({
            mouseenter: function() {
                $(this).addClass("over");
            },
            mouseleave: function() {
                $(this).removeClass("over");
            }
        });
    },

    /**
     * shows a "to top" button on long pages
     *
     * @returns {undefined}
     */
    scrollToTop: function() {

        var $scrollToTop = $('.scroll_to_top');

        $(window).on('scroll.scrollToTop', function() {
            $('.scroll_to_top').css({ visibility: ($scrollToTop.offset().top > window.innerHeight) ? 'visible' : 'hidden' }, 600);
        });

        $scrollToTop.on('click.scrollToTop', function() {
            $('body, html').animate({ scrollTop: 0 }, 600);
        });
    }
};

/**
 * Common UI Components, that are reused across the Game
 *
 **/


/**
 * Fill level bar display for storage rooms and cargo space
 *
 * @param barContainerClass
 * @param barClass
 * @param premiumBarClass - if additional premium bar is wanted
 *
 **/
function refreshBars(barContainerClass, barClass, premiumBarClass) {

    var $barContainer = $('.' + barContainerClass);

    $barContainer.each(function() {
        var $this = $(this),
            amountFull = $this.data('currentAmount'),
            capacity = $this.data('capacity'),
            wPercent = amountFull/capacity * 100,
            $bar = $this.find('.' + barClass);

            if (wPercent > 100) { wPercent = 100; }
            else if (wPercent == 0) { wPercent = 0; }
            else if (wPercent < 1.3) { wPercent = 1.3; }

            $bar.css('width',  wPercent + '%');

            if (wPercent < 90) {
                $bar.attr('class', barClass + ' filllevel_undermark');
            } else if (wPercent > 90 && wPercent < 100) {
                $bar.attr('class', barClass + ' filllevel_middlemark');
            } else {
                $bar.attr('class', barClass + ' filllevel_overmark');
            }

            if (premiumBarClass) {
                var $premiumBar = $this.find('.' + premiumBarClass),
                    wPercentPremium = $premiumBar.data('premiumPercent');

                if ((wPercent + wPercentPremium) > 100) {
                    wPercentPremium = 100 - wPercent;
                }

                $premiumBar.css('width',  wPercentPremium + '%');
            }
    });
}
