const FLEET_DISPATCH_PAGE1 = 'fleet1'
const FLEET_DISPATCH_PAGE2 = 'fleet2'
const FLEET_DISPATCH_PAGE3 = 'fleet3'

function getResourcesFromHeader(resourceId) {
  let value = $('#resources_' + resourceId).data('raw')
  return parseInt(value)
}

function getKeyCode(e) {
  if (window.event) {
    return window.event.keyCode
  } else if (e) {
    return e.which
  }
  return null
}

function FleetDispatcher(cfg) {
  this.fleetHelper = new FleetHelper(cfg)

  this.checkTargetUrl = cfg.checkTargetUrl
  this.sendFleetUrl = cfg.sendFleetUrl

  this.fleetSendingToken = cfg.fleetSendingToken || null

  this.currentPlanet = cfg.currentPlanet

  this.targetPlanet = cfg.targetPlanet || null
  this.targetInhabited = cfg.targetInhabited || false
  this.targetPlayerId = cfg.targetPlayerId || this.fleetHelper.PLAYER_ID_SPACE
  this.targetPlayerName = cfg.targetPlayerName || ''
  this.targetPlayerColorClass = cfg.targetPlayerColorClass || ''
  this.targetPlayerRankIcon = cfg.targetPlayerRankIcon || ''

  this.cargoCapacity = cfg.cargoCapacity
  this.fuelCapacity = cfg.fuelCapacity

  this.currentPage = FLEET_DISPATCH_PAGE1

  this.shipsOnPlanet = cfg.shipsOnPlanet || []
  this.shipsToSend = cfg.shipsToSend || []
  this.planets = cfg.planets || []
  this.standardFleets = cfg.standardFleets || []
  this.unions = cfg.unions || []

  this.orders = []
  this.orderNames = cfg.orderNames || []
  this.orderDescriptions = cfg.orderDescriptions || []
  this.mission = cfg.mission || this.fleetHelper.MISSION_NONE
  this.union = 0

  this.targetIsStrong = false
  this.targetIsOutlaw = false
  this.targetIsBuddyOrAllyMember = false
  this.playerIsOutlaw = false

  this.retreatAfterDefenderRetreat = false

  this.holdingTime = cfg.holdingTime
  this.expeditionTime = cfg.expeditionTime
  this.speedPercent = 10

  this.cargoMetal = 0
  this.cargoCrystal = 0
  this.cargoDeuterium = 0

  this.prioMetal = 1
  this.prioCrystal = 2
  this.prioDeuterium = 3

  this.metalOnPlanet = cfg.metalOnPlanet
  this.crystalOnPlanet = cfg.crystalOnPlanet
  this.deuteriumOnPlanet = cfg.deuteriumOnPlanet

  this.fleetCount = cfg.fleetCount
  this.maxFleetCount = cfg.maxFleetCount
  this.expeditionCount = cfg.expeditionCount
  this.maxExpeditionCount = cfg.maxExpeditionCount

  this.warningsEnabled = cfg.warningsEnabled

  this.playerId = cfg.playerId
  this.hasAdmiral = cfg.hasAdmiral
  this.hasCommander = cfg.hasCommander
  this.isOnVacation = cfg.isOnVacation

  this.moveInProgress = cfg.moveInProgress
  this.planetCount = cfg.planetCount
  this.explorationCount = cfg.explorationCount

  this.apiCommonData = cfg.apiCommonData
  this.apiTechData = cfg.apiTechData
  this.apiDefenseData = cfg.apiDefenseData

  this.loca = cfg.loca
  this.locadyn = cfg.locadyn
  this.errorCodeMap = cfg.errorCodeMap
  this.urlFleetCheck = cfg.urlFleetCheck

  this.timerTimes = null
}

FleetDispatcher.prototype.init = function () {
  this.initFleet1()
  this.initFleet2()
  this.initFleet3()

  let that = this;

  $('#fleetdispatchcomponent').on('keypress', function (e) {
    if (getKeyCode(e) === 13) {
      e.preventDefault()
      e.stopPropagation()
      if ($('#fleet1').is(':visible')) {
        that.trySubmitFleet1()

      } else if ($('#fleet2').is(':visible')) {
        that.trySubmitFleet2()

      } else {
        that.updateCargo()
        that.refreshCargo()
        that.trySubmitFleet3()
      }
      return false
    }
  })
}

FleetDispatcher.prototype.displayErrors = function (errors) {
  // only display the first error
  let error = errors[0] || undefined
  if (error) {
    fadeBox(error.message, true)
  }
}

FleetDispatcher.prototype.refresh = function () {
  switch (this.currentPage) {
    case FLEET_DISPATCH_PAGE1:
      this.refreshFleet1()
      break
    case FLEET_DISPATCH_PAGE2:
      this.refreshFleet2()
      break
    case FLEET_DISPATCH_PAGE3:
      this.refreshFleet3()
      break
  }
}

FleetDispatcher.prototype.switchToPage = function (page) {
  let that = this

  if (page === this.currentPage) {
    return
  }

  if (page === FLEET_DISPATCH_PAGE1) {
    this.currentPage = page

    $('#' + FLEET_DISPATCH_PAGE1).show()
    $('#' + FLEET_DISPATCH_PAGE2).hide()
    $('#' + FLEET_DISPATCH_PAGE3).hide()
    that.focusSubmitFleet1()
  }
  if (page === FLEET_DISPATCH_PAGE2) {
    this.currentPage = page

    $('#' + FLEET_DISPATCH_PAGE1).hide()
    $('#' + FLEET_DISPATCH_PAGE2).show()
    $('#' + FLEET_DISPATCH_PAGE3).hide()
    that.focusSubmitFleet2()
  }
  if (page === FLEET_DISPATCH_PAGE3) {
    this.currentPage = page

    $('#' + FLEET_DISPATCH_PAGE1).hide()
    $('#' + FLEET_DISPATCH_PAGE2).hide()
    $('#' + FLEET_DISPATCH_PAGE3).show()
    that.focusSubmitFleet3()
  }

  if (this.currentPage === FLEET_DISPATCH_PAGE1 && this.timerTimes !== null) {
    clearInterval(this.timerTimes)
    this.timerTimes = null
  }

  // create timer to refresh fleet arrival and return times
  if (this.currentPage === FLEET_DISPATCH_PAGE2 || this.currentPage === FLEET_DISPATCH_PAGE3) {
    if (this.timerTimes === null) {
      this.timerTimes = setInterval(function () {
        that.refreshFleetTimes()
      }, 1000)
    }
  }

  this.refresh()
}

FleetDispatcher.prototype.startLoading = function () {
  $('#fleetdispatchcomponent .ajax_loading').show()
}
FleetDispatcher.prototype.stopLoading = function () {
  $('#fleetdispatchcomponent .ajax_loading').hide()
}

FleetDispatcher.prototype.updateToken = function (tokenNew) {
  this.fleetSendingToken = tokenNew
}
FleetDispatcher.prototype.appendTokenParams = function (params) {
  params.token = this.fleetSendingToken
}

FleetDispatcher.prototype.appendShipParams = function (params) {
  this.shipsToSend.forEach(function (ship) {
    params['am' + ship.id] = ship.number
  })
}

