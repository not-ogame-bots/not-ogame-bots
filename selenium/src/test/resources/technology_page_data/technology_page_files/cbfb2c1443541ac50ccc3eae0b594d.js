function initGlobalTechtree(id) {
    var $techtree = $("div.graph[data-id='" + id + "']");

    $techtree.find(".headline")
        .unbind('click')
        .bind('click', function () {
            $(this).next().toggle(function() {
                var $dialog = $techtree.parents('.ui-dialog');
                $dialog.hide();
                $(this).toggleClass("open");
                $dialog.show();
            });

            /*$(this).next().slideToggle("slow", function() {
                $(this).toggleClass("open");

                $dialog.css('zoom', 1.1);
                setTimeout(function() {
                    $dialog.css('zoom', 1);
                }, 1000);
            });*/
        });
    if (openTree == 'all') {
        $techtree.find('.techtree_content')
            .show(0, function() {
                $(this).addClass('open');
            });
    } else if (openTree != null) {
        $techtree.find('.techtree_content_' + openTree)
            .show(0, function() {
                $(this).addClass('open');
            });
    }
}

function initOverlayName() {
    let title = $('#technologytree').data('title');
    $("#technologytree").closest(".ui-dialog").find('.ui-dialog-title').html(title);
}

function drawArrows(id)
{
    var $techtree = $("div.graph[data-id='" + id + "']");
    var rowHeight   = $techtree.find('.techImage').outerHeight(true);

    var overallWidth = 20;
    $techtree.find('.techWrapper.depth1').each(function() {
        overallWidth += $(this).outerWidth();
    });
    $techtree.css('width', overallWidth);

    var newTree = jsPlumb.getInstance();
    newTree.Defaults.Container = $techtree;
    // for round edges set corner radius below
    newTree.Defaults.Connector = ["Flowchart", {cornerRadius: 20}];
    newTree.Defaults.Endpoint = ["Rectangle", { cssClass: "endpoint", width: 1, height: 1}];
    // default anchors. in most cases a bad idea, only as backup.
    // Continuous Anchors may produce partially overlapping connections
    newTree.Defaults.Anchors = ["ContinuousTop", "ContinuousBottom"];

    // coordinates: an array containing the coordinates of each endpoint in pixels as [left,top]
    var coordinates = {};

    // endpoints-array is set in singleTree.tpl.php. the values are techIds.
    $.each(endpoints, function() {
        var $elem = $techtree.find(".tech" + this.toString());
        newTree.addEndpoint($elem);
        var elemLeft = Math.floor($elem.find('a').offset().left);
        var elemTop = Math.floor($elem.find('a').offset().top);

        coordinates[this] = [elemLeft,elemTop];
    });

    var changedSomething;
    do {
        // for every connection:
        // check if the source is below the target on the screen. if not, move the source downwards one row
        changedSomething = false;
        $.each(connections, function() {
            var $source = $techtree.find(".tech" + this.source + " a");
            var $target = $techtree.find(".tech" + this.target + " a");
            if ($source.offset().top >= $target.offset().top - 10 && $source.offset().top <= $target.offset().top + 10) {

                $source.parent().css('margin-top', parseInt($source.parent().css('margin-top').replace(/px/, '')) + rowHeight);

                // we just moved a tech downwards... we have to adjust all corresponding coordinates
                // the surrounding div with class depth* is the second parent
                $source.parent().parent().find('a[data-tech-id]').each(function() {
                    coordinates[$(this).attr('data-tech-id')][1] += rowHeight;// 1 == top
                    //console.log("RESET "+$(this).attr('data-tech-id') + " "+(coordinates[$(this).attr('data-tech-id')][1]));
                });

                changedSomething = true;
            }
        });
    } while(changedSomething);

    // columns and rows: these 2 arrays will contain all different left- and top-values of the endpoint coordinates
    var columns = [];
    var rows = [];
    for (var elem in coordinates) {
        if (columns.indexOf(coordinates[elem][0]) ==-1) {columns.push(coordinates[elem][0]);}
        if (rows.indexOf(coordinates[elem][1]) ==-1) {rows.push(coordinates[elem][1]);}
    }
    columns.sort(function(a,b) {return (a>b?1:-1);});
    rows.sort(function(a,b) {return (a>b?1:-1);});
    // now the 2 arrays contain a numeric sorted list

    // translated: an array containing the same keys as the coordinates-array, but the values aren't pixels anymore.
    // they are the numbers of the row/column containing that endpoint
    // with this information, it will be possible to test whether the path for a connection-line is free or not.
    var translated = {};
    for (var elem2 in coordinates) {
        translated[elem2] = {'left':columns.indexOf(coordinates[elem2][0]),'top':rows.indexOf(coordinates[elem2][1])};
    }
    //console.dir(translated);

    // unfortunately, it matters in which order you put the connections into the drawing algorithm.
    // (because every line that is drawn blocks 1 anchor place at its 2 endpoints)
    // lines that will be drawn straight upwards have to be protected and drawn first..
    // -> sort by the minimum column distance
    connections.sort(function(a,b) {
        return (Math.abs(translated[a.source]['left']-translated[a.target]['left'])
            < Math.abs(translated[b.source]['left']-translated[b.target]['left']))?-1:1;
    });

    // connection styling, see http://jsplumbtoolkit.com/doc/paint-styles
    var connectStyles = {
        hasRequirements: {
            strokeStyle: "#015100",
            lineWidth: 3
        },
        hasNotRequirements: {
            strokeStyle: "#510009",
            lineWidth: 3
        }
    };

    // our anchors, format: [x-coordinate 0-1, y-coordinate 0-1, dx {-1;+1}, dy {-1;+1}
    // the first two parameters mark the point where the line starts at the endpoint
    // (change if you need, or add more entries),
    // the last two parameters give the initial direction of the line (do not change).
    // the anchors at the beginning of the anchor-arrays will be used first (try to keep it symmetrical)
    var leftAnchors = [
        [0, 0.5, -1, 0],
        [0, 0.3, -1, 0],
        [0, 0.7, -1, 0],
        [0, 0.9, -1, 0]
    ];
    var rightAnchors = [
        [1, 0.5, 1, 0],
        [1, 0.3, 1, 0],
        [1, 0.7, 1, 0],
        [1, 0.9, 1, 0]
    ];
    var bottomAnchors = [
        [0.5, 1, 0, 1],
        [0.3, 1, 0, 1],
        [0.7, 1, 0, 1],
        [0.9, 1, 0, 1]
    ];
    var topAnchors = [
        [0.5, 0, 0, -1],
        [0.3, 0, 0, -1],
        [0.7, 0, 0, -1],
        [0.9, 0, 0, -1]
    ];// 0.9 is used in tech TECH_NETZTECHNIK. i guess that's the only one


    // we do not want to use the same anchor twice. so we have to remember which of the anchors was already used
    var alreadyUsedAnchors = {};
    function chooseAnchor(elemId, orientation, anchors, alreadyUsedAnchors) {
        if (!alreadyUsedAnchors[elemId]) { alreadyUsedAnchors[elemId] = {}; }
        if (alreadyUsedAnchors[elemId][orientation] == undefined) { alreadyUsedAnchors[elemId][orientation] = 0; }

        // a stupid algorithm perhaps, but we only track the number of already used anchors.
        // one after we other is used. could be modified in the future; should be enough for now.
        ++alreadyUsedAnchors[elemId][orientation];
        return anchors[alreadyUsedAnchors[elemId][orientation] - 1];
    }

    // draw each connection
    $.each(connections, function() {
        var $source = $techtree.find(".tech" + this.source + " a");
        var $target = $techtree.find(".tech" + this.target + " a");
        var connectOptions = {
            source: $source,
            target: $target,
            overlays: [
                ["Arrow", {location: -5, paintStyle: connectStyles[this.paintStyle], width: 8, length: 8, foldback: 0.8}],
                ["Label", {label: this.label, cssClass: "label " + this.paintStyle, location: 0.85}]
            ],
            paintStyle: connectStyles[this.paintStyle],
            hoverPaintStyle:{ strokeStyle:"rgb(255, 255, 0)" }
        };
        // if you want to change options
        // consult the documentation at http://jsplumbtoolkit.com for details

        //var sourcePosition = $source.offset();
        //var targetPosition = $target.offset();
        //console.info("source:" + $source.attr('data-tech-name') + ", target: " + $target.attr('data-tech-name') + ". pos " + sourcePosition.left + "|" + sourcePosition.top + " vs pos " + targetPosition.left + "|" + targetPosition.top);



        //#############################################
        // search for the path that we line should take
        // it may not go through other endpoints/techs
        // it should use the shortest path and should not change directions too often (1-2)
        if (translated[this.target].left < translated[this.source].left) {
            // target is left of the source
            if (!lineInCoordinatesBlocked(translated,translated[this.source].left,translated[this.source].top,translated[this.source].left,translated[this.target].top)
                && !positionInCoordinatesBlocked(translated,translated[this.source].left,translated[this.target].top)
                && !lineInCoordinatesBlocked(translated,translated[this.source].left,translated[this.target].top,translated[this.target].left,translated[this.target].top)
            ) { // vertical, horizontal
                connectOptions.anchors = [chooseAnchor(this.source, 'top',   topAnchors,   alreadyUsedAnchors),
                    chooseAnchor(this.target, 'right', rightAnchors, alreadyUsedAnchors)];
                //console.log("chose top right");
            } else { // horizontal, vertical
                connectOptions.anchors = [chooseAnchor(this.source, 'left',   leftAnchors,   alreadyUsedAnchors),
                    chooseAnchor(this.target, 'bottom', bottomAnchors, alreadyUsedAnchors)];
                connectOptions.overlays[1][1] = readableVersionOfLabel(connectOptions.overlays[1][1], alreadyUsedAnchors[this.target].bottom);
                //console.log("chose left bottom");
            }

        } else if (translated[this.target].left > translated[this.source].left) {
            // target is right of the source
            if (!lineInCoordinatesBlocked(translated,translated[this.source].left,translated[this.source].top,translated[this.source].left,translated[this.target].top)
                && !positionInCoordinatesBlocked(translated,translated[this.source].left,translated[this.target].top)
                && !lineInCoordinatesBlocked(translated,translated[this.source].left,translated[this.target].top,translated[this.target].left,translated[this.target].top)
            ) { // vertical, horizontal
                connectOptions.anchors = [chooseAnchor(this.source, 'top',  topAnchors,  alreadyUsedAnchors),
                    chooseAnchor(this.target, 'left', leftAnchors, alreadyUsedAnchors)];
                //console.log("chose top left");
            } else { // horizontal, vertical
                connectOptions.anchors = [chooseAnchor(this.source, 'right',  rightAnchors,  alreadyUsedAnchors),
                    chooseAnchor(this.target, 'bottom', bottomAnchors, alreadyUsedAnchors)];
                connectOptions.overlays[1][1] = readableVersionOfLabel(connectOptions.overlays[1][1], alreadyUsedAnchors[this.target].bottom);
                //console.log("chose right bottom");
            }
        } else {
            // target is above the source
            if (translated[this.target].top < translated[this.source].top - 1
                && lineInCoordinatesBlocked(translated,translated[this.source].left,translated[this.source].top,translated[this.target].left,translated[this.target].top)
            ) {
                // but it's far away and some tech/endpoint would block our path for a direct line
                connectOptions.anchors = [chooseAnchor(this.source, 'left', leftAnchors,  alreadyUsedAnchors),
                    chooseAnchor(this.target, 'left', leftAnchors, alreadyUsedAnchors)];
                //console.log("chose left left");
                // NOTE: i didn't find a case where this occurred,
                // but it could be possible that 2 connections on the same column could overlap with this rule
                // in this case, "right,right" should be chosen
            } else {
                // the target is right above us. shoot it.
                connectOptions.anchors = [chooseAnchor(this.source, 'top', topAnchors,  alreadyUsedAnchors),
                    chooseAnchor(this.target, 'bottom', bottomAnchors, alreadyUsedAnchors)];
                connectOptions.overlays[1][1] = readableVersionOfLabel(connectOptions.overlays[1][1], alreadyUsedAnchors[this.target].bottom);
                //console.log("chose top bottom");
            }
        }
        // else: default.

        newTree.connect(connectOptions);
    });
}

