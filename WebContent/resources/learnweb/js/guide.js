/* global commandStartGuide, commandFinishGuide, Shepherd */

/* Welcome Page */
function welcomePageInitializeGuide(message, elementToAttach) {
  const guide = createGuide();
  guide.addSteps([
    {
      text: message,
      attachTo: {
        element: '.breadcrumb-toggle',
        on: 'right',
      },
      when: {
        show: commandStartGuide,
        cancel: commandFinishGuide,
      },
      classes: 'shepherd-arrow-left ms-2',
      advanceOn: { selector: '.breadcrumb-toggle', event: 'click' },
    },
    {
      text: message,
      attachTo: {
        element: elementToAttach,
        on: 'right',
      },
      classes: 'shepherd-arrow-left ms-2',
    },
  ]);
  return guide;
}

function welcomePageInitializeGuideSettings(message) {
  const guide = createGuide();
  guide.addSteps([
    {
      text: message,
      attachTo: {
        element: '.profile-picture',
        on: 'bottom',
      },
      when: {
        show: commandStartGuide,
        cancel: commandFinishGuide,
      },
      classes: 'shepherd-arrow-top mt-2',
      advanceOn: { selector: '.profile-picture', event: 'click' },
    },
    {
      text: message,
      attachTo: {
        element: '.guide-profile-page',
        on: 'left',
      },
      classes: 'shepherd-arrow-right me-2',
    },
  ]);
  return guide;
}

/* Resource Page */
function resourcePageInitializeGuide(message) {
  const guide = createGuide();
  guide.addSteps([
    {
      text: message,
      attachTo: {
        element: '.guide-add-resource',
        on: 'top',
      },
      advanceOn: { selector: '.guide-add-resource', event: 'click' },
      classes: 'shepherd-arrow-bottom',
    }, {
      text: message,
      attachTo: {
        element: '.guide-menu-add-resource',
        on: 'left',
      },
      advanceOn: { selector: '.guide-menu-add-resource', event: 'click' },
      classes: 'shepherd-arrow-right me-2',
    },
  ]);
  return guide;
}

/* Profile Page */
function profilePageInitializeGuide(message) {
  const guide = createGuide();
  guide.addStep({
    text: message,
    attachTo: {
      element: '.ui-fileupload-choose',
      on: 'top',
    },
    cancelIcon: { enabled: true },
    advanceOn: { selector: '.ui-fileupload-choose', event: 'click' },
    classes: 'shepherd-arrow-bottom mb-2',
  });
  return guide;
}

/* Group Pages */
function groupsPageInitializeGuide(message) {
  const guide = createGuide();
  guide.addStep({
    text: message,
    attachTo: {
      element: '.guide-groups-controls',
      on: 'top',
    },
    advanceOn: { selector: '.guide-create-group', event: 'click' },
    cancelIcon: { enabled: true },
    classes: 'shepherd-arrow-bottom mb-2',
  });
  return guide;
}

function groupsSearchPageInitializeGuide(filterGroups, exploreGroups, next) {
  const guide = createGuide();
  guide.addSteps([{
    text: filterGroups,
    attachTo: {
      element: '#other_groups',
      on: 'top',
    },
    buttons: [
      {
        text: next,
        action: guide.next,
        classes: 'me-0 ms-auto',
      },
    ],
    classes: 'shepherd-arrow-bottom mb-2',
  }, {
    text: exploreGroups,
    attachTo: {
      element: '.guide-col-title',
      on: 'top',
    },
    classes: 'shepherd-arrow-bottom mb-2',
  }]);
  return guide;
}

function groupOverviewPageInitializeGuide(joinGroup) {
  const guide = new Shepherd.Tour({
    defaultStepOptions: {
      scrollTo: { behavior: 'smooth', block: 'center' },
      cancelIcon: { enabled: true },
      when: {
        cancel: commandFinishGuide,
      },
    },
  });
  guide.addStep({
    text: joinGroup,
    attachTo: {
      element: '.guide-join-button',
      on: 'top',
    },
    advanceOn: { selector: '.guide-join-button', event: 'click' },
    classes: 'shepherd-arrow-bottom mb-2',
  });
  return guide;
}

function guideAfterComplete(finish, congratulations, yes, no, goToResource, goToGroups, goToSettings, ...stepsState) {
  const guide = createGuide();
  if (stepsState.every(Boolean)) {
    guide.addStep({
      text: finish,
      classes: 'shepherd-arrow-bottom',
      cancelIcon: { enabled: true },
      when: {
        cancel: commandFinishGuide,
      },
    });
  } else {
    guide.addStep({
      text: congratulations,
      classes: 'shepherd-arrow-bottom',
      cancelIcon: { enabled: true },
      when: {
        cancel: commandFinishGuide,
      },
      buttons: [
        {
          text: no,
          action() {
            commandFinishGuide();
            return guide.hide();
          },
        },
        {
          text: yes,
          action: guide.next,
        },
      ],
    });
  }
  if (!stepsState[0]) {
    guide.addSteps([
      {
        text: goToResource,
        attachTo: {
          element: '.breadcrumb-toggle',
          on: 'right',
        },
        classes: 'shepherd-arrow-left ms-2',
        advanceOn: { selector: '.breadcrumb-toggle', event: 'click' },
      },
      {
        text: goToResource,
        attachTo: {
          element: '.guide-resources',
          on: 'right',
        },
        classes: 'shepherd-arrow-left ms-2',
      },
    ]);
  }
  if (!stepsState[1]) {
    guide.addSteps([
      {
        text: goToGroups,
        attachTo: {
          element: '.breadcrumb-toggle',
          on: 'right',
        },
        classes: 'shepherd-arrow-left ms-2',
        advanceOn: { selector: '.breadcrumb-toggle', event: 'click' },
      },
      {
        text: goToGroups,
        attachTo: {
          element: '.guide-resources',
          on: 'right',
        },
        classes: 'shepherd-arrow-left ms-2',
      },
    ]);
  }
  if (!stepsState[2]) {
    guide.addSteps([
      {
        text: goToSettings,
        attachTo: {
          element: '.profile-picture',
          on: 'bottom',
        },
        classes: 'shepherd-arrow-top mt-2',
        advanceOn: { selector: '.breadcrumb-toggle', event: 'click' },
      },
      {
        text: goToSettings,
        attachTo: {
          element: '.guide-profile-page',
          on: 'left',
        },
        classes: 'shepherd-arrow-right me-2',
      }]);
  }

  return guide;
}

function createGuide() {
  return new Shepherd.Tour({
    defaultStepOptions: {
      scrollTo: { behavior: 'smooth', block: 'center' },
      cancelIcon: { enabled: true },
      when: {
        cancel: commandFinishGuide,
      },
    },
    useModalOverlay: true,
  });
}

function overlayClickCancelGuide(guide) {
  $(document).on('click', '.shepherd-modal-overlay-container', () => {
    guide.cancel();
    commandFinishGuide();
  });
}
