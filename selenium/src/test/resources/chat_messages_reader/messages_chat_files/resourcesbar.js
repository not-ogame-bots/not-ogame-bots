var metalTicker     = undefined;
var crystalTicker   = undefined;
var deuteriumTicker = undefined;

function ResourceTicker() {
    this.timerObj = undefined
}

ResourceTicker.Metal = 'metal'
ResourceTicker.Crystal = 'crystal'
ResourceTicker.Deuterium = 'deuterium'
ResourceTicker.Energy = 'energy'

ResourceTicker.prototype.reload = function(data) {
    this.techs = data.techs || {}
    this.resources = data.resources || {}

    changeTooltip($("#metal_box"), data.resources.metal.tooltip);
    changeTooltip($("#crystal_box"), data.resources.crystal.tooltip);
    changeTooltip($("#deuterium_box"), data.resources.deuterium.tooltip);
    changeTooltip($("#darkmatter_box"), data.resources.darkmatter.tooltip);
    changeTooltip($("#energy_box"), data.resources.energy.tooltip);

    this.refresh()
}

ResourceTicker.prototype.start = function() {
    if(this.timerObj === undefined) {
        this.timerObj = timerHandler.appendCallback(this.update.bind(this))
    }
}
ResourceTicker.prototype.stop = function() {
    if(this.timerObj !== undefined) {
        timerHandler.removeCallback(this.timerObj)
        delete this.timerObj
    }
}

ResourceTicker.prototype.restart = function() {
    this.stop()
    this.start()
}

ResourceTicker.prototype.update = function() {
    let productionFactor = this.getProductionFactor()
    let production = this.calcProduction(productionFactor)

    if (this.resources.metal.amount < this.resources.metal.storage) {
        this.resources.metal.amount = Math.min(this.resources.metal.amount + production.metal, this.resources.metal.storage)
    }
    if (this.resources.crystal.amount < this.resources.crystal.storage) {
        this.resources.crystal.amount = Math.min(this.resources.crystal.amount + production.crystal, this.resources.crystal.storage)
    }
    if (this.resources.deuterium.amount < this.resources.deuterium.storage) {
        this.resources.deuterium.amount = Math.min(this.resources.deuterium.amount + production.deuterium, this.resources.deuterium.storage)
    }

    this.refresh()
}
ResourceTicker.prototype.refresh = function () {
    let elements = {
        metal:$('#resources_metal'),
        crystal:$('#resources_crystal'),
        deuterium: $('#resources_deuterium'),
        darkmatter:$('#resources_darkmatter'),
        energy:$('#resources_energy')
    }

    // metal
    elements.metal.html(gfNumberGetHumanReadable(this.resources.metal.amount.toFixed(0), isMobile))
    elements.metal.removeClass('overmark middlemark')
    storageClass = this.getStorageClass(this.resources.metal.amount,this.resources.metal.storage)
    if(storageClass) {
        elements.metal.toggleClass(storageClass,true)
    }

    // crystal
    elements.crystal.html(gfNumberGetHumanReadable(this.resources.crystal.amount.toFixed(0), isMobile))
    elements.crystal.removeClass('overmark middlemark')
    storageClass = this.getStorageClass(this.resources.crystal.amount,this.resources.crystal.storage)
    if(storageClass) {
        elements.crystal.toggleClass(storageClass,true)
    }

    // deuterium
    elements.deuterium.html(gfNumberGetHumanReadable(this.resources.deuterium.amount.toFixed(0), isMobile))
    elements.deuterium.removeClass('overmark middlemark')
    storageClass = this.getStorageClass(this.resources.deuterium.amount,this.resources.deuterium.storage)
    if(storageClass) {
        elements.deuterium.toggleClass(storageClass,true)
    }

    // darkmatter
    elements.darkmatter.html(gfNumberGetHumanReadable(this.resources.darkmatter.amount, isMobile))

    // energy
    elements.energy.html(gfNumberGetHumanReadable(this.resources.energy.amount.toFixed(0), isMobile))
    elements.energy.toggleClass('overmark',this.resources.energy.amount < 0)
}

ResourceTicker.prototype.getStorageClass = function(amount, storage)
{
    if(amount >= storage) {
        return 'overmark'
    } else if(amount >= (storage * 0.9)){
        return 'middlemark'
    }
    return undefined
}

ResourceTicker.prototype.getProduction = function (resource) {
    let production = 0
    for (let techId in this.techs) {
        let tech = this.techs[techId]
        if(this.hasResourcesAvailable(tech.consumption)) {
            production += tech.production[resource] || 0
        }

    }
    return production
}

ResourceTicker.prototype.getConsumption = function (resource) {
    let consumption = 0
    for (let techId in this.techs) {
        let tech = this.techs[techId]
        if(this.hasResourcesAvailable(tech.consumption)) {
            consumption += tech.consumption[resource] || 0
        }
    }
    return consumption
}

ResourceTicker.prototype.calcProduction = function(productionFactor) {
    let production = {
        metal:0,
        crystal:0,
        deuterium:0
    }
    for (let techId in this.techs) {
        let tech = this.techs[techId]
        if (this.hasResourcesAvailable(tech.consumption)) {

            for(let resource in production) {
                production[resource] += tech.production[resource] || 0
                production[resource] -= tech.consumption[resource] || 0
            }
        }
    }
    production = this.scaleResources(production, productionFactor)

    //base production
    for(let resource in production) {
        production[resource] += this.resources[resource].baseProduction || 0
    }
    return production
}

ResourceTicker.prototype.hasResourcesAvailable = function(resources) {
    for(let resource in resources) {
        if(resource !== "energy" && resources[resource] > 0 && this.resources[resource].amount < resources[resource]) {
            return false
        }
    }
    return true
}

ResourceTicker.prototype.getProductionFactor = function() {
    let energyProduced = this.getProduction(ResourceTicker.Energy)
    let energyNeeded = this.getConsumption(ResourceTicker.Energy)

    let factor = energyNeeded > 0 ? (energyProduced / energyNeeded) : 1;
    factor = Math.min(1, factor);
    return factor
}

ResourceTicker.prototype.scaleResources = function (resources, scale) {
    let resourcesScaled = {}
    for (let resource in resources) {
        resourcesScaled[resource] = resources[resource] * scale
    }
    return resourcesScaled
}

var resourcesBar = new ResourceTicker();

function reloadResources(data, callback)
{
    if (typeof(data) == 'string') {
        data = $.parseJSON(data);
    }

    resourcesBar.reload(data)

    if (data.vacation === true) {
        resourcesBar.stop()
    } else {
        resourcesBar.restart()
    }

    honorScore = data.honorScore;
    darkMatter = data.resources.darkmatter.amount

    if (typeof(callback) == 'function') {
        callback(data.resources);
    }
}

function getAjaxResourcebox(callback)    {
    $.get(ajaxResourceboxURI, function(data) {
        reloadResources(data, callback);
    }, "text");
}
