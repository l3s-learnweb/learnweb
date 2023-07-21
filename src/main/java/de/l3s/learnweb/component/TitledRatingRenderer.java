package de.l3s.learnweb.component;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

import org.primefaces.component.rating.Rating;
import org.primefaces.util.ComponentUtils;

import de.l3s.learnweb.i18n.MessagesBundle;

/**
 * This renderer, adds title to the stars' icons in the rating component.
 */
public class TitledRatingRenderer extends org.primefaces.component.rating.RatingRenderer {
    @Override
    protected void encodeMarkup(final FacesContext context, final Rating rating) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String clientId = rating.getClientId(context);
        String valueToRender = ComponentUtils.getValueToRender(context, rating);
        Integer value = isValueBlank(valueToRender) ? null : Integer.valueOf(valueToRender);
        int stars = rating.getStars();
        boolean disabled = rating.isDisabled();
        boolean readonly = rating.isReadonly();
        String style = rating.getStyle();
        String styleClass = rating.getStyleClass();
        styleClass = styleClass == null ? Rating.CONTAINER_CLASS : Rating.CONTAINER_CLASS + " " + styleClass;

        if (disabled) {
            styleClass = styleClass + " ui-state-disabled";
        }

        writer.startElement("div", rating);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("class", styleClass, null);
        if (style != null) {
            writer.writeAttribute("style", style, null);
        }

        encodeInput(context, rating, clientId + "_input", valueToRender);

        if (rating.isCancel() && !disabled && !readonly) {
            encodeIcon(context, Rating.CANCEL_CLASS);
        }

        UIViewRoot viewRoot = context.getViewRoot();
        Locale locale = viewRoot.getLocale();
        ResourceBundle bundle = MessagesBundle.of(locale);

        String[] customRateNames = {
            bundle.getString("rating.star_1"),
            bundle.getString("rating.star_2"),
            bundle.getString("rating.star_3"),
            bundle.getString("rating.star_4"),
            bundle.getString("rating.star_5")
        };

        // TODO: send PR to primefaces or remove this hack
        //noinspection ConstantValue
        if (customRateNames.length != 0) {
            for (int i = 0, len = customRateNames.length; i < len; i++) {
                String starClass = (value != null && i < value) ? Rating.STAR_ON_CLASS : Rating.STAR_CLASS;
                encodeIcon(context, starClass, customRateNames[i]);
            }
        } else {
            for (int i = 0; i < stars; i++) {
                String starClass = (value != null && i < value) ? Rating.STAR_ON_CLASS : Rating.STAR_CLASS;
                encodeIcon(context, starClass, null);
            }
        }

        writer.endElement("div");
    }

    protected void encodeIcon(FacesContext context, String styleClass, String title) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        writer.startElement("div", null);
        writer.writeAttribute("class", styleClass, null);
        if (title != null) {
            writer.writeAttribute("title", title, null);
        }

        writer.startElement("a", null);
        writer.endElement("a");

        writer.endElement("div");
    }
}