FleetDispatcher.prototype.appendTargetParams = function (params) {
  params.galaxy = this.targetPlanet.galaxy
  params.system = this.targetPlanet.system
  params.position = this.targetPlanet.position
  params.type = this.targetPlanet.type
}

FleetDispatcher.prototype.appendCargoParams = function (params) {
  params.metal = this.cargoMetal
  params.crystal = this.cargoCrystal
  params.deuterium = this.cargoDeuterium
}

FleetDispatcher.prototype.appendPrioParams = function (params) {
  params.prioMetal = this.prioMetal
  params.prioCrystal = this.prioCrystal
  params.prioDeuterium = this.prioDeuterium
}

/**
 * FLEET 1
 */
FleetDispatcher.prototype.initFleet1 = function () {
  initToggleHeader('fleet1')

  let that = this
  let elem = $('#fleet1')

  elem.find('select.combatunits').ogameDropDown()
  that.refresh()

  elem.on('click', '#continueToFleet2', function (e) {
    e.preventDefault()
    that.trySubmitFleet1()
  })
  elem.on('keyup', '#technologies li input', function (e) {
    e.preventDefault()
    let shipId = parseInt($(e.currentTarget).closest('li').data('technology'))
    let number = getValue($(e.currentTarget).val())
    that.selectShip(shipId, number)
    that.refresh()
  })
  elem.on('click', '#technologies li .icon', function (e) {
    e.preventDefault()
    let shipId = parseInt($(e.currentTarget).closest('li').data('technology'))
    if(that.getNumberOfShipsSelected(shipId) < that.getNumberOfShipsOnPlanet(shipId)) {
      that.selectMaxShips(shipId)
    } else {
      that.selectShip(shipId, 0)
    }
    that.refresh()
    that.focusSubmitFleet1()
  })
  elem.on('click', '#sendall', function (e) {
    e.preventDefault()
    that.selectAllShips()
    that.refresh()
    that.focusSubmitFleet1()
  })
  elem.on('click', '#resetall', function (e) {
    e.preventDefault()
    that.resetShips()
    that.refresh()
    that.focusSubmitFleet1()
  })
  elem.on('change', '#standardfleet', function (e) {
    let standardFleetId = getValue($('select.combatunits').val())
    that.selectStandardFleet(standardFleetId)
    that.refresh()
  })
}

FleetDispatcher.prototype.focusSubmitFleet1 = function() {
  $('#continueToFleet2').focus()
}

FleetDispatcher.prototype.hasShipsSelected = function () {
  return this.getTotalNumberOfShipsSelected() > 0
}

FleetDispatcher.prototype.hasFreeSlots = function () {
  return (this.maxFleetCount - this.fleetCount) > 0
}

FleetDispatcher.prototype.hasEnoughFuel = function() {
  return this.deuteriumOnPlanet >= this.getConsumption()
}

FleetDispatcher.prototype.validateFleet1 = function (onError, onSuccess) {
  if (!this.hasShipsSelected()) {
    this.displayErrors([{message:this.loca.LOCA_FLEET_NO_SELECTION}])
    return false
  }
  if (!this.hasFreeSlots()) {
    this.displayErrors([{message:this.loca.LOCA_FLEET_NO_FREE_SLOTS}])
    return false
  }

  return true
}

FleetDispatcher.prototype.trySubmitFleet1 = function () {
  if (this.validateFleet1() === false) {
    return
  }

  this.switchToPage(FLEET_DISPATCH_PAGE2)
}

FleetDispatcher.prototype.refreshFleet1 = function () {
  this.refreshNavigationFleet1()
  this.refreshShips()
  this.refreshAPIData()
  this.refreshStatusBarFleet()
}

FleetDispatcher.prototype.refreshNavigationFleet1 = function () {
  let invalidInfo = ''
  if (!this.hasShipsSelected()) {
    $('#continueToFleet2').attr('class', 'continue off')
    invalidInfo = this.loca.LOCA_FLEET_NO_SELECTION
  } else if (!this.hasFreeSlots()) {
    $('#continueToFleet2').attr('class', 'continue off')
    invalidInfo = this.loca.LOCA_FLEET_NO_FREE_SLOTS
  } else {
    $('#continueToFleet2').attr('class', 'continue on')
  }

  $('#allornone .info').html(invalidInfo)
}

FleetDispatcher.prototype.refreshShips = function () {
  let that = this

  $('#fleet1 #technologies li').each(function (i, elem) {
    let shipId = $(elem).data('technology')
    let ship = that.findShip(shipId)
    if (ship) {
      let number = ship.number || 0
      $(elem).find('input').val(number)
    } else {
      $(elem).find('input').val('')
    }
  })
}

FleetDispatcher.prototype.refreshAPIData = function() {
  let apiShipData = this.shipsToSend.map(function(ship){ return [ship.id,ship.number] })
  let apiData = []
    .concat(this.apiCommonData)
    .concat(this.apiTechData)
    .concat(apiShipData).concat(this.apiDefenseData)
    .map(function(item) { return item.join(';')})
    .join('|')

  let title = this.loca.LOCA_API_FLEET_DATA + '<br/><input id="FLEETAPI" value="'
    + apiData + '" readonly onclick="select()" style="width: 360px;"></input>';

  changeTooltip($(".show_fleet_apikey"), title);
}

/**
 * SHIP LOGIC
 */
FleetDispatcher.prototype.selectShip = function (shipId, number) {

  let shipsAvailable = this.getNumberOfShipsOnPlanet(shipId)
  number = Math.min(shipsAvailable, number)

  if (number <= 0) {
    this.removeShip(shipId)
  } else if (this.hasShip(shipId)) {
    this.updateShip(shipId, number)
  } else {
    this.addShip(shipId, number)
  }

  this.resetCargo()
}

FleetDispatcher.prototype.addShip = function (shipId, number) {
  this.shipsToSend.push({id: shipId, number: number})
}

FleetDispatcher.prototype.findShip = function (shipId) {
  return this.shipsToSend.find(function (ship) {
    return ship.id === shipId
  })
}

FleetDispatcher.prototype.getNumberOfShipsSelected = function(shipId) {
  let ship = this.findShip(shipId)
  if(ship !== undefined) {
    return ship.number
  }
  return 0;
}

FleetDispatcher.prototype.hasShip = function (shipId) {
  return this.findShip(shipId) !== undefined
}

FleetDispatcher.prototype.hasColonizationShip = function () {
  return this.hasShip(this.SHIP_ID_COLONIZATION)
}

FleetDispatcher.prototype.hasRecycler = function () {
  return this.hasShip(this.SHIP_ID_RECYCLER)
}

FleetDispatcher.prototype.hasValidTarget = function () {
  return (this.targetPlanet.galaxy !== this.currentPlanet.galaxy
      || this.targetPlanet.system !== this.currentPlanet.system
      || this.targetPlanet.position !== this.currentPlanet.position
      || this.targetPlanet.type !== this.currentPlanet.type)
    && this.targetPlanet.galaxy > 0
    && this.targetPlanet.system > 0
    && this.targetPlanet.position > 0
}

FleetDispatcher.prototype.removeShip = function (shipId) {
  let shipIndex = this.shipsToSend.findIndex(function (ship) {
    return ship.id === shipId
  })

  if (shipIndex != -1) {
    this.shipsToSend.splice(shipIndex, 1)
  }
}

