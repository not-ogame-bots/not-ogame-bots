function Tutorial(params) {
  this.urlGetTutorialMission = params.urlGetTutorialMission

  $('#inhalt .pagination a').click(function (e) {
    this.switchMission($(e.currentTarget).attr('ref'))
  }.bind(this))

  if(params.openStep != null) {
    this.switchMission(params.openStep)
  }
}

Tutorial.prototype.switchMission = function (missionID) {
  this.showPagination()
  var elementTutorialStep = $('#tutorialStep_' + missionID)

  if (elementTutorialStep.attr('class') === 'tut_new') {
    elementTutorialStep.attr('class', 'tutorial_done')
  }

  var className = elementTutorialStep.attr('class')
  let params = {
    missionID: missionID
  }

  $.post(this.urlGetTutorialMission, params, function (data) {
    this.displayMission(data)
  }.bind(this))

  $('#inhalt .pagination a').removeClass('currentpage').removeClass('tutorial_done_selected')
  elementTutorialStep.removeClass('tut_new')

  if (className === 'tutorial_done') {
    this.markStepDoneSelected(missionID)
  } else {
    this.markStepCurrent(missionID)
  }
}

Tutorial.prototype.showPagination = function() {
  $('#tutorial .pagination ul').show()
}

Tutorial.prototype.displayMission = function (data) {
  let json = JSON.parse(data);
  if(json) {
    $('#inhalt .content').html(json.content[json.target])
  }
}

Tutorial.prototype.markStepDone = function (missionID) {
  this.setTutorialStepClass(missionID, 'tutorial_done')
}

Tutorial.prototype.markStepDoneSelected = function (missionID) {
  this.setTutorialStepClass(missionID, 'tutorial_done_selected')
}

Tutorial.prototype.markStepCurrent = function (missionID) {
  this.setTutorialStepClass(missionID, 'currentpage')
}

Tutorial.prototype.setTutorialStepClass = function (missionID, className) {
  $('#tutorialStep_' + missionID).addClass(className)
}
