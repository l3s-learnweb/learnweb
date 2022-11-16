/* global Shepherd */
/* global guideBits, guideSteps, guideDismissBtn, guideGotchaBtn */
/* global commandGuideStepComplete */

const tour = new Shepherd.Tour({
  useModalOverlay: true,
  exitOnEsc: true,
  defaultStepOptions: {
    cancelIcon: {
      enabled: false,
    },
    // classes: 'class-1 class-2',
    scrollTo: { behavior: 'smooth', block: 'center' },
    buttons: [
      {
        text: guideDismissBtn,
        secondary: true,
        action() {
          this.cancel();
        },
      },
      {
        text: guideGotchaBtn,
        action() {
          this.next();
        },
      },
    ],
    when: {
      hide() {
        return commandGuideStepComplete([{ name: 'step', value: this.id }]);
      },
    },
  },
});

tour.on('complete', () => {
  tour.currentStep.hide();
});

const guideCompleted = guideBits.split('');
const guideBasePath = new URL(document.baseURI).pathname;
let guideCurrentPage = window.location.pathname;
if (guideBasePath.length > 0 && guideCurrentPage.startsWith(guideBasePath)) {
  guideCurrentPage = guideCurrentPage.substring(guideBasePath.length);
}
Array.from(guideSteps).forEach((step) => {
  // check if not completed before
  if (guideCompleted[step.id] !== undefined && guideCompleted[step.id] === '1') {
    return;
  }

  // check if on target page
  if (step.page && (Array.isArray(step.page) ? !step.page.includes(guideCurrentPage) : step.page !== guideCurrentPage)) {
    return;
  }

  tour.addStep(step);
});

tour.start();