FleetDispatcher.prototype.updateShip = function (shipId, number) {
  let ship = this.findShip(shipId)
  if (ship) {
    ship.number = number
  }
}

FleetDispatcher.prototype.getNumberOfShipsOnPlanet = function (shipId) {
  let ship = this.shipsOnPlanet.find(function (ship) {
    return ship.id === shipId
  })

  if (ship) {
    return ship.number
  }

  return 0
}

FleetDispatcher.prototype.getTotalNumberOfShipsSelected = function () {
  let numberOfShipsSelected = 0
  this.shipsToSend.forEach(function (ship) {
    numberOfShipsSelected += ship.number
  })
  return numberOfShipsSelected
}

FleetDispatcher.prototype.getShipIds = function () {
  return this.shipsToSend.map(function (ship) {
    return ship.id
  })
}

FleetDispatcher.prototype.resetShips = function () {
  this.shipsToSend = []
}

FleetDispatcher.prototype.selectAllShips = function () {
  let that = this
  this.shipsOnPlanet.forEach(function (ship) {
    that.selectShip(ship.id, ship.number)
  })
}

FleetDispatcher.prototype.selectMaxShips = function (shipId) {
  let number = this.getNumberOfShipsOnPlanet(shipId)
  this.selectShip(shipId, number)
}

FleetDispatcher.prototype.selectShips = function (ships) {
  for (let shipId in ships) {
    let number = ships[shipId]

    this.selectShip(parseInt(shipId), number)
  }
}

FleetDispatcher.prototype.selectStandardFleet = function (standardFleetId) {
  let standardFleet = this.standardFleets.find(function (item) {
    return item.id === standardFleetId
  })

  if (standardFleet === undefined || standardFleet.ships === undefined) {
    return
  }

  this.selectShips(standardFleet.ships)
}

/**
 * FLEET 2
 */

FleetDispatcher.prototype.initFleet2 = function () {

  // @todo jquery is loaded twice
  addPercentageBarPlugin()


  initToggleHeader('fleet2')

  let that = this
  let elem = $('#fleet2').off()

  $('#speedPercentage').percentageBar().on('change', function (e) {
    that.setFleetPercent(e.value)
    that.refresh()
    that.focusSubmitFleet2()
  })

  elem.find('#slbox').ogameDropDown()
  elem.find('#aksbox').ogameDropDown()

  elem.on('keypress', function (e) {
      if (getKeyCode(e) === 13) {
          e.preventDefault()
          e.stopPropagation()
          that.trySubmitFleet2()
          return false
      }
  })

  // back and forth navigation
  elem.on('click', '#continueToFleet3', function (e) {
    e.preventDefault()
    that.trySubmitFleet2()
  })
  elem.on('click', '#backToFleet1', function (e) {
    e.preventDefault()
    that.switchToPage(FLEET_DISPATCH_PAGE1)
  })

  // clear inputs on focus
  elem.on('focus', '#galaxy', function () {
    clearInput('#galaxy')
    that.targetPlanet.galaxy = ''
    that.refreshFleet2()
  })
  elem.on('focus', '#system', function () {
    clearInput('#system')
    that.targetPlanet.system = ''
    that.refreshFleet2()
  })
  elem.on('focus', '#position', function () {
    clearInput('#position')
    that.targetPlanet.position = ''
    that.refreshFleet2()
  })

  elem.on('keyup', '#galaxy, #system, #position', function (e) {
    that.updateTarget()
    that.updateTargetDropDowns()
    that.refresh()
  })
  elem.on('click', '#pbutton', function (e) {
    e.preventDefault()
    that.setTargetType(that.fleetHelper.PLANETTYPE_PLANET)
    that.updateTargetDropDowns()
    that.refresh()
    that.focusSubmitFleet2()
  })
  elem.on('click', '#mbutton', function (e) {
    e.preventDefault()
    that.setTargetType(that.fleetHelper.PLANETTYPE_MOON)
    that.updateTargetDropDowns()
    that.refresh()
    that.focusSubmitFleet2()
  })
  elem.on('click', '#dbutton', function (e) {
    e.preventDefault()
    that.setTargetType(that.fleetHelper.PLANETTYPE_DEBRIS)
    that.updateTargetDropDowns()
    that.refresh()
    that.focusSubmitFleet2()
  })

  elem.on('change', '#slbox', function (e) {
    e.preventDefault()
    that.selectShortLink($(e.currentTarget))
    that.updateTarget()
    that.refresh()
    that.focusSubmitFleet2()
  })

  elem.on('change', '#aksbox', function (e) {
    e.preventDefault()
    that.selectCombatUnion($(e.currentTarget))
    that.updateTarget()
    that.refresh()
    that.focusSubmitFleet2()
  })
}

FleetDispatcher.prototype.focusSubmitFleet2 = function() {
  $('#continueToFleet3').focus()
}

FleetDispatcher.prototype.validateFleet2 = function () {
  if (!this.hasValidTarget()) {
    return false
  }

  return true
}

FleetDispatcher.prototype.trySubmitFleet2 = function () {
  let that = this

  if (this.validateFleet2() === false) {
    return
  }

  let params = {}
  this.appendShipParams(params)
  this.appendTargetParams(params)
  params.union = this.union

  if (this.hasColonizationShip()) {
    params.cs = 1
  }
  if (this.hasRecycler()) {
    params.recycler = 1
  }

  this.startLoading()

  $.post(this.checkTargetUrl, params, function (response) {
    let data = JSON.parse(response)
    let status = data.status || 'failure'

    that.stopLoading()

    // request successfull
    if (status === 'success') {
      that.setOrders(data.orders)
      that.setTargetInhabited(data.targetInhabited)
      that.setTargetPlayerId(data.targetPlayerId)
      that.setTargetPlayerName(data.targetPlayerName)
      that.setTargetIsStrong(data.targetIsStrong)
      that.setTargetIsOutlaw(data.targetIsOutlaw)
      that.setTargetIsBuddyOrAllyMember(data.targetIsBuddyOrAllyMember)
      that.setTargetPlayerColorClass(data.targetPlayerColorClass)
      that.setTargetPlayerRankIcon(data.targetPlayerRankIcon)
      that.setPlayerIsOutlaw(data.playerIsOutlaw)
      that.setTargetPlanet(data.targetPlanet)

      // select union attack if union was selected and not mission is set
      if (that.union !== 0
        && that.hasMission() === false
        && that.isMissionAvailable(that.fleetHelper.MISSION_UNIONATTACK)
      ) {
        that.selectMission(that.fleetHelper.MISSION_UNIONATTACK)
      }

      //select expedition if no mission is selected if it is the only one available
      else if (that.hasMission() === false && that.isOnlyMissionAvailable(that.fleetHelper.MISSION_EXPEDITION)) {
        that.selectMission(that.fleetHelper.MISSION_EXPEDITION)
      }

      that.switchToPage(FLEET_DISPATCH_PAGE3)
    }
    // request failed
    else {
      that.displayErrors(data.errors || [])
    }
  })
}

FleetDispatcher.prototype.refreshFleet2 = function () {
  this.refreshNavigationFleet2()
  this.refreshTarget()
  this.refreshBriefing()
  this.refreshStatusBarFleet()
}

