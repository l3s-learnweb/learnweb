if (window.PrimeFaces) {
  PrimeFaces.locales.en = $.extend(true, {}, PrimeFaces.locales.en, {
    transcriptDeleteSelection: 'Delete selection',
    guideDismiss: 'Dismiss',
    guideGotcha: 'Got it',
  });
  PrimeFaces.locales.en_US = PrimeFaces.locales.en;

  PrimeFaces.locales.de = $.extend(true, {}, PrimeFaces.locales.de, {
    transcriptDeleteSelection: 'Auswahl löschen',
    guideDismiss: 'Schließen',
    guideGotcha: 'Verstanden',
  });

  PrimeFaces.locales.es = $.extend(true, {}, PrimeFaces.locales.de, {
    transcriptDeleteSelection: 'Eliminar selección',
    guideDismiss: 'Descartar',
    guideGotcha: 'Entendido',
  });

  PrimeFaces.locales.it = $.extend(true, {}, PrimeFaces.locales.de, {
    transcriptDeleteSelection: 'Elimina selezione',
    guideDismiss: 'Congedare',
    guideGotcha: 'Capito',
  });

  PrimeFaces.locales.pt = $.extend(true, {}, PrimeFaces.locales.de, {
    transcriptDeleteSelection: 'Eliminar seleção',
    guideDismiss: 'Fechar',
    guideGotcha: 'Entenda',
  });

  PrimeFaces.locales.uk = $.extend(true, {}, PrimeFaces.locales.de, {
    transcriptDeleteSelection: 'Видалити виділене',
    guideDismiss: 'Сховати',
    guideGotcha: 'Зрозуміло',
  });
}
