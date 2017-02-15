package de.l3s.learnweb.beans;

import java.io.Serializable;

public class Car implements Serializable
{
    private static final long serialVersionUID = 6054761057129544944L;

    String id;
    String brand;
    int year;
    String color;
    int price;
    boolean soldState;

    public Car(String id, String brand, int year, String color, int price, boolean soldState)
    {
        super();
        this.id = id;
        this.brand = brand;
        this.year = year;
        this.color = color;
        this.price = price;
        this.soldState = soldState;
    }

    public Car()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getBrand()
    {
        return brand;
    }

    public void setBrand(String brand)
    {
        this.brand = brand;
    }

    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public int getPrice()
    {
        return price;
    }

    public void setPrice(int price)
    {
        this.price = price;
    }

    public boolean isSoldState()
    {
        return soldState;
    }

    public void setSoldState(boolean soldState)
    {
        this.soldState = soldState;
    }

}
