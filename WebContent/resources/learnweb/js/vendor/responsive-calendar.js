/*!
  # Responsive Celendar widget script
  # by w3widgets
  #
  # Author: Lukasz Kokoszkiewicz
  # http://w3widgets.com/responsive-calendar/
  # v0.9.0
  #
  # Copyright © w3widgets 2013 All Rights Reserved
*/
(function ($) {
  class Calendar {
    constructor(element, options) {
      this.$element = element;
      this.options = options;
      this.weekDays = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
      this.time = new Date();
      this.currentYear = this.time.getFullYear();
      this.currentMonth = this.time.getMonth();

      if (this.options.time) {
        const time = this.splitDateString(this.options.time);
        this.currentYear = time.year;
        this.currentMonth = time.month;
      }

      // Do the initial draw
      this.initialDraw();
    }

    // eslint-disable-next-line class-methods-use-this
    addLeadingZero(num) {
      if (num < 10) {
        return `0${num}`;
      }
      return `${num}`;
    }

    // eslint-disable-next-line class-methods-use-this
    applyTransition(el, transition) {
      return el.css('transition', transition);
    }

    // eslint-disable-next-line class-methods-use-this
    applyBackfaceVisibility(el) {
      return el.css('backface-visibility', 'hidden');
    }

    // eslint-disable-next-line class-methods-use-this
    applyTransform(el, transform) {
      return el.css('transform', transform);
    }

    // eslint-disable-next-line class-methods-use-this
    splitDateString(dateString) {
      const time = dateString.split('-');
      const year = parseInt(time[0], 10);
      const month = parseInt(time[1] - 1, 10);
      const day = parseInt(time[2], 10);

      return {
        year,
        month,
        day,
      };
    }

    initialDraw() {
      return this.drawDays(this.currentYear, this.currentMonth);
    }

    editDays(events) {
      const results = [];
      // eslint-disable-next-line no-restricted-syntax
      for (const [dateString, dayEvents] of Object.entries(events)) {
        this.options.events[dateString] = events[dateString];
        const time = this.splitDateString(dateString);
        const day = this.$element.find(`[data-year="${time.year}"][data-month="${time.month + 1}"][data-day="${time.day}"]`).parent('.day');
        day.removeClass('active');
        day.find('.badge').remove();
        day.find('a').removeAttr('href');
        if (this.currentMonth === time.month || this.options.activateNonCurrentMonths) {
          results.push(this.makeActive(day, dayEvents));
        } else {
          results.push(undefined);
        }
      }
      return results;
    }

    clearDays(days) {
      const results = [];
      for (let j = 0, len = days.length; j < len; j++) {
        const dateString = days[j];
        delete this.options.events[dateString];
        const time = this.splitDateString(dateString);
        const day = this.$element.find(`[data-year="${time.year}"][data-month="${time.month + 1}"][data-day="${time.day}"]`).parent('.day');
        day.removeClass('active');
        day.find('.badge').remove();
        results.push(day.find('a').removeAttr('href'));
      }
      return results;
    }

    clearAll() {
      this.options.events = {};
      const days = this.$element.find('[data-group="days"] .day');
      const results = [];
      for (let i = 0, len = days.length; i < len; i += 1) {
        const day = days[i];
        $(day).removeClass('active');
        $(day).find('.badge').remove();
        results.push($(day).find('a').removeAttr('href'));
      }
      return results;
    }

    setMonthYear(dateString) {
      const time = this.splitDateString(dateString);
      this.currentMonth = this.drawDays(time.year, time.month);
      this.currentYear = time.year;
    }

    prev() {
      if (this.currentMonth - 1 < 0) {
        this.currentYear -= 1;
        this.currentMonth = 11;
      } else {
        this.currentMonth -= 1;
      }
      this.drawDays(this.currentYear, this.currentMonth);
      // callback function
      if (this.options.onMonthChange) {
        this.options.onMonthChange.call(this);
      }
    }

    next() {
      if (this.currentMonth + 1 > 11) {
        this.currentYear += 1;
        this.currentMonth = 0;
      } else {
        this.currentMonth += 1;
      }
      this.drawDays(this.currentYear, this.currentMonth);
      // callback function
      if (this.options.onMonthChange) {
        this.options.onMonthChange.call(this);
      }
    }

    curr() {
      this.currentYear = this.time.getFullYear();
      this.currentMonth = this.time.getMonth();
      this.drawDays(this.currentYear, this.currentMonth);
      // callback function
      if (this.options.onMonthChange) {
        this.options.onMonthChange.call(this);
      }
    }

    // eslint-disable-next-line class-methods-use-this
    addOthers(day, dayEvents) {
      // if events word is an object (array)
      // create badge with the number of events
      if (typeof dayEvents === 'object') {
        // add badge
        if (dayEvents.number != null) {
          const badge = $('<span></span>').html(dayEvents.number).addClass('badge');
          if (dayEvents.badgeClass != null) {
            badge.addClass(dayEvents.badgeClass);
          }
          day.append(badge);
        }
        // add url
        if (dayEvents.url) {
          day.find('a').attr('href', dayEvents.url);
        }
      }
      return day;
    }

    makeActive(day, dayEvents) {
      // if event exists for the given day ...
      if (dayEvents) {
        // ... add class `active`
        if (dayEvents.class) {
          const classes = dayEvents.class.split(' ');
          for (let i = 0, len = classes.length; i < len; i++) {
            const eventClass = classes[i];
            day.addClass(eventClass);
          }
        } else {
          day.addClass('active');
        }

        // add badge
        day = this.addOthers(day, dayEvents);
      }
      return day;
    }

    // eslint-disable-next-line class-methods-use-this
    getDaysInMonth(year, month) {
      return new Date(year, month + 1, 0).getDate();
    }

    drawDay(lastDayOfMonth, yearNum, monthNum, dayNum, i) {
      let day = $('<div></div>').addClass('day');
      const dateNow = new Date();
      dateNow.setHours(0, 0, 0, 0);
      const dayDate = new Date(yearNum, monthNum - 1, dayNum);

      let pastFutureClass;
      if (dayDate.getTime() < dateNow.getTime()) {
        pastFutureClass = 'past';
      } else if (dayDate.getTime() === dateNow.getTime()) {
        pastFutureClass = 'today';
      } else {
        pastFutureClass = 'future';
      }
      day.addClass(this.weekDays[i % 7]);
      day.addClass(pastFutureClass);
      let dateString = `${yearNum}-${this.addLeadingZero(monthNum)}-${this.addLeadingZero(dayNum)}`;

      // starts drawing days from the appropriate day of the week
      if (dayNum <= 0 || dayNum > lastDayOfMonth) {
        const calcDate = new Date(yearNum, monthNum - 1, dayNum);
        dayNum = calcDate.getDate();
        monthNum = calcDate.getMonth() + 1;
        yearNum = calcDate.getFullYear();
        day.addClass('not-current').addClass(pastFutureClass);
        if (this.options.activateNonCurrentMonths) {
          // create date string to access `events` options dictionary
          dateString = `${yearNum}-${this.addLeadingZero(monthNum)}-${this.addLeadingZero(dayNum)}`;
        }
      }
      const anchor = $(`<a>${dayNum}</a>`).attr('data-day', dayNum).attr('data-month', monthNum).attr('data-year', yearNum);
      day.append(anchor);
      if (this.options.monthChangeAnimation) {
        this.applyTransform(day, 'rotateY(180deg)');
        this.applyBackfaceVisibility(day);
      }

      // make active if event for a day exists
      day = this.makeActive(day, this.options.events[dateString]);
      return this.$element.find('[data-group="days"]').append(day);
    }

    drawDays(year, month) {
      const thisRef = this;
      // set initial time parameters
      const time = new Date(year, month);
      const currentMonth = time.getMonth(); // count from 0
      const monthNum = time.getMonth() + 1; // count from 1
      const yearNum = time.getFullYear();

      // get week day for the first day of the current month
      time.setDate(1);
      const firstDayOfMonth = this.options.startFromSunday ? time.getDay() + 1 : time.getDay() || 7; // sunday fix

      // get week day for the last day of the current month
      const lastDayOfMonth = this.getDaysInMonth(year, month);

      // out animation
      let timeout = 0;
      if (this.options.monthChangeAnimation) {
        const days = this.$element.find('[data-group="days"] .day');
        for (let i = 0, len = days.length; i < len; i++) {
          const day = days[i];
          const delay = i * 0.01;
          this.applyTransition($(day), `transform .5s ease ${delay}s`);
          this.applyTransform($(day), 'rotateY(180deg)');
          this.applyBackfaceVisibility($(day));
          timeout = (delay + 0.1) * 1000;
        }
      }

      let loopBase;
      let multiplier;
      const dayBase = 2;
      // celculate loop base / number of possible calendar day cells
      if (this.options.allRows) {
        loopBase = 42;
      } else {
        multiplier = Math.ceil((firstDayOfMonth - (dayBase - 1) + lastDayOfMonth) / 7);
        loopBase = multiplier * 7;
      }
      // @$element.find(".timeInfo").html time.getFullYear() + " " + @options.translateMonths[time.getMonth()]
      this.$element.find('[data-head-year]').html(time.getFullYear());
      this.$element.find('[data-head-month]').html(this.options.translateMonths[time.getMonth()]);
      const draw = function () {
        thisRef.$element.find('[data-group="days"]').empty();
        // fill callendar
        let dayNum = dayBase - firstDayOfMonth;
        let i = thisRef.options.startFromSunday ? 0 : 1;
        while (dayNum < loopBase - firstDayOfMonth + dayBase) {
          thisRef.drawDay(lastDayOfMonth, yearNum, monthNum, dayNum, i);
          dayNum += 1;
          i += 1;
        }
        const setEvents = function () {
          const days = thisRef.$element.find('[data-group="days"] .day');
          for (let j = 0, len = days.length; j < len; j++) {
            const day = days[j];
            thisRef.applyTransition($(day), `transform .5s ease ${j * 0.01}s`);
            thisRef.applyTransform($(day), 'rotateY(0deg)');
          }
          if (thisRef.options.onDayClick) {
            thisRef.$element.find('[data-group="days"] .day a').click(function () {
              return thisRef.options.onDayClick.call(this, thisRef.options.events);
            });
          }
          if (thisRef.options.onDayHover) {
            thisRef.$element.find('[data-group="days"] .day a').hover(function () {
              return thisRef.options.onDayHover.call(this, thisRef.options.events);
            });
          }
          if (thisRef.options.onActiveDayClick) {
            thisRef.$element.find('[data-group="days"] .day.active a').click(function () {
              return thisRef.options.onActiveDayClick.call(this, thisRef.options.events);
            });
          }
          if (thisRef.options.onActiveDayHover) {
            return thisRef.$element.find('[data-group="days"] .day.active a').hover(function () {
              return thisRef.options.onActiveDayHover.call(this, thisRef.options.events);
            });
          }
        };
        return setTimeout(setEvents, 0);
      };
      setTimeout(draw, timeout);
      return currentMonth;
    }
  }

  $.fn.responsiveCalendar = function (option, params) {
    let options = $.extend({}, $.fn.responsiveCalendar.defaults, typeof option === 'object' && option);
    const publicFunc = {
      next: 'next',
      prev: 'prev',
      edit: 'editDays',
      clear: 'clearDays',
      clearAll: 'clearAll',
      getYearMonth: 'getYearMonth',
      jump: 'jump',
      curr: 'curr',
    };

    const init = function ($this) {
      let data;
      // support for metadata plugin
      options = $.metadata ? $.extend({}, options, $this.metadata()) : options;
      $this.data('calendar', (data = new Calendar($this, options)));

      // call onInit function
      if (options.onInit) {
        options.onInit.call(data);
      }

      // create events for manual month change
      return $this.find('[data-go]').click(function () {
        if ($(this).data('go') === 'prev') {
          data.prev();
        }
        if ($(this).data('go') === 'next') {
          return data.next();
        }
      });
    };

    return this.each(function () {
      const $this = $(this);
      // create "calendar" data variable
      const data = $this.data('calendar');
      if (!data) {
        init($this);
      } else if (typeof option === 'string') {
        if (publicFunc[option] != null) {
          data[publicFunc[option]](params);
        } else {
          data.setMonthYear(option); // sets month to display, format "YYYY-MM"
        }
      } else if (typeof option === 'number') {
        data.jump(Math.abs(option) + 1);
      }
      return null;
    });
  };

  // plugin defaults - added as a property on our plugin function
  $.fn.responsiveCalendar.defaults = {
    translateMonths: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'],
    events: {},
    time: undefined, // string - example: "2013-04"
    allRows: true,
    startFromSunday: false,
    activateNonCurrentMonths: false,
    monthChangeAnimation: true,
    // callback functions
    onInit: undefined,
    onDayClick: undefined,
    onDayHover: undefined,
    onActiveDayClick: undefined,
    onActiveDayHover: undefined,
    onMonthChange: undefined,
  };

  const spy = $('[data-spy="responsive-calendar"]');
  if (spy.length) {
    const opts = {};
    if ((spy.data('translate-months')) != null) {
      opts.translateMonths = spy.data('translate-months').split(',');
    }
    // if (spy.data 'events')? then opts.events = spy.data 'events'
    if ((spy.data('time')) != null) {
      opts.time = spy.data('time');
    }
    if ((spy.data('all-rows')) != null) {
      opts.allRows = spy.data('all-rows');
    }
    if ((spy.data('start-from-sunday')) != null) {
      opts.startFromSunday = spy.data('start-from-sunday');
    }
    if ((spy.data('activate-non-current-months')) != null) {
      opts.activateNonCurrentMonths = spy.data('activate-non-current-months');
    }
    if ((spy.data('month-change-animation')) != null) {
      opts.monthChangeAnimation = spy.data('month-change-animation');
    }
    return spy.responsiveCalendar(opts);
  }
}(jQuery));
