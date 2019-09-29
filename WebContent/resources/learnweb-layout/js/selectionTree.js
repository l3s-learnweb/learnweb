$(() => {
  $('.checker').each((i, el) => {
    $(el).on('click', (e) => {
      $(e.currentTarget).parent().find('input:checkbox').attr('checked', e.currentTarget.checked);
    });
  });

  $('.klapper_zu').each((i, el) => {
    $(el).on('click', (e) => {
      $(e.currentTarget).toggleClass('klapper_auf');
      $(e.currentTarget).parent().find('ul').first()
        .slideToggle();
    });
  });
});
