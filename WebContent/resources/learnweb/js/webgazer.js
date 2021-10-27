/* eslint-disable class-methods-use-this */
/* global webgazer */

class WebgazerCalibrate {
  constructor(options) {
    this.options = Object.assign({
      accuracyHeaderId: 'webgazerAccuracy',
      canvasId: 'plotting_canvas',
      areaSelector: '.calibration-area',
      pointsSelector: '.calibration-point',
      middlePointId: 'pt5',
      measurementResultId: 'precision_measurement',
    }, options);

    // Set to true if you want to save the data even if you reload the page.
    window.saveDataAcrossSessions = true;

    this.pointCalibrate = 0;
    this.calibrationPoints = {};

    this.$accuracyHeader = document.getElementById(this.options.accuracyHeaderId);
    this.$calibrationArea = document.querySelector(this.options.areaSelector);

    if (localStorage.getItem(this.options.accuracyHeaderId)) {
      this.$accuracyHeader.innerHTML = localStorage.getItem(this.options.accuracyHeaderId);
    }

    this._initEvents();
  }

  toggleKarmaFilter() {
    webgazer.applyKalmanFilter(!webgazer.params.applyKalmanFilter);
  }

  /**
   * Restart the calibration process by clearing the local storage and reseting the calibration point
   */
  restart() {
    this.$accuracyHeader.innerHTML = '<a>Not yet Calibrated</a>';
    webgazer.clearData();

    this._clearCalibration();
    this.popUpInstruction();
  }

  recalibrate() {
    // use restart function to restart the calibration
    this.$accuracyHeader.innerHTML = '<a>Not yet Calibrated</a>';
    webgazer.clearData();

    this._clearCalibration();
    this.clearCanvas();
    this.showCalibrationPoint();
  }

  /**
   * Clear the canvas and the calibration button.
   */
  clearCanvas() {
    this.$calibrationArea.style.display = 'none';

    const canvas = document.getElementById(this.options.canvasId);
    canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
  }

  /**
   * Show the instruction of using calibration at the start up screen.
   */
  popUpInstruction() {
    this.clearCanvas();
    PF('calibrationDialog').show();
  }

  /**
   * makes the variables true for 5 seconds & plots the points
   */
  calculateMeasurements() {
    this.$calibrationArea.style.display = 'block';
    this.$calibrationArea.classList.add('middle-only');
    // Sets store_points to true, so all the occurring prediction points are stored
    webgazer.params.storingPoints = true;

    setTimeout(() => {
      // Sets store_points to false, so prediction points aren't stored any more
      webgazer.params.storingPoints = false;
      const past50 = webgazer.getStoredPoints(); // retrieve the stored points
      const precision = this._calculatePrecision(past50);
      const accuracyLabel = `<a>Accuracy | ${precision}%</a>`;
      this.$accuracyHeader.innerHTML = accuracyLabel; // Show the accuracy in the nav bar.
      localStorage.setItem(this.options.accuracyHeaderId, accuracyLabel);

      this.$calibrationArea.style.display = 'none';
      this.$calibrationArea.classList.remove('middle-only');
      document.getElementById(this.options.measurementResultId).textContent = precision;
      PF('measurementResultsDialog').show();
    }, 5000);
  }

  /**
   * Show the Calibration Points
   */
  showCalibrationPoint() {
    this.$calibrationArea.style.display = 'block';
    document.getElementById(this.options.middlePointId).style.display = 'none'; // initially hides the middle button
  }