FleetDispatcher.prototype.refreshNavigationFleet2 = function () {
  let invalidInfo = ''
  if (!this.hasShipsSelected()) {
    $('#continueToFleet3').attr('class', 'continue off')
    invalidInfo = this.loca.LOCA_FLEET_NO_SELECTION
  } else if(!this.hasEnoughFuel()) {
    $('#continueToFleet3').attr('class', 'continue off')
    invalidInfo = this.loca.LOCA_FLEETSENDING_NOT_ENOUGH_FOIL
  } else if (!this.hasFreeSlots()) {
    $('#continueToFleet3').attr('class', 'continue off')
    invalidInfo = this.loca.LOCA_FLEET_NO_FREE_SLOTS
  } else if (!this.hasValidTarget()) {
    invalidInfo = this.loca.LOCA_FLEETSENDING_NO_TARGET
    $('#continueToFleet3').attr('class', 'continue off')
  } else {
    $('#continueToFleet3').attr('class', 'continue on')
  }

  $('#allornone .info').html(invalidInfo)
}
FleetDispatcher.prototype.refreshTarget = function () {
  if (this.targetPlanet.type === this.fleetHelper.PLANETTYPE_PLANET) {
    $('#pbutton').attr('class', 'planet_selected')
  } else {
    $('#pbutton').attr('class', 'planet')
  }

  if (this.targetPlanet.type === this.fleetHelper.PLANETTYPE_DEBRIS) {
    $('#dbutton').attr('class', 'debris_selected')
  } else {
    $('#dbutton').attr('class', 'debris')
  }

  if (this.targetPlanet.type === this.fleetHelper.PLANETTYPE_MOON) {
    $('#mbutton').attr('class', 'moon_selected')
  } else {
    $('#mbutton').attr('class', 'moon')
  }

  $('#galaxy').val(this.targetPlanet.galaxy)
  $('#system').val(this.targetPlanet.system)
  $('#position').val(this.targetPlanet.position)
  $('#type').val(this.targetPlanet.type)

  $('#distanceValue').html(tsdpkt(this.getDistance()))

  let planetName = this.getOwnPlanetName(this.targetPlanet, this.targetPlanet.type)

  if (planetName !== undefined && planetName !== '') {
    $('#targetPlanetName').html(planetName)
  } else if (this.targetPlanet.type === this.fleetHelper.PLANETTYPE_PLANET) {
    $('#targetPlanetName').html(this.loca.LOCA_ALL_PLANET)
  } else if (this.targetPlanet.type === this.fleetHelper.PLANETTYPE_DEBRIS) {
    $('#targetPlanetName').html(this.loca.LOCA_FLEET_DEBRIS)
  } else if (this.targetPlanet.type === this.fleetHelper.PLANETTYPE_MOON) {
    $('#targetPlanetName').html(this.loca.LOCA_ALL_MOON)
  }
}

FleetDispatcher.prototype.refreshBriefing = function () {
  this.refreshTargetPlanet()
  this.refreshDuration()
  this.refreshConsumption()
  this.refreshStorage()
  this.refreshFleetTimes()
  this.refreshMaxSpeed()
}

FleetDispatcher.prototype.refreshTargetPlanet = function () {

  let planetIcon = this.getPlanetIcon(this.targetPlanet.type, false);
  let label = '[' + this.targetPlanet.galaxy + ':' + this.targetPlanet.system + ':' + this.targetPlanet.position + '] ' + planetIcon + this.targetPlanet.name

  let elem = $('#fleet3 #targetPlanet')

  let tooltip = this.targetInhabited === true ? this.loca.LOCA_ALL_PLAYER + ': ' + this.targetPlayerName : ''

  elem.toggleClass('tooltip', this.targetInhabited)
    .toggleClass('active', this.targetInhabited)
    .attr('title', tooltip)
    .html(label)

  changeTooltip(elem)
}

FleetDispatcher.prototype.refreshDuration = function () {
  let duration = this.getDuration()

  $('#fleet2 #duration, #fleet3 #duration').html(formatTime(duration) + ' h')
}
FleetDispatcher.prototype.refreshConsumption = function () {
  let fuelCapacity = this.getFuelCapacity()
  let deuterium = getResourcesFromHeader('deuterium')
  let consumption = this.getConsumption()
  let styleClass = ((consumption > fuelCapacity) || (consumption > deuterium)) ? 'overmark' : 'undermark'
  let fuelLevel = Math.ceil(100 * consumption / fuelCapacity)
  let htmlConsumption = '<span class="' + styleClass + '">' + tsdpkt(consumption) + ' (' + fuelLevel + '%)</span>'

  $('#fleet2 #consumption, #fleet3 #consumption').html(htmlConsumption)
}
FleetDispatcher.prototype.refreshStorage = function () {
  let cargoSpace = this.getFreeCargoSpace()
  let styleClass = cargoSpace < 0 ? 'overmark' : 'undermark'
  let htmlStorage = '<span class="' + styleClass + '">' + tsdpkt(cargoSpace) + '</span>'

  $('#storage').html(htmlStorage)
}

FleetDispatcher.prototype.refreshFleetTimes = function () {
  let duration = this.getDuration()
  let holdingTime = 0

  if (this.mission === this.fleetHelper.MISSION_EXPEDITION) {
    holdingTime = this.expeditionTime * 3600
  }

  if (this.mission === this.fleetHelper.MISSION_HOLD) {
    holdingTime = this.holdingTime * 3600
  }

  let arrivalTime = getFormatedDate(serverTime.getTime() + duration * 1000, '[d].[m].[y] [G]:[i]:[s]')
  let returnTime = getFormatedDate(serverTime.getTime() + (2 * duration + holdingTime) * 1000, '[d].[m].[y] [G]:[i]:[s]')

  $('#fleet2 #arrivalTime, #fleet3 #arrivalTime').html(arrivalTime)
  $('#fleet2 #returnTime, #fleet3 #returnTime').html(returnTime)

  if (this.mission === this.fleetHelper.MISSION_UNIONATTACK) {
    let union = this.getUnionData(this.union)
    if (union !== null) {
      let durationAKS = parseInt(union.time - serverTime.getTime() / 1000)
      let unionArrivalTime = formatTime(durationAKS)
      $('#durationAKS').html(unionArrivalTime)
    }
  }
}

FleetDispatcher.prototype.refreshMaxSpeed = function () {
  let maxSpeed = this.getMaxSpeed()

  $('#maxspeed').html(tsdpkt(maxSpeed))
}

FleetDispatcher.prototype.getPlanetIcon = function(planetType, showTooltip) {
  showTooltip = showTooltip || true

  let className = '';
  let name = '';
  switch (planetType) {
    case this.fleetHelper.PLANETTYPE_MOON:
      className = "moon";
      name = this.loca.LOCA_ALL_MOON;
      break;
    case this.fleetHelper.PLANETTYPE_DEBRIS:
      className = "tf";
      name = this.loca.LOCA_FLEET_DEBRIS;
      break;
    case this.fleetHelper.PLANETTYPE_PLANET:
    default:
      className = "planet";
      name = this.loca.LOCA_ALL_PLANET;
  }
  let title = '';
  if (showTooltip) {
    className += " tooltip js_hideTipOnMobile";
    title = ' title="' + name + '"';
  }
  return '<figure class="planetIcon ' + className + '"' + title +'></figure>';
}