function initTechtree(id) {
    (function($){
        drawArrows(id);
    })(jQuery)

    tabletToggleTechtreeInfos(id);
}

/**
 *
 * @param labelObject object
 * @param useCount int
 * @returns string
 */
function readableVersionOfLabel(labelObject, useCount) {
    labelObject.location = (-0.05 * useCount +0.85);
    var split = labelObject.label.indexOf('/');
    if (split) {
//        label = label.substring(0,split) + '<br/>/<br/>' + label.substring(split + 1);
    }
    return labelObject;
}

/**
 * check single line of the matrix if a line could be drawn. does not check start or end of the line
 * @param coordinates array containing the positions of the endpoints
 * @param sourceLeft int x-coordinate of the source
 * @param sourceTop int y-coordinate of the source
 * @param targetLeft int x-coordinate of the target
 * @param targetTop int y-coordinate of the target
 * @return bool if the path is blocked by an element
 */
function lineInCoordinatesBlocked(coordinates, sourceLeft, sourceTop, targetLeft, targetTop) {
    if (sourceLeft == targetLeft) {
        // check column (target is above. every time!)
        for (var i in coordinates) {
            if (coordinates[i].left == sourceLeft && sourceTop > coordinates[i].top && targetTop < coordinates[i].top) {
                return true;
            }
        }
    } else if (sourceTop == targetTop && sourceLeft > targetLeft) {
        // check row to the left
        for (var j in coordinates) {
            if (coordinates[j].top == sourceTop && sourceLeft > coordinates[j].left && targetLeft < coordinates[j].left) {
                return true;
            }
        }
    } else if (sourceTop == targetTop && sourceLeft < targetLeft) {
        // check row to the right
        for (var k in coordinates) {
            if (coordinates[k].top == sourceTop && sourceLeft < coordinates[k].left && targetLeft > coordinates[k].left) {
                return true;
            }
        }
    }
    return false;
}

/**
 * check if a single spot in the coordinates array is blocked by an element. used for edges of the connection line
 * @param coordinates the coordinates matrix data
 * @param left x-coordinate to check
 * @param top y-coordinate to check
 * @returns {boolean}
 */
function positionInCoordinatesBlocked(coordinates, left, top) {
    for (var i in coordinates) {
        if (coordinates[i].left == left && coordinates[i].top == top) {
            return true;
        }
    }
    return false;
}