  _initEvents() {
    /**
     * This function occurs on resizing the frame
     * clears the canvas & then resizes it (as plots have moved position, can't resize without clear)
     */
    window.addEventListener('resize', () => {
      const canvas = document.getElementById(this.options.canvasId);
      const context = canvas.getContext('2d');
      context.clearRect(0, 0, canvas.width, canvas.height);
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    }, false);

    window.addEventListener('beforeunload', () => {
      webgazer.end();
    });

    /**
     * This function listens for button clicks on the html page
     * checks that all buttons have been clicked 5 times each, and then goes on to measuring the precision
     */
    delegateEvent('click', '.calibration-point', (e) => { // click event on the calibration buttons
      const $point = e.target;

      if (!this.calibrationPoints[$point.id]) { // initialises if not done
        this.calibrationPoints[$point.id] = 0;
      }
      this.calibrationPoints[$point.id]++; // increments values

      if (this.calibrationPoints[$point.id] === 5) { // only turn to yellow after 5 clicks
        $point.classList.add('completed');
        $point.disabled = true; // disables the button
        this.pointCalibrate++;
      } else if (this.calibrationPoints[$point.id] < 5) {
        // Gradually increase the opacity of calibration points when click to give some indication to user.
        $point.style.opacity = 0.2 * this.calibrationPoints[$point.id] + 0.2;
      }

      // Show the middle calibration point after all other points have been clicked.
      if (this.pointCalibrate === 8) {
        document.getElementById(this.options.middlePointId).style.display = 'block';
      }

      if (this.pointCalibrate >= 9) { // last point is calibrated
        // using jquery to grab every element in calibration-point class and hide them except the middle point.
        this.clearCanvas();
        document.getElementById(this.options.middlePointId).style.display = 'block';

        // notification for the measurement process
        PF('measurementDialog').show();
      }
    });

    /**
     * Load this function when the index page starts.
     */
    window.onload = () => {
      this.clearCanvas();

      webgazer.params.showVideoPreview = true;
      // start the webgazer tracker
      webgazer.setRegression('ridge') /* currently must set regression and tracker */
        // .setTracker('clmtrackr')
        .setGazeListener((data, clock) => {
          //   console.log(data); /* data is an object containing an x and y key which are the x and y prediction coordinates (no bounds limiting) */
          //   console.log(clock); /* elapsed time in milliseconds since webgazer.begin() was called */
        })
        .saveDataAcrossSessions(true)
        .begin()
        .then(() => {
          webgazer.showVideoPreview(true) /* shows all video previews */
            .showPredictionPoints(true) /* shows a square every 100 milliseconds where current prediction is */
            .applyKalmanFilter(true); /* Kalman Filter defaults to on. Can be toggled by user. */

          // Set up the webgazer video feedback.
          const setup = () => {
            // Set up the main canvas. The main canvas is used to calibrate the webgazer.
            const canvas = document.getElementById(this.options.canvasId);
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
            canvas.style.position = 'fixed';
          };
          setup();
        });
    };
  }

  /**
   * This function clears the calibration buttons memory
   */
  _clearCalibration() {
    // Clear data from WebGazer
    document.querySelectorAll(this.options.pointsSelector).forEach((el) => {
      el.classList.remove('completed');
      el.style.opacity = 0.2;
      el.disabled = false;
    });

    this.calibrationPoints = {};
    this.pointCalibrate = 0;
  }

  /**
   * This function calculates a measurement for how precise
   * the eye tracker currently is which is displayed to the user
   */
  _calculatePrecision(past50Array) {
    const windowHeight = parseInt(getComputedStyle(document.body, null).height.replace('px', ''), 10);
    const windowWidth = parseInt(getComputedStyle(document.body, null).width.replace('px', ''), 10);

    // Retrieve the last 50 gaze prediction points
    const x50 = past50Array[0];
    const y50 = past50Array[1];

    // Calculate the position of the point the user is staring at
    const staringPointX = windowWidth / 2;
    const staringPointY = windowHeight / 2;

    const precisionPercentages = new Array(50);
    this._calculatePrecisionPercentages(precisionPercentages, windowHeight, x50, y50, staringPointX, staringPointY);
    const precision = this._calculateAverage(precisionPercentages);

    // Return the precision measurement as a rounded percentage
    return Math.round(precision);
  }

  /**
   * Calculate percentage accuracy for each prediction based on distance of
   * the prediction point from the centre point (uses the window height as
   * lower threshold 0%)
   */
  _calculatePrecisionPercentages(precisionPercentages, windowHeight, x50, y50, staringPointX, staringPointY) {
    for (let x = 0; x < 50; x++) {
      // Calculate distance between each prediction and staring point
      const xDiff = staringPointX - x50[x];
      const yDiff = staringPointY - y50[x];
      const distance = Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));

      // Calculate precision percentage
      const halfWindowHeight = windowHeight / 2;
      let precision = 0;
      if (distance <= halfWindowHeight && distance > -1) {
        precision = 100 - ((distance / halfWindowHeight) * 100);
      } else if (distance > halfWindowHeight) {
        precision = 0;
      } else if (distance > -1) {
        precision = 100;
      }

      // Store the precision
      precisionPercentages[x] = precision;
    }
  }

  /**
   * Calculates the average of all precision percentages calculated
   */
  _calculateAverage(precisionPercentages) {
    let precision = 0;
    for (let x = 0; x < 50; x++) {
      precision += precisionPercentages[x];
    }
    precision /= 50;
    return precision;
  }
}

function delegateEvent(eventName, elementSelector, handler) {
  document.addEventListener(eventName, function (e) {
    // loop parent nodes from the target to the delegation node
    for (let { target } = e; target && target !== this; target = target.parentNode) {
      if (target.matches(elementSelector)) {
        handler.call(target, e);
        break;
      }
    }
  }, false);
}

const calibrate = new WebgazerCalibrate();