FleetDispatcher.prototype.updateTarget = function () {
  let galaxy = clampInt(getValue($('#galaxy').val()), 1, this.fleetHelper.MAX_GALAXY, true)
  let system = clampInt(getValue($('#system').val()), 1, this.fleetHelper.MAX_SYSTEM, true)
  let position = clampInt(getValue($('#position').val()), 1, this.fleetHelper.MAX_POSITION, true)

  this.targetPlanet.galaxy = galaxy
  this.targetPlanet.system = system
  this.targetPlanet.position = position

  if (this.targetPlanet.position === this.fleetHelper.EXPEDITION_POSITION) {
    this.targetPlanet.type = this.fleetHelper.PLANETTYPE_PLANET
  }
}

FleetDispatcher.prototype.updateTargetDropDowns = function() {
  this.resetDropDown("#slbox")
  this.resetDropDown("#aksbox")
}

FleetDispatcher.prototype.resetDropDown = function(elementId) {
  let coords = [
    this.targetPlanet.galaxy,
    this.targetPlanet.system,
    this.targetPlanet.position,
    this.targetPlanet.type
  ].join('#')

  let selection = $(elementId).find("option[value^=\""+coords+"\"]")

  if(selection.length === 0) {
    $(elementId).ogameDropDown('select','-');
  }
}

FleetDispatcher.prototype.selectShortLink = function (elem) {
  let value = elem.val()
  let parts = value.split('#')

  if (parts instanceof Array && parts.length >= 5) {

    $('#galaxy').val(parts[0])
    $('#system').val(parts[1])
    $('#position').val(parts[2])

    this.setTargetType(parseInt(parts[3]))
  }
}
FleetDispatcher.prototype.setTargetType = function (type) {
  this.targetPlanet.type = type
}

FleetDispatcher.prototype.selectCombatUnion = function (elem) {
  let value = elem.val()
  let parts = value.split('#')

  if (parts instanceof Array && parts.length >= 5) {
    $('#galaxy').val(parts[0])
    $('#system').val(parts[1])
    $('#position').val(parts[2])

    this.setTargetType(parseInt(parts[3]))

    this.union = parseInt(parts[5])

  } else {
    this.mission = this.fleetHelper.MISSION_NONE
    this.union = 0
  }
}

FleetDispatcher.prototype.setFleetPercent = function (speedPercent) {
  this.speedPercent = speedPercent
}

FleetDispatcher.prototype.findOwnPlanet = function (coords, type) {
  if (!coords) return undefined
  if (!type) return undefined

  let planet = this.planets.find(function (elem) {
    if (elem.galaxy != coords.galaxy) return false
    if (elem.system != coords.system) return false
    if (elem.position != coords.position) return false
    if (elem.type != type) return false

    return true
  })

  return planet
}

FleetDispatcher.prototype.getOwnPlanetName = function (coords, type) {
  if (!coords) return undefined
  if (!type) return undefined

  let planet = this.findOwnPlanet(coords, type)

  if (planet) {
    return planet.name
  }

  return undefined
}

FleetDispatcher.prototype.getDistance = function () {
  return this.fleetHelper.calcDistance(this.currentPlanet, this.targetPlanet)
}

FleetDispatcher.prototype.getMaxSpeed = function () {
  let shipIds = this.getShipIds()
  return this.fleetHelper.getMaxSpeed(shipIds)
}

FleetDispatcher.prototype.getConsumption = function () {
  return this.fleetHelper.calcConsumption(this.shipsToSend, this.getDistance(), this.speedPercent, this.getHoldingTime())
}

FleetDispatcher.prototype.getDuration = function () {
  let distance = this.getDistance()
  let maxSpeed = this.getMaxSpeed()

  return this.fleetHelper.calcDuration(distance, maxSpeed, this.speedPercent)
}

FleetDispatcher.prototype.getHoldingTime = function() {
  switch(this.mission)
  {
    case this.fleetHelper.MISSION_EXPEDITION:
      return this.expeditionTime
    case this.fleetHelper.MISSION_HOLD:
      return this.holdingTime
    default:
      return 0
  }
}

FleetDispatcher.prototype.getMaxSpeed = function () {
  let shipIds = this.getShipIds()
  return this.fleetHelper.getMaxSpeed(shipIds)
}

FleetDispatcher.prototype.getCargoCapacity = function () {
  let that = this
  let cargoCapacity = 0

  this.shipsToSend.forEach(function (ship) {
    cargoCapacity += that.fleetHelper.calcCargoCapacity(ship.id, ship.number)
  })

  return Math.floor(cargoCapacity)
}

FleetDispatcher.prototype.getFuelCapacity = function () {
  let that = this
  let fuelCapacity = 0

  this.shipsToSend.forEach(function (ship) {
    fuelCapacity += that.fleetHelper.calcFuelCapacity(ship.id, ship.number)
  })

  return Math.floor(fuelCapacity)
}

FleetDispatcher.prototype.getFreeCargoSpace = function () {
  return this.getCargoCapacity() - this.cargoMetal - this.cargoCrystal - this.cargoDeuterium
}

FleetDispatcher.prototype.getUsedCargoSpace = function () {
  return this.cargoMetal + this.cargoCrystal + this.cargoDeuterium
}

FleetDispatcher.prototype.setOrders = function (ordersNew) {
  this.orders = ordersNew
}

FleetDispatcher.prototype.setTargetInhabited = function (inhabitedNew) {
  this.targetInhabited = inhabitedNew
}

FleetDispatcher.prototype.setTargetPlayerId = function (targetPlayerIdNew) {
  this.targetPlayerId = targetPlayerIdNew
}

FleetDispatcher.prototype.setTargetPlayerName = function (targetPlayerNameNew) {
  this.targetPlayerName = targetPlayerNameNew
}

FleetDispatcher.prototype.setTargetIsStrong = function (targetIsStrongNew) {
  this.targetIsStrong = targetIsStrongNew
}

FleetDispatcher.prototype.setTargetIsOutlaw = function (targetIsOutlawNew) {
  this.targetIsOutlaw = targetIsOutlawNew
}

FleetDispatcher.prototype.setTargetIsBuddyOrAllyMember = function (targetIsBuddyOrAllyMemberNew) {
  this.targetIsBuddyOrAllyMember = targetIsBuddyOrAllyMemberNew
}

FleetDispatcher.prototype.setPlayerIsOutlaw = function (playerIsOutlawNew) {
  this.playerIsOutlaw = playerIsOutlawNew
}

FleetDispatcher.prototype.setTargetPlayerColorClass = function(targetPlayerColorClassNew) {
  this.targetPlayerColorClass = targetPlayerColorClassNew
}

FleetDispatcher.prototype.setTargetPlayerRankIcon = function(targetPlayerRankIconNew) {
  this.targetPlayerRankIcon = targetPlayerRankIconNew
}

FleetDispatcher.prototype.setTargetPlanet = function(targetPlanetNew) {
  this.targetPlanet = targetPlanetNew
}

/**
 * FLEET 3
 */

