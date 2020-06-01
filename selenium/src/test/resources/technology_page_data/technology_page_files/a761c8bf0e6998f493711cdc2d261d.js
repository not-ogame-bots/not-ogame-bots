function rocketsDestroyed(data) {
	var data = $.parseJSON(data);

    errorBoxAsArray(data["errorbox"]);
    if (!data["errorbox"].failed) {
        $("#rocketsilo").parents('.overlayDiv').dialog('close');
        let technologyId = $("#technologydetails").data('technology-id')
        technologyDetails.show(technologyId)
    }
}

function supplyFleet(data) {
	var data = $.parseJSON(data);

	if(data.status) {
		getAjaxResourcebox();
        /*$("#holdingTime-" + data.id).remove();

        var $holdingTime = $('<span class="countdown holdingTime" id="holdingTime-' + data.id + '"></span>')
            .show()
            .appendTo($('#holdingTimeCell'));
*/
        supplyTimes[data.id] = data.time;
        new simpleCountdown($("#holdingTime-" + data.id), data.time);
	}

    errorBoxAsArray(data["errorbox"]);
}

function updateSupplyDetails(ships,costs,index) {
    $("#shipCount").html(gfNumberGetHumanReadable(ships));
    $("#deutCosts").html(gfNumberGetHumanReadable(costs));
    $("span.countdown").hide();
    $("#holdingTime-" + index).show();
}

function initAllianceDepot() {
    $(".overlayDiv #allydepotlayer select").ogameDropDown();
    $(".holdingTime:first-child").show();

    for (var id in supplyTimes) {
        new simpleCountdown($("#holdingTime-" + id), supplyTimes[id]);
    }
    $("#supplyTimeInput")
        .focus(function() {
            clearInput(this);
        })
        .keyup(function() {
            var deuterium = getValue($('#resources_deuterium').text());
            var costs = getValue($("#deutCosts").text());
            checkIntInput(this, 1, Math.floor(deuterium / costs));
        });

}
