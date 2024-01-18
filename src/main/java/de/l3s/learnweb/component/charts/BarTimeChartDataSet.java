package de.l3s.learnweb.component.charts;

import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;

import org.primefaces.model.charts.ChartDataSet;
import org.primefaces.model.charts.data.NumericPoint;
import org.primefaces.util.ChartUtils;
import org.primefaces.util.FastStringWriter;

public class BarTimeChartDataSet extends ChartDataSet {
    @Serial
    private static final long serialVersionUID = -2261676926838466056L;

    private ArrayList<NumericPoint> data;
    private String label;
    private String xaxisID;
    private String yaxisID;
    private String stack;
    private String backgroundColor;
    private String borderColor;
    private Integer borderWidth;
    private String borderSkipped;
    private String hoverBackgroundColor;
    private String hoverBorderColor;
    private Integer hoverBorderWidth;

    /**
     * Gets the list of data in this dataSet
     *
     * @return List&#60;Number&#62; list of data
     */
    public ArrayList<NumericPoint> getData() {
        return data;
    }

    /**
     * Sets the list of data in this dataSet
     *
     * @param data List&#60;Number&#62; list of data
     */
    public void setData(ArrayList<NumericPoint> data) {
        this.data = data;
    }

    /**
     * Gets the label
     *
     * @return label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label
     *
     * @param label The label for the dataset which appears in the legend and tooltips
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the xAxisID
     *
     * @return xAxisID
     */
    public String getXaxisID() {
        return xaxisID;
    }

    /**
     * Sets the xAxisID
     *
     * @param xaxisID The ID of the x axis to plot this dataset on.
     * If not specified, this defaults to the ID of the first found x axis
     */
    public void setXaxisID(String xaxisID) {
        this.xaxisID = xaxisID;
    }

    /**
     * Gets the yAxisID
     *
     * @return yAxisID
     */
    public String getYaxisID() {
        return yaxisID;
    }

    /**
     * Sets the yAxisID
     *
     * @param yaxisID The ID of the y axis to plot this dataset on.
     * If not specified, this defaults to the ID of the first found y axis.
     */
    public void setYaxisID(String yaxisID) {
        this.yaxisID = yaxisID;
    }

    /**
     * Gets the stack
     *
     * @return stack
     */
    public String getStack() {
        return stack;
    }

    /**
     * Sets the stack
     *
     * @param stack The ID of the group to which this dataset belongs to (when stacked, each group will be a separate stack)
     */
    public void setStack(String stack) {
        this.stack = stack;
    }

    /**
     * Gets the backgroundColor
     *
     * @return backgroundColor
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the backgroundColor
     *
     * @param backgroundColor The fill color of the bar.
     */
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Gets the borderColor
     *
     * @return borderColor
     */
    public String getBorderColor() {
        return borderColor;
    }

    /**
     * Sets the borderColor
     *
     * @param borderColor The color of the bar border.
     */
    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    /**
     * Gets the borderWidth
     *
     * @return borderWidth
     */
    public Integer getBorderWidth() {
        return borderWidth;
    }

    /**
     * Sets the borderWidth
     *
     * @param borderWidth The stroke width of the bar in pixels.
     */
    public void setBorderWidth(Integer borderWidth) {
        this.borderWidth = borderWidth;
    }

    /**
     * Gets the borderSkipped
     *
     * @return borderSkipped
     */
    public String getBorderSkipped() {
        return borderSkipped;
    }

    /**
     * Sets the borderSkipped
     *
     * @param borderSkipped Which edge to skip drawing the border for.
     */
    public void setBorderSkipped(String borderSkipped) {
        this.borderSkipped = borderSkipped;
    }

    /**
     * Gets the hoverBackgroundColor
     *
     * @return hoverBackgroundColor
     */
    public String getHoverBackgroundColor() {
        return hoverBackgroundColor;
    }

    /**
     * Sets the hoverBackgroundColor
     *
     * @param hoverBackgroundColor The fill colour of the bars when hovered.
     */
    public void setHoverBackgroundColor(String hoverBackgroundColor) {
        this.hoverBackgroundColor = hoverBackgroundColor;
    }

    /**
     * Gets the hoverBorderColor
     *
     * @return hoverBorderColor
     */
    public String getHoverBorderColor() {
        return hoverBorderColor;
    }

    /**
     * Sets the hoverBorderColor
     *
     * @param hoverBorderColor The stroke colour of the bars when hovered.
     */
    public void setHoverBorderColor(String hoverBorderColor) {
        this.hoverBorderColor = hoverBorderColor;
    }

    /**
     * Gets the hoverBorderWidth
     *
     * @return hoverBorderWidth
     */
    public Integer getHoverBorderWidth() {
        return hoverBorderWidth;
    }

    /**
     * Sets the hoverBorderWidth
     *
     * @param hoverBorderWidth The stroke width of the bars when hovered.
     */
    public void setHoverBorderWidth(Integer hoverBorderWidth) {
        this.hoverBorderWidth = hoverBorderWidth;
    }

    /**
     * Gets the type
     *
     * @return type of current chart
     */
    public String getType() {
        return "bar";
    }

    /**
     * Write the options of this dataSet
     *
     * @return options as JSON object
     * @throws java.io.IOException If an I/O error occurs
     */
    @Override
    public String encode() throws IOException {
        try (FastStringWriter fsw = new FastStringWriter()) {

            fsw.write("{");

            ChartUtils.writeDataValue(fsw, "type", this.getType(), false);
            ChartUtils.writeDataValue(fsw, "data", this.data, true);
            ChartUtils.writeDataValue(fsw, "label", this.label, true);
            ChartUtils.writeDataValue(fsw, "hidden", this.isHidden(), true);
            ChartUtils.writeDataValue(fsw, "xAxisID", this.xaxisID, true);
            ChartUtils.writeDataValue(fsw, "yAxisID", this.yaxisID, true);
            ChartUtils.writeDataValue(fsw, "stack", this.stack, true);
            ChartUtils.writeDataValue(fsw, "backgroundColor", this.backgroundColor, true);
            ChartUtils.writeDataValue(fsw, "borderColor", this.borderColor, true);
            ChartUtils.writeDataValue(fsw, "borderWidth", this.borderWidth, true);
            ChartUtils.writeDataValue(fsw, "borderSkipped", this.borderSkipped, true);
            ChartUtils.writeDataValue(fsw, "hoverBackgroundColor", this.hoverBackgroundColor, true);
            ChartUtils.writeDataValue(fsw, "hoverBorderColor", this.hoverBorderColor, true);
            ChartUtils.writeDataValue(fsw, "hoverBorderWidth", this.hoverBorderWidth, true);

            fsw.write("}");

            return fsw.toString();
        }
    }
}