FleetDispatcher.prototype.initFleet3 = function () {
  initToggleHeader('fleet3')

  let that = this
  let elem = $('#fleet3').off()

  elem.find('select[name=expeditiontime]').ogameDropDown()
  elem.find('select[name=holdingtime]').ogameDropDown()


  elem.on('click', '#sendFleet', function (e) {
    e.preventDefault()
    that.trySubmitFleet3()
  })

  elem.on('click', '#backToFleet2', function (e) {
    e.preventDefault()
    that.switchToPage(FLEET_DISPATCH_PAGE2)
  })

  elem.on('click', '#selectMaxMetal', function (e) {
    e.preventDefault()
    that.selectMaxMetal()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('click', '#selectMinMetal', function (e) {
    e.preventDefault()
    that.selectMinMetal()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('click', '#selectMaxCrystal', function (e) {
    e.preventDefault()
    that.selectMaxCrystal()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('click', '#selectMinCrystal', function (e) {
    e.preventDefault()
    that.selectMinCrystal()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('click', '#selectMaxDeuterium', function (e) {
    e.preventDefault()
    that.selectMaxDeuterium()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('click', '#selectMinDeuterium', function (e) {
    e.preventDefault()
    that.selectMinDeuterium()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('click', '#allresources', function (e) {
    e.preventDefault()
    that.selectMaxAll()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('keyup', '#metal', function (e) {
    that.updateMetal()
    that.refresh()
  })

  elem.on('change', '#metal', function (e) {
    that.updateMetal()
    that.refresh()
  })

  elem.on('keyup', '#crystal', function (e) {
    that.updateCrystal()
    that.refresh()
  })

  elem.on('change', '#crystal', function (e) {
    that.updateCrystal()
    that.refresh()
  })

  elem.on('keyup', '#deuterium', function (e) {
    that.updateDeuterium()
    that.refresh()
  })

  elem.on('change', '#deuterium', function (e) {
    that.updateDeuterium()
    that.refresh()
  })

  elem.on('click', '#missions > li > a', function (e) {
    e.preventDefault()
    let mission = parseInt($(e.currentTarget).data('mission') || this.fleetHelper.MISSION_NONE)
    that.selectMission(mission)
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('change', 'input[name=retreatAfterDefenderRetreat]', function (e) {
    that.selectRetreatAfterDefenderRetreat($(e.currentTarget).is(':checked'))
  })

  elem.on('change keyup', '#holdingtime', function () {
    that.updateHoldingTime()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('change keyup', '#expeditiontime', function (e) {
    that.updateExpeditionTime()
    that.refresh()
    that.focusSubmitFleet3()
  })

  elem.on('click', '.prioButton', function (e) {
    e.preventDefault()
    let type = $(e.currentTarget).attr('data-resource-type')
    let priority = parseInt($(e.currentTarget).attr('data-resource-prio'))
    that.selectPriority(type, priority)
    that.refresh()
    that.focusSubmitFleet3()
  })
}

FleetDispatcher.prototype.focusSubmitFleet3 = function() {
  $('#sendFleet').focus()
}

FleetDispatcher.prototype.validateFleet3 = function () {
  if (!this.hasMission()) {
    return false
  }

  return true
}

FleetDispatcher.prototype.trySubmitFleet3 = function () {
  let that = this

  if (this.validateFleet3() === false) {
    return
  }

  if (this.moveInProgress) {
    errorBoxDecision(
      this.loca.LOCA_ALL_NETWORK_ATTENTION,
      this.loca.LOCA_PLANETMOVE_BREAKUP_WARNING,
      this.loca.LOCA_ALL_YES,
      this.loca.LOCA_ALL_NO,
      function () {
        that.submitFleet3()
      }
    )
  } else if (this.warningsEnabled && this.targetIsStrong && !this.targetIsOutlaw && !this.targetIsBuddyOrAllyMember && !this.playerIsOutlaw
    && this.fleetHelper.isAggressiveMission(this.mission)) {
    errorBoxDecision(
      this.loca.LOCA_ALL_NETWORK_ATTENTION,
      this.locadyn.locaAllOutlawWarning,
      this.loca.LOCA_ALL_YES,
      this.loca.LOCA_ALL_NO,
      function () {
        that.submitFleet3()
      }
    )
  } else if (this.mission === this.fleetHelper.MISSION_COLONIZE && this.fleetHelper.COLONIZATION_ENABLED === true && !this.hasFreePlanetSlots()) {
    errorBoxDecision(
      this.loca.LOCA_ALL_NOTICE,
      this.loca.LOCA_FLEETSENDING_MAX_PLANET_WARNING,
      this.loca.LOCA_ALL_YES,
      this.loca.LOCA_ALL_NO,
      function () {
        that.submitFleet3()
      }
    )
  } else {
    this.submitFleet3()
  }
}

FleetDispatcher.prototype.submitFleet3 = function (force) {
  force = force || false

  let that = this

  let params = {}
  this.appendTokenParams(params)
  this.appendShipParams(params)
  this.appendTargetParams(params)
  this.appendCargoParams(params)
  this.appendPrioParams(params)
  params.mission = this.mission
  params.speed = this.speedPercent
  params.retreatAfterDefenderRetreat = this.retreatAfterDefenderRetreat === true ? 1 : 0
  params.union = this.union
  if (force) params.force = force
  params.holdingtime = this.getHoldingTime()

  this.startLoading()

  $.post(this.sendFleetUrl, params, function (response) {
    let data = JSON.parse(response)

    that.updateToken(data.fleetSendingToken || '')

    // request successful
    if (data.success  === true) {
      fadeBox(data.message,false);

      setTimeout(function(){
        window.location = data.redirectUrl
      }, 50)
    }
    // request failed
    else {
      that.stopLoading()

      // @TODO display confirmation popup to infringe bashlimit rules
      if (data.responseArray && data.responseArray.limitReached && !data.responseArray.force) {
        errorBoxDecision(
          that.loca.LOCA_ALL_NETWORK_ATTENTION,
          that.locadyn.localBashWarning,
          that.loca.LOCA_ALL_YES,
          that.loca.LOCA_ALL_NO,
          function () {
            that.submitFleet3(true)
          }
        )
      } else {
        that.displayErrors(data.errors)
      }
    }
  })
}

FleetDispatcher.prototype.refreshFleet3 = function () {
  this.refreshNavigationFleet3()
  this.refreshStatusBarFleet()
  this.refreshMissions()
  this.refreshCargo()
  this.refreshPriorities()
  this.refreshBriefing()
  this.updateHoldingTime()
  this.updateExpeditionTime()
}

FleetDispatcher.prototype.refreshNavigationFleet3 = function () {
  let invalidInfo = ''
  if (!this.hasShipsSelected()) {
    $('#sendFleet').attr('class', 'start off')
    invalidInfo = this.loca.LOCA_FLEET_NO_SELECTION
  } else if (!this.hasFreeSlots()) {
    $('#sendFleet').attr('class', 'start off')
    invalidInfo = this.loca.LOCA_FLEET_NO_FREE_SLOTS
  } else if (!this.hasMission()) {
    $('#sendFleet').attr('class', 'start off')
    invalidInfo = this.loca.fleetNoMission
  } else {
    $('#sendFleet').attr('class', 'start on')
  }

  $('#allornone .info').html(invalidInfo)
}

FleetDispatcher.prototype.refreshStatusBarFleet = function() {
  this.hasValidTarget()

  let missionData = this.getMissionData(this.mission)
  let missionName = missionData !== null ? missionData.name : this.loca.LOCA_FLEET_NO_SELECTION
  let planetIcon = this.getPlanetIcon(this.targetPlanet.type, false);
  let targetName = '[' + this.targetPlanet.galaxy + ':' + this.targetPlanet.system + ':' + this.targetPlanet.position + '] ' + planetIcon + this.targetPlanet.name

  $('#statusBarFleet .missionName').text(missionName)
  $('#statusBarFleet .targetName').html(targetName)

  let elemTargetPlayerName = $('#statusBarFleet .targetPlayerName')
  if(this.targetPlanet.type === this.fleetHelper.PLANETTYPE_DEBRIS) {
    elemTargetPlayerName.closest('li').hide()
    elemTargetPlayerName.html('')
  } else {

    let targetPlayerName = ''
    if(this.targetPlayerId === 0 || this.fleetHelper.isPlayerSpace(this.targetPlayerId)) {
      targetPlayerName = this.loca.LOCA_EVENTH_ENEMY_INFINITELY_SPACE
    } else if(this.targetPlayerId === this.playerId) {
      targetPlayerName = this.targetPlayerName
    } else {
      targetPlayerName = this.targetPlayerRankIcon + '<span class="status_abbr_{color}">{name}</span>'
      targetPlayerName = targetPlayerName.replace('{color}',this.targetPlayerColorClass)
      targetPlayerName = targetPlayerName.replace('{name}',this.targetPlayerName)
    }

    elemTargetPlayerName.closest('li').show()
    elemTargetPlayerName.html(targetPlayerName)
  }
}

FleetDispatcher.prototype.refreshMissions = function () {
  $('#missions>li>a.selected').removeClass('selected')

  // refresh mission buttons
  for (let mission in this.orders) {
    let missionData = this.getMissionData(mission)

    $('#missions>li#button' + mission)
      .toggleClass('on', missionData.isAvailable === true)
      .toggleClass('off', missionData.isAvailable === false)
  }
  // @TODO LOCA_FLEET_NO_SELECTION when no mission selected

  // refresh mission
  let missionData = this.getMissionData(this.mission)
  if (missionData !== null) {
    $('.missionName').text(missionData.name)
    $('.mission_description').text(missionData.description)
    $('#missions>li#button' + this.mission + '>a').toggleClass('selected', true)

    if (missionData.isAvailable === false) {
      $('.briefing_overlay').show()
      $('#missionNameWrapper').addClass('off')
    } else {
      $('.briefing_overlay').hide()
      $('#missionNameWrapper').removeClass('off')
    }
  } else {
    $('.missionName').text(this.loca.LOCA_FLEET_NO_SELECTION)
  }

  $('form input[name="mission"]').val(this.mission)

  $('#fightAfterRetreat,' +
    '#aks,' +
    '#holdtimeline,' +
    '#expeditiontimeline,' +
    '.prioButton'
  )
    .hide()

  if (this.mission === this.fleetHelper.MISSION_ATTACK) {
    $('#fightAfterRetreat').show()
    $('.prioButton').show()
  }

  if (this.mission === this.fleetHelper.MISSION_UNIONATTACK) {
    $('#aks').show()
    $('.prioButton').show()
  }

  if (this.mission === this.fleetHelper.MISSION_HOLD) {
    $('#holdtimeline').show()
  }
  if (this.mission === this.fleetHelper.MISSION_EXPEDITION) {
    $('#expeditiontimeline').show()
  }

  //@TODO hide holdingtime or expeditiontime when mission is not selected
}


FleetDispatcher.prototype.refreshCargo = function () {
  formatNumber($('#metal'), this.cargoMetal)
  formatNumber($('#crystal'), this.cargoCrystal)
  formatNumber($('#deuterium'), this.cargoDeuterium)

  let cargoSpaceUsed = this.getUsedCargoSpace()
  let cargoSpaceFree = this.getFreeCargoSpace()
  let cargoCapacity = this.getCargoCapacity()

  let styleClass = cargoSpaceFree < 0 ? 'overmark' : 'undermark'
  $('#remainingresources').html('<span class="' + styleClass + '">' + tsdpkt(cargoSpaceFree) + '</style>')
  $('#maxresources').html(tsdpkt(cargoCapacity))


  $('#loadRoom .bar_container')
    .data('currentAmount', cargoSpaceUsed)
    .data('capacity', cargoCapacity)

  refreshBars('bar_container', 'filllevel_bar')
}

FleetDispatcher.prototype.refreshPriorities = function () {
  $('form input[name="prioMetal"]').val(this.prioMetal)
  $('form input[name="prioCrystal"]').val(this.prioCrystal)
  $('form input[name="prioDeuterium"]').val(this.prioDeuterium)

  $('#prioM1').attr('src', '//gf1.geo.gfsrv.net/cdn91/4b53e83f8b8583ea279fd26f3a55a5.gif')
  $('#prioM2').attr('src', '//gf3.geo.gfsrv.net/cdn26/8afbd59ffe091239a7c6f1e961b267.gif')
  $('#prioM3').attr('src', '//gf1.geo.gfsrv.net/cdn0a/4acc67e1ca4d8debb1b114abcb7c1e.gif')

  switch (this.prioMetal) {
    case 1:
      $('#prioM1').attr('src', '//gf1.geo.gfsrv.net/cdn9c/b357323b99e20a46fc0b2495728351.gif')
      break
    case 2:
      $('#prioM2').attr('src', '//gf3.geo.gfsrv.net/cdnb1/f8959fe540cd329f3a764ad9aeaf93.gif')
      break
    case 3:
      $('#prioM3').attr('src', '//gf1.geo.gfsrv.net/cdnf2/823b3270ed0f4a243287c12d4ee5f8.gif')
      break

  }

  $('#prioC1').attr('src', '//gf1.geo.gfsrv.net/cdn91/4b53e83f8b8583ea279fd26f3a55a5.gif')
  $('#prioC2').attr('src', '//gf3.geo.gfsrv.net/cdn26/8afbd59ffe091239a7c6f1e961b267.gif')
  $('#prioC3').attr('src', '//gf1.geo.gfsrv.net/cdn0a/4acc67e1ca4d8debb1b114abcb7c1e.gif')

  switch (this.prioCrystal) {
    case 1:
      $('#prioC1').attr('src', '//gf1.geo.gfsrv.net/cdn9c/b357323b99e20a46fc0b2495728351.gif')
      break
    case 2:
      $('#prioC2').attr('src', '//gf3.geo.gfsrv.net/cdnb1/f8959fe540cd329f3a764ad9aeaf93.gif')
      break
    case 3:
      $('#prioC3').attr('src', '//gf1.geo.gfsrv.net/cdnf2/823b3270ed0f4a243287c12d4ee5f8.gif')
      break

  }

  $('#prioD1').attr('src', '//gf1.geo.gfsrv.net/cdn91/4b53e83f8b8583ea279fd26f3a55a5.gif')
  $('#prioD2').attr('src', '//gf3.geo.gfsrv.net/cdn26/8afbd59ffe091239a7c6f1e961b267.gif')
  $('#prioD3').attr('src', '//gf1.geo.gfsrv.net/cdn0a/4acc67e1ca4d8debb1b114abcb7c1e.gif')

  switch (this.prioDeuterium) {
    case 1:
      $('#prioD1').attr('src', '//gf1.geo.gfsrv.net/cdn9c/b357323b99e20a46fc0b2495728351.gif')
      break
    case 2:
      $('#prioD2').attr('src', '//gf3.geo.gfsrv.net/cdnb1/f8959fe540cd329f3a764ad9aeaf93.gif')
      break
    case 3:
      $('#prioD3').attr('src', '//gf1.geo.gfsrv.net/cdnf2/823b3270ed0f4a243287c12d4ee5f8.gif')
      break

  }
}

FleetDispatcher.prototype.isMissionAvailable = function (missionId) {
  return this.orders[missionId] === true
}

FleetDispatcher.prototype.hasMission = function () {
  return (
    this.fleetHelper.isMissionValid(this.mission)
    &&
    this.isMissionAvailable(this.mission)
  )
}

FleetDispatcher.prototype.hasFreePlanetSlots = function () {
  return this.planetCount < this.fleetHelper.MAX_NUMBER_OF_PLANETS
}

FleetDispatcher.prototype.getAvailableMissions = function () {
  let missions = []
  for (let mission in this.orders) {
    if (this.orders[mission] === true) {
      missions.push(parseInt(mission))
    }
  }

  return missions
}

FleetDispatcher.prototype.isOnlyMissionAvailable = function (missionId) {
  let missionsAvailable = this.getAvailableMissions()

  return missionsAvailable.length === 1 && missionsAvailable[0] === missionId
}

FleetDispatcher.prototype.getMissionData = function (missionId) {
  if (missionId === this.fleetHelper.MISSION_NONE) {
    return null
  }

  return {
    isAvailable: this.orders[missionId] || false,
    name: this.orderNames[missionId] || '',
    description: this.orderDescriptions[missionId] || ''
  }
}

FleetDispatcher.prototype.getUnionData = function(unionId) {
  for(let i=0;i<this.unions.length;++i) {
    if(this.unions[i].id === unionId) {
      return this.unions[i];
    }
  }
  return null;
}
FleetDispatcher.prototype.selectMaxMetal = function () {
  let amount

  amount = this.getCargoCapacity() - this.cargoCrystal - this.cargoDeuterium
  amount = Math.max(amount, 0)
  amount = Math.min(amount, this.metalOnPlanet)

  this.cargoMetal = Math.max(this.cargoMetal, amount)
}

FleetDispatcher.prototype.selectMinMetal = function() {
  this.cargoMetal = 0;
}

FleetDispatcher.prototype.selectMaxCrystal = function () {
  let amount

  amount = this.getCargoCapacity() - this.cargoMetal - this.cargoDeuterium
  amount = Math.max(amount, 0)
  amount = Math.min(amount, this.crystalOnPlanet)

  this.cargoCrystal = Math.max(this.cargoCrystal, amount)
}

FleetDispatcher.prototype.getDeuteriumOnPlanetWithoutConsumption = function() {
  return Math.max(0, this.deuteriumOnPlanet - this.getConsumption())
}

FleetDispatcher.prototype.selectMinCrystal = function() {
  this.cargoCrystal = 0;
}

FleetDispatcher.prototype.selectMaxDeuterium = function () {
  let amount
  amount = this.getCargoCapacity() - this.cargoMetal - this.cargoCrystal
  amount = Math.max(amount, 0)
  amount = Math.min(amount, this.getDeuteriumOnPlanetWithoutConsumption())

  this.cargoDeuterium = Math.max(this.cargoDeuterium, amount)
}

FleetDispatcher.prototype.selectMinDeuterium = function() {
  this.cargoDeuterium = 0
}

FleetDispatcher.prototype.selectMaxAll = function () {
  this.cargoMetal = 0
  this.cargoCrystal = 0
  this.cargoDeuterium = 0

  this.selectMaxDeuterium()
  this.selectMaxCrystal()
  this.selectMaxMetal()
}

FleetDispatcher.prototype.resetCargo = function() {
  this.cargoMetal = 0
  this.cargoCrystal = 0
  this.cargoDeuterium = 0
}

FleetDispatcher.prototype.updateCargo = function() {
  this.updateMetal()
  this.updateCrystal()
  this.updateDeuterium()
}
FleetDispatcher.prototype.updateMetal = function () {
  let amount = getValue($('#metal').val())
  let cargoSpace = this.getCargoCapacity() - this.cargoCrystal - this.cargoDeuterium

  this.cargoMetal = Math.min(amount, this.metalOnPlanet, cargoSpace)
}

FleetDispatcher.prototype.updateCrystal = function () {
  let amount = getValue($('#crystal').val())
  let cargoSpace = this.getCargoCapacity() - this.cargoMetal - this.cargoDeuterium

  this.cargoCrystal = Math.min(amount, this.crystalOnPlanet, cargoSpace)
}

FleetDispatcher.prototype.updateDeuterium = function () {
  let amount = getValue($('#deuterium').val())
  let cargoSpace = this.getCargoCapacity() - this.cargoMetal - this.cargoCrystal
  let deuteriumOnPlanetWithoutConsumption = this.getDeuteriumOnPlanetWithoutConsumption()

  this.cargoDeuterium = Math.min(amount, this.deuteriumOnPlanet, cargoSpace, deuteriumOnPlanetWithoutConsumption)
}

FleetDispatcher.prototype.selectMission = function (mission) {
  if (this.fleetHelper.isMissionValid(mission)) {
    this.mission = mission
  }

  this.updateHoldingTime()
  this.updateExpeditionTime()
}

FleetDispatcher.prototype.selectRetreatAfterDefenderRetreat = function (retreatAfterDefenderRetreat) {
  this.retreatAfterDefenderRetreat = retreatAfterDefenderRetreat
}

FleetDispatcher.prototype.updateHoldingTime = function () {
  if (this.mission === this.fleetHelper.MISSION_HOLD) {
    this.holdingTime = getValue($('#fleet3 #holdingtime').val())
  } else {
    this.holdingTime = 0
  }
}

FleetDispatcher.prototype.updateExpeditionTime = function () {
  if (this.mission === this.fleetHelper.MISSION_EXPEDITION) {
    this.expeditionTime = getValue($('#fleet3 #expeditiontime').val())
  } else {
    this.expeditionTime = 0
  }
}

FleetDispatcher.prototype.selectPriority = function (type, priority) {
  switch (type) {
    case 'metal':
      if (this.prioMetal === priority) break
      if (this.prioCrystal === priority) this.prioCrystal = this.prioMetal
      if (this.prioDeuterium === priority) this.prioDeuterium = this.prioMetal
      this.prioMetal = priority
      break
    case 'crystal':
      if (this.prioCrystal === priority) break
      if (this.prioMetal === priority) this.prioMetal = this.prioCrystal
      if (this.prioDeuterium === priority) this.prioDeuterium = this.prioCrystal
      this.prioCrystal = priority
      break
    case 'deuterium':
      if (this.prioDeuterium === priority) break
      if (this.prioMetal === priority) this.prioMetal = this.prioDeuterium
      if (this.prioCrystal === priority) this.prioCrystal = this.prioDeuterium
      this.prioDeuterium = priority
      break
  }
}
