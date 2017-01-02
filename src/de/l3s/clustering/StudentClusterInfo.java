package de.l3s.clustering;

public class StudentClusterInfo
{

    private double radius;
    private String info;
    private String nome;
    private String color;
    private double cx;
    private double cy;

    public StudentClusterInfo()
    {
        super();
    }

    public double getRadius()
    {
        return radius;
    }

    public void setRadius(double radius)
    {
        this.radius = radius;
    }

    public String getInfo()
    {
        return info;
    }

    public void setInfo(String info)
    {
        this.info = info;
    }

    public String getNome()
    {
        return nome;
    }

    public void setNome(String nome)
    {
        this.nome = nome;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public double getCx()
    {
        return cx;
    }

    public void setCx(double cx)
    {
        this.cx = cx;
    }

    public double getCy()
    {
        return cy;
    }

    public void setCy(double cy)
    {
        this.cy = cy;
    }

    @Override
    public String toString()
    {
        return "-- Name:" + this.nome + " -Info:" + this.info + " -Color:" + this.color + " -Radius:" + this.radius + " - (cx, cy):(" + this.cx + "," + this.cy + ")";
    }

}
